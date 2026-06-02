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

-- Operator-console template run history (P3)
CREATE TABLE IF NOT EXISTS `task_execution`
(
    `id`               LONG         NOT NULL,
    `template_id`      LONG         NOT NULL,
    `template_name`    VARCHAR(128) DEFAULT NULL,
    `instance_id`      LONG         NOT NULL,
    `definition_kind`  VARCHAR(8)   DEFAULT NULL,
    `status`           VARCHAR(16)  NOT NULL,
    `queued_at`        TIMESTAMP    DEFAULT NULL,
    `started_at`       TIMESTAMP    DEFAULT NULL,
    `finished_at`      TIMESTAMP    DEFAULT NULL,
    `row_count`        LONG         DEFAULT NULL,
    `error_message`    VARCHAR(4000) DEFAULT NULL,
    `metrics_json`     CLOB         DEFAULT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_task_execution_instance` UNIQUE (`instance_id`)
);

-- Upgrade existing file DBs created before operator-console columns (CREATE TABLE IF NOT EXISTS is a no-op).
ALTER TABLE `template` ADD COLUMN IF NOT EXISTS `archived` BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE `template` ADD COLUMN IF NOT EXISTS `archived_at` TIMESTAMP DEFAULT NULL;
ALTER TABLE `task_execution` ADD COLUMN IF NOT EXISTS `parent_pipeline_run_id` VARCHAR(64);
ALTER TABLE `task_execution` ADD COLUMN IF NOT EXISTS `upstream_artifact_refs_json` CLOB;
ALTER TABLE `task_execution` ADD COLUMN IF NOT EXISTS `report_json` CLOB;
ALTER TABLE `template` ADD COLUMN IF NOT EXISTS `status` VARCHAR(16) DEFAULT 'PUBLISHED';
ALTER TABLE `datasource_config` ADD COLUMN IF NOT EXISTS `password_secret_ref` VARCHAR(256);
ALTER TABLE `task_execution` ADD COLUMN IF NOT EXISTS `cancel_requested` BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE `task_execution` ADD COLUMN IF NOT EXISTS `template_version` VARCHAR(64);
ALTER TABLE `task_execution` ADD COLUMN IF NOT EXISTS `plugin_set_json` CLOB;
ALTER TABLE `task_execution` ADD COLUMN IF NOT EXISTS `datasource_config_hash` VARCHAR(64);
ALTER TABLE `task_execution` ADD COLUMN IF NOT EXISTS `trigger_type` VARCHAR(16) DEFAULT 'MANUAL';
ALTER TABLE `task_execution` ADD COLUMN IF NOT EXISTS `schedule_id` LONG;

-- Secret registry for passwordSecretRef resolution (Phase B)
CREATE TABLE IF NOT EXISTS `secret_entry`
(
    `name`        VARCHAR(256) NOT NULL,
    `secret_value` VARCHAR(2048) NOT NULL,
    `description` VARCHAR(512) DEFAULT NULL,
    `created_at`  TIMESTAMP    DEFAULT NULL,
    `updated_at`  TIMESTAMP    DEFAULT NULL,
    PRIMARY KEY (`name`)
);

-- Operator audit trail (Phase B)
CREATE TABLE IF NOT EXISTS `audit_event`
(
    `id`            LONG         NOT NULL,
    `occurred_at`   TIMESTAMP    NOT NULL,
    `actor`         VARCHAR(128) DEFAULT NULL,
    `action`        VARCHAR(64)  NOT NULL,
    `resource_type` VARCHAR(32)  NOT NULL,
    `resource_id`   VARCHAR(256) DEFAULT NULL,
    `detail_json`   CLOB         DEFAULT NULL,
    PRIMARY KEY (`id`)
);

-- Distributed execution queue and lease state (Phase C2)
CREATE TABLE IF NOT EXISTS `distributed_job`
(
    `id`                LONG         NOT NULL,
    `task_execution_id` LONG         DEFAULT NULL,
    `template_id`       LONG         DEFAULT NULL,
    `instance_id`       LONG         NOT NULL,
    `status`            VARCHAR(16)  NOT NULL,
    `worker_id`         VARCHAR(128) DEFAULT NULL,
    `queued_at`         TIMESTAMP    NOT NULL,
    `leased_at`         TIMESTAMP    DEFAULT NULL,
    `last_heartbeat_at` TIMESTAMP    DEFAULT NULL,
    `lease_until`       TIMESTAMP    DEFAULT NULL,
    `started_at`        TIMESTAMP    DEFAULT NULL,
    `finished_at`       TIMESTAMP    DEFAULT NULL,
    `attempts`          INT          DEFAULT 0 NOT NULL,
    `error_message`     VARCHAR(4000) DEFAULT NULL,
    `payload_json`      CLOB         DEFAULT NULL,
    `created_at`        TIMESTAMP    DEFAULT NULL,
    `updated_at`        TIMESTAMP    DEFAULT NULL,
    PRIMARY KEY (`id`)
);

-- Cron-driven template run schedules (Phase B schedule hook)
CREATE TABLE IF NOT EXISTS `task_schedule`
(
    `id`                LONG         NOT NULL,
    `template_id`       LONG         NOT NULL,
    `cron_expression`   VARCHAR(128) NOT NULL,
    `enabled`           BOOLEAN      DEFAULT TRUE NOT NULL,
    `description`       VARCHAR(512) DEFAULT NULL,
    `last_triggered_at` TIMESTAMP    DEFAULT NULL,
    `last_instance_id`  LONG         DEFAULT NULL,
    `next_trigger_at`   TIMESTAMP    DEFAULT NULL,
    `created_at`        TIMESTAMP    DEFAULT NULL,
    `updated_at`        TIMESTAMP    DEFAULT NULL,
    PRIMARY KEY (`id`)
);

ALTER TABLE `task_schedule` ADD COLUMN IF NOT EXISTS `last_instance_id` LONG;

-- Cron-driven template run schedules (Phase B schedule hook)
CREATE TABLE IF NOT EXISTS `task_schedule`
(
    `id`                LONG         NOT NULL,
    `template_id`       LONG         NOT NULL,
    `cron_expression`   VARCHAR(128) NOT NULL,
    `enabled`           BOOLEAN      DEFAULT TRUE NOT NULL,
    `description`       VARCHAR(512) DEFAULT NULL,
    `last_triggered_at` TIMESTAMP    DEFAULT NULL,
    `next_trigger_at`   TIMESTAMP    DEFAULT NULL,
    `created_at`        TIMESTAMP    DEFAULT NULL,
    `updated_at`        TIMESTAMP    DEFAULT NULL,
    PRIMARY KEY (`id`)
);