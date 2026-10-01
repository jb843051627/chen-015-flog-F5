package com.fc.v2.model.custom.flog;

import java.math.BigDecimal;

/**
 * 清册逐行递入报文（JSON 送法）。
 * 行次、落地情形、前置项代号、缘故、执行期、账页一律不由界面塞，
 * 进来由业务服务层逐行定夺。
 *
 * @author fuce
 * @date 2026-09-29
 */
public class FlogDeclareRowDto {

    /** 伐区申报清册批次号 */
    private String batchNo;

    /** 申报编制单位底档字号（一册只许一家） */
    private String siteNo;

    /** 小班号 */
    private String subcompNo;

    /** 采伐方式 1皆伐 2渐伐 3择伐 4抚育采伐 5更新采伐 6低质低效林改造 */
    private Integer cutMode;

    /** 林种性质 1公益林 2商品林 */
    private Integer forestKind;

    /** 权属是否在诉 0无 1有 */
    private Integer disputeFlag;

    /** 本行申报拟采面积(公顷) */
    private BigDecimal area;

    /** 附没附伐区调查设计材料 0未附 1已附 */
    private Integer surveyFlag;

    /** 本行申报拟采蓄积(立方米) */
    private BigDecimal qty;

    /** 本行实际核收蓄积(立方米)；不填按申报数收，不得大过申报 */
    private BigDecimal recvQty;

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

    public Integer getForestKind() {
        return forestKind;
    }

    public void setForestKind(Integer forestKind) {
        this.forestKind = forestKind;
    }

    public Integer getDisputeFlag() {
        return disputeFlag;
    }

    public void setDisputeFlag(Integer disputeFlag) {
        this.disputeFlag = disputeFlag;
    }

    public BigDecimal getArea() {
        return area;
    }

    public void setArea(BigDecimal area) {
        this.area = area;
    }

    public Integer getSurveyFlag() {
        return surveyFlag;
    }

    public void setSurveyFlag(Integer surveyFlag) {
        this.surveyFlag = surveyFlag;
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
}
