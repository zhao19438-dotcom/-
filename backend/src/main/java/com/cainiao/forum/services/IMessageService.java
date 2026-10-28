package com.cainiao.forum.services;

import com.cainiao.forum.model.Message;
import com.cainiao.forum.model.MessageBlacklist;
import com.cainiao.forum.model.Notification;

import java.util.List;

// 站内信业务接口，定义发送、查询、标记已读和回复消息等能力。
public interface IMessageService {


    // 新建一条站内信。
    void create (Message message);


    // 按 ID 查询站内信。
    Message selectById (Long id);


    // 查询某个用户收到的未读消息数量。
    Integer selectUnreadCount(Long receiveUserId);


    // 查询收件箱列表。
    List<Message> selectByReceiveUserId (Long receiveUserId);

    // 查询已发送列表。
    List<Message> selectByPostUserId (Long postUserId);


    // 修改消息状态，例如未读改已读。
    void updateStateById (Long id, Byte state);


    // 回复消息时，需要把原消息标记为已回复，并创建新消息，所以使用事务。
    void reply (Long repliedId, Message message);


    // 查询我与指定用户的完整会话（双向、时间正序），并把对方发来的未读消息批量标记为已读。
    List<Message> selectConversation (Long userId, Long peerId);

    List<Notification> selectNotifications (Long userId, Integer limit);

    void addToBlacklist (Long userId, Long blockedUserId);

    void removeFromBlacklist (Long userId, Long blockedUserId);

    List<MessageBlacklist> selectBlacklist (Long userId);

}

