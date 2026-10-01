package com.fc.v2.model.custom.flog;

import java.math.BigDecimal;

/**
 * 勾销卡“往前推进一格”唯一入口的送法。
 *
 * 卡停在哪段，就只认那段该报的几道关口，别段的格子报了也不看：
 * 核株段只认真采两数与一句说法；验更段只认更新面积与成活。
 * 报伐段不在这里——开卡即报伐，见 {@link FlogCloseOpenDto}；
 * 了断段无关口可报（摘册、退额由系统办）。
 *
 * @author fuce
 * @date 2026-10-01
 */
public class FlogClosePassDto {

    /** 勾销事务卡id */
    private Long id;

    /** 核株：实采株数（山里回来再报一遍——第二遍库里只认头一遍） */
    private Integer actualTrees;

    /** 核株：实采蓄积(立方米) */
    private BigDecimal actualVolume;

    /** 核株：差额越过站里差格时必填的一句说法 */
    private String diffReason;

    /** 验更：更新造林面积(公顷，小于采伐面积当场点出) */
    private BigDecimal renewArea;

    /** 验更：更新成活率(%) */
    private BigDecimal surviveRate;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getActualTrees() {
        return actualTrees;
    }

    public void setActualTrees(Integer actualTrees) {
        this.actualTrees = actualTrees;
    }

    public BigDecimal getActualVolume() {
        return actualVolume;
    }

    public void setActualVolume(BigDecimal actualVolume) {
        this.actualVolume = actualVolume;
    }

    public String getDiffReason() {
        return diffReason;
    }

    public void setDiffReason(String diffReason) {
        this.diffReason = diffReason;
    }

    public BigDecimal getRenewArea() {
        return renewArea;
    }

    public void setRenewArea(BigDecimal renewArea) {
        this.renewArea = renewArea;
    }

    public BigDecimal getSurviveRate() {
        return surviveRate;
    }

    public void setSurviveRate(BigDecimal surviveRate) {
        this.surviveRate = surviveRate;
    }
}
