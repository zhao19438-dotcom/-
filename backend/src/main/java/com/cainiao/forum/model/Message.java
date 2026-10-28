package com.cainiao.forum.model;

import lombok.Data;

import java.util.Date;

@Data
// 站内信实体类，对应数据库中的 t_message 表。
public class Message {
    // 站内信主键 ID。
    private Long id;

    // 发送者用户 ID。
    private Long postUserId;

    // 接收者用户 ID。
    private Long receiveUserId;

    // 站内信正文内容。
    private String content;

    // 消息状态：0 未读，1 已读，2 已回复。
    private Byte state;

    // 逻辑删除标记：0 未删除，1 已删除。
    private Byte deleteState;

    // 发送时间。
    private Date createTime;

    // 最近更新时间。
    private Date updateTime;


    // 发送者用户信息，由 MyBatis 关联查询填充。
    private User postUser;

    // 接收者用户信息，由 MyBatis 关联查询填充。
    private User receiveUser;

}
