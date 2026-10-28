package com.cainiao.forum.dao;

import com.cainiao.forum.model.Message;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
// 站内信 Mapper，负责 t_message 的基础 CRUD 和收发件箱查询。
public interface MessageMapper {
    // 完整插入站内信记录。
    int insert(Message row);

    // 选择性插入站内信记录。
    int insertSelective(Message row);

    // 根据主键查询站内信。
    Message selectByPrimaryKey(Long id);

    // 选择性更新站内信字段。
    int updateByPrimaryKeySelective(Message row);

    // 全字段更新站内信记录。
    int updateByPrimaryKey(Message row);


    // 查询指定用户收到的未读站内信数量。
    Integer selectUnreadCount(@Param("receiveUserId") Long receiveUserId);


    // 查询指定用户的收件箱。
    List<Message> selectByReceiveUserId (@Param("receiveUserId") Long receiveUserId);

    // 查询指定用户的已发送站内信。
    List<Message> selectByPostUserId (@Param("postUserId") Long postUserId);

    // 查询我与指定用户的完整会话（双向，时间正序）。
    List<Message> selectConversation(@Param("userId") Long userId, @Param("peerId") Long peerId);

    // 批量把对方发给我的未读消息标记为已读。
    int markConversationRead(@Param("userId") Long userId, @Param("peerId") Long peerId);
}
