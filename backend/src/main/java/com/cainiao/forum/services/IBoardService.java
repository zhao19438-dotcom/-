package com.cainiao.forum.services;

import com.cainiao.forum.model.Board;

import java.util.List;

// 版块业务接口，负责首页版块查询和版块帖子数维护。
public interface IBoardService {


    // 按数量查询首页展示的版块。
    List<Board> selectByNum (Integer num);
    List<Board> selectAvailable();


    // 根据 ID 查询单个版块。
    Board selectById (Long id);


    // 新增帖子后，版块帖子数加 1。
    void addOneArticleCountById (Long id);


    // 删除帖子后，版块帖子数减 1。
    void subOneArticleCountById (Long id);
}

