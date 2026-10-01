package com.fc.v2.model.custom.flog;

import java.math.BigDecimal;

/**
 * 建档卡保存报文（另一种送法）。
 * 编号（卡号、底档字号）、顺位、核验串、版本序都不在此收口，由业务层折算回填。
 *
 * @author fuce
 * @date 2026-09-29
 */
public class FlogCardSaveDto {

    /** 所属底档id（以档内字号为准核，名号不算数） */
    private Long siteId;

    /** 小班号 */
    private String subcompNo;

    /** 伐区名号 */
    private String cutBlockNo;

    /** 采伐方式 1皆伐 2渐伐 3择伐 4抚育采伐 5更新采伐 6低质低效林改造 */
    private Integer cutMode;

    /** 本卡合并后拟采蓄积(立方米) */
    private BigDecimal qty;

    /** 本卡新报拟采面积(公顷)；皆伐要的面积就是这一栏 */
    private BigDecimal fineAmt;

    /** 皆伐专属：株数 */
    private Integer clearTrees;

    /** 渐伐专属：分年次数 */
    private Integer gradYears;

    /** 渐伐专属：间隔年数 */
    private Integer gradInterval;

    /** 择伐专属：择伐强度(%) */
    private BigDecimal selectIntensity;

    /** 择伐专属：保留株数 */
    private Integer selectKeepTrees;

    /** 抚育采伐专属：抚育办法 */
    private String tendMethod;

    /** 抚育采伐专属：每公顷采出量(立方米/公顷) */
    private BigDecimal tendPerHa;

    /** 更新采伐专属：更新方式 */
    private String renewMode;

    /** 更新采伐专属：完成年度 */
    private Integer renewFinishYear;

    /** 低质低效林改造专属：目标树种 */
    private String reformTree;

    /** 重录源卡id：带了即按该现行卡升版（支持改挂别家档）；空着按字号+小班号认对 */
    private Long reviseId;

    /** 备注 */
    private String remark;

    public Long getSiteId() {
        return siteId;
    }

    public void setSiteId(Long siteId) {
        this.siteId = siteId;
    }

    public String getSubcompNo() {
        return subcompNo;
    }

    public void setSubcompNo(String subcompNo) {
        this.subcompNo = subcompNo;
    }

    public String getCutBlockNo() {
        return cutBlockNo;
    }

    public void setCutBlockNo(String cutBlockNo) {
        this.cutBlockNo = cutBlockNo;
    }

    public Integer getCutMode() {
        return cutMode;
    }

    public void setCutMode(Integer cutMode) {
        this.cutMode = cutMode;
    }

    public BigDecimal getQty() {
        return qty;
    }

    public void setQty(BigDecimal qty) {
        this.qty = qty;
    }

    public BigDecimal getFineAmt() {
        return fineAmt;
    }

    public void setFineAmt(BigDecimal fineAmt) {
        this.fineAmt = fineAmt;
    }

    public Integer getClearTrees() {
        return clearTrees;
    }

    public void setClearTrees(Integer clearTrees) {
        this.clearTrees = clearTrees;
    }

    public Integer getGradYears() {
        return gradYears;
    }

    public void setGradYears(Integer gradYears) {
        this.gradYears = gradYears;
    }

    public Integer getGradInterval() {
        return gradInterval;
    }

    public void setGradInterval(Integer gradInterval) {
        this.gradInterval = gradInterval;
    }

    public BigDecimal getSelectIntensity() {
        return selectIntensity;
    }

    public void setSelectIntensity(BigDecimal selectIntensity) {
        this.selectIntensity = selectIntensity;
    }

    public Integer getSelectKeepTrees() {
        return selectKeepTrees;
    }

    public void setSelectKeepTrees(Integer selectKeepTrees) {
        this.selectKeepTrees = selectKeepTrees;
    }

    public String getTendMethod() {
        return tendMethod;
    }

    public void setTendMethod(String tendMethod) {
        this.tendMethod = tendMethod;
    }

    public BigDecimal getTendPerHa() {
        return tendPerHa;
    }

    public void setTendPerHa(BigDecimal tendPerHa) {
        this.tendPerHa = tendPerHa;
    }

    public String getRenewMode() {
        return renewMode;
    }

    public void setRenewMode(String renewMode) {
        this.renewMode = renewMode;
    }

    public Integer getRenewFinishYear() {
        return renewFinishYear;
    }

    public void setRenewFinishYear(Integer renewFinishYear) {
        this.renewFinishYear = renewFinishYear;
    }

    public String getReformTree() {
        return reformTree;
    }

    public void setReformTree(String reformTree) {
        this.reformTree = reformTree;
    }

    public Long getReviseId() {
        return reviseId;
    }

    public void setReviseId(Long reviseId) {
        this.reviseId = reviseId;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
