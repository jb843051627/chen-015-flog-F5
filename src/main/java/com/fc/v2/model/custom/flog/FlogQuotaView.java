package com.fc.v2.model.custom.flog;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 同屏额度账给回（含占着额度的现行卡清单，已扣/尚存同回折算）。
 *
 * @author fuce
 * @date 2026-09-29
 */
public class FlogQuotaView implements Serializable {
    private static final long serialVersionUID = 1L;

    private FlogQuotaVo quota;

    /** 占着本年额度的现行未压死卡 */
    private List<OccupyLine> occupyLines;

    public FlogQuotaView() {
    }

    public FlogQuotaView(FlogQuotaVo quota, List<OccupyLine> occupyLines) {
        this.quota = quota;
        this.occupyLines = occupyLines;
    }

    public FlogQuotaVo getQuota() {
        return quota;
    }

    public void setQuota(FlogQuotaVo quota) {
        this.quota = quota;
    }

    public List<OccupyLine> getOccupyLines() {
        return occupyLines;
    }

    public void setOccupyLines(List<OccupyLine> occupyLines) {
        this.occupyLines = occupyLines;
    }

    /** 单卡占额一行：卡号、小班、拟采蓄积、立卡人、最近一笔办理人。 */
    public static class OccupyLine implements Serializable {
        private static final long serialVersionUID = 1L;

        private String billNo;
        private Integer nodeNo;
        private String subcompNo;
        private String cutBlockNo;
        private Integer gradeLevel;
        private Integer status;
        private BigDecimal qty;
        private String registerBy;
        private Date registerTime;
        private String lastOpBy;
        private Date lastOpTime;

        public String getBillNo() {
            return billNo;
        }

        public void setBillNo(String billNo) {
            this.billNo = billNo;
        }

        public Integer getNodeNo() {
            return nodeNo;
        }

        public void setNodeNo(Integer nodeNo) {
            this.nodeNo = nodeNo;
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

        public Integer getGradeLevel() {
            return gradeLevel;
        }

        public void setGradeLevel(Integer gradeLevel) {
            this.gradeLevel = gradeLevel;
        }

        public Integer getStatus() {
            return status;
        }

        public void setStatus(Integer status) {
            this.status = status;
        }

        public BigDecimal getQty() {
            return qty;
        }

        public void setQty(BigDecimal qty) {
            this.qty = qty;
        }

        public String getRegisterBy() {
            return registerBy;
        }

        public void setRegisterBy(String registerBy) {
            this.registerBy = registerBy;
        }

        public Date getRegisterTime() {
            return registerTime;
        }

        public void setRegisterTime(Date registerTime) {
            this.registerTime = registerTime;
        }

        public String getLastOpBy() {
            return lastOpBy;
        }

        public void setLastOpBy(String lastOpBy) {
            this.lastOpBy = lastOpBy;
        }

        public Date getLastOpTime() {
            return lastOpTime;
        }

        public void setLastOpTime(Date lastOpTime) {
            this.lastOpTime = lastOpTime;
        }
    }
}
