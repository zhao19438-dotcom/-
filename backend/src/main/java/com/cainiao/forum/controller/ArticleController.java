package com.cainiao.forum.controller;

import com.cainiao.forum.common.AppResult;
import com.cainiao.forum.common.Constants;
import com.cainiao.forum.common.PageResult;
import com.cainiao.forum.common.ResultCode;
import com.cainiao.forum.config.AppConfig;
import com.cainiao.forum.model.Article;
import com.cainiao.forum.model.Board;
import com.cainiao.forum.model.User;
import com.cainiao.forum.services.IArticleService;
import com.cainiao.forum.services.IBoardService;
import com.cainiao.forum.services.IFileStorageService;
import com.cainiao.forum.utils.StringUtil;
import com.cainiao.forum.utils.SessionIdentity;
import com.cainiao.forum.services.IUserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;


@Api(tags = "文章接口")
@Slf4j
@RestController
@RequestMapping("/article")
// 帖子接口控制器，负责发布、分页列表、搜索、详情、修改、点赞、收藏和删除帖子。
public class ArticleController {

    @Resource
    private IArticleService articleService;
    @Resource
    private IBoardService boardService;
    @Resource
    private IUserService userService;
    @Resource
    private IFileStorageService fileStorageService;


    @ApiOperation("发布新帖")
    @PostMapping("/create")
    // 发帖接口：当前登录用户在指定版块下创建帖子。
    public AppResult create (HttpServletRequest request,
                             @ApiParam("版块Id") @RequestParam("boardId") @NonNull Long boardId,
                             @ApiParam("文章标题") @RequestParam("title") @NonNull String title,
                             @ApiParam("文章内容") @RequestParam("content") @NonNull String content,
                             @ApiParam("业务扩展字段(JSON,可选)") @RequestParam(value = "extJson", required = false) String extJson) {

        // 登录拦截器已经保证用户登录，这里直接从 Session 取当前用户。
        HttpSession session = request.getSession(false);
        User user = (User) session.getAttribute(AppConfig.USER_SESSION);
        if (user.getState() == 1) {

            return AppResult.failed(ResultCode.FAILED_USER_BANNED);
        }

        // 去除标题和正文首尾空格后再做长度校验。
        title = StringUtil.trim(title);
        content = StringUtil.trim(content);
        if (!StringUtil.lengthBetween(title, Constants.TITLE_MIN_LENGTH, Constants.TITLE_MAX_LENGTH)
                || !StringUtil.lengthBetween(content, Constants.CONTENT_MIN_LENGTH, Constants.CONTENT_MAX_LENGTH)) {

            return AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE);
        }

        // 版块必须存在，并且不能处于删除或禁用状态。
        Board board = boardService.selectById(boardId.longValue());
        if (board == null || board.getDeleteState() == 1 || board.getState() == 1) {

            log.warn(ResultCode.FAILED_BOARD_BANNED.toString());

            return AppResult.failed(ResultCode.FAILED_BOARD_BANNED);
        }

        Article article = new Article();
        // Controller 只组装必要字段，默认值和计数维护由 Service 处理。
        article.setTitle(title);
        article.setContent(content);
        article.setBoardId(boardId);
        article.setUserId(user.getId());
        // 扩展字段透传到 Service，由业务处理器解析使用；普通帖子可留空。
        article.setExtJson(extJson);

        articleService.create(article);

        return AppResult.success();
    }


    @ApiOperation("获取帖子列表(分页)")
    @GetMapping("/getAllByBoardId")
    // 获取帖子分页列表：不传 boardId 表示查询全部，传 boardId 表示查询某个版块。
    public AppResult<PageResult<Article>> getAllByBoardId (@ApiParam("版块Id") @RequestParam(value = "boardId", required = false) Long boardId,
                                                            @ApiParam("排序") @RequestParam(value = "orderBy", required = false) String orderBy,
                                                            @ApiParam("页码") @RequestParam(value = "page", required = false) Integer page,
                                                            @ApiParam("每页数量") @RequestParam(value = "size", required = false) Integer size) {

        PageResult<Article> result = articleService.selectPage(boardId, null, orderBy, page, size);

        return AppResult.success(result);
    }


    @ApiOperation("搜索帖子(分页)")
    @GetMapping("/search")
    // 帖子搜索：按标题和正文关键字过滤，可叠加版块过滤，置顶优先返回。
    public AppResult<PageResult<Article>> search (@ApiParam("关键字") @RequestParam(value = "keyword", required = false) String keyword,
                                                   @ApiParam("版块Id") @RequestParam(value = "boardId", required = false) Long boardId,
                                                   @ApiParam("排序") @RequestParam(value = "orderBy", required = false) String orderBy,
                                                   @ApiParam("页码") @RequestParam(value = "page", required = false) Integer page,
                                                   @ApiParam("每页数量") @RequestParam(value = "size", required = false) Integer size) {

        PageResult<Article> result = articleService.selectPage(boardId, keyword, orderBy, page, size);

        return AppResult.success(result);
    }


    @ApiOperation("根据帖子Id获取详情")
    @GetMapping("/details")
    // 帖子详情接口：查询完整帖子信息，并标记当前用户是否为作者、是否已点赞收藏（支持未登录访客查看）。
    public AppResult<Article> getDetails (HttpServletRequest request,
            @ApiParam("帖子Id") @RequestParam("id") @NonNull Long id) {

        User user = SessionIdentity.current(request, userService);
        Long currentUserId = (user != null) ? user.getId() : null;

        Article article = articleService.selectDetailById(id, currentUserId);

        if (article == null) {
            return AppResult.failed(ResultCode.FAILED_ARTICLE_NOT_EXISTS);
        }

        // own 字段供前端判断是否显示编辑、删除按钮。
        if (user != null && user.getId().equals(article.getUserId())) {
            article.setOwn(true);
        }

        return AppResult.success(article);
    }

    @ApiOperation("修改帖子")
    @PostMapping("/modify")
    // 修改帖子：只有帖子作者本人可以修改，并且帖子不能被禁用或删除。
    public AppResult modify (HttpServletRequest request,
                             @ApiParam("帖子Id") @RequestParam("id") @NonNull Long id,
                             @ApiParam("帖子标题") @RequestParam("title") @NonNull String title,
                             @ApiParam("帖子正文") @RequestParam("content") @NonNull String content,
                             @RequestParam(value = "boardId", required = false) Long boardId,
                             @RequestParam(value = "extJson", required = false) String extJson) {

        HttpSession session = request.getSession(false);
        User user = (User) session.getAttribute(AppConfig.USER_SESSION);

        if (user.getState() == 1) {

            return AppResult.failed(ResultCode.FAILED_USER_BANNED);
        }

        // 修改时复用发帖的长度规则。
        title = StringUtil.trim(title);
        content = StringUtil.trim(content);
        if (!StringUtil.lengthBetween(title, Constants.TITLE_MIN_LENGTH, Constants.TITLE_MAX_LENGTH)
                || !StringUtil.lengthBetween(content, Constants.CONTENT_MIN_LENGTH, Constants.CONTENT_MAX_LENGTH)) {

            return AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE);
        }

        Article article = articleService.selectById(id);

        if (article == null) {

            return AppResult.failed(ResultCode.FAILED_ARTICLE_NOT_EXISTS);
        }

        // 普通用户不能修改别人的帖子。
        if (!user.getId().equals(article.getUserId())) {

            return AppResult.failed(ResultCode.FAILED_FORBIDDEN);
        }

        if (article.getState() == 1 || article.getDeleteState() == 1) {

            return AppResult.failed(ResultCode.FAILED_ARTICLE_BANNED);
        }


        articleService.modify(id, title, content, boardId, extJson);

        log.info("帖子更新成功. Article id = " + id + "User id = " + user.getId() + ".");

        return AppResult.success();
    }

    @ApiOperation("点赞")
    @PostMapping("/thumbsUp")
    // 点赞接口：同一用户对同一帖子只有一条点赞记录，重复点赞幂等不重复计数。
    public AppResult<Map<String, Object>> thumbsUp (HttpServletRequest request,
                               @ApiParam("帖子Id") @RequestParam("id") @NonNull Long id) {

        HttpSession session = request.getSession(false);
        User user = (User) session.getAttribute(AppConfig.USER_SESSION);

        if (user.getState() == 1) {

            return AppResult.failed(ResultCode.FAILED_USER_BANNED);
        }

        articleService.thumbsUp(id, user.getId());

        // 返回最新点赞数，前端无需二次查询即可更新界面。
        Article article = articleService.selectById(id);
        Map<String, Object> data = new HashMap<>();
        data.put("likeCount", article.getLikeCount());
        data.put("liked", true);
        return AppResult.success(data);
    }

    @ApiOperation("取消点赞")
    @PostMapping("/thumbsDown")
    // 取消点赞接口：删除点赞记录并减 1，未点赞时幂等。
    public AppResult<Map<String, Object>> thumbsDown (HttpServletRequest request,
                                 @ApiParam("帖子Id") @RequestParam("id") @NonNull Long id) {

        HttpSession session = request.getSession(false);
        User user = (User) session.getAttribute(AppConfig.USER_SESSION);

        if (user.getState() == 1) {

            return AppResult.failed(ResultCode.FAILED_USER_BANNED);
        }

        articleService.thumbsDown(id, user.getId());

        Article article = articleService.selectById(id);
        Map<String, Object> data = new HashMap<>();
        data.put("likeCount", article.getLikeCount());
        data.put("liked", false);
        return AppResult.success(data);
    }

    @ApiOperation("收藏帖子")
    @PostMapping("/collect")
    // 收藏接口：同一用户对同一帖子只有一条收藏记录，重复收藏幂等。
    public AppResult collect (HttpServletRequest request,
                              @ApiParam("帖子Id") @RequestParam("id") @NonNull Long id) {

        HttpSession session = request.getSession(false);
        User user = (User) session.getAttribute(AppConfig.USER_SESSION);

        articleService.collect(id, user.getId());

        return AppResult.success();
    }

    @ApiOperation("取消收藏")
    @PostMapping("/uncollect")
    // 取消收藏接口：删除收藏记录，未收藏时幂等。
    public AppResult uncollect (HttpServletRequest request,
                                @ApiParam("帖子Id") @RequestParam("id") @NonNull Long id) {

        HttpSession session = request.getSession(false);
        User user = (User) session.getAttribute(AppConfig.USER_SESSION);

        articleService.uncollect(id, user.getId());

        return AppResult.success();
    }

    @ApiOperation("我的收藏(分页)")
    @GetMapping("/collections")
    // 查询当前登录用户收藏的帖子列表，按收藏时间倒序。
    public AppResult<PageResult<Article>> myCollections (HttpServletRequest request,
                                   @ApiParam("页码") @RequestParam(value = "page", required = false) Integer page,
                                   @ApiParam("每页数量") @RequestParam(value = "size", required = false) Integer size) {

        HttpSession session = request.getSession(false);
        User user = (User) session.getAttribute(AppConfig.USER_SESSION);

        PageResult<Article> result = articleService.selectCollectPage(user.getId(), page, size);

        return AppResult.success(result);
    }

    @ApiOperation("删除帖子")
    @PostMapping("/delete")
    // 删除帖子：普通删除是逻辑删除，只有作者本人可以操作。
    public AppResult deleteById (HttpServletRequest request,
                                 @ApiParam("帖子Id") @RequestParam("id") @NonNull Long id) {

        HttpSession session = request.getSession(false);
        User user = (User) session.getAttribute(AppConfig.USER_SESSION);
        if (user.getState() == 1) {

            return AppResult.failed(ResultCode.FAILED_USER_BANNED);
        }

        Article article = articleService.selectById(id);

        if (article == null || article.getDeleteState() == 1) {

            return AppResult.failed(ResultCode.FAILED_ARTICLE_NOT_EXISTS);
        }

        // 防止用户通过改请求参数删除别人的帖子。
        if (!user.getId().equals(article.getUserId())) {
            return AppResult.failed(ResultCode.FAILED_FORBIDDEN);
        }

        articleService.deleteById(id);

        return AppResult.success();
    }

    @ApiOperation("获取用户的帖子列表")
    @GetMapping("/getAllByUserId")
    // 查询用户帖子：不传 userId 时查询当前登录用户的帖子。
    public AppResult<java.util.List<Article>> getAllByUserId (HttpServletRequest request ,
                                     @ApiParam("用户Id") @RequestParam(value = "userId", required = false) Long userId) {

        if (userId == null) {

            HttpSession session = request.getSession(false);

            User user = (User) session.getAttribute(AppConfig.USER_SESSION);
            // 个人主页默认展示当前登录用户自己的帖子。
            userId = user.getId();
        }

        java.util.List<Article> articles = articleService.selectByUserId(userId);

        return AppResult.success(articles);
    }

    @ApiOperation("上传帖子图片")
    @PostMapping("/uploadImage")
    // 发帖图片上传：白名单校验扩展名、限制大小，保存到 uploads/article 并返回可访问的 URL。
    public AppResult<Map<String, String>> uploadImage (HttpServletRequest request,
                                                       @ApiParam("图片文件") @RequestParam("file") @NonNull MultipartFile file) {

        HttpSession session = request.getSession(false);
        User user = (User) session.getAttribute(AppConfig.USER_SESSION);

        String url = fileStorageService.saveArticleImage(user.getId(), file);
        Map<String, String> data = new HashMap<>();
        data.put("url", url);
        return AppResult.success(data);
    }

}
