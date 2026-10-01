package com.fc.v2.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.exception.biz.FlogBizException;
import com.fc.v2.mapper.auto.TFlogVolumeTableMapper;
import com.fc.v2.mapper.auto.TFlogYieldRuleMapper;
import com.fc.v2.model.auto.TFlogVolumeTable;
import com.fc.v2.model.auto.TFlogYieldRule;
import com.fc.v2.model.custom.flog.FlogApplyScope;
import com.fc.v2.model.custom.flog.FlogSurveyCalcDto;
import com.fc.v2.model.custom.flog.FlogSurveyCalcVo;
import com.fc.v2.service.ITFlogYieldRuleService;

/**
 * 可采强度界限值线 Service业务层处理（rule-eval 形状）。
 *
 * 全站取界只走这一处：
 * 一、请界看单子上的调查设计那一日，不看今天。时段左含右不含——旧线管到交棒日前一刻，
 *     新线交棒日开口；撤了旗、删了的不取；让位版凭它在窗口内的旧日子仍请得回，只供求值回查。
 * 二、两条界同时靠上的，序位大的说话；序位也分不出高下，认代号排在后头那条。
 * 三、定档挨着界往高里数：与中界一样高落重界，越过重界的统统算重界；
 *     重界缺着（早年纸质录入的几条）是不设重档，随中界往下落中档。
 * 四、从检尺两数到出材量再到定档只在 {@link #evaluate(FlogSurveyCalcDto)} 一处走完：
 *     界版、并线、材积表版、逐株材积同一回计算现算现给，不许两头各取一版表。
 *
 * @author fuce
 * @date 2026-09-14
 */
@Service
public class TFlogYieldRuleServiceImpl implements ITFlogYieldRuleService {

    /** 强度档位：轻 */
    private static final int GRADE_LIGHT = 1;
    /** 强度档位：中 */
    private static final int GRADE_MEDIUM = 2;
    /** 强度档位：重 */
    private static final int GRADE_HEAVY = 3;
    /** 翻不到依据 / 输入不合法时的档位 */
    private static final int GRADE_NONE = 0;

    /** 逐株材积保留位数 */
    private static final int SCALE_TREE_VOLUME = 6;
    /** 总蓄积、每公顷出材量保留位数 */
    private static final int SCALE_TOTAL = 4;

    /** 山场记账的哨兵数：胸径量成“四位九”即录入来路不对，退回重录 */
    private static final BigDecimal DBH_SENTINEL = new BigDecimal("9999");

    /** 序位降序；并列按代号降序（代号排在后头那条说话），全库一个口径 */
    private static final Comparator<TFlogYieldRule> RULE_ORDER = new Comparator<TFlogYieldRule>() {
        @Override
        public int compare(TFlogYieldRule a, TFlogYieldRule b) {
            int pa = a.getPriority() == null ? 0 : a.getPriority();
            int pb = b.getPriority() == null ? 0 : b.getPriority();
            if (pa != pb) {
                return pb - pa;
            }
            String ca = a.getRuleCode() == null ? "" : a.getRuleCode();
            String cb = b.getRuleCode() == null ? "" : b.getRuleCode();
            return cb.compareTo(ca);
        }
    };

    /** 材积表版同一套并线口径：序位降序、代号降序 */
    private static final Comparator<TFlogVolumeTable> TABLE_ORDER = new Comparator<TFlogVolumeTable>() {
        @Override
        public int compare(TFlogVolumeTable a, TFlogVolumeTable b) {
            int pa = a.getPriority() == null ? 0 : a.getPriority();
            int pb = b.getPriority() == null ? 0 : b.getPriority();
            if (pa != pb) {
                return pb - pa;
            }
            String ca = a.getTableCode() == null ? "" : a.getTableCode();
            String cb = b.getTableCode() == null ? "" : b.getTableCode();
            return cb.compareTo(ca);
        }
    };

    @javax.annotation.Resource
    private TFlogYieldRuleMapper flogYieldRuleMapper;

    @javax.annotation.Resource
    private TFlogVolumeTableMapper flogVolumeTableMapper;

    /**
     * 时段比较统一走**墙钟字符串**（yyyy-MM-dd HH:mm:ss）绑定：
     * JDBC 的 serverTimezone 与本机时区不对称，直接把 java.util.Date 作参数会整体偏移，
     * 使边界判定在"交棒当日"这类用例上随机错档（实测踩到）。
     */
    private static String ts(Date d) {
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(d);
    }

    @Override
    public TFlogYieldRule selectTFlogYieldRuleById(Long id) {
        return this.flogYieldRuleMapper.selectById(id);
    }

    // ------------------------------------------------------------------
    // 旧调用口：按代号取线求值，照旧可调
    // ------------------------------------------------------------------

    @Override
    public int evaluate(String ruleCode, BigDecimal input, Date at) {
        if (ruleCode == null || ruleCode.trim().isEmpty() || input == null || at == null) {
            return GRADE_NONE;
        }
        if (input.compareTo(BigDecimal.ZERO) < 0) {
            return GRADE_NONE;
        }
        TFlogYieldRule rule = findRuleByCode(ruleCode, at);
        if (rule == null) {
            return GRADE_NONE;
        }
        return levelOf(rule, input);
    }

    // ------------------------------------------------------------------
    // 三级链：检尺两数 → 材积（同一版表）→ 每公顷出材量 → 靠界定档，一处走完
    // ------------------------------------------------------------------

    @Override
    public FlogSurveyCalcVo evaluate(FlogSurveyCalcDto dto) {
        validateCalcInput(dto);

        FlogSurveyCalcVo vo = new FlogSurveyCalcVo();
        vo.setDesignDate(dto.getDesignDate());
        vo.setApplyScope(dto.getApplyScope());
        vo.setTreeCount(dto.getTrees().size());

        // 第二级的表版与第三级的界版都按“调查设计那一日”请，各请各的，但请与算只在这一回。
        TFlogVolumeTable table = pickVolumeTable(dto.getDesignDate());
        TFlogYieldRule rule = pickRule(dto.getApplyScope(), dto.getDesignDate());

        if (table != null) {
            vo.setVolTableCode(table.getTableCode());
            vo.setVolTableEffStart(table.getEffStart());

            // 同一份检尺：逐株材积在这里算一遍，总蓄积就是这份逐株值的累加——
            // 绝不许蓄积回写取一版表、强度评定另取一版表。
            List<BigDecimal> treeVolumes = new ArrayList<>();
            BigDecimal total = BigDecimal.ZERO;
            for (FlogSurveyCalcDto.TreeMeasure t : dto.getTrees()) {
                BigDecimal v = volumeOf(table, t);
                treeVolumes.add(v);
                total = total.add(v);
            }
            total = total.setScale(SCALE_TOTAL, RoundingMode.HALF_UP);
            BigDecimal yieldPerHa = total.multiply(dto.getYieldRate())
                    .divide(dto.getArea(), SCALE_TOTAL, RoundingMode.HALF_UP);
            vo.setTreeVolumes(treeVolumes);
            vo.setTotalVolume(total);
            vo.setYieldPerHa(yieldPerHa);
        }

        List<String> missing = new ArrayList<>();
        if (rule != null) {
            vo.setRuleCode(rule.getRuleCode());
            vo.setRuleName(rule.getRuleName());
            vo.setRuleEffStart(rule.getEffStart());
            // 出材量算得出才靠界；材积表缺失时这一趟随 basisMissing 写明无线，不硬定档
            if (vo.getYieldPerHa() != null) {
                vo.setGradeLevel(levelOf(rule, vo.getYieldPerHa()));
            }
        } else {
            missing.add("调查设计日 " + ts(dto.getDesignDate()) + " 在场的"
                    + FlogApplyScope.of(dto.getApplyScope()).getName() + "强度界");
        }
        if (table == null) {
            missing.add("调查设计日 " + ts(dto.getDesignDate()) + " 在场的材积表版");
        }

        if (missing.isEmpty()) {
            vo.setBasisMissing(false);
            vo.setMessage("依界 " + rule.getRuleCode() + "（" + rule.getRuleName() + "，"
                    + ts(rule.getEffStart()) + " 启用）、材积表 " + table.getTableCode()
                    + " 同一回计算定档：每公顷出材量 " + vo.getYieldPerHa() + "，档位 "
                    + gradeName(vo.getGradeLevel()));
        } else {
            // 翻不出依据的，这一趟写明无线可依，切勿拿邻近日子上的某条来凑
            vo.setBasisMissing(true);
            vo.setGradeLevel(null);
            vo.setMessage("无线可依：翻不出" + String.join("与", missing)
                    + "；本趟不评定强度档位，不许拿邻近日子的界/表凑数");
        }
        return vo;
    }

    @Override
    public int evaluateTop(BigDecimal input, Date at) {
        if (input == null || at == null || input.compareTo(BigDecimal.ZERO) < 0) {
            return GRADE_NONE;
        }
        List<TFlogYieldRule> avail = listAvailable(at);
        if (avail.isEmpty()) {
            return GRADE_NONE;
        }
        return levelOf(avail.get(0), input);
    }

    @Override
    public int evaluateTop(BigDecimal input, Integer applyScope, Date at) {
        if (input == null || at == null || !FlogApplyScope.isValid(applyScope)
                || input.compareTo(BigDecimal.ZERO) < 0) {
            return GRADE_NONE;
        }
        List<TFlogYieldRule> avail = listAvailable(applyScope, at);
        if (avail.isEmpty()) {
            return GRADE_NONE;
        }
        return levelOf(avail.get(0), input);
    }

    @Override
    public List<TFlogYieldRule> listAvailable(Date at) {
        if (at == null) {
            return new ArrayList<>();
        }
        List<TFlogYieldRule> rows = this.flogYieldRuleMapper.selectList(new QueryWrapper<TFlogYieldRule>()
                .eq("del_flag", 0)
                .eq("present_flag", 1)
                .le("eff_start", ts(at))
                .and(w -> w.isNull("eff_end").or().gt("eff_end", ts(at))));
        Collections.sort(rows, RULE_ORDER);
        return rows;
    }

    @Override
    public List<TFlogYieldRule> listAllVersions(String ruleCode, Integer applyScope, Integer status) {
        QueryWrapper<TFlogYieldRule> w = new QueryWrapper<TFlogYieldRule>()
                .eq("del_flag", 0)
                .eq(ruleCode != null && !ruleCode.trim().isEmpty(), "rule_code", ruleCode.trim())
                .eq(applyScope != null, "apply_scope", applyScope)
                .eq(status != null, "status", status)
                .orderByAsc("apply_scope", "rule_code", "eff_start");
        return this.flogYieldRuleMapper.selectList(w);
    }

    @Override
    public List<TFlogYieldRule> listAvailable(Integer applyScope, Date at) {        if (at == null || !FlogApplyScope.isValid(applyScope)) {
            return new ArrayList<>();
        }
        List<TFlogYieldRule> all = listAvailable(at);
        List<TFlogYieldRule> rs = new ArrayList<>();
        for (TFlogYieldRule r : all) {
            if (applyScope.equals(r.getApplyScope())) {
                rs.add(r);
            }
        }
        return rs;
    }

    @Override
    public TFlogYieldRule pickRule(Integer applyScope, Date at) {
        List<TFlogYieldRule> avail = listAvailable(applyScope, at);
        return avail.isEmpty() ? null : avail.get(0);
    }

    @Override
    public TFlogVolumeTable pickVolumeTable(Date at) {
        if (at == null) {
            return null;
        }
        List<TFlogVolumeTable> rows = this.flogVolumeTableMapper.selectList(new QueryWrapper<TFlogVolumeTable>()
                .eq("del_flag", 0)
                .eq("present_flag", 1)
                .le("eff_start", ts(at))
                .and(w -> w.isNull("eff_end").or().gt("eff_end", ts(at))));
        if (rows.isEmpty()) {
            return null;
        }
        Collections.sort(rows, TABLE_ORDER);
        return rows.get(0);
    }

    @Override
    public boolean usable(Long id, Date at) {
        if (id == null || at == null) {
            return false;
        }
        // 窗口条件同样走字符串绑定，与定位/列表同口径（见类头时区注记）
        Integer cnt = this.flogYieldRuleMapper.selectCount(new QueryWrapper<TFlogYieldRule>()
                .eq("id", id)
                .eq("del_flag", 0)
                .eq("present_flag", 1)
                .le("eff_start", ts(at))
                .and(w -> w.isNull("eff_end").or().gt("eff_end", ts(at))));
        return cnt != null && cnt > 0;
    }

    @Override
    public int countAvailable(Date at) {
        return listAvailable(at).size();
    }

    @Override
    public int countAvailable(Integer applyScope, Date at) {
        return listAvailable(applyScope, at).size();
    }

    // ------------------------------------------------------------------
    // 内线
    // ------------------------------------------------------------------

    /** 按代号请 at 时刻在场的那一版（同代号多版时窗口互不重叠，取窗口内的一条） */
    private TFlogYieldRule findRuleByCode(String ruleCode, Date at) {
        List<TFlogYieldRule> hit = this.flogYieldRuleMapper.selectList(new QueryWrapper<TFlogYieldRule>()
                .eq("del_flag", 0)
                .eq("present_flag", 1)
                .eq("rule_code", ruleCode)
                .le("eff_start", ts(at))
                .and(w -> w.isNull("eff_end").or().gt("eff_end", ts(at))));
        return hit.isEmpty() ? null : hit.get(0);
    }

    /**
     * 定档：挨着界往高里数。
     * x &lt; th1 轻；th1 &lt;= x &lt; th2 中；x &gt;= th2 重（与中界一样高即落重，越过重界仍算重）。
     * 重界(th3)缺着＝不设重档，到了中界及以上随中界往下落中档。
     * 轻界缺着时中界以下落轻档（同理不设的那一档往下落）。
     */
    private int levelOf(TFlogYieldRule rule, BigDecimal x) {
        if (x == null) {
            return GRADE_NONE;
        }
        BigDecimal th1 = rule.getTh1Max();
        BigDecimal th2 = rule.getTh2Max();
        BigDecimal th3 = rule.getTh3Max();

        // 轻、中两条界都空着＝这条界定不了档（纸质录入只允许重界空，不允许连中界也空）
        if (th1 == null && th2 == null) {
            return GRADE_NONE;
        }
        if (th1 != null && x.compareTo(th1) < 0) {
            return GRADE_LIGHT;
        }
        if (th2 != null && x.compareTo(th2) < 0) {
            return GRADE_MEDIUM;
        }
        // 到了中界及以上：设有重界（th3 在册）落重界；重界空着=不设重档，随中界落中档
        return th3 != null ? GRADE_HEAVY : GRADE_MEDIUM;
    }

    /** 二元立木材积式 V = a * D^b * H^c（D 厘米、H 米），逐株算到 6 位 */
    private BigDecimal volumeOf(TFlogVolumeTable t, FlogSurveyCalcDto.TreeMeasure m) {
        double v = t.getParamA().doubleValue()
                * Math.pow(m.getDbh().doubleValue(), t.getExpD().doubleValue())
                * Math.pow(m.getHeight().doubleValue(), t.getExpH().doubleValue());
        return BigDecimal.valueOf(v).setScale(SCALE_TREE_VOLUME, RoundingMode.HALF_UP);
    }

    /**
     * 检尺进门校验：来路不对的数不进链，退回调查员重录。
     * 负数、胸径四位九(9999)、株数为零，连同设计日/面积/出材率/林地类别逐一点名。
     */
    private void validateCalcInput(FlogSurveyCalcDto dto) {
        if (dto == null) {
            throw new FlogBizException("送上来的检尺账是空的，没东西可算，退回调查员重录");
        }
        List<String> bad = new ArrayList<>();
        if (!FlogApplyScope.isValid(dto.getApplyScope())) {
            bad.add("林地类别（商品林/公益林抚育/公益林更新/公益林低质低效林改造四选一）");
        }
        if (dto.getDesignDate() == null) {
            bad.add("调查设计那一日（取界只认这一天，不看今天）");
        }
        if (dto.getArea() == null || dto.getArea().compareTo(BigDecimal.ZERO) <= 0) {
            bad.add("小班面积（须为正数公顷）");
        }
        if (dto.getYieldRate() == null
                || dto.getYieldRate().compareTo(BigDecimal.ZERO) <= 0
                || dto.getYieldRate().compareTo(BigDecimal.ONE) > 0) {
            bad.add("出材率（0 与 1 之间的正数）");
        }
        if (dto.getTrees() == null || dto.getTrees().isEmpty()) {
            bad.add("株数为零：没有一株检尺数进不了链");
        } else {
            for (int i = 0; i < dto.getTrees().size(); i++) {
                FlogSurveyCalcDto.TreeMeasure t = dto.getTrees().get(i);
                int no = i + 1;
                if (t == null || t.getDbh() == null) {
                    bad.add("第 " + no + " 株胸径空着");
                    continue;
                }
                if (t.getHeight() == null) {
                    bad.add("第 " + no + " 株树高空着");
                    continue;
                }
                if (t.getDbh().compareTo(BigDecimal.ZERO) <= 0) {
                    bad.add("第 " + no + " 株胸径 " + t.getDbh() + " 是非正数，量回来的数来路不对");
                } else if (t.getDbh().compareTo(DBH_SENTINEL) >= 0) {
                    bad.add("第 " + no + " 株胸径 " + t.getDbh() + " 是“四位九”一类的哨兵数，来路不对");
                }
                if (t.getHeight().compareTo(BigDecimal.ZERO) <= 0) {
                    bad.add("第 " + no + " 株树高 " + t.getHeight() + " 是非正数，量回来的数来路不对");
                }
            }
        }
        if (!bad.isEmpty()) {
            throw new FlogBizException("检尺数不进链，退回调查员重录：" + String.join("；", bad));
        }
    }

    private String gradeName(int grade) {
        if (grade == GRADE_LIGHT) {
            return "轻度";
        }
        if (grade == GRADE_MEDIUM) {
            return "中度";
        }
        if (grade == GRADE_HEAVY) {
            return "重度";
        }
        return "不定档";
    }
}
