package com.cainiao.forum.controller;

import com.cainiao.forum.common.AppResult;
import com.cainiao.forum.common.Constants;
import com.cainiao.forum.common.ResultCode;
import com.cainiao.forum.config.AppConfig;
import com.cainiao.forum.model.Article;
import com.cainiao.forum.model.ArticleReply;
import com.cainiao.forum.model.User;
import com.cainiao.forum.services.IArticleReplyService;
import com.cainiao.forum.services.IArticleService;
import com.cainiao.forum.utils.StringUtil;
import com.cainiao.forum.utils.SessionIdentity;
import com.cainiao.forum.services.IUserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.List;


@Api(tags = "回复接口")
@Slf4j
@RestController
@RequestMapping("/reply")
// 回复接口控制器，负责发表回复（支持楼中楼）和查询帖子下的回复列表。
public class ArticleReplyController {

    @Resource
    private IArticleService articleService;
    @Resource
    private IUserService userService;
    @Resource
    private IArticleReplyService articleReplyService;

    @ApiOperation("回复帖子")
    @PostMapping("/create")
    // 创建回复：当前登录用户对某个帖子发表评论；可选 replyId 表示回复某条已有评论（楼中楼）。
    public AppResult create (HttpServletRequest request,
                             @ApiParam("帖子Id") @RequestParam("articleId") @NonNull Long articleId,
                             @ApiParam("帖子内容") @RequestParam("content") @NonNull String content,
                             @ApiParam("被回复的回复Id(楼中楼,可选)") @RequestParam(value = "replyId", required = false) Long replyId) {

        HttpSession session = request.getSession(false);
        User user = (User) session.getAttribute(AppConfig.USER_SESSION);

        if (user.getState() == 1) {

            return AppResult.failed(ResultCode.FAILED_USER_BANNED);
        }

        // 回复先去除首尾空格，再判断是否为空或过长。
        content = StringUtil.trim(content);
        if (!StringUtil.lengthBetween(content, Constants.REPLY_MIN_LENGTH, Constants.REPLY_MAX_LENGTH)) {
            return AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE);
        }

        // 被回复的帖子必须存在，并且不能已删除。
        HttpSession readSession = request.getSession(false);
        User viewer = readSession == null ? null : (User) readSession.getAttribute(AppConfig.USER_SESSION);
        Article article = articleService.requireReadable(articleId, viewer == null ? null : viewer.getId());

        if (article == null || article.getDeleteState() == 1) {

            return AppResult.failed(ResultCode.FAILED_ARTICLE_NOT_EXISTS);
        }

        if (article.getState() == 1) {

            return AppResult.failed(ResultCode.FAILED_ARTICLE_BANNED);
        }


        ArticleReply articleReply = new ArticleReply();
        // 只设置创建回复所需字段，默认状态和时间由 Service 处理。
        articleReply.setArticleId(articleId);
        articleReply.setPostUserId(user.getId());
        articleReply.setContent(content);
        // 楼中楼：replyId 指向被回复的评论，由 Service 校验归属并解析 replyUserId。
        articleReply.setReplyId(replyId);

        articleReplyService.create(articleReply);

        return AppResult.success();
    }

    @ApiOperation("获取回复列表")
    @GetMapping("/getReplies")
    // 查询某个帖子下的回复列表，楼中楼回复带出被回复用户信息；登录用户会额外标记每条回复的点赞状态。
    public AppResult<List<ArticleReply>> getRepliesByArticleId (@ApiParam("帖子Id") @RequestParam("articleId") @NonNull Long articleId,
                                                                HttpServletRequest request) {

        User user = SessionIdentity.current(request, userService);
        articleService.requireReadable(articleId, user == null ? null : user.getId());
        List<ArticleReply> articleReplies = articleReplyService.selectByArticleId(articleId,
                user != null ? user.getId() : null);

        return AppResult.success(articleReplies);

    }

    @ApiOperation("点赞回复")
    @PostMapping("/thumbsUp")
    // 给某条回复点赞：同一用户对同一回复只有一条点赞记录（幂等）。
    public AppResult thumbsUp (HttpServletRequest request,
                               @ApiParam("回复Id") @RequestParam("id") @NonNull Long id) {

        HttpSession session = request.getSession(false);
        User user = (User) session.getAttribute(AppConfig.USER_SESSION);

        if (user.getState() == 1) {

            return AppResult.failed(ResultCode.FAILED_USER_BANNED);
        }

        articleReplyService.thumbsUp(id, user.getId());

        return AppResult.success();
    }

    @ApiOperation("取消回复点赞")
    @PostMapping("/thumbsDown")
    // 取消对某条回复的点赞（幂等）。
    public AppResult thumbsDown (HttpServletRequest request,
                                 @ApiParam("回复Id") @RequestParam("id") @NonNull Long id) {

        HttpSession session = request.getSession(false);
        User user = (User) session.getAttribute(AppConfig.USER_SESSION);

        articleReplyService.thumbsDown(id, user.getId());

        return AppResult.success();
    }
}
