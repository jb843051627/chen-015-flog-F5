package com.fc.v2.model.custom.flog;

import com.fc.v2.model.auto.TFlogCloseCard;
import com.fc.v2.model.auto.TFlogCloseJournal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 勾销履历给回：卡 + 逐笔流水（含沉底旧笔）+ 当场核验结果。
 * 从了断往回报伐数，缺页、凭空多出、过后手动补的，一处对不上 intact 即为 false。
 *
 * @author fuce
 * @date 2026-10-01
 */
public class FlogCloseLedgerVo implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 卡 */
    private TFlogCloseCard card;

    /** 履历逐笔（按流水序） */
    private List<TFlogCloseJournal> lines = new ArrayList<>();

    /** 每一笔还在不在册（true=现行有效；false=已被回退沉底，只供回查），与 lines 同序 */
    private List<Boolean> alive = new ArrayList<>();

    /** 整链核不核得过 */
    private boolean intact = true;

    /** 头一处对不上的流水序（null=全合） */
    private Integer breakSeq;

    /** 点破的话：缺页/多笔/指纹对不上/沉底无着等 */
    private String breakReason;

    public TFlogCloseCard getCard() {
        return card;
    }

    public void setCard(TFlogCloseCard card) {
        this.card = card;
    }

    public List<TFlogCloseJournal> getLines() {
        return lines;
    }

    public void setLines(List<TFlogCloseJournal> lines) {
        this.lines = lines;
    }

    public List<Boolean> getAlive() {
        return alive;
    }

    public void setAlive(List<Boolean> alive) {
        this.alive = alive;
    }

    public boolean isIntact() {
        return intact;
    }

    public void setIntact(boolean intact) {
        this.intact = intact;
    }

    public Integer getBreakSeq() {
        return breakSeq;
    }

    public void setBreakSeq(Integer breakSeq) {
        this.breakSeq = breakSeq;
    }

    public String getBreakReason() {
        return breakReason;
    }

    public void setBreakReason(String breakReason) {
        this.breakReason = breakReason;
    }
}
