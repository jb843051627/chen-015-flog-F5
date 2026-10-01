package com.fc.v2.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.domain.ResultTable;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.auto.TFlogCloseCard;
import com.fc.v2.model.custom.flog.FlogCloseOpenDto;
import com.fc.v2.model.custom.flog.FlogClosePassDto;
import com.fc.v2.model.custom.flog.FlogCloseRollbackDto;
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
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 伐区勾销事务卡 Controller（四段流转唯一入口）。
 *
 * 界面上没有现开一张卡、就地涂一句的地方：开卡即报伐，往后只准“推进/退回”
 * 那一对服务方法；没有删除口、没有改记事口。卡停在哪段、过没过成，一律以
 * 服务层给回的回执为准，本控制器不自算、不代填。
 *
 * @author fuce
 * @date 2026-09-14
 */
@Api(value = "伐区勾销事务卡")
@Controller
@RequestMapping("/flogCloseCard")
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
    @ApiOperation(value = "流转台账", notes = "段次由系统按勾销履历重算后摆")
    @GetMapping("/list")
    @RequiresPermissions("flog:flogCloseCard:view")
    @ResponseBody
    public ResultTable list(TFlogCloseCard record) {
        QueryWrapper<TFlogCloseCard> queryWrapper = new QueryWrapper<TFlogCloseCard>();
        queryWrapper.eq(StringUtils.isNotEmpty(record.getBizNo()), "biz_no", record.getBizNo());
        queryWrapper.eq(StringUtils.isNotEmpty(record.getSiteNo()), "site_no", record.getSiteNo());
        queryWrapper.like(StringUtils.isNotEmpty(record.getSubcompNo()), "subcomp_no", record.getSubcompNo());
        queryWrapper.eq(record.getStatus() != null, "status", record.getStatus());
        startPage();
        PageInfo<TFlogCloseCard> page =
                new PageInfo<TFlogCloseCard>(flogCloseCardService.selectTFlogCloseCardList(queryWrapper));
        return pageTable(page.getList(), page.getTotal());
    }

    @ApiOperation(value = "开卡（报伐）跳转", notes = "开卡与报伐同一口")
    @GetMapping("/add")
    @RequiresPermissions("flog:flogCloseCard:add")
    public String add() {
        return prefix + "/add";
    }

    @Log(title = "勾销卡开卡报伐", action = "add")
    @ApiOperation(value = "开卡即报伐（表单送法）", notes = "没有先开空卡再涂一句的第二道")
    @PostMapping("/add")
    @RequiresPermissions("flog:flogCloseCard:add")
    @ResponseBody
    public AjaxResult add(TFlogCloseCard record) {
        // 卡号、段次、落定、更新面积一律不许界面塞
        record.setId(null);
        record.setBizNo(null);
        record.setStage(null);
        record.setStatus(null);
        record.setContent(null);
        record.setLastAction(null);
        record.setRenewArea(null);
        record.setSurviveRate(null);
        record.setOccupyQty(null);
        record.setQuotaYear(null);
        record.setApplyCardId(null);
        record.setLiveKey(null);
        return AjaxResult.successData(200, flogCloseCardService.open(record));
    }

    @Log(title = "勾销卡开卡报伐(JSON)", action = "add")
    @ApiOperation(value = "开卡即报伐（JSON 送法）", notes = "与表单口同一套规矩")
    @PostMapping("/addJson")
    @RequiresPermissions("flog:flogCloseCard:add")
    @ResponseBody
    public AjaxResult addJson(@RequestBody FlogCloseOpenDto dto) {
        return AjaxResult.successData(200, flogCloseCardService.open(dto));
    }

    @Log(title = "勾销卡推进一档(JSON)", action = "advance")
    @ApiOperation(value = "往前推进一格（JSON 送法）", notes = "只认当前段该报的关口，跳段/重报一概不认")
    @PostMapping("/pass")
    @RequiresPermissions("flog:flogCloseCard:edit")
    @ResponseBody
    public AjaxResult pass(@RequestBody FlogClosePassDto dto) {
        return AjaxResult.successData(200, flogCloseCardService.advance(dto));
    }

    @Log(title = "勾销卡推进一档", action = "advance")
    @ApiOperation(value = "往前推进一格（表单送法）", notes = "核株报实采两数；验更报更新面积与成活；了断空身过")
    @PostMapping("/passForm")
    @RequiresPermissions("flog:flogCloseCard:edit")
    @ResponseBody
    public AjaxResult passForm(FlogClosePassDto dto) {
        return AjaxResult.successData(200, flogCloseCardService.advance(dto));
    }

    @Log(title = "勾销卡推进(旧送法)", action = "advance")
    @ApiOperation(value = "推进一格（旧入参 id+remark 一直可调）", notes = "核株段 remark 当差额说法")
    @PostMapping("/advance")
    @RequiresPermissions("flog:flogCloseCard:edit")
    @ResponseBody
    public AjaxResult advance(Long id, String remark) {
        return AjaxResult.successData(200, flogCloseCardService.advance(id, remark));
    }

    @Log(title = "勾销卡退回一格(JSON)", action = "rollback")
    @ApiOperation(value = "往回退一格（JSON 送法）", notes = "前一段名目尽数沉底，沉掉的不与后补的摆一处")
    @PostMapping("/back")
    @RequiresPermissions("flog:flogCloseCard:edit")
    @ResponseBody
    public AjaxResult back(@RequestBody FlogCloseRollbackDto dto) {
        return AjaxResult.successData(200, flogCloseCardService.rollback(dto));
    }

    @Log(title = "勾销卡退回一格", action = "rollback")
    @ApiOperation(value = "往回退一格（旧入参 id+remark 一直可调）", notes = "退回上一段重做")
    @PostMapping("/rollback")
    @RequiresPermissions("flog:flogCloseCard:edit")
    @ResponseBody
    public AjaxResult rollback(Long id, String remark) {
        return AjaxResult.successData(200, flogCloseCardService.rollback(id, remark));
    }

    @Log(title = "勾销卡止步", action = "halt")
    @ApiOperation(value = "止步封死", notes = "写下这一段不过了，谁也续不进；要动另起新卡")
    @PostMapping("/halt/{id}")
    @RequiresPermissions("flog:flogCloseCard:edit")
    @ResponseBody
    public AjaxResult halt(@PathVariable("id") Long id, String reason) {
        return AjaxResult.successData(200, flogCloseCardService.halt(id, reason));
    }

    @ApiOperation(value = "勾销履历跳转", notes = "一张卡一段段存下来的东西，含沉底旧轮")
    @GetMapping("/history/{id}")
    @RequiresPermissions("flog:flogCloseCard:view")
    public String history(@PathVariable("id") Long id, ModelMap modelMap) {
        modelMap.put("card", flogCloseCardService.selectTFlogCloseCardById(id));
        modelMap.put("stages", flogCloseCardService.listHistory(id));
        return prefix + "/history";
    }

    @Log(title = "勾销履历核验", action = "verify")
    @ApiOperation(value = "履历核验", notes = "从了断往回数回报伐，缺页/多笔/对不上当场点破")
    @PostMapping("/verify/{id}")
    @RequiresPermissions("flog:flogCloseCard:view")
    @ResponseBody
    public AjaxResult verify(@PathVariable("id") Long id) {
        return AjaxResult.successData(200, flogCloseCardService.verifyHistory(id));
    }
}
