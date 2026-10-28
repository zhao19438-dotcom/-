package com.cainiao.forum.services.impl;

import com.cainiao.forum.common.AppResult;
import com.cainiao.forum.common.Constants;
import com.cainiao.forum.common.PageResult;
import com.cainiao.forum.common.ResultCode;
import com.cainiao.forum.dao.ArticleMapper;
import com.cainiao.forum.dao.BoardMapper;
import com.cainiao.forum.dao.UserMapper;
import com.cainiao.forum.exception.ApplicationException;
import com.cainiao.forum.model.Article;
import com.cainiao.forum.model.Board;
import com.cainiao.forum.model.User;
import com.cainiao.forum.services.IAdminService;
import com.cainiao.forum.services.IArticleService;
import com.cainiao.forum.services.IBoardService;
import com.cainiao.forum.services.IUserService;
import com.cainiao.forum.utils.PasswordUtil;
import com.cainiao.forum.utils.StringUtil;
import com.cainiao.forum.utils.UUIDUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
// 后台管理业务实现类：统一参数校验与业务规则，AdminController 不再直接读 Mapper。
public class AdminServiceImpl implements IAdminService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private BoardMapper boardMapper;

    @Resource
    private ArticleMapper articleMapper;

    @Resource
    private IArticleService articleService;

    @Resource
    private IUserService userService;

    @Resource
    private IBoardService boardService;

    @Override
    public Map<String, Object> summary() {
        Map<String, Object> result = new HashMap<>();
        result.put("users", userMapper.countAllForAdmin(null));
        result.put("boards", boardMapper.countAllForAdmin());
        result.put("articles", articleMapper.countAllForAdmin(null));
        return result;
    }

    @Override
    public PageResult<User> users(String keyword, Integer page, Integer size) {
        keyword = cleanKeyword(keyword);
        int p = safePage(page);
        int s = safeSize(size);
        int total = userMapper.countAllForAdmin(keyword);
        List<User> records = userMapper.selectAllForAdmin(keyword, offset(p, s), s);
        return PageResult.of(records, total, p, s);
    }

    @Override
    public PageResult<Board> boards(Integer page, Integer size) {
        int p = safePage(page);
        int s = safeSize(size);
        int total = boardMapper.countAllForAdmin();
        List<Board> records = boardMapper.selectAllForAdmin(offset(p, s), s);
        return PageResult.of(records, total, p, s);
    }

    @Override
    public PageResult<Article> articles(String keyword, Integer page, Integer size) {
        keyword = cleanKeyword(keyword);
        int p = safePage(page);
        int s = safeSize(size);
        int total = articleMapper.countAllForAdmin(keyword);
        List<Article> records = articleMapper.selectAllForAdmin(keyword, offset(p, s), s);
        return PageResult.of(records, total, p, s);
    }

    @Override
    public void updateUserState(Long adminId, Long id, Byte state) {
        checkStateParam(state);
        User targetUser = requireUser(id);
        // 防止管理员把自己禁言，导致当前后台账号无法继续管理。
        if (adminId.equals(id) && state == 1) {
            throw new ApplicationException(AppResult.failed("不能禁言当前登录的管理员"));
        }
        User updateUser = new User();
        updateUser.setId(id);
        updateUser.setState(state);
        updateUser.setUpdateTime(new Date());
        userMapper.updateByPrimaryKeySelective(updateUser);
    }

    @Override
    public void updateUserAdmin(Long adminId, Long id, Byte isAdmin) {
        checkStateParam(isAdmin);
        User targetUser = requireUser(id);
        // 防止管理员取消自己的权限后无法继续进入后台。
        if (adminId.equals(id) && isAdmin == 0) {
            throw new ApplicationException(AppResult.failed("不能取消当前登录管理员的权限"));
        }
        User updateUser = new User();
        updateUser.setId(id);
        updateUser.setIsAdmin(isAdmin);
        updateUser.setUpdateTime(new Date());
        userMapper.updateByPrimaryKeySelective(updateUser);
    }

    @Override
    public void updateUserDeleteState(Long adminId, Long id, Byte deleteState) {
        checkStateParam(deleteState);
        User targetUser = requireUser(id);
        // 防止管理员删除自己的账号。
        if (adminId.equals(id) && deleteState == 1) {
            throw new ApplicationException(AppResult.failed("不能删除当前登录的管理员"));
        }
        User updateUser = new User();
        updateUser.setId(id);
        updateUser.setDeleteState(deleteState);
        updateUser.setUpdateTime(new Date());
        userMapper.updateByPrimaryKeySelective(updateUser);
    }

    @Override
    public String resetUserPassword(Long id) {
        requireUser(id);
        String temporaryPassword = UUIDUtil.UUID_32().substring(0, 10);
        User updateUser = new User();
        updateUser.setId(id);
        // 临时密码随机生成并使用 BCrypt 存储，避免所有账号共享同一个默认密码。
        updateUser.setPassword(PasswordUtil.encode(temporaryPassword));
        updateUser.setSalt("");
        updateUser.setUpdateTime(new Date());
        int row = userMapper.updateByPrimaryKeySelective(updateUser);
        if (row != 1) {
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED));
        }
        log.info("管理员重置用户密码成功, userId = " + id);
        return temporaryPassword;
    }

    @Override
    public void saveBoard(Board board) {
        if (board != null) {
            Board existing = board.getId() == null ? null : boardMapper.selectByPrimaryKey(board.getId());
            String type = board.getBizType();
            if (type == null || type.trim().isEmpty()) type = existing == null || existing.getBizType() == null ? "forum" : existing.getBizType();
            if (!java.util.Arrays.asList("forum", "lostfound", "announcement").contains(type)) {
                throw new ApplicationException(AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE));
            }
            if (existing != null && existing.getArticleCount() != null && existing.getArticleCount() > 0
                    && "lostfound".equals(type) != "lostfound".equals(existing.getBizType())) {
                throw new ApplicationException(AppResult.failed("已有帖子的版块不能切换失物招领类型"));
            }
            board.setBizType(type);
        }
        if (board == null || !StringUtil.lengthBetween(board.getName(), Constants.BOARD_NAME_MIN_LENGTH, Constants.BOARD_NAME_MAX_LENGTH)
                || board.getSort() == null || board.getSort() < Constants.BOARD_SORT_MIN || board.getSort() > Constants.BOARD_SORT_MAX
                || board.getState() == null || board.getState() < 0 || board.getState() > 1) {
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE));
        }
        Date date = new Date();
        board.setUpdateTime(date);
        if (board.getId() == null) {
            // 新增版块时初始化统计数量、删除状态和创建时间。
            board.setArticleCount(0);
            board.setDeleteState((byte) 0);
            board.setCreateTime(date);
            boardMapper.insertSelective(board);
        } else {
            if (boardMapper.selectByPrimaryKey(board.getId()) == null) {
                throw new ApplicationException(AppResult.failed(ResultCode.FAILED_BOARD_NOT_EXISTS));
            }
            boardMapper.updateByPrimaryKeySelective(board);
        }
    }

    @Override
    public void updateBoardState(Long id, Byte state) {
        checkStateParam(state);
        requireBoard(id);
        Board board = new Board();
        board.setId(id);
        board.setState(state);
        board.setUpdateTime(new Date());
        boardMapper.updateByPrimaryKeySelective(board);
    }

    @Override
    public void updateBoardDeleteState(Long id, Byte deleteState) {
        checkStateParam(deleteState);
        requireBoard(id);
        Board board = new Board();
        board.setId(id);
        board.setDeleteState(deleteState);
        board.setUpdateTime(new Date());
        boardMapper.updateByPrimaryKeySelective(board);
    }

    @Override
    public void updateArticleState(Long id, Byte state) {
        checkStateParam(state);
        requireArticle(id);
        Article article = new Article();
        article.setId(id);
        article.setState(state);
        article.setUpdateTime(new Date());
        articleMapper.updateByPrimaryKeySelective(article);
    }

    @Override
    public void updateArticleDeleteState(Long id, Byte deleteState) {
        checkStateParam(deleteState);
        Article existsArticle = requireArticle(id);
        if (deleteState == 1) {
            // 从未删除改为删除时，走 Service 以保证计数同步减少。
            if (existsArticle.getDeleteState() == 0) {
                articleService.deleteById(id);
            }
            return;
        }
        // 恢复帖子时先把 deleteState 改回 0。
        Article article = new Article();
        article.setId(id);
        article.setDeleteState((byte) 0);
        article.setUpdateTime(new Date());
        articleMapper.updateByPrimaryKeySelective(article);
        if (existsArticle.getDeleteState() == 1) {
            // 只有从删除状态恢复时，才把用户和版块帖子数加回来。
            userService.addOneArticleCountById(existsArticle.getUserId());
            boardService.addOneArticleCountById(existsArticle.getBoardId());
        }
    }

    @Override
    public void updateArticleTop(Long id, Byte topState) {
        checkStateParam(topState);
        requireArticle(id);
        Article article = new Article();
        article.setId(id);
        article.setTopState(topState);
        article.setUpdateTime(new Date());
        articleMapper.updateTopState(article);
    }

    @Override
    public void updateArticleEssence(Long id, Byte essenceState) {
        checkStateParam(essenceState);
        requireArticle(id);
        Article article = new Article();
        article.setId(id);
        article.setEssenceState(essenceState);
        article.setUpdateTime(new Date());
        articleMapper.updateEssenceState(article);
    }

    private User requireUser(Long id) {
        if (id == null) {
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE));
        }
        User user = userMapper.selectByPrimaryKey(id);
        if (user == null) {
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_USER_NOT_EXISTS));
        }
        return user;
    }

    private Board requireBoard(Long id) {
        if (id == null) {
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE));
        }
        Board board = boardMapper.selectByPrimaryKey(id);
        if (board == null) {
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_BOARD_NOT_EXISTS));
        }
        return board;
    }

    private Article requireArticle(Long id) {
        if (id == null) {
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE));
        }
        Article article = articleMapper.selectByPrimaryKey(id);
        if (article == null) {
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_ARTICLE_NOT_EXISTS));
        }
        return article;
    }

    private void checkStateParam(Byte state) {
        if (state == null || state < 0 || state > 1) {
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE));
        }
    }

    private String cleanKeyword(String keyword) {
        // 空搜索词统一转为 null，XML 中就不会拼接 like 条件。
        if (StringUtil.isEmpty(keyword)) {
            return null;
        }
        return keyword.trim();
    }

    private int safePage(Integer page) {
        // 页码非法时回到第一页。
        if (page == null || page < 1) {
            return Constants.DEFAULT_PAGE;
        }
        return page;
    }

    private int safeSize(Integer size) {
        // 每页数量非法时使用默认值，过大时限制到最大值。
        if (size == null || size < 1) {
            return Constants.DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, Constants.MAX_PAGE_SIZE);
    }

    private int offset(int page, int size) {
        // MySQL limit 的 offset 从 0 开始，所以第一页偏移量是 0。
        return (page - 1) * size;
    }
}
