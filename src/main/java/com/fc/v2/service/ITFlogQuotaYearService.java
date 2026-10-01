package com.fc.v2.service;

import com.fc.v2.model.auto.TFlogQuotaYear;

import java.math.BigDecimal;

/**
 * 年度采伐额度下达 Service：年初把一家本年可占的立方米总数批到账上。
 *
 * @author fuce
 * @date 2026-09-29
 */
public interface ITFlogQuotaYearService {

    /**
     * 下达（或改批）某档某年总额。已扣合计不许被改批压穿。仅上级账号可批。
     */
    TFlogQuotaYear issue(String siteNo, Integer quotaYear, BigDecimal totalQty);
}
