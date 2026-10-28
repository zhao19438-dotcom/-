package com.cainiao.forum.controller;

import com.cainiao.forum.common.AppResult;
import com.cainiao.forum.common.ResultCode;
import com.cainiao.forum.config.AppConfig;
import com.cainiao.forum.model.Message;
import com.cainiao.forum.model.MessageBlacklist;
import com.cainiao.forum.model.Notification;
import com.cainiao.forum.model.User;
import com.cainiao.forum.services.IMessageService;
import com.cainiao.forum.services.IReportService;
import com.cainiao.forum.services.IUserService;
import com.cainiao.forum.utils.StringUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.List;


@Slf4j
@Api(tags = "站内信接口")
@RestController
@RequestMapping("/message")
// 站内信控制器，负责发送、收件箱、发件箱、通知、已读和回复。
public class MessageController {

    // 站内信长度限制，对应数据库 content varchar(255)。
    private static final int MESSAGE_MIN_LENGTH = 1;
    private static final int MESSAGE_MAX_LENGTH = 255;

    @Resource
    private IMessageService messageService;
    @Resource
    private IUserService userService;
    @Resource
    private IReportService reportService;

    @ApiOperation("发送站内信")
    @PostMapping("/send")
    // 发送站内信：当前登录用户向另一个用户发送消息。
    public AppResult send (HttpServletRequest request,
                           @ApiParam("接收者Id") @RequestParam("receiveUserId") @NonNull Long receiveUserId,
                           @ApiParam("内容") @RequestParam("content") @NonNull String content) {

        HttpSession session = request.getSession(false);
        User user = (User) session.getAttribute(AppConfig.USER_SESSION);

        if (user.getState() == 1) {

            return AppResult.failed(ResultCode.FAILED_USER_BANNED);
        }

        // 空消息和超长消息都不允许发送。
        content = StringUtil.trim(content);
        if (!StringUtil.lengthBetween(content, MESSAGE_MIN_LENGTH, MESSAGE_MAX_LENGTH)) {
            return AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE);
        }

        // 避免用户给自己发送站内信。
        if (user.getId().equals(receiveUserId)) {
            return AppResult.failed("不能给自己发送站内信");
        }

        // 接收者必须存在且未被删除。
        User receiveUser = userService.selectById(receiveUserId);
        if (receiveUser == null || receiveUser.getDeleteState() == 1) {

            return AppResult.failed("接收者状态异常");
        }

        Message message = new Message();
        // 发送者从 Session 获取，不能相信前端传入发送者 ID。
        message.setPostUserId(user.getId());
        message.setReceiveUserId(receiveUserId);
        message.setContent(content);

        messageService.create(message);

        return AppResult.success("发送成功");
    }

    @ApiOperation("获取未读数")
    @GetMapping("/getUnreadCount")
    // 查询当前登录用户的未读站内信数量。
    public AppResult<Integer> getUnreadCount (HttpServletRequest request) {

        HttpSession session = request.getSession(false);
        User user = (User) session.getAttribute(AppConfig.USER_SESSION);

        Integer count = messageService.selectUnreadCount(user.getId());

        return AppResult.success(count);
    }


    @ApiOperation("查询收件箱")
    @GetMapping("/inbox")
    // 查询当前登录用户收到的站内信。
    public AppResult<List<Message>> getInbox (HttpServletRequest request) {

        HttpSession session = request.getSession(false);
        User user = (User) session.getAttribute(AppConfig.USER_SESSION);

        List<Message> messages = messageService.selectByReceiveUserId(user.getId());

        return AppResult.success(messages);
    }

    @ApiOperation("查询已发送站内信")
    @GetMapping("/sent")
    // 查询当前登录用户发出的站内信。
    public AppResult<List<Message>> getSent (HttpServletRequest request) {

        HttpSession session = request.getSession(false);
        User user = (User) session.getAttribute(AppConfig.USER_SESSION);

        List<Message> messages = messageService.selectByPostUserId(user.getId());

        return AppResult.success(messages);
    }

    @ApiOperation("查询与某用户的会话")
    @GetMapping("/conversation")
    // 查询当前用户与指定用户的完整往来私信（时间正序），并自动把对方发来的未读消息批量标记为已读。
    public AppResult<List<Message>> getConversation (HttpServletRequest request,
                                                     @ApiParam("对方用户Id") @RequestParam("peerId") @NonNull Long peerId) {

        HttpSession session = request.getSession(false);
        User user = (User) session.getAttribute(AppConfig.USER_SESSION);

        List<Message> conversation = messageService.selectConversation(user.getId(), peerId);

        return AppResult.success(conversation);
    }

    @ApiOperation("查询站内通知")
    @GetMapping("/notifications")
    // 查询站内通知：别人回复/点赞/收藏了你的帖子，以及你提交的举报被处置。
    public AppResult<List<Notification>> notifications (HttpServletRequest request,
                                                        @RequestParam(value = "limit", required = false, defaultValue = "50") Integer limit) {

        HttpSession session = request.getSession(false);
        User user = (User) session.getAttribute(AppConfig.USER_SESSION);

        List<Notification> notifications = messageService.selectNotifications(user.getId(), limit);

        return AppResult.success(notifications);
    }

    @ApiOperation("更新为已读")
    @PostMapping("/markRead")
    // 标记已读：只有消息接收者本人可以把消息改为已读。
    public AppResult markRead (HttpServletRequest request,
                               @ApiParam("站内信Id") @RequestParam("id") @NonNull Long id) {

        Message message = messageService.selectById(id);

        if (message == null || message.getDeleteState() == 1) {

            return AppResult.failed(ResultCode.FAILED_MESSAGE_NOT_EXISTS);
        }

        HttpSession session = request.getSession(false);
        User user = (User) session.getAttribute(AppConfig.USER_SESSION);
        // 防止用户通过传别人的消息 ID 标记别人的消息。
        if (!user.getId().equals(message.getReceiveUserId())) {

            return AppResult.failed(ResultCode.FAILED_FORBIDDEN);
        }


        messageService.updateStateById(id, (byte) 1);

        return AppResult.success();
    }


    @ApiOperation("回复站内信")
    @PostMapping("/reply")
    // 回复站内信：根据原消息确定接收者，再创建一条新消息。
    public AppResult reply (HttpServletRequest request,
                            @ApiParam("要回复的站内信Id") @RequestParam("repliedId") @NonNull Long repliedId,
                            @ApiParam("站内信的内容") @RequestParam("content") @NonNull String content) {

        HttpSession session = request.getSession(false);
        User user = (User) session.getAttribute(AppConfig.USER_SESSION);
        if (user.getState() == 1) {

            return AppResult.failed(ResultCode.FAILED_USER_BANNED);
        }

        // 回复内容同样需要做长度校验。
        content = StringUtil.trim(content);
        if (!StringUtil.lengthBetween(content, MESSAGE_MIN_LENGTH, MESSAGE_MAX_LENGTH)) {
            return AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE);
        }

        // 先查原消息，才能知道应该回复给谁。
        Message existsMessage = messageService.selectById(repliedId);
        if (existsMessage == null || existsMessage.getDeleteState() == 1) {

            return AppResult.failed(ResultCode.FAILED_MESSAGE_NOT_EXISTS);
        }

        // 原消息是自己发的，就没有必要再回复自己。
        if (user.getId().equals(existsMessage.getPostUserId())) {

            return AppResult.failed("不能回复自己的站内信");
        }

        Message message = new Message();
        message.setPostUserId(user.getId());
        message.setReceiveUserId(existsMessage.getPostUserId());
        message.setContent(content);

        messageService.reply(repliedId, message);

        return AppResult.success();
    }

    @ApiOperation("加入私信黑名单")
    @PostMapping("/blacklist/add")
    // 拉黑某用户：此后对方无法再给我发私信（幂等）。
    public AppResult blacklistAdd (HttpServletRequest request,
                                   @ApiParam("要拉黑的用户Id") @RequestParam("blockedUserId") @NonNull Long blockedUserId) {

        HttpSession session = request.getSession(false);
        User user = (User) session.getAttribute(AppConfig.USER_SESSION);

        messageService.addToBlacklist(user.getId(), blockedUserId);

        return AppResult.success("已加入黑名单");
    }

    @ApiOperation("解除私信黑名单")
    @PostMapping("/blacklist/remove")
    // 解除拉黑：恢复对方给我发私信的能力（幂等）。
    public AppResult blacklistRemove (HttpServletRequest request,
                                      @ApiParam("要解除拉黑的用户Id") @RequestParam("blockedUserId") @NonNull Long blockedUserId) {

        HttpSession session = request.getSession(false);
        User user = (User) session.getAttribute(AppConfig.USER_SESSION);

        messageService.removeFromBlacklist(user.getId(), blockedUserId);

        return AppResult.success("已解除拉黑");
    }

    @ApiOperation("查询私信黑名单")
    @GetMapping("/blacklist/list")
    // 当前用户的黑名单列表，带出被拉黑用户信息。
    public AppResult<List<MessageBlacklist>> blacklistList (HttpServletRequest request) {

        HttpSession session = request.getSession(false);
        User user = (User) session.getAttribute(AppConfig.USER_SESSION);

        return AppResult.success(messageService.selectBlacklist(user.getId()));
    }

    @ApiOperation("举报私信")
    @PostMapping("/report")
    // 举报收到的私信：只有消息接收者可以举报，提交后进入管理后台举报处置流程。
    public AppResult reportMessage (HttpServletRequest request,
                                    @ApiParam("要举报的私信Id") @RequestParam("messageId") @NonNull Long messageId,
                                    @ApiParam("举报类型") @RequestParam(value = "type", required = false) Byte type,
                                    @ApiParam("举报说明") @RequestParam(value = "reason", required = false) String reason) {

        HttpSession session = request.getSession(false);
        User user = (User) session.getAttribute(AppConfig.USER_SESSION);

        // 消息必须存在，且只有收件人可以举报（防止恶意举报他人会话）。
        Message message = messageService.selectById(messageId);
        if (message == null || message.getDeleteState() == 1) {
            return AppResult.failed(ResultCode.FAILED_MESSAGE_NOT_EXISTS);
        }
        if (!user.getId().equals(message.getReceiveUserId())) {
            return AppResult.failed("只能举报自己收到的私信");
        }

        reportService.submitMessage(messageId, user.getId(), type, reason);

        return AppResult.success("举报已提交，管理员将尽快核查");
    }

}

