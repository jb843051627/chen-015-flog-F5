package com.fc.v2.controller.admin;

import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.log.Log;
import com.fc.v2.mapper.auto.TFlogStandDocMapper;
import com.fc.v2.model.auto.TFlogStandDoc;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.service.ITFlogQuotaYearService;
import com.fc.v2.service.flog.FlogDataScope;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 年度采伐额度下达 Controller（上级账号把本年可占总数批到账上）
 *
 * @author fuce
 * @date 2026-09-29
 */
@Api(value = "年度采伐额度下达")
@Controller
@RequestMapping("/FlogQuotaController")
public class FlogQuotaController extends BaseController {

    private final String prefix = "admin/flogQuota";

    @Autowired
    private ITFlogQuotaYearService quotaYearService;

    @Autowired
    private TFlogStandDocMapper standDocMapper;

    @Autowired
    private FlogDataScope dataScope;

    @GetMapping("/view")
    @RequiresPermissions("flog:flogQuota:view")
    public String view(ModelMap model) {
        QueryWrapper<TFlogStandDoc> w = new QueryWrapper<TFlogStandDoc>().eq("del_flag", 0).orderByAsc("site_no");
        if (!dataScope.isSuperior()) {
            List<String> nos = dataScope.visibleSiteNos(com.fc.v2.shiro.util.ShiroUtils.getUserId());
            if (nos.isEmpty()) {
                model.put("standDocs", java.util.Collections.emptyList());
                return prefix + "/issue";
            }
            w.in("site_no", nos);
        }
        model.put("standDocs", standDocMapper.selectList(w));
        return prefix + "/issue";
    }

    @Log(title = "年度采伐额度下达", action = "issue")
    @ApiOperation(value = "下达年度总额", notes = "年初批总数，已扣合计不许被压穿")
    @PostMapping("/issue")
    @RequiresPermissions("flog:flogQuota:add")
    @ResponseBody
    public AjaxResult issue(String siteNo, Integer quotaYear, BigDecimal totalQty) {
        return AjaxResult.success(200,
                siteNo + " " + (quotaYear == null ? "" : quotaYear) + " 年度额度已下达",
                quotaYearService.issue(siteNo, quotaYear, totalQty));
    }
}
