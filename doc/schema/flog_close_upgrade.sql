-- flog 伐区勾销事务卡（报伐/核株/验更/了断 四段事务线 + 勾销履历指纹链）—— 旧库升级脚本
-- 在已建过初版 t_flog_close_card（只有 stage/status/content/last_action 空架子）的库上执行一遍。
-- 新库直接执行 flog.sql 即可，勿重复执行本件。
-- 库：chen_015

-- 1) 勾销卡从空架子补成事务卡。
--    status 口径改了：旧的 2=已了断 仍作 3=已了断；旧架子里 status=0(未起) 的草稿行转 1(在办)，
--    stage 照旧（0..3 为停着待过的道口）。旧 content/last_action 两栏没有履历含义，保留不读。
ALTER TABLE t_flog_close_card
  ADD COLUMN site_no varchar(64) DEFAULT NULL COMMENT '编制单位底档字号' AFTER biz_no,
  ADD COLUMN subcomp_no varchar(64) DEFAULT NULL COMMENT '小班号' AFTER site_no,
  ADD COLUMN cut_block_no varchar(64) DEFAULT NULL COMMENT '伐区名号' AFTER subcomp_no,
  ADD COLUMN design_id bigint DEFAULT NULL COMMENT '钉过号的调查设计单id' AFTER cut_block_no,
  ADD COLUMN design_no varchar(64) DEFAULT NULL COMMENT '钉过号的调查设计单号' AFTER design_id,
  ADD COLUMN apply_card_id bigint DEFAULT NULL COMMENT '挂上的现行采伐建档卡id（了断摘它、退它的额）' AFTER design_no,
  ADD COLUMN bill_no varchar(64) DEFAULT NULL COMMENT '挂上的采伐建档卡号' AFTER apply_card_id,
  ADD COLUMN halt_reason varchar(500) DEFAULT NULL COMMENT '止步缘故' AFTER status,
  ADD COLUMN closed_by varchar(64) DEFAULT NULL COMMENT '了断经手人' AFTER halt_reason,
  ADD COLUMN closed_time datetime DEFAULT NULL COMMENT '了断时刻' AFTER closed_by,
  ADD COLUMN active_anchor varchar(160) DEFAULT NULL
    COMMENT '活卡锚：在办=底档字号#小班号，止步/了断置空' AFTER closed_time,
  ADD UNIQUE KEY uk_close_active_anchor (active_anchor),
  ADD INDEX idx_closebiz_no (biz_no),
  ADD INDEX idx_close_site_sub (site_no, subcomp_no, status);

-- 旧卡补活卡锚：在办(status 平移后=1) 的按 字号#小班号 钉；止步/了断留空。
UPDATE t_flog_close_card SET active_anchor = CONCAT(site_no, '#', subcomp_no) WHERE status = 1;

-- 旧 status 口径平移：2 已了断 -> 3；0 未起/1 在办 都按在办（旧架子根本没有止步一档）。
UPDATE t_flog_close_card SET status = 3 WHERE status = 2;
UPDATE t_flog_close_card SET status = 1 WHERE status IS NULL OR status NOT IN (1, 3);

-- 2) 勾销履历流水（只追加 + 指纹链；回退用“沉底”事件留痕，旧行不删不改）。
CREATE TABLE IF NOT EXISTS t_flog_close_journal (
  id bigint NOT NULL COMMENT '主键',
  card_id bigint NOT NULL COMMENT '勾销卡id',
  biz_no varchar(64) DEFAULT NULL COMMENT '勾销卡号',
  seq int NOT NULL COMMENT '卡内流水序（从1挨号，断号即缺页）',
  stage int NOT NULL COMMENT '本段 0报伐 1核株 2验更 3了断',
  attempt_no int NOT NULL DEFAULT 1 COMMENT '本段第几遍（头一遍1，回退重做挨号）',
  action int NOT NULL COMMENT '笔目 1过口 2止步 3沉底',
  ref_seq int DEFAULT NULL COMMENT '沉底事件专用：指被沉掉那笔过口的流水序',
  design_no varchar(64) DEFAULT NULL COMMENT '报伐：设计单上钉过的号',
  actual_trees int DEFAULT NULL COMMENT '报伐：实采株数',
  actual_volume decimal(14,4) DEFAULT NULL COMMENT '报伐：实采蓄积(立方米)',
  design_trees int DEFAULT NULL COMMENT '核株：设计株数（钉档那轮逐株账数出）',
  design_volume decimal(14,4) DEFAULT NULL COMMENT '核株：设计蓄积(立方米，钉档结论那笔)',
  tree_diff_rate decimal(8,4) DEFAULT NULL COMMENT '核株：株数相差比率(实采对设计)',
  vol_diff_rate decimal(8,4) DEFAULT NULL COMMENT '核株：蓄积相差比率(实采对设计)',
  diff_reason varchar(500) DEFAULT NULL COMMENT '核株：差过站里那格时的一句说法',
  cut_area decimal(12,2) DEFAULT NULL COMMENT '验更：采伐面积(公顷，照设计单钉住)',
  renew_area decimal(12,2) DEFAULT NULL COMMENT '验更：更新造林面积(公顷，小于采伐面积当场点回)',
  renew_survival decimal(5,2) DEFAULT NULL COMMENT '验更：更新造林成活率(%)',
  occupy_qty decimal(12,2) DEFAULT NULL COMMENT '了断：退回前该建档卡占着的额度(立方米)',
  refund_qty decimal(12,2) DEFAULT NULL COMMENT '了断：原样退回的额度(立方米)',
  op_by varchar(64) DEFAULT NULL COMMENT '本笔经手人',
  op_time datetime DEFAULT NULL COMMENT '本笔落账时刻',
  prev_fp varchar(64) DEFAULT NULL COMMENT '上一笔指纹（头一笔为空，链断即破）',
  fp varchar(64) DEFAULT NULL COMMENT '本笔指纹（落账一刻钉死，改过任何一格都对不上）',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常（履历只追加，永不置1）',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  UNIQUE KEY uk_closejrn_card_seq (card_id, seq),
  KEY idx_closejrn_stage (card_id, stage, action)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='伐区勾销履历流水（只追加+指纹链）';

-- 3) 菜单与按钮权限点（勾销卡段），与 flog.sql 中 seed 同号，已在新库执行过会自动跳过。
INSERT IGNORE INTO t_sys_permission
  (id, name, descripion, url, is_blank, pid, perms, type, icon, order_num, visible, create_by, create_time)
VALUES
  (910000000000000017, '伐区勾销事务卡', '一小班一张卡：报伐/核株/验更/了断挨着走', '/FlogCloseCardController/view', 0, 0,
   'flog:flogCloseCard:view', 1, 'fa fa-check-square-o', 96, 0, 'seed', NOW()),
  (910000000000000018, '勾销卡流转台账', '在办/止步/已了断集合', '/FlogCloseCardController/list', 0, 910000000000000017,
   'flog:flogCloseCard:list', 2, '', 1, 0, 'seed', NOW()),
  (910000000000000019, '起卡过口回退止步', '起卡/过口/回退沉底/止步', '/FlogCloseCardController/advance', 0, 910000000000000017,
   'flog:flogCloseCard:add', 2, '', 2, 0, 'seed', NOW()),
  (910000000000000020, '勾销履历翻检', '逐笔履历/了断核验', '/FlogCloseCardController/ledger', 0, 910000000000000017,
   'flog:flogCloseCard:edit', 2, '', 3, 0, 'seed', NOW());

INSERT IGNORE INTO t_sys_permission_role (id, role_id, permission_id, create_by, create_time)
SELECT 911000000000000000 + r.n + 10 * p.n, r.role_id, p.permission_id, 'seed', NOW()
FROM (
  SELECT 0 AS n, 488243256161730560 AS role_id
  UNION ALL SELECT 1, 488289006124007424
) r
CROSS JOIN (
  SELECT 17 AS n, 910000000000000017 AS permission_id
  UNION ALL SELECT 18, 910000000000000018
  UNION ALL SELECT 19, 910000000000000019
  UNION ALL SELECT 20, 910000000000000020
) p;
