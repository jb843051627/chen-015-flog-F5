package com.fc.v2.model.custom.flog;

import java.math.BigDecimal;

/**
 * 采伐方式六类与其专属必填栏。
 * 方式一换，缺了便起不来的格子跟着换，用不上的空着。
 *
 * @author fuce
 * @date 2026-09-29
 */
public enum FlogCutMode {

    /** 主伐-皆伐：要面积(走通用栏新报拟采面积)与株数 */
    CLEAR(1, "皆伐"),
    /** 主伐-渐伐：要分年次数与间隔年数 */
    GRADUAL(2, "渐伐"),
    /** 主伐-择伐：要择伐强度与保留株数 */
    SELECTIVE(3, "择伐"),
    /** 抚育采伐：要抚育办法与每公顷采出量 */
    TENDING(4, "抚育采伐"),
    /** 更新采伐：要更新方式与完成年度 */
    RENEWAL(5, "更新采伐"),
    /** 低质低效林改造：要目标树种 */
    REFORM(6, "低质低效林改造");

    private final int code;
    private final String name;

    FlogCutMode(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public int getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static FlogCutMode of(Integer code) {
        if (code == null) {
            return null;
        }
        for (FlogCutMode m : values()) {
            if (m.code == code) {
                return m;
            }
        }
        return null;
    }

    public static boolean isValid(Integer code) {
        return of(code) != null;
    }

    /**
     * 正数判断：零、负数一律不认。
     */
    public static boolean positive(BigDecimal v) {
        return v != null && v.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * 正整数判断。
     */
    public static boolean positive(Integer v) {
        return v != null && v > 0;
    }
}
