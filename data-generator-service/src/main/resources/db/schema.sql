-- 模板表
CREATE TABLE IF NOT EXISTS `template`
(
    `id`           LONG NOT NULL,
    `name`         VARCHAR(128) DEFAULT NULL,
    `file_name`    VARCHAR(512) DEFAULT NULL,
    `file_ext`     VARCHAR(8)   DEFAULT NULL,
    `path_md5`     VARCHAR(64)  DEFAULT NULL,
    `content_md5`  VARCHAR(64)  DEFAULT NULL,
    `content_json` CLOB         DEFAULT NULL,
    `content_yaml` CLOB         DEFAULT NULL,
    `status`       VARCHAR(8)   DEFAULT NULL,
    `archived`     BOOLEAN      DEFAULT FALSE NOT NULL,
    `archived_at`  TIMESTAMP    DEFAULT NULL,
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
    `reason`     VARCHAR(200) DEFAULT NULL,
    PRIMARY KEY (`id`)
);