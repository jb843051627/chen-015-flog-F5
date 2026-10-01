package com.fc.v2.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.exception.biz.FlogBizException;
import com.fc.v2.mapper.auto.TFlogSurveyDesignMapper;
import com.fc.v2.mapper.auto.TFlogSurveyResultMapper;
import com.fc.v2.mapper.auto.TFlogSurveyTreeMapper;
import com.fc.v2.model.auto.TFlogSurveyDesign;
import com.fc.v2.model.auto.TFlogSurveyResult;
import com.fc.v2.model.auto.TFlogSurveyTree;
import com.fc.v2.model.custom.flog.FlogSurveyCalcDto;
import com.fc.v2.model.custom.flog.FlogSurveyCalcVo;
import com.fc.v2.model.custom.flog.FlogSurveyDesignDto;
import com.fc.v2.model.custom.flog.FlogSurveyRecheckDto;
import com.fc.v2.model.custom.flog.FlogSurveySubmitVo;
import com.fc.v2.service.ITFlogSurveyDesignService;
import com.fc.v2.service.ITFlogYieldRuleService;
import com.fc.v2.service.flog.FlogDataScope;
import com.fc.v2.util.SnowflakeIdWorker;
import com.fc.v2.util.StringUtils;

/**
 * 伐区调查设计单 Service业务层处理。
 *
 * 从检尺两数到出材量再到定档，只调界册服务那条求值方法：本类绝不自己挑界、不自己定档，
 * 线代号与启用之日压在结论上的值一律以求值返回为准。结论钉过不再改口；
 * 整改复验重算一遍只追加一行，两回结论各留各的代号，合成一条不算对。
 *
 * @author fuce
 * @date 2026-09-30
 */
@Service
public class TFlogSurveyDesignServiceImpl implements ITFlogSurveyDesignService {

    /** 首评轮次 */
    private static final int ROUND_FIRST = 1;
    /** 首评 */
    private static final int RECHECK_NO = 0;
    /** 整改复验 */
    private static final int RECHECK_YES = 1;

    @Autowired
    private TFlogSurveyDesignMapper designMapper;

    @Autowired
    private TFlogSurveyTreeMapper treeMapper;

    @Autowired
    private TFlogSurveyResultMapper resultMapper;

    @Autowired
    private ITFlogYieldRuleService yieldRuleService;

    @Autowired
    private FlogDataScope dataScope;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FlogSurveySubmitVo submit(FlogSurveyDesignDto dto) {
        if (dto == null) {
            throw new FlogBizException("送上来的设计单是空的，没东西可算");
        }
        List<String> missing = new ArrayList<>();
        if (StringUtils.isEmpty(dto.getSiteNo())) {
            missing.add("底档字号");
        }
        if (StringUtils.isEmpty(dto.getSubcompNo())) {
            missing.add("小班号");
        }
        if (StringUtils.isEmpty(dto.getCutBlockNo())) {
            missing.add("伐区名号");
        }
        if (!missing.isEmpty()) {
            throw new FlogBizException("设计单立不住，缺了：" + String.join("、", missing));
        }
        dataScope.assertVisible(currentUserId(), dto.getSiteNo());

        TFlogSurveyDesign design = new TFlogSurveyDesign();
        design.setDesignNo(StringUtils.isEmpty(dto.getDesignNo()) ? nextDesignNo() : dto.getDesignNo().trim());
        design.setSiteNo(dto.getSiteNo());
        design.setSubcompNo(dto.getSubcompNo());
        design.setCutBlockNo(dto.getCutBlockNo());
        design.setApplyScope(dto.getApplyScope());
        design.setArea(dto.getArea());
        design.setYieldRate(dto.getYieldRate());
        design.setSurveyBy(dto.getSurveyBy());
        design.setDesignDate(dto.getDesignDate());
        design.setDelFlag(0);
        design.setCreateTime(new Date());
        design.setCreateBy(currentLoginName());

        // 同号不许立两张：重录结论请走整改复验，不在这口覆写
        Integer dup = designMapper.selectCount(new QueryWrapper<TFlogSurveyDesign>()
                .eq("design_no", design.getDesignNo())
                .eq("del_flag", 0));
        if (dup != null && dup > 0) {
            throw new FlogBizException("调查设计单号 " + design.getDesignNo() + " 已在册，结论钉过不改口；要改走整改复验");
        }

        // 算在落单之前：检尺来路不对当场退回，连单头也不钉
        FlogSurveyCalcVo calcVo = doEvaluate(design, dto.getTrees());
        designMapper.insert(design);
        FlogSurveySubmitVo vo = persistRound(design, ROUND_FIRST, calcVo, dto.getTrees());
        vo.setMessage("单号 " + design.getDesignNo() + " 首评已钉：" + vo.getMessage());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FlogSurveySubmitVo recheck(FlogSurveyRecheckDto dto) {
        if (dto == null || (dto.getDesignId() == null && StringUtils.isEmpty(dto.getDesignNo()))) {
            throw new FlogBizException("复验得认一张单（id 与单号给其一）");
        }
        TFlogSurveyDesign design = requireDesign(dto.getDesignId(), dto.getDesignNo());
        dataScope.assertVisible(currentUserId(), design.getSiteNo());

        // 下一轮次：各轮结论行数出来，绝不覆盖前轮
        Integer rounds = resultMapper.selectCount(new QueryWrapper<TFlogSurveyResult>()
                .eq("design_id", design.getId())
                .eq("del_flag", 0));
        int nextRound = (rounds == null ? 0 : rounds) + 1;

        // 算在任何落库之前：复验检尺来路不对，旧单旧结论原样留着
        FlogSurveyCalcVo calcVo = doEvaluate(design, dto.getTrees());
        FlogSurveySubmitVo vo = persistRound(design, nextRound, calcVo, dto.getTrees());
        vo.setRecheckFlag(RECHECK_YES);
        vo.setMessage("单号 " + design.getDesignNo() + " 第 " + nextRound + " 轮整改复验已钉（与前 "
                + (nextRound - 1) + " 轮各留各的代号，不并条不改口）：" + vo.getMessage());
        return vo;
    }

    @Override
    public TFlogSurveyDesign selectById(Long id) {
        TFlogSurveyDesign d = designMapper.selectById(id);
        if (d != null) {
            dataScope.assertVisible(currentUserId(), d.getSiteNo());
        }
        return d;
    }

    @Override
    public List<TFlogSurveyDesign> listDesigns(String siteNo, String subcompNo, Integer applyScope) {
        QueryWrapper<TFlogSurveyDesign> w = new QueryWrapper<TFlogSurveyDesign>()
                .eq("del_flag", 0)
                .eq(!StringUtils.isEmpty(siteNo), "site_no", siteNo == null ? null : siteNo.trim())
                .like(!StringUtils.isEmpty(subcompNo), "subcomp_no",
                        subcompNo == null ? null : subcompNo.trim())
                .eq(applyScope != null, "apply_scope", applyScope)
                .orderByDesc("design_date");
        if (!dataScope.isSuperior()) {
            // 翻看界：上级全辖；本站/受托只摆名下那几家
            List<String> nos = dataScope.visibleSiteNos(currentUserId());
            if (nos.isEmpty()) {
                return new ArrayList<>();
            }
            w.in("site_no", nos);
        }
        return designMapper.selectList(w);
    }

    @Override
    public TFlogSurveyDesign selectByDesignNo(String designNo) {
        if (StringUtils.isEmpty(designNo)) {
            return null;
        }
        TFlogSurveyDesign d = designMapper.selectOne(new QueryWrapper<TFlogSurveyDesign>()
                .eq("design_no", designNo)
                .eq("del_flag", 0)
                .last("limit 1"));
        if (d != null) {
            dataScope.assertVisible(currentUserId(), d.getSiteNo());
        }
        return d;
    }

    @Override
    public List<TFlogSurveyResult> listResults(Long designId) {
        if (designId == null) {
            return new ArrayList<>();
        }
        TFlogSurveyDesign d = designMapper.selectById(designId);
        if (d != null) {
            dataScope.assertVisible(currentUserId(), d.getSiteNo());
        }
        return resultMapper.selectList(new QueryWrapper<TFlogSurveyResult>()
                .eq("design_id", designId)
                .eq("del_flag", 0)
                .orderByAsc("round_no"));
    }

    @Override
    public List<TFlogSurveyTree> listTrees(Long designId, Integer roundNo) {
        if (designId == null) {
            return new ArrayList<>();
        }
        Integer round = roundNo;
        if (round == null) {
            TFlogSurveyResult last = resultMapper.selectOne(new QueryWrapper<TFlogSurveyResult>()
                    .eq("design_id", designId)
                    .eq("del_flag", 0)
                    .orderByDesc("round_no")
                    .last("limit 1"));
            if (last == null) {
                return new ArrayList<>();
            }
            round = last.getRoundNo();
        }
        return treeMapper.selectList(new QueryWrapper<TFlogSurveyTree>()
                .eq("design_id", designId)
                .eq("round_no", round)
                .eq("del_flag", 0)
                .orderByAsc("tree_no"));
    }

    // ------------------------------------------------------------------
    // 唯一算钉口：调一次界册服务的三级链求值，返回原样落库；不二次取界、不二次算
    // ------------------------------------------------------------------

    /** 先算：构参调唯一求值口；检尺来路不对（负数、四位九、零株）在这一处抛出，不进落库 */
    private FlogSurveyCalcVo doEvaluate(TFlogSurveyDesign design,
                                        List<FlogSurveyCalcDto.TreeMeasure> trees) {
        FlogSurveyCalcDto calc = new FlogSurveyCalcDto();
        calc.setApplyScope(design.getApplyScope());
        calc.setDesignDate(design.getDesignDate());
        calc.setArea(design.getArea());
        calc.setYieldRate(design.getYieldRate());
        calc.setTrees(trees);
        return yieldRuleService.evaluate(calc);
    }

    /**
     * 后钉：求值返回原样落三轮——本轮检尺账（含逐株材积）、定档结论行、单头指向。
     * 不二次取界、不二次算；线代号与启用日以求值返回为准。
     */
    private FlogSurveySubmitVo persistRound(TFlogSurveyDesign design, int roundNo,
                                            FlogSurveyCalcVo calcVo,
                                            List<FlogSurveyCalcDto.TreeMeasure> trees) {
        // 1) 本轮逐株检尺账连同一回算出的逐株材积一并钉入（不许回写取另一版表）
        for (int i = 0; i < trees.size(); i++) {
            TFlogSurveyTree t = new TFlogSurveyTree();
            t.setDesignId(design.getId());
            t.setRoundNo(roundNo);
            t.setTreeNo(i + 1);
            t.setDbh(trees.get(i).getDbh());
            t.setHeight(trees.get(i).getHeight());
            if (i < calcVo.getTreeVolumes().size()) {
                t.setTreeVolume(calcVo.getTreeVolumes().get(i));
            }
            t.setDelFlag(0);
            treeMapper.insert(t);
        }

        // 2) 结论行：评定结果连同所依代号、界启用日、材积表版原样钉死；无线可依也写明钉一行
        TFlogSurveyResult result = new TFlogSurveyResult();
        result.setDesignId(design.getId());
        result.setRoundNo(roundNo);
        result.setRecheckFlag(roundNo == ROUND_FIRST ? RECHECK_NO : RECHECK_YES);
        result.setTotalVolume(calcVo.getTotalVolume());
        result.setYieldPerHa(calcVo.getYieldPerHa());
        result.setGradeLevel(calcVo.isBasisMissing() ? null : calcVo.getGradeLevel());
        result.setBasisMissing(calcVo.isBasisMissing() ? 1 : 0);
        result.setRuleCode(calcVo.getRuleCode());
        result.setRuleEffStart(calcVo.getRuleEffStart());
        result.setVolTableCode(calcVo.getVolTableCode());
        result.setVolTableEffStart(calcVo.getVolTableEffStart());
        result.setPinnedBy(currentLoginName());
        result.setPinnedTime(new Date());
        result.setDelFlag(0);
        result.setRemark(calcVo.getMessage());
        resultMapper.insert(result);

        // 3) 单头只指向最近一轮，旧结论行原样留着
        design.setLatestResultId(result.getId());
        design.setUpdateBy(currentLoginName());
        design.setUpdateTime(new Date());
        designMapper.updateById(design);

        return toVo(design, roundNo, calcVo);
    }

    private FlogSurveySubmitVo toVo(TFlogSurveyDesign design, int roundNo, FlogSurveyCalcVo c) {
        FlogSurveySubmitVo vo = new FlogSurveySubmitVo();
        vo.setDesignId(design.getId());
        vo.setDesignNo(design.getDesignNo());
        vo.setSiteNo(design.getSiteNo());
        vo.setSubcompNo(design.getSubcompNo());
        vo.setApplyScope(design.getApplyScope());
        vo.setDesignDate(design.getDesignDate());
        vo.setRoundNo(roundNo);
        vo.setRecheckFlag(roundNo == ROUND_FIRST ? RECHECK_NO : RECHECK_YES);
        vo.setTotalVolume(c.getTotalVolume());
        vo.setYieldPerHa(c.getYieldPerHa());
        vo.setGradeLevel(c.isBasisMissing() ? null : c.getGradeLevel());
        vo.setBasisMissing(c.isBasisMissing());
        vo.setRuleCode(c.getRuleCode());
        vo.setRuleEffStart(c.getRuleEffStart());
        vo.setVolTableCode(c.getVolTableCode());
        vo.setVolTableEffStart(c.getVolTableEffStart());
        vo.setMessage(c.getMessage());
        return vo;
    }

    private TFlogSurveyDesign requireDesign(Long id, String designNo) {
        TFlogSurveyDesign d = null;
        if (id != null) {
            d = designMapper.selectById(id);
        } else {
            d = designMapper.selectOne(new QueryWrapper<TFlogSurveyDesign>()
                    .eq("design_no", designNo)
                    .eq("del_flag", 0)
                    .last("limit 1"));
        }
        if (d == null || (d.getDelFlag() != null && d.getDelFlag() == 1)) {
            throw new FlogBizException("这张调查设计单不存在或已删，复验无处落");
        }
        return d;
    }

    private String nextDesignNo() {
        for (int i = 0; i < 10; i++) {
            String no = "DC-SD-" + String.format("%010d",
                    Math.abs((SnowflakeIdWorker.getUUID() + "-" + i).hashCode()) % 10000000000L);
            Integer cnt = designMapper.selectCount(new QueryWrapper<TFlogSurveyDesign>()
                    .eq("design_no", no)
                    .eq("del_flag", 0));
            if (cnt == null || cnt == 0) {
                return no;
            }
        }
        throw new FlogBizException("调查设计单号连撞，缓一缓再报");
    }

    private Long currentUserId() {
        try {
            return com.fc.v2.shiro.util.ShiroUtils.getUserId();
        } catch (Exception e) {
            return null;
        }
    }

    private String currentLoginName() {
        try {
            return com.fc.v2.shiro.util.ShiroUtils.getLoginName();
        } catch (Exception e) {
            return null;
        }
    }
}
