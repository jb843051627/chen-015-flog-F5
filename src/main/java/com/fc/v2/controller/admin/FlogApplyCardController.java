package com.fc.v2.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.domain.ResultTable;
import com.fc.v2.common.log.Log;
import com.fc.v2.mapper.auto.TFlogStandDocMapper;
import com.fc.v2.model.auto.TFlogApplyCard;
import com.fc.v2.model.auto.TFlogStandDoc;
import com.fc.v2.model.custom.flog.FlogCardSaveDto;
import com.fc.v2.model.custom.flog.FlogCardSaveVo;
import com.fc.v2.service.ITFlogApplyCardService;
import com.fc.v2.service.flog.FlogDataScope;
import com.fc.v2.shiro.util.ShiroUtils;
import com.fc.v2.util.StringUtils;
import com.github.pagehelper.PageInfo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 伐区采伐申请建档卡 Controller
 *
 * 开不开得出卡，只由业务服务层那一条保存方法说了算；本控制器不收编号、不顺位、不拼核验串，
 * 屏上落的字与服务层给回的一份对得上。列表只摆现行版本，旧版本只供翻检不容覆写。
 *
 * @author fuce
 * @date 2026-09-12
 */
@Api(value = "伐区采伐申请建档卡")
@Controller
@RequestMapping("/FlogApplyCardController")
public class FlogApplyCardController extends BaseController {

    private final String prefix = "admin/flogApplyCard";

    @Autowired
    private ITFlogApplyCardService flogApplyCardService;

    @Autowired
    private TFlogStandDocMapper flogStandDocMapper;

    @Autowired
    private FlogDataScope dataScope;

    @ApiOperation(value = "分页跳转", notes = "分页跳转")
    @GetMapping("/view")
    @RequiresPermissions("flog:flogApplyCard:view")
    public String view(ModelMap model) {
        model.put("isSuperior", dataScope.isSuperior());
        return prefix + "/list";
    }

    @Log(title = "伐区采伐申请建档卡集合查询", action = "list")
    @ApiOperation(value = "分页查询", notes = "只摆现行版本，按账号辖域收口")
    @GetMapping("/list")
    @RequiresPermissions("flog:flogApplyCard:list")
    @ResponseBody
    public ResultTable list(TFlogApplyCard record) {
        QueryWrapper<TFlogApplyCard> queryWrapper = new QueryWrapper<TFlogApplyCard>();
        queryWrapper.eq(StringUtils.isNotEmpty(record.getBillNo()), "bill_no", record.getBillNo());
        queryWrapper.eq(StringUtils.isNotEmpty(record.getSiteNo()), "site_no", record.getSiteNo());
        queryWrapper.like(StringUtils.isNotEmpty(record.getSubcompNo()), "subcomp_no", record.getSubcompNo());
        queryWrapper.like(StringUtils.isNotEmpty(record.getCutBlockNo()), "cut_block_no", record.getCutBlockNo());
        queryWrapper.eq(record.getCutMode() != null, "cut_mode", record.getCutMode());
        queryWrapper.eq(record.getStatus() != null, "status", record.getStatus());
        startPage();
        PageInfo<TFlogApplyCard> page =
                new PageInfo<TFlogApplyCard>(flogApplyCardService.selectTFlogApplyCardList(queryWrapper));
        return pageTable(page.getList(), page.getTotal());
    }

    @ApiOperation(value = "新增跳转", notes = "新增跳转")
    @GetMapping("/add")
    @RequiresPermissions("flog:flogApplyCard:add")
    public String add(ModelMap modelMap) {
        modelMap.put("standDocs", visibleInRegisterDocs());
        modelMap.put("isSuperior", dataScope.isSuperior());
        modelMap.put("loginName", ShiroUtils.getLoginName());
        return prefix + "/add";
    }

    @Log(title = "伐区采伐申请建档卡立卡/重录", action = "add")
    @ApiOperation(value = "立卡或重录（表单送法）", notes = "唯一保存口：同号同小班二次上报自动升版")
    @PostMapping("/add")
    @RequiresPermissions("flog:flogApplyCard:add")
    @ResponseBody
    public AjaxResult add(TFlogApplyCard record) {
        // 卡号、字号、顺位、核验串、版本序一律不许由界面塞，进来先抹掉
        record.setId(null);
        record.setBillNo(null);
        record.setSiteNo(null);
        record.setNodeNo(null);
        record.setGradeLevel(null);
        record.setContent(null);
        record.setCurrentFlag(null);
        record.setStatus(null);
        record.setRegisterBy(null);
        record.setRegisterTime(null);
        FlogCardSaveVo vo = flogApplyCardService.insertTFlogApplyCard(record);
        return AjaxResult.success(200, vo.getMessage(), vo);
    }

    @Log(title = "伐区采伐申请建档卡立卡/重录(JSON)", action = "add")
    @ApiOperation(value = "立卡或重录（JSON 送法）", notes = "与表单口同一条保存规矩的同名重载送法")
    @PostMapping("/addJson")
    @RequiresPermissions("flog:flogApplyCard:add")
    @ResponseBody
    public AjaxResult addJson(@RequestBody FlogCardSaveDto dto) {
        FlogCardSaveVo vo = flogApplyCardService.insertTFlogApplyCard(dto);
        return AjaxResult.success(200, vo.getMessage(), vo);
    }

    @ApiOperation(value = "重录跳转", notes = "按现行卡带出，提交即升一版；旧版不可原地覆写")
    @GetMapping("/edit/{id}")
    @RequiresPermissions("flog:flogApplyCard:edit")
    public String edit(@PathVariable("id") Long id, ModelMap modelMap) {
        TFlogApplyCard card = flogApplyCardService.selectTFlogApplyCardById(id);
        modelMap.put("card", card);
        modelMap.put("standDocs", visibleInRegisterDocs());
        return prefix + "/edit";
    }

    @Log(title = "伐区采伐申请建档卡重录保存", action = "edit")
    @ApiOperation(value = "重录保存", notes = "旧版退档留检，另立新版；没有原地覆写通道")
    @PostMapping("/edit")
    @RequiresPermissions("flog:flogApplyCard:edit")
    @ResponseBody
    public AjaxResult editSave(TFlogApplyCard record) {
        // 编辑口同样抹掉一切该由系统回填的格子，只留业务栏与源卡id
        record.setBillNo(null);
        record.setSiteNo(null);
        record.setNodeNo(null);
        record.setGradeLevel(null);
        record.setContent(null);
        record.setCurrentFlag(null);
        record.setStatus(null);
        record.setRegisterBy(null);
        record.setRegisterTime(null);
        FlogCardSaveVo vo = flogApplyCardService.updateTFlogApplyCard(record);
        return AjaxResult.success(200, vo.getMessage(), vo);
    }

    @Log(title = "建档卡核对成形", action = "check")
    @ApiOperation(value = "核对成形", notes = "钉核验串，成形后覆不掉")
    @PostMapping("/check/{id}")
    @RequiresPermissions("flog:flogApplyCard:edit")
    @ResponseBody
    public AjaxResult check(@PathVariable("id") Long id) {
        FlogCardSaveVo vo = flogApplyCardService.checkCard(id);
        return AjaxResult.success(200, vo.getMessage(), vo);
    }

    @Log(title = "建档卡压死了断", action = "press")
    @ApiOperation(value = "压死了断", notes = "占用额度原样退回本年账")
    @PostMapping("/press/{id}")
    @RequiresPermissions("flog:flogApplyCard:edit")
    @ResponseBody
    public AjaxResult press(@PathVariable("id") Long id) {
        FlogCardSaveVo vo = flogApplyCardService.pressCard(id);
        return AjaxResult.success(200, vo.getMessage(), vo);
    }

    @ApiOperation(value = "旧版翻检", notes = "一张卡名下各版本，只供事后看")
    @GetMapping("/history")
    @RequiresPermissions("flog:flogApplyCard:view")
    public String history(@RequestParam("billNo") String billNo, ModelMap modelMap) {
        modelMap.put("versions", flogApplyCardService.selectHistoryVersions(billNo));
        modelMap.put("billNo", billNo);
        return prefix + "/history";
    }

    @ApiOperation(value = "额度账跳转", notes = "已扣与尚存同屏")
    @GetMapping("/quotaView")
    @RequiresPermissions("flog:flogApplyCard:view")
    public String quotaView(ModelMap model) {
        model.put("standDocs", visibleInRegisterDocs());
        return prefix + "/quota";
    }

    @Log(title = "年度额度账同屏查询", action = "quota")
    @ApiOperation(value = "额度账数据", notes = "本年可占/已扣/尚存同一回计算返回")
    @GetMapping("/quota")
    @RequiresPermissions("flog:flogApplyCard:view")
    @ResponseBody
    public AjaxResult quota(@RequestParam(value = "siteNo", required = false) String siteNo,
                            @RequestParam(value = "quotaYear", required = false) Integer quotaYear) {
        return AjaxResult.successData(200, flogApplyCardService.quotaView(siteNo, quotaYear));
    }

    @Log(title = "建档卡删除", action = "remove")
    @ApiOperation(value = "逻辑删除", notes = "未压死卡占额随删退回")
    @DeleteMapping("/remove")
    @RequiresPermissions("flog:flogApplyCard:remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        return toAjax(flogApplyCardService.deleteTFlogApplyCardByIds(ids));
    }

    /**
     * 下拉用：当前账号辖域内、在册未收回的底档。上级账号全辖。
     */
    private List<TFlogStandDoc> visibleInRegisterDocs() {
        QueryWrapper<TFlogStandDoc> w = new QueryWrapper<TFlogStandDoc>()
                .eq("del_flag", 0)
                .eq("status", 0)
                .orderByAsc("site_no");
        if (!dataScope.isSuperior()) {
            List<String> nos = dataScope.visibleSiteNos(ShiroUtils.getUserId());
            if (nos.isEmpty()) {
                return java.util.Collections.emptyList();
            }
            w.in("site_no", nos);
        }
        return flogStandDocMapper.selectList(w);
    }
}
