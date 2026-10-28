package com.cainiao.forum.services.impl;

import com.cainiao.forum.common.AppResult;
import com.cainiao.forum.common.ResultCode;
import com.cainiao.forum.dao.BoardMapper;
import com.cainiao.forum.exception.ApplicationException;
import com.cainiao.forum.model.Board;
import com.cainiao.forum.services.IBoardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;


@Slf4j
@Service
// 版块业务实现类，负责版块查询和版块帖子数维护。
public class BoardServiceImpl implements IBoardService {

    @Resource
    private BoardMapper boardMapper;

    @Override
    public List<Board> selectByNum(Integer num) {

        // 首页查询数量必须为正数。
        if (num <= 0) {

            log.warn(ResultCode.FAILED_PARAMS_VALIDATE.toString());

            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE));
        }

        // 查询未删除、正常状态的前 num 个版块。
        List<Board> result = boardMapper.selectByNum(num);

        return result;
    }

    @Override
    public List<Board> selectAvailable() { return boardMapper.selectAvailable(); }

    @Override
    public Board selectById(Long id) {
        // 版块 ID 必须合法。
        if (id == null || id <= 0) {

            log.warn(ResultCode.FAILED_BOARD_ARTICLE_COUNT.toString());

            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_BOARD_ARTICLE_COUNT));
        }

        // 按主键查询版块，是否删除由调用方按场景判断。
        Board board = boardMapper.selectByPrimaryKey(id);

        return board;
    }

    @Override
    public void addOneArticleCountById(Long id) {
        // 发帖后维护版块帖子数。
        if (id == null || id <= 0) {

            log.warn(ResultCode.FAILED_BOARD_ARTICLE_COUNT.toString());

            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_BOARD_ARTICLE_COUNT));
        }

        Board board = boardMapper.selectByPrimaryKey(id);
        if (board == null) {

            log.warn(ResultCode.ERROR_IS_NULL.toString() + ", board id = " + id);

            throw new ApplicationException(AppResult.failed(ResultCode.ERROR_IS_NULL));
        }

        Board updateBoard = new Board();
        updateBoard.setId(board.getId());
        // 在数据库当前值基础上加 1。
        updateBoard.setArticleCount(board.getArticleCount() + 1);

        int row = boardMapper.updateByPrimaryKeySelective(updateBoard);

        if (row != 1) {
            log.warn(ResultCode.FAILED.toString() + ", 受影响的行数不等于 1 .");
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED));
        }
    }

    @Override
    public void subOneArticleCountById(Long id) {

        // 删除帖子后维护版块帖子数。
        if (id == null || id <= 0) {

            log.warn(ResultCode.FAILED_BOARD_ARTICLE_COUNT.toString());

            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_BOARD_ARTICLE_COUNT));
        }

        Board board = boardMapper.selectByPrimaryKey(id);
        if (board == null) {

            log.warn(ResultCode.FAILED_BOARD_NOT_EXISTS.toString() + ", board id = " + id);

            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_BOARD_NOT_EXISTS));
        }

        Board updateBoard = new Board();
        updateBoard.setId(board.getId());
        updateBoard.setArticleCount(board.getArticleCount() - 1);

        // 防止重复删除等异常情况导致计数变成负数。
        if (updateBoard.getArticleCount() < 0) {

            updateBoard.setArticleCount(0);
        }


        int row = boardMapper.updateByPrimaryKeySelective(updateBoard);
        if (row != 1) {
            log.warn(ResultCode.FAILED.toString() + ", 受影响的行数不等于 1 .");
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED));
        }
    }
}

