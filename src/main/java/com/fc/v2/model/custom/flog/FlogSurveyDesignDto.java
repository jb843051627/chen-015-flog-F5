package com.fc.v2.model.custom.flog;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 伐区调查设计单提交入参（首评）。
 *
 * 一张单子一个小班；山场检尺逐株数随单进门。界哪一条、档哪一格不由录单选——
 * 服务层拿 designDate 现算现给，屏上只摆返回那份。
 *
 * @author fuce
 * @date 2026-09-30
 */
public class FlogSurveyDesignDto implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 调查设计单号（空着由系统生成） */
    private String designNo;

    /** 编制单位底档字号 */
    private String siteNo;

    /** 小班号 */
    private String subcompNo;

    /** 伐区名号 */
    private String cutBlockNo;

    /** 林地类别 2商品林 11公益林·抚育 12公益林·更新 13公益林·低质低效林改造 */
    private Integer applyScope;

    /** 小班面积(公顷) */
    private BigDecimal area;

    /** 出材率 */
    private BigDecimal yieldRate;

    /** 调查员（山场检尺人） */
    private String surveyBy;

    /** 调查设计那一日（取界只认这一天） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date designDate;

    /** 山场逐株检尺两数 */
    private List<FlogSurveyCalcDto.TreeMeasure> trees;

    public String getDesignNo() {
        return designNo;
    }

    public void setDesignNo(String designNo) {
        this.designNo = designNo;
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

    public Integer getApplyScope() {
        return applyScope;
    }

    public void setApplyScope(Integer applyScope) {
        this.applyScope = applyScope;
    }

    public BigDecimal getArea() {
        return area;
    }

    public void setArea(BigDecimal area) {
        this.area = area;
    }

    public BigDecimal getYieldRate() {
        return yieldRate;
    }

    public void setYieldRate(BigDecimal yieldRate) {
        this.yieldRate = yieldRate;
    }

    public String getSurveyBy() {
        return surveyBy;
    }

    public void setSurveyBy(String surveyBy) {
        this.surveyBy = surveyBy;
    }

    public Date getDesignDate() {
        return designDate;
    }

    public void setDesignDate(Date designDate) {
        this.designDate = designDate;
    }

    public List<FlogSurveyCalcDto.TreeMeasure> getTrees() {
        return trees;
    }

    public void setTrees(List<FlogSurveyCalcDto.TreeMeasure> trees) {
        this.trees = trees;
    }
}
