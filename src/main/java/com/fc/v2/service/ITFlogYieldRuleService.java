package com.fc.v2.service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import com.fc.v2.model.auto.TFlogVolumeTable;
import com.fc.v2.model.auto.TFlogYieldRule;
import com.fc.v2.model.custom.flog.FlogSurveyCalcDto;
import com.fc.v2.model.custom.flog.FlogSurveyCalcVo;

/**
 * 可采强度界限值线 Service接口（rule-eval 形状：规则求值，无增删改）。
 *
 * 取界只走这一处：看调查设计那一日（不看今天），旧版让位了仍凭日子请回来；
 * 界取哪一版、并线取哪一条、材积取哪一版，全在 {@link #evaluate(FlogSurveyCalcDto)} 现算现给。
 *
 * @author fuce
 * @date 2026-09-14
 */
public interface ITFlogYieldRuleService {

    /** 按主键查询规则（让位版也查得出，供回查来历） */
    TFlogYieldRule selectTFlogYieldRuleById(Long id);

    /**
     * 单规则求值：按 ruleCode 请出 at 时刻在场的那一版，判档（1轻 2中 3重）。
     * 无在场版本 / 输入越界 / 参数缺失一律返回 0。旧调用口一字不动，照旧可调。
     */
    int evaluate(String ruleCode, BigDecimal input, Date at);

    /**
     * 三级链求值（检尺两数→材积→每公顷出材量→定档），与按代号取线同一方法名下的重载。
     * 同一份检尺数，蓄积与强度评定只许取同一回计算：界版、材积表版、逐株材积都在这一份给回里。
     * 当日翻不出在场界或材积表版，返回里 basisMissing=true、写明无线可依，绝不拿邻近日子凑。
     */
    FlogSurveyCalcVo evaluate(FlogSurveyCalcDto dto);

    /** 多规则求值（不分林地类别）：at 时刻在场各线中序位最高者的档位；无在场线返回 0 */
    int evaluateTop(BigDecimal input, Date at);

    /** 多规则求值（指定林地类别）：该类别 at 时刻在场各线并线后说话那条的档位；无在场线返回 0 */
    int evaluateTop(BigDecimal input, Integer applyScope, Date at);

    /** at 时刻在场的全部线（序位降序、代号降序），让位版与撤旗线不取 */
    List<TFlogYieldRule> listAvailable(Date at);

    /**
     * 界册全版只读列表（含让位版，供翻来历）：按代号/类别/情形过滤，类别/情形空着不过滤。
     * 这是给界册管理页的回查口，不参与现行求值。
     */
    List<TFlogYieldRule> listAllVersions(String ruleCode, Integer applyScope, Integer status);

    /** 指定林地类别 at 时刻在场的线（序位降序、代号降序） */
    List<TFlogYieldRule> listAvailable(Integer applyScope, Date at);

    /**
     * 并线现算：请出指定林地类别 at 时刻唯一说话的那条界。
     * 两条同时靠上的序位大的说话；序位也分不出高下，认代号排在后头那条。翻不出返回 null。
     */
    TFlogYieldRule pickRule(Integer applyScope, Date at);

    /** 请出 at 时刻在场的材积表版（同一套并线口径：序位降序、代号降序）；翻不出返回 null */
    TFlogVolumeTable pickVolumeTable(Date at);

    /** 单条规则在 at 时刻是否在场可用（批量导入/报表等旁路复用，口径须与定位/列表一致） */
    boolean usable(Long id, Date at);

    /** at 时刻在场的线条数（不分林地类别，列表页角标 / 看板） */
    int countAvailable(Date at);

    /** 指定林地类别 at 时刻在场的线条数 */
    int countAvailable(Integer applyScope, Date at);
}
