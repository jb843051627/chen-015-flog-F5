package com.fc.v2.model.auto;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 伐区调查山场检尺逐株账对象 t_flog_survey_tree
 *
 * 三级链头一级：山场每株量胸径(厘米)与树高(米)，逐笔记账。
 * treeVolume 是同一回三级计算算出的逐株材积，不许回写取另一版材积表。
 *
 * @author fuce
 * @date 2026-09-30
 */
@TableName("t_flog_survey_tree")
@ApiModel(value = "TFlogSurveyTree", description = "伐区调查山场检尺逐株账")
public class TFlogSurveyTree implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 所属调查设计单id */
    @TableField("design_id")
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "所属调查设计单id")
    private Long designId;

    /** 株序（山场记账顺序） */
    @TableField("tree_no")
    @ApiModelProperty(value = "株序")
    private Integer treeNo;

    /** 所属轮次 1首评 2整改复验…（各轮检尺账各记各的） */
    @TableField("round_no")
    @ApiModelProperty(value = "所属轮次")
    private Integer roundNo;

    /** 胸径(厘米) */
    @TableField("dbh")
    @ApiModelProperty(value = "胸径(厘米)")
    private BigDecimal dbh;

    /** 树高(米) */
    @TableField("height")
    @ApiModelProperty(value = "树高(米)")
    private BigDecimal height;

    /** 逐株材积(立方米，同一回计算钉入) */
    @TableField("tree_volume")
    @ApiModelProperty(value = "逐株材积")
    private BigDecimal treeVolume;

    @TableField("del_flag")
    @ApiModelProperty(value = "删除标记")
    private Integer delFlag;

    @TableField(value = "create_by", fill = FieldFill.INSERT)
    @ApiModelProperty(value = "创建者")
    private String createBy;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    @TableField(value = "update_by", fill = FieldFill.UPDATE)
    @ApiModelProperty(value = "更新者")
    private String updateBy;

    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    @ApiModelProperty(value = "更新时间")
    private Date updateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDesignId() {
        return designId;
    }

    public void setDesignId(Long designId) {
        this.designId = designId;
    }

    public Integer getTreeNo() {
        return treeNo;
    }

    public void setTreeNo(Integer treeNo) {
        this.treeNo = treeNo;
    }

    public Integer getRoundNo() {
        return roundNo;
    }

    public void setRoundNo(Integer roundNo) {
        this.roundNo = roundNo;
    }

    public BigDecimal getDbh() {
        return dbh;
    }

    public void setDbh(BigDecimal dbh) {
        this.dbh = dbh;
    }

    public BigDecimal getHeight() {
        return height;
    }

    public void setHeight(BigDecimal height) {
        this.height = height;
    }

    public BigDecimal getTreeVolume() {
        return treeVolume;
    }

    public void setTreeVolume(BigDecimal treeVolume) {
        this.treeVolume = treeVolume;
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
