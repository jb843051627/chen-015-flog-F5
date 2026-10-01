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
 * 二元立木材积表版本册对象 t_flog_volume_table
 *
 * 材积式 V = paramA * dbh^expD * height^expH。换版只准添新版、给旧版填交棒日，
 * 从检尺到出材量的同一回三级计算只认同一版。
 *
 * @author fuce
 * @date 2026-09-30
 */
@TableName("t_flog_volume_table")
@ApiModel(value = "TFlogVolumeTable", description = "二元立木材积表版本册")
public class TFlogVolumeTable implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 材积表代号 */
    @TableField("table_code")
    @ApiModelProperty(value = "材积表代号")
    private String tableCode;

    /** 材积表名 */
    @TableField("table_name")
    @ApiModelProperty(value = "材积表名")
    private String tableName;

    /** 材积式系数 a（V=a*D^b*H^c） */
    @TableField("param_a")
    @ApiModelProperty(value = "材积式系数 a")
    private BigDecimal paramA;

    /** 胸径指数 b */
    @TableField("exp_d")
    @ApiModelProperty(value = "胸径指数 b")
    private BigDecimal expD;

    /** 树高指数 c */
    @TableField("exp_h")
    @ApiModelProperty(value = "树高指数 c")
    private BigDecimal expH;

    /** 启用之日（含） */
    @TableField("eff_start")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "启用之日")
    private Date effStart;

    /** 交棒之日(不含)；空着=现行版 */
    @TableField("eff_end")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "交棒之日(不含)")
    private Date effEnd;

    /** 抢版顺位(数值越大越先开口) */
    @TableField("priority")
    @ApiModelProperty(value = "抢版顺位")
    private Integer priority;

    /** 在不在场的旗子 1在场 0撤下 */
    @TableField("present_flag")
    @ApiModelProperty(value = "在不在场的旗子")
    private Integer presentFlag;

    /** 情形 0现行 1已让位 */
    @TableField("status")
    @ApiModelProperty(value = "情形 0现行 1已让位")
    private Integer status;

    /** 删除标记 0正常 1删除 */
    @TableField("del_flag")
    @ApiModelProperty(value = "删除标记")
    private Integer delFlag;

    @TableField(value = "create_by", fill = FieldFill.INSERT)
    @ApiModelProperty(value = "创建者")
    private String createBy;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    @TableField(value = "update_by", fill = FieldFill.UPDATE)
    @ApiModelProperty(value = "更新者")
    private String updateBy;

    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "更新时间")
    private Date updateTime;

    @TableField("remark")
    @ApiModelProperty(value = "备注")
    private String remark;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTableCode() {
        return tableCode;
    }

    public void setTableCode(String tableCode) {
        this.tableCode = tableCode;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public BigDecimal getParamA() {
        return paramA;
    }

    public void setParamA(BigDecimal paramA) {
        this.paramA = paramA;
    }

    public BigDecimal getExpD() {
        return expD;
    }

    public void setExpD(BigDecimal expD) {
        this.expD = expD;
    }

    public BigDecimal getExpH() {
        return expH;
    }

    public void setExpH(BigDecimal expH) {
        this.expH = expH;
    }

    public Date getEffStart() {
        return effStart;
    }

    public void setEffStart(Date effStart) {
        this.effStart = effStart;
    }

    public Date getEffEnd() {
        return effEnd;
    }

    public void setEffEnd(Date effEnd) {
        this.effEnd = effEnd;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Integer getPresentFlag() {
        return presentFlag;
    }

    public void setPresentFlag(Integer presentFlag) {
        this.presentFlag = presentFlag;
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
