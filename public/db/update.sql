-- >>>>>>>【数据库结构更新日志】<<<<<<<
-- 从老版本升级时，根据旧版本号，选择更新脚本，手动执行。
-- 注意：需要按时间顺序执行，大于旧版本号的脚本都要执行

-- >>>>>>>【更新日志】<<<<<<<
INSERT INTO `sys_update_log` (`create_time`, `update_time`, `version`, `note`)
VALUES (now(), now(), '2025022301', '本地文件上传配置变更');

-- 新增本地私有文件上传路径配置
INSERT INTO `sys_config` (`config_name`, `config_key`, `config_value`, `config_type`)
VALUES ('本地存储-上传文章文件目录', 'local.articleFilePath', '/app/blog/private/res/article/', '1');

-- >>>>>>>【更新日志】<<<<<<<
INSERT INTO `sys_update_log` (`create_time`, `update_time`, `version`, `note`)
VALUES (now(), now(), '2025031201', '用户表名称变更');

ALTER TABLE `user` RENAME TO `sys_user`;
