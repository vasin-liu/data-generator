-- 模板表
CREATE TABLE IF NOT EXISTS `template`
(
    `id`            LONG NOT NULL,
    `name`          VARCHAR(128) DEFAULT NULL,
    `file_name`     VARCHAR(512) DEFAULT NULL,
    `file_ext`      VARCHAR(8)   DEFAULT NULL,
    `json_content`  CLOB         DEFAULT NULL,
    `yaml_content`  CLOB         DEFAULT NULL,
    `status`        VARCHAR(8)   DEFAULT NULL,
    PRIMARY KEY (`id`)
);

-- 任务表
CREATE TABLE IF NOT EXISTS `task`
(
    `id`         LONG NOT NULL,
    `name`       VARCHAR(50)  DEFAULT NULL,
    `start_time` DATETIME     DEFAULT NULL,
    `end_time`   DATETIME     DEFAULT NULL,
    `count`      LONG         DEFAULT NULL,
    `status`     VARCHAR(2)   DEFAULT NULL,
    `reason`     VARCHAR(200) DEFAULT NULL,
    PRIMARY KEY (`id`)
);