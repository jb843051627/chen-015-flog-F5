package com.fc.v2.service.flog;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.exception.biz.FlogBizException;
import com.fc.v2.mapper.auto.TFlogSiteGrantMapper;
import com.fc.v2.model.auto.TFlogSiteGrant;
import org.apache.shiro.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 翻看界：本站只翻本辖那几家，上级账号看全辖，跨县协办只把受托那一家摆给受托人。
 * 别场的册不许并列摆出来挑。
 *
 * @author fuce
 * @date 2026-09-29
 */
@Component
public class FlogDataScope {

    /** 上级账号角色名（全辖尽览） */
    public static final String SUPERIOR_ROLE = "管理员";

    @Autowired
    private TFlogSiteGrantMapper siteGrantMapper;

    /**
     * 是否上级账号：全辖。
     */
    public boolean isSuperior() {
        try {
            return SecurityUtils.getSubject().hasRole(SUPERIOR_ROLE);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 当前账号看得见的底档字号；上级账号不看授权表。
     */
    public List<String> visibleSiteNos(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        List<TFlogSiteGrant> grants = siteGrantMapper.selectList(new QueryWrapper<TFlogSiteGrant>()
                .eq("user_id", userId)
                .eq("del_flag", 0));
        return grants.stream().map(TFlogSiteGrant::getSiteNo).distinct().collect(Collectors.toList());
    }

    /**
     * 核验某字号摆不摆得给当前账号；越界当场回绝。
     */
    public void assertVisible(Long userId, String siteNo) {
        if (siteNo == null || isSuperior()) {
            return;
        }
        List<String> nos = visibleSiteNos(userId);
        if (!nos.contains(siteNo)) {
            throw new FlogBizException("底档字号 " + siteNo + " 不在你名下受权册内，别家的册不许翻看");
        }
    }
}
