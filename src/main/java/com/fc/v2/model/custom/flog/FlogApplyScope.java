package com.fc.v2.model.custom.flog;

/**
 * 界册管的林地类别：商品林自成一类；公益林只许抚育、更新、低质低效林改造三性质的采伐，
 * 三性质各管各的界，不许借错。
 *
 * 编码与 t_flog_yield_rule.apply_scope、t_flog_survey_design.apply_scope 逐格对齐。
 *
 * @author fuce
 * @date 2026-09-30
 */
public enum FlogApplyScope {

    /** 商品林 */
    COMMERCIAL(2, "商品林"),
    /** 公益林·抚育性质 */
    WELFARE_TENDING(11, "公益林·抚育"),
    /** 公益林·更新性质 */
    WELFARE_RENEWAL(12, "公益林·更新"),
    /** 公益林·低质低效林改造性质 */
    WELFARE_REFORM(13, "公益林·低质低效林改造");

    private final int code;
    private final String name;

    FlogApplyScope(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public int getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static FlogApplyScope of(Integer code) {
        if (code == null) {
            return null;
        }
        for (FlogApplyScope s : values()) {
            if (s.code == code) {
                return s;
            }
        }
        return null;
    }

    public static boolean isValid(Integer code) {
        return of(code) != null;
    }
}
