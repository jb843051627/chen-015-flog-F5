package com.fc.v2.model.custom.flog;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 同屏额度账给回：本年可占、已扣、尚存三个数出自同一回计算，
 * 页面自己拼出来的数一律不算。
 *
 * @author fuce
 * @date 2026-09-29
 */
public class FlogQuotaVo implements Serializable {
    private static final long serialVersionUID = 1L;

    private String siteNo;

    private String siteName;

    private Integer quotaYear;

    /** 本年可占总数 */
    private BigDecimal totalQty;

    /** 已扣合计（现行未压死卡占用之和） */
    private BigDecimal occupiedQty;

    /** 尚存额度 = 总数 - 已扣 */
    private BigDecimal remainQty;

    public String getSiteNo() {
        return siteNo;
    }

    public void setSiteNo(String siteNo) {
        this.siteNo = siteNo;
    }

    public String getSiteName() {
        return siteName;
    }

    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }

    public Integer getQuotaYear() {
        return quotaYear;
    }

    public void setQuotaYear(Integer quotaYear) {
        this.quotaYear = quotaYear;
    }

    public BigDecimal getTotalQty() {
        return totalQty;
    }

    public void setTotalQty(BigDecimal totalQty) {
        this.totalQty = totalQty;
    }

    public BigDecimal getOccupiedQty() {
        return occupiedQty;
    }

    public void setOccupiedQty(BigDecimal occupiedQty) {
        this.occupiedQty = occupiedQty;
    }

    public BigDecimal getRemainQty() {
        return remainQty;
    }

    public void setRemainQty(BigDecimal remainQty) {
        this.remainQty = remainQty;
    }
}
