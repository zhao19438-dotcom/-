package com.cainiao.forum.controller;

import com.cainiao.forum.common.AppResult;
import com.cainiao.forum.common.Constants;
import com.cainiao.forum.common.ResultCode;
import com.cainiao.forum.config.AppConfig;
import com.cainiao.forum.model.User;
import com.cainiao.forum.services.IAuditLogService;
import com.cainiao.forum.services.IFileStorageService;
import com.cainiao.forum.services.IUserService;
import com.cainiao.forum.utils.CaptchaUtil;
import com.cainiao.forum.utils.StringUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.awt.image.BufferedImage;
import java.io.IOException;




@Api(tags = "用户接口")

@Slf4j

@RestController

@RequestMapping("/user")
// 用户接口控制器，负责注册、登录、查询资料、修改资料和修改密码。
public class UserController {

    @Resource
    // 用户业务对象，Controller 只负责接收请求，具体业务规则交给 Service。
    private IUserService userService;

    @Resource
    private IFileStorageService fileStorageService;

    @Resource
    // 审计日志服务：用于记录登录成功/失败等安全事件。
    private IAuditLogService auditLogService;


    @ApiOperation("用户注册")
    @PostMapping("/register")
    // 注册接口：接收表单参数，校验后创建一个学生用户。
    public AppResult register (HttpServletRequest request,
                               @ApiParam("用户名") @RequestParam("username") @NonNull String username,
                               @ApiParam("学号") @RequestParam("studentNo") @NonNull String studentNo,
                               @ApiParam("真实姓名") @RequestParam("realName") @NonNull String realName,
                               @ApiParam("学院") @RequestParam("college") @NonNull String college,
                               @ApiParam("班级") @RequestParam("className") @NonNull String className,
                               @ApiParam("昵称") @RequestParam("nickname") @NonNull String nickname,
                               @ApiParam("密码") @RequestParam("password") @NonNull String password,
                               @ApiParam("确认密码") @RequestParam("passwordRepeat") @NonNull String passwordRepeat,
                               @ApiParam(value = "验证码", required = false) @RequestParam(value = "captcha", required = false) String captcha) {

        // 注册必须通过一次性验证码，避免绕过前端直接批量创建账号。
        HttpSession preSession = request.getSession(false);
        String expected = preSession == null ? null : (String) preSession.getAttribute(CaptchaUtil.SESSION_KEY);
        if (StringUtil.isEmpty(captcha) || expected == null
                || !expected.equalsIgnoreCase(StringUtil.trim(captcha))) {
            return AppResult.failed(ResultCode.FAILED_CAPTCHA);
        }
        preSession.removeAttribute(CaptchaUtil.SESSION_KEY);

        // 去除用户输入前后的空格，避免 "admin " 和 "admin" 被当成两个用户名。
        username = StringUtil.trim(username);
        nickname = StringUtil.trim(nickname);
        studentNo = StringUtil.trim(studentNo);
        realName = StringUtil.trim(realName);
        college = StringUtil.trim(college);
        className = StringUtil.trim(className);

        // 本论坛仅面向学生开放：学号、姓名、学院、班级都是必填项。
        if (StringUtil.isEmpty(studentNo) || StringUtil.isEmpty(realName)
                || StringUtil.isEmpty(college) || StringUtil.isEmpty(className)) {
            return AppResult.failed("学号、姓名、学院、班级均为必填项");
        }

        // 学号只允许纯数字，长度 6 到 20。
        if (!studentNo.matches("^[0-9]{6,20}$")) {
            return AppResult.failed("学号格式不正确，应为 6-20 位数字");
        }

        // 密码长度必须在允许范围内，太短不安全，太长也没有必要。
        if (!StringUtil.lengthBetween(password, Constants.PASSWORD_MIN_LENGTH, Constants.PASSWORD_MAX_LENGTH)) {
            return AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE);
        }

        // 注册时要求两次输入的密码一致。
        if (!password.equals(passwordRepeat)) {
            log.warn(ResultCode.FAILED_TWO_PWD_NOT_SAME.toString());

            return AppResult.failed(ResultCode.FAILED_TWO_PWD_NOT_SAME);
        }

        User user = new User();
        user.setUsername(username);
        user.setNickname(nickname);
        // 学生身份信息：学号唯一，注册后在管理后台可见。
        user.setStudentNo(studentNo);
        user.setRealName(realName);
        user.setCollege(college);
        user.setClassName(className);

        // 密码传明文给 Service，由 Service 统一使用 BCrypt 加密后入库。
        user.setPassword(password);


        userService.createNormalUser(user);

        return AppResult.success();
    }


    @ApiOperation("用户登录")
    @PostMapping("/login")
    // 登录接口：校验账号密码，成功后把用户信息写入 Session（支持免验证码登录）。
    public AppResult login (HttpServletRequest request,
                            @ApiParam("用户名") @RequestParam("username") @NonNull String username,
                            @ApiParam("密码") @RequestParam("password") @NonNull String password,
                            @ApiParam(value = "验证码", required = false) @RequestParam(value = "captcha", required = false) String captcha) {

        // 验证码非必填：若前端传递了验证码则进行一次性校验，未传递则直接放行密码登录
        if (!StringUtil.isEmpty(captcha)) {
            HttpSession preSession = request.getSession(false);
            String expected = preSession == null ? null : (String) preSession.getAttribute(CaptchaUtil.SESSION_KEY);
            if (expected == null || !expected.equalsIgnoreCase(StringUtil.trim(captcha))) {
                return AppResult.failed(ResultCode.FAILED_CAPTCHA);
            }
            preSession.removeAttribute(CaptchaUtil.SESSION_KEY);
        }

        User user;
        try {
            user = userService.login(username, password);
        } catch (RuntimeException e) {
            // 登录失败写入审计日志，便于发现暴力破解行为。
            auditLogService.log(null, username, "登录失败", "login", null,
                    "账号或密码不正确", clientIp(request));
            throw e;
        }

        auditLogService.log(user.getId(), user.getUsername(), "登录成功", "login",
                String.valueOf(user.getId()), "账号密码校验通过", clientIp(request));

        // 防会话固定：登录成功后先销毁旧 Session（连同其中的属性），
        // 再创建全新 Session 写入用户，攻击者无法凭登录前的 Session ID 冒充登录态。
        HttpSession oldSession = request.getSession(false);
        if (oldSession != null) {
            oldSession.invalidate();
        }
        HttpSession session = request.getSession(true);
        // 登录可能将旧密码哈希升级为 BCrypt，使用数据库中的最新凭据建立会话。
        user = userService.selectById(user.getId());
        session.setAttribute(AppConfig.USER_SESSION, user);
        session.setAttribute(AppConfig.USER_CREDENTIAL, user.getPassword());

        return AppResult.success();
    }

    // 取客户端真实 IP：优先 X-Forwarded-For（Nginx 反代场景），退回 remoteAddr。
    private String clientIp (HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            int idx = xff.indexOf(',');
            return idx > 0 ? xff.substring(0, idx).trim() : xff.trim();
        }
        return request.getRemoteAddr();
    }


    @ApiOperation("获取用户信息")
    @GetMapping("/info")
    // 查询用户信息：不传 id 时查当前登录用户；传 id 且为他人时，严格执行隐私脱敏（隐藏学号、手机号、真实姓名、密保等）。
    public AppResult<User> getUserInfo (HttpServletRequest request,
                                        @ApiParam("用户Id") @RequestParam(value = "id", required = false) Long id) {

        HttpSession session = request.getSession(false);
        User sessionUser = (session != null) ? (User) session.getAttribute(AppConfig.USER_SESSION) : null;

        User user;
        boolean isSelf = (id == null || (sessionUser != null && id.equals(sessionUser.getId())));

        if (isSelf) {
            // 查询自己时，从数据库获取最新完整信息（允许本人查看自己的学号、手机号等）
            if (sessionUser == null) {
                return AppResult.failed(ResultCode.FAILED_UNAUTHORIZED);
            }
            user = userService.selectById(sessionUser.getId());
        } else {
            // 查询他人资料页，查询后通过 maskForPublic 严格脱敏，阻断 IDOR 隐私泄露
            user = userService.selectById(id);
            if (user != null) {
                user = userService.maskForPublic(user);
            }
        }

        if (user == null) {
            return AppResult.failed(ResultCode.FAILED_USER_NOT_EXISTS);
        }

        return AppResult.success(user);
    }



    @ApiOperation("退出登录")
    @PostMapping("/logout")
    // 退出登录：销毁当前 Session，浏览器就不再保持登录态。
    public AppResult logout (HttpServletRequest request) {

        HttpSession session = request.getSession(false);

        if (session != null) {

            log.info("退出成功");

            session.invalidate();
        }

        return AppResult.success("退出成功");
    }


    @ApiOperation("修改个人信息")
    @PostMapping("/modifyInfo")
    // 修改个人资料：只允许修改当前登录用户自己的资料。
    public AppResult modifyInfo (HttpServletRequest request,
                                 @ApiParam("用户名") @RequestParam(value = "username",required = false) String username,
                                 @ApiParam("昵称") @RequestParam(value = "nickname",required = false) String nickname,
                                 @ApiParam("性别") @RequestParam(value = "gender",required = false) Byte gender,
                                 @ApiParam("邮箱") @RequestParam(value = "email",required = false) String email,
                                 @ApiParam("电话号") @RequestParam(value = "phoneNum",required = false) String phoneNum,
                                 @ApiParam("学院") @RequestParam(value = "college",required = false) String college,
                                 @ApiParam("班级") @RequestParam(value = "className",required = false) String className,
                                 @ApiParam("个人简介") @RequestParam(value = "remark",required = false) String remark) {


        // 所有字段都没传时，直接返回失败，避免无意义的数据库更新。
        if (StringUtil.isEmpty(username) && StringUtil.isEmpty(nickname)
           && StringUtil.isEmpty(email) && StringUtil.isEmpty(phoneNum)
           && StringUtil.isEmpty(college) && StringUtil.isEmpty(className)
           && StringUtil.isEmpty(remark) && gender == null) {

            return AppResult.failed("请输入要修改的内容");
        }


        HttpSession session = request.getSession(false);
        User user = (User) session.getAttribute(AppConfig.USER_SESSION);


        // 只设置允许修改的字段，Service 会继续做合法性校验。
        // 注意：学号与姓名注册后不可修改，接口层不接收这两个参数。
        User updateUser = new User();
        updateUser.setId(user.getId());
        updateUser.setUsername(username);
        updateUser.setNickname(nickname);
        updateUser.setGender(gender);
        updateUser.setEmail(email);
        updateUser.setPhoneNum(phoneNum);
        updateUser.setCollege(college);
        updateUser.setClassName(className);
        updateUser.setRemark(remark);


        userService.modifyInfo(updateUser);

        // 修改成功后重新查询用户，保证 Session 中保存的是最新资料。
        user = userService.selectById(user.getId());

        session.setAttribute(AppConfig.USER_SESSION, user);

        return AppResult.success(user);
    }

    @ApiOperation("修改密码")
    @PostMapping("/modifyPwd")
    // 修改密码：校验新密码和确认密码，再交给 Service 校验旧密码。
    public AppResult modifyPassword (HttpServletRequest request,
                                     @ApiParam("原密码") @RequestParam("oldPassword") @NonNull String oldPassword,
                                     @ApiParam("新密码") @RequestParam("newPassword") @NonNull String newPassword,
                                     @ApiParam("确认密码") @RequestParam("passwordRepeat") @NonNull String passwordRepeat) {        // 新密码也必须符合长度规则。
        if (!StringUtil.lengthBetween(newPassword, Constants.PASSWORD_MIN_LENGTH, Constants.PASSWORD_MAX_LENGTH)) {
            return AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE);
        }

        // 确认密码用于防止用户输错新密码。
        if (!newPassword.equals(passwordRepeat)) {

            return AppResult.failed(ResultCode.FAILED_TWO_PWD_NOT_SAME);
        }

        HttpSession session = request.getSession(false);
        User user = (User) session.getAttribute(AppConfig.USER_SESSION);

        userService.modifyPassword(user.getId(), newPassword, oldPassword);

        // 密码修改后让用户重新登录，避免旧 Session 长期有效。
        if (session != null) {
            session.invalidate();
        }

        return AppResult.success();
    }


    @ApiOperation("上传头像")
    @PostMapping("/avatar")
    // 头像上传：白名单校验扩展名、限制大小、按用户重命名，保存后更新 avatarUrl。
    public AppResult<User> uploadAvatar (HttpServletRequest request,
                                          @ApiParam("头像文件") @RequestParam("file") @NonNull MultipartFile file) throws IOException {

        HttpSession session = request.getSession(false);
        User user = (User) session.getAttribute(AppConfig.USER_SESSION);

        String avatarUrl = fileStorageService.saveAvatar(user.getId(), file);
        User updateUser = new User();
        updateUser.setId(user.getId());
        updateUser.setAvatarUrl(avatarUrl);
        userService.modifyAvatar(user.getId(), avatarUrl);

        // 返回最新用户信息，前端立即刷新头像显示。
        User latest = userService.selectById(user.getId());
        session.setAttribute(AppConfig.USER_SESSION, latest);
        return AppResult.success(latest);
    }


    @ApiOperation("获取图形验证码")
    @GetMapping("/captcha")
    // 验证码接口：生成图片直接写回响应，文本存入 Session（校验一次后即失效）。
    public void captcha (HttpServletRequest request, HttpServletResponse response) throws IOException {

        String code = CaptchaUtil.generateCode();
        request.getSession(true).setAttribute(CaptchaUtil.SESSION_KEY, code);

        // 禁止浏览器缓存验证码图片，否则点击刷新不会真正换一张。
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        response.setContentType("image/png");
        BufferedImage image = CaptchaUtil.generateImage(code);
        ImageIO.write(image, "png", response.getOutputStream());
    }


    @ApiOperation("获取密保问题")
    @GetMapping("/securityQuestion")
    // 忘记密码第一步：用用户名换回该账号设置的密保问题。
    public AppResult securityQuestion (@ApiParam("用户名") @RequestParam("username") @NonNull String username) {

        User user = userService.selectByUserName(username);
        if (user == null || StringUtil.isEmpty(user.getSecurityQuestion())) {
            return AppResult.failed("该账号未设置密保问题，请联系管理员处理");
        }

        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("securityQuestion", user.getSecurityQuestion());
        return AppResult.success(data);
    }


    @ApiOperation("通过密保问题重置密码")
    @PostMapping("/resetPwd")
    // 忘记密码第二步：校验图形验证码与密保答案，通过后把密码重置为新密码。
    public AppResult resetPwd (HttpServletRequest request,
                               @ApiParam("用户名") @RequestParam("username") @NonNull String username,
                               @ApiParam("密保答案") @RequestParam("securityAnswer") @NonNull String securityAnswer,
                               @ApiParam("新密码") @RequestParam("newPassword") @NonNull String newPassword,
                               @ApiParam("确认密码") @RequestParam("passwordRepeat") @NonNull String passwordRepeat,
                               @ApiParam("验证码") @RequestParam("captcha") @NonNull String captcha) {

        // 校验图形验证码，防止攻击者自动化爆破密保答案
        HttpSession session = request.getSession(false);
        String expected = session == null ? null : (String) session.getAttribute(CaptchaUtil.SESSION_KEY);
        if (expected == null || !expected.equalsIgnoreCase(StringUtil.trim(captcha))) {
            return AppResult.failed(ResultCode.FAILED_CAPTCHA);
        }
        session.removeAttribute(CaptchaUtil.SESSION_KEY);

        if (!StringUtil.lengthBetween(newPassword, Constants.PASSWORD_MIN_LENGTH, Constants.PASSWORD_MAX_LENGTH)) {
            return AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE);
        }
        if (!newPassword.equals(passwordRepeat)) {
            return AppResult.failed(ResultCode.FAILED_TWO_PWD_NOT_SAME);
        }

        userService.resetPasswordBySecurity(username, securityAnswer, newPassword);

        return AppResult.success("密码重置成功，请使用新密码登录");
    }



    @ApiOperation("设置或修改密保问题")
    @PostMapping("/setSecurity")
    // 设置密保：登录用户为自己设置找回密码用的问题与答案。
    public AppResult setSecurity (HttpServletRequest request,
                                  @ApiParam("密保问题") @RequestParam("question") @NonNull String question,
                                  @ApiParam("密保答案") @RequestParam("answer") @NonNull String answer) {

        HttpSession session = request.getSession(false);
        User user = (User) session.getAttribute(AppConfig.USER_SESSION);

        userService.setSecurityQuestion(user.getId(), question, answer);

        return AppResult.success();
    }


}

