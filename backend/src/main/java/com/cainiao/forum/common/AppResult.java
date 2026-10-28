package com.cainiao.forum.common;

import com.fasterxml.jackson.annotation.JsonInclude;



/**
 * 统一接口返回对象。
 * Controller 返回它以后，前端可以固定读取 code、message、data 三个字段。
 */
public class AppResult<T> {


    // 业务状态码，0 通常表示成功，非 0 表示失败。
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private int code;

    // 给前端或用户看的提示信息。
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private String message;

    // 真正的业务数据，使用泛型 T 表示可以放任意类型。
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private T data;


    public AppResult(int code, String message) {
        this(code, message, null);
    }

    public AppResult(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }


    // 不需要返回数据时使用的成功结果。
    public static AppResult success () {
        return new AppResult(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage());
    }

    // 成功但想自定义提示信息时使用。
    public static AppResult success (String message) {
        return new AppResult(ResultCode.SUCCESS.getCode(), message);
    }

    // 成功并返回数据时使用，例如返回用户信息、帖子列表。
    public static <T> AppResult<T> success (T data) {
        return new AppResult<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    // 成功时同时自定义提示信息和数据。
    public static <T> AppResult<T> success (String message, T data) {
        return new AppResult<>(ResultCode.SUCCESS.getCode(), message, data);
    }


    // 通用失败结果，适合没有更具体错误原因的场景。
    public static AppResult failed () {
        return new AppResult(ResultCode.FAILED.getCode(), ResultCode.FAILED.getMessage());
    }

    // 失败时自定义提示信息。
    public static AppResult failed (String message) {
        return new AppResult(ResultCode.FAILED.getCode(), message);
    }

    // 失败时直接使用枚举中预定义的错误码和错误信息。
    public static AppResult failed (ResultCode resultCode) {
        return new AppResult(resultCode.getCode(), resultCode.getMessage());
    }


    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }


}

