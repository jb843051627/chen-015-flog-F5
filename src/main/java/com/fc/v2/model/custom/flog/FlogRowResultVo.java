package com.fc.v2.model.custom.flog;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 一行落账给回。
 * 这一行算收下、算挂起还是并作一行，全照这一份；屏上不许另起一套数。
 *
 * @author fuce
 * @date 2026-09-29
 */
public class FlogRowResultVo implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 批次号 */
    private String batchNo;

    /** 原清册行次（进门顺序） */
    private Integer rowNo;

    /** 底档字号 */
    private String siteNo;

    /** 小班号 */
    private String subcompNo;

    /** 落地情形 0待核 1已收 2挂起 */
    private Integer status;

    /** 落地情形字面 */
    private String statusText;

    /** 前置项代号（挂起才有） */
    private String itemCode;

    /** 缘故一句话（挂起才有） */
    private String reason;

    /** 申报拟采蓄积 */
    private BigDecimal qty;

    /** 实际核收蓄积（并账行不落数） */
    private BigDecimal recvQty;

    /** 并进的头行行次；非并账行为空 */
    private Integer mergedIntoRowNo;

    /** 进门时刻 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    /** 给屏上的话 */
    private String message;

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public Integer getRowNo() {
        return rowNo;
    }

    public void setRowNo(Integer rowNo) {
        this.rowNo = rowNo;
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

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getStatusText() {
        return statusText;
    }

    public void setStatusText(String statusText) {
        this.statusText = statusText;
    }

    public String getItemCode() {
        return itemCode;
    }

    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public BigDecimal getQty() {
        return qty;
    }

    public void setQty(BigDecimal qty) {
        this.qty = qty;
    }

    public BigDecimal getRecvQty() {
        return recvQty;
    }

    public void setRecvQty(BigDecimal recvQty) {
        this.recvQty = recvQty;
    }

    public Integer getMergedIntoRowNo() {
        return mergedIntoRowNo;
    }

    public void setMergedIntoRowNo(Integer mergedIntoRowNo) {
        this.mergedIntoRowNo = mergedIntoRowNo;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
