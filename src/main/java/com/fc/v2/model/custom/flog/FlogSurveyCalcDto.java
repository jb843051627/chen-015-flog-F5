package com.fc.v2.model.custom.flog;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 伐区调查设计三级链求值入参。
 *
 * 头一级山场检尺数（胸径厘米、树高米）原样进门；取界只认 designDate（调查设计那一日）。
 * 来路不对的数（负数、胸径四位九 9999 这类、株数为零）由服务层当场退回调查员重录，不进链。
 *
 * @author fuce
 * @date 2026-09-30
 */
public class FlogSurveyCalcDto implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 林地类别 2商品林 11公益林·抚育 12公益林·更新 13公益林·低质低效林改造 */
    private Integer applyScope;

    /** 调查设计那一日（取界、取材积表版都只认这一天，不看今天） */
    private Date designDate;

    /** 小班面积(公顷) */
    private BigDecimal area;

    /** 出材率 */
    private BigDecimal yieldRate;

    /** 山场逐株检尺：株数即本单长度，株数为零不进链 */
    private List<TreeMeasure> trees;

    /** 单株检尺两数：胸径(厘米)、树高(米) */
    public static class TreeMeasure implements java.io.Serializable {
        private static final long serialVersionUID = 1L;

        /** 胸径(厘米) */
        private BigDecimal dbh;
        /** 树高(米) */
        private BigDecimal height;

        public TreeMeasure() {
        }

        public TreeMeasure(BigDecimal dbh, BigDecimal height) {
            this.dbh = dbh;
            this.height = height;
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
    }

    public Integer getApplyScope() {
        return applyScope;
    }

    public void setApplyScope(Integer applyScope) {
        this.applyScope = applyScope;
    }

    public Date getDesignDate() {
        return designDate;
    }

    public void setDesignDate(Date designDate) {
        this.designDate = designDate;
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

    public List<TreeMeasure> getTrees() {
        return trees;
    }

    public void setTrees(List<TreeMeasure> trees) {
        this.trees = trees;
    }
}
