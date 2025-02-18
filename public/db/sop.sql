-- 【数据库初始化创建SOP脚本】

-- 1.创建数据库，数据库名可自定义
CREATE DATABASE IF NOT EXISTS `pn_blog` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

-- 切换到数据库
USE `pn_blog`;

-- 2.创建表结构
source schema.sql;

-- 初始化数据
source data.sql;

-- 初始化安装记录，版本号为update.sql的最后一次更新版本号
INSERT INTO `sys_update_log` (`create_time`, `update_time`, `version`, `note`)
VALUES (now(), now(), '2025021701', '系统初始化安装完成');