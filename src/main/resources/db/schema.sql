CREATE TABLE IF NOT EXISTS `task`
(
    `id`        LONG NOT NULL,
    `name`      VARCHAR(50)  DEFAULT NULL,
    `start_time` DATETIME     DEFAULT NULL,
    `end_time`   DATETIME     DEFAULT NULL,
    `count`     LONG         DEFAULT NULL,
    `status`    VARCHAR(2)   DEFAULT NULL,
    `reason`    VARCHAR(200) DEFAULT NULL,
    PRIMARY KEY (`id`)
);