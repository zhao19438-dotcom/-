package com.cainiao.forum.dao;

import com.cainiao.forum.model.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
// 通知聚合 Mapper：把回复、点赞、收藏、举报处置四类事件实时聚合为通知流，不落地存储。
public interface NotificationMapper {

    // 查询当前用户收到的全部类型通知，按时间倒序，limit 限制返回条数。
    List<Notification> selectActivityNotifications(@Param("userId") Long userId,
                                                   @Param("limit") Integer limit);
}
