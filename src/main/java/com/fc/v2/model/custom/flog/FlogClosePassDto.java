package com.fc.v2.model.custom.flog;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 勾销卡过口送报。卡停在哪段，只收那段该报的格子，其余送了也不认：
 * 报伐：钉过号的设计单号 + 实采株数 + 实采蓄积；
 * 核株：差过站里那格时给一句说法（设计两数由系统照钉档取，不许手填）；
 * 验更：更新造林面积 + 成活率（采伐面积照设计单钉住）；
 * 了断：无（摘现行建档卡、退占额由系统同一回做）。
 *
 * @author fuce
 * @date 2026-10-01
 */
public class FlogClosePassDto implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 勾销卡id */
    private Long id;

    /** 经办人自认正在过的道口（0报伐 1核株 2验更 3了断）；与系统算的不符即跳段/重报，一概不算过成 */
    private Integer stage;

    /** 报伐：设计单上钉过的号 */
    private String designNo;

    /** 报伐：实采株数 */
    private Integer actualTrees;

    /** 报伐：实采蓄积(立方米) */
    private BigDecimal actualVolume;

    /** 核株：差过站里那格时的一句说法 */
    private String diffReason;

    /** 验更：更新造林面积(公顷) */
    private BigDecimal renewArea;

    /** 验更：更新造林成活率(%) */
    private BigDecimal renewSurvival;

    /** 本笔附言（只随这笔履历走，不替任何关口） */
    private String remark;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getStage() {
        return stage;
    }

    public void setStage(Integer stage) {
        this.stage = stage;
    }

    public String getDesignNo() {
        return designNo;
    }

    public void setDesignNo(String designNo) {
        this.designNo = designNo;
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

    public BigDecimal getRenewSurvival() {
        return renewSurvival;
    }

    public void setRenewSurvival(BigDecimal renewSurvival) {
        this.renewSurvival = renewSurvival;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
