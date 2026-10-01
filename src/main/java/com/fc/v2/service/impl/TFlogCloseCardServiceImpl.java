package com.fc.v2.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fc.v2.common.exception.biz.FlogBizException;
import com.fc.v2.mapper.auto.TFlogCloseCardMapper;
import com.fc.v2.mapper.auto.TFlogCloseStageMapper;
import com.fc.v2.mapper.auto.TFlogSurveyDesignMapper;
import com.fc.v2.mapper.auto.TFlogSurveyResultMapper;
import com.fc.v2.mapper.auto.TFlogSurveyTreeMapper;
import com.fc.v2.model.auto.TFlogApplyCard;
import com.fc.v2.model.auto.TFlogCloseCard;
import com.fc.v2.model.auto.TFlogCloseStage;
import com.fc.v2.model.auto.TFlogSurveyDesign;
import com.fc.v2.model.auto.TFlogSurveyResult;
import com.fc.v2.model.auto.TFlogSurveyTree;
import com.fc.v2.model.custom.flog.FlogCloseMoveVo;
import com.fc.v2.model.custom.flog.FlogCloseOpenDto;
import com.fc.v2.model.custom.flog.FlogClosePassDto;
import com.fc.v2.model.custom.flog.FlogCloseRollbackDto;
import com.fc.v2.service.ITFlogApplyCardService;
import com.fc.v2.service.ITFlogCloseCardService;
import com.fc.v2.service.flog.FlogDataScope;
import com.fc.v2.shiro.util.ShiroUtils;
import com.fc.v2.util.SnowflakeIdWorker;
import com.fc.v2.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * 伐区勾销事务卡 Service业务层。
 *
 * 四段——报伐、核株、验更、了断——只准挨着走，全卡只有两条动法：
 * {@link #advance} 往前推一格、{@link #rollback} 往回退一格，两条路各只有一个落点。
 * 卡此刻停在哪段，系统照勾销履历（t_flog_close_stage 里 active 的过成行）自己数，
 * 卡头 stage/status/content 只是摆示，屏上摆的、经办人嘴上认的都当不得真；
 * 改没改成只看这一对方法给回的回执。
 *
 * 铁律：
 * 四段只准挨着行——跳段的、倒着走的不算过成；同段第二遍库里只认头一遍，
 * 半句记事都不许再落；真要改口退回去重做，先前那一段落过的名目尽数沉底，
 * 沉掉的与后补的不摆在一处。哪一段写下“这一段不过了”卡就此止步，谁也续不进；
 * 一了断各栏封死，要动只能重新起一张卡；卡永不物理删除。一小班同时只许压一张活卡，
 * 头一张了断或止步后才许另起。
 *
 * @author fuce
 * @date 2026-09-14
 */
@Service
public class TFlogCloseCardServiceImpl implements ITFlogCloseCardService {

    /** 站里定的核株差格（立方米）：|实采蓄积-设计蓄积| 越过此格就得一句说法 */
    private static final BigDecimal DEFAULT_DIFF_TOLERANCE = new BigDecimal("10.0000");

    @Autowired
    private TFlogCloseCardMapper closeCardMapper;

    @Autowired
    private TFlogCloseStageMapper closeStageMapper;

    @Autowired
    private TFlogSurveyDesignMapper surveyDesignMapper;

    @Autowired
    private TFlogSurveyTreeMapper surveyTreeMapper;

    @Autowired
    private TFlogSurveyResultMapper surveyResultMapper;

    @Autowired
    private ITFlogApplyCardService applyCardService;

    @Autowired
    private FlogDataScope dataScope;

    // ------------------------------------------------------------------
    // 查询
    // ------------------------------------------------------------------

    @Override
    public TFlogCloseCard selectTFlogCloseCardById(Long id) {
        TFlogCloseCard card = closeCardMapper.selectById(id);
        if (card != null && card.getDelFlag() != null && card.getDelFlag() == 1) {
            return null;
        }
        if (card != null) {
            dataScope.assertVisible(currentUserId(), card.getSiteNo());
        }
        return card;
    }

    @Override
    public List<TFlogCloseCard> selectTFlogCloseCardList(QueryWrapper<TFlogCloseCard> queryWrapper) {
        QueryWrapper<TFlogCloseCard> w = queryWrapper == null ? new QueryWrapper<>() : queryWrapper;
        w.eq("del_flag", 0);
        if (!dataScope.isSuperior()) {
            List<String> nos = dataScope.visibleSiteNos(currentUserId());
            if (nos.isEmpty()) {
                return new ArrayList<>();
            }
            w.in("site_no", nos);
        }
        // 卡头摆示段次由本口按履历重算后再摆，绝不直接信库里的摆示格
        w.orderByDesc("create_time");
        List<TFlogCloseCard> cards = closeCardMapper.selectList(w);
        for (TFlogCloseCard c : cards) {
            recomputeDisplay(c);
        }
        return cards;
    }

    @Override
    public List<TFlogCloseStage> listHistory(Long id) {
        TFlogCloseCard card = requireCard(id);
        dataScope.assertVisible(currentUserId(), card.getSiteNo());
        return closeStageMapper.selectList(new QueryWrapper<TFlogCloseStage>()
                .eq("card_id", id)
                .eq("del_flag", 0)
                .orderByAsc("stage", "attempt_no", "op_time", "id"));
    }

    /**
     * 勾销履历核验：随便抽一张走到底的卡，从了断往回数回报伐，每一笔都得合得上。
     * active 的过成行必须段段相连、一道不缺、一道不多；沉底行只许是退回的旧轮；
     * 卡头摆示与快照对不上、凭空多出来或缺了页，抽见一处即点破这张卡没走成。
     */
    @Override
    public List<TFlogCloseStage> verifyHistory(Long id) {
        TFlogCloseCard card = requireCard(id);
        dataScope.assertVisible(currentUserId(), card.getSiteNo());
        List<TFlogCloseStage> all = listHistory(id);

        List<TFlogCloseStage> activePass = new ArrayList<>();
        TFlogCloseStage failLine = null;
        for (TFlogCloseStage h : all) {
            int active = h.getActiveFlag() == null ? 0 : h.getActiveFlag();
            if (active == TFlogCloseStage.ACTIVE_YES) {
                if (TFlogCloseStage.ACTION_FAIL.equals(h.getAction())) {
                    if (failLine != null) {
                        throw new FlogBizException("卡号 " + card.getBizNo() + " 履历里 active 的止步记有两道，凭空多出来，这张卡没走成");
                    }
                    failLine = h;
                } else if (TFlogCloseStage.ACTION_PASS.equals(h.getAction())) {
                    activePass.add(h);
                } else {
                    throw new FlogBizException("卡号 " + card.getBizNo() + " 履历里退回行竟还作着数，沉底没沉净，这张卡没走成");
                }
            } else if (!TFlogCloseStage.ACTION_RETREAT.equals(h.getAction())
                    && !TFlogCloseStage.ACTION_PASS.equals(h.getAction())) {
                throw new FlogBizException("卡号 " + card.getBizNo() + " 履历里混进了既非过成也非退回/止步的手笔，这张卡没走成");
            }
        }

        // 段段相连：active 过成行按段次排开必须恰是 0,1,...,n-1
        for (int i = 0; i < activePass.size(); i++) {
            if (activePass.get(i).getStage() == null || activePass.get(i).getStage() != i) {
                throw new FlogBizException("卡号 " + card.getBizNo() + " 勾销履历在第 " + i
                        + " 段缺了页或段次对不上（从了断往回数数不齐），这张卡没走成");
            }
        }
        int n = activePass.size();
        int status = card.getStatus() == null ? TFlogCloseCard.STATUS_ACTIVE : card.getStatus();
        // 卡头摆示段次也得与履历数出来的当前段合得上：摆示格本不作权威，可一旦有人
        // 手动抽了履历最上面一页而卡头还摆着旧段次，两厢一对就点破“缺了页”。
        int headerStage = card.getStage() == null ? 0 : card.getStage();
        int expectStage = status == TFlogCloseCard.STATUS_TERMINAL ? TFlogCloseCard.STAGE_COUNT : n;
        if (headerStage != expectStage) {
            throw new FlogBizException("卡号 " + card.getBizNo() + " 卡头摆着停在第 " + headerStage
                    + " 段，履历却只数得出 " + n + " 段过成，缺了页或被人手动挪过，这张卡没走成");
        }
        if (failLine != null && (failLine.getStage() == null || failLine.getStage() != n)) {
            throw new FlogBizException("卡号 " + card.getBizNo() + " 止步那一笔与过成段数接不上，这张卡没走成");
        }
        if (status == TFlogCloseCard.STATUS_TERMINAL) {
            if (n != TFlogCloseCard.STAGE_COUNT) {
                throw new FlogBizException("卡号 " + card.getBizNo() + " 卡头记着已了断，履历却数不齐四段（只有 "
                        + n + " 段过成），缺了页，这张卡没走成");
            }
            if (failLine != null) {
                throw new FlogBizException("卡号 " + card.getBizNo() + " 了断卡里竟压着一笔止步，凭空多出来，这张卡没走成");
            }
        }
        if (status == TFlogCloseCard.STATUS_HALTED && failLine == null) {
            throw new FlogBizException("卡号 " + card.getBizNo() + " 卡头记着止步，履历里却翻不到止步那一笔，这张卡没走成");
        }

        // 快照与卡头当天那屏对得上号（卡头只存最近一道的数，沉掉的旧道只在履历里）
        if (n > 0) {
            TFlogCloseStage declare = activePass.get(TFlogCloseCard.STAGE_DECLARE);
            if (!eq(declare.getActualVolume(), card.getActualVolume())
                    || !eq(declare.getDesignVolume(), card.getDesignVolume())) {
                throw new FlogBizException("卡号 " + card.getBizNo() + " 报伐那道的数与卡头对不上，像是过后手动补的，这张卡没走成");
            }
        }
        if (n > TFlogCloseCard.STAGE_RENEW) {
            TFlogCloseStage renew = activePass.get(TFlogCloseCard.STAGE_RENEW);
            if (!eq(renew.getRenewArea(), card.getRenewArea())) {
                throw new FlogBizException("卡号 " + card.getBizNo() + " 验更那道的更新面积与卡头对不上，这张卡没走成");
            }
        }
        return all;
    }

    // ------------------------------------------------------------------
    // 开卡即报伐（没有“先开空卡再涂一句”的第二条道）
    // ------------------------------------------------------------------

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FlogCloseMoveVo open(FlogCloseOpenDto dto) {
        if (dto == null) {
            throw new FlogBizException("送上来的报伐口是空的，卡立不起来");
        }
        List<String> missing = new ArrayList<>();
        if (StringUtils.isEmpty(dto.getSiteNo())) {
            missing.add("底档字号");
        }
        if (StringUtils.isEmpty(dto.getSubcompNo())) {
            missing.add("小班号");
        }
        if (StringUtils.isEmpty(dto.getDesignNo())) {
            missing.add("调查设计单号");
        }
        if (dto.getActualTrees() == null || dto.getActualTrees() <= 0) {
            missing.add("实采株数");
        }
        if (dto.getActualVolume() == null || dto.getActualVolume().compareTo(BigDecimal.ZERO) <= 0) {
            missing.add("实采蓄积(正数)");
        }
        if (!missing.isEmpty()) {
            throw new FlogBizException("报伐口立不住，缺了便起不来的格子：" + String.join("、", missing));
        }
        String siteNo = dto.getSiteNo().trim();
        String subcompNo = dto.getSubcompNo().trim();
        dataScope.assertVisible(currentUserId(), siteNo);

        // 认设计单：钉过号的那张，且字号、小班必须是同一片——拿别片的单顶不过来
        TFlogSurveyDesign design = surveyDesignMapper.selectOne(new QueryWrapper<TFlogSurveyDesign>()
                .eq("design_no", dto.getDesignNo().trim())
                .eq("del_flag", 0)
                .last("limit 1"));
        if (design == null) {
            throw new FlogBizException("调查设计单号 " + dto.getDesignNo() + " 查无钉档的设计单，报伐认不上");
        }
        if (!siteNo.equals(design.getSiteNo()) || !subcompNo.equals(design.getSubcompNo())) {
            throw new FlogBizException("设计单 " + design.getDesignNo() + " 钉的是 " + design.getSiteNo()
                    + " " + design.getSubcompNo() + "，与本报 " + siteNo + " " + subcompNo
                    + " 不是同一片小班，报伐认不上");
        }
        if (design.getLatestResultId() == null) {
            throw new FlogBizException("设计单 " + design.getDesignNo() + " 还没钉过定档结论，钉过的号才认");
        }
        TFlogSurveyResult result = surveyResultMapper.selectById(design.getLatestResultId());
        if (result == null || (result.getDelFlag() != null && result.getDelFlag() == 1)) {
            throw new FlogBizException("设计单 " + design.getDesignNo() + " 的定档结论行翻不着，报伐认不上");
        }
        Integer designTrees = surveyTreeMapper.selectCount(new QueryWrapper<TFlogSurveyTree>()
                .eq("design_id", design.getId())
                .eq("round_no", result.getRoundNo())
                .eq("del_flag", 0));
        BigDecimal designVolume = nullToZero(result.getTotalVolume());

        // 一小班同时只许压一张活卡：在办的挡；头一张了断或止步后，第二张才立得起来
        Integer live = closeCardMapper.selectCount(new QueryWrapper<TFlogCloseCard>()
                .eq("site_no", siteNo)
                .eq("subcomp_no", subcompNo)
                .eq("status", TFlogCloseCard.STATUS_ACTIVE)
                .eq("del_flag", 0));
        if (live != null && live > 0) {
            throw new FlogBizException(siteNo + " " + subcompNo
                    + " 已压着一张在办的勾销卡，一小班同时只许一张活卡；要改先把那张了断或止步，再另起新卡");
        }

        // 认这片小班的在册建档卡：了断时就从那一处摘下来、把占着的额度原样退回。
        // 走建档卡自己的台账口：它强制只认现行未删、并按账号辖域收口，这里不另开查询。
        TFlogApplyCard applyCard = applyCardService.selectTFlogApplyCardList(new QueryWrapper<TFlogApplyCard>()
                .eq("site_no", siteNo)
                .eq("subcomp_no", subcompNo)
                .ne("status", 2)
                .last("limit 1"))
                .stream().findFirst().orElse(null);

        Date now = new Date();
        String opBy = currentLoginName();
        String bizNo = nextBizNo(yearOf(now));

        TFlogCloseCard card = new TFlogCloseCard();
        card.setBizNo(bizNo);
        card.setSiteNo(siteNo);
        card.setSubcompNo(subcompNo);
        card.setCutBlockNo(design.getCutBlockNo());
        card.setDesignNo(design.getDesignNo());
        card.setDesignTrees(designTrees == null ? 0 : designTrees);
        card.setDesignVolume(designVolume);
        card.setCutArea(design.getArea());
        card.setActualTrees(dto.getActualTrees());
        card.setActualVolume(dto.getActualVolume());
        BigDecimal tol = dto.getDiffTolerance() == null ? DEFAULT_DIFF_TOLERANCE : dto.getDiffTolerance();
        if (tol.compareTo(BigDecimal.ZERO) < 0) {
            throw new FlogBizException("核株差格不能是负数");
        }
        card.setDiffTolerance(tol);
        if (applyCard != null) {
            card.setApplyCardId(applyCard.getId());
            card.setOccupyQty(nullToZero(applyCard.getQty()));
            card.setQuotaYear(yearOf(applyCard.getCreateTime() == null
                    ? (applyCard.getRegisterTime() == null ? now : applyCard.getRegisterTime())
                    : applyCard.getCreateTime()));
        }
        card.setStatus(TFlogCloseCard.STATUS_ACTIVE);
        card.setStage(1);
        card.setDelFlag(0);
        // 活卡对锁：在办期间占着“字号@小班”，了断/止步才放开；唯一索引兜并发
        card.setLiveKey(siteNo + "@" + subcompNo);
        card.setCreateBy(opBy);
        card.setCreateTime(now);
        card.setRemark(dto.getRemark());
        card.setContent("报伐已过成");
        card.setLastAction(TFlogCloseStage.ACTION_PASS + ":0");
        try {
            closeCardMapper.insert(card);
        } catch (org.springframework.dao.DuplicateKeyException dk) {
            throw new FlogBizException(siteNo + " " + subcompNo
                    + " 已压着一张在办的勾销卡，一小班同时只许一张活卡；要改先把那张了断或止步，再另起新卡");
        }

        TFlogCloseStage h = newStageRow(card, TFlogCloseCard.STAGE_DECLARE, TFlogCloseStage.ACTION_PASS, now, opBy);
        h.setDesignNo(design.getDesignNo());
        h.setActualTrees(dto.getActualTrees());
        h.setActualVolume(dto.getActualVolume());
        h.setDesignTrees(card.getDesignTrees());
        h.setDesignVolume(designVolume);
        h.setNote("报伐：设计单 " + design.getDesignNo() + "，实采 " + dto.getActualTrees()
                + " 株、" + dto.getActualVolume() + " 立方米");
        closeStageMapper.insert(h);

        FlogCloseMoveVo vo = FlogCloseMoveVo.of(card);
        vo.setAction(TFlogCloseStage.ACTION_PASS);
        vo.setNote(h.getNote());
        vo.setMessage("勾销卡 " + bizNo + " 已立，报伐段过成，当前停在核株段"
                + (applyCard == null ? "（这片小班没有在册建档卡占着额度，了断时无额可退）"
                : "（认着建档卡 " + applyCard.getBillNo() + "，占额 " + card.getOccupyQty() + " 立方米）"));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FlogCloseMoveVo open(TFlogCloseCard record) {
        // 实体送法：只取业务栏，卡号/段次/落定一律不由界面塞
        if (record == null) {
            throw new FlogBizException("送上来的报伐口是空的，卡立不起来");
        }
        FlogCloseOpenDto dto = new FlogCloseOpenDto();
        dto.setSiteNo(record.getSiteNo());
        dto.setSubcompNo(record.getSubcompNo());
        dto.setCutBlockNo(record.getCutBlockNo());
        dto.setDesignNo(record.getDesignNo());
        dto.setActualTrees(record.getActualTrees());
        dto.setActualVolume(record.getActualVolume());
        dto.setDiffTolerance(record.getDiffTolerance());
        dto.setRemark(record.getRemark());
        return open(dto);
    }

    // ------------------------------------------------------------------
    // 唯一往前口：往前推进一格
    // ------------------------------------------------------------------

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FlogCloseMoveVo advance(FlogClosePassDto dto) {
        if (dto == null || dto.getId() == null) {
            throw new FlogBizException("推进得认一张勾销卡（id）");
        }
        TFlogCloseCard card = requireCard(dto.getId());
        dataScope.assertVisible(currentUserId(), card.getSiteNo());
        card = lockAndLocate(card);
        int stage = card.getStage();
        if (stage >= TFlogCloseCard.STAGE_COUNT) {
            throw new FlogBizException("卡号 " + card.getBizNo() + " 四段已走完、了断封死，要动只能重新起一张卡");
        }
        // 同一段过第二遍，库里只认头一遍——半句记事都不再落
        if (existsActive(card.getId(), stage)) {
            throw new FlogBizException("卡号 " + card.getBizNo() + " " + stageName(stage)
                    + " 段头一遍已过成，第二遍库里不认；真要改口请退回上一段重做");
        }

        Date now = new Date();
        String opBy = currentLoginName();
        TFlogCloseStage h = newStageRow(card, stage, TFlogCloseStage.ACTION_PASS, now, opBy);
        String note;
        CloseOutcome outcome = null;
        UpdateWrapper<TFlogCloseCard> uw = new UpdateWrapper<TFlogCloseCard>()
                .eq("id", card.getId())
                .eq("del_flag", 0)
                .eq("status", TFlogCloseCard.STATUS_ACTIVE);

        switch (stage) {
            case TFlogCloseCard.STAGE_DECLARE: {
                // 退回头一段后的重做：卡与设计单已钉死，不能换片，只准把实采株数、实采蓄积重报一遍
                if (dto.getRenewArea() != null || dto.getSurviveRate() != null) {
                    throw new FlogBizException("卡号 " + card.getBizNo() + " 眼下重过报伐关口，验更的数报不上来；四段只准挨着行");
                }
                Integer actualTrees = dto.getActualTrees();
                BigDecimal actualVolume = dto.getActualVolume();
                if (actualTrees == null || actualTrees <= 0) {
                    throw new FlogBizException("重过报伐得把实采株数（正整数）重新报齐");
                }
                if (actualVolume == null || actualVolume.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new FlogBizException("重过报伐得把实采蓄积（正数）重新报齐");
                }
                card.setActualTrees(actualTrees);
                card.setActualVolume(actualVolume);
                card.setDiffReason(null);
                h.setDesignNo(card.getDesignNo());
                h.setActualTrees(actualTrees);
                h.setActualVolume(actualVolume);
                h.setDesignTrees(card.getDesignTrees());
                h.setDesignVolume(nullToZero(card.getDesignVolume()));
                uw.set("actual_trees", actualTrees)
                        .set("actual_volume", actualVolume)
                        .set("diff_reason", null);
                note = "报伐重做过成：设计单 " + card.getDesignNo() + "，实采 " + actualTrees
                        + " 株、" + actualVolume + " 立方米";
                break;
            }
            case TFlogCloseCard.STAGE_CHECK: {
                // 卡停在核株，只放核株的关口：夹带验更的数来就是想跳段，一概不算过成
                if (dto.getRenewArea() != null || dto.getSurviveRate() != null) {
                    throw new FlogBizException("卡号 " + card.getBizNo() + " 眼下停在核株段，验更的数报不上来；四段只准挨着行，跳段的不算过成");
                }
                // 核株：把实采与设计两笔对着看。山里复核带了新数就用新数，没带仍照报伐那两笔。
                Integer actualTrees = dto.getActualTrees() != null ? dto.getActualTrees() : card.getActualTrees();
                BigDecimal actualVolume = dto.getActualVolume() != null ? dto.getActualVolume() : card.getActualVolume();
                if (actualTrees == null || actualTrees <= 0) {
                    throw new FlogBizException("核株得有实采株数（正整数），缺了过不了这道关口");
                }
                if (actualVolume == null || actualVolume.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new FlogBizException("核株得有实采蓄积（正数），缺了过不了这道关口");
                }
                BigDecimal designVolume = nullToZero(card.getDesignVolume());
                BigDecimal diff = actualVolume.subtract(designVolume).abs();
                BigDecimal tol = card.getDiffTolerance() == null ? DEFAULT_DIFF_TOLERANCE : card.getDiffTolerance();
                String reason = dto.getDiffReason() == null ? "" : dto.getDiffReason().trim();
                if (diff.compareTo(tol) > 0 && StringUtils.isEmpty(reason)) {
                    throw new FlogBizException("卡号 " + card.getBizNo() + " 核株对不上：实采 " + actualVolume
                            + " 与设计 " + designVolume + " 差 " + diff.stripTrailingZeros().toPlainString()
                            + " 立方米，越过站里定的差格 " + tol.stripTrailingZeros().toPlainString()
                            + "，得给一句说法才过得这道关口");
                }
                card.setActualTrees(actualTrees);
                card.setActualVolume(actualVolume);
                card.setDiffReason(StringUtils.isEmpty(reason) ? null : reason);
                h.setActualTrees(actualTrees);
                h.setActualVolume(actualVolume);
                h.setDesignTrees(card.getDesignTrees());
                h.setDesignVolume(designVolume);
                h.setDiffValue(diff);
                // 本段对上的两笔与说法同卡头一道落库，不留给第二条道补
                uw.set("actual_trees", actualTrees)
                        .set("actual_volume", actualVolume)
                        .set("diff_reason", StringUtils.isEmpty(reason) ? null : reason);
                note = "核株过成：实采 " + actualVolume + " 对设计 " + designVolume
                        + "，差 " + diff.stripTrailingZeros().toPlainString() + " 立方米"
                        + (StringUtils.isEmpty(reason) ? "，在差格内" : "，说法：" + reason);
                break;
            }
            case TFlogCloseCard.STAGE_RENEW: {
                // 停在验更却报核株的数，那是倒着走，不算过成
                if (dto.getActualTrees() != null || dto.getActualVolume() != null
                        || StringUtils.isNotEmpty(dto.getDiffReason())) {
                    throw new FlogBizException("卡号 " + card.getBizNo() + " 核株段已过成封在头一遍里，倒着再报一笔不算过成；真要改口请退回重做");
                }
                // 验更：看更新造林的面积与成活；面积比采伐面积小，当场点出来
                BigDecimal renewArea = dto.getRenewArea();
                if (renewArea == null || renewArea.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new FlogBizException("验更得报更新造林面积（正数），缺了过不了这道关口");
                }
                BigDecimal cutArea = nullToZero(card.getCutArea());
                if (renewArea.compareTo(cutArea) < 0) {
                    throw new FlogBizException("卡号 " + card.getBizNo() + " 更新面积 " + renewArea
                            + " 公顷比采伐面积 " + cutArea + " 公顷小，更新不得小于采伐面积，当场点出，这道关口没过成");
                }
                BigDecimal rate = dto.getSurviveRate();
                if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0
                        || rate.compareTo(new BigDecimal("100")) > 0) {
                    throw new FlogBizException("验更还得报更新成活（0 到 100 之间的正数），缺了或不像话都过不了这道关口");
                }
                card.setRenewArea(renewArea);
                card.setSurviveRate(rate);
                h.setRenewArea(renewArea);
                h.setSurviveRate(rate);
                uw.set("renew_area", renewArea).set("survive_rate", rate);
                note = "验更过成：更新 " + renewArea + " 公顷（采伐 " + cutArea + "）"
                        + "，成活 " + rate.stripTrailingZeros().toPlainString() + "%";
                break;
            }
            case TFlogCloseCard.STAGE_CLOSE: {
                // 了断段无关口可报：夹带任何段的数来都不收
                if (dto.getActualTrees() != null || dto.getActualVolume() != null
                        || dto.getRenewArea() != null || dto.getSurviveRate() != null
                        || StringUtils.isNotEmpty(dto.getDiffReason())) {
                    throw new FlogBizException("卡号 " + card.getBizNo() + " 了断只办摘册退额，前三段的数一个都不收；要改口请退回重做");
                }
                // 了断：把这片小班从在册那一处摘下来，占着的额度原样退回
                outcome = doClose(card, h);
                note = outcome.note;
                uw.set("occupy_qty", BigDecimal.ZERO);
                uw.set("live_key", null);
                break;
            }
            default:
                throw new FlogBizException("不认的段次 " + stage);
        }

        h.setNote(note);
        closeStageMapper.insert(h);

        int passCount = stage + 1;
        boolean terminal = stage == TFlogCloseCard.STAGE_CLOSE;
        // 并发令牌只认落定（在办）+ 行锁：摆示段次不作权威，绝不能拿它当条件
        uw.set("status", terminal ? TFlogCloseCard.STATUS_TERMINAL : TFlogCloseCard.STATUS_ACTIVE)
                .set("stage", terminal ? TFlogCloseCard.STAGE_COUNT : passCount)
                .set("content", note)
                .set("last_action", TFlogCloseStage.ACTION_PASS + ":" + stage)
                .set("update_by", opBy)
                .set("update_time", now);
        int rows = closeCardMapper.update(null, uw);
        if (rows == 0) {
            throw new FlogBizException("卡号 " + card.getBizNo() + " 刚被另一笔过口（已止步或已了断），本笔请刷新后再看");
        }
        card.setStatus(terminal ? TFlogCloseCard.STATUS_TERMINAL : TFlogCloseCard.STATUS_ACTIVE);
        card.setStage(terminal ? TFlogCloseCard.STAGE_COUNT : passCount);
        card.setContent(note);
        card.setLastAction(TFlogCloseStage.ACTION_PASS + ":" + stage);

        FlogCloseMoveVo vo = buildVo(card, TFlogCloseStage.ACTION_PASS, note,
                outcome == null ? null : outcome.refund,
                "卡号 " + card.getBizNo() + " " + stageName(stage) + "段过成，当前停在 "
                        + (terminal ? "了断（封死）" : stageName(passCount) + "段"));
        if (outcome != null) {
            vo.setOccupiedQty(outcome.occupied);
            vo.setRemainQty(outcome.remain);
        }
        return vo;
    }

    /**
     * 旧送法（id + 一句备注）照样调得通，走的是同一对规矩：
     * 核株段无新数即照报伐两笔对着看，备注当差过格时的说法；
     * 验更段关口要更新面积，备注送法给不了，得走 dto 口。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TFlogCloseCard advance(Long id, String remark) {
        FlogClosePassDto dto = new FlogClosePassDto();
        dto.setId(id);
        dto.setDiffReason(remark);
        advance(dto);
        return recomputeDisplay(requireCard(id));
    }

    // ------------------------------------------------------------------
    // 唯一往回口：往回退一格
    // ------------------------------------------------------------------

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FlogCloseMoveVo rollback(FlogCloseRollbackDto dto) {
        if (dto == null || dto.getId() == null) {
            throw new FlogBizException("退回得认一张勾销卡（id）");
        }
        return rollbackCore(dto.getId(), dto.getReason());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TFlogCloseCard rollback(Long id, String remark) {
        // 旧入参旧返回一直可调：退回一格后，把系统重算过段次的卡给回
        rollbackCore(id, remark);
        return cardOf(id);
    }

    private FlogCloseMoveVo rollbackCore(Long id, String reason) {
        TFlogCloseCard card = requireCard(id);
        dataScope.assertVisible(currentUserId(), card.getSiteNo());
        card = lockAndLocate(card);
        int stage = card.getStage();
        int status = card.getStatus() == null ? TFlogCloseCard.STATUS_ACTIVE : card.getStatus();
        if (status == TFlogCloseCard.STATUS_TERMINAL) {
            // 一了断各栏随即封死，要动只能重新起一张卡
            throw new FlogBizException("卡号 " + card.getBizNo() + " 已了断封死，退不回来了；要动只能重新起一张卡");
        }
        if (stage <= TFlogCloseCard.STAGE_DECLARE) {
            // 报伐是头一道、与开卡一体，它前面再没有一段；卡也不许抽走，不要它只能写下止步
            throw new FlogBizException("卡号 " + card.getBizNo()
                    + " 已停在报伐段，往前没有可退的一段；这张卡抽不走，不要它只能写下止步");
        }
        int target = stage - 1;

        Date now = new Date();
        String opBy = currentLoginName();

        // 先前那一段落过的名目尽数沉底：该段所有 active 行改 0，沉掉的与后补的不摆在一处
        int sunk = closeStageMapper.update(null, new UpdateWrapper<TFlogCloseStage>()
                .eq("card_id", card.getId())
                .eq("stage", target)
                .eq("active_flag", TFlogCloseStage.ACTIVE_YES)
                .set("active_flag", TFlogCloseStage.ACTIVE_NO));

        // 退回本身也只续一笔、且不作数（active=0），履历上看得出退过、当前段却数不进它
        TFlogCloseStage retreat = TFlogCloseStageBuilder.retreat(card, target,
                nextAttemptNo(card.getId(), target), now, opBy, reason);
        closeStageMapper.insert(retreat);

        // 卡头上被沉掉那段占着的业务格一并回退到上一道口径：沉掉的不许还摆在屏上
        UpdateWrapper<TFlogCloseCard> uw = new UpdateWrapper<TFlogCloseCard>()
                .eq("id", card.getId())
                .eq("del_flag", 0)
                .eq("status", TFlogCloseCard.STATUS_ACTIVE)
                .set("stage", target)
                .set("content", retreat.getNote())
                .set("last_action", TFlogCloseStage.ACTION_RETREAT + ":" + target)
                .set("update_by", opBy)
                .set("update_time", now);
        if (target == TFlogCloseCard.STAGE_DECLARE) {
            // 报伐沉底：旧的实采两数随头一道沉掉，卡头先清空，等重过报伐关口再落新数
            card.setActualTrees(null);
            card.setActualVolume(null);
            card.setDiffReason(null);
            uw.set("actual_trees", null)
                    .set("actual_volume", null)
                    .set("diff_reason", null);
        } else if (target == TFlogCloseCard.STAGE_CHECK) {
            // 核株沉底：实采两数恢复成报伐头一遍那道，核株说法随旧道沉掉。
            // 报伐道此刻尚未沉（沉的是 target=核株），active 还在，抓得住头一遍快照。
            TFlogCloseStage declare = activeStage(card.getId(), TFlogCloseCard.STAGE_DECLARE);
            card.setActualTrees(declare == null ? card.getActualTrees() : declare.getActualTrees());
            card.setActualVolume(declare == null ? card.getActualVolume() : declare.getActualVolume());
            card.setDiffReason(null);
            uw.set("actual_trees", card.getActualTrees());
            uw.set("actual_volume", card.getActualVolume());
            uw.set("diff_reason", null);
        } else if (target == TFlogCloseCard.STAGE_RENEW) {
            // 验更沉底：更新面积与成活旧数不摆
            card.setRenewArea(null);
            card.setSurviveRate(null);
            uw.set("renew_area", null);
            uw.set("survive_rate", null);
        }
        int rows = closeCardMapper.update(null, uw);
        if (rows == 0) {
            throw new FlogBizException("卡号 " + card.getBizNo() + " 刚被另一笔过口，本笔请刷新后再看");
        }
        card.setStage(target);
        card.setContent(retreat.getNote());
        card.setLastAction(TFlogCloseStage.ACTION_RETREAT + ":" + target);

        String note = "退回" + stageName(target) + "段重做，先前那道" + sunk + "笔名目已尽数沉底";
        return buildVo(card, TFlogCloseStage.ACTION_RETREAT, note, null,
                "卡号 " + card.getBizNo() + " " + note + "，沉掉的与后补的不摆在一处");
    }

    /** 履历行构造另收一处，免得退回行与过成行的默认值互相串。 */
    private static final class TFlogCloseStageBuilder {
        static TFlogCloseStage retreat(TFlogCloseCard card, int target, int attemptNo,
                                       Date now, String opBy, String reason) {
            TFlogCloseStage h = new TFlogCloseStage();
            h.setCardId(card.getId());
            h.setBizNo(card.getBizNo());
            h.setStage(target);
            h.setAttemptNo(attemptNo);
            h.setAction(TFlogCloseStage.ACTION_RETREAT);
            h.setActiveFlag(TFlogCloseStage.ACTIVE_NO);
            h.setNote("退回重做" + (reason == null || reason.trim().isEmpty() ? "" : "：" + reason.trim()));
            h.setOpBy(opBy);
            h.setOpTime(now);
            h.setDelFlag(0);
            h.setCreateBy(opBy);
            h.setCreateTime(now);
            return h;
        }
    }

    private TFlogCloseStage activeStage(Long cardId, int stage) {
        List<TFlogCloseStage> hs = closeStageMapper.selectList(new QueryWrapper<TFlogCloseStage>()
                .eq("card_id", cardId)
                .eq("stage", stage)
                .eq("active_flag", TFlogCloseStage.ACTIVE_YES)
                .eq("action", TFlogCloseStage.ACTION_PASS)
                .eq("del_flag", 0)
                .orderByDesc("attempt_no")
                .last("limit 1"));
        return hs.isEmpty() ? null : hs.get(0);
    }

    // ------------------------------------------------------------------
    // 止步：哪一段写下“这一段不过了”，卡就此止步，过后谁也续不进去
    // ------------------------------------------------------------------

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FlogCloseMoveVo halt(Long id, String reason) {
        TFlogCloseCard card = requireCard(id);
        dataScope.assertVisible(currentUserId(), card.getSiteNo());
        card = lockAndLocate(card);
        int stage = card.getStage();
        if (stage >= TFlogCloseCard.STAGE_COUNT) {
            throw new FlogBizException("卡号 " + card.getBizNo() + " 已了断，封死了，止步无从写起");
        }
        if (StringUtils.isEmpty(reason)) {
            throw new FlogBizException("写下止步得附一句缘故（这一段为什么不过了）");
        }
        Date now = new Date();
        String opBy = currentLoginName();

        TFlogCloseStage fail = newStageRow(card, stage, TFlogCloseStage.ACTION_FAIL, now, opBy);
        fail.setNote(stageName(stage) + "段止步：" + reason);
        closeStageMapper.insert(fail);

        int rows = closeCardMapper.update(null, new UpdateWrapper<TFlogCloseCard>()
                .eq("id", card.getId())
                .eq("del_flag", 0)
                .eq("status", TFlogCloseCard.STATUS_ACTIVE)
                .set("status", TFlogCloseCard.STATUS_HALTED)
                .set("live_key", null)
                .set("content", fail.getNote())
                .set("last_action", TFlogCloseStage.ACTION_FAIL + ":" + stage)
                .set("update_by", opBy)
                .set("update_time", now));
        if (rows == 0) {
            throw new FlogBizException("卡号 " + card.getBizNo() + " 刚被另一笔过口，本笔请刷新后再看");
        }
        card.setStatus(TFlogCloseCard.STATUS_HALTED);
        card.setContent(fail.getNote());
        card.setLastAction(TFlogCloseStage.ACTION_FAIL + ":" + stage);
        return buildVo(card, TFlogCloseStage.ACTION_FAIL, fail.getNote(), null,
                "卡号 " + card.getBizNo() + " 已在 " + stageName(stage) + " 段止步封死，过后谁也续不进去；"
                        + "这片小班要再走，只能重新起一张卡");
    }

    // ------------------------------------------------------------------
    // 内部
    // ------------------------------------------------------------------

    /**
     * 了断段落法：从在册建档卡处摘下（走它自己的压死口，锁行、退额、续流水一处办齐），
     * 退回数与本笔快照钉在了断履历上。
     */
    private CloseOutcome doClose(TFlogCloseCard card, TFlogCloseStage h) {
        BigDecimal refund = BigDecimal.ZERO;
        BigDecimal occupied = null;
        BigDecimal remain = null;
        String pickNote;
        if (card.getApplyCardId() != null) {
            TFlogApplyCard apply = applyCardService.selectTFlogApplyCardById(card.getApplyCardId());
            if (apply == null) {
                throw new FlogBizException("卡号 " + card.getBizNo()
                        + " 认着的建档卡翻不着，摘不下册来，了断没过成");
            }
            if (apply.getStatus() != null && apply.getStatus() == 2) {
                // 建档卡已被别处压死：摘册已完成，无额可退，照样钉了断
                pickNote = "建档卡 " + apply.getBillNo() + " 此前已压死摘册，本笔无额可退";
            } else {
                // 同一事务里走建档卡唯一压死口：占额原样退回其归属年度账；
                // 退多少以压死回执那笔实际退账为准，不拿本卡快照自算
                com.fc.v2.model.custom.flog.FlogCardSaveVo pressed = applyCardService.pressCard(apply.getId());
                refund = pressed.getDeltaQty() == null ? BigDecimal.ZERO : pressed.getDeltaQty().negate();
                occupied = pressed.getOccupiedQty();
                remain = pressed.getRemainQty();
                pickNote = "建档卡 " + apply.getBillNo() + " 已从在册处摘下，占额 " + refund
                        + " 立方米原样退回 " + card.getQuotaYear() + " 年度账";
            }
        } else {
            pickNote = "这片小班没有在册建档卡占着额度，无额可退";
        }
        h.setRefundQty(refund);
        card.setOccupyQty(BigDecimal.ZERO);
        return new CloseOutcome("了断：" + pickNote, refund, occupied, remain);
    }

    /** 了断出口：记事一句话连同退回后的额度账。 */
    private static class CloseOutcome {
        final String note;
        final BigDecimal refund;
        final BigDecimal occupied;
        final BigDecimal remain;

        CloseOutcome(String note, BigDecimal refund, BigDecimal occupied, BigDecimal remain) {
            this.note = note;
            this.refund = refund;
            this.occupied = occupied;
            this.remain = remain;
        }
    }

    /**
     * 卡此刻停在哪段，只照履历算：active 过成几道就在几段；止步/了断一并按落定校。
     * 先对卡行上 FOR UPDATE：同一卡的推进/退回/止步在本事务里串行，
     * “同段第二遍只认头一遍”靠这把行锁 + 锁内查 active 双保险。
     */
    private TFlogCloseCard lockAndLocate(TFlogCloseCard card) {
        TFlogCloseCard fresh = closeCardMapper.selectOne(new QueryWrapper<TFlogCloseCard>()
                .eq("id", card.getId())
                .last("for update"));
        if (fresh == null || (fresh.getDelFlag() != null && fresh.getDelFlag() == 1)) {
            throw new FlogBizException("这张勾销卡不在册，办不了");
        }
        int status = fresh.getStatus() == null ? TFlogCloseCard.STATUS_ACTIVE : fresh.getStatus();
        if (status == TFlogCloseCard.STATUS_HALTED) {
            throw new FlogBizException("卡号 " + fresh.getBizNo() + " 已止步封死，谁也续不进去；要走只能重新起一张卡");
        }
        Integer cnt = closeStageMapper.selectCount(new QueryWrapper<TFlogCloseStage>()
                .eq("card_id", fresh.getId())
                .eq("active_flag", TFlogCloseStage.ACTIVE_YES)
                .eq("action", TFlogCloseStage.ACTION_PASS)
                .eq("del_flag", 0));
        int n = cnt == null ? 0 : cnt;
        if (status == TFlogCloseCard.STATUS_TERMINAL && n < TFlogCloseCard.STAGE_COUNT) {
            throw new FlogBizException("卡号 " + fresh.getBizNo() + " 卡头了断与履历对不上段数，这张卡没走成");
        }
        // 把履历算出的数回填到摆示格（屏上摆的一律以这一口径为准）
        fresh.setStage(status == TFlogCloseCard.STATUS_TERMINAL ? TFlogCloseCard.STAGE_COUNT : n);
        return fresh;
    }

    private boolean existsActive(Long cardId, int stage) {
        Integer c = closeStageMapper.selectCount(new QueryWrapper<TFlogCloseStage>()
                .eq("card_id", cardId)
                .eq("stage", stage)
                .eq("active_flag", TFlogCloseStage.ACTIVE_YES)
                .eq("del_flag", 0));
        return c != null && c > 0;
    }

    private int nextAttemptNo(Long cardId, int stage) {
        // 只数过成/止步行：退回行与它开启的那一轮同号（退回开了第 2 轮，新补的过成也记第 2 轮）
        Integer c = closeStageMapper.selectCount(new QueryWrapper<TFlogCloseStage>()
                .eq("card_id", cardId)
                .eq("stage", stage)
                .ne("action", TFlogCloseStage.ACTION_RETREAT)
                .eq("del_flag", 0));
        return (c == null ? 0 : c) + 1;
    }

    private TFlogCloseStage newStageRow(TFlogCloseCard card, int stage, String action, Date now, String opBy) {
        TFlogCloseStage h = new TFlogCloseStage();
        h.setCardId(card.getId());
        h.setBizNo(card.getBizNo());
        h.setStage(stage);
        h.setAttemptNo(nextAttemptNo(card.getId(), stage));
        h.setAction(action);
        h.setActiveFlag(TFlogCloseStage.ACTIVE_YES);
        h.setOpBy(opBy);
        h.setOpTime(now);
        h.setDelFlag(0);
        h.setCreateBy(opBy);
        h.setCreateTime(now);
        return h;
    }

    private TFlogCloseCard requireCard(Long id) {
        if (id == null) {
            throw new FlogBizException("没给卡号，办不了这一笔");
        }
        TFlogCloseCard card = closeCardMapper.selectById(id);
        if (card == null || (card.getDelFlag() != null && card.getDelFlag() == 1)) {
            throw new FlogBizException("这张勾销事务卡不存在，年终查不着的卡更不许现造一张顶替");
        }
        return card;
    }

    /** 按履历重算单卡摆示（含落定/段次），供查询口与旧签名返回用。 */
    private TFlogCloseCard recomputeDisplay(TFlogCloseCard card) {
        if (card == null) {
            return null;
        }
        Integer cnt = closeStageMapper.selectCount(new QueryWrapper<TFlogCloseStage>()
                .eq("card_id", card.getId())
                .eq("active_flag", TFlogCloseStage.ACTIVE_YES)
                .eq("action", TFlogCloseStage.ACTION_PASS)
                .eq("del_flag", 0));
        int status = card.getStatus() == null ? TFlogCloseCard.STATUS_ACTIVE : card.getStatus();
        card.setStage(cnt == null ? 0 : (status == TFlogCloseCard.STATUS_TERMINAL ? TFlogCloseCard.STAGE_COUNT : cnt));
        return card;
    }

    private TFlogCloseCard cardOf(Long id) {
        return recomputeDisplay(requireCard(id));
    }

    private FlogCloseMoveVo buildVo(TFlogCloseCard card, String action, String note,
                                    BigDecimal refund, String message) {
        FlogCloseMoveVo vo = FlogCloseMoveVo.of(card);
        vo.setAction(action);
        vo.setNote(note);
        vo.setRefundQty(refund);
        vo.setMessage(message);
        return vo;
    }

    private String nextBizNo(int year) {
        for (int i = 0; i < 10; i++) {
            String no = "GX" + year + "-" + String.format("%08d",
                    Math.abs((SnowflakeIdWorker.getUUID() + "-gx-" + i).hashCode()) % 100000000L);
            Integer cnt = closeCardMapper.selectCount(new QueryWrapper<TFlogCloseCard>()
                    .eq("biz_no", no)
                    .eq("del_flag", 0));
            if (cnt == null || cnt == 0) {
                return no;
            }
        }
        throw new FlogBizException("勾销事务卡号连撞，缓一缓再报");
    }

    private String stageName(int stage) {
        switch (stage) {
            case TFlogCloseCard.STAGE_DECLARE:
                return "报伐";
            case TFlogCloseCard.STAGE_CHECK:
                return "核株";
            case TFlogCloseCard.STAGE_RENEW:
                return "验更";
            case TFlogCloseCard.STAGE_CLOSE:
                return "了断";
            default:
                return "第" + stage + "段";
        }
    }

    private boolean eq(BigDecimal a, BigDecimal b) {
        return nullToZero(a).compareTo(nullToZero(b)) == 0;
    }

    private BigDecimal nullToZero(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private Long currentUserId() {
        try {
            return ShiroUtils.getUserId();
        } catch (Exception e) {
            return null;
        }
    }

    private String currentLoginName() {
        try {
            return ShiroUtils.getLoginName();
        } catch (Exception e) {
            return "Task";
        }
    }

    private int yearOf(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date == null ? new Date() : date);
        return cal.get(Calendar.YEAR);
    }
}
