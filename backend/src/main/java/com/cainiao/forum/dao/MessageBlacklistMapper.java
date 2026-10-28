package com.cainiao.forum.dao;

import com.cainiao.forum.model.Message;
import com.cainiao.forum.model.MessageBlacklist;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
// 私信会话管理 Mapper：黑名单维护 + 会话最后一条消息查询（用于一条未回复限制）。
public interface MessageBlacklistMapper {

    // 加入黑名单：insert ignore 保证重复拉黑幂等，返回 1 表示本次真正写入。
    int insertIgnore(@Param("userId") Long userId, @Param("blockedUserId") Long blockedUserId);

    // 解除拉黑，返回受影响行数。
    int delete(@Param("userId") Long userId, @Param("blockedUserId") Long blockedUserId);

    // 判断某用户是否拉黑了另一用户，大于 0 表示已拉黑。
    int countBy(@Param("userId") Long userId, @Param("blockedUserId") Long blockedUserId);

    // 查询当前用户的黑名单列表，带出被拉黑用户信息。
    List<MessageBlacklist> selectByUserId(@Param("userId") Long userId);

    // 查询两人之间（双向）最后一条私信，用于"对方回复之前只能发一条"的会话限制。
    Message selectLastMessageBetween(@Param("userA") Long userA, @Param("userB") Long userB);
}
