package com.cainiao.forum.services.impl;

import com.cainiao.forum.model.User;
import com.cainiao.forum.services.IUserService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
// 用户业务集成测试，验证注册、登录、资料修改与密码修改。
// 每个用例自建临时账号，不依赖数据库中已有数据；事务在测试结束后回滚。
class UserServiceImplTest {

    @Resource
    private IUserService userService;

    private static final String RAW_PASSWORD = "123456";

    /**
     * 创建临时用户并返回数据库中的完整记录。
     * 用户名与学号带纳秒后缀保证唯一，避免与其他用例或历史数据冲突。
     */
    private User tempUser(String tag) {
        String suffix = String.valueOf(System.nanoTime());
        suffix = suffix.substring(Math.max(0, suffix.length() - 10));

        User user = new User();
        user.setUsername("u" + suffix + tag);
        user.setNickname("测试" + tag);
        user.setStudentNo("8" + suffix);
        user.setRealName("测试用户" + tag);
        user.setCollege("测试学院");
        user.setClassName("测试班级");
        user.setPassword(RAW_PASSWORD);

        userService.createNormalUser(user);

        User saved = userService.selectByUserName(user.getUsername());
        assertNotNull(saved, "创建后应能按用户名查到用户");
        return saved;
    }

    @Test
    @Transactional
    // 测试创建普通用户：应写入数据库并完成密码加密。
    void createNormalUser() {
        User user = tempUser("a");

        assertNotNull(user.getId(), "创建后应回填主键");
        assertNotEquals(RAW_PASSWORD, user.getPassword(), "数据库中不应保存明文密码");
        assertEquals((byte) 0, user.getIsAdmin(), "新注册用户不应是管理员");
        assertEquals((byte) 0, user.getDeleteState(), "新注册用户不应是删除状态");
    }

    @Test
    @Transactional
    // 测试用户名重复时拒绝注册。
    void createDuplicateUserName() {
        User first = tempUser("b");

        User duplicate = new User();
        duplicate.setUsername(first.getUsername());
        duplicate.setNickname("重复用户");
        duplicate.setStudentNo("7" + System.nanoTime() % 1000000000L);
        duplicate.setRealName("重复用户");
        duplicate.setCollege("测试学院");
        duplicate.setClassName("测试班级");
        duplicate.setPassword(RAW_PASSWORD);

        assertThrows(RuntimeException.class, () -> userService.createNormalUser(duplicate),
                "用户名重复时应拒绝注册");
    }

    @Test
    @Transactional
    // 测试按用户名查询用户。
    void selectByUserName() {
        User created = tempUser("c");

        User found = userService.selectByUserName(created.getUsername());

        assertNotNull(found, "应能按用户名查到用户");
        assertEquals(created.getId(), found.getId());
    }

    @Test
    @Transactional
    // 测试用户名密码登录逻辑：密码正确应返回用户，密码错误应抛异常。
    void login() {
        User created = tempUser("d");

        User logged = userService.login(created.getUsername(), RAW_PASSWORD);
        assertNotNull(logged, "密码正确时应登录成功");
        assertEquals(created.getId(), logged.getId());

        assertThrows(RuntimeException.class,
                () -> userService.login(created.getUsername(), "wrong-password"),
                "密码错误时应拒绝登录");
    }

    @Test
    @Transactional
    // 测试按 ID 查询用户。
    void selectById() {
        User created = tempUser("e");

        User found = userService.selectById(created.getId());

        assertNotNull(found, "应能按 ID 查到用户");
        assertEquals(created.getUsername(), found.getUsername());
    }

    @Test
    @Transactional
    // 测试用户发帖数加 1。
    void addOneArticleCountById() {
        User created = tempUser("f");
        int before = created.getArticleCount();

        userService.addOneArticleCountById(created.getId());

        User after = userService.selectById(created.getId());
        assertEquals(before + 1, after.getArticleCount(), "发帖数应加 1");
    }

    @Test
    @Transactional
    // 测试用户发帖数减 1。
    void subOneArticleCountById() {
        User created = tempUser("g");
        userService.addOneArticleCountById(created.getId());
        int before = userService.selectById(created.getId()).getArticleCount();

        userService.subOneArticleCountById(created.getId());

        User after = userService.selectById(created.getId());
        assertEquals(before - 1, after.getArticleCount(), "发帖数应减 1");
    }

    @Test
    @Transactional
    // 测试修改用户资料中的多个字段。
    void modifyInfo() {
        User created = tempUser("h");

        User patch = new User();
        patch.setId(created.getId());
        patch.setNickname("改后的昵称");
        patch.setEmail("student@campus.edu.cn");
        patch.setPhoneNum("15366668888");
        patch.setRemark("测试资料修改");

        userService.modifyInfo(patch);

        User after = userService.selectById(created.getId());
        assertEquals("改后的昵称", after.getNickname(), "昵称应更新");
        assertEquals("student@campus.edu.cn", after.getEmail(), "邮箱应更新");
        assertEquals("15366668888", after.getPhoneNum(), "手机号应更新");
    }

    @Test
    @Transactional
    // 测试修改密码：旧密码正确时可改，改后新密码生效、旧密码失效。
    void modifyPassword() {
        User created = tempUser("i");
        String newPassword = "newPass2026";

        userService.modifyPassword(created.getId(), newPassword, RAW_PASSWORD);

        assertNotNull(userService.login(created.getUsername(), newPassword),
                "新密码应能登录");
        assertThrows(RuntimeException.class,
                () -> userService.login(created.getUsername(), RAW_PASSWORD),
                "旧密码应失效");
    }

    @Test
    @Transactional
    // 测试旧密码错误时拒绝修改密码。
    void modifyPasswordWithWrongOld() {
        User created = tempUser("j");

        assertThrows(RuntimeException.class,
                () -> userService.modifyPassword(created.getId(), "anotherPass2026", "wrong-old"),
                "旧密码不正确时应拒绝修改");
    }

    @Test
    // 测试公开资料脱敏：验证手机号、邮箱、密保均被清空，学号姓名脱敏。
    void testMaskForPublic() {
        User user = new User();
        user.setId(100L);
        user.setUsername("student01");
        user.setRealName("张小凡");
        user.setStudentNo("2550710704");
        user.setPhoneNum("13812345678");
        user.setEmail("test@campus.edu.cn");
        user.setSecurityQuestion("我的小学是？");
        user.setSecurityAnswer("光明小学");

        User masked = userService.maskForPublic(user);
        assertNull(masked.getPhoneNum(), "手机号必须清空");
        assertNull(masked.getEmail(), "邮箱必须清空");
        assertNull(masked.getSecurityQuestion(), "密保问题必须清空");
        assertEquals("2550****04", masked.getStudentNo(), "学号脱敏格式必须为 2550****04");
        assertEquals("张*凡", masked.getRealName(), "姓名脱敏格式必须为 张*凡");
    }
}
