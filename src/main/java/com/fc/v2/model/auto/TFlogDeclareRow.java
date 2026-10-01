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
 * 伐区申报清册核收行对象 t_flog_declare_row
 *
 * @author fuce
 * @date 2026-09-12
 */
@TableName("t_flog_declare_row")
@ApiModel(value = "TFlogDeclareRow", description = "伐区申报清册核收行")
public class TFlogDeclareRow implements Serializable {
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

    /** 原清册行次（进门顺序钉死，不许挪册） */
    @TableField("row_no")
    @ApiModelProperty(value = "原清册行次")
    private Integer rowNo;

    /** 申报编制单位底档字号 */
    @TableField("site_no")
    @ApiModelProperty(value = "申报编制单位底档字号")
    private String siteNo;

    /** 小班号 */
    @TableField("subcomp_no")
    @ApiModelProperty(value = "小班号")
    private String subcompNo;

    /** 采伐方式 1皆伐 2渐伐 3择伐 4抚育采伐 5更新采伐 6低质低效林改造 */
    @TableField("cut_mode")
    @ApiModelProperty(value = "采伐方式")
    private Integer cutMode;

    /** 林种性质 1公益林 2商品林 */
    @TableField("forest_kind")
    @ApiModelProperty(value = "林种性质 1公益林 2商品林")
    private Integer forestKind;

    /** 权属是否在诉 0无 1有 */
    @TableField("dispute_flag")
    @ApiModelProperty(value = "权属是否在诉 0无 1有")
    private Integer disputeFlag;

    /** 本行申报拟采面积(公顷) */
    @TableField("area")
    @ApiModelProperty(value = "本行申报拟采面积(公顷)")
    private BigDecimal area;

    /** 附没附伐区调查设计材料 0未附 1已附 */
    @TableField("survey_flag")
    @ApiModelProperty(value = "附没附伐区调查设计材料 0未附 1已附")
    private Integer surveyFlag;

    /** 被打回的前置项代号 */
    @TableField("item_code")
    @ApiModelProperty(value = "被打回的前置项代号")
    private String itemCode;

    /** 本行申报拟采蓄积(立方米) */
    @TableField("qty")
    @ApiModelProperty(value = "本行申报拟采蓄积(立方米)")
    private BigDecimal qty;

    /** 本行实际核收蓄积(立方米，不得大过申报) */
    @TableField("recv_qty")
    @ApiModelProperty(value = "本行实际核收蓄积(立方米)")
    private BigDecimal recvQty;

    /** 归属执行期年度（进门那年） */
    @TableField("period_year")
    @ApiModelProperty(value = "归属执行期年度")
    private Integer periodYear;

    /** 账页 0现行页 1往期页（收满两个执行期转往期） */
    @TableField("page_flag")
    @ApiModelProperty(value = "账页 0现行页 1往期页")
    private Integer pageFlag;

    /** 重复行并账：本行并进的头行行次（头行本格为空） */
    @TableField("merge_of_row_no")
    @ApiModelProperty(value = "本行并进的头行行次")
    private Integer mergeOfRowNo;

    /** 行落地情形 0待核 1已收 2挂起 */
    @TableField("status")
    @ApiModelProperty(value = "行落地情形 0待核 1已收 2挂起")
    private Integer status;

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

    /** 备注（挂起缘故一句话落此） */
    @TableField("remark")
    @ApiModelProperty(value = "备注（挂起缘故）")
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

    public Integer getRowNo() {
        return rowNo;
    }

    public void setRowNo(Integer rowNo) {
        this.rowNo = rowNo;
    }

    public String getItemCode() {
        return itemCode;
    }

    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }

    public BigDecimal getQty() {
        return qty;
    }

    public void setQty(BigDecimal qty) {
        this.qty = qty;
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

    public BigDecimal getRecvQty() {
        return recvQty;
    }

    public void setRecvQty(BigDecimal recvQty) {
        this.recvQty = recvQty;
    }

    public Integer getPeriodYear() {
        return periodYear;
    }

    public void setPeriodYear(Integer periodYear) {
        this.periodYear = periodYear;
    }

    public Integer getPageFlag() {
        return pageFlag;
    }

    public void setPageFlag(Integer pageFlag) {
        this.pageFlag = pageFlag;
    }

    public Integer getMergeOfRowNo() {
        return mergeOfRowNo;
    }

    public void setMergeOfRowNo(Integer mergeOfRowNo) {
        this.mergeOfRowNo = mergeOfRowNo;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
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
