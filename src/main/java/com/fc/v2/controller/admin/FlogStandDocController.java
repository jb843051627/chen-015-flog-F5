package com.fc.v2.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.domain.ResultTable;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.auto.TFlogStandDoc;
import com.fc.v2.service.ITFlogStandDocService;
import com.fc.v2.util.StringUtils;
import com.github.pagehelper.PageInfo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

/**
 * 编制单位在册底档 Controller
 *
 * @author fuce
 * @date 2026-09-29
 */
@Api(value = "编制单位在册底档")
@Controller
@RequestMapping("/FlogStandDocController")
public class FlogStandDocController extends BaseController {

    private final String prefix = "admin/flogStandDoc";

    @Autowired
    private ITFlogStandDocService standDocService;

    @GetMapping("/view")
    @RequiresPermissions("flog:flogStandDoc:view")
    public String view(ModelMap model) {
        return prefix + "/list";
    }

    @Log(title = "编制单位底档集合查询", action = "list")
    @GetMapping("/list")
    @RequiresPermissions("flog:flogStandDoc:view")
    @ResponseBody
    public ResultTable list(TFlogStandDoc query) {
        QueryWrapper<TFlogStandDoc> w = new QueryWrapper<>();
        w.eq(StringUtils.isNotEmpty(query.getSiteNo()), "site_no", query.getSiteNo());
        w.like(StringUtils.isNotEmpty(query.getSiteName()), "site_name", query.getSiteName());
        w.eq(StringUtils.isNotEmpty(query.getSiteType()), "site_type", query.getSiteType());
        w.eq(query.getStatus() != null, "status", query.getStatus());
        startPage();
        PageInfo<TFlogStandDoc> page = new PageInfo<>(standDocService.selectList(w));
        return pageTable(page.getList(), page.getTotal());
    }

    @GetMapping("/add")
    @RequiresPermissions("flog:flogStandDoc:add")
    public String add() {
        return prefix + "/add";
    }

    @Log(title = "编制单位底档立户", action = "add")
    @PostMapping("/add")
    @RequiresPermissions("flog:flogStandDoc:add")
    @ResponseBody
    public AjaxResult add(TFlogStandDoc doc) {
        // 字号、情形由系统定，界面不设口子
        doc.setSiteNo(null);
        doc.setStatus(null);
        TFlogStandDoc saved = standDocService.register(doc);
        return AjaxResult.success(200, "立户成形，系统排定字号 " + saved.getSiteNo(), saved);
    }

    @Log(title = "编制单位底档年初收回", action = "recall")
    @PostMapping("/recall/{id}")
    @RequiresPermissions("flog:flogStandDoc:add")
    @ResponseBody
    public AjaxResult recall(@PathVariable("id") Long id) {
        TFlogStandDoc doc = standDocService.recall(id);
        return AjaxResult.success(200, "字号 " + doc.getSiteNo() + " 已收回，不再接纳新卡", doc);
    }
}
