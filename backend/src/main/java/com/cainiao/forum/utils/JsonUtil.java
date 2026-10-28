package com.cainiao.forum.utils;

import com.cainiao.forum.common.AppResult;
import com.cainiao.forum.common.ResultCode;
import com.cainiao.forum.exception.ApplicationException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

/**
 * JSON 工具：业务扩展字段 extJson 的解析与序列化集中在这里。
 * 解析失败统一抛参数校验异常，避免在每个业务处理器里重复处理。
 */
public class JsonUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // 把扩展字段 JSON 字符串解析成 Map；空串返回空 Map；格式错误抛参数校验异常。
    public static Map<String, Object> parseExt(String extJson) {
        if (extJson == null || extJson.trim().isEmpty()) {
            return new HashMap<>();
        }
        try {
            Map<String, Object> map = MAPPER.readValue(extJson, Map.class);
            return map == null ? new HashMap<>() : map;
        } catch (Exception e) {
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE));
        }
    }

    // 把对象序列化为 JSON 字符串，写扩展表前或返回前端前使用。
    public static String toJson(Object obj) {
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }
}
