package com.fc.v2.model.custom.flog;

import java.math.BigDecimal;

/**
 * 勾销事务卡·报伐立卡送法。
 *
 * 开卡与报伐是同一口：没有“先开一张空卡、再就地补一句”的第二条道。
 * 把设计单上钉过的号、小班号、实采株数、实采蓄积一次报齐，关口过了卡才立得起来，
 * 立起来便已停在核株段。
 *
 * @author fuce
 * @date 2026-10-01
 */
public class FlogCloseOpenDto {

    /** 编制单位底档字号 */
    private String siteNo;

    /** 小班号 */
    private String subcompNo;

    /** 伐区名号（可空） */
    private String cutBlockNo;

    /** 调查设计单号（必须是钉过档结论的那张） */
    private String designNo;

    /** 实采株数 */
    private Integer actualTrees;

    /** 实采蓄积(立方米) */
    private BigDecimal actualVolume;

    /** 本站核株差格(立方米)，空着用站里默认格 */
    private BigDecimal diffTolerance;

    /** 备注 */
    private String remark;

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

    public String getCutBlockNo() {
        return cutBlockNo;
    }

    public void setCutBlockNo(String cutBlockNo) {
        this.cutBlockNo = cutBlockNo;
    }

    public String getDesignNo() {
        return designNo;
    }

    public void setDesignNo(String designNo) {
        this.designNo = designNo;
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

    public BigDecimal getDiffTolerance() {
        return diffTolerance;
    }

    public void setDiffTolerance(BigDecimal diffTolerance) {
        this.diffTolerance = diffTolerance;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
