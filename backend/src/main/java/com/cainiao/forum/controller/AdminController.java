package com.cainiao.forum.controller;

import com.cainiao.forum.common.AppResult;
import com.cainiao.forum.common.PageResult;
import com.cainiao.forum.common.ResultCode;
import com.cainiao.forum.config.AppConfig;
import com.cainiao.forum.model.Article;
import com.cainiao.forum.model.Board;
import com.cainiao.forum.model.User;
import com.cainiao.forum.services.IAdminService;
import com.cainiao.forum.services.IUserService;
import com.cainiao.forum.utils.StringUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Date;
import java.util.Map;

@RestController
@RequestMapping("/admin")
// 后台管理控制器：只负责接收参数与鉴权，具体业务与数据访问全部由 AdminService 处理。
public class AdminController {

    @Resource
    // 后台业务服务，统计/列表/管理操作都走它，Controller 不再直接操作 Mapper。
    private IAdminService adminService;

    @Resource
    private IUserService userService;

    @GetMapping("/current")
    // 获取当前管理员信息，前端进入后台页面时用它判断是否有权限。
    public AppResult<User> current(HttpServletRequest request) {
        User admin = currentAdmin(request);
        if (admin == null) {
            return AppResult.failed(ResultCode.FAILED_FORBIDDEN);
        }
        return AppResult.success(admin);
    }

    @GetMapping("/summary")
    // 后台概览统计：用户数、版块数、帖子数。
    public AppResult<Map<String, Object>> summary(HttpServletRequest request) {
        if (currentAdmin(request) == null) {
            return AppResult.failed(ResultCode.FAILED_FORBIDDEN);
        }
        return AppResult.success(adminService.summary());
    }

    @GetMapping("/users")
    // 后台学生列表，支持按学号、姓名、用户名等关键字搜索和分页。
    public AppResult<PageResult<User>> users(HttpServletRequest request,
                                             @RequestParam(value = "keyword", required = false) String keyword,
                                             @RequestParam(value = "page", required = false) Integer page,
                                             @RequestParam(value = "size", required = false) Integer size) {
        if (currentAdmin(request) == null) {
            return AppResult.failed(ResultCode.FAILED_FORBIDDEN);
        }
        return AppResult.success(adminService.users(cleanKeyword(keyword), page, size));
    }

    @PostMapping("/users/state")
    // 修改用户禁言状态，state 为 0 正常，1 禁言。
    public AppResult updateUserState(HttpServletRequest request,
                                     @RequestParam("id") Long id,
                                     @RequestParam("state") Byte state) {
        User admin = currentAdmin(request);
        if (admin == null) {
            return AppResult.failed(ResultCode.FAILED_FORBIDDEN);
        }
        adminService.updateUserState(admin.getId(), id, state);
        return AppResult.success();
    }

    @PostMapping("/users/admin")
    // 修改用户管理员权限，isAdmin 为 0 普通用户，1 管理员。
    public AppResult updateUserAdmin(HttpServletRequest request,
                                     @RequestParam("id") Long id,
                                     @RequestParam("isAdmin") Byte isAdmin) {
        User admin = currentAdmin(request);
        if (admin == null) {
            return AppResult.failed(ResultCode.FAILED_FORBIDDEN);
        }
        adminService.updateUserAdmin(admin.getId(), id, isAdmin);
        return AppResult.success();
    }

    @PostMapping("/users/delete")
    // 修改用户逻辑删除状态，deleteState 为 0 未删除，1 已删除。
    public AppResult updateUserDeleteState(HttpServletRequest request,
                                           @RequestParam("id") Long id,
                                           @RequestParam("deleteState") Byte deleteState) {
        User admin = currentAdmin(request);
        if (admin == null) {
            return AppResult.failed(ResultCode.FAILED_FORBIDDEN);
        }
        adminService.updateUserDeleteState(admin.getId(), id, deleteState);
        return AppResult.success();
    }

    @PostMapping("/users/resetPassword")
    // 管理员生成随机临时密码；临时密码只在本次响应中展示。
    public AppResult<String> resetUserPassword(HttpServletRequest request,
                                       @RequestParam("id") Long id) {
        if (currentAdmin(request) == null) {
            return AppResult.failed(ResultCode.FAILED_FORBIDDEN);
        }
        String temporaryPassword = adminService.resetUserPassword(id);
        return AppResult.success(ResultCode.SUCCESS.getMessage(), temporaryPassword);
    }

    @GetMapping("/boards")
    // 后台版块列表，支持分页。
    public AppResult<PageResult<Board>> boards(HttpServletRequest request,
                                               @RequestParam(value = "page", required = false) Integer page,
                                               @RequestParam(value = "size", required = false) Integer size) {
        if (currentAdmin(request) == null) {
            return AppResult.failed(ResultCode.FAILED_FORBIDDEN);
        }
        return AppResult.success(adminService.boards(page, size));
    }

    @PostMapping("/boards/save")
    // 新增或编辑版块：id 为空表示新增，id 不为空表示修改。
    public AppResult saveBoard(HttpServletRequest request,
                               @RequestParam(value = "id", required = false) Long id,
                               @RequestParam("name") String name,
                               @RequestParam(value = "sort", required = false, defaultValue = "0") Integer sort,
                               @RequestParam(value = "state", required = false, defaultValue = "0") Byte state,
                               @RequestParam(value = "bizType", required = false) String bizType) {
        if (currentAdmin(request) == null) {
            return AppResult.failed(ResultCode.FAILED_FORBIDDEN);
        }
        Board board = new Board();
        board.setId(id);
        board.setName(StringUtil.trim(name));
        board.setSort(sort);
        board.setState(state);
        board.setBizType(bizType);
        board.setUpdateTime(new Date());
        adminService.saveBoard(board);
        return AppResult.success();
    }

    @PostMapping("/boards/state")
    // 修改版块启用/禁用状态。
    public AppResult updateBoardState(HttpServletRequest request,
                                      @RequestParam("id") Long id,
                                      @RequestParam("state") Byte state) {
        if (currentAdmin(request) == null) {
            return AppResult.failed(ResultCode.FAILED_FORBIDDEN);
        }
        adminService.updateBoardState(id, state);
        return AppResult.success();
    }

    @PostMapping("/boards/delete")
    // 修改版块逻辑删除状态。
    public AppResult updateBoardDeleteState(HttpServletRequest request,
                                            @RequestParam("id") Long id,
                                            @RequestParam("deleteState") Byte deleteState) {
        if (currentAdmin(request) == null) {
            return AppResult.failed(ResultCode.FAILED_FORBIDDEN);
        }
        adminService.updateBoardDeleteState(id, deleteState);
        return AppResult.success();
    }

    @GetMapping("/articles")
    // 后台帖子列表，支持标题关键字搜索和分页（置顶帖排在最前）。
    public AppResult<PageResult<Article>> articles(HttpServletRequest request,
                                                   @RequestParam(value = "keyword", required = false) String keyword,
                                                   @RequestParam(value = "page", required = false) Integer page,
                                                   @RequestParam(value = "size", required = false) Integer size) {
        if (currentAdmin(request) == null) {
            return AppResult.failed(ResultCode.FAILED_FORBIDDEN);
        }
        return AppResult.success(adminService.articles(cleanKeyword(keyword), page, size));
    }

    @PostMapping("/articles/state")
    // 修改帖子启用/禁用状态。
    public AppResult updateArticleState(HttpServletRequest request,
                                        @RequestParam("id") Long id,
                                        @RequestParam("state") Byte state) {
        if (currentAdmin(request) == null) {
            return AppResult.failed(ResultCode.FAILED_FORBIDDEN);
        }
        adminService.updateArticleState(id, state);
        return AppResult.success();
    }

    @PostMapping("/articles/delete")
    // 后台删除或恢复帖子，删除走 Service 以同步维护用户和版块计数。
    public AppResult updateArticleDeleteState(HttpServletRequest request,
                                              @RequestParam("id") Long id,
                                              @RequestParam("deleteState") Byte deleteState) {
        if (currentAdmin(request) == null) {
            return AppResult.failed(ResultCode.FAILED_FORBIDDEN);
        }
        adminService.updateArticleDeleteState(id, deleteState);
        return AppResult.success();
    }

    @PostMapping("/articles/top")
    // 置顶或取消置顶：置顶帖在列表与首页中排在最前。
    public AppResult updateArticleTop(HttpServletRequest request,
                                      @RequestParam("id") Long id,
                                      @RequestParam("topState") Byte topState) {
        if (currentAdmin(request) == null) {
            return AppResult.failed(ResultCode.FAILED_FORBIDDEN);
        }
        adminService.updateArticleTop(id, topState);
        return AppResult.success();
    }

    @PostMapping("/articles/essence")
    // 设为精华或取消精华。
    public AppResult updateArticleEssence(HttpServletRequest request,
                                          @RequestParam("id") Long id,
                                          @RequestParam("essenceState") Byte essenceState) {
        if (currentAdmin(request) == null) {
            return AppResult.failed(ResultCode.FAILED_FORBIDDEN);
        }
        adminService.updateArticleEssence(id, essenceState);
        return AppResult.success();
    }

    // 从 Session 取当前用户并校验管理员身份：必须已登录、是管理员、未被禁言、未删除。
    private User currentAdmin(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        User user = (User) session.getAttribute(AppConfig.USER_SESSION);
        if (user == null || user.getId() == null) {
            return null;
        }
        // 每次回查数据库，保证权限变更立即生效。
        User currentUser = userService.selectById(user.getId());
        if (currentUser == null
                || currentUser.getIsAdmin() == null
                || currentUser.getIsAdmin() != 1
                || currentUser.getState() == 1
                || currentUser.getDeleteState() == 1) {
            return null;
        }
        session.setAttribute(AppConfig.USER_SESSION, currentUser);
        return currentUser;
    }

    // 空搜索词统一转为 null，XML 中就不会拼接 like 条件。
    private String cleanKeyword(String keyword) {
        if (StringUtil.isEmpty(keyword)) {
            return null;
        }
        return keyword.trim();
    }
}
