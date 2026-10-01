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
 * 伐区申报清册批次头与回执对象 t_flog_declare_batch
 *
 * 一册一行；回执三个数（本册总行/收下/挂起）在逐行落账同一回计算里落此，
 * 界面自行清点的不算。重送只认头一遍。
 *
 * @author fuce
 * @date 2026-09-29
 */
@TableName("t_flog_declare_batch")
@ApiModel(value = "TFlogDeclareBatch", description = "伐区申报清册批次头与回执")
public class TFlogDeclareBatch implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 伐区申报清册批次号 */
    @TableField("batch_no")
    @ApiModelProperty(value = "伐区申报清册批次号")
    private String batchNo;

    /** 申报编制单位底档字号 */
    @TableField("site_no")
    @ApiModelProperty(value = "申报编制单位底档字号")
    private String siteNo;

    /** 进门执行期年度 */
    @TableField("period_year")
    @ApiModelProperty(value = "进门执行期年度")
    private Integer periodYear;

    /** 本册总计行数（原册行次口径，重复行也占行次） */
    @TableField("total_rows")
    @ApiModelProperty(value = "本册总计行数")
    private Integer totalRows;

    /** 收下几行（逐行落账同一回数出） */
    @TableField("accepted_rows")
    @ApiModelProperty(value = "收下几行")
    private Integer acceptedRows;

    /** 挂起几行（逐行落账同一回数出） */
    @TableField("suspended_rows")
    @ApiModelProperty(value = "挂起几行")
    private Integer suspendedRows;

    /** 已进门行次序（最后落定的行次，单行递入时续它） */
    @TableField("max_row_no")
    @ApiModelProperty(value = "已进门行次序")
    private Integer maxRowNo;

    /** 收册落法 0收册中 1核讫 2空册 3整册退回 */
    @TableField("recv_status")
    @ApiModelProperty(value = "收册落法 0收册中 1核讫 2空册 3整册退回")
    private Integer recvStatus;

    /** 收册回执时刻 */
    @TableField("recv_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "收册回执时刻")
    private Date recvTime;

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

    public Integer getPeriodYear() {
        return periodYear;
    }

    public void setPeriodYear(Integer periodYear) {
        this.periodYear = periodYear;
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

    public Integer getMaxRowNo() {
        return maxRowNo;
    }

    public void setMaxRowNo(Integer maxRowNo) {
        this.maxRowNo = maxRowNo;
    }

    public Integer getRecvStatus() {
        return recvStatus;
    }

    public void setRecvStatus(Integer recvStatus) {
        this.recvStatus = recvStatus;
    }

    public Date getRecvTime() {
        return recvTime;
    }

    public void setRecvTime(Date recvTime) {
        this.recvTime = recvTime;
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
