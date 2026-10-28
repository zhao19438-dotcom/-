package com.cainiao.forum.services.impl;

import com.cainiao.forum.model.Message;
import com.cainiao.forum.model.User;
import com.cainiao.forum.services.IMessageService;
import com.cainiao.forum.services.IUserService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
// 站内信业务集成测试，验证发送、查询、状态更新和回复。
// 测试自建临时用户，不依赖数据库中已有账号，避免数据调整导致测试失效。
class MessageServiceImplTest {

    @Resource
    private IMessageService messageService;

    @Resource
    private IUserService userService;

    /**
     * 创建一个临时用户，用于隔离私信会话。
     * 用户名与学号带纳秒后缀保证唯一；测试方法标注 @Transactional，结束后自动回滚。
     */
    private User tempUser(String tag) {
        String suffix = String.valueOf(System.nanoTime());
        suffix = suffix.substring(Math.max(0, suffix.length() - 10));

        User user = new User();
        user.setUsername("t" + suffix + tag);
        user.setNickname("测试" + tag);
        user.setStudentNo("9" + suffix);
        user.setRealName("测试用户" + tag);
        user.setCollege("测试学院");
        user.setClassName("测试班级");
        user.setPassword("123456");

        userService.createNormalUser(user);
        return userService.selectByUserName(user.getUsername());
    }

    @Test
    @Transactional
    // 测试发送站内信。
    void create() {
        User sender = tempUser("a");
        User receiver = tempUser("b");

        Message message = new Message();
        message.setPostUserId(sender.getId());
        message.setReceiveUserId(receiver.getId());
        message.setContent("单元测试私信内容");

        messageService.create(message);

        assertNotNull(message.getId(), "发送后应回填主键");
        Message saved = messageService.selectById(message.getId());
        assertNotNull(saved, "应能按主键查到刚发送的消息");
        assertEquals("单元测试私信内容", saved.getContent());
        assertEquals(sender.getId(), saved.getPostUserId());
    }

    @Test
    @Transactional
    // 同一发送者在对方回复前不能连续发第二条。
    void createTwiceBeforeReply() {
        User sender = tempUser("c");
        User receiver = tempUser("d");

        Message first = new Message();
        first.setPostUserId(sender.getId());
        first.setReceiveUserId(receiver.getId());
        first.setContent("第一条私信");
        messageService.create(first);

        Message second = new Message();
        second.setPostUserId(sender.getId());
        second.setReceiveUserId(receiver.getId());
        second.setContent("第二条私信");

        assertThrows(RuntimeException.class, () -> messageService.create(second),
                "对方未回复时应拒绝继续发送");
    }

    @Test
    @Transactional
    // 测试查询收件箱列表。
    void selectByReceiveUserId() {
        User sender = tempUser("e");
        User receiver = tempUser("f");

        Message message = new Message();
        message.setPostUserId(sender.getId());
        message.setReceiveUserId(receiver.getId());
        message.setContent("查询收件箱用消息");
        messageService.create(message);

        List<Message> inbox = messageService.selectByReceiveUserId(receiver.getId());

        assertNotNull(inbox, "收件箱不应为 null");
        assertFalse(inbox.isEmpty(), "收件箱应至少包含一条消息");
        assertTrue(inbox.stream().anyMatch(m -> "查询收件箱用消息".equals(m.getContent())),
                "收件箱应包含刚发送的消息");
    }

    @Test
    @Transactional
    // 测试把消息状态更新为已读。
    void updateStateById() {
        User sender = tempUser("g");
        User receiver = tempUser("h");

        Message message = new Message();
        message.setPostUserId(sender.getId());
        message.setReceiveUserId(receiver.getId());
        message.setContent("标记已读用消息");
        messageService.create(message);

        Integer unreadBefore = messageService.selectUnreadCount(receiver.getId());
        assertTrue(unreadBefore != null && unreadBefore >= 1, "发送后接收方应至少有 1 条未读");

        messageService.updateStateById(message.getId(), (byte) 1);

        Message updated = messageService.selectById(message.getId());
        assertEquals((byte) 1, updated.getState(), "状态应更新为已读");
    }

    @Test
    @Transactional
    // 测试按 ID 查询单条站内信。
    void selectById() {
        User sender = tempUser("i");
        User receiver = tempUser("j");

        Message message = new Message();
        message.setPostUserId(sender.getId());
        message.setReceiveUserId(receiver.getId());
        message.setContent("按主键查询用消息");
        messageService.create(message);

        Message found = messageService.selectById(message.getId());

        assertNotNull(found, "按主键应能查到消息");
        assertEquals("按主键查询用消息", found.getContent());
    }

    @Test
    @Transactional
    // 测试回复站内信，原消息会被标记为已回复。
    void reply() {
        User sender = tempUser("k");
        User receiver = tempUser("l");

        Message initial = new Message();
        initial.setPostUserId(sender.getId());
        initial.setReceiveUserId(receiver.getId());
        initial.setContent("测试私信咨询");
        messageService.create(initial);

        Message reply = new Message();
        reply.setPostUserId(receiver.getId());
        reply.setReceiveUserId(sender.getId());
        reply.setContent("单元测试回复：收到");

        messageService.reply(initial.getId(), reply);

        Message replied = messageService.selectById(initial.getId());
        assertEquals((byte) 2, replied.getState(), "原消息状态应标记为已回复（0 未读 / 1 已读 / 2 已回复）");

        List<Message> conversation = messageService.selectConversation(sender.getId(), receiver.getId());
        assertNotNull(conversation, "会话列表不应为 null");
        assertTrue(conversation.size() >= 2, "会话中应包含原消息与回复");
    }
}
