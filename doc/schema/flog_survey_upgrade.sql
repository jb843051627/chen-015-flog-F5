-- flog 伐区调查设计（界册并版 + 检尺三级链）—— 旧库升级脚本
-- 在已建过初版 t_flog_yield_rule 的库上执行一遍；新库直接执行 flog.sql 即可，勿重复执行本件。
-- 库：chen_015

-- 1) 界册补两格：管哪一类林地、在不在场的旗子。
--    旧册里既有的线先归到商品林(2)并置为在场，换版前请逐条甄别改挂，别让公益林的小班错借商品林的界。
ALTER TABLE t_flog_yield_rule
  ADD COLUMN apply_scope int NOT NULL DEFAULT 2
    COMMENT '管哪一类林地 2商品林 11公益林·抚育 12公益林·更新 13公益林·低质低效林改造' AFTER rule_name,
  ADD COLUMN present_flag int NOT NULL DEFAULT 1
    COMMENT '在不在场的旗子 1在场 0撤下' AFTER priority,
  ADD INDEX idx_rule_scope_eff (apply_scope, del_flag, present_flag, status, eff_start, eff_end);

-- 旧线 eff_start/priority/status 原先允许空；并版后取界要靠这几格，给个兜底再由业务补齐。
UPDATE t_flog_yield_rule SET priority = 0 WHERE priority IS NULL;
UPDATE t_flog_yield_rule SET status = 0 WHERE status IS NULL;
UPDATE t_flog_yield_rule SET del_flag = 0 WHERE del_flag IS NULL;

-- 2) 材积表版本册（三级链第二级取数处；同一份检尺只认同一版）。
CREATE TABLE IF NOT EXISTS t_flog_volume_table (
  id bigint NOT NULL COMMENT '主键',
  table_code varchar(64) NOT NULL COMMENT '材积表代号',
  table_name varchar(128) DEFAULT NULL COMMENT '材积表名',
  param_a decimal(20,10) NOT NULL COMMENT '材积式系数 a（V=a*D^b*H^c）',
  exp_d decimal(8,4) NOT NULL COMMENT '胸径指数 b',
  exp_h decimal(8,4) NOT NULL COMMENT '树高指数 c',
  eff_start datetime NOT NULL COMMENT '启用之日（含）',
  eff_end datetime DEFAULT NULL COMMENT '交棒之日(不含)；空着=现行版',
  priority int NOT NULL DEFAULT 0 COMMENT '抢版顺位(数值越大越先开口)',
  present_flag int NOT NULL DEFAULT 1 COMMENT '在不在场的旗子 1在场 0撤下',
  status int NOT NULL DEFAULT 0 COMMENT '情形 0现行 1已让位',
  del_flag int NOT NULL DEFAULT 0 COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  KEY idx_voltbl_eff (del_flag, present_flag, status, eff_start, eff_end),
  KEY idx_voltbl_code (table_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='二元立木材积表版本册';

-- 3) 调查设计单头 + 逐株检尺账 + 定档结论（逐轮追加）。
CREATE TABLE IF NOT EXISTS t_flog_survey_design (
  id bigint NOT NULL COMMENT '主键',
  design_no varchar(64) DEFAULT NULL COMMENT '调查设计单号',
  site_no varchar(64) DEFAULT NULL COMMENT '编制单位底档字号',
  subcomp_no varchar(64) DEFAULT NULL COMMENT '小班号',
  cut_block_no varchar(64) DEFAULT NULL COMMENT '伐区名号',
  apply_scope int NOT NULL COMMENT '林地类别 2商品林 11公益林·抚育 12公益林·更新 13公益林·低质低效林改造',
  area decimal(12,2) NOT NULL COMMENT '小班面积(公顷)',
  yield_rate decimal(8,4) NOT NULL COMMENT '出材率',
  survey_by varchar(64) DEFAULT NULL COMMENT '调查员（山场检尺人）',
  design_date datetime NOT NULL COMMENT '调查设计那一日（取界只认这一天）',
  latest_result_id bigint DEFAULT NULL COMMENT '最近一轮定档结论id（只作指向；结论行本身钉死不改）',
  del_flag int NOT NULL DEFAULT 0 COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  UNIQUE KEY uk_survey_no (design_no),
  KEY idx_survey_site_sub (site_no, subcomp_no),
  KEY idx_survey_date (design_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='伐区调查设计单头';

CREATE TABLE IF NOT EXISTS t_flog_survey_tree (
  id bigint NOT NULL COMMENT '主键',
  design_id bigint NOT NULL COMMENT '所属调查设计单id',
  tree_no int NOT NULL COMMENT '株序（山场记账顺序）',
  round_no int NOT NULL DEFAULT 1 COMMENT '所属轮次 1首评 2整改复验…（各轮检尺账各记各的）',
  dbh decimal(8,2) NOT NULL COMMENT '胸径(厘米)',
  height decimal(8,2) NOT NULL COMMENT '树高(米)',
  tree_volume decimal(14,6) DEFAULT NULL COMMENT '逐株材积(立方米，同一回计算钉入)',
  del_flag int NOT NULL DEFAULT 0 COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_tree_design (design_id, round_no, tree_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='伐区调查山场检尺逐株账';

CREATE TABLE IF NOT EXISTS t_flog_survey_result (
  id bigint NOT NULL COMMENT '主键',
  design_id bigint NOT NULL COMMENT '所属调查设计单id',
  round_no int NOT NULL COMMENT '轮次 1首评 2整改复验 3再复验…',
  recheck_flag int NOT NULL DEFAULT 0 COMMENT '是否复验 0首评 1整改复验',
  total_volume decimal(14,4) DEFAULT NULL COMMENT '本回总蓄积(立方米，逐株累加)',
  yield_per_ha decimal(14,4) DEFAULT NULL COMMENT '本回每公顷出材量(立方米/公顷)=蓄积*出材率/面积',
  grade_level int DEFAULT NULL COMMENT '强度档位 1轻 2中 3重；无线可依时为空',
  basis_missing int NOT NULL DEFAULT 0 COMMENT '是否无线可依 0有线 1当日在场界翻不出，写明无线，不拿邻近日子凑',
  rule_code varchar(64) DEFAULT NULL COMMENT '所依界代号',
  rule_eff_start datetime DEFAULT NULL COMMENT '所依那条界的启用之日',
  vol_table_code varchar(64) DEFAULT NULL COMMENT '所用材积表版代号',
  vol_table_eff_start datetime DEFAULT NULL COMMENT '所用材积表版启用之日',
  pinned_by varchar(64) DEFAULT NULL COMMENT '钉单人',
  pinned_time datetime DEFAULT NULL COMMENT '钉单时刻',
  del_flag int NOT NULL DEFAULT 0 COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  KEY idx_result_design_round (design_id, round_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='伐区调查设计定档结论（逐轮追加，钉死不改）';

-- 4) 菜单与按钮权限点（界册段、调查设计段），与 flog.sql 中 seed 同号，已在新库执行过会自动跳过。
INSERT IGNORE INTO t_sys_permission
  (id, name, descripion, url, is_blank, pid, perms, type, icon, order_num, visible, create_by, create_time)
VALUES
  (910000000000000014, '可采强度界册', '界册各版只供翻检', '/FlogYieldRuleController/view', 0, 0,
   'flog:flogYieldRule:view', 1, 'fa fa-ruler', 94, 0, 'seed', NOW()),
  (910000000000000015, '伐区调查设计', '检尺三级链一处算清并钉档', '/FlogSurveyDesignController/view', 0, 0,
   'flog:flogSurveyDesign:view', 1, 'fa fa-file-text', 95, 0, 'seed', NOW()),
  (910000000000000016, '调查设计提交复验', '提交钉档/整改复验重算追加', '/FlogSurveyDesignController/submit', 0, 910000000000000015,
   'flog:flogSurveyDesign:add', 2, '', 1, 0, 'seed', NOW());

INSERT IGNORE INTO t_sys_permission_role (id, role_id, permission_id, create_by, create_time)
SELECT 911000000000000000 + r.n + 10 * p.n, r.role_id, p.permission_id, 'seed', NOW()
FROM (
  SELECT 0 AS n, 488243256161730560 AS role_id
  UNION ALL SELECT 1, 488289006124007424
) r
CROSS JOIN (
  SELECT 14 AS n, 910000000000000014 AS permission_id
  UNION ALL SELECT 15, 910000000000000015
  UNION ALL SELECT 16, 910000000000000016
) p;
