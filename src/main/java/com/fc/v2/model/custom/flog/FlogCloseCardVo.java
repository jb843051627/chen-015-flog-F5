package com.fc.v2.model.custom.flog;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 勾销卡起卡/过口/回退/止步给回。
 * 卡此刻停在哪段，只看本回服务层算成的 stage 与 passedCount，屏上的、经办人心里认的都当不得真。
 *
 * @author fuce
 * @date 2026-10-01
 */
public class FlogCloseCardVo implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 勾销卡id */
    private Long id;

    /** 勾销卡号 */
    private String bizNo;

    /** 底档字号 */
    private String siteNo;

    /** 小班号 */
    private String subcompNo;

    /** 伐区名号 */
    private String cutBlockNo;

    /** 已过几段（系统照没过的过口履历算出，权威） */
    private Integer passedCount;

    /** 眼下停着待过的道口 0报伐 1核株 2验更 3了断；止步/了断后为停住的那一段 */
    private Integer stage;

    /** 停着的道口名 */
    private String stageName;

    /** 卡的落定 1在办 2止步 3已了断 */
    private Integer status;

    /** 落定名 */
    private String statusName;

    /** 钉过号的调查设计单号 */
    private String designNo;

    /** 挂上的采伐建档卡号 */
    private String billNo;

    /** 止步缘故 */
    private String haltReason;

    /** 了断经手人 */
    private String closedBy;

    /** 了断时刻 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date closedTime;

    /** 了断：退回前占着的额度(立方米) */
    private BigDecimal occupyQty;

    /** 了断：原样退回的额度(立方米) */
    private BigDecimal refundQty;

    /** 本回是不是重复过口（同一段第二遍，库里只认头一遍，不新落记事） */
    private Boolean duplicate;

    /** 给屏上的话 */
    private String message;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBizNo() {
        return bizNo;
    }

    public void setBizNo(String bizNo) {
        this.bizNo = bizNo;
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

    public String getCutBlockNo() {
        return cutBlockNo;
    }

    public void setCutBlockNo(String cutBlockNo) {
        this.cutBlockNo = cutBlockNo;
    }

    public Integer getPassedCount() {
        return passedCount;
    }

    public void setPassedCount(Integer passedCount) {
        this.passedCount = passedCount;
    }

    public Integer getStage() {
        return stage;
    }

    public void setStage(Integer stage) {
        this.stage = stage;
    }

    public String getStageName() {
        return stageName;
    }

    public void setStageName(String stageName) {
        this.stageName = stageName;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getStatusName() {
        return statusName;
    }

    public void setStatusName(String statusName) {
        this.statusName = statusName;
    }

    public String getDesignNo() {
        return designNo;
    }

    public void setDesignNo(String designNo) {
        this.designNo = designNo;
    }

    public String getBillNo() {
        return billNo;
    }

    public void setBillNo(String billNo) {
        this.billNo = billNo;
    }

    public String getHaltReason() {
        return haltReason;
    }

    public void setHaltReason(String haltReason) {
        this.haltReason = haltReason;
    }

    public String getClosedBy() {
        return closedBy;
    }

    public void setClosedBy(String closedBy) {
        this.closedBy = closedBy;
    }

    public Date getClosedTime() {
        return closedTime;
    }

    public void setClosedTime(Date closedTime) {
        this.closedTime = closedTime;
    }

    public BigDecimal getOccupyQty() {
        return occupyQty;
    }

    public void setOccupyQty(BigDecimal occupyQty) {
        this.occupyQty = occupyQty;
    }

    public BigDecimal getRefundQty() {
        return refundQty;
    }

    public void setRefundQty(BigDecimal refundQty) {
        this.refundQty = refundQty;
    }

    public Boolean getDuplicate() {
        return duplicate;
    }

    public void setDuplicate(Boolean duplicate) {
        this.duplicate = duplicate;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
