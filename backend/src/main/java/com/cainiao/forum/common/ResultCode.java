package com.cainiao.forum.common;

// 项目中所有接口统一使用的状态码枚举，避免每个地方手写数字和提示语。
public enum ResultCode {
    // 通用结果码。
    SUCCESS                     (0, "操作成功"),
    FAILED                      (1000, "操作失败"),
    FAILED_UNAUTHORIZED         (1001, "未授权"),
    FAILED_PARAMS_VALIDATE      (1002, "参数校验失败"),
    FAILED_FORBIDDEN            (1003, "禁止访问"),
    FAILED_CREATE               (1004, "新增失败"),
    FAILED_NOT_EXISTS           (1005, "资源不存在"),
    FAILED_TOO_MANY_REQUESTS    (1006, "操作过于频繁，请稍后再试"),
    FAILED_CAPTCHA              (1007, "验证码错误或已失效"),


    // 用户相关错误码。
    FAILED_USER_EXISTS          (1101, "用户已存在"),
    FAILED_USER_NOT_EXISTS      (1102, "用户不存在"),
    FAILED_LOGIN                (1103, "用户名或密码错误"),
    FAILED_USER_BANNED          (1104, "账号已被禁言，请联系管理员处理后再重新登录"),
    FAILED_USER_ARTICLE_COUNT   (1105, "更新帖子数量失败"),
    FAILED_TWO_PWD_NOT_SAME     (1106, "两次输入的密码不一致"),



    // 版块相关错误码。
    FAILED_BOARD_ARTICLE_COUNT  (1201, "更新帖子数量失败"),
    FAILED_BOARD_BANNED         (1202, "版块状况异常"),
    FAILED_BOARD_NOT_EXISTS     (1203, "版块不存在"),

    // 帖子相关错误码。
    FAILED_ARTICLE_NOT_EXISTS   (1301, "帖子不存在"),
    FAILED_ARTICLE_BANNED       (1302, "帖子状况异常"),

    // 站内信相关错误码。
    FAILED_MESSAGE_NOT_EXISTS   (1401, "站内信不存在"),

    // 服务端内部错误码。
    ERROR_SERVICES              (2000, "服务器内部错误"),
    // 供服务端日志定位使用，对外统一提示为可读文案，避免把技术细节暴露给用户。
    ERROR_IS_NULL               (2001, "数据异常，请稍后重试");



    // 返回给前端的数字状态码。
    int code;

    // 返回给前端的提示信息。
    String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String toString() {
        return "code = " + code + ", message = " + message + ". ";
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}

