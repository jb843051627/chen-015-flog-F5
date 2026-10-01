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
import java.util.Date;

/**
 * 伐区勾销事务卡对象 t_flog_close_card
 *
 * 一小班一张活卡，四段挨着走：报伐(0)→核株(1)→验更(2)→了断(3)。
 * stage 是卡眼下停着、待过的那一道口，由勾销履历里没过的过口笔数算成，
 * 再用条件更新钉住；屏上摆的、经办人口头认的都不作数。
 *
 * @author fuce
 * @date 2026-10-01
 */
@TableName("t_flog_close_card")
@ApiModel(value = "TFlogCloseCard", description = "伐区勾销事务卡")
public class TFlogCloseCard implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 伐区勾销事务卡号 */
    @TableField("biz_no")
    @ApiModelProperty(value = "伐区勾销事务卡号")
    private String bizNo;

    /** 编制单位底档字号 */
    @TableField("site_no")
    @ApiModelProperty(value = "编制单位底档字号")
    private String siteNo;

    /** 小班号 */
    @TableField("subcomp_no")
    @ApiModelProperty(value = "小班号")
    private String subcompNo;

    /** 伐区名号 */
    @TableField("cut_block_no")
    @ApiModelProperty(value = "伐区名号")
    private String cutBlockNo;

    /** 钉过号的调查设计单id（报伐段认下，往后两段照它对） */
    @TableField("design_id")
    @ApiModelProperty(value = "钉过号的调查设计单id")
    private Long designId;

    /** 钉过号的调查设计单号 */
    @TableField("design_no")
    @ApiModelProperty(value = "钉过号的调查设计单号")
    private String designNo;

    /** 挂上的现行采伐建档卡id（了断摘它、退它占着的额） */
    @TableField("apply_card_id")
    @ApiModelProperty(value = "挂上的现行采伐建档卡id")
    private Long applyCardId;

    /** 挂上的采伐建档卡号 */
    @TableField("bill_no")
    @ApiModelProperty(value = "挂上的采伐建档卡号")
    private String billNo;

    /** 当前停着待过的道口 0报伐 1核株 2验更 3了断 */
    @TableField("stage")
    @ApiModelProperty(value = "当前停着待过的道口 0报伐 1核株 2验更 3了断")
    private Integer stage;

    /** 卡的落定 1在办 2止步 3已了断 */
    @TableField("status")
    @ApiModelProperty(value = "卡的落定 1在办 2止步 3已了断")
    private Integer status;

    /** 止步缘故（写“这一段不过了”那一刻落此） */
    @TableField("halt_reason")
    @ApiModelProperty(value = "止步缘故")
    private String haltReason;

    /** 了断经手人 */
    @TableField("closed_by")
    @ApiModelProperty(value = "了断经手人")
    private String closedBy;

    /** 了断时刻 */
    @TableField("closed_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "了断时刻")
    private Date closedTime;

    /** 活卡锚：在办=底档字号#小班号，止步/了断置空；唯一索引保证一小班至多一张活卡 */
    @TableField("active_anchor")
    @ApiModelProperty(value = "活卡锚（在办时有值，唯一）")
    private String activeAnchor;

    /** 删除标记 0正常（勾销卡不许抽走，服务层永不置1） */
    @TableField("del_flag")
    @ApiModelProperty(value = "删除标记 0正常")
    private Integer delFlag;

    /** 起卡人 */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    @ApiModelProperty(value = "起卡人")
    private String createBy;

    /** 起卡时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "起卡时间")
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

    /** 备注（不是勾销履历，过口记事一律走履历） */
    @TableField("remark")
    @ApiModelProperty(value = "备注")
    private String remark;

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

    public Long getDesignId() {
        return designId;
    }

    public void setDesignId(Long designId) {
        this.designId = designId;
    }

    public String getDesignNo() {
        return designNo;
    }

    public void setDesignNo(String designNo) {
        this.designNo = designNo;
    }

    public Long getApplyCardId() {
        return applyCardId;
    }

    public void setApplyCardId(Long applyCardId) {
        this.applyCardId = applyCardId;
    }

    public String getBillNo() {
        return billNo;
    }

    public void setBillNo(String billNo) {
        this.billNo = billNo;
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

    public String getActiveAnchor() {
        return activeAnchor;
    }

    public void setActiveAnchor(String activeAnchor) {
        this.activeAnchor = activeAnchor;
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
