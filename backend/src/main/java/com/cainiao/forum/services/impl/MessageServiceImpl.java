package com.cainiao.forum.services.impl;

import com.cainiao.forum.common.AppResult;
import com.cainiao.forum.common.ResultCode;
import com.cainiao.forum.dao.MessageBlacklistMapper;
import com.cainiao.forum.dao.MessageMapper;
import com.cainiao.forum.dao.NotificationMapper;
import com.cainiao.forum.exception.ApplicationException;
import com.cainiao.forum.model.Message;
import com.cainiao.forum.model.MessageBlacklist;
import com.cainiao.forum.model.Notification;
import com.cainiao.forum.model.User;
import com.cainiao.forum.services.IMessageService;
import com.cainiao.forum.services.IUserService;
import com.cainiao.forum.utils.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;



@Slf4j
@Service
// 站内信业务实现类，负责发送、查询、状态更新和回复站内信。
public class MessageServiceImpl implements IMessageService {

    // 站内信内容长度限制，对应数据库 varchar(255)。
    private static final int MESSAGE_MIN_LENGTH = 1;
    private static final int MESSAGE_MAX_LENGTH = 255;

    @Resource
    private MessageMapper messageMapper;

    @Resource
    private MessageBlacklistMapper messageBlacklistMapper;
    @Resource
    private NotificationMapper notificationMapper;
    @Resource
    private IUserService userService;

    @Override
    public void create(Message message) {

        // 发送站内信必须有发送者和接收者。
        if (message == null || message.getPostUserId() == null || message.getReceiveUserId() == null
           || message.getPostUserId() <= 0 || message.getReceiveUserId() <= 0) {

            log.warn(ResultCode.FAILED_PARAMS_VALIDATE.toString());

            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE));
        }

        // 去除首尾空格后校验消息长度。
        message.setContent(StringUtil.trim(message.getContent()));
        if (!StringUtil.lengthBetween(message.getContent(), MESSAGE_MIN_LENGTH, MESSAGE_MAX_LENGTH)) {

            log.warn(ResultCode.FAILED_PARAMS_VALIDATE.toString());

            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE));
        }

        // 不允许自己给自己发站内信。
        if (message.getPostUserId().equals(message.getReceiveUserId())) {
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE));
        }

        // 发送者必须存在、未删除、未禁言。
        User postUser = userService.selectById(message.getPostUserId());
        if (postUser == null || postUser.getDeleteState() == 1) {
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_USER_NOT_EXISTS));
        }
        if (postUser.getState() == 1) {
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_USER_BANNED));
        }

        // 接收者必须存在且未删除。
        User receiveUser = userService.selectById(message.getReceiveUserId());
        if (receiveUser == null || receiveUser.getDeleteState() == 1) {

            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE));
        }

        // 会话管理规则一：任一方拉黑对方则禁止私信。
        if (messageBlacklistMapper.countBy(message.getReceiveUserId(), message.getPostUserId()) > 0) {
            throw new ApplicationException(AppResult.failed("对方已将你拉黑，无法发送私信"));
        }
        if (messageBlacklistMapper.countBy(message.getPostUserId(), message.getReceiveUserId()) > 0) {
            throw new ApplicationException(AppResult.failed("你已拉黑该用户，请先在私信黑名单中解除后再发送"));
        }

        // 会话管理规则二：对方回复之前只能发送一条私信（双方最后一条消息是自己发的则拦截）。
        Message last = messageBlacklistMapper.selectLastMessageBetween(
                message.getPostUserId(), message.getReceiveUserId());
        if (last != null && message.getPostUserId().equals(last.getPostUserId())) {
            throw new ApplicationException(AppResult.failed("对方回复之前只能发送一条私信，请耐心等待回复"));
        }


        // 新消息默认未读，且未删除。
        message.setState((byte) 0);
        message.setDeleteState((byte) 0);

        Date date = new Date();
        message.setCreateTime(date);
        message.setUpdateTime(date);


        // 写入站内信记录。
        int row = messageMapper.insertSelective(message);
        if (row != 1) {
            log.warn(ResultCode.FAILED_CREATE.toString());
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_CREATE));
        }
    }

    @Override
    public Message selectById(Long id) {

        // 查询消息必须指定合法 ID。
        if (id == null || id <= 0) {

            log.warn(ResultCode.FAILED_PARAMS_VALIDATE.toString());

            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE));
        }

        Message message = messageMapper.selectByPrimaryKey(id);

        return message;
    }

    @Override
    public Integer selectUnreadCount(Long receiveUserId) {

        // 未读数按接收者用户 ID 查询。
        if (receiveUserId == null || receiveUserId <= 0) {

            log.warn(ResultCode.FAILED_PARAMS_VALIDATE.toString());

            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE));
        }

        Integer count = messageMapper.selectUnreadCount(receiveUserId);

        // count 查询正常情况下至少返回 0，返回 null 说明 SQL 或映射异常。
        if (count == null) {
            log.warn(ResultCode.ERROR_SERVICES.toString());
            throw new ApplicationException(AppResult.failed(ResultCode.ERROR_SERVICES));
        }

        return count;
    }

    @Override
    public List<Message> selectByReceiveUserId(Long receiveUserId) {

        // 收件箱按接收者 ID 查询。
        if (receiveUserId == null || receiveUserId <= 0) {

            log.warn(ResultCode.FAILED_PARAMS_VALIDATE.toString());

            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE));
        }

        // 扩展 SQL 会带出发送者和接收者信息。
        List<Message> messages = messageMapper.selectByReceiveUserId(receiveUserId);

        return messages;
    }

    @Override
    public List<Message> selectByPostUserId(Long postUserId) {

        // 发件箱按发送者 ID 查询。
        if (postUserId == null || postUserId <= 0) {

            log.warn(ResultCode.FAILED_PARAMS_VALIDATE.toString());

            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE));
        }

        // 查询当前用户发出的消息列表。
        List<Message> messages = messageMapper.selectByPostUserId(postUserId);

        return messages;
    }

    @Override
    public void updateStateById(Long id, Byte state) {

        // 消息状态只允许 0 未读、1 已读、2 已回复。
        if (id == null || id <= 0 || state == null || state < 0 || state > 2) {

            log.warn(ResultCode.FAILED_PARAMS_VALIDATE.toString());

            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE));
        }

        // 只更新状态和更新时间。
        Message updateMessage = new Message();
        updateMessage.setId(id);
        updateMessage.setState(state);
        Date date = new Date();
        updateMessage.setUpdateTime(date);

        int row = messageMapper.updateByPrimaryKeySelective(updateMessage);
        if (row != 1) {
            log.warn(ResultCode.ERROR_SERVICES.toString());
            throw new ApplicationException(AppResult.failed(ResultCode.ERROR_SERVICES));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    // 回复站内信需要更新原消息状态并创建新消息，所以使用事务。
    public void reply(Long repliedId, Message message) {

        // repliedId 表示被回复的原消息 ID。
        if (repliedId == null || repliedId <= 0) {

            log.warn(ResultCode.FAILED_PARAMS_VALIDATE.toString());

            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE));
        }

        Message existsMessage = messageMapper.selectByPrimaryKey(repliedId);
        // 原消息必须存在且未删除。
        if (existsMessage == null || existsMessage.getDeleteState() == 1) {

            log.warn(ResultCode.FAILED_MESSAGE_NOT_EXISTS.toString());

            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_MESSAGE_NOT_EXISTS));
        }


        // 原消息状态改为已回复。
        updateStateById(repliedId, (byte) 2);

        // 创建真正的回复消息。
        create(message);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    // 查询会话 + 批量已读必须同事务，避免读到一半状态被并发回复改变。
    public List<Message> selectConversation(Long userId, Long peerId) {

        // 双方 ID 都必须合法，且不能查询与自己的会话。
        if (userId == null || userId <= 0 || peerId == null || peerId <= 0 || userId.equals(peerId)) {
            log.warn(ResultCode.FAILED_PARAMS_VALIDATE.toString());
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE));
        }

        // 对方用户必须存在，防止查询已注销用户的会话。
        User peer = userService.selectById(peerId);
        if (peer == null || peer.getDeleteState() == 1) {
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_USER_NOT_EXISTS));
        }

        // 先批量已读对方发来的未读消息，再返回完整会话（保证返回的状态一致）。
        messageMapper.markConversationRead(userId, peerId);

        List<Message> conversation = messageMapper.selectConversation(userId, peerId);
        // 内存中同步已读状态，保证本次返回的 state 与落库一致。
        if (conversation != null) {
            for (Message m : conversation) {
                if (m.getState() == 0 && peerId.equals(m.getPostUserId())) {
                    m.setState((byte) 1);
                }
            }
        }

        return conversation;
    }

    @Override
    public List<Notification> selectNotifications(Long userId, Integer limit) {
        if (userId == null || userId <= 0) {
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE));
        }
        int safeLimit = (limit == null || limit <= 0) ? 50 : Math.min(limit, 100);
        return notificationMapper.selectActivityNotifications(userId, safeLimit);
    }

    @Override
    public void addToBlacklist(Long userId, Long blockedUserId) {
        if (userId == null || blockedUserId == null || userId <= 0 || blockedUserId <= 0
                || userId.equals(blockedUserId)) {
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE));
        }
        User blockedUser = userService.selectById(blockedUserId);
        if (blockedUser == null || blockedUser.getDeleteState() == 1) {
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_USER_NOT_EXISTS));
        }
        messageBlacklistMapper.insertIgnore(userId, blockedUserId);
    }

    @Override
    public void removeFromBlacklist(Long userId, Long blockedUserId) {
        if (userId == null || blockedUserId == null || userId <= 0 || blockedUserId <= 0) {
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE));
        }
        messageBlacklistMapper.delete(userId, blockedUserId);
    }

    @Override
    public List<MessageBlacklist> selectBlacklist(Long userId) {
        if (userId == null || userId <= 0) {
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE));
        }
        return messageBlacklistMapper.selectByUserId(userId);
    }

}

