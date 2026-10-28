package com.cainiao.forum.controller;

import com.cainiao.forum.common.AppResult;
import com.cainiao.forum.common.ResultCode;
import com.cainiao.forum.exception.ApplicationException;
import com.cainiao.forum.model.Board;
import com.cainiao.forum.services.IBoardService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;


@Slf4j
@Api(tags = "版块接口")
@RestController
@RequestMapping("/board")
// 版块接口控制器，负责首页版块列表和单个版块详情查询。
public class BoardController {


    @Value("${cainiao-forum.index.board-num:9}")
    // 首页展示的版块数量，优先读取配置，配置缺失时默认 9 个。
    private Integer indexBoardNum;

    @Resource
    private IBoardService boardService;


    @ApiOperation("获取首页版块列表")
    @GetMapping("/topList")
    // 首页顶部导航需要的版块列表。
    public AppResult<List<Board>> topList () {
        log.info("首页版块个数为：" + indexBoardNum);

        List<Board> boards = boardService.selectByNum(indexBoardNum);

        if (boards == null) {
            // 返回空列表比返回 null 更方便前端遍历。
            boards = new ArrayList<>();
        }

        return AppResult.success(boards);
    }

    @GetMapping("/available")
    public AppResult<List<Board>> available() {
        return AppResult.success(boardService.selectAvailable());
    }

    @ApiOperation("获取版块信息")
    @GetMapping("getById")
    // 根据版块 ID 查询版块信息，帖子列表页会用它显示当前版块名称。
    public AppResult<Board> getById (@ApiParam("版块Id") @RequestParam("id") @NonNull Long id) {

        Board board = boardService.selectById(id);

        // 删除的版块对普通页面来说等同于不存在。
        if (board == null || board.getDeleteState() == 1) {

            log.warn(ResultCode.FAILED_BOARD_NOT_EXISTS.toString());

            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_BOARD_NOT_EXISTS));
        }

        return AppResult.success(board);
    }
}

