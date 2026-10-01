package com.fc.v2.service;

import java.util.List;

import com.fc.v2.model.auto.TFlogDeclareDiff;
import com.fc.v2.model.auto.TFlogDeclareRow;
import com.fc.v2.model.custom.flog.FlogDeclareRowDto;
import com.fc.v2.model.custom.flog.FlogReceiptVo;
import com.fc.v2.model.custom.flog.FlogRowResultVo;

/**
 * 伐区申报清册核收行 Service接口。
 *
 * 一册册子从进门到落库只有一条道：{@link #processRow} 这一条逐行处理的方法——
 * 一行递进去，算收下、算挂起、重复并账或挪去往期，都由它的给回定夺。
 * {@link #submitBatch} 整册提交只是在同一事务里把这一行行的道走完，入参照旧。
 *
 * @author fuce
 * @date 2026-09-14
 */
public interface ITFlogDeclareRowService {

    /** 按主键回查单行 */
    TFlogDeclareRow selectTFlogDeclareRowById(Long id);

    /**
     * 唯一逐行处理方法：一行递进去，行次、落地情形、前置项代号、缘故全由本层给。
     * 同一册同一家同一小班重复进门并作头行一行；重送已收讫的册当场挡住（改数走差异清单）；
     * 越过单册行限整册退回。
     */
    FlogRowResultVo processRow(FlogDeclareRowDto row);

    /**
     * 整册提交（入参照旧：批次号 + 行清单）：逐行走同一条 {@link #processRow} 的道，
     * 走完同一回计算出回执三数。合法行入库、非法行各按前置项挂起，同册余下行照常入账。
     * 同一批次重复提交不得重复入库；行数超过单批上限返回 -1 且整册不入库；空批次返回 0（回执只写空册）。
     */
    int submitBatch(String batchNo, List<TFlogDeclareRow> rows);

    /** 逐行递入结束后收口：库里逐行重数，同一回计算落回执三数并验平 */
    FlogReceiptVo finalizeBatch(String batchNo);

    /** 回执（三数取批次头同一次计算那份；已收讫册账后被改、对不平则当场点破） */
    FlogReceiptVo getReceipt(String batchNo);

    /**
     * 现行页逐行查询（page_flag=0）；挂起/并账行都摆得出，挂起那笔直写出自本册第几行。
     */
    List<TFlogDeclareRow> listCurrentRows(String batchNo, String siteNo, Integer status);

    /** 该批次的挂起行明细（按行号升序；重复并账行不在内） */
    List<TFlogDeclareRow> listErrors(String batchNo);

    /** 往期页：凭批次号（或年度+字号）把早年收下的账拉出来 */
    List<TFlogDeclareRow> listArchived(String batchNo, String siteNo, Integer periodYear);

    /**
     * 往期结转：进门那年至今满两个执行期（period_year &lt;= asOfYear-2）的已收现行页行，
     * 成页转到往期页，现行页不再露。返回转走行数。
     */
    int turnArchived(Integer asOfYear);

    /** 登一笔差异申请（只对已收讫册的落账行；重送不覆盖，要改蓄积走这里） */
    TFlogDeclareDiff registerDiff(String batchNo, Integer rowNo, Integer diffKind,
                                  java.math.BigDecimal newQty, String remark);

    /** 差异清单（status 空看全部） */
    List<TFlogDeclareDiff> listDiffs(Integer status);

    /** 差异办结：approve=true 按新数入帐，false 驳回；返回办结后的差异单 */
    TFlogDeclareDiff handleDiff(Long id, boolean approve, String remark);
}
