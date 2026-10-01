package com.fc.v2.model.auto;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 伐区勾销履历行对象 t_flog_close_stage
 *
 * 卡上一段一段存下来的东西全在这：过一段、退一段、止一步都只许续一行，
 * 旧行不改不删。同段第二遍只认头一遍（active_flag=1 只留头一道）；退回重做时
 * 先前那段落过的名目尽数沉底（active_flag=0），沉掉的与后补的不摆在一处。
 * 当前停在哪段，数 active 的过成行算出来——不看屏、不看经办人嘴上认的。
 *
 * @author fuce
 * @date 2026-10-01
 */
@TableName("t_flog_close_stage")
@ApiModel(value = "TFlogCloseStage", description = "伐区勾销履历行")
public class TFlogCloseStage implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 过口动作：过成 */
    public static final String ACTION_PASS = "PASS";
    /** 过口动作：止步（这一段不过了，卡封死） */
    public static final String ACTION_FAIL = "FAIL";
    /** 过口动作：退回（沉底本段，回上一段重做） */
    public static final String ACTION_RETREAT = "RETREAT";

    /** 算数：当前这道 */
    public static final int ACTIVE_YES = 1;
    /** 算数：已沉底 */
    public static final int ACTIVE_NO = 0;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 勾销事务卡id */
    @TableField("card_id")
    @ApiModelProperty(value = "勾销事务卡id")
    private Long cardId;

    /** 勾销事务卡号（冗作回查） */
    @TableField("biz_no")
    @ApiModelProperty(value = "勾销事务卡号")
    private String bizNo;

    /** 段次 0报伐 1核株 2验更 3了断 */
    @TableField("stage")
    @ApiModelProperty(value = "段次 0报伐 1核株 2验更 3了断")
    private Integer stage;

    /** 本段第几轮（退回重做一轮，旧轮沉底、新轮另起） */
    @TableField("attempt_no")
    @ApiModelProperty(value = "本段第几轮")
    private Integer attemptNo;

    /** 过口动作 PASS过成 FAIL止步 RETREAT退回 */
    @TableField("action")
    @ApiModelProperty(value = "过口动作")
    private String action;

    /** 算不算数 1当前这道 0已沉底 */
    @TableField("active_flag")
    @ApiModelProperty(value = "算不算数 1当前这道 0已沉底")
    private Integer activeFlag;

    /** 本报伐钉的设计单号 */
    @TableField("design_no")
    @ApiModelProperty(value = "本报伐钉的设计单号")
    private String designNo;

    /** 本报伐实采株数 */
    @TableField("actual_trees")
    @ApiModelProperty(value = "本报伐实采株数")
    private Integer actualTrees;

    /** 本报伐实采蓄积 */
    @TableField("actual_volume")
    @ApiModelProperty(value = "本报伐实采蓄积")
    private BigDecimal actualVolume;

    /** 设计株数快照 */
    @TableField("design_trees")
    @ApiModelProperty(value = "设计株数快照")
    private Integer designTrees;

    /** 设计蓄积快照 */
    @TableField("design_volume")
    @ApiModelProperty(value = "设计蓄积快照")
    private BigDecimal designVolume;

    /** 实采与设计差额快照 */
    @TableField("diff_value")
    @ApiModelProperty(value = "实采与设计差额快照")
    private BigDecimal diffValue;

    /** 本验更更新面积 */
    @TableField("renew_area")
    @ApiModelProperty(value = "本验更更新面积")
    private BigDecimal renewArea;

    /** 本验更成活率 */
    @TableField("survive_rate")
    @ApiModelProperty(value = "本验更成活率")
    private BigDecimal surviveRate;

    /** 了断退回额度(立方米) */
    @TableField("refund_qty")
    @ApiModelProperty(value = "了断退回额度(立方米)")
    private BigDecimal refundQty;

    /** 本段一句话记事（说法/止步缘故，落头一遍，不许追第二句） */
    @TableField("note")
    @ApiModelProperty(value = "本段一句话记事")
    private String note;

    /** 过口经办人 */
    @TableField("op_by")
    @ApiModelProperty(value = "过口经办人")
    private String opBy;

    /** 过口时刻 */
    @TableField("op_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "过口时刻")
    private Date opTime;

    /** 删除标记（履历行永不删，此格恒0） */
    @TableField("del_flag")
    @ApiModelProperty(value = "删除标记（恒0）")
    private Integer delFlag;

    /** 创建者 */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    @ApiModelProperty(value = "创建者")
    private String createBy;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    /** 更新者 */
    @TableField(value = "update_by", fill = FieldFill.UPDATE)
    @ApiModelProperty(value = "更新者")
    private String updateBy;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "更新时间")
    private Date updateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCardId() {
        return cardId;
    }

    public void setCardId(Long cardId) {
        this.cardId = cardId;
    }

    public String getBizNo() {
        return bizNo;
    }

    public void setBizNo(String bizNo) {
        this.bizNo = bizNo;
    }

    public Integer getStage() {
        return stage;
    }

    public void setStage(Integer stage) {
        this.stage = stage;
    }

    public Integer getAttemptNo() {
        return attemptNo;
    }

    public void setAttemptNo(Integer attemptNo) {
        this.attemptNo = attemptNo;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Integer getActiveFlag() {
        return activeFlag;
    }

    public void setActiveFlag(Integer activeFlag) {
        this.activeFlag = activeFlag;
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

    public Integer getDesignTrees() {
        return designTrees;
    }

    public void setDesignTrees(Integer designTrees) {
        this.designTrees = designTrees;
    }

    public BigDecimal getDesignVolume() {
        return designVolume;
    }

    public void setDesignVolume(BigDecimal designVolume) {
        this.designVolume = designVolume;
    }

    public BigDecimal getDiffValue() {
        return diffValue;
    }

    public void setDiffValue(BigDecimal diffValue) {
        this.diffValue = diffValue;
    }

    public BigDecimal getRenewArea() {
        return renewArea;
    }

    public void setRenewArea(BigDecimal renewArea) {
        this.renewArea = renewArea;
    }

    public BigDecimal getSurviveRate() {
        return surviveRate;
    }

    public void setSurviveRate(BigDecimal surviveRate) {
        this.surviveRate = surviveRate;
    }

    public BigDecimal getRefundQty() {
        return refundQty;
    }

    public void setRefundQty(BigDecimal refundQty) {
        this.refundQty = refundQty;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getOpBy() {
        return opBy;
    }

    public void setOpBy(String opBy) {
        this.opBy = opBy;
    }

    public Date getOpTime() {
        return opTime;
    }

    public void setOpTime(Date opTime) {
        this.opTime = opTime;
    }

    public Integer getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(Integer delFlag) {
        this.delFlag = delFlag;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}
