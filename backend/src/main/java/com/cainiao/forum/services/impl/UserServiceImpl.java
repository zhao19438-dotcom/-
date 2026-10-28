package com.cainiao.forum.services.impl;

import com.cainiao.forum.common.AppResult;
import com.cainiao.forum.common.Constants;
import com.cainiao.forum.common.ResultCode;
import com.cainiao.forum.dao.UserMapper;
import com.cainiao.forum.exception.ApplicationException;
import com.cainiao.forum.model.User;
import com.cainiao.forum.services.IUserService;
import com.cainiao.forum.utils.PasswordUtil;
import com.cainiao.forum.utils.StringUtil;
import com.cainiao.forum.utils.UUIDUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.regex.Pattern;

@Slf4j        // Lombok 自动生成 log 对象，供下面 log.warn / log.info 使用
@Service      // 注册为 Spring 容器管理的 Service Bean
public class UserServiceImpl implements IUserService {

    // 用户名只允许字母、数字、下划线，长度 3 到 20。
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{3,20}$");
    // 学号只允许纯数字，长度 6 到 20，避免冒用非学生身份注册。
    private static final Pattern STUDENT_NO_PATTERN = Pattern.compile("^[0-9]{6,20}$");
    // 邮箱格式基础校验。
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    // 手机号格式基础校验，允许国际区号、空格和短横线。
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9\\- ]{6,20}$");

    @Resource
    private UserMapper userMapper;

    @Override
    public void createNormalUser(User user) {
        // Service 层再次校验参数，防止绕过 Controller 直接调用。
        if (user == null) {
            log.warn(ResultCode.FAILED_PARAMS_VALIDATE.toString());
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE));
        }
        // 保存前清理用户输入，避免首尾空格影响唯一性和展示。
        user.setUsername(StringUtil.trim(user.getUsername()));
        user.setNickname(StringUtil.trim(user.getNickname()));
        user.setStudentNo(StringUtil.trim(user.getStudentNo()));
        user.setRealName(StringUtil.trim(user.getRealName()));
        user.setCollege(StringUtil.trim(user.getCollege()));
        user.setClassName(StringUtil.trim(user.getClassName()));
        // 注册必须提供合法用户名、昵称、加密密码、盐，以及完整的学生身份信息。
        if (!validUsername(user.getUsername())
                || !validStudentNo(user.getStudentNo())
                || StringUtil.isEmpty(user.getRealName())
                || !StringUtil.lengthBetween(user.getRealName(), 2, 50)
                || StringUtil.isEmpty(user.getCollege())
                || !StringUtil.lengthBetween(user.getCollege(), 2, 50)
                || StringUtil.isEmpty(user.getClassName())
                || !StringUtil.lengthBetween(user.getClassName(), 1, 50)
                || StringUtil.isEmpty(user.getPassword())
                || !StringUtil.lengthBetween(user.getPassword(), Constants.PASSWORD_MIN_LENGTH, Constants.PASSWORD_MAX_LENGTH)) {
            log.warn(ResultCode.FAILED_PARAMS_VALIDATE.toString());
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE));
        }
        // 用户名唯一，注册前先查重。
        User existsUser = userMapper.selectByUserName(user.getUsername());
        if (existsUser != null) {
            log.info(ResultCode.FAILED_USER_EXISTS.toString());
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_USER_EXISTS));
        }
        // 学号唯一，一个学号只能注册一个账号。
        User existsByStudentNo = userMapper.selectByStudentNo(user.getStudentNo());
        if (existsByStudentNo != null) {
            log.info("学号已被注册. studentNo = " + user.getStudentNo());
            throw new ApplicationException(AppResult.failed("该学号已注册，请勿重复使用"));
        }
        // 密码在 Service 层统一加密：BCrypt 自带随机盐，无需单独的盐字段。
        user.setPassword(PasswordUtil.encode(user.getPassword()));
        // salt 列为 NOT NULL，BCrypt 用户写入空串占位。
        user.setSalt("");
        // 初始化普通用户默认值，避免数据库里出现 null 状态。
        user.setGender((byte) 2);
        user.setArticleCount(0);
        user.setIsAdmin((byte) 0);
        user.setState((byte) 0);
        user.setDeleteState((byte) 0);
        Date date = new Date();
        // 创建时间和更新时间在新增时相同。
        user.setCreateTime(date);
        user.setUpdateTime(date);
        // insertSelective 只插入非空字段，适合部分字段允许为空的用户表。
        int row = userMapper.insertSelective(user);
        if (row != 1) {
            log.info(ResultCode.FAILED_CREATE.toString());
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_CREATE));
        }
        log.info("新用户成功. username = " + user.getUsername() + ". ");
    }

    @Override
    public User selectByUserName(String username) {
        username = StringUtil.trim(username);
        // 查询前也校验用户名，避免非法字符串进入 SQL 查询。
        if (!validUsername(username)) {
            log.warn(ResultCode.FAILED_PARAMS_VALIDATE.toString());
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE));
        }
        return userMapper.selectByUserName(username);
    }

    @Override
    public User login(String username, String password) {
        username = StringUtil.trim(username);
        // 登录失败时统一返回 FAILED_LOGIN，避免暴露到底是用户名错还是密码错。
        if (!validUsername(username) || StringUtil.isEmpty(password)) {
            log.warn(ResultCode.FAILED_PARAMS_VALIDATE.toString());
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_LOGIN));
        }
        // 先按用户名查用户，再根据密文格式选择校验方式。
        User user = selectByUserName(username);
        if (user == null) {
            log.warn(ResultCode.FAILED_LOGIN.toString() + ", username = " + username);
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_LOGIN));
        }
        boolean passwordOk;
        if (PasswordUtil.isBCrypt(user.getPassword())) {
            // 新密码：BCrypt 直接校验。
            passwordOk = PasswordUtil.matches(password, user.getPassword());
        } else {
            // 存量密码：MD5+盐 校验，通过后自动升级为 BCrypt。
            passwordOk = PasswordUtil.matchesLegacyMD5(password, user.getSalt(), user.getPassword());
            if (passwordOk) {
                User updateUser = new User();
                updateUser.setId(user.getId());
                updateUser.setPassword(PasswordUtil.encode(password));
                updateUser.setSalt("");
                updateUser.setUpdateTime(new Date());
                userMapper.updateByPrimaryKeySelective(updateUser);
                log.info("存量密码已自动升级为BCrypt, userId = " + user.getId());
            }
        }
        if (!passwordOk) {
            log.warn(ResultCode.FAILED_LOGIN.toString() + ", 密码错误, username = " + username);
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_LOGIN));
        }
        log.info("登录成功, username = " + username);
        return user;
    }

    @Override
    public User selectById(Long id) {
        // 所有按 ID 查询都要求传入正向 ID。
        if (id == null) {
            log.warn(ResultCode.FAILED_PARAMS_VALIDATE.toString());
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_LOGIN));
        }
        User user = userMapper.selectByPrimaryKey(id);
        return user;
    }

    @Override
    public void addOneArticleCountById(Long id) {
        // 发帖数更新依赖用户 ID，必须先校验。
        if (id == null || id <= 0) {
            log.warn(ResultCode.FAILED_BOARD_ARTICLE_COUNT.toString());
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_BOARD_ARTICLE_COUNT));
        }
        User user = userMapper.selectByPrimaryKey(id);
        if (user == null) {
            log.warn(ResultCode.ERROR_IS_NULL.toString() + ", user id = " + id);
            throw new ApplicationException(AppResult.failed(ResultCode.ERROR_IS_NULL));
        }
        // 原子加 1：直接在数据库当前值上累加，避免先查后改的并发丢更新。
        int row = userMapper.increaseArticleCount(id);
        if (row != 1) {
            log.warn(ResultCode.FAILED.toString() + ", 受影响的行数不等于 1 .");
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED));
        }
    }

    @Override
    public void subOneArticleCountById(Long id) {
        // 删除帖子时减少用户发帖数。
        if (id == null || id <= 0) {
            log.warn(ResultCode.FAILED_BOARD_ARTICLE_COUNT.toString());
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_BOARD_ARTICLE_COUNT));
        }
        User user = userMapper.selectByPrimaryKey(id);
        if (user == null) {
            log.warn(ResultCode.ERROR_IS_NULL.toString() + ", user id = " + id);
            throw new ApplicationException(AppResult.failed(ResultCode.ERROR_IS_NULL));
        }
        // 原子减 1：greatest 保证计数不会变成负数。
        int row = userMapper.decreaseArticleCount(id);
        if (row != 1) {
            log.warn(ResultCode.FAILED.toString() + ", 受影响的行数不等于 1 .");
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED));
        }
    }

    @Override
    public void modifyInfo(User user) {
        // 修改资料必须知道要修改哪个用户。
        if (user == null || user.getId() == null || user.getId() <= 0) {
            log.warn(ResultCode.FAILED_PARAMS_VALIDATE.toString());
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE));
        }
        User existsUser = userMapper.selectByPrimaryKey(user.getId());
        if (existsUser == null) {
            log.warn(ResultCode.FAILED_USER_NOT_EXISTS.toString());
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_USER_NOT_EXISTS));
        }
        // 记录是否真的有字段需要更新。
        boolean checkAttr = false;
        User updateUser = new User();
        updateUser.setId(user.getId());
        // 统一清理传入字段，后续每个字段再分别做格式校验。
        user.setUsername(StringUtil.trim(user.getUsername()));
        user.setNickname(StringUtil.trim(user.getNickname()));
        user.setEmail(StringUtil.trim(user.getEmail()));
        user.setPhoneNum(StringUtil.trim(user.getPhoneNum()));
        user.setCollege(StringUtil.trim(user.getCollege()));
        user.setClassName(StringUtil.trim(user.getClassName()));
        user.setRemark(StringUtil.trim(user.getRemark()));
        // 用户名非空且发生变化时才检查格式和唯一性。
        if (!StringUtil.isEmpty(user.getUsername())
                && !user.getUsername().equals(existsUser.getUsername())) {
            if (!validUsername(user.getUsername())) {
                throw new ApplicationException(AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE));
            }
            // 新用户名不能和其他用户重复。
            User checkUser = userMapper.selectByUserName(user.getUsername());
            if (checkUser != null) {
                log.warn(ResultCode.FAILED_USER_EXISTS.toString());
                throw new ApplicationException(AppResult.failed(ResultCode.FAILED_USER_EXISTS));
            }
            updateUser.setUsername(user.getUsername());
            checkAttr = true;
        }
        // 昵称非空且发生变化时才写入更新对象。
        if (!StringUtil.isEmpty(user.getNickname())
                && !user.getNickname().equals(existsUser.getNickname())) {
            if (!StringUtil.lengthBetween(user.getNickname(), 2, 50)) {
                throw new ApplicationException(AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE));
            }
            updateUser.setNickname(user.getNickname());
            checkAttr = true;
        }
        // 性别只能是 0、1、2，越界时统一设为保密。
        if (user.getGender() != null && user.getGender() != existsUser.getGender()) {
            updateUser.setGender(user.getGender());
            if (updateUser.getGender() > 2 || updateUser.getGender() < 0) {
                updateUser.setGender((byte) 2);
            }
            checkAttr = true;
        }
        // 邮箱变化时做长度和格式校验。
        if (!StringUtil.isEmpty(user.getEmail())
                && !user.getEmail().equals(existsUser.getEmail())) {
            if (user.getEmail().length() > 50 || !EMAIL_PATTERN.matcher(user.getEmail()).matches()) {
                throw new ApplicationException(AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE));
            }
            updateUser.setEmail(user.getEmail());
            checkAttr = true;
        }
        // 手机号变化时做格式校验。
        if (!StringUtil.isEmpty(user.getPhoneNum())
                && !user.getPhoneNum().equals(existsUser.getPhoneNum())) {
            if (!PHONE_PATTERN.matcher(user.getPhoneNum()).matches()) {
                throw new ApplicationException(AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE));
            }
            updateUser.setPhoneNum(user.getPhoneNum());
            checkAttr = true;
        }
        // 学院变化时做长度校验（学生转学院等场景可修改）。
        if (!StringUtil.isEmpty(user.getCollege())
                && !user.getCollege().equals(existsUser.getCollege())) {
            if (!StringUtil.lengthBetween(user.getCollege(), 2, 50)) {
                throw new ApplicationException(AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE));
            }
            updateUser.setCollege(user.getCollege());
            checkAttr = true;
        }
        // 班级变化时做长度校验（升级、转班等场景可修改）。
        if (!StringUtil.isEmpty(user.getClassName())
                && !user.getClassName().equals(existsUser.getClassName())) {
            if (!StringUtil.lengthBetween(user.getClassName(), 1, 50)) {
                throw new ApplicationException(AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE));
            }
            updateUser.setClassName(user.getClassName());
            checkAttr = true;
        }
        // 个人简介变化时限制最大长度。
        if (!StringUtil.isEmpty(user.getRemark())
                && !user.getRemark().equals(existsUser.getRemark())) {
            if (user.getRemark().length() > 1000) {
                throw new ApplicationException(AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE));
            }
            updateUser.setRemark(user.getRemark());
            checkAttr = true;
        }
        // 没有任何字段变化时，不执行数据库更新，按幂等成功处理。
        // 用户打开资料页未做修改直接保存是常见操作，不应返回技术性错误提示。
        if (checkAttr == false) {
            log.info("用户资料未发生变化，跳过更新. userId = " + user.getId());
            return;
        }
        int row = userMapper.updateByPrimaryKeySelective(updateUser);
        if (row != 1) {
            log.warn(ResultCode.FAILED.toString() + ", 受影响的行数不等于 1 .");
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED));
        }
    }

    @Override
    public void modifyPassword(Long id, String newPassword, String oldPassword) {
        // 修改密码需要用户 ID、旧密码和新密码都存在。
        if (id == null || id <= 0 || StringUtil.isEmpty(newPassword) || StringUtil.isEmpty(oldPassword)) {
            log.warn(ResultCode.FAILED_PARAMS_VALIDATE.toString());
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE));
        }
        User user = userMapper.selectByPrimaryKey(id);
        if (user == null || user.getDeleteState() == 1) {
            log.warn(ResultCode.FAILED_USER_NOT_EXISTS.toString());
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_USER_NOT_EXISTS));
        }
        // 旧密码按密文格式分流校验：BCrypt 直接比对，存量 MD5+盐 走兼容逻辑。
        boolean oldPasswordOk;
        if (PasswordUtil.isBCrypt(user.getPassword())) {
            oldPasswordOk = PasswordUtil.matches(oldPassword, user.getPassword());
        } else {
            oldPasswordOk = PasswordUtil.matchesLegacyMD5(oldPassword, user.getSalt(), user.getPassword());
        }
        if (!oldPasswordOk) {
            log.warn(ResultCode.FAILED_PARAMS_VALIDATE.toString());
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE));
        }
        // 新密码统一用 BCrypt 存储，salt 写空串占位（列约束 NOT NULL）。
        User updateUser = new User();
        updateUser.setId(user.getId());
        updateUser.setSalt("");
        updateUser.setPassword(PasswordUtil.encode(newPassword));
        Date date = new Date();
        updateUser.setUpdateTime(date);
        int row = userMapper.updateByPrimaryKeySelective(updateUser);
        if (row != 1) {
            log.warn(ResultCode.FAILED.toString() + ", 受影响的行数不等于 1 .");
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED));
        }
    }

    @Override
    public void modifyAvatar(Long id, String avatarUrl) {
        // 头像更新必须指定用户 ID 和非空路径。
        if (id == null || id <= 0 || StringUtil.isEmpty(avatarUrl)) {
            log.warn(ResultCode.FAILED_PARAMS_VALIDATE.toString());
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE));
        }
        User updateUser = new User();
        updateUser.setId(id);
        updateUser.setAvatarUrl(avatarUrl);
        updateUser.setUpdateTime(new Date());
        int row = userMapper.updateByPrimaryKeySelective(updateUser);
        if (row != 1) {
            log.warn(ResultCode.FAILED.toString() + ", 受影响的行数不等于 1 .");
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED));
        }
    }

    @Override
    public void setSecurityQuestion(Long id, String question, String answer) {
        // 设置密保必须指定用户，且问题与答案都不能为空。
        if (id == null || id <= 0
                || StringUtil.isEmpty(question) || StringUtil.isEmpty(answer)) {
            log.warn(ResultCode.FAILED_PARAMS_VALIDATE.toString());
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE));
        }
        question = StringUtil.trim(question);
        answer = StringUtil.trim(answer);
        if (question.length() > 100 || answer.length() > 100) {
            throw new ApplicationException(AppResult.failed("密保问题与答案均不能超过 100 字"));
        }
        User user = userMapper.selectByPrimaryKey(id);
        if (user == null || user.getDeleteState() == 1) {
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_USER_NOT_EXISTS));
        }
        User updateUser = new User();
        updateUser.setId(id);
        updateUser.setSecurityQuestion(question);
        // 答案同样用 BCrypt 存储，避免明文泄露。
        updateUser.setSecurityAnswer(PasswordUtil.encode(answer));
        updateUser.setUpdateTime(new Date());
        int row = userMapper.updateByPrimaryKeySelective(updateUser);
        if (row != 1) {
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED));
        }
    }

    @Override
    public void resetPasswordBySecurity(String username, String securityAnswer, String newPassword) {
        username = StringUtil.trim(username);
        securityAnswer = StringUtil.trim(securityAnswer);
        if (!validUsername(username) || StringUtil.isEmpty(securityAnswer)
                || !StringUtil.lengthBetween(newPassword, Constants.PASSWORD_MIN_LENGTH, Constants.PASSWORD_MAX_LENGTH)) {
            log.warn(ResultCode.FAILED_PARAMS_VALIDATE.toString());
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE));
        }

        User user = userMapper.selectByUserName(username);
        if (user == null || user.getDeleteState() == 1) {
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_USER_NOT_EXISTS));
        }
        // 未设置密保的账号不能走这条路，避免绕过身份校验重置他人密码。
        if (StringUtil.isEmpty(user.getSecurityQuestion()) || StringUtil.isEmpty(user.getSecurityAnswer())) {
            throw new ApplicationException(AppResult.failed("该账号未设置密保问题，请联系管理员处理"));
        }

        boolean answerOk;
        if (PasswordUtil.isBCrypt(user.getSecurityAnswer())) {
            answerOk = PasswordUtil.matches(securityAnswer, user.getSecurityAnswer());
        } else {
            // 兼容早期直接存明文答案的情况（忽略大小写）。
            answerOk = user.getSecurityAnswer().equalsIgnoreCase(securityAnswer);
        }
        if (!answerOk) {
            log.warn("密保答案错误, username = " + username);
            throw new ApplicationException(AppResult.failed("密保答案不正确"));
        }

        User updateUser = new User();
        updateUser.setId(user.getId());
        updateUser.setPassword(PasswordUtil.encode(newPassword));
        updateUser.setSalt("");
        updateUser.setUpdateTime(new Date());
        int row = userMapper.updateByPrimaryKeySelective(updateUser);
        if (row != 1) {
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED));
        }
        log.info("通过密保重置密码成功, userId = " + user.getId());
    }

    private boolean validUsername(String username) {
        // 正则校验用户名格式，返回 true 表示用户名合法。
        return username != null && USERNAME_PATTERN.matcher(username).matches();
    }

    private boolean validStudentNo(String studentNo) {
        // 正则校验学号格式：纯数字，长度 6 到 20。
        return studentNo != null && STUDENT_NO_PATTERN.matcher(studentNo).matches();
    }

    @Override
    public User maskForPublic(User user) {
        if (user == null) {
            return null;
        }
        User copy = new User();
        copy.setId(user.getId());
        copy.setUsername(user.getUsername());
        copy.setNickname(user.getNickname());
        copy.setGender(user.getGender());
        copy.setAvatarUrl(user.getAvatarUrl());
        copy.setArticleCount(user.getArticleCount());
        copy.setIsAdmin(user.getIsAdmin());
        copy.setRemark(user.getRemark());
        copy.setState(user.getState());
        copy.setCreateTime(user.getCreateTime());
        copy.setCollege(user.getCollege());
        copy.setClassName(user.getClassName());

        // 学号脱敏：如 2550710704 -> 2550****04
        if (user.getStudentNo() != null && user.getStudentNo().length() >= 6) {
            String sno = user.getStudentNo();
            copy.setStudentNo(sno.substring(0, 4) + "****" + sno.substring(sno.length() - 2));
        } else if (user.getStudentNo() != null) {
            copy.setStudentNo("****");
        }

        // 真实姓名脱敏：张三 -> 张*；诸葛孔明 -> 诸**明
        if (user.getRealName() != null && !user.getRealName().isEmpty()) {
            String rname = user.getRealName();
            if (rname.length() == 1) {
                copy.setRealName(rname);
            } else if (rname.length() == 2) {
                copy.setRealName(rname.substring(0, 1) + "*");
            } else {
                copy.setRealName(rname.substring(0, 1) + "*" + rname.substring(rname.length() - 1));
            }
        }

        // 敏感信息对他人完全清空置空
        copy.setPhoneNum(null);
        copy.setEmail(null);
        copy.setSecurityQuestion(null);
        copy.setSecurityAnswer(null);
        copy.setPassword(null);
        copy.setSalt(null);
        copy.setDeleteState(user.getDeleteState());
        return copy;
    }
}
