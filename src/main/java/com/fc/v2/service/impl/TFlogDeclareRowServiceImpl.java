package com.fc.v2.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fc.v2.common.exception.biz.FlogBizException;
import com.fc.v2.mapper.auto.TFlogDeclareBatchMapper;
import com.fc.v2.mapper.auto.TFlogDeclareDiffMapper;
import com.fc.v2.mapper.auto.TFlogDeclareRowMapper;
import com.fc.v2.model.auto.TFlogDeclareBatch;
import com.fc.v2.model.auto.TFlogDeclareDiff;
import com.fc.v2.model.auto.TFlogDeclareRow;
import com.fc.v2.model.custom.flog.FlogCutMode;
import com.fc.v2.model.custom.flog.FlogDeclareRowDto;
import com.fc.v2.model.custom.flog.FlogReceiptVo;
import com.fc.v2.model.custom.flog.FlogRowResultVo;
import com.fc.v2.service.ITFlogDeclareRowService;
import com.fc.v2.util.StringUtils;

/**
 * 伐区申报清册核收行 Service业务层处理。
 *
 * 一本册子从进门到落库只有一条道：{@link #processRow} 这一条逐行处理的方法。
 * 一行递进去，行次按进门顺序钉、同册重复并一行、前置项各按代号挂起、收回数不许越过申报，
 * 全在这一处定夺；整册提交 {@link #submitBatch} 也只是在同一事务里把这条道一行行走完。
 * 回执三数与库里逐行重数同一把尺，界面另起一套数、手工补/涂一行的口子一概不留。
 *
 * @author fuce
 * @date 2026-09-14
 */
@Service
public class TFlogDeclareRowServiceImpl implements ITFlogDeclareRowService {

    /** 单册行数上限：越线整册退回、一行不留 */
    private static final int MAX_ROWS = 500;

    /** 行落地：待核 */
    private static final int STATUS_PENDING = 0;
    /** 行落地：已收 */
    private static final int STATUS_ACCEPTED = 1;
    /** 行落地：挂起 */
    private static final int STATUS_SUSPENDED = 2;

    /** 账页：现行页 */
    private static final int PAGE_CURRENT = 0;
    /** 账页：往期页 */
    private static final int PAGE_ARCHIVED = 1;

    /** 收册：收册中 */
    private static final int RECV_RECEIVING = 0;
    /** 收册：核讫 */
    private static final int RECV_DONE = 1;
    /** 收册：空册 */
    private static final int RECV_EMPTY = 2;
    /** 收册：整册退回 */
    private static final int RECV_REJECTED = 3;

    /** 林种：公益林（依法只许抚育、更新、低质低效林改造性质的采伐） */
    private static final int FOREST_WELFARE = 1;

    /** 前置项代号：公益林走了主伐 */
    private static final String ITEM_CUT_MODE = "PRE_CUT_MODE";
    /** 前置项代号：权属在诉 */
    private static final String ITEM_OWNERSHIP = "PRE_OWNERSHIP";
    /** 前置项代号：超省级限额未附调查设计材料 */
    private static final String ITEM_SURVEY = "PRE_SURVEY";

    /** 省级规定单小班拟采蓄积上限（立方米，等于不越、越过才算超） */
    private static final BigDecimal PROVINCE_MAX_QTY = new BigDecimal("300");
    /** 省级规定单小班拟采面积上限（公顷） */
    private static final BigDecimal PROVINCE_MAX_AREA = new BigDecimal("30");

    /** 差异：蓄积调整 */
    private static final int DIFF_QTY = 1;
    /** 差异办理：待办 */
    private static final int DIFF_TODO = 0;
    /** 差异办理：已入帐 */
    private static final int DIFF_DONE = 1;
    /** 差异办理：驳回 */
    private static final int DIFF_REJECTED = 2;

    @javax.annotation.Resource
    private TFlogDeclareRowMapper rowMapper;

    @javax.annotation.Resource
    private TFlogDeclareBatchMapper batchMapper;

    @javax.annotation.Resource
    private TFlogDeclareDiffMapper diffMapper;

    @Override
    public TFlogDeclareRow selectTFlogDeclareRowById(Long id) {
        return this.rowMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FlogRowResultVo processRow(FlogDeclareRowDto dto) {
        validateRow(dto);
        TFlogDeclareBatch batch = lockBatch(dto.getBatchNo(), true);
        return doProcessRow(dto, batch);
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int submitBatch(String batchNo, List<TFlogDeclareRow> rows) {
        if (StringUtils.isEmpty(batchNo)) {
            throw new FlogBizException("没给清册批次号，册子进不了门");
        }
        // 一册一行也没有：回执只写空册，绝不许写核讫
        if (rows == null || rows.isEmpty()) {
            TFlogDeclareBatch batch = lockBatch(batchNo, true);
            assertReceiving(batch);
            finalizeReceipt(batch);
            return 0;
        }
        // 行数越线：整册退回、一行不留（行还没进门，本事务里也不会落任何一行）
        if (rows.size() > MAX_ROWS) {
            TFlogDeclareBatch batch = lockBatch(batchNo, true);
            assertReceiving(batch);
            rejectBatch(batch, "本册 " + rows.size() + " 行，越过单册 " + MAX_ROWS
                    + " 行上限，整册退回、一行不留");
            return -1;
        }
        // 一册只许一家：字号由各行自带，混进别册的行当场点破
        String siteNo = null;
        for (TFlogDeclareRow r : rows) {
            if (r != null && StringUtils.isNotEmpty(r.getSiteNo())) {
                if (siteNo == null) {
                    siteNo = r.getSiteNo().trim();
                } else if (!siteNo.equals(r.getSiteNo().trim())) {
                    throw new FlogBizException("批次号 " + batchNo
                            + " 里混进了别的编制单位的行，一册的行不许挪进别册，本册不收");
                }
            }
        }
        if (siteNo == null) {
            throw new FlogBizException("批次号 " + batchNo + " 的行上没给编制单位底档字号，落不了账");
        }
        TFlogDeclareBatch batch = lockBatch(batchNo, true);
        assertReceiving(batch);

        int accepted = 0;
        boolean rejected = false;
        for (TFlogDeclareRow r : rows) {
            FlogDeclareRowDto dto = toDto(batchNo, siteNo, r);
            // 两种递法一把尺：整册送法缺栏同样当场点破，不叫坏行混进门
            validateRow(dto);
            FlogRowResultVo vo = doProcessRow(dto, batch);
            if (vo.getStatus() == null) {
                // 越线整册退回：头里已落的行一行不留，册头钉死，余下行不再进门
                rejected = true;
                break;
            }
            if (vo.getStatus() == STATUS_ACCEPTED) {
                accepted++;
            }
        }
        if (rejected) {
            return -1;
        }
        // 走完逐行这条道，回执三数同一回计算收口
        FlogReceiptVo receipt = finalizeReceipt(batch);
        return receipt.getAcceptedRows() == null ? accepted : receipt.getAcceptedRows();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FlogReceiptVo finalizeBatch(String batchNo) {
        if (StringUtils.isEmpty(batchNo)) {
            throw new FlogBizException("没给清册批次号，收口收不到册子");
        }
        TFlogDeclareBatch batch = lockBatch(batchNo, false);
        if (batch.getRecvStatus() != null && batch.getRecvStatus() != RECV_RECEIVING) {
            return toReceiptVo(batch);
        }
        return finalizeReceipt(batch);
    }

    @Override
    public FlogReceiptVo getReceipt(String batchNo) {
        TFlogDeclareBatch batch = requireBatch(batchNo);
        int[] cnt = countRows(batchNo);
        // 还在收册中：回执尚没出过，给实时进门数，不拿账头旧零数去对账
        if (batch.getRecvStatus() == null || batch.getRecvStatus() == RECV_RECEIVING) {
            batch.setTotalRows(cnt[0]);
            batch.setAcceptedRows(cnt[1]);
            batch.setSuspendedRows(cnt[2]);
            return toReceiptVo(batch);
        }
        // 回执三数必须同库里逐行算出的是同一把尺；收完之后账被动过、对不平，当场点破，
        // 再不叫“究竟错在收时还是收后动过、没人说得清”那桩旧事重演。
        Integer storedTotal = nz(batch.getTotalRows());
        Integer storedAccepted = nz(batch.getAcceptedRows());
        Integer storedSuspended = nz(batch.getSuspendedRows());
        if (batch.getRecvStatus() != null && batch.getRecvStatus() == RECV_EMPTY) {
            if (cnt[0] != 0 || storedTotal != 0) {
                throw new FlogBizException("批次号 " + batchNo
                        + " 回执写的是空册，库里却有行，账被动过，回执作不得数");
            }
        } else if (storedTotal != cnt[0] || storedAccepted != cnt[1] || storedSuspended != cnt[2]) {
            throw new FlogBizException("批次号 " + batchNo + " 回执上的数（总 " + storedTotal + "/收 "
                    + storedAccepted + "/挂 " + storedSuspended + "）同库里逐行数出的（总 " + cnt[0]
                    + "/收 " + cnt[1] + "/挂 " + cnt[2] + "）对不平，账在收讫后被动过");
        }
        return toReceiptVo(batch);
    }

    @Override
    public List<TFlogDeclareRow> listCurrentRows(String batchNo, String siteNo, Integer status) {
        QueryWrapper<TFlogDeclareRow> w = new QueryWrapper<TFlogDeclareRow>()
                .eq("page_flag", PAGE_CURRENT)
                .eq("del_flag", 0)
                .eq(StringUtils.isNotEmpty(batchNo), "batch_no", batchNo)
                .eq(StringUtils.isNotEmpty(siteNo), "site_no", siteNo)
                .eq(status != null, "status", status)
                .orderByAsc("batch_no", "row_no");
        return this.rowMapper.selectList(w);
    }

    @Override
    public List<TFlogDeclareRow> listErrors(String batchNo) {
        // 挂起只摆头行：重复并账行不算新的挂起一笔
        return this.rowMapper.selectList(new QueryWrapper<TFlogDeclareRow>()
                .eq("batch_no", batchNo)
                .eq("status", STATUS_SUSPENDED)
                .isNull("merge_of_row_no")
                .eq("del_flag", 0)
                .orderByAsc("row_no"));
    }

    @Override
    public List<TFlogDeclareRow> listArchived(String batchNo, String siteNo, Integer periodYear) {
        QueryWrapper<TFlogDeclareRow> w = new QueryWrapper<TFlogDeclareRow>()
                .eq("page_flag", PAGE_ARCHIVED)
                .eq("del_flag", 0)
                .eq(StringUtils.isNotEmpty(batchNo), "batch_no", batchNo)
                .eq(StringUtils.isNotEmpty(siteNo), "site_no", siteNo)
                .eq(periodYear != null, "period_year", periodYear)
                .orderByAsc("batch_no", "row_no");
        return this.rowMapper.selectList(w);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int turnArchived(Integer asOfYear) {
        int year = asOfYear == null ? yearOf(new Date()) : asOfYear;
        // 进门那年起满两个执行期：period_year <= asOfYear-2，现行页不露，成页转往期
        int cutoff = year - 2;
        return this.rowMapper.update(null, new UpdateWrapper<TFlogDeclareRow>()
                .eq("page_flag", PAGE_CURRENT)
                .eq("status", STATUS_ACCEPTED)
                .le("period_year", cutoff)
                .eq("del_flag", 0)
                .set("page_flag", PAGE_ARCHIVED));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TFlogDeclareDiff registerDiff(String batchNo, Integer rowNo, Integer diffKind,
                                         BigDecimal newQty, String remark) {
        if (StringUtils.isEmpty(batchNo) || rowNo == null) {
            throw new FlogBizException("差异单得指明批次号与原清册行次");
        }
        if (newQty == null || newQty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new FlogBizException("改成的蓄积得是正数");
        }
        TFlogDeclareBatch batch = requireBatch(batchNo);
        if (batch.getRecvStatus() == null || batch.getRecvStatus() == RECV_RECEIVING
                || batch.getRecvStatus() == RECV_REJECTED) {
            throw new FlogBizException("批次号 " + batchNo + " 尚未收讫或已整册退回，没有落账行可提差异");
        }
        TFlogDeclareRow row = findRow(batchNo, rowNo);
        if (row.getMergeOfRowNo() != null) {
            throw new FlogBizException("第 " + rowNo + " 行是并进第 " + row.getMergeOfRowNo()
                    + " 行的重复行，蓄积只在头行落一次，差异提在头行上");
        }
        if (row.getStatus() == null || row.getStatus() != STATUS_ACCEPTED) {
            throw new FlogBizException("第 " + rowNo + " 行不是已收行，挂起行不走差异清单，先把前置项了断");
        }
        BigDecimal oldQty = row.getRecvQty() == null ? row.getQty() : row.getRecvQty();
        if (newQty.compareTo(oldQty) == 0) {
            throw new FlogBizException("新数与原数一致，算不上差异");
        }
        // 差异改的是收回数，照样不许越过本行原申报
        if (newQty.compareTo(row.getQty()) > 0) {
            throw new FlogBizException("第 " + rowNo + " 行原申报 " + row.getQty()
                    + "，改成 " + newQty + " 大过申报，多的不许硬塞进来");
        }
        TFlogDeclareDiff diff = new TFlogDeclareDiff();
        diff.setBatchNo(batchNo);
        diff.setSiteNo(row.getSiteNo());
        diff.setSubcompNo(row.getSubcompNo());
        diff.setRowNo(rowNo);
        diff.setDiffKind(diffKind == null ? DIFF_QTY : diffKind);
        diff.setOldQty(oldQty);
        diff.setNewQty(newQty);
        diff.setStatus(DIFF_TODO);
        diff.setDelFlag(0);
        diff.setRemark(remark);
        this.diffMapper.insert(diff);
        return diff;
    }

    @Override
    public List<TFlogDeclareDiff> listDiffs(Integer status) {
        return this.diffMapper.selectList(new QueryWrapper<TFlogDeclareDiff>()
                .eq("del_flag", 0)
                .eq(status != null, "status", status)
                .orderByDesc("create_time"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TFlogDeclareDiff handleDiff(Long id, boolean approve, String remark) {
        if (id == null) {
            throw new FlogBizException("没给差异单id");
        }
        TFlogDeclareDiff diff = this.diffMapper.selectOne(new QueryWrapper<TFlogDeclareDiff>()
                .eq("id", id)
                .eq("del_flag", 0)
                .last("limit 1 for update"));
        if (diff == null) {
            throw new FlogBizException("差异单 " + id + " 不存在");
        }
        if (diff.getStatus() != null && diff.getStatus() != DIFF_TODO) {
            throw new FlogBizException("差异单已经办结，不许再翻案");
        }
        if (approve) {
            TFlogDeclareRow row = findRow(diff.getBatchNo(), diff.getRowNo());
            if (row.getStatus() == null || row.getStatus() != STATUS_ACCEPTED
                    || row.getMergeOfRowNo() != null) {
                throw new FlogBizException("第 " + diff.getRowNo() + " 行已不是可改账的已收头行，差异驳回");
            }
            if (diff.getNewQty().compareTo(row.getQty()) > 0) {
                throw new FlogBizException("第 " + diff.getRowNo() + " 行核准数大过原申报，入不了账");
            }
            this.rowMapper.update(null, new UpdateWrapper<TFlogDeclareRow>()
                    .eq("id", row.getId())
                    .set("recv_qty", diff.getNewQty()));
            diff.setStatus(DIFF_DONE);
        } else {
            diff.setStatus(DIFF_REJECTED);
        }
        diff.setHandleBy(currentLoginName());
        diff.setHandleTime(new Date());
        if (StringUtils.isNotEmpty(remark)) {
            diff.setRemark(remark);
        }
        this.diffMapper.updateById(diff);
        return diff;
    }

    // ------------------------------------------------------------------
    // 唯一逐行处理口
    // ------------------------------------------------------------------

    /**
     * 一行落账：调用前已校验报文、已 FOR UPDATE 锁住批次头。
     * 行次、已收/挂起、前置项代号、缘故、执行期、账页全在这里落，别处碰不着。
     */
    private FlogRowResultVo doProcessRow(FlogDeclareRowDto dto, TFlogDeclareBatch batch) {
        int rowNo = nz(batch.getMaxRowNo()) + 1;
        if (rowNo > MAX_ROWS) {
            // 越线整册退回：先前逐行递入的也一并清空，一行不留。
            // 不抛异常（抛了这回的“退回”会跟着回滚）；给回 status=null，调用方见此即止。
            rejectBatch(batch, "批次号 " + batch.getBatchNo() + " 行数越过单册 " + MAX_ROWS
                    + " 行上限，整册退回、一行不留");
            FlogRowResultVo rejectVo = new FlogRowResultVo();
            rejectVo.setBatchNo(batch.getBatchNo());
            rejectVo.setRowNo(rowNo);
            rejectVo.setSiteNo(batch.getSiteNo());
            rejectVo.setMessage("批次号 " + batch.getBatchNo() + " 已有 " + nz(batch.getMaxRowNo())
                    + " 行，再进一行便越过单册 " + MAX_ROWS + " 行上限，整册退回、一行不留");
            return rejectVo;
        }
        if (StringUtils.isEmpty(batch.getSiteNo())) {
            batch.setSiteNo(dto.getSiteNo().trim());
        } else if (!batch.getSiteNo().equals(dto.getSiteNo().trim())) {
            throw new FlogBizException("批次号 " + batch.getBatchNo() + " 册下是 " + batch.getSiteNo()
                    + " 的行，" + dto.getSiteNo() + " 的行不许挪进这一册");
        }
        int year = yearOf(new Date());
        if (batch.getPeriodYear() == null) {
            batch.setPeriodYear(year);
        }

        // 同一家名下同一小班在一册里占两个行目：账上并成一行，蓄积只落一次，行次认前头那一个
        TFlogDeclareRow head = this.rowMapper.selectOne(new QueryWrapper<TFlogDeclareRow>()
                .eq("batch_no", batch.getBatchNo())
                .eq("site_no", batch.getSiteNo())
                .eq("subcomp_no", dto.getSubcompNo().trim())
                .isNull("merge_of_row_no")
                .eq("del_flag", 0)
                .orderByAsc("row_no")
                .last("limit 1"));

        TFlogDeclareRow row = newRowBase(batch, rowNo, dto, year);
        FlogRowResultVo vo;
        if (head != null) {
            row.setMergeOfRowNo(head.getRowNo());
            // 并账行随头行落同一种情形，但收回数不落第二遍；缘故也只随头行那一句，不另追
            row.setStatus(head.getStatus());
            row.setItemCode(head.getItemCode());
            row.setRemark(head.getRemark());
            row.setQty(dto.getQty());
            row.setRecvQty(null);
            this.rowMapper.insert(row);
            vo = buildRowVo(row);
            vo.setMessage("批次号 " + batch.getBatchNo() + " 第 " + rowNo + " 行与第 " + head.getRowNo()
                    + " 行是同一家同一小班，账上并作一行，蓄积只在第 " + head.getRowNo() + " 行落一次");
        } else {
            String[] hr = checkPrerequisite(dto);
            if (hr[0] != null) {
                // 一行不合格只挂那一行：各按各的前置项代号落，缘故一句话；同册余下行照常入账
                row.setStatus(STATUS_SUSPENDED);
                row.setItemCode(hr[0]);
                row.setRemark(hr[1]);
                row.setRecvQty(null);
                this.rowMapper.insert(row);
                vo = buildRowVo(row);
                vo.setMessage("批次号 " + batch.getBatchNo() + " 第 " + rowNo + " 行挂起（" + hr[0]
                        + "）：" + hr[1] + "；同册余下行照常入账");
            } else {
                BigDecimal recvQty = dto.getRecvQty() == null ? dto.getQty() : dto.getRecvQty();
                if (recvQty.compareTo(dto.getQty()) > 0) {
                    // 收的不能大过报的：多的既不塞进本行，也不准挪去应付邻行
                    throw new FlogBizException("批次号 " + batch.getBatchNo() + " 第 " + rowNo
                            + " 行申报 " + dto.getQty() + "，核收 " + recvQty
                            + "，收回数大过申报数，多的那截既不许塞进本行也不准挪去邻行");
                }
                row.setStatus(STATUS_ACCEPTED);
                row.setRecvQty(recvQty);
                this.rowMapper.insert(row);
                vo = buildRowVo(row);
                vo.setMessage("批次号 " + batch.getBatchNo() + " 第 " + rowNo + " 行已收，核收蓄积 "
                        + recvQty + " 立方米");
            }
        }

        // 行次进门一刻钉死，续在批次头上；谁查挂起那笔，屏上直写出自本册第几行
        batch.setMaxRowNo(rowNo);
        this.batchMapper.updateById(batch);
        return vo;
    }

    /**
     * 前置项核验，返回 [代号, 一句话缘故]；都过得去返回 [null, null]。
     * 三种挂起各按各的代号，命中头一条即挂（一行只挂一笔、一句缘故）。
     */
    private String[] checkPrerequisite(FlogDeclareRowDto dto) {
        FlogCutMode mode = FlogCutMode.of(dto.getCutMode());
        // 1) 公益林走了主伐：现行法只许抚育、更新与低质低效林改造性质的采伐
        if (dto.getForestKind() != null && dto.getForestKind() == FOREST_WELFARE
                && (mode == FlogCutMode.CLEAR || mode == FlogCutMode.GRADUAL || mode == FlogCutMode.SELECTIVE)) {
            return new String[] {ITEM_CUT_MODE,
                    "公益林不得走主伐（皆伐/渐伐/择伐），依法只许抚育采伐、更新采伐与低质低效林改造性质的采伐"};
        }
        // 2) 权属还打着官司
        if (dto.getDisputeFlag() != null && dto.getDisputeFlag() == 1) {
            return new String[] {ITEM_OWNERSHIP, "该小班权属尚在诉讼，权属落定前挂起不收"};
        }
        // 3) 超省级规定的面积或蓄积量，却没附伐区调查设计材料
        boolean overQty = dto.getQty().compareTo(PROVINCE_MAX_QTY) > 0;
        boolean overArea = dto.getArea() != null && dto.getArea().compareTo(PROVINCE_MAX_AREA) > 0;
        if ((overQty || overArea) && (dto.getSurveyFlag() == null || dto.getSurveyFlag() != 1)) {
            StringBuilder why = new StringBuilder("本行");
            if (overQty) {
                why.append("申报拟采蓄积 ").append(dto.getQty()).append(" 立方米越过省级规定 ")
                        .append(PROVINCE_MAX_QTY).append(" 立方米");
            }
            if (overArea) {
                if (overQty) {
                    why.append("，且");
                }
                why.append("申报拟采面积 ").append(dto.getArea()).append(" 公顷越过省级规定 ")
                        .append(PROVINCE_MAX_AREA).append(" 公顷");
            }
            why.append("，未附伐区调查设计材料，补齐前挂起");
            return new String[] {ITEM_SURVEY, why.toString()};
        }
        return new String[] {null, null};
    }

    private TFlogDeclareRow newRowBase(TFlogDeclareBatch batch, int rowNo, FlogDeclareRowDto dto, int year) {
        TFlogDeclareRow row = new TFlogDeclareRow();
        // 界面塞的行次、状态、代号、缘故一律不认，到这里全部由本层给
        row.setBatchNo(batch.getBatchNo());
        row.setRowNo(rowNo);
        row.setSiteNo(batch.getSiteNo());
        row.setSubcompNo(dto.getSubcompNo().trim());
        row.setCutMode(dto.getCutMode());
        row.setForestKind(dto.getForestKind());
        row.setDisputeFlag(dto.getDisputeFlag() == null ? 0 : dto.getDisputeFlag());
        row.setArea(dto.getArea());
        row.setSurveyFlag(dto.getSurveyFlag() == null ? 0 : dto.getSurveyFlag());
        row.setQty(dto.getQty());
        row.setPeriodYear(batch.getPeriodYear() == null ? year : batch.getPeriodYear());
        row.setPageFlag(PAGE_CURRENT);
        row.setDelFlag(0);
        return row;
    }

    private FlogRowResultVo buildRowVo(TFlogDeclareRow row) {
        FlogRowResultVo vo = new FlogRowResultVo();
        vo.setBatchNo(row.getBatchNo());
        vo.setRowNo(row.getRowNo());
        vo.setSiteNo(row.getSiteNo());
        vo.setSubcompNo(row.getSubcompNo());
        vo.setStatus(row.getStatus());
        vo.setStatusText(statusText(row.getStatus()));
        vo.setItemCode(row.getItemCode());
        vo.setReason(row.getRemark());
        vo.setQty(row.getQty());
        vo.setRecvQty(row.getRecvQty());
        vo.setMergedIntoRowNo(row.getMergeOfRowNo());
        vo.setCreateTime(new Date());
        return vo;
    }

    // ------------------------------------------------------------------
    // 批次头、回执、计数
    // ------------------------------------------------------------------

    /**
     * 锁批次头（FOR UPDATE），串行化同一册的逐行递入；create=true 时头一回进门先立头。
     * 已收讫（核讫/空册/整册退回）的册当场挡住：第二遍不覆盖、不给挂起行追第二句缘故。
     */
    private TFlogDeclareBatch lockBatch(String batchNo, boolean create) {
        TFlogDeclareBatch batch = this.batchMapper.selectOne(new QueryWrapper<TFlogDeclareBatch>()
                .eq("batch_no", batchNo)
                .last("limit 1 for update"));
        if (batch == null) {
            if (!create) {
                throw new FlogBizException("批次号 " + batchNo + " 从没进门，查无此册");
            }
            batch = new TFlogDeclareBatch();
            batch.setBatchNo(batchNo);
            batch.setTotalRows(0);
            batch.setAcceptedRows(0);
            batch.setSuspendedRows(0);
            batch.setMaxRowNo(0);
            batch.setRecvStatus(RECV_RECEIVING);
            batch.setDelFlag(0);
            try {
                this.batchMapper.insert(batch);
            } catch (DataIntegrityViolationException dup) {
                // 并发送册撞唯一号：谁先谁后排队重取，落不出两个头
                batch = this.batchMapper.selectOne(new QueryWrapper<TFlogDeclareBatch>()
                        .eq("batch_no", batchNo)
                        .last("limit 1 for update"));
                if (batch == null) {
                    throw dup;
                }
            }
        }
        if (batch.getDelFlag() != null && batch.getDelFlag() == 1) {
            throw new FlogBizException("批次号 " + batchNo + " 已删，收不了册");
        }
        if (batch.getRecvStatus() != null && batch.getRecvStatus() != RECV_RECEIVING) {
            throw new FlogBizException("批次号 " + batchNo + " " + recvStatusText(batch.getRecvStatus())
                    + "，账上只认头一遍那批行，重送不覆盖、不追第二句缘故；蓄积真要改，走差异清单");
        }
        return batch;
    }

    private void assertReceiving(TFlogDeclareBatch batch) {
        // lockBatch 已挡，留一道显式口子给空册/越线分支前的语义点破
        if (batch.getRecvStatus() == null || batch.getRecvStatus() != RECV_RECEIVING) {
            throw new FlogBizException("批次号 " + batch.getBatchNo() + " 已收讫，不许再往里递行");
        }
    }

    private TFlogDeclareBatch requireBatch(String batchNo) {
        if (StringUtils.isEmpty(batchNo)) {
            throw new FlogBizException("没给清册批次号");
        }
        TFlogDeclareBatch batch = this.batchMapper.selectOne(new QueryWrapper<TFlogDeclareBatch>()
                .eq("batch_no", batchNo)
                .eq("del_flag", 0)
                .last("limit 1"));
        if (batch == null) {
            throw new FlogBizException("批次号 " + batchNo + " 从没进门，查无此册");
        }
        return batch;
    }

    private TFlogDeclareRow findRow(String batchNo, int rowNo) {
        TFlogDeclareRow row = this.rowMapper.selectOne(new QueryWrapper<TFlogDeclareRow>()
                .eq("batch_no", batchNo)
                .eq("row_no", rowNo)
                .eq("del_flag", 0)
                .last("limit 1"));
        if (row == null) {
            throw new FlogBizException("批次号 " + batchNo + " 第 " + rowNo + " 行查无此行");
        }
        return row;
    }

    /** 越线整册退回：批次头记退回，已落行一行不留。 */
    private void rejectBatch(TFlogDeclareBatch batch, String reason) {
        this.rowMapper.delete(new QueryWrapper<TFlogDeclareRow>()
                .eq("batch_no", batch.getBatchNo()));
        batch.setTotalRows(0);
        batch.setAcceptedRows(0);
        batch.setSuspendedRows(0);
        batch.setMaxRowNo(0);
        batch.setRecvStatus(RECV_REJECTED);
        batch.setRecvTime(new Date());
        batch.setRemark(reason);
        this.batchMapper.updateById(batch);
    }

    /**
     * 回执收口：库里逐行重数（挂起 + 收下 = 原册行数），同一回计算写死在批次头上。
     */
    private FlogReceiptVo finalizeReceipt(TFlogDeclareBatch batch) {
        int[] cnt = countRows(batch.getBatchNo());
        int total = cnt[0];
        int accepted = cnt[1];
        int suspended = cnt[2];
        if (accepted + suspended != total) {
            // 计数只认 status=1/2 两类落地行；有待核或对不平的行，回执不许出
            throw new FlogBizException("批次号 " + batch.getBatchNo() + " 收下 " + accepted + " 行加挂起 "
                    + suspended + " 行抵不上原册 " + total + " 行，账对不平，回执出不了");
        }
        batch.setTotalRows(total);
        batch.setAcceptedRows(accepted);
        batch.setSuspendedRows(suspended);
        batch.setRecvStatus(total == 0 ? RECV_EMPTY : RECV_DONE);
        batch.setRecvTime(new Date());
        this.batchMapper.updateById(batch);
        return toReceiptVo(batch);
    }

    /**
     * 同一把尺：总数=本册全部行（重复并账行随头行计 status，故左右自平）；
     * 收下=status=1，挂起=status=2。两个出口只准由这一处数。
     */
    private int[] countRows(String batchNo) {
        List<TFlogDeclareRow> all = this.rowMapper.selectList(new QueryWrapper<TFlogDeclareRow>()
                .select("status")
                .eq("batch_no", batchNo)
                .eq("del_flag", 0));
        int total = all.size();
        int accepted = 0;
        int suspended = 0;
        for (TFlogDeclareRow r : all) {
            if (r.getStatus() != null && r.getStatus() == STATUS_ACCEPTED) {
                accepted++;
            } else if (r.getStatus() != null && r.getStatus() == STATUS_SUSPENDED) {
                suspended++;
            }
        }
        return new int[] {total, accepted, suspended};
    }

    private FlogReceiptVo toReceiptVo(TFlogDeclareBatch batch) {
        FlogReceiptVo vo = new FlogReceiptVo();
        vo.setBatchNo(batch.getBatchNo());
        vo.setSiteNo(batch.getSiteNo());
        vo.setPeriodYear(batch.getPeriodYear());
        vo.setRecvStatus(batch.getRecvStatus());
        vo.setRecvStatusText(recvStatusText(batch.getRecvStatus()));
        vo.setTotalRows(nz(batch.getTotalRows()));
        vo.setAcceptedRows(nz(batch.getAcceptedRows()));
        vo.setSuspendedRows(nz(batch.getSuspendedRows()));
        vo.setRecvTime(batch.getRecvTime());
        String msg;
        if (batch.getRecvStatus() != null && batch.getRecvStatus() == RECV_EMPTY) {
            // 空册就是空册，绝不许写“核讫”——从前拿那三个字当全辖收毕出过事
            msg = "批次号 " + batch.getBatchNo() + "：空册";
        } else if (batch.getRecvStatus() != null && batch.getRecvStatus() == RECV_REJECTED) {
            msg = StringUtils.isNotEmpty(batch.getRemark()) ? batch.getRemark()
                    : "批次号 " + batch.getBatchNo() + "：整册退回、一行不留";
        } else if (batch.getRecvStatus() != null && batch.getRecvStatus() == RECV_RECEIVING) {
            msg = "批次号 " + batch.getBatchNo() + "：收册中，已进门 " + nz(batch.getMaxRowNo()) + " 行";
        } else {
            msg = "批次号 " + batch.getBatchNo() + " 核讫：本册总计 " + nz(batch.getTotalRows())
                    + " 行，收下 " + nz(batch.getAcceptedRows()) + " 行，挂起 "
                    + nz(batch.getSuspendedRows()) + " 行";
        }
        vo.setMessage(msg);
        return vo;
    }

    private void validateRow(FlogDeclareRowDto dto) {
        if (dto == null) {
            throw new FlogBizException("递上来的行是空的，没东西可落");
        }
        List<String> missing = new ArrayList<>();
        if (StringUtils.isEmpty(dto.getBatchNo())) {
            missing.add("清册批次号");
        }
        if (StringUtils.isEmpty(dto.getSiteNo())) {
            missing.add("编制单位底档字号");
        }
        if (StringUtils.isEmpty(dto.getSubcompNo())) {
            missing.add("小班号");
        }
        if (FlogCutMode.of(dto.getCutMode()) == null) {
            missing.add("采伐方式（六类选一）");
        }
        if (dto.getForestKind() == null || (dto.getForestKind() != 1 && dto.getForestKind() != 2)) {
            missing.add("林种性质（公益林/商品林）");
        }
        if (!missing.isEmpty()) {
            throw new FlogBizException("这一行落不了账，缺了便起不来的格子：" + String.join("、", missing));
        }
        if (dto.getQty() == null || dto.getQty().compareTo(BigDecimal.ZERO) <= 0) {
            throw new FlogBizException("申报拟采蓄积得是正数，零、负数落不了账");
        }
        if (dto.getArea() != null && dto.getArea().compareTo(BigDecimal.ZERO) <= 0) {
            throw new FlogBizException("申报拟采面积若填，得是正数");
        }
        if (dto.getRecvQty() != null && dto.getRecvQty().compareTo(BigDecimal.ZERO) <= 0) {
            throw new FlogBizException("核收蓄积若填，得是正数；不填按申报数收");
        }
    }

    private FlogDeclareRowDto toDto(String batchNo, String siteNo, TFlogDeclareRow r) {
        FlogDeclareRowDto dto = new FlogDeclareRowDto();
        dto.setBatchNo(batchNo);
        // 整册提交的行上哪怕被人塞了别的字号，也以册头认下的那一家为准，不许借行挪册
        dto.setSiteNo(siteNo);
        dto.setSubcompNo(r.getSubcompNo());
        dto.setCutMode(r.getCutMode());
        dto.setForestKind(r.getForestKind());
        dto.setDisputeFlag(r.getDisputeFlag());
        dto.setArea(r.getArea());
        dto.setSurveyFlag(r.getSurveyFlag());
        dto.setQty(r.getQty());
        dto.setRecvQty(r.getRecvQty());
        return dto;
    }

    private String statusText(Integer status) {
        if (status == null) {
            return "";
        }
        switch (status) {
            case STATUS_PENDING:
                return "待核";
            case STATUS_ACCEPTED:
                return "已收";
            case STATUS_SUSPENDED:
                return "挂起";
            default:
                return String.valueOf(status);
        }
    }

    private String recvStatusText(Integer status) {
        if (status == null) {
            return "收册中";
        }
        switch (status) {
            case RECV_DONE:
                return "核讫";
            case RECV_EMPTY:
                return "空册";
            case RECV_REJECTED:
                return "整册退回";
            default:
                return "收册中";
        }
    }

    private int nz(Integer v) {
        return v == null ? 0 : v;
    }

    private int yearOf(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal.get(Calendar.YEAR);
    }

    private String currentLoginName() {
        try {
            return com.fc.v2.shiro.util.ShiroUtils.getLoginName();
        } catch (Exception e) {
            return null;
        }
    }
}
