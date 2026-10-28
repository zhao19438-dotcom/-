package com.cainiao.forum.services;

import com.cainiao.forum.common.PageResult;
import com.cainiao.forum.model.Article;
import com.cainiao.forum.model.Board;
import com.cainiao.forum.model.User;

import java.util.Map;

/**
 * 后台管理业务接口：AdminController 只做参数接收和管理员身份校验，
 * 统计、列表、用户/版块/帖子管理等业务规则全部收敛到本层，不再直接操作 Mapper。
 */
public interface IAdminService {

    // 后台概览统计：用户数、版块数、帖子数。
    Map<String, Object> summary ();

    // 分页查询用户列表，支持关键字搜索（用户名/昵称/学号/姓名/学院）。
    PageResult<User> users (String keyword, Integer page, Integer size);

    // 分页查询版块列表。
    PageResult<Board> boards (Integer page, Integer size);

    // 分页查询帖子列表，支持关键字搜索（标题/作者/版块）。
    PageResult<Article> articles (String keyword, Integer page, Integer size);

    // 修改用户禁言状态，state 0 正常，1 禁言；adminId 用于禁止管理员操作自己。
    void updateUserState (Long adminId, Long id, Byte state);

    // 修改用户管理员权限，isAdmin 0 普通用户，1 管理员。
    void updateUserAdmin (Long adminId, Long id, Byte isAdmin);

    // 修改用户逻辑删除状态，deleteState 0 未删除，1 已删除。
    void updateUserDeleteState (Long adminId, Long id, Byte deleteState);

    // 管理员重置用户密码，返回只展示一次的随机临时密码。
    String resetUserPassword (Long id);

    // 新增或编辑版块，board.id 为空表示新增。
    void saveBoard (Board board);

    // 修改版块启用/禁用状态。
    void updateBoardState (Long id, Byte state);

    // 修改版块逻辑删除状态。
    void updateBoardDeleteState (Long id, Byte deleteState);

    // 修改帖子启用/禁用状态。
    void updateArticleState (Long id, Byte state);

    // 修改帖子逻辑删除状态，删除/恢复会同步维护用户与版块计数。
    void updateArticleDeleteState (Long id, Byte deleteState);

    // 设置帖子置顶状态，topState 0 否，1 是。
    void updateArticleTop (Long id, Byte topState);

    // 设置帖子精华状态，essenceState 0 否，1 是。
    void updateArticleEssence (Long id, Byte essenceState);
}
