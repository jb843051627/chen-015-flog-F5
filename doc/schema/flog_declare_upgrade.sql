-- flog 伐区申报清册逐行核收 —— 旧库升级脚本（在已建过初版 t_flog_declare_row 的库上执行一遍）。
-- 新库直接执行 flog.sql 即可，勿重复执行本件。
-- 库：chen_015

ALTER TABLE t_flog_declare_row
  ADD COLUMN site_no varchar(64) DEFAULT NULL COMMENT '申报编制单位底档字号' AFTER row_no,
  ADD COLUMN subcomp_no varchar(64) DEFAULT NULL COMMENT '小班号' AFTER site_no,
  ADD COLUMN cut_mode int DEFAULT NULL COMMENT '采伐方式 1皆伐 2渐伐 3择伐 4抚育采伐 5更新采伐 6低质低效林改造' AFTER subcomp_no,
  ADD COLUMN forest_kind int DEFAULT NULL COMMENT '林种性质 1公益林 2商品林' AFTER cut_mode,
  ADD COLUMN dispute_flag int DEFAULT 0 COMMENT '权属是否在诉 0无 1有' AFTER forest_kind,
  ADD COLUMN area decimal(12,2) DEFAULT NULL COMMENT '本行申报拟采面积(公顷)' AFTER dispute_flag,
  ADD COLUMN survey_flag int DEFAULT 0 COMMENT '附没附伐区调查设计材料 0未附 1已附' AFTER area,
  ADD COLUMN recv_qty decimal(12,2) DEFAULT NULL COMMENT '本行实际核收蓄积(立方米，不得大过申报)' AFTER qty,
  ADD COLUMN period_year int DEFAULT NULL COMMENT '归属执行期年度（进门那年）' AFTER recv_qty,
  ADD COLUMN page_flag int DEFAULT 0 COMMENT '账页 0现行页 1往期页（收满两个执行期转往期）' AFTER period_year,
  ADD COLUMN merge_of_row_no int DEFAULT NULL COMMENT '重复行并账：本行并进的头行行次' AFTER page_flag,
  ADD INDEX idx_declrow_batch_row (batch_no, row_no),
  ADD INDEX idx_declrow_batch_site_sub (batch_no, site_no, subcomp_no),
  ADD INDEX idx_declrow_page (page_flag, period_year),
  ADD INDEX idx_declrow_status (status);

CREATE TABLE IF NOT EXISTS t_flog_declare_batch (
  id bigint NOT NULL COMMENT '主键',
  batch_no varchar(64) NOT NULL COMMENT '伐区申报清册批次号',
  site_no varchar(64) DEFAULT NULL COMMENT '申报编制单位底档字号',
  period_year int DEFAULT NULL COMMENT '进门执行期年度',
  total_rows int DEFAULT 0 COMMENT '本册总计行数（原册行次口径，重复行也占行次）',
  accepted_rows int DEFAULT 0 COMMENT '收下几行（逐行落账同一回数出）',
  suspended_rows int DEFAULT 0 COMMENT '挂起几行（逐行落账同一回数出）',
  max_row_no int DEFAULT 0 COMMENT '已进门行次序（最后落定的行次，单行递入时续它）',
  recv_status int DEFAULT 0 COMMENT '收册落法 0收册中 1核讫 2空册 3整册退回',
  recv_time datetime DEFAULT NULL COMMENT '收册回执时刻',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  UNIQUE KEY uk_declbatch_no (batch_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='伐区申报清册批次头与回执';

CREATE TABLE IF NOT EXISTS t_flog_declare_diff (
  id bigint NOT NULL COMMENT '主键',
  batch_no varchar(64) NOT NULL COMMENT '批次号',
  site_no varchar(64) DEFAULT NULL COMMENT '编制单位底档字号',
  subcomp_no varchar(64) DEFAULT NULL COMMENT '小班号',
  row_no int DEFAULT NULL COMMENT '原清册行次',
  diff_kind int DEFAULT 1 COMMENT '差异种类 1蓄积调整 2其它更正',
  old_qty decimal(12,2) DEFAULT NULL COMMENT '原申报/原核收蓄积',
  new_qty decimal(12,2) DEFAULT NULL COMMENT '申请改成的蓄积',
  status int DEFAULT 0 COMMENT '办理情形 0待办 1已入帐 2驳回',
  handle_by varchar(64) DEFAULT NULL COMMENT '办理人',
  handle_time datetime DEFAULT NULL COMMENT '办理时刻',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  KEY idx_decldiff_batch (batch_no),
  KEY idx_decldiff_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='伐区申报清册差异清单';

-- 菜单与按钮权限点（清册段），挂给管理员与用户两角色；与 flog.sql 中 seed 同号，已在新库执行过会自动跳过。
INSERT IGNORE INTO t_sys_permission
  (id, name, descripion, url, is_blank, pid, perms, type, icon, order_num, visible, create_by, create_time)
VALUES
  (910000000000000010, '申报清册核收', '伐区申报清册逐行核收', '/FlogDeclareRowController/view', 0, 0,
   'flog:flogDeclareRow:view', 1, 'fa fa-file-text-o', 93, 0, 'seed', NOW()),
  (910000000000000011, '清册逐行查询', '现行页/挂起/往期', '/FlogDeclareRowController/list', 0, 910000000000000010,
   'flog:flogDeclareRow:list', 2, '', 1, 0, 'seed', NOW()),
  (910000000000000012, '清册收册递行', '整册提交/逐行递入/回执', '/FlogDeclareRowController/submit', 0, 910000000000000010,
   'flog:flogDeclareRow:add', 2, '', 2, 0, 'seed', NOW()),
  (910000000000000013, '往期结转与差异办理', '收满两期转往期/差异清单入账', '/FlogDeclareRowController/turn', 0, 910000000000000010,
   'flog:flogDeclareRow:edit', 2, '', 3, 0, 'seed', NOW());

INSERT IGNORE INTO t_sys_permission_role (id, role_id, permission_id, create_by, create_time)
SELECT 911000000000000010 + r.n + 10 * p.n, r.role_id, p.permission_id, 'seed', NOW()
FROM (
  SELECT 0 AS n, 488243256161730560 AS role_id
  UNION ALL SELECT 1, 488289006124007424
) r
CROSS JOIN (
  SELECT 1 AS n, 910000000000000010 AS permission_id
  UNION ALL SELECT 2, 910000000000000011
  UNION ALL SELECT 3, 910000000000000012
  UNION ALL SELECT 4, 910000000000000013
) p;
