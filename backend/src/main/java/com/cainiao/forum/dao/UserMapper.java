package com.cainiao.forum.dao;

import com.cainiao.forum.model.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
// 用户表 Mapper，方法名会和 resources/mapper 下的 XML SQL 对应。
public interface UserMapper {
    // 完整插入用户记录，所有字段都参与 SQL。
    int insert(User row);

    // 选择性插入，只有非空字段参与 SQL。
    int insertSelective(User row);

    // 根据主键查询用户。
    User selectByPrimaryKey(Long id);

    // 选择性更新，只有非空字段参与 SQL。
    int updateByPrimaryKeySelective(User row);

    // 全字段更新用户记录。
    int updateByPrimaryKey(User row);

    // 根据用户名查询用户，用于登录和注册查重。
    User selectByUserName (@Param("username") String username);

    // 根据学号查询用户，用于注册时校验学号未被其他学生使用。
    User selectByStudentNo (@Param("studentNo") String studentNo);

    // 后台用户管理分页查询，可按关键字搜索用户名或昵称。
    List<User> selectAllForAdmin(@Param("keyword") String keyword,
                                 @Param("offset") Integer offset,
                                 @Param("limit") Integer limit);

    // 后台用户管理统计总数，用于计算分页。
    int countAllForAdmin(@Param("keyword") String keyword);

    // 用户发帖数原子加 1。
    int increaseArticleCount(@Param("id") Long id);

    // 用户发帖数原子减 1，最小为 0。
    int decreaseArticleCount(@Param("id") Long id);
}

