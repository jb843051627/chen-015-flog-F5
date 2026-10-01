package com.fc.v2.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.domain.ResultTable;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.auto.TFlogCloseCard;
import com.fc.v2.model.custom.flog.FlogCloseOpenDto;
import com.fc.v2.model.custom.flog.FlogClosePassDto;
import com.fc.v2.service.ITFlogCloseCardService;
import com.fc.v2.util.StringUtils;
import com.github.pagehelper.PageInfo;
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

/**
 * 伐区勾销事务卡 Controller（state-machine 形状：流转入口）。
 *
 * 卡往哪走，屏上不算数：起卡/过口/回退/止步全只递到业务服务层那一对方法里，
 * 本控制器不判段次、不改记事、不删卡。履历另页翻检，缺页多页当场点破。
 *
 * @author fuce
 * @date 2026-09-14
 */
@Api(value = "伐区勾销事务卡")
@Controller
@RequestMapping("/FlogCloseCardController")
public class FlogCloseCardController extends BaseController {

    private final String prefix = "admin/flogCloseCard";

    @Autowired
    private ITFlogCloseCardService flogCloseCardService;

    @ApiOperation(value = "流转台账跳转", notes = "流转台账跳转")
    @GetMapping("/view")
    @RequiresPermissions("flog:flogCloseCard:view")
    public String view(ModelMap model) {
        return prefix + "/list";
    }

    @Log(title = "伐区勾销事务卡流转台账", action = "list")
    @ApiOperation(value = "流转台账", notes = "在办/止步/已了断集合，按账号辖域收口")
    @GetMapping("/list")
    @RequiresPermissions("flog:flogCloseCard:list")
    @ResponseBody
    public ResultTable list(TFlogCloseCard record) {
        QueryWrapper<TFlogCloseCard> queryWrapper = new QueryWrapper<TFlogCloseCard>();
        queryWrapper.eq(StringUtils.isNotEmpty(record.getBizNo()), "biz_no", record.getBizNo());
        queryWrapper.eq(StringUtils.isNotEmpty(record.getSiteNo()), "site_no", record.getSiteNo());
        queryWrapper.like(StringUtils.isNotEmpty(record.getSubcompNo()), "subcomp_no", record.getSubcompNo());
        queryWrapper.eq(record.getStatus() != null, "status", record.getStatus());
        queryWrapper.eq(record.getStage() != null, "stage", record.getStage());
        startPage();
        PageInfo<TFlogCloseCard> page =
                new PageInfo<TFlogCloseCard>(flogCloseCardService.selectTFlogCloseCardList(queryWrapper));
        return pageTable(page.getList(), page.getTotal());
    }

    @ApiOperation(value = "起卡跳转", notes = "起卡跳转")
    @GetMapping("/openPage")
    @RequiresPermissions("flog:flogCloseCard:add")
    public String openPage() {
        return prefix + "/open";
    }

    @Log(title = "伐区勾销事务卡起卡", action = "open")
    @ApiOperation(value = "起卡", notes = "一小班一张活卡，卡一起停在报伐口待过")
    @PostMapping("/open")
    @RequiresPermissions("flog:flogCloseCard:add")
    @ResponseBody
    public AjaxResult open(FlogCloseOpenDto dto) {
        return AjaxResult.success(200, "起卡成功", flogCloseCardService.open(dto));
    }

    @ApiOperation(value = "过口办理跳转", notes = "卡停在哪段，只摆那段该报的格子")
    @GetMapping("/pass/{id}")
    @RequiresPermissions("flog:flogCloseCard:add")
    public String pass(@PathVariable("id") Long id, ModelMap model) {
        model.put("cardId", id);
        return prefix + "/pass";
    }

    @ApiOperation(value = "卡面（系统算定的当前道口）", notes = "办理页同屏摆示")
    @GetMapping("/detail/{id}")
    @RequiresPermissions("flog:flogCloseCard:view")
    @ResponseBody
    public AjaxResult detail(@PathVariable("id") Long id) {
        return AjaxResult.successData(200, flogCloseCardService.selectTFlogCloseCardById(id));
    }

    @Log(title = "伐区勾销事务卡过口", action = "advance")
    @ApiOperation(value = "过口推进", notes = "旧入口(id,remark)与新入口(段次+该段各格)同收，都走同一方法")
    @PostMapping("/advance")
    @RequiresPermissions("flog:flogCloseCard:add")
    @ResponseBody
    public AjaxResult advance(FlogClosePassDto dto) {
        return AjaxResult.success(200, dto.getStage() == null ? "已推进" : "过口成功",
                flogCloseCardService.advance(dto));
    }

    @Log(title = "伐区勾销事务卡过口", action = "advance-json")
    @PostMapping("/advanceJson")
    @RequiresPermissions("flog:flogCloseCard:add")
    @ResponseBody
    public AjaxResult advanceJson(@RequestBody FlogClosePassDto dto) {
        return AjaxResult.success(200, "过口成功", flogCloseCardService.advance(dto));
    }

    @Log(title = "伐区勾销事务卡回退", action = "rollback")
    @ApiOperation(value = "回退一格", notes = "旧入口(id,remark)可调；新入口带段次核对落点；旧那笔沉底留库")
    @PostMapping("/rollback")
    @RequiresPermissions("flog:flogCloseCard:add")
    @ResponseBody
    public AjaxResult rollback(@RequestParam("id") Long id,
                               @RequestParam(value = "stage", required = false) Integer stage,
                               @RequestParam(value = "reason", required = false) String reason,
                               @RequestParam(value = "remark", required = false) String remark) {
        String note = reason != null ? reason : remark;
        return AjaxResult.success(200, "已回退", flogCloseCardService.rollback(id, stage, note));
    }

    @Log(title = "伐区勾销事务卡止步", action = "halt")
    @ApiOperation(value = "止步", notes = "这一段不过了；卡就此止步，谁也续不进去")
    @PostMapping("/halt")
    @RequiresPermissions("flog:flogCloseCard:add")
    @ResponseBody
    public AjaxResult halt(@RequestParam("id") Long id,
                           @RequestParam(value = "stage", required = false) Integer stage,
                           @RequestParam("reason") String reason) {
        return AjaxResult.success(200, "已止步", flogCloseCardService.halt(id, stage, reason));
    }

    @ApiOperation(value = "勾销履历翻检跳转", notes = "逐笔履历与当场核验")
    @GetMapping("/ledger")
    @RequiresPermissions("flog:flogCloseCard:edit")
    public String ledgerPage(@RequestParam("id") Long id, ModelMap model) {
        model.put("cardId", id);
        return prefix + "/ledger";
    }

    @ApiOperation(value = "勾销履历数据", notes = "从了断往回报伐当场核验，缺页多页手补即点破")
    @GetMapping("/ledgerData/{id}")
    @RequiresPermissions("flog:flogCloseCard:edit")
    @ResponseBody
    public AjaxResult ledgerData(@PathVariable("id") Long id) {
        return AjaxResult.successData(200, flogCloseCardService.ledger(id));
    }
}
