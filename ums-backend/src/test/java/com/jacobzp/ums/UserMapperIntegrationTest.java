package com.jacobzp.ums;

import static org.junit.jupiter.api.Assertions.*;

import com.jacobzp.ums.entity.User;
import com.jacobzp.ums.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:mappertest;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password="
})
@Sql("/user-test-schema.sql")
class UserMapperIntegrationTest {
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void insertsSelectsUpdatesAndLogicallyDeletesUser() {
        User user = new User();
        user.setAccount("test-user");
        user.setPasswordHash("test-password-hash");
        user.setNickname("Before");
        assertEquals(1, userMapper.insert(user));
        assertNotNull(user.getId());
    }
}
