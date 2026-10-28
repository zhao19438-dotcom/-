package com.cainiao.forum.controller;

import com.cainiao.forum.common.AppResult;
import com.cainiao.forum.common.PageResult;
import com.cainiao.forum.common.ResultCode;
import com.cainiao.forum.config.AppConfig;
import com.cainiao.forum.model.AuditLog;
import com.cainiao.forum.model.SensitiveWord;
import com.cainiao.forum.model.User;
import com.cainiao.forum.services.IAuditLogService;
import com.cainiao.forum.services.ISensitiveWordService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.NonNull;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.List;

/**
 * 内容审核与审计后台接口（均需管理员权限）。
 * 路径统一在 /admin 前缀下，Nginx 已代理该前缀。
 */
@Api(tags = "内容审核与审计")
@RestController
public class ModerationController {

    @Resource
    private ISensitiveWordService sensitiveWordService;

    @Resource
    private IAuditLogService auditLogService;

    // 统一的权限校验：非管理员直接拒绝。
    private AppResult checkAdmin(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        User user = session == null ? null : (User) session.getAttribute(AppConfig.USER_SESSION);
        if (user == null || user.getIsAdmin() == null || user.getIsAdmin() != 1) {
            return AppResult.failed(ResultCode.FAILED_FORBIDDEN);
        }
        return null;
    }

    @ApiOperation("敏感词列表")
    @GetMapping("/admin/sensitive/list")
    public AppResult listWords(HttpServletRequest request) {
        AppResult denied = checkAdmin(request);
        if (denied != null) {
            return denied;
        }
        List<SensitiveWord> words = sensitiveWordService.listAll();
        return AppResult.success(words);
    }

    @ApiOperation("新增敏感词")
    @PostMapping("/admin/sensitive/add")
    public AppResult addWord(HttpServletRequest request,
                             @ApiParam("敏感词") @RequestParam("word") @NonNull String word) {
        AppResult denied = checkAdmin(request);
        if (denied != null) {
            return denied;
        }
        sensitiveWordService.add(word);
        return AppResult.success();
    }

    @ApiOperation("删除敏感词")
    @PostMapping("/admin/sensitive/delete")
    public AppResult deleteWord(HttpServletRequest request,
                                @ApiParam("敏感词Id") @RequestParam("id") @NonNull Long id) {
        AppResult denied = checkAdmin(request);
        if (denied != null) {
            return denied;
        }
        sensitiveWordService.delete(id);
        return AppResult.success();
    }

    @ApiOperation("刷新敏感词缓存")
    @PostMapping("/admin/sensitive/refresh")
    public AppResult refreshWords(HttpServletRequest request) {
        AppResult denied = checkAdmin(request);
        if (denied != null) {
            return denied;
        }
        sensitiveWordService.refresh();
        return AppResult.success();
    }

    @ApiOperation("审计日志列表")
    @GetMapping("/admin/audit/list")
    public AppResult<PageResult<AuditLog>> auditList(HttpServletRequest request,
                                                     @ApiParam("关键字") @RequestParam(value = "keyword", required = false) String keyword,
                                                     @ApiParam("页码") @RequestParam(value = "page", required = false) Integer page,
                                                     @ApiParam("每页数量") @RequestParam(value = "size", required = false) Integer size) {
        AppResult denied = checkAdmin(request);
        if (denied != null) {
            return denied;
        }
        return AppResult.success(auditLogService.list(keyword, page, size));
    }
}
