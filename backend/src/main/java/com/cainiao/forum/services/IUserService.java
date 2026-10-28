package com.cainiao.forum.services;


import com.cainiao.forum.model.User;


// 用户业务接口，定义用户注册、登录、资料修改和发帖数维护等能力。
public interface IUserService {


    // 创建普通用户，包含参数校验、用户名查重和默认字段初始化。
    void createNormalUser (User user);


    // 根据用户名查询用户，登录和注册查重都会用到。
    User selectByUserName (String username);


    // 校验用户名和密码，成功时返回用户对象。
    User login (String username, String password);


    // 根据用户 ID 查询用户。
    User selectById (Long id);


    // 用户发帖成功后，发帖数加 1。
    void addOneArticleCountById (Long id);


    // 用户帖子被删除后，发帖数减 1。
    void subOneArticleCountById (Long id);



    // 修改用户资料，只更新用户真正提交并通过校验的字段。
    void modifyInfo (User user);


    // 修改密码，需要先校验原密码，新密码使用 BCrypt 存储。
    void modifyPassword (Long id, String newPassword, String oldPassword);


    // 修改头像路径，头像上传成功后调用。
    void modifyAvatar (Long id, String avatarUrl);


    // 设置或更新密保问题与答案，答案以 BCrypt 密文保存。
    void setSecurityQuestion (Long id, String question, String answer);


    // 忘记密码：校验密保答案通过后，把密码重置为新密码。
    void resetPasswordBySecurity (String username, String securityAnswer, String newPassword);

    // 对外公开展示时的数据脱敏：隐藏真实姓名、学号脱敏、清空手机/邮箱/密保等私密字段。
    User maskForPublic (User user);

}

