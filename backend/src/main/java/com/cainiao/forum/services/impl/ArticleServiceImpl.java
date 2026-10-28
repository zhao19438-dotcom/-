package com.cainiao.forum.services.impl;

import com.cainiao.forum.common.AppResult;
import com.cainiao.forum.common.Constants;
import com.cainiao.forum.common.PageResult;
import com.cainiao.forum.common.ResultCode;
import com.cainiao.forum.dao.ArticleCollectMapper;
import com.cainiao.forum.dao.ArticleLikeMapper;
import com.cainiao.forum.dao.ArticleMapper;
import com.cainiao.forum.dao.LostFoundMapper;
import com.cainiao.forum.model.LostFound;
import com.cainiao.forum.exception.ApplicationException;
import com.cainiao.forum.model.Article;
import com.cainiao.forum.model.Board;
import com.cainiao.forum.model.User;
import com.cainiao.forum.services.IArticleService;
import com.cainiao.forum.services.IBoardService;
import com.cainiao.forum.services.IUserService;
import com.cainiao.forum.biz.BizHandler;
import com.cainiao.forum.biz.BizHandlerRegistry;
import com.cainiao.forum.services.ISensitiveWordService;
import com.cainiao.forum.utils.JsonUtil;
import com.cainiao.forum.utils.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Map;


@Slf4j
@Service
// 帖子业务实现类，负责帖子校验、创建、分页查询、点赞收藏、修改和逻辑删除。
public class ArticleServiceImpl implements IArticleService {

    @Resource
    private ArticleMapper articleMapper;

    @Resource
    private ArticleLikeMapper articleLikeMapper;

    @Resource
    private ArticleCollectMapper articleCollectMapper;
    @Resource
    private LostFoundMapper lostFoundMapper;

    @Resource
    private IUserService userService;
    @Resource
    private IBoardService boardService;

    @Resource
    private BizHandlerRegistry bizHandlerRegistry;

    @Resource
    private ISensitiveWordService sensitiveWordService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    // 创建帖子涉及帖子表、用户表、版块表三处更新，任何一步失败都要回滚。
    public void create(Article article) {

        // 必须有发帖用户和所属版块。
        if (article == null || article.getUserId() == null || article.getBoardId() == null
            || article.getUserId() <= 0 || article.getBoardId() <= 0) {

            log.warn(ResultCode.FAILED_PARAMS_VALIDATE.toString());

            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE));
        }

        // 去除用户输入首尾空格，再做长度校验。
        article.setTitle(StringUtil.trim(article.getTitle()));
        article.setContent(StringUtil.trim(article.getContent()));

        // 内容清洗：XSS 危险脚本过滤与敏感词过滤
        article.setTitle(StringUtil.cleanXss(sensitiveWordService.filter(article.getTitle())));
        article.setContent(StringUtil.cleanXss(sensitiveWordService.filter(article.getContent())));


        if (!StringUtil.lengthBetween(article.getTitle(), Constants.TITLE_MIN_LENGTH, Constants.TITLE_MAX_LENGTH)
            || !StringUtil.lengthBetween(article.getContent(), Constants.CONTENT_MIN_LENGTH, Constants.CONTENT_MAX_LENGTH)) {

            log.warn(ResultCode.FAILED_PARAMS_VALIDATE.toString());

            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE));
        }

        // 发帖用户必须存在且未删除。
        User user = userService.selectById(article.getUserId());
        if (user == null || user.getDeleteState() == 1) {

            log.warn(ResultCode.FAILED_USER_NOT_EXISTS.toString() + ", user id = " + article.getUserId());

            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_USER_NOT_EXISTS));
        }

        // 被禁言用户不能发帖。
        if (user.getState() == 1) {

            log.warn(ResultCode.FAILED_USER_BANNED.toString() + ", user id = " + article.getUserId());

            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_USER_BANNED));
        }

        // 版块必须存在、未删除、未禁用。
        Board board = boardService.selectById(article.getBoardId());
        if (board == null || board.getDeleteState() == 1 || board.getState() == 1) {

            log.warn(ResultCode.FAILED_BOARD_BANNED.toString() + ", board id = " + article.getBoardId());

            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_BOARD_BANNED));
        }

        // 解析业务类型与扩展字段，交给对应业务处理器校验/处理（模块化扩展点）。
        // 版块若没有显式业务类型，回退为通用 forum 处理器。
        String bizType = board.getBizType() == null ? "forum" : board.getBizType();
        article.setBizType(bizType);
        Map<String, Object> ext = JsonUtil.parseExt(article.getExtJson());
        BizHandler handler = bizHandlerRegistry.get(bizType);
        handler.validate(article, ext);
        handler.beforePublish(article, ext);

        // 初始化帖子统计值和状态字段。
        article.setVisitCount(0);
        article.setReplyCount(0);
        article.setLikeCount(0);
        article.setTopState((byte) 0);
        article.setEssenceState((byte) 0);
        article.setDeleteState((byte) 0);
        article.setState((byte) 0);
        Date date = new Date();
        article.setCreateTime(date);
        article.setUpdateTime(date);

        // 新增帖子本体。
        int articleRow = articleMapper.insertSelective(article);
        if (articleRow <= 0) {
            log.warn(ResultCode.FAILED_CREATE.toString());
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_CREATE));
        }

        // 入库后交给业务处理器写扩展表（此时 article.getId() 已由 MyBatis 回填）。
        handler.afterPublish(article, ext);

        // 发帖成功后，用户发帖数加 1。
        userService.addOneArticleCountById(user.getId());

        // 发帖成功后，版块帖子数加 1。
        boardService.addOneArticleCountById(board.getId());


        log.info(ResultCode.SUCCESS.toString() + ", user id = " + article.getUserId()
                + ", board id = " + article.getBoardId() + ", article id = "+article.getId() + "发帖成功");

    }

    @Override
    public PageResult<Article> selectPage(Long boardId, String keyword, String orderBy, Integer page, Integer size) {

        // 修正分页参数：页码从 1 开始，每页数量有上限。
        int p = (page == null || page < 1) ? Constants.DEFAULT_PAGE : page;
        int s = (size == null || size < 1) ? Constants.DEFAULT_PAGE_SIZE : Math.min(size, Constants.MAX_PAGE_SIZE);

        // 版块 ID 传了才过滤，非法正数外的值视为未传。
        if (boardId != null && boardId <= 0) {
            boardId = null;
        }

        // 搜索词去空格，空串视为不搜索。
        if (keyword != null) {
            keyword = keyword.trim();
            if (keyword.isEmpty()) {
                keyword = null;
            }
        }

        int total = articleMapper.countPage(boardId, keyword);
        // 仅允许白名单排序，避免把用户输入直接拼进 SQL。
        String order = "hot".equals(orderBy) ? "hot" : "latest";
        List<Article> records = articleMapper.selectPage(boardId, keyword, order, (p - 1) * s, s);

        return PageResult.of(records, total, p, s);
    }

    @Override
    public List<Article> selectByUserId(Long userId) {

        // 查询用户帖子前先校验用户 ID。
        if (userId == null || userId <= 0) {

            log.warn(ResultCode.FAILED_PARAMS_VALIDATE.toString());

            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE));
        }

        // 用户不存在时直接返回业务错误。
        User user = userService.selectById(userId);
        if (user == null) {

            log.warn(ResultCode.FAILED_USER_NOT_EXISTS.toString() + ", user id = " + userId);

            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_USER_NOT_EXISTS));
        }

        // 查询该用户发布的帖子列表。
        List<Article> articles = articleMapper.selectByUserId(userId);

        return articles;
    }

    @Override
    public Article selectDetailById(Long id, Long currentUserId) {
        Article article = requireReadable(id, currentUserId);
        int row = articleMapper.increaseVisitCount(id);
        if (row == 1) article.setVisitCount(article.getVisitCount() + 1);
        if (currentUserId != null) {
            article.setLiked(articleLikeMapper.countByArticleAndUser(id, currentUserId) > 0);
            article.setCollected(articleCollectMapper.countByArticleAndUser(id, currentUserId) > 0);
        }
        return article;
    }

    @Override
    public Article requireReadable(Long id, Long currentUserId) {

        // 详情查询需要合法的帖子 ID。
        if (id == null || id <= 0) {

            log.warn(ResultCode.FAILED_PARAMS_VALIDATE.toString());

            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE));
        }

        // 详情查询会带出作者、版块和正文。
        Article article = articleMapper.selectDetailById(id);

        if (article == null || Byte.valueOf((byte) 1).equals(article.getDeleteState())
                || article.getBoard() == null
                || Byte.valueOf((byte) 1).equals(article.getBoard().getDeleteState())) {

            log.warn(ResultCode.FAILED_ARTICLE_NOT_EXISTS.toString());

            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_ARTICLE_NOT_EXISTS));
        }

        if (Byte.valueOf((byte) 1).equals(article.getState()) && !article.getUserId().equals(currentUserId)) {
            User viewer = currentUserId == null ? null : userService.selectById(currentUserId);
            if (viewer == null || !Byte.valueOf((byte) 1).equals(viewer.getIsAdmin())
                    || Byte.valueOf((byte) 1).equals(viewer.getDeleteState())) {
                throw new ApplicationException(AppResult.failed(ResultCode.FAILED_ARTICLE_NOT_EXISTS));
            }
        }

        return article;
    }

    @Override
    public Article selectById(Long id) {

        // 只查询基础帖子记录时，也先校验 ID。
        if (id == null || id <= 0) {

            log.warn(ResultCode.FAILED_PARAMS_VALIDATE.toString());

            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE));
        }

        Article article = articleMapper.selectByPrimaryKey(id);

        return article;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void modify(Long id, String title, String content) {
        modify(id, title, content, null, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void modify(Long id, String title, String content, Long boardId, String extJson) {

        // 修改前清理标题和正文（XSS危险标签清洗与敏感词过滤）。
        title = StringUtil.cleanXss(sensitiveWordService.filter(StringUtil.trim(title)));
        content = StringUtil.cleanXss(sensitiveWordService.filter(StringUtil.trim(content)));


        // 修改帖子必须有合法 ID、标题和正文。
        if (id == null || id <= 0
            || !StringUtil.lengthBetween(title, Constants.TITLE_MIN_LENGTH, Constants.TITLE_MAX_LENGTH)
            || !StringUtil.lengthBetween(content, Constants.CONTENT_MIN_LENGTH, Constants.CONTENT_MAX_LENGTH)) {

            log.warn(ResultCode.FAILED_PARAMS_VALIDATE.toString());

            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE));
        }

        Article existing = articleMapper.selectByPrimaryKey(id);
        if (existing == null || Byte.valueOf((byte) 1).equals(existing.getDeleteState())) {
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_ARTICLE_NOT_EXISTS));
        }
        Long targetBoardId = boardId == null ? existing.getBoardId() : boardId;
        Board targetBoard = boardService.selectById(targetBoardId);
        if (targetBoard == null || Byte.valueOf((byte) 1).equals(targetBoard.getState())
                || Byte.valueOf((byte) 1).equals(targetBoard.getDeleteState())) {
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_BOARD_BANNED));
        }
        String bizType = targetBoard.getBizType() == null ? "forum" : targetBoard.getBizType();
        String extension = extJson == null ? existing.getExtJson() : extJson;
        if (!"lostfound".equals(bizType)) extension = "";
        Map<String, Object> ext = JsonUtil.parseExt(extension);
        Article candidate = new Article();
        candidate.setId(id); candidate.setBizType(bizType); candidate.setExtJson(extension);
        BizHandler handler = bizHandlerRegistry.get(bizType);
        handler.validate(candidate, ext);

        // 帖子、所属版块计数和业务扩展表在同一事务内更新。
        Article updateArticle = new Article();
        updateArticle.setId(id);
        updateArticle.setTitle(title);
        updateArticle.setContent(content);
        updateArticle.setBoardId(targetBoardId);
        updateArticle.setBizType(bizType);
        updateArticle.setExtJson(extension);
        updateArticle.setUpdateTime(new Date());

        int row = articleMapper.updateByPrimaryKeySelective(updateArticle);
        if (row != 1) {

            log.warn(ResultCode.ERROR_SERVICES.toString());

            throw new ApplicationException(AppResult.failed(ResultCode.ERROR_SERVICES));
        }
        if (!targetBoardId.equals(existing.getBoardId())) {
            boardService.subOneArticleCountById(existing.getBoardId());
            boardService.addOneArticleCountById(targetBoardId);
        }
        if ("lostfound".equals(bizType)) {
            LostFound previous = lostFoundMapper.selectByArticleId(id);
            if (previous == null) handler.afterPublish(candidate, ext);
            else {
                previous.setLostType(String.valueOf(ext.get("lostType")));
                previous.setItemName(ext.get("itemName") == null ? null : String.valueOf(ext.get("itemName")));
                previous.setLocation(ext.get("location") == null ? null : String.valueOf(ext.get("location")));
                previous.setContact(String.valueOf(ext.get("contact")));
                lostFoundMapper.updateDetails(previous);
            }
        } else lostFoundMapper.deleteByArticleId(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    // 点赞与计数两步必须在同一事务里，任一失败整体回滚。
    public void thumbsUp(Long articleId, Long userId) {

        checkArticleForInteract(articleId);

        // insert ignore 配合唯一索引：首次点赞插入成功返回 1，重复点赞返回 0（幂等，不重复计数）。
        int row = articleLikeMapper.insertIgnore(articleId, userId);
        if (row == 1) {
            // 只有真正新增了点赞记录才把帖子点赞数原子加 1。
            articleMapper.increaseLikeCount(articleId);
        }
        log.info("点赞成功, article id = " + articleId + ", user id = " + userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void thumbsDown(Long articleId, Long userId) {

        checkArticleForInteract(articleId);

        // 删除点赞记录：已点赞返回 1 并原子减 1，未点赞返回 0（幂等）。
        int row = articleLikeMapper.delete(articleId, userId);
        if (row == 1) {
            articleMapper.decreaseLikeCount(articleId);
        }
        log.info("取消点赞, article id = " + articleId + ", user id = " + userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void collect(Long articleId, Long userId) {

        checkArticleForInteract(articleId);

        // insert ignore 配合唯一索引：重复收藏幂等。
        int row = articleCollectMapper.insertIgnore(articleId, userId);
        if (row == 1) {
            log.info("收藏成功, article id = " + articleId + ", user id = " + userId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void uncollect(Long articleId, Long userId) {

        checkArticleForInteract(articleId);

        // 取消收藏：未收藏时幂等。
        articleCollectMapper.delete(articleId, userId);
        log.info("取消收藏, article id = " + articleId + ", user id = " + userId);
    }

    @Override
    public PageResult<Article> selectCollectPage(Long userId, Integer page, Integer size) {

        if (userId == null || userId <= 0) {
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE));
        }
        int p = (page == null || page < 1) ? Constants.DEFAULT_PAGE : page;
        int s = (size == null || size < 1) ? Constants.DEFAULT_PAGE_SIZE : Math.min(size, Constants.MAX_PAGE_SIZE);
        int total = articleCollectMapper.countCollectByUserId(userId);
        List<Article> records = articleCollectMapper.selectCollectPage(userId, (p - 1) * s, s);
        return PageResult.of(records, total, p, s);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    // 删除帖子要同步减少版块和用户的帖子数量，所以使用事务。
    public void deleteById(Long id) {

        // 删除必须指定合法帖子 ID。
        if (id == null || id <= 0) {

            log.warn(ResultCode.FAILED_PARAMS_VALIDATE.toString());

            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE));
        }

        Article article = articleMapper.selectByPrimaryKey(id);
        // 已删除或不存在的帖子不能重复删除。
        if (article == null || article.getDeleteState() == 1) {

            log.warn(ResultCode.FAILED_BOARD_NOT_EXISTS.toString() + ", article id = " + id);

            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_BOARD_NOT_EXISTS));
        }

        // 这里执行逻辑删除，只把 deleteState 改成 1。
        Article updateArticle = new Article();
        updateArticle.setId(article.getId());
        updateArticle.setDeleteState((byte) 1);

        int row = articleMapper.updateByPrimaryKeySelective(updateArticle);
        if (row != 1) {

            log.warn(ResultCode.ERROR_SERVICES.toString());

            throw new ApplicationException(AppResult.failed(ResultCode.ERROR_SERVICES));
        }

        // 逻辑删除成功后，版块帖子数减 1。
        boardService.subOneArticleCountById(article.getBoardId());

        // 逻辑删除成功后，用户发帖数减 1。
        userService.subOneArticleCountById(article.getUserId());
        log.info("删除帖子成功, article id = " + article.getId() + ", user id = " + article.getUserId() + ".");
    }

    @Override
    public void addOneReplyCountById(Long id) {

        // 回复数更新必须指定合法帖子 ID。
        if (id == null || id <= 0) {

            log.warn(ResultCode.FAILED_PARAMS_VALIDATE.toString());

            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE));
        }

        Article article = articleMapper.selectByPrimaryKey(id);

        // 已删除帖子不能增加回复数。
        if (article == null || article.getDeleteState() == 1) {

            log.warn(ResultCode.FAILED_ARTICLE_NOT_EXISTS.toString());

            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_ARTICLE_NOT_EXISTS));
        }

        // 被禁用帖子不能增加回复数。
        if (article.getState() == 1) {

            log.warn(ResultCode.FAILED_ARTICLE_BANNED.toString());

            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_ARTICLE_BANNED));
        }

        // 回复数原子加 1。
        int row = articleMapper.increaseReplyCount(id);
        if (row != 1) {

            log.warn(ResultCode.ERROR_SERVICES.toString());

            throw new ApplicationException(AppResult.failed(ResultCode.ERROR_SERVICES));
        }
    }

    // 点赞/收藏共用的帖子状态校验：必须存在、未删除、未禁用。
    private void checkArticleForInteract(Long articleId) {
        if (articleId == null || articleId <= 0) {
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE));
        }
        Article article = articleMapper.selectByPrimaryKey(articleId);
        if (article == null || article.getDeleteState() == 1) {
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_ARTICLE_NOT_EXISTS));
        }
        if (article.getState() == 1) {
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_ARTICLE_BANNED));
        }
    }

}
