package com.fc.v2.model.custom.flog;

import java.io.Serializable;
import java.util.List;

import com.fc.v2.model.auto.TFlogDeclareRow;

/**
 * 整册提交报文（批次号 + 行清单）。整册提交方法的入参照旧能用，换 JSON 递法时也收这一份。
 *
 * @author fuce
 * @date 2026-09-29
 */
public class FlogDeclareBatchDto implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 伐区申报清册批次号 */
    private String batchNo;

    /** 本册各行（form 送法按 rows[0].qty 索引绑定） */
    private List<TFlogDeclareRow> rows;

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public List<TFlogDeclareRow> getRows() {
        return rows;
    }

    public void setRows(List<TFlogDeclareRow> rows) {
        this.rows = rows;
    }
}
