-- 在目标数据库中手动执行；本脚本不会自动运行。
CREATE TABLE `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户主键',
    `account` VARCHAR(64) NOT NULL COMMENT '登录账号',
    `password_hash` VARCHAR(255) NOT NULL COMMENT '密码哈希，不存储明文密码',
    `nickname` VARCHAR(64) NOT NULL COMMENT '用户昵称',
    `avatar_url` VARCHAR(512) DEFAULT NULL COMMENT '头像地址',
    `role` TINYINT NOT NULL DEFAULT 0 COMMENT '角色：0-普通用户，1-管理员',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-正常，1-禁用',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    `tags` JSON NOT NULL DEFAULT (JSON_ARRAY()) COMMENT '用户选中的 PMS 标签 ID 数组',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_account` (`account`),
    CONSTRAINT `chk_user_role` CHECK (`role` IN (0, 1)),
    CONSTRAINT `chk_user_status` CHECK (`status` IN (0, 1)),
    CONSTRAINT `chk_user_is_deleted` CHECK (`is_deleted` IN (0, 1)),
    CONSTRAINT `chk_user_tags` CHECK (JSON_SCHEMA_VALID(
        '{"type":"array","items":{"type":"integer","minimum":1},"uniqueItems":true}',
        `tags`
    ))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';
