-- 【基础数据初始化脚本】

-- 初始化安装记录，版本号为update.sql的最后一次更新版本号
INSERT INTO `sys_update_log` (`create_time`, `update_time`, `version`, `note`)
VALUES (now(), now(), '2025031201', '系统初始化安装完成');

-- 默认管理员信息，账号admin，密码admin，todo:强化密码加密规则，现在为md5('明文密码')
INSERT INTO `sys_user`(`id`, `username`, `password`, `phone_number`, `email`, `user_status`, `gender`, `open_id`, `admire`,
                   `subscribe`, `avatar`, `introduction`, `user_type`, `update_by`, `deleted`)
VALUES (1, 'admin', '21232f297a57a5a743894a0e4a801fc3', '', '', 1, 1, '', '', '', '', '', 0, 'admin', 0);

-- 默认网站配置
INSERT INTO `web_info`(`id`, `web_name`, `web_title`, `notices`, `footer`, `background_image`, `avatar`,
                       `random_avatar`, `random_name`, `random_cover`, `waifu_json`, `status`)
VALUES (1, '诗与远方', 'POETIZE-NEXT', '[]', '春风轻抚栏边柳，露点晶莹华彩延。', '', '', '[]', '[]', '[]', '{}', 1);

-- 默认家庭配置
INSERT INTO `family` (`id`, `user_id`, `bg_cover`, `man_cover`, `woman_cover`, `man_name`, `woman_name`, `timing`,
                      `countdown_title`, `countdown_time`, `status`, `family_info`, `like_count`, `create_time`,
                      `update_time`)
VALUES (1, 1, '/res/sys/love.jpg', '/res/sys/avatar1.jpg', '/res/sys/avatar0.jpg', '司马相如', '卓文君',
        '1970-01-01 00:00:00', '春节倒计时',
        '2026-02-17 00:00:00', 1, '', 0, now(), now());

-- 默认聊天室配置
INSERT INTO `im_chat_group` (`id`, `group_name`, `master_user_id`, `introduction`, `notice`, `in_type`)
VALUES (-1, '公共聊天室', 1, '公共聊天室', '欢迎光临！', 0);

insert into `im_chat_group_user` (`id`, `group_id`, `user_id`, `admin_flag`, `user_status`)
values (1, -1, 1, 1, 1);

-- 默认系统配置
INSERT INTO `sys_config` (`config_name`, `config_key`, `config_value`, `config_type`)
VALUES ('邮箱-发件号', 'spring.mail.username', '', '1'),
       ('邮箱-授权码', 'spring.mail.password', '', '1'),
       ('邮箱-验证码模板', 'user.code.format',
        '【POETIZE-NEXT】%s为本次验证的验证码，请在5分钟内完成验证。为保证账号安全，请勿泄漏此验证码。', '1'),
       ('邮箱-订阅模板', 'user.subscribe.format', '【POETIZE-NEXT】您订阅的专栏【%s】新增一篇文章：%s。', '1'),
       ('默认存储平台', 'store.type', 'local', '2'),
       ('本地存储-启用状态', 'local.enable', 'true', '2'),
       ('本地存储-上传文件根目录', 'local.uploadUrl', '/app/blog/public/res/', '1'),
       ('本地存储-上传文章文件目录', 'local.articleFilePath', '/app/blog/private/res/article/', '1'),
       ('本地存储-下载前缀', 'local.downloadUrl', '/res/', '2'),
       ('静态资源-访问前缀', 'webStaticResourcePrefix', '/res/', '2'),
       ('七牛云-启用状态', 'qiniu.enable', 'false', '2'),
       ('七牛云-accessKey', 'qiniu.accessKey', '', '1'),
       ('七牛云-secretKey', 'qiniu.secretKey', '', '1'),
       ('七牛云-bucket', 'qiniu.bucket', '', '1'),
       ('七牛云-下载域名', 'qiniu.downloadUrl', 'https://file.my.com/', '2'),
       ('七牛云-上传地址', 'qiniuUrl', 'https://upload.qiniup.com/', '2'),
       ('IM-聊天室启用状态', 'im.enable', 'true', '1'),
       ('备案号', 'beian', '', '2');

-- 默认友链
INSERT INTO `resource_path` (`title`, `classify`, `cover`, `url`, `introduction`, `type`, `status`, `remark`,
                             `create_time`)
VALUES ('POETIZE', '🥇友情链接', 'https://s1.ax1x.com/2022/11/10/z9VlHs.png', 'https://gitee.com/littledokey/poetize',
        '遇见最美博客，诗意~', 'friendUrl', 1, '', now()),
       ('POETIZE-NEXT', '🥇友情链接', '/res/sys/poetize-next.png', 'https://gitee.com/siaor/poetize-next',
        '遇见最美博客，下一站，出发！诗与远方~', 'friendUrl', 1, '', now());
