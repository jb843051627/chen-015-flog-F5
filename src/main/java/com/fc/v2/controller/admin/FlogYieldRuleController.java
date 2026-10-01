package com.fc.v2.controller.admin;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import com.github.pagehelper.PageInfo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.domain.ResultTable;
import com.fc.v2.common.exception.biz.FlogBizException;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.auto.TFlogYieldRule;
import com.fc.v2.service.ITFlogYieldRuleService;

/**
 * 可采强度界册 Controller。
 *
 * 界册只读：每一版（含让位版）都留在册里翻得着来历，本口不添不改版。
 * “当日在场”的口径只认服务层 listAvailable/pickRule，页面不自挑一条界摆结论。
 *
 * @author fuce
 * @date 2026-09-30
 */
@Api(value = "可采强度界册")
@Controller
@RequestMapping("/FlogYieldRuleController")
public class FlogYieldRuleController extends BaseController {

    private final String prefix = "admin/flogYieldRule";

    @Autowired
    private ITFlogYieldRuleService flogYieldRuleService;

    @ApiOperation(value = "分页跳转", notes = "分页跳转")
    @GetMapping("/view")
    @RequiresPermissions("flog:flogYieldRule:view")
    public String view(ModelMap model) {
        return prefix + "/list";
    }

    @Log(title = "界册全版查询", action = "list")
    @ApiOperation(value = "界册全版查询", notes = "含让位版，按代号/类别/情形过滤")
    @GetMapping("/list")
    @RequiresPermissions("flog:flogYieldRule:view")
    @ResponseBody
    public ResultTable list(@RequestParam(value = "ruleCode", required = false) String ruleCode,
                            @RequestParam(value = "applyScope", required = false) Integer applyScope,
                            @RequestParam(value = "status", required = false) Integer status) {
        startPage();
        List<TFlogYieldRule> rows = flogYieldRuleService.listAllVersions(ruleCode, applyScope, status);
        PageInfo<TFlogYieldRule> page = new PageInfo<>(rows);
        return pageTable(page.getList(), page.getTotal());
    }

    @Log(title = "当日在场界查询", action = "available")
    @ApiOperation(value = "当日在场界", notes = "按林地类别看某时刻在场的界；让位版、撤旗线不取")
    @GetMapping("/available")
    @RequiresPermissions("flog:flogYieldRule:view")
    @ResponseBody
    public AjaxResult available(@RequestParam(value = "applyScope", required = false) Integer applyScope,
                                @RequestParam(value = "at", required = false) String at) {
        Date day = parseDay(at);
        if (applyScope == null) {
            return AjaxResult.successData(200, flogYieldRuleService.listAvailable(day));
        }
        return AjaxResult.successData(200, flogYieldRuleService.listAvailable(applyScope, day));
    }

    @ApiOperation(value = "界版详情", notes = "让位版也翻得出")
    @GetMapping("/detail/{id}")
    @RequiresPermissions("flog:flogYieldRule:view")
    @ResponseBody
    public AjaxResult detail(@PathVariable("id") Long id) {
        return AjaxResult.successData(200, flogYieldRuleService.selectTFlogYieldRuleById(id));
    }

    private Date parseDay(String at) {
        if (at == null || at.trim().isEmpty()) {
            return new Date();
        }
        String s = at.trim();
        try {
            if (s.length() <= 10) {
                return new SimpleDateFormat("yyyy-MM-dd").parse(s);
            }
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(s);
        } catch (ParseException e) {
            throw new FlogBizException("日子格式不对，要 yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss");
        }
    }
}
