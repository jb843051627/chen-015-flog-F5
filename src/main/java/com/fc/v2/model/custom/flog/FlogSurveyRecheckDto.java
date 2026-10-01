package com.fc.v2.model.custom.flog;

import java.io.Serializable;
import java.util.List;

/**
 * 整改复验入参：认单（id 与单号给其一即可），交整改后重录的逐株检尺数。
 *
 * 小班、林地类别、面积、出材率、调查设计那一日全沿用原单——复验重算请的还是设计日在场的那条界。
 *
 * @author fuce
 * @date 2026-09-30
 */
public class FlogSurveyRecheckDto implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 调查设计单id（与 designNo 给其一） */
    private Long designId;

    /** 调查设计单号（与 designId 给其一） */
    private String designNo;

    /** 整改后重录的山场逐株检尺两数 */
    private List<FlogSurveyCalcDto.TreeMeasure> trees;

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

    public List<FlogSurveyCalcDto.TreeMeasure> getTrees() {
        return trees;
    }

    public void setTrees(List<FlogSurveyCalcDto.TreeMeasure> trees) {
        this.trees = trees;
    }
}
