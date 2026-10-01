package com.fc.v2.model.custom.flog;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * 清册回执。
 * 本册总计几行、收下几行、挂起几行——三个数只准由逐行落账那同一次计算端出，
 * 且与库里逐行重数的一把尺对平（挂起 + 收下 = 原册行数）后才落回执；界面自点的不算。
 *
 * @author fuce
 * @date 2026-09-29
 */
public class FlogReceiptVo implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 批次号 */
    private String batchNo;

    /** 底档字号 */
    private String siteNo;

    /** 进门执行期年度 */
    private Integer periodYear;

    /** 收册落法 0收册中 1核讫 2空册 3整册退回 */
    private Integer recvStatus;

    /** 收册落法字面 */
    private String recvStatusText;

    /** 本册总计行数（原册行次口径，重复行也占行次） */
    private Integer totalRows;

    /** 收下几行 */
    private Integer acceptedRows;

    /** 挂起几行 */
    private Integer suspendedRows;

    /** 收册回执时刻 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date recvTime;

    /** 给屏上的话（空册只写“空册”，绝不写“核讫”） */
    private String message;

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public String getSiteNo() {
        return siteNo;
    }

    public void setSiteNo(String siteNo) {
        this.siteNo = siteNo;
    }

    public Integer getPeriodYear() {
        return periodYear;
    }

    public void setPeriodYear(Integer periodYear) {
        this.periodYear = periodYear;
    }

    public Integer getRecvStatus() {
        return recvStatus;
    }

    public void setRecvStatus(Integer recvStatus) {
        this.recvStatus = recvStatus;
    }

    public String getRecvStatusText() {
        return recvStatusText;
    }

    public void setRecvStatusText(String recvStatusText) {
        this.recvStatusText = recvStatusText;
    }

    public Integer getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(Integer totalRows) {
        this.totalRows = totalRows;
    }

    public Integer getAcceptedRows() {
        return acceptedRows;
    }

    public void setAcceptedRows(Integer acceptedRows) {
        this.acceptedRows = acceptedRows;
    }

    public Integer getSuspendedRows() {
        return suspendedRows;
    }

    public void setSuspendedRows(Integer suspendedRows) {
        this.suspendedRows = suspendedRows;
    }

    public Date getRecvTime() {
        return recvTime;
    }

    public void setRecvTime(Date recvTime) {
        this.recvTime = recvTime;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
