-- --------------------------------------------------------
-- 网易企业邮箱组织架构同步 — 数据库初始化脚本
-- 适用版本：若依（RuoYi）V3.9.2
-- 创建日期：2026-08-20
-- 说明：执行前请确认数据库已选择正确的 schema（ry-vue）
-- --------------------------------------------------------

-- ----------------------------
-- 1. 第三方同步映射表
-- ----------------------------
CREATE TABLE IF NOT EXISTS sys_third_sync_mapping (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',

    -- 定位维度
    third_type      VARCHAR(20)  NOT NULL DEFAULT 'netease_mail'
                    COMMENT '第三方类型：netease_mail',
    ruoyi_table     VARCHAR(50)  NOT NULL
                    COMMENT '若依表名：sys_dept、sys_user',
    ruoyi_id        BIGINT       NOT NULL
                    COMMENT '若依表主键ID（dept_id / user_id）',

    -- 第三方身份标识
    third_id        VARCHAR(64)  DEFAULT NULL
                    COMMENT '第三方唯一ID（unitOpenId / accountOpenId）',
    third_alt_id    VARCHAR(64)  DEFAULT NULL
                    COMMENT '第三方数字ID（unitId，父子关联键）',
    third_parent_id VARCHAR(64)  DEFAULT NULL
                    COMMENT '第三方父级ID（仅部门树使用，如 unitParentId）',

    -- 数据快照
    third_json      TEXT         COMMENT '第三方原始数据JSON快照',

    -- 同步状态机
    sync_status     TINYINT      NOT NULL DEFAULT 1
                    COMMENT '1-正常、2-待删除、3-已停用、4-冲突待处理',
    sync_time       DATETIME     DEFAULT NULL COMMENT '最后同步时间',
    sync_version    BIGINT       DEFAULT 0 COMMENT '同步版本号（对应网易 revision）',

    -- 审计字段
    create_by       VARCHAR(64)  DEFAULT '' COMMENT '创建者',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_by       VARCHAR(64)  DEFAULT '' COMMENT '更新者',
    update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- 约束
    UNIQUE KEY uk_third (third_type, ruoyi_table, third_id),
    UNIQUE KEY uk_ruoyi (third_type, ruoyi_table, ruoyi_id),
    KEY idx_third_alt_id (third_alt_id),
    KEY idx_sync_status (sync_status),
    KEY idx_sync_time (sync_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='第三方系统同步映射表';

-- ----------------------------
-- 2. 同步日志表
-- ----------------------------
CREATE TABLE IF NOT EXISTS sys_third_sync_log (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    sync_type       VARCHAR(20)  NOT NULL COMMENT '同步类型：dept、user、inspect',
    sync_action     VARCHAR(20)  NOT NULL COMMENT '操作：INSERT、UPDATE、DELETE、SKIP、INSPECT_FIX',
    third_id        VARCHAR(64)  DEFAULT NULL COMMENT '第三方ID',
    ruoyi_id        BIGINT       DEFAULT NULL COMMENT '若依ID',
    before_json     TEXT         COMMENT '同步前数据',
    after_json      TEXT         COMMENT '同步后数据',
    sync_status     TINYINT      NOT NULL DEFAULT 1 COMMENT '1-成功、0-失败',
    error_msg       VARCHAR(2000) DEFAULT NULL,
    sync_version    BIGINT       DEFAULT 0 COMMENT '本次同步版本号',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    KEY idx_third_id (third_id),
    KEY idx_create_time (create_time),
    KEY idx_sync_type (sync_type, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='第三方同步日志表';

-- ----------------------------
-- 3. 网易邮箱同步配置项（通过 sys_config 表管理）
-- 注意：以下 INSERT 使用 IGNORE 避免重复执行时报错
-- ----------------------------
INSERT IGNORE INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
VALUES
('网易企业邮箱 AppId',         'netease.app.id',       '', 'Y', 'admin', NOW(), '网易企业邮箱开放平台应用ID'),
('网易企业邮箱 AuthCode',      'netease.auth.code',    '', 'Y', 'admin', NOW(), '应用授权码'),
('网易企业邮箱企业OpenId',     'netease.org.open.id',  '', 'Y', 'admin', NOW(), '企业OpenId'),
('网易企业邮箱域名',           'netease.domain',       '', 'Y', 'admin', NOW(), '企业邮箱域名，如 xxx.com'),
('网易企业邮箱同步版本号',     'netease.sync.revision','0', 'Y', 'admin', NOW(), '已同步的最新版本号，默认0'),
('网易企业邮箱同步模式',       'netease.sync.mode',    'increment', 'Y', 'admin', NOW(), 'increment 增量 / full 全量'),
('网易邮箱部门对账开关',       'netease.dept.reconcile.enabled', 'true', 'Y', 'admin', NOW(), 'false 时跳过部门对账');

-- ----------------------------
-- 4. 定时任务注册（通过 sys_job 表管理）
-- 注意：concurrent='1' 表示禁止并发执行，Quartz 自身会防止重叠
-- ----------------------------
INSERT IGNORE INTO sys_job (job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent, status, create_by, create_time, remark)
VALUES
('网易邮箱日常同步', 'NETEASE', 'neteaseSyncJob.syncDaily', '0 0 2 * * ?', '3', '1', '0', 'admin', NOW(), '每日凌晨2点增量同步网易企业邮箱组织架构'),
('网易邮箱全量巡检', 'NETEASE', 'neteaseSyncJob.inspect', '0 0 3 ? * 1', '3', '1', '0', 'admin', NOW(), '每周日03:00全量哈希比对巡检'),
('网易邮箱部门对账', 'NETEASE', 'neteaseSyncJob.syncDeptReconcile', '0 */30 * * * ?', '3', '1', '0', 'admin', NOW(), '每30分钟轮询getUnitList对账部门（网易revision流不含部门事件）');

-- ----------------------------
-- 5. 开发环境初始化：删除旧种子数据，保留根锚点，admin/ry 挂顶层
-- 注意：仅开发环境执行，切勿在生产环境运行
-- ----------------------------
-- 删除旧种子部门（保留根锚点 dept_id=100）
DELETE FROM sys_dept WHERE dept_id != 100;

-- admin 和 ry 挂到根锚点（顶层）
UPDATE sys_user SET dept_id = 100 WHERE user_id IN (1, 2);