package com.cainiao.forum.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

@ApiModel("用户信息")
@Data
// 用户实体类，对应数据库中的 t_user 表。
public class User {
    // 用户主键 ID。
    private Long id;

    @ApiModelProperty("用户名")
    // 登录时使用的唯一账号。
    private String username;

    @ApiModelProperty("学号")
    // 学生学号，注册必填且全校唯一，用于确认学生身份。
    private String studentNo;

    @ApiModelProperty("真实姓名")
    // 学生真实姓名，注册必填。
    private String realName;

    @ApiModelProperty("学院")
    // 学生所在学院。
    private String college;

    @ApiModelProperty("班级")
    // 学生所在班级。
    private String className;

    @JsonIgnore
    // 加密后的密码，不返回给前端。
    private String password;

    @ApiModelProperty("昵称")
    // 页面展示用的名称，可以和用户名不同。
    private String nickname;

    @ApiModelProperty("电话号码")
    // 用户绑定的手机号。
    private String phoneNum;

    @ApiModelProperty("邮箱")
    // 用户绑定的邮箱。
    private String email;

    @ApiModelProperty("密保问题")
    // 密保问题，用户忘记密码时用于身份校验。
    private String securityQuestion;

    @JsonIgnore
    // 密保答案（BCrypt 密文），不返回给前端。
    private String securityAnswer;

    @ApiModelProperty("性别")
    // 性别：0 女，1 男，2 保密。
    private Byte gender;

    @JsonIgnore
    // 密码盐，不返回给前端，用来提升密码加密安全性。
    private String salt;

    @ApiModelProperty("头像地址")
    @JsonInclude(JsonInclude.Include.ALWAYS)
    // 头像图片路径，允许为空也返回给前端，便于前端使用默认头像。
    private String avatarUrl;

    @ApiModelProperty("发帖数量")
    // 当前用户发布的帖子数量。
    private Integer articleCount;

    @ApiModelProperty("是否管理员")
    // 是否管理员：0 普通用户，1 管理员。
    private Byte isAdmin;

    @ApiModelProperty("个人简介")
    // 用户填写的个人介绍。
    private String remark;

    @ApiModelProperty("用户状态")
    // 用户状态：0 正常，1 禁言。
    private Byte state;

    // 逻辑删除标记：0 未删除，1 已删除。
    private Byte deleteState;

    @ApiModelProperty("注册日期")
    // 账号创建时间。
    private Date createTime;

    // 最近一次资料更新时间。
    private Date updateTime;
}

