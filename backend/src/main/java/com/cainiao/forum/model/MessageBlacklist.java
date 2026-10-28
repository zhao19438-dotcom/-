package com.cainiao.forum.model;

import lombok.Data;

import java.util.Date;

@Data
// 私信黑名单实体，对应数据库中的 t_message_blacklist 表。
public class MessageBlacklist {

    // 记录主键 ID。
    private Long id;

    // 拉黑发起人（收信保护方）：被拉黑者无法再给他发私信。
    private Long userId;

    // 被拉黑用户 ID。
    private Long blockedUserId;

    // 拉黑时间。
    private Date createTime;

    // 被拉黑用户信息（查询黑名单列表时带出，不落库）。
    private User blockedUser;
}
