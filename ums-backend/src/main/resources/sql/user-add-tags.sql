-- 已有 ums.user 且尚无 tags 字段时，手动执行一次。新建库使用 user.sql。
ALTER TABLE `ums`.`user`
    ADD COLUMN `tags` JSON NOT NULL DEFAULT (JSON_ARRAY()) COMMENT '用户选中的 PMS 标签 ID 数组',
    ADD CONSTRAINT `chk_user_tags` CHECK (JSON_SCHEMA_VALID(
        '{"type":"array","items":{"type":"integer","minimum":1},"uniqueItems":true}',
        `tags`
    ));
