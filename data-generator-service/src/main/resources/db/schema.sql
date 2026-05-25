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

-- Operator-console JDBC datasource registry (D1)
CREATE TABLE IF NOT EXISTS `datasource_config`
(
    `name`              VARCHAR(128)  NOT NULL,
    `url`               VARCHAR(1024) NOT NULL,
    `username`          VARCHAR(256)  DEFAULT NULL,
    `password`          VARCHAR(256)  DEFAULT NULL,
    `driver_class_name` VARCHAR(512)  NOT NULL,
    `driver_jar_path`   VARCHAR(1024) DEFAULT NULL,
    `enabled`           BOOLEAN       DEFAULT TRUE NOT NULL,
    `created_at`        TIMESTAMP     DEFAULT NULL,
    `updated_at`        TIMESTAMP     DEFAULT NULL,
    PRIMARY KEY (`name`)
);