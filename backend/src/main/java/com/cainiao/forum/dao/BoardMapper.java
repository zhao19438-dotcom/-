package com.cainiao.forum.dao;

import com.cainiao.forum.model.Board;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
// 版块表 Mapper，负责 t_board 的基础 CRUD 和后台分页查询。
public interface BoardMapper {
    // 完整插入版块记录。
    int insert(Board row);

    // 选择性插入版块记录。
    int insertSelective(Board row);

    // 根据主键查询版块。
    Board selectByPrimaryKey(Long id);

    // 选择性更新版块字段。
    int updateByPrimaryKeySelective(Board row);

    // 全字段更新版块记录。
    int updateByPrimaryKey(Board row);

    // 查询首页展示的前 num 个版块。
    List<Board> selectByNum (@Param("num") Integer num);
    List<Board> selectAvailable();

    // 后台版块管理分页查询。
    List<Board> selectAllForAdmin(@Param("offset") Integer offset,
                                  @Param("limit") Integer limit);

    // 后台版块管理统计总数。
    int countAllForAdmin();

    // 版块帖子数原子加 1。
    int increaseArticleCount(@Param("id") Long id);

    // 版块帖子数原子减 1，最小为 0。
    int decreaseArticleCount(@Param("id") Long id);
}

