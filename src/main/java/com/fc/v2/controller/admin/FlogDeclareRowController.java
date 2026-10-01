package com.fc.v2.controller.admin;

import java.util.List;

import com.github.pagehelper.PageInfo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.DeleteMapping;
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
import com.fc.v2.model.auto.TFlogDeclareDiff;
import com.fc.v2.model.auto.TFlogDeclareRow;
import com.fc.v2.model.custom.flog.FlogDeclareBatchDto;
import com.fc.v2.model.custom.flog.FlogDeclareRowDto;
import com.fc.v2.model.custom.flog.FlogReceiptVo;
import com.fc.v2.model.custom.flog.FlogRowResultVo;
import com.fc.v2.service.ITFlogDeclareRowService;
import com.fc.v2.util.StringUtils;

/**
 * 伐区申报清册核收 Controller。
 *
 * 一本册子从进门到落库只有业务服务层那条逐行处理的方法；本控制器不编行次、不点已收挂起、
 * 不自行清点回执三数，屏上摆的全跟着服务层给回那一份走。整册提交的入参照旧能用，
 * JSON 换一种递法在同域名下另搭一口（/submitJson、/row），原方法那句一字不动。
 *
 * @author fuce
 * @date 2026-09-14
 */
@Api(value = "伐区申报清册核收")
@Controller
@RequestMapping("/FlogDeclareRowController")
public class FlogDeclareRowController extends BaseController {

    private final String prefix = "admin/flogDeclareRow";

    @Autowired
    private ITFlogDeclareRowService flogDeclareRowService;

    @ApiOperation(value = "分页跳转", notes = "分页跳转")
    @GetMapping("/view")
    @RequiresPermissions("flog:flogDeclareRow:view")
    public String view(ModelMap model) {
        return prefix + "/list";
    }

    @Log(title = "清册现行页逐行查询", action = "list")
    @ApiOperation(value = "现行页查询", notes = "只摆现行页；挂起行直写出自本册第几行")
    @GetMapping("/list")
    @RequiresPermissions("flog:flogDeclareRow:list")
    @ResponseBody
    public ResultTable list(@RequestParam(value = "batchNo", required = false) String batchNo,
                            @RequestParam(value = "siteNo", required = false) String siteNo,
                            @RequestParam(value = "status", required = false) Integer status) {
        startPage();
        List<TFlogDeclareRow> rows = flogDeclareRowService.listCurrentRows(batchNo, siteNo, status);
        PageInfo<TFlogDeclareRow> page = new PageInfo<>(rows);
        return pageTable(page.getList(), page.getTotal());
    }

    @ApiOperation(value = "整册收册跳转", notes = "整册收册跳转")
    @GetMapping("/submitView")
    @RequiresPermissions("flog:flogDeclareRow:add")
    public String submitView() {
        return prefix + "/submit";
    }

    @ApiOperation(value = "逐行递入跳转", notes = "一行一行地落")
    @GetMapping("/rowView")
    @RequiresPermissions("flog:flogDeclareRow:add")
    public String rowView() {
        return prefix + "/row";
    }

    @Log(title = "清册整册提交", action = "submit")
    @ApiOperation(value = "整册提交（表单送法）", notes = "入参照旧：批次号+行清单；越线-1整册退回，空册回执只写空册")
    @PostMapping("/submit")
    @RequiresPermissions("flog:flogDeclareRow:add")
    @ResponseBody
    public AjaxResult submit(FlogDeclareBatchDto dto) {
        return doSubmit(dto);
    }

    @Log(title = "清册整册提交(JSON)", action = "submit")
    @ApiOperation(value = "整册提交（JSON 送法）", notes = "同域名下另搭的一种递法，走的是同一条逐行处理道")
    @PostMapping("/submitJson")
    @RequiresPermissions("flog:flogDeclareRow:add")
    @ResponseBody
    public AjaxResult submitJson(@RequestBody FlogDeclareBatchDto dto) {
        return doSubmit(dto);
    }

    @Log(title = "清册逐行递入", action = "row")
    @ApiOperation(value = "逐行递入（JSON）", notes = "一行递进去，给回定夺已收/挂起/并账/退回")
    @PostMapping("/row")
    @RequiresPermissions("flog:flogDeclareRow:add")
    @ResponseBody
    public AjaxResult row(@RequestBody FlogDeclareRowDto dto) {
        FlogRowResultVo vo = flogDeclareRowService.processRow(dto);
        if (vo.getStatus() == null) {
            // 越线整册退回：行没落，回执落“整册退回”
            return AjaxResult.error(vo.getMessage());
        }
        return AjaxResult.success(200, vo.getMessage(), vo);
    }

    @Log(title = "逐行递入收口出回执", action = "finalize")
    @ApiOperation(value = "收口", notes = "逐行递完后收口，同一回计算出回执三数")
    @PostMapping("/finalize")
    @RequiresPermissions("flog:flogDeclareRow:add")
    @ResponseBody
    public AjaxResult finalize(@RequestParam("batchNo") String batchNo) {
        FlogReceiptVo vo = flogDeclareRowService.finalizeBatch(batchNo);
        return AjaxResult.success(200, vo.getMessage(), vo);
    }

    @Log(title = "清册回执查询", action = "receipt")
    @ApiOperation(value = "回执", notes = "三数取同一次计算那份；账后被改对不平当场点破")
    @GetMapping("/receipt")
    @RequiresPermissions("flog:flogDeclareRow:view")
    @ResponseBody
    public AjaxResult receipt(@RequestParam("batchNo") String batchNo) {
        return AjaxResult.successData(200, flogDeclareRowService.getReceipt(batchNo));
    }

    @Log(title = "挂起明细查询", action = "suspended")
    @ApiOperation(value = "挂起明细", notes = "按行号升序，直写出自本册第几行")
    @GetMapping("/suspended")
    @RequiresPermissions("flog:flogDeclareRow:list")
    @ResponseBody
    public AjaxResult suspended(@RequestParam("batchNo") String batchNo) {
        return AjaxResult.successData(200, flogDeclareRowService.listErrors(batchNo));
    }

    @ApiOperation(value = "往期页跳转", notes = "早年收下的行凭批次号拉出来")
    @GetMapping("/archivedView")
    @RequiresPermissions("flog:flogDeclareRow:view")
    public String archivedView() {
        return prefix + "/archived";
    }

    @Log(title = "往期页查询", action = "archived")
    @ApiOperation(value = "往期页", notes = "现行页不露的早年行，凭批次号/字号/年度拉出")
    @GetMapping("/archived")
    @RequiresPermissions("flog:flogDeclareRow:list")
    @ResponseBody
    public AjaxResult archived(@RequestParam(value = "batchNo", required = false) String batchNo,
                               @RequestParam(value = "siteNo", required = false) String siteNo,
                               @RequestParam(value = "periodYear", required = false) Integer periodYear) {
        return AjaxResult.successData(200,
                flogDeclareRowService.listArchived(batchNo, siteNo, periodYear));
    }

    @Log(title = "往期结转", action = "turn")
    @ApiOperation(value = "往期结转", notes = "收满两个执行期的已收现行页行成页转往期")
    @PostMapping("/turn")
    @RequiresPermissions("flog:flogDeclareRow:edit")
    @ResponseBody
    public AjaxResult turn(@RequestParam(value = "asOfYear", required = false) Integer asOfYear) {
        int n = flogDeclareRowService.turnArchived(asOfYear);
        return AjaxResult.success("结转完成，共转走 " + n + " 行到往期页");
    }

    @ApiOperation(value = "差异清单跳转", notes = "蓄积要改走差异清单")
    @GetMapping("/diffView")
    @RequiresPermissions("flog:flogDeclareRow:view")
    public String diffView() {
        return prefix + "/diff";
    }

    @Log(title = "差异清单查询", action = "diff")
    @GetMapping("/diff")
    @RequiresPermissions("flog:flogDeclareRow:view")
    @ResponseBody
    public AjaxResult diff(@RequestParam(value = "status", required = false) Integer status) {
        return AjaxResult.successData(200, flogDeclareRowService.listDiffs(status));
    }

    @Log(title = "差异登记", action = "diffRegister")
    @ApiOperation(value = "差异登记", notes = "重送不覆盖，改蓄积在此登记")
    @PostMapping("/diff/register")
    @RequiresPermissions("flog:flogDeclareRow:add")
    @ResponseBody
    public AjaxResult diffRegister(@RequestParam("batchNo") String batchNo,
                                   @RequestParam("rowNo") Integer rowNo,
                                   @RequestParam(value = "diffKind", required = false) Integer diffKind,
                                   @RequestParam("newQty") java.math.BigDecimal newQty,
                                   @RequestParam(value = "remark", required = false) String remark) {
        TFlogDeclareDiff d = flogDeclareRowService.registerDiff(batchNo, rowNo, diffKind, newQty, remark);
        return AjaxResult.success(200, "差异已登记，待办结", d);
    }

    @Log(title = "差异办结", action = "diffHandle")
    @ApiOperation(value = "差异办结", notes = "approve 按新数入帐，否则驳回")
    @PostMapping("/diff/handle/{id}")
    @RequiresPermissions("flog:flogDeclareRow:edit")
    @ResponseBody
    public AjaxResult diffHandle(@PathVariable("id") Long id,
                                 @RequestParam("approve") boolean approve,
                                 @RequestParam(value = "remark", required = false) String remark) {
        TFlogDeclareDiff d = flogDeclareRowService.handleDiff(id, approve, remark);
        String msg = approve ? "差异已按新数入帐" : "差异已驳回";
        return AjaxResult.success(200, msg, d);
    }

    private AjaxResult doSubmit(FlogDeclareBatchDto dto) {
        if (dto == null || StringUtils.isEmpty(dto.getBatchNo())) {
            return AjaxResult.error("没给清册批次号，册子进不了门");
        }
        // 行上的行次、状态、代号、缘故、执行期、账页一律不由界面塞：服务层只取业务栏，其余进门前抹掉。
        // recvQty 是业务栏（实收可少于申报，服务层仍守“不得大过申报”），不在抹除之列。
        if (dto.getRows() != null) {
            for (TFlogDeclareRow r : dto.getRows()) {
                r.setId(null);
                r.setRowNo(null);
                r.setStatus(null);
                r.setItemCode(null);
                r.setRemark(null);
                r.setPeriodYear(null);
                r.setPageFlag(null);
                r.setMergeOfRowNo(null);
            }
        }
        int n = flogDeclareRowService.submitBatch(dto.getBatchNo(), dto.getRows());
        FlogReceiptVo receipt = flogDeclareRowService.getReceipt(dto.getBatchNo());
        if (n == -1) {
            return AjaxResult.error(receipt.getMessage());
        }
        return AjaxResult.success(200, receipt.getMessage(), receipt);
    }
}
