package com.fc.v2.model.custom.flog;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 伐区调查三级链求值给回：界取哪一版、并线取哪一条、材积取哪一版，全在这一份里现算现给。
 *
 * 页面只许照这份摆：线代号与启用之日压在设计单上的值以本返回为准，
 * 不许页面自己凑一条界再摆一个结论。
 *
 * @author fuce
 * @date 2026-09-30
 */
public class FlogSurveyCalcVo implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 调查设计那一日（本回取界/取表所认的日子） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date designDate;

    /** 林地类别 */
    private Integer applyScope;

    /** 株数 */
    private Integer treeCount;

    /** 总蓄积(立方米，逐株累加) */
    private BigDecimal totalVolume;

    /** 每公顷出材量(立方米/公顷)=蓄积*出材率/面积 */
    private BigDecimal yieldPerHa;

    /** 强度档位 1轻 2中 3重；无线可依时为空 */
    private Integer gradeLevel;

    /** 是否无线（表）可依：当日在场的界/材积表翻不出，写明无线，不拿邻近日子凑 */
    private boolean basisMissing;

    /** 所依界代号 */
    private String ruleCode;

    /** 所依界线名 */
    private String ruleName;

    /** 所依那条界的启用之日 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date ruleEffStart;

    /** 所用材积表版代号 */
    private String volTableCode;

    /** 所用材积表版启用之日 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date volTableEffStart;

    /** 逐株材积(立方米)，与总蓄积同一回计算，株序对齐入参 */
    private List<BigDecimal> treeVolumes = new ArrayList<>();

    /** 给屏上/钉单的话（无线可依时写明缺哪一路依据） */
    private String message;

    public Date getDesignDate() {
        return designDate;
    }

    public void setDesignDate(Date designDate) {
        this.designDate = designDate;
    }

    public Integer getApplyScope() {
        return applyScope;
    }

    public void setApplyScope(Integer applyScope) {
        this.applyScope = applyScope;
    }

    public Integer getTreeCount() {
        return treeCount;
    }

    public void setTreeCount(Integer treeCount) {
        this.treeCount = treeCount;
    }

    public BigDecimal getTotalVolume() {
        return totalVolume;
    }

    public void setTotalVolume(BigDecimal totalVolume) {
        this.totalVolume = totalVolume;
    }

    public BigDecimal getYieldPerHa() {
        return yieldPerHa;
    }

    public void setYieldPerHa(BigDecimal yieldPerHa) {
        this.yieldPerHa = yieldPerHa;
    }

    public Integer getGradeLevel() {
        return gradeLevel;
    }

    public void setGradeLevel(Integer gradeLevel) {
        this.gradeLevel = gradeLevel;
    }

    public boolean isBasisMissing() {
        return basisMissing;
    }

    public void setBasisMissing(boolean basisMissing) {
        this.basisMissing = basisMissing;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public void setRuleCode(String ruleCode) {
        this.ruleCode = ruleCode;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public Date getRuleEffStart() {
        return ruleEffStart;
    }

    public void setRuleEffStart(Date ruleEffStart) {
        this.ruleEffStart = ruleEffStart;
    }

    public String getVolTableCode() {
        return volTableCode;
    }

    public void setVolTableCode(String volTableCode) {
        this.volTableCode = volTableCode;
    }

    public Date getVolTableEffStart() {
        return volTableEffStart;
    }

    public void setVolTableEffStart(Date volTableEffStart) {
        this.volTableEffStart = volTableEffStart;
    }

    public List<BigDecimal> getTreeVolumes() {
        return treeVolumes;
    }

    public void setTreeVolumes(List<BigDecimal> treeVolumes) {
        this.treeVolumes = treeVolumes;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
