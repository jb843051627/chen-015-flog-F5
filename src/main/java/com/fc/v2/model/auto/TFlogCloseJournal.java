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
 * 伐区勾销履历流水对象 t_flog_close_journal
 *
 * 过口、止步、沉底一律只追加：不原地改、不删行。回退改口时旧那笔由一笔“沉底”事件沉掉，
 * 旧行原样留库，沉掉的与后补的不摆在一处。fp 按序首尾相扣，事后从了断往回数，
 * 缺页、凭空多出来、过后手动补上的，一处对不上这张卡就算没走成。
 *
 * @author fuce
 * @date 2026-10-01
 */
@TableName("t_flog_close_journal")
@ApiModel(value = "TFlogCloseJournal", description = "伐区勾销履历流水")
public class TFlogCloseJournal implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 勾销卡id */
    @TableField("card_id")
    @ApiModelProperty(value = "勾销卡id")
    private Long cardId;

    /** 勾销卡号 */
    @TableField("biz_no")
    @ApiModelProperty(value = "勾销卡号")
    private String bizNo;

    /** 卡内流水序（从1挨号，断号即缺页） */
    @TableField("seq")
    @ApiModelProperty(value = "卡内流水序")
    private Integer seq;

    /** 本段 0报伐 1核株 2验更 3了断 */
    @TableField("stage")
    @ApiModelProperty(value = "本段 0报伐 1核株 2验更 3了断")
    private Integer stage;

    /** 本段第几遍（头一遍1，回退重做挨号） */
    @TableField("attempt_no")
    @ApiModelProperty(value = "本段第几遍")
    private Integer attemptNo;

    /** 笔目 1过口 2止步 3沉底 */
    @TableField("action")
    @ApiModelProperty(value = "笔目 1过口 2止步 3沉底")
    private Integer action;

    /** 沉底事件专用：指被沉掉那笔过口的流水序 */
    @TableField("ref_seq")
    @ApiModelProperty(value = "被沉掉那笔过口的流水序")
    private Integer refSeq;

    /** 报伐：设计单上钉过的号 */
    @TableField("design_no")
    @ApiModelProperty(value = "报伐：设计单上钉过的号")
    private String designNo;

    /** 报伐：实采株数 */
    @TableField("actual_trees")
    @ApiModelProperty(value = "报伐：实采株数")
    private Integer actualTrees;

    /** 报伐：实采蓄积(立方米) */
    @TableField("actual_volume")
    @ApiModelProperty(value = "报伐：实采蓄积(立方米)")
    private BigDecimal actualVolume;

    /** 核株：设计株数（钉档那轮逐株账数出） */
    @TableField("design_trees")
    @ApiModelProperty(value = "核株：设计株数")
    private Integer designTrees;

    /** 核株：设计蓄积(立方米，钉档结论那笔) */
    @TableField("design_volume")
    @ApiModelProperty(value = "核株：设计蓄积(立方米)")
    private BigDecimal designVolume;

    /** 核株：株数相差比率(实采对设计) */
    @TableField("tree_diff_rate")
    @ApiModelProperty(value = "核株：株数相差比率")
    private BigDecimal treeDiffRate;

    /** 核株：蓄积相差比率(实采对设计) */
    @TableField("vol_diff_rate")
    @ApiModelProperty(value = "核株：蓄积相差比率")
    private BigDecimal volDiffRate;

    /** 核株：差过站里那格时的一句说法 */
    @TableField("diff_reason")
    @ApiModelProperty(value = "核株：差异说法")
    private String diffReason;

    /** 验更：采伐面积(公顷，照设计单钉住) */
    @TableField("cut_area")
    @ApiModelProperty(value = "验更：采伐面积(公顷)")
    private BigDecimal cutArea;

    /** 验更：更新造林面积(公顷，小于采伐面积当场点回) */
    @TableField("renew_area")
    @ApiModelProperty(value = "验更：更新造林面积(公顷)")
    private BigDecimal renewArea;

    /** 验更：更新造林成活率(%) */
    @TableField("renew_survival")
    @ApiModelProperty(value = "验更：更新造林成活率(%)")
    private BigDecimal renewSurvival;

    /** 了断：退回前该建档卡占着的额度(立方米) */
    @TableField("occupy_qty")
    @ApiModelProperty(value = "了断：退回前占用额度(立方米)")
    private BigDecimal occupyQty;

    /** 了断：原样退回的额度(立方米) */
    @TableField("refund_qty")
    @ApiModelProperty(value = "了断：退回额度(立方米)")
    private BigDecimal refundQty;

    /** 本笔经手人 */
    @TableField("op_by")
    @ApiModelProperty(value = "本笔经手人")
    private String opBy;

    /** 本笔落账时刻 */
    @TableField("op_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "本笔落账时刻")
    private Date opTime;

    /** 上一笔指纹（头一笔为空，链断即破） */
    @TableField("prev_fp")
    @ApiModelProperty(value = "上一笔指纹")
    private String prevFp;

    /** 本笔指纹（落账一刻钉死，改过任何一格都对不上） */
    @TableField("fp")
    @ApiModelProperty(value = "本笔指纹")
    private String fp;

    /** 删除标记 0正常（履历只追加，永不置1） */
    @TableField("del_flag")
    @ApiModelProperty(value = "删除标记 0正常")
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

    /** 备注 */
    @TableField("remark")
    @ApiModelProperty(value = "备注")
    private String remark;

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

    public Integer getSeq() {
        return seq;
    }

    public void setSeq(Integer seq) {
        this.seq = seq;
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

    public Integer getAction() {
        return action;
    }

    public void setAction(Integer action) {
        this.action = action;
    }

    public Integer getRefSeq() {
        return refSeq;
    }

    public void setRefSeq(Integer refSeq) {
        this.refSeq = refSeq;
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

    public BigDecimal getTreeDiffRate() {
        return treeDiffRate;
    }

    public void setTreeDiffRate(BigDecimal treeDiffRate) {
        this.treeDiffRate = treeDiffRate;
    }

    public BigDecimal getVolDiffRate() {
        return volDiffRate;
    }

    public void setVolDiffRate(BigDecimal volDiffRate) {
        this.volDiffRate = volDiffRate;
    }

    public String getDiffReason() {
        return diffReason;
    }

    public void setDiffReason(String diffReason) {
        this.diffReason = diffReason;
    }

    public BigDecimal getCutArea() {
        return cutArea;
    }

    public void setCutArea(BigDecimal cutArea) {
        this.cutArea = cutArea;
    }

    public BigDecimal getRenewArea() {
        return renewArea;
    }

    public void setRenewArea(BigDecimal renewArea) {
        this.renewArea = renewArea;
    }

    public BigDecimal getRenewSurvival() {
        return renewSurvival;
    }

    public void setRenewSurvival(BigDecimal renewSurvival) {
        this.renewSurvival = renewSurvival;
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

    public String getPrevFp() {
        return prevFp;
    }

    public void setPrevFp(String prevFp) {
        this.prevFp = prevFp;
    }

    public String getFp() {
        return fp;
    }

    public void setFp(String fp) {
        this.fp = fp;
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

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
