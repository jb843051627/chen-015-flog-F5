package com.fc.v2.model.custom.flog;

/**
 * 勾销卡“往回退一格”送法（新用法，与旧的 rollback(id, remark) 同名叠加）。
 *
 * @author fuce
 * @date 2026-10-01
 */
public class FlogCloseRollbackDto {

    /** 勾销事务卡id */
    private Long id;

    /** 退回缘故（可空，只落一笔，不作数） */
    private String reason;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
