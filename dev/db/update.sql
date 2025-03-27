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

-- >>>>>>>【更新日志】<<<<<<<
INSERT INTO `sys_update_log` (`create_time`, `update_time`, `version`, `note`)
VALUES (now(), now(), '2025031501', '本地文件上传配置变更');

UPDATE `sys_config`
SET `config_key` = 'local.resPath'
WHERE `config_key` = 'local.uploadUrl';
UPDATE `sys_config`
SET `config_key` = 'local.articlePath'
WHERE `config_key` = 'local.articleFilePath';
UPDATE `sys_config`
SET `config_key` = 'local.visitUrl'
WHERE `config_key` = 'local.downloadUrl';
DELETE
FROM `sys_config`
WHERE `config_key` = 'webStaticResourcePrefix';

-- >>>>>>>【更新日志】<<<<<<<
INSERT INTO `sys_update_log` (`create_time`, `update_time`, `version`, `note`)
VALUES (now(), now(), '2025032601', '系统配置值长度扩增，增加支付配置、支付订单，支持打赏功能');

ALTER TABLE `sys_config`
    MODIFY COLUMN `config_value` varchar(2047) DEFAULT NULL COMMENT '键值' AFTER `config_key`;

INSERT INTO `sys_config` (`config_name`, `config_key`, `config_value`, `config_type`)
VALUES ('支付宝-服务调用地址', 'alipay.serverUrl', 'https://openapi.alipay.com/gateway.do', '1'),
       ('支付宝-服务公钥', 'alipay.publicKey', '', '1'),
       ('支付宝-用户ID', 'alipay.userId', '2088922043284376', '1'),
       ('支付宝-应用ID', 'alipay.appId', '', '1'),
       ('支付宝-应用私钥', 'alipay.appKey', '', '1');

CREATE TABLE IF NOT EXISTS `pay_order`
(
    `id`          bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '唯一编号',
    `create_time` datetime            NOT NULL COMMENT '创建时间',
    `update_time` datetime            NOT NULL COMMENT '最后更新时间',
    `num`         varchar(63)         NOT NULL DEFAULT '' COMMENT '系统支付订单号',
    `status`      tinyint(1)          NOT NULL DEFAULT 0 COMMENT '状态：0.未支付1.已支付2.取消支付3.支付超时4.支付失败',
    `user_id`     int                 NOT NULL DEFAULT 0 COMMENT '系统用户ID',
    `act_type`    tinyint(1)          NOT NULL DEFAULT 0 COMMENT '操作类型：0.其他1.打赏2.付费文章',
    `act_id`      int                 NOT NULL DEFAULT 0 COMMENT '操作关联ID：系统文章ID',
    `money`       decimal(10, 4)      NOT NULL DEFAULT 0 COMMENT '订单金额',
    `pay_url`     varchar(1024)       NOT NULL DEFAULT '' COMMENT '第三方支付地址',
    `pay_num`     varchar(63)         NOT NULL DEFAULT '' COMMENT '第三方支付订单号',
    `pay_user_id` varchar(63)         NOT NULL DEFAULT '' COMMENT '第三方支付账号ID',
    `pay_time`    datetime            NOT NULL COMMENT '实际支付时间',
    `pay_money`   decimal(10, 4)      NOT NULL DEFAULT 0 COMMENT '实际支付金额',
    `note`        varchar(255)        NOT NULL DEFAULT '' COMMENT '备注',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='系统-支付订单表';