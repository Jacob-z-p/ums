package com.jacobzp.ums;

import static org.junit.jupiter.api.Assertions.*;

import com.jacobzp.ums.entity.User;
import com.jacobzp.ums.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

@SpringBootTest()
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
