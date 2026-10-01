package com.fc.v2.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fc.v2.common.exception.biz.FlogBizException;
import com.fc.v2.mapper.auto.TFlogQuotaYearMapper;
import com.fc.v2.mapper.auto.TFlogStandDocMapper;
import com.fc.v2.model.auto.TFlogQuotaYear;
import com.fc.v2.model.auto.TFlogStandDoc;
import com.fc.v2.service.ITFlogQuotaYearService;
import com.fc.v2.service.flog.FlogDataScope;
import com.fc.v2.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Calendar;

/**
 * 年度采伐额度下达 Service实现
 *
 * @author fuce
 * @date 2026-09-29
 */
@Service
public class TFlogQuotaYearServiceImpl extends ServiceImpl<TFlogQuotaYearMapper, TFlogQuotaYear>
        implements ITFlogQuotaYearService {

    @Autowired
    private TFlogStandDocMapper standDocMapper;

    @Autowired
    private FlogDataScope dataScope;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TFlogQuotaYear issue(String siteNo, Integer quotaYear, BigDecimal totalQty) {
        if (!dataScope.isSuperior()) {
            throw new FlogBizException("只有上级账号能下达年度额度");
        }
        if (StringUtils.isEmpty(siteNo)) {
            throw new FlogBizException("缺了底档字号，额度批不到账");
        }
        int year = quotaYear == null ? Calendar.getInstance().get(Calendar.YEAR) : quotaYear;
        if (totalQty == null || totalQty.compareTo(BigDecimal.ZERO) < 0) {
            throw new FlogBizException("本年可占总数得填不小于零的立方米数");
        }
        TFlogStandDoc doc = standDocMapper.selectOne(new QueryWrapper<TFlogStandDoc>()
                .eq("site_no", siteNo)
                .eq("del_flag", 0)
                .last("limit 1"));
        if (doc == null) {
            throw new FlogBizException("底档字号 " + siteNo + " 查无在册底档");
        }

        TFlogQuotaYear quota = this.baseMapper.selectOne(new QueryWrapper<TFlogQuotaYear>()
                .eq("site_no", siteNo)
                .eq("quota_year", year)
                .eq("del_flag", 0)
                .last("limit 1 for update"));
        if (quota == null) {
            quota = new TFlogQuotaYear();
            quota.setSiteNo(siteNo);
            quota.setQuotaYear(year);
            quota.setTotalQty(totalQty);
            quota.setOccupiedQty(BigDecimal.ZERO);
            quota.setStatus(0);
            quota.setDelFlag(0);
            this.baseMapper.insert(quota);
        } else {
            BigDecimal occupied = quota.getOccupiedQty() == null ? BigDecimal.ZERO : quota.getOccupiedQty();
            if (totalQty.compareTo(occupied) < 0) {
                throw new FlogBizException(siteNo + " " + year + " 年已扣合计 " + occupied
                        + " 立方米，改批总数 " + totalQty + " 会把已扣的压穿，批不下去");
            }
            quota.setTotalQty(totalQty);
            this.baseMapper.updateById(quota);
        }
        return quota;
    }
}
