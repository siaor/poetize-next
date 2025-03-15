-- 【手动创建数据库SOP脚本】application.yml设置sys:auto-init: true时，会自动创建表结构、初始化数据

-- 非h2数据库需要手动创建数据库，第2、3步可自动创建

-- 1.创建数据库，数据库名可自定义
CREATE DATABASE IF NOT EXISTS `poetize_next` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

-- 切换到数据库
USE `poetize_next`;

-- 2.创建表结构：resource/db/mysql/schema.sql
-- source schema.sql;

-- 3.初始化数据：resource/db/mysql/data.sql
-- source data.sql;