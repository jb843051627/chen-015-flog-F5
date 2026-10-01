package com.fc.v2.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fc.v2.common.exception.biz.FlogBizException;
import com.fc.v2.mapper.auto.TFlogStandDocMapper;
import com.fc.v2.model.auto.TFlogStandDoc;
import com.fc.v2.service.ITFlogStandDocService;
import com.fc.v2.service.flog.FlogDataScope;
import com.fc.v2.shiro.util.ShiroUtils;
import com.fc.v2.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 编制单位在册底档Service业务层处理
 *
 * @author fuce
 * @date 2026-09-29
 */
@Service
public class TFlogStandDocServiceImpl extends ServiceImpl<TFlogStandDocMapper, TFlogStandDoc>
        implements ITFlogStandDocService {

    /** 底档情形 */
    private static final int STATUS_ACTIVE = 0;
    private static final int STATUS_RECALLED = 1;

    @Autowired
    private FlogDataScope dataScope;

    @Override
    public List<TFlogStandDoc> selectList(Wrapper<TFlogStandDoc> queryWrapper) {
        QueryWrapper<TFlogStandDoc> wrapper =
                queryWrapper instanceof QueryWrapper ? (QueryWrapper<TFlogStandDoc>) queryWrapper : new QueryWrapper<>();
        wrapper.eq("del_flag", 0).orderByAsc("site_no");
        if (!dataScope.isSuperior()) {
            List<String> nos = dataScope.visibleSiteNos(currentUserId());
            if (nos.isEmpty()) {
                return new ArrayList<>();
            }
            wrapper.in("site_no", nos);
        }
        return this.baseMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TFlogStandDoc register(TFlogStandDoc doc) {
        if (!dataScope.isSuperior()) {
            throw new FlogBizException("只有上级账号能立编制单位底档户");
        }
        if (doc == null) {
            throw new FlogBizException("立户内容是空的");
        }
        if (StringUtils.isEmpty(doc.getSiteName())) {
            throw new FlogBizException("卡立不住，缺了便起不来的格子：编制单位全称");
        }
        if (StringUtils.isEmpty(doc.getSiteType())) {
            throw new FlogBizException("缺了经营类别（国有林场/集体林场/个人承包山），字号没法排");
        }
        if (StringUtils.isEmpty(doc.getRoadName())) {
            throw new FlogBizException("缺了管辖林业站（省—市—县）");
        }
        requireThresholds(doc);

        // 一档只管一家：同一名号（在册未删）不许立两户，单凭字号重名更不行
        Integer nameCnt = this.baseMapper.selectCount(new QueryWrapper<TFlogStandDoc>()
                .eq("site_name", doc.getSiteName().trim())
                .eq("del_flag", 0));
        if (nameCnt != null && nameCnt > 0) {
            throw new FlogBizException("编制单位「" + doc.getSiteName().trim() + "」已有在册底档，一档只管一家，不立第二户");
        }

        // 字号自动生成、按经营类别各排各的号；人往里塞的自拟号一律不存
        doc.setId(null);
        doc.setSiteNo(nextSiteNo(doc.getSiteType()));
        doc.setSiteName(doc.getSiteName().trim());
        doc.setStatus(STATUS_ACTIVE);
        doc.setDelFlag(0);
        this.baseMapper.insert(doc);
        return doc;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TFlogStandDoc recall(Long id) {
        if (!dataScope.isSuperior()) {
            throw new FlogBizException("只有上级账号能在年初收回底档");
        }
        TFlogStandDoc doc = this.baseMapper.selectById(id);
        if (doc == null || (doc.getDelFlag() != null && doc.getDelFlag() == 1)) {
            throw new FlogBizException("底档不存在");
        }
        if (doc.getStatus() != null && doc.getStatus() == STATUS_RECALLED) {
            throw new FlogBizException("底档字号 " + doc.getSiteNo() + " 早已收回，不必再收");
        }
        // 收回后不再接纳新卡；早年挂过的卡仍翻得到（不动卡、不删档）
        this.baseMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<TFlogStandDoc>()
                .eq("id", id)
                .set("status", STATUS_RECALLED));
        doc.setStatus(STATUS_RECALLED);
        return doc;
    }

    @Override
    public TFlogStandDoc selectById(Long id) {
        TFlogStandDoc doc = this.baseMapper.selectById(id);
        if (doc != null) {
            dataScope.assertVisible(currentUserId(), doc.getSiteNo());
        }
        return doc;
    }

    /**
     * 字号排法：国有林场 GL、集体林场 JT、个人承包山 GR，各自四位顺号递增。
     */
    private String nextSiteNo(String siteType) {
        String prefix;
        if ("国有林场".equals(siteType)) {
            prefix = "GL";
        } else if ("集体林场".equals(siteType)) {
            prefix = "JT";
        } else if ("个人承包山".equals(siteType)) {
            prefix = "GR";
        } else {
            throw new FlogBizException("经营类别「" + siteType + "」不在三档（国有林场/集体林场/个人承包山）内");
        }
        List<TFlogStandDoc> same = this.baseMapper.selectList(new QueryWrapper<TFlogStandDoc>()
                .likeRight("site_no", prefix)
                .eq("del_flag", 0));
        int max = 0;
        for (TFlogStandDoc d : same) {
            String tail = d.getSiteNo() == null ? "" : d.getSiteNo().substring(prefix.length());
            try {
                max = Math.max(max, Integer.parseInt(tail));
            } catch (NumberFormatException ignore) {
                // 种子号（如 DC00）不参与顺号
            }
        }
        for (int salt = 1; salt <= 100; salt++) {
            String no = prefix + String.format("%04d", max + salt);
            Integer cnt = this.baseMapper.selectCount(new QueryWrapper<TFlogStandDoc>()
                    .eq("site_no", no)
                    .eq("del_flag", 0));
            if (cnt == null || cnt == 0) {
                return no;
            }
        }
        throw new FlogBizException("「" + siteType + "」字号连撞，缓一缓再立户");
    }

    private void requireThresholds(TFlogStandDoc doc) {
        if (doc.getTh1Max() == null || doc.getTh2Max() == null || doc.getTh3Max() == null) {
            throw new FlogBizException("档内三条线（单卡拟采蓄积轻/中/重上限）得压全，缺一档都折不出顺位");
        }
        if (doc.getTh1Max().compareTo(BigDecimal.ZERO) <= 0
                || doc.getTh2Max().compareTo(doc.getTh1Max()) <= 0
                || doc.getTh3Max().compareTo(doc.getTh2Max()) <= 0) {
            throw new FlogBizException("三条线得都是正数且顺着往上抬：轻度上限 < 中度上限 < 重度上限");
        }
    }

    private Long currentUserId() {
        try {
            return ShiroUtils.getUserId();
        } catch (Exception e) {
            return null;
        }
    }
}
