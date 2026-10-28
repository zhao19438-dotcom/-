package com.cainiao.forum.exception;

import com.cainiao.forum.common.AppResult;
import com.cainiao.forum.common.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;


@Slf4j
@ControllerAdvice
// 全局异常处理器：拦截 Controller 抛出的异常，并统一包装成 AppResult。
public class GlobalExceptionHandler {

    @ResponseBody
    @ExceptionHandler(ApplicationException.class)
    public AppResult applicationExceptionHandler (ApplicationException e) {

        // 业务异常只记录日志（含堆栈），不再 printStackTrace，也不把内部信息回给前端。
        log.error("业务异常：" + e.getMessage(), e);

        // 如果业务异常里已经带了 AppResult，就直接返回业务层指定的错误码。
        if (e.getErrorResult() != null) {
            return e.getErrorResult();
        }

        // 没有明确错误信息时，统一返回服务器内部错误，避免前端拿到空提示。
        if (e.getMessage() == null || e.getMessage().equals("")) {
            return AppResult.failed(ResultCode.ERROR_SERVICES);
        }

        return AppResult.failed(e.getMessage());
    }

    @ResponseBody
    @ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
    public AppResult missingServletRequestParameterExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException e) {
        log.warn("缺少必要请求参数: " + e.getParameterName());
        return AppResult.failed("缺少必要参数: " + e.getParameterName());
    }

    @ResponseBody
    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public AppResult methodArgumentTypeMismatchExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException e) {
        log.warn("参数格式类型错误: " + e.getName() + " -> " + e.getValue());
        return AppResult.failed("参数格式不匹配: " + e.getName());
    }

    @ResponseBody
    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public AppResult httpRequestMethodNotSupportedExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException e) {
        log.warn("请求方法不支持: " + e.getMethod());
        return AppResult.failed("请求方法不支持: " + e.getMethod());
    }

    @ResponseBody
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public AppResult maxUploadSizeExceededExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException e) {
        log.warn("上传文件大小超限: " + e.getMessage());
        // 与 application.yml 的 multipart.max-file-size 保持一致（5MB）。
        return AppResult.failed("上传文件过大，单个文件请勿超过 5MB");
    }

    @ResponseBody
    @ExceptionHandler(IllegalArgumentException.class)
    public AppResult illegalArgumentExceptionHandler(IllegalArgumentException e) {
        log.warn("非法参数异常: " + e.getMessage());
        return AppResult.failed(e.getMessage() == null ? "参数校验不通过" : e.getMessage());
    }

    @ResponseBody
    @ExceptionHandler(Exception.class)
    public AppResult exceptionHandler (Exception e) {

        // 系统异常：完整堆栈只进日志，前端统一拿到"服务器内部错误"，避免泄露实现细节。
        log.error("系统异常：" + e.getMessage(), e);

        return AppResult.failed(ResultCode.ERROR_SERVICES);
    }
}


