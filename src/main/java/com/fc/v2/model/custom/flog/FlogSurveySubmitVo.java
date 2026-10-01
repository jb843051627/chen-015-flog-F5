package com.fc.v2.model.custom.flog;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 提交/复验给回：单头与本轮钉死的结论各摆一份，另给总蓄积/每公顷出材量等计算明细。
 * 屏上要压的代号、启用日、档位全取自这里，不许页面自己凑一条界再摆结论。
 *
 * @author fuce
 * @date 2026-09-30
 */
public class FlogSurveySubmitVo implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 调查设计单id */
    private Long designId;

    /** 单号 */
    private String designNo;

    /** 字号 */
    private String siteNo;

    /** 小班 */
    private String subcompNo;

    /** 林地类别 */
    private Integer applyScope;

    /** 调查设计那一日 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date designDate;

    /** 本轮轮次 */
    private Integer roundNo;

    /** 是否复验 0首评 1整改复验 */
    private Integer recheckFlag;

    /** 本回总蓄积(立方米) */
    private BigDecimal totalVolume;

    /** 本回每公顷出材量(立方米/公顷) */
    private BigDecimal yieldPerHa;

    /** 强度档位 1轻 2中 3重；无线可依时为空 */
    private Integer gradeLevel;

    /** 是否无线可依：当日在场界/材积表翻不出，写明无线，不拿邻近日子凑 */
    private boolean basisMissing;

    /** 所依界代号 */
    private String ruleCode;

    /** 所依界启用之日 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date ruleEffStart;

    /** 所用材积表版代号 */
    private String volTableCode;

    /** 所用材积表版启用之日 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date volTableEffStart;

    /** 给屏上的话 */
    private String message;

    public Long getDesignId() {
        return designId;
    }

    public void setDesignId(Long designId) {
        this.designId = designId;
    }

    public String getDesignNo() {
        return designNo;
    }

    public void setDesignNo(String designNo) {
        this.designNo = designNo;
    }

    public String getSiteNo() {
        return siteNo;
    }

    public void setSiteNo(String siteNo) {
        this.siteNo = siteNo;
    }

    public String getSubcompNo() {
        return subcompNo;
    }

    public void setSubcompNo(String subcompNo) {
        this.subcompNo = subcompNo;
    }

    public Integer getApplyScope() {
        return applyScope;
    }

    public void setApplyScope(Integer applyScope) {
        this.applyScope = applyScope;
    }

    public Date getDesignDate() {
        return designDate;
    }

    public void setDesignDate(Date designDate) {
        this.designDate = designDate;
    }

    public Integer getRoundNo() {
        return roundNo;
    }

    public void setRoundNo(Integer roundNo) {
        this.roundNo = roundNo;
    }

    public Integer getRecheckFlag() {
        return recheckFlag;
    }

    public void setRecheckFlag(Integer recheckFlag) {
        this.recheckFlag = recheckFlag;
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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
