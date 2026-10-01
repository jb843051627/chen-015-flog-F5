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
 * 伐区调查设计定档结论对象 t_flog_survey_result
 *
 * 评定结果连同所依界代号、界启用之日、材积表版一并钉死，钉过不再改口。
 * 整改复验重算一遍即追加一行：两回结论各留各的代号，合成一条不算对。
 *
 * @author fuce
 * @date 2026-09-30
 */
@TableName("t_flog_survey_result")
@ApiModel(value = "TFlogSurveyResult", description = "伐区调查设计定档结论")
public class TFlogSurveyResult implements Serializable {
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

    /** 轮次 1首评 2整改复验 3再复验… */
    @TableField("round_no")
    @ApiModelProperty(value = "轮次")
    private Integer roundNo;

    /** 是否复验 0首评 1整改复验 */
    @TableField("recheck_flag")
    @ApiModelProperty(value = "是否复验")
    private Integer recheckFlag;

    /** 本回总蓄积(立方米，逐株累加) */
    @TableField("total_volume")
    @ApiModelProperty(value = "本回总蓄积")
    private BigDecimal totalVolume;

    /** 本回每公顷出材量(立方米/公顷)=蓄积*出材率/面积 */
    @TableField("yield_per_ha")
    @ApiModelProperty(value = "本回每公顷出材量")
    private BigDecimal yieldPerHa;

    /** 强度档位 1轻 2中 3重；无线可依时为空 */
    @TableField("grade_level")
    @ApiModelProperty(value = "强度档位")
    private Integer gradeLevel;

    /** 是否无线可依 0有线 1翻不出在场界，写明无线，不拿邻近日子凑 */
    @TableField("basis_missing")
    @ApiModelProperty(value = "是否无线可依")
    private Integer basisMissing;

    /** 所依界代号 */
    @TableField("rule_code")
    @ApiModelProperty(value = "所依界代号")
    private String ruleCode;

    /** 所依那条界的启用之日 */
    @TableField("rule_eff_start")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "所依那条界的启用之日")
    private Date ruleEffStart;

    /** 所用材积表版代号 */
    @TableField("vol_table_code")
    @ApiModelProperty(value = "所用材积表版代号")
    private String volTableCode;

    /** 所用材积表版启用之日 */
    @TableField("vol_table_eff_start")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "所用材积表版启用之日")
    private Date volTableEffStart;

    /** 钉单人 */
    @TableField("pinned_by")
    @ApiModelProperty(value = "钉单人")
    private String pinnedBy;

    /** 钉单时刻 */
    @TableField("pinned_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "钉单时刻")
    private Date pinnedTime;

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

    public Long getDesignId() {
        return designId;
    }

    public void setDesignId(Long designId) {
        this.designId = designId;
    }

    public Integer getRoundNo() {
        return roundNo;
    }

    public void setRoundNo(Integer roundNo) {
        this.roundNo = roundNo;
    }

    public Integer getRecheckFlag() {
        return recheckFlag;
    }

    public void setRecheckFlag(Integer recheckFlag) {
        this.recheckFlag = recheckFlag;
    }

    public BigDecimal getTotalVolume() {
        return totalVolume;
    }

    public void setTotalVolume(BigDecimal totalVolume) {
        this.totalVolume = totalVolume;
    }

    public BigDecimal getYieldPerHa() {
        return yieldPerHa;
    }

    public void setYieldPerHa(BigDecimal yieldPerHa) {
        this.yieldPerHa = yieldPerHa;
    }

    public Integer getGradeLevel() {
        return gradeLevel;
    }

    public void setGradeLevel(Integer gradeLevel) {
        this.gradeLevel = gradeLevel;
    }

    public Integer getBasisMissing() {
        return basisMissing;
    }

    public void setBasisMissing(Integer basisMissing) {
        this.basisMissing = basisMissing;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public void setRuleCode(String ruleCode) {
        this.ruleCode = ruleCode;
    }

    public Date getRuleEffStart() {
        return ruleEffStart;
    }

    public void setRuleEffStart(Date ruleEffStart) {
        this.ruleEffStart = ruleEffStart;
    }

    public String getVolTableCode() {
        return volTableCode;
    }

    public void setVolTableCode(String volTableCode) {
        this.volTableCode = volTableCode;
    }

    public Date getVolTableEffStart() {
        return volTableEffStart;
    }

    public void setVolTableEffStart(Date volTableEffStart) {
        this.volTableEffStart = volTableEffStart;
    }

    public String getPinnedBy() {
        return pinnedBy;
    }

    public void setPinnedBy(String pinnedBy) {
        this.pinnedBy = pinnedBy;
    }

    public Date getPinnedTime() {
        return pinnedTime;
    }

    public void setPinnedTime(Date pinnedTime) {
        this.pinnedTime = pinnedTime;
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
