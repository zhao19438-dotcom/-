package com.cainiao.forum.utils;



// 字符串工具类，集中处理空值、空白和长度校验。
public class StringUtil {


    public static boolean isEmpty (String value) {
        // 只判断 null 或长度为 0，不会把空格当作空字符串。
        return value == null || value.length() == 0;
    }

    public static boolean isBlank(String value) {
        // trim 后长度为 0，说明字符串只有空白字符。
        return value == null || value.trim().length() == 0;
    }

    public static String trim(String value) {
        // 对 null 保持 null，避免调用 trim 时出现空指针异常。
        return value == null ? null : value.trim();
    }

    public static boolean lengthBetween(String value, int min, int max) {
        if (value == null) {
            return false;
        }
        // Java 字符串 length 统计字符数量，适合做用户名、标题等长度限制。
        int length = value.length();
        return length >= min && length <= max;
    }

    /**
     * 基础 HTML/XSS 安全清洗：
     * 去除 <script>、<iframe> 标签，拦截 javascript:/vbscript: 伪协议及 onXxx 伪事件，
     * 形成后端入库防护屏障，防止攻击者绕过前端注入恶意可执行脚本。
     */
    public static String cleanXss(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        String cleaned = value;
        // 移除 <script>...</script> 代码块
        cleaned = cleaned.replaceAll("(?i)<script[^>]*>[\\s\\S]*?</script>", "");
        // 移除 <iframe>...</iframe> 嵌入框
        cleaned = cleaned.replaceAll("(?i)<iframe[^>]*>[\\s\\S]*?</iframe>", "");
        // 移除 javascript: 伪协议
        cleaned = cleaned.replaceAll("(?i)javascript:", "");
        // 移除 vbscript: 伪协议
        cleaned = cleaned.replaceAll("(?i)vbscript:", "");
        // 中和内联 DOM 事件（如 onload=, onerror=, onclick=）
        cleaned = cleaned.replaceAll("(?i)\\bon[a-zA-Z]+\\s*=", "data-disabled-event=");
        return cleaned;
    }
}


