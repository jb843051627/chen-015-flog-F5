package com.fc.v2.common.exception.biz;

/**
 * 伐区额度账业务异常：回执须点明缺的是哪一格、是哪张卡越了面子，
 * 不许丢一句含糊话搪塞。由全局异常处理接成 AjaxResult。
 *
 * @author fuce
 * @date 2026-09-29
 */
public class FlogBizException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public FlogBizException(String message) {
        super(message);
    }
}
