package com.fc.v2.controller.admin;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.domain.ResultTable;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.custom.flog.FlogSurveyDesignDto;
import com.fc.v2.model.custom.flog.FlogSurveyRecheckDto;
import com.fc.v2.model.custom.flog.FlogSurveySubmitVo;
import com.fc.v2.service.ITFlogSurveyDesignService;
import com.github.pagehelper.PageInfo;

import java.util.List;

/**
 * 伐区调查设计 Controller。
 *
 * 屏上只有提交、看结论两件事：线代号与启用之日压在设计单上的值全以业务服务层
 * 那条三级链求值方法的返回为准，页面不许自己凑一条界再摆一个结论。
 *
 * @author fuce
 * @date 2026-09-30
 */
@Api(value = "伐区调查设计")
@Controller
@RequestMapping("/FlogSurveyDesignController")
public class FlogSurveyDesignController extends BaseController {

    private final String prefix = "admin/flogSurveyDesign";

    @Autowired
    private ITFlogSurveyDesignService flogSurveyDesignService;

    @ApiOperation(value = "分页跳转", notes = "分页跳转")
    @GetMapping("/view")
    @RequiresPermissions("flog:flogSurveyDesign:view")
    public String view(ModelMap model) {
        return prefix + "/list";
    }

    @Log(title = "调查设计单查询", action = "list")
    @ApiOperation(value = "设计单列表", notes = "数据权限内按字号/小班/类别过滤")
    @GetMapping("/list")
    @RequiresPermissions("flog:flogSurveyDesign:view")
    @ResponseBody
    public ResultTable list(@RequestParam(value = "siteNo", required = false) String siteNo,
                            @RequestParam(value = "subcompNo", required = false) String subcompNo,
                            @RequestParam(value = "applyScope", required = false) Integer applyScope) {
        startPage();
        List<?> rows = flogSurveyDesignService.listDesigns(siteNo, subcompNo, applyScope);
        PageInfo<?> page = new PageInfo<>(rows);
        return pageTable(page.getList(), page.getTotal());
    }

    @ApiOperation(value = "提交页跳转", notes = "山场检尺逐株录入；带 designId 为整改复验")
    @GetMapping("/submitView")
    @RequiresPermissions("flog:flogSurveyDesign:add")
    public String submitView(@RequestParam(value = "designId", required = false) Long designId, ModelMap model) {
        model.put("designId", designId);
        return prefix + "/submit";
    }

    @Log(title = "调查设计首评提交", action = "submit")
    @ApiOperation(value = "首评提交", notes = "三级链一处算清，结论连同界代号/启用日钉死")
    @PostMapping("/submit")
    @RequiresPermissions("flog:flogSurveyDesign:add")
    @ResponseBody
    public AjaxResult submit(@RequestBody FlogSurveyDesignDto dto) {
        FlogSurveySubmitVo vo = flogSurveyDesignService.submit(dto);
        if (vo.isBasisMissing()) {
            // 无线可依不是录入错误：单照钉、结论写明无线，提醒别拿邻近日子凑
            return AjaxResult.success(200, vo.getMessage(), vo);
        }
        return AjaxResult.success(200, vo.getMessage(), vo);
    }

    @Log(title = "整改复验重算", action = "recheck")
    @ApiOperation(value = "整改复验", notes = "沿用原单设计日重算一遍，追加结论，两回各留各的代号")
    @PostMapping("/recheck")
    @RequiresPermissions("flog:flogSurveyDesign:add")
    @ResponseBody
    public AjaxResult recheck(@RequestBody FlogSurveyRecheckDto dto) {
        FlogSurveySubmitVo vo = flogSurveyDesignService.recheck(dto);
        return AjaxResult.success(200, vo.getMessage(), vo);
    }

    @ApiOperation(value = "设计单详情", notes = "单头")
    @GetMapping("/detail/{id}")
    @RequiresPermissions("flog:flogSurveyDesign:view")
    @ResponseBody
    public AjaxResult detail(@PathVariable("id") Long id) {
        return AjaxResult.successData(200, flogSurveyDesignService.selectById(id));
    }

    @Log(title = "各轮定档结论查询", action = "results")
    @ApiOperation(value = "各轮结论", notes = "首评与历次复验并列，钉死的行一字不改")
    @GetMapping("/results")
    @RequiresPermissions("flog:flogSurveyDesign:view")
    @ResponseBody
    public AjaxResult results(@RequestParam("designId") Long designId) {
        return AjaxResult.successData(200, flogSurveyDesignService.listResults(designId));
    }

    @Log(title = "逐株检尺账查询", action = "trees")
    @ApiOperation(value = "逐株检尺账", notes = "含同一回算出的逐株材积；roundNo 空着取最近一轮")
    @GetMapping("/trees")
    @RequiresPermissions("flog:flogSurveyDesign:view")
    @ResponseBody
    public AjaxResult trees(@RequestParam("designId") Long designId,
                            @RequestParam(value = "roundNo", required = false) Integer roundNo) {
        return AjaxResult.successData(200, flogSurveyDesignService.listTrees(designId, roundNo));
    }
}
