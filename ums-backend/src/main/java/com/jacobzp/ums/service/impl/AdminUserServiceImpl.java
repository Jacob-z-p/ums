package com.jacobzp.ums.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jacobzp.ums.model.vo.AdminUserPageVO;
import com.jacobzp.ums.model.query.AdminUserQuery;
import com.jacobzp.ums.model.vo.AdminUserVO;
import com.jacobzp.ums.entity.User;
import com.jacobzp.ums.mapper.UserMapper;
import com.jacobzp.ums.service.AdminUserService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 描述：用户管理服务层实现类
 * </p>
 * @author Jacobzp
 * @version 1.0.0
 */
@Service
public class AdminUserServiceImpl implements AdminUserService {
    private static final int USER_ROLE = 0;
    private static final int ADMIN_ROLE = 1;
    private static final int ACTIVE_STATUS = 0;
    private final UserMapper userMapper;

    public AdminUserServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public Access checkAccess(long userId) {
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery()
                .select(User::getRole, User::getStatus)
                .eq(User::getId, userId));
        if (user == null || user.getStatus() != ACTIVE_STATUS) {
            return Access.UNAUTHENTICATED;
        }
        return user.getRole() == ADMIN_ROLE ? Access.ALLOWED : Access.FORBIDDEN;
    }

    /**
     * <p>
     * 用户查询：目前只支持对 account 进行精确查询
     * </p>
     * <p>AI目前有点过度实现，目前查询的结果其实只有1条条0条，没有必要分页查询</p>
     * @param query AdminUserQuery
     * @return AdminUserPageVO
     */
    @Override
    public AdminUserPageVO list(AdminUserQuery query) {
        var users = userMapper.selectPage(new Page<>(query.getPage(), query.getSize()), // 编译器可以从上下文中推断出泛型类型是 User，不用写 Page<User>
                Wrappers.<User>lambdaQuery()
                        .select(User::getId, User::getAccount, User::getNickname, User::getAvatarUrl,
                                User::getRole, User::getStatus, User::getCreatedAt, User::getUpdatedAt)
                        .eq(query.getAccount() != null, User::getAccount, query.getAccount())
                        .orderByDesc(User::getId));
        var items = users.getRecords().stream()
                .map(user -> new AdminUserVO(user.getId(), user.getAccount(), user.getNickname(),
                        user.getAvatarUrl(), user.getRole(), user.getStatus(), user.getCreatedAt(), user.getUpdatedAt()))
                .toList();
        return new AdminUserPageVO(items, users.getTotal(), query.getPage(), query.getSize());
    }

    /**
     * 删除用户
     *
     * @param id 用户 ID
     * @return DeleteResult 删除结果
     */
    @Override
    public DeleteResult delete(long id) {
        // 尝试删除用户 - mybatis-plus
        int deleted = userMapper.delete(Wrappers.<User>lambdaQuery()
                .eq(User::getId, id)
                .eq(User::getRole, USER_ROLE));
        // 如果已删除
        if (deleted > 0) {
            return DeleteResult.DELETED;
        }
        // 如果未删除
        boolean adminExists = userMapper.exists(Wrappers.<User>lambdaQuery()
                .eq(User::getId, id)
                .eq(User::getRole, ADMIN_ROLE));
        return adminExists ? DeleteResult.ADMIN_PROTECTED : DeleteResult.NOT_FOUND;
    }
}
