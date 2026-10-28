package com.cainiao.forum;

import com.cainiao.forum.dao.UserMapper;
import com.cainiao.forum.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

@SpringBootTest
// Spring Boot 集成测试，用来验证应用上下文、数据库连接和 MyBatis 是否能正常工作。
class ForumApplicationTests {


	@Resource
	// 数据源对象，由 Spring 根据 application.yml 中的数据库配置创建。
	private DataSource dataSource;


	@Resource
	// 用户 Mapper，用来验证 MyBatis SQL 映射能否正常查询。
	private UserMapper userMapper;

	@Test
	// 测试数据库连接能否创建并关闭。
	void testConnection() throws SQLException {
		System.out.println("dataSource = " + dataSource.getClass());

		Connection connection = dataSource.getConnection();
		System.out.println("connection = " + connection);
		connection.close();
	}

	@Test
	// 测试 MyBatis 能否按主键查询用户。
	void testMybatis () {
		User user = userMapper.selectByPrimaryKey(1l);
		System.out.println(user);
		System.out.println(user.getUsername());
	}

	@Test
	// 测试 Java 自带 UUID 的生成结果。
	void testUUID () {
		System.out.println(UUID.randomUUID().toString());
	}
}

