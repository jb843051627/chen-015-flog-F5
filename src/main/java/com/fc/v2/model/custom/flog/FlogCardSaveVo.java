package com.fc.v2.model.custom.flog;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 建档卡保存（立卡/改版/核对/压死）给回。
 * 屏上落的字必须与这一份对得上：顺位、核验串、划走多少立方全取自此处。
 *
 * @author fuce
 * @date 2026-09-29
 */
public class FlogCardSaveVo implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 建档卡号（自动生成） */
    private String billNo;

    /** 版本序 */
    private Integer nodeNo;

    /** 底档字号 */
    private String siteNo;

    /** 小班号 */
    private String subcompNo;

    /** 采伐方式 */
    private Integer cutMode;

    /** 拟采蓄积 */
    private BigDecimal qty;

    /** 系统折回的取用顺位 1轻度 2中度 3重度 */
    private Integer gradeLevel;

    /** 进展 0待核对 1已核对(成形) 2已压死 */
    private Integer status;

    /** 核验串（成形后钉版，随版本序走） */
    private String content;

    /** 立卡人（钉住不挪） */
    private String registerBy;

    /** 立卡时刻 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date registerTime;

    /** 本笔办理人 */
    private String opBy;

    /** 本笔之后该卡占着的额度(立方米) */
    private BigDecimal occupyQty;

    /** 本笔从本年额度划走(+)或退回(-)的立方数 */
    private BigDecimal deltaQty;

    /** 该单位本年可占总数 */
    private BigDecimal yearTotalQty;

    /** 已扣合计（与尚存同一回计算） */
    private BigDecimal occupiedQty;

    /** 尚存额度（与已扣同一回计算） */
    private BigDecimal remainQty;

    /** 给屏上的话 */
    private String message;

    public String getBillNo() {
        return billNo;
    }

    public void setBillNo(String billNo) {
        this.billNo = billNo;
    }

    public Integer getNodeNo() {
        return nodeNo;
    }

    public void setNodeNo(Integer nodeNo) {
        this.nodeNo = nodeNo;
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

    public Integer getGradeLevel() {
        return gradeLevel;
    }

    public void setGradeLevel(Integer gradeLevel) {
        this.gradeLevel = gradeLevel;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getRegisterBy() {
        return registerBy;
    }

    public void setRegisterBy(String registerBy) {
        this.registerBy = registerBy;
    }

    public Date getRegisterTime() {
        return registerTime;
    }

    public void setRegisterTime(Date registerTime) {
        this.registerTime = registerTime;
    }

    public String getOpBy() {
        return opBy;
    }

    public void setOpBy(String opBy) {
        this.opBy = opBy;
    }

    public BigDecimal getOccupyQty() {
        return occupyQty;
    }

    public void setOccupyQty(BigDecimal occupyQty) {
        this.occupyQty = occupyQty;
    }

    public BigDecimal getDeltaQty() {
        return deltaQty;
    }

    public void setDeltaQty(BigDecimal deltaQty) {
        this.deltaQty = deltaQty;
    }

    public BigDecimal getYearTotalQty() {
        return yearTotalQty;
    }

    public void setYearTotalQty(BigDecimal yearTotalQty) {
        this.yearTotalQty = yearTotalQty;
    }

    public BigDecimal getOccupiedQty() {
        return occupiedQty;
    }

    public void setOccupiedQty(BigDecimal occupiedQty) {
        this.occupiedQty = occupiedQty;
    }

    public BigDecimal getRemainQty() {
        return remainQty;
    }

    public void setRemainQty(BigDecimal remainQty) {
        this.remainQty = remainQty;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
