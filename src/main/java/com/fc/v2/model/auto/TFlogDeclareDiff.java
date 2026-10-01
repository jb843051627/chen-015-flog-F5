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
 * 伐区申报清册差异清单对象 t_flog_declare_diff
 *
 * 同一批次号重送不覆盖旧账，蓄积真要改走差异清单这一路；
 * 已落账行不接第二遍数，只在差异单办结时按批准的新数改写。
 *
 * @author fuce
 * @date 2026-09-29
 */
@TableName("t_flog_declare_diff")
@ApiModel(value = "TFlogDeclareDiff", description = "伐区申报清册差异清单")
public class TFlogDeclareDiff implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 批次号 */
    @TableField("batch_no")
    @ApiModelProperty(value = "批次号")
    private String batchNo;

    /** 编制单位底档字号 */
    @TableField("site_no")
    @ApiModelProperty(value = "编制单位底档字号")
    private String siteNo;

    /** 小班号 */
    @TableField("subcomp_no")
    @ApiModelProperty(value = "小班号")
    private String subcompNo;

    /** 原清册行次 */
    @TableField("row_no")
    @ApiModelProperty(value = "原清册行次")
    private Integer rowNo;

    /** 差异种类 1蓄积调整 2其它更正 */
    @TableField("diff_kind")
    @ApiModelProperty(value = "差异种类 1蓄积调整 2其它更正")
    private Integer diffKind;

    /** 原申报/原核收蓄积 */
    @TableField("old_qty")
    @ApiModelProperty(value = "原申报/原核收蓄积")
    private BigDecimal oldQty;

    /** 申请改成的蓄积 */
    @TableField("new_qty")
    @ApiModelProperty(value = "申请改成的蓄积")
    private BigDecimal newQty;

    /** 办理情形 0待办 1已入帐 2驳回 */
    @TableField("status")
    @ApiModelProperty(value = "办理情形 0待办 1已入帐 2驳回")
    private Integer status;

    /** 办理人 */
    @TableField("handle_by")
    @ApiModelProperty(value = "办理人")
    private String handleBy;

    /** 办理时刻 */
    @TableField("handle_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "办理时刻")
    private Date handleTime;

    /** 删除标记 0正常 1删除 */
    @TableField("del_flag")
    @ApiModelProperty(value = "删除标记 0正常 1删除")
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

    public Integer getRowNo() {
        return rowNo;
    }

    public void setRowNo(Integer rowNo) {
        this.rowNo = rowNo;
    }

    public Integer getDiffKind() {
        return diffKind;
    }

    public void setDiffKind(Integer diffKind) {
        this.diffKind = diffKind;
    }

    public BigDecimal getOldQty() {
        return oldQty;
    }

    public void setOldQty(BigDecimal oldQty) {
        this.oldQty = oldQty;
    }

    public BigDecimal getNewQty() {
        return newQty;
    }

    public void setNewQty(BigDecimal newQty) {
        this.newQty = newQty;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getHandleBy() {
        return handleBy;
    }

    public void setHandleBy(String handleBy) {
        this.handleBy = handleBy;
    }

    public Date getHandleTime() {
        return handleTime;
    }

    public void setHandleTime(Date handleTime) {
        this.handleTime = handleTime;
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
