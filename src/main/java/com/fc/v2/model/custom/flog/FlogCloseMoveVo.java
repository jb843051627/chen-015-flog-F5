package com.fc.v2.model.custom.flog;

import com.fc.v2.model.auto.TFlogCloseCard;

import java.math.BigDecimal;

/**
 * 勾销卡过口回执。
 *
 * 改没改成，只看推进/退回那一对方法给回的这一份：当前段次、卡的落定、
 * 本笔名目与退额数都由系统按履历算后盖章；屏上摆的、经办人嘴上认的都当不得真。
 *
 * @author fuce
 * @date 2026-10-01
 */
public class FlogCloseMoveVo {

    /** 卡id */
    private Long id;

    /** 勾销事务卡号 */
    private String bizNo;

    /** 底档字号 */
    private String siteNo;

    /** 小班号 */
    private String subcompNo;

    /** 系统按履历算出的当前段次 0报伐 1核株 2验更 3了断（=已过成段数） */
    private Integer stage;

    /** 卡的落定 1在办 2已了断 3止步 */
    private Integer status;

    /** 本笔过口动作 */
    private String action;

    /** 本笔名目（过成/止步/退回的一句话） */
    private String note;

    /** 本笔了断退回额度(立方米)；非了断为空 */
    private BigDecimal refundQty;

    /** 退额后该年度账已扣合计(立方米) */
    private BigDecimal occupiedQty;

    /** 该年度账尚存(立方米) */
    private BigDecimal remainQty;

    /** 给经办人的一句话回执 */
    private String message;

    public static FlogCloseMoveVo of(TFlogCloseCard card) {
        FlogCloseMoveVo vo = new FlogCloseMoveVo();
        vo.id = card.getId();
        vo.bizNo = card.getBizNo();
        vo.siteNo = card.getSiteNo();
        vo.subcompNo = card.getSubcompNo();
        vo.stage = card.getStage();
        vo.status = card.getStatus();
        return vo;
    }

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

    public Integer getStage() {
        return stage;
    }

    public void setStage(Integer stage) {
        this.stage = stage;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public BigDecimal getRefundQty() {
        return refundQty;
    }

    public void setRefundQty(BigDecimal refundQty) {
        this.refundQty = refundQty;
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
