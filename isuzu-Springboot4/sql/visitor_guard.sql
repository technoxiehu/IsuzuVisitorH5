-- ============================================================
-- 门卫电脑端（来访核验 + 入场记录）初始化脚本
-- 适用：isuzu-Springboot4（若依 RuoYi v3.9.2）
-- 内容：
--   1. visitor_entry 入场/放行记录表 DDL（v1.10，与 docs/03_接口契约.md §2.6 一致）
--   2. 门卫角色 + 门卫菜单 + 权限按钮
--   3. 角色-菜单关联（门卫角色仅可见来访核验/入场记录两个菜单）
-- 幂等：可重复执行（插入前先按 menu_id/role_id/表名判断存在则跳过）
-- ============================================================

-- ------------------------------------------------------------
-- 1. 入场/放行记录表 visitor_entry
-- ------------------------------------------------------------
create table if not exists visitor_entry (
  entry_id       varchar(36)  not null comment '入场记录ID(后端生成UUID)',
  application_id varchar(36)  not null comment '申请单号',
  operator_id    bigint       not null comment '放行门卫(sys_user.user_id)',
  operator_name  varchar(30)  not null comment '放行门卫姓名(冗余)',
  create_time    datetime     not null comment '放行时间(应用服务器时间)',
  primary key (entry_id),
  key idx_application_id (application_id),
  key idx_entry_time (create_time)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_general_ci comment = '访客入场/放行记录表';

-- ------------------------------------------------------------
-- 2. 门卫菜单（顶层目录「门卫核验」，路由前缀 /visitor）
-- ------------------------------------------------------------
-- 顶层目录
insert into sys_menu(menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 2000, '门卫核验', 0, 5, 'visitor', null, 1, 0, 'M', '0', '0', '', 'monitor', 'admin', sysdate(), '门卫核验目录(电脑端大屏)'
from dual where not exists (select 1 from sys_menu where menu_id = 2000);

-- 来访核验（卡片墙大屏，后台态入口）
insert into sys_menu(menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 2001, '来访核验', 2000, 1, 'guard', 'visitor/guard/index', 1, 0, 'C', '0', '0', 'visitor:guard:list', 'fullscreen', 'admin', sysdate(), '门卫核验：今日有效单据卡片墙+放行'
from dual where not exists (select 1 from sys_menu where menu_id = 2001);

-- 入场记录（放行历史查询）
insert into sys_menu(menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 2002, '入场记录', 2000, 2, 'guard/entry', 'visitor/guard/entry/index', 1, 0, 'C', '0', '0', 'visitor:entry:list', 'time', 'admin', sysdate(), '放行/入场记录查询'
from dual where not exists (select 1 from sys_menu where menu_id = 2002);

-- 放行按钮（来访核验页）
insert into sys_menu(menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 2003, '放行', 2001, 1, '', null, 1, 0, 'F', '0', '0', 'visitor:guard:entry', '#', 'admin', sysdate(), '来访核验-放行按钮'
from dual where not exists (select 1 from sys_menu where menu_id = 2003);

-- ------------------------------------------------------------
-- 3. 门卫角色（role_key=guard，仅访问门卫核验菜单）
-- ------------------------------------------------------------
insert into sys_role(role_id, role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, del_flag, create_by, create_time, remark)
select 3, '门卫', 'guard', 3, 1, 1, 1, '0', '0', 'admin', sysdate(), '门卫角色：仅来访核验/入场记录'
from dual where not exists (select 1 from sys_role where role_id = 3);

-- 角色-菜单关联（门卫：目录2000 + 菜单2001/2002 + 按钮2003）
insert into sys_role_menu(role_id, menu_id)
select 3, menu_id from sys_menu
where menu_id in (2000, 2001, 2002, 2003)
  and not exists (select 1 from sys_role_menu rm where rm.role_id = 3 and rm.menu_id = sys_menu.menu_id);

-- ------------------------------------------------------------
-- 4. 将门卫角色分配给具体门卫账号
--    方式一：后台「系统管理-用户管理」编辑用户勾选「门卫」角色；
--    方式二：执行下方语句（user_id 换成实际门卫账号，如 sys_user 中 id=2 的普通用户）：
-- insert into sys_user_role(user_id, role_id) select 2, 3
--   from dual where not exists (select 1 from sys_user_role where user_id = 2 and role_id = 3);
-- ------------------------------------------------------------
