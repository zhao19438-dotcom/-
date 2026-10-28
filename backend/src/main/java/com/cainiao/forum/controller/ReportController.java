package com.cainiao.forum.controller;

import com.cainiao.forum.common.AppResult;
import com.cainiao.forum.common.PageResult;
import com.cainiao.forum.common.ResultCode;
import com.cainiao.forum.config.AppConfig;
import com.cainiao.forum.model.Report;
import com.cainiao.forum.model.User;
import com.cainiao.forum.services.IReportService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * 举报接口。
 *
 * 路径说明：Nginx 只转发 user/article/reply/board/message/admin 等前缀，
 * 因此用户提交走 /article/report，后台管理走 /admin/report/*，
 * 这样无需改动 Nginx 反向代理配置。
 */
@Api(tags = "举报接口")
@Slf4j
@RestController
public class ReportController {

    @Resource
    private IReportService reportService;

    @ApiOperation("举报帖子")
    @PostMapping("/article/report")
    public AppResult report (HttpServletRequest request,
                             @ApiParam("帖子Id") @RequestParam(value = "articleId", required = false) Long articleId,
                             @ApiParam("目标Id兼容参数") @RequestParam(value = "targetId", required = false) Long targetId,
                             @ApiParam("举报类型 0垃圾广告 1色情低俗 2辱骂攻击 3违法违规 4其他") @RequestParam(value = "type", required = false) Byte type,
                             @ApiParam("举报说明") @RequestParam(value = "reason", required = false) String reason) {

        Long realArticleId = articleId != null ? articleId : targetId;
        if (realArticleId == null) {
            return AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE);
        }
        HttpSession session = request.getSession(false);
        User user = (User) session.getAttribute(AppConfig.USER_SESSION);

        reportService.submit(realArticleId, user.getId(), type, reason);
        return AppResult.success("举报已提交，管理员会尽快处理");
    }

    @ApiOperation("后台举报列表")
    @GetMapping("/admin/report/list")
    public AppResult<PageResult<Report>> listForAdmin (HttpServletRequest request,
                                                        @ApiParam("状态 0待处理 1已处理 2已驳回") @RequestParam(value = "status", required = false) Byte status,
                                                        @ApiParam("页码") @RequestParam(value = "page", required = false) Integer page,
                                                        @ApiParam("每页数量") @RequestParam(value = "size", required = false) Integer size) {

        HttpSession session = request.getSession(false);
        User user = (User) session.getAttribute(AppConfig.USER_SESSION);
        // 只有管理员可以查看举报列表。
        if (user == null || user.getIsAdmin() == null || user.getIsAdmin() != 1) {
            return AppResult.failed(ResultCode.FAILED_FORBIDDEN);
        }

        return AppResult.success(reportService.listForAdmin(status, page, size));
    }

    @ApiOperation("处理举报")
    @PostMapping("/admin/report/handle")
    public AppResult handle (HttpServletRequest request,
                             @ApiParam("举报Id") @RequestParam("id") @NonNull Long id,
                             @ApiParam("处理结果状态 1已处理 2已驳回") @RequestParam("status") @NonNull Byte status,
                             @ApiParam("处理说明") @RequestParam(value = "handleResult", required = false) String handleResult,
                             @ApiParam("联动制裁 none/warn/delete/ban") @RequestParam(value = "action", required = false, defaultValue = "none") String action) {

        HttpSession session = request.getSession(false);
        User user = (User) session.getAttribute(AppConfig.USER_SESSION);
        // 处理举报是管理员专属操作。
        if (user == null || user.getIsAdmin() == null || user.getIsAdmin() != 1) {
            return AppResult.failed(ResultCode.FAILED_FORBIDDEN);
        }

        // 联动制裁取值白名单，非法值降级为 none。
        if (!"warn".equals(action) && !"delete".equals(action) && !"ban".equals(action)) {
            action = "none";
        }

        reportService.handleWithAction(id, status, handleResult, action, user.getId());
        return AppResult.success();
    }
}
