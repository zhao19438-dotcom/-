package com.cainiao.forum.exception;

import com.cainiao.forum.common.AppResult;



/**
 * 业务异常。
 * Service 中发现参数不合法、资源不存在等业务问题时抛出它，
 * 最终由 GlobalExceptionHandler 统一转换为 AppResult 返回给前端。
 */
public class ApplicationException extends RuntimeException {


    // 保存完整的错误返回对象，便于异常处理器直接返回指定 code 和 message。
    protected AppResult errorResult;


    public ApplicationException (AppResult errorResult) {
        super(errorResult.getMessage());
        this.errorResult = errorResult;
    }

    public ApplicationException(String message) {
        super(message);
    }

    public ApplicationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ApplicationException(Throwable cause) {
        super(cause);
    }

    public AppResult getErrorResult() {
        return errorResult;
    }
}

