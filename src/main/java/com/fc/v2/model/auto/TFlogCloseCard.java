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
 * 伐区勾销事务卡对象 t_flog_close_card
 *
 * 一小班一张卡，四段——报伐、核株、验更、了断——挨着推进。
 * 卡头只放身份、四段报上来的业务格与落定；停在哪段由勾销履历
 * （t_flog_close_stage 里 active 的行数）算出来，stage/status/content/lastAction
 * 四格只作摆示，手填一律不作数。了断即摘册并把占额原样退回；止步、了断皆封死；
 * 卡永不物理删除（抽走了年底就查不着这片小班那年怎么收的）。
 *
 * @author fuce
 * @date 2026-09-12
 */
@TableName("t_flog_close_card")
@ApiModel(value = "TFlogCloseCard", description = "伐区勾销事务卡")
public class TFlogCloseCard implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 落定：在办 */
    public static final int STATUS_ACTIVE = 1;
    /** 落定：已了断（封死） */
    public static final int STATUS_TERMINAL = 2;
    /** 落定：止步（封死，谁也续不进） */
    public static final int STATUS_HALTED = 3;

    /** 段次：报伐 */
    public static final int STAGE_DECLARE = 0;
    /** 段次：核株 */
    public static final int STAGE_CHECK = 1;
    /** 段次：验更 */
    public static final int STAGE_RENEW = 2;
    /** 段次：了断 */
    public static final int STAGE_CLOSE = 3;
    /** 段次数上限（四段编号 0..3） */
    public static final int STAGE_COUNT = 4;

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

    /** 小班号（一张卡一片小班） */
    @TableField("subcomp_no")
    @ApiModelProperty(value = "小班号")
    private String subcompNo;

    /** 伐区名号 */
    @TableField("cut_block_no")
    @ApiModelProperty(value = "伐区名号")
    private String cutBlockNo;

    /** 报伐所认调查设计单号（钉过号的那张） */
    @TableField("design_no")
    @ApiModelProperty(value = "报伐所认调查设计单号")
    private String designNo;

    /** 设计株数（核株拿实采跟它对着看） */
    @TableField("design_trees")
    @ApiModelProperty(value = "设计株数")
    private Integer designTrees;

    /** 设计蓄积(立方米) */
    @TableField("design_volume")
    @ApiModelProperty(value = "设计蓄积(立方米)")
    private BigDecimal designVolume;

    /** 采伐面积(公顷，验更拿更新面积跟它比) */
    @TableField("cut_area")
    @ApiModelProperty(value = "采伐面积(公顷)")
    private BigDecimal cutArea;

    /** 实采株数（报伐口报上来） */
    @TableField("actual_trees")
    @ApiModelProperty(value = "实采株数")
    private Integer actualTrees;

    /** 实采蓄积(立方米，报伐口报上来) */
    @TableField("actual_volume")
    @ApiModelProperty(value = "实采蓄积(立方米)")
    private BigDecimal actualVolume;

    /** 本站定的核株差格(立方米)，越过就得给一句说法 */
    @TableField("diff_tolerance")
    @ApiModelProperty(value = "核株差格(立方米)")
    private BigDecimal diffTolerance;

    /** 核株差额越过差格时的一句说法 */
    @TableField("diff_reason")
    @ApiModelProperty(value = "差额越过差格时的一句说法")
    private String diffReason;

    /** 更新造林面积(公顷，不得小于采伐面积) */
    @TableField("renew_area")
    @ApiModelProperty(value = "更新造林面积(公顷)")
    private BigDecimal renewArea;

    /** 更新成活率(%) */
    @TableField("survive_rate")
    @ApiModelProperty(value = "更新成活率(%)")
    private BigDecimal surviveRate;

    /** 本卡占着的额度(立方米)，了断原样退回 */
    @TableField("occupy_qty")
    @ApiModelProperty(value = "本卡占着的额度(立方米)")
    private BigDecimal occupyQty;

    /** 占额归属年度（退就退回这一年账） */
    @TableField("quota_year")
    @ApiModelProperty(value = "占额归属年度")
    private Integer quotaYear;

    /** 占额来源建档卡id（了断把它从在册处摘下、走它的压死退额） */
    @TableField("apply_card_id")
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "占额来源建档卡id")
    private Long applyCardId;

    /** 活卡对锁：在办=字号@小班，了断/止步置空；唯一索引兜底一小班一活卡 */
    @TableField("live_key")
    @ApiModelProperty(value = "活卡对锁")
    private String liveKey;

    /** 摆示用段次（系统算：过成几段就在几段，不作权威） */
    @TableField("stage")
    @ApiModelProperty(value = "摆示用段次（系统按履历算）")
    private Integer stage;

    /** 卡的落定 1在办 2已了断 3止步（止步封死，谁也续不进） */
    @TableField("status")
    @ApiModelProperty(value = "卡的落定 1在办 2已了断 3止步")
    private Integer status;

    /** 勾销记事 */
    @TableField("content")
    @ApiModelProperty(value = "勾销记事")
    private String content;

    /** 最近一次过口动作 */
    @TableField("last_action")
    @ApiModelProperty(value = "最近一次过口动作")
    private String lastAction;

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

    public String getDesignNo() {
        return designNo;
    }

    public void setDesignNo(String designNo) {
        this.designNo = designNo;
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

    public BigDecimal getCutArea() {
        return cutArea;
    }

    public void setCutArea(BigDecimal cutArea) {
        this.cutArea = cutArea;
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

    public BigDecimal getDiffTolerance() {
        return diffTolerance;
    }

    public void setDiffTolerance(BigDecimal diffTolerance) {
        this.diffTolerance = diffTolerance;
    }

    public String getDiffReason() {
        return diffReason;
    }

    public void setDiffReason(String diffReason) {
        this.diffReason = diffReason;
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

    public BigDecimal getOccupyQty() {
        return occupyQty;
    }

    public void setOccupyQty(BigDecimal occupyQty) {
        this.occupyQty = occupyQty;
    }

    public Integer getQuotaYear() {
        return quotaYear;
    }

    public void setQuotaYear(Integer quotaYear) {
        this.quotaYear = quotaYear;
    }

    public Long getApplyCardId() {
        return applyCardId;
    }

    public void setApplyCardId(Long applyCardId) {
        this.applyCardId = applyCardId;
    }

    public String getLiveKey() {
        return liveKey;
    }

    public void setLiveKey(String liveKey) {
        this.liveKey = liveKey;
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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getLastAction() {
        return lastAction;
    }

    public void setLastAction(String lastAction) {
        this.lastAction = lastAction;
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
