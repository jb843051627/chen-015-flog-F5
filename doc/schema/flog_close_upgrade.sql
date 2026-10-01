-- flog 伐区勾销事务卡 —— 旧库升级脚本（在已建过初版 t_flog_close_card 的库上执行一遍）。
-- 新库直接执行 flog.sql 即可，勿重复执行本件。
-- 库：chen_015
--
-- 升级口径：卡头补齐四段要落的业务格；落定改记 1在办 2已了断 3止步；
-- “停在哪段”不再由卡头手填，改由 t_flog_close_stage 履历里 active 行数算出来。
-- 旧卡若已有 stage 数，按它补一道对应段次的 active 履历，使数出来的当前段与旧屏一致；
-- status=2 的旧卡补了断行。旧库没走过这趟业务时，这两步只是空补。

ALTER TABLE t_flog_close_card
  ADD COLUMN site_no varchar(64) DEFAULT NULL COMMENT '编制单位底档字号' AFTER biz_no,
  ADD COLUMN subcomp_no varchar(64) DEFAULT NULL COMMENT '小班号（一张卡一片小班）' AFTER site_no,
  ADD COLUMN cut_block_no varchar(64) DEFAULT NULL COMMENT '伐区名号' AFTER subcomp_no,
  ADD COLUMN design_no varchar(64) DEFAULT NULL COMMENT '报伐所认调查设计单号（钉过号的那张）' AFTER cut_block_no,
  ADD COLUMN design_trees int DEFAULT NULL COMMENT '设计株数（核株拿实采跟它对着看）' AFTER design_no,
  ADD COLUMN design_volume decimal(14,4) DEFAULT NULL COMMENT '设计蓄积(立方米)' AFTER design_trees,
  ADD COLUMN cut_area decimal(12,2) DEFAULT NULL COMMENT '采伐面积(公顷，验更拿更新面积跟它比)' AFTER design_volume,
  ADD COLUMN actual_trees int DEFAULT NULL COMMENT '实采株数（报伐口报上来）' AFTER cut_area,
  ADD COLUMN actual_volume decimal(14,4) DEFAULT NULL COMMENT '实采蓄积(立方米，报伐口报上来)' AFTER actual_trees,
  ADD COLUMN diff_tolerance decimal(8,4) DEFAULT NULL COMMENT '本站定的核株差格(立方米)，越过就得给一句说法' AFTER actual_volume,
  ADD COLUMN diff_reason varchar(500) DEFAULT NULL COMMENT '核株差额越过差格时的一句说法' AFTER diff_tolerance,
  ADD COLUMN renew_area decimal(12,2) DEFAULT NULL COMMENT '更新造林面积(公顷，不得小于采伐面积)' AFTER diff_reason,
  ADD COLUMN survive_rate decimal(6,2) DEFAULT NULL COMMENT '更新成活率(%)' AFTER renew_area,
  ADD COLUMN occupy_qty decimal(14,2) DEFAULT NULL COMMENT '本卡占着的额度(立方米)，了断原样退回' AFTER survive_rate,
  ADD COLUMN quota_year int DEFAULT NULL COMMENT '占额归属年度（退就退回这一年账）' AFTER occupy_qty,
  ADD COLUMN apply_card_id bigint DEFAULT NULL COMMENT '占额来源建档卡id（了断把它从在册处摘下、走它的压死退额）' AFTER quota_year,
  ADD COLUMN live_key varchar(128) DEFAULT NULL COMMENT '活卡对锁：在办=字号@小班，了断/止步置空' AFTER apply_card_id,
  ADD UNIQUE KEY uk_close_biz_no (biz_no),
  ADD UNIQUE KEY uk_close_live_key (live_key),
  ADD KEY idx_close_site_sub (site_no, subcomp_no),
  ADD KEY idx_close_live (status, del_flag);

-- 旧库 status=0（未起）并进“在办”（有卡即起）；活卡对锁回填，了断/止步卡留空不占对锁。
UPDATE t_flog_close_card SET status = 1 WHERE status = 0;
UPDATE t_flog_close_card SET live_key = CONCAT(site_no, '@', subcomp_no)
 WHERE status = 1 AND del_flag = 0;

-- 旧库 status 口径：0未起/1在办/2已了断。新口径把“未起”并进“在办”（有卡即起），
-- 3 留给止步。旧库没有止步卡，无需搬数；stage 旧值只是摆示，照旧留着由系统重算覆盖。

CREATE TABLE IF NOT EXISTS t_flog_close_stage (
  id bigint NOT NULL COMMENT '主键',
  card_id bigint NOT NULL COMMENT '勾销事务卡id',
  biz_no varchar(64) DEFAULT NULL COMMENT '勾销事务卡号（冗作回查）',
  stage int NOT NULL COMMENT '段次 0报伐 1核株 2验更 3了断',
  attempt_no int NOT NULL DEFAULT 1 COMMENT '本段第几轮（退回重做一轮，旧轮沉底、新轮另起）',
  action varchar(32) NOT NULL COMMENT '过口动作 PASS过成 FAIL止步 RETREAT退回',
  active_flag int NOT NULL DEFAULT 1 COMMENT '算不算数 1当前这道 0已沉底（退回/止步后旧道作废）',
  design_no varchar(64) DEFAULT NULL COMMENT '本报伐钉的设计单号',
  actual_trees int DEFAULT NULL COMMENT '本报伐实采株数',
  actual_volume decimal(14,4) DEFAULT NULL COMMENT '本报伐实采蓄积',
  design_trees int DEFAULT NULL COMMENT '设计株数快照',
  design_volume decimal(14,4) DEFAULT NULL COMMENT '设计蓄积快照',
  diff_value decimal(14,4) DEFAULT NULL COMMENT '实采与设计差额快照',
  renew_area decimal(12,2) DEFAULT NULL COMMENT '本验更更新面积',
  survive_rate decimal(6,2) DEFAULT NULL COMMENT '本验更成活率',
  refund_qty decimal(14,2) DEFAULT NULL COMMENT '了断退回额度(立方米)',
  note varchar(500) DEFAULT NULL COMMENT '本段一句话记事（说法/止步缘故，落头一遍，不许追第二句）',
  op_by varchar(64) DEFAULT NULL COMMENT '过口经办人',
  op_time datetime NOT NULL COMMENT '过口时刻',
  del_flag int NOT NULL DEFAULT 0 COMMENT '删除标记（履历行永不删，此格恒0）',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_stage_card_active (card_id, active_flag, stage),
  KEY idx_stage_biz (biz_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='伐区勾销履历（只追加，不改不删）';

-- 旧卡补履历：按旧卡头 stage 补 0..stage-1 各一道过成行（缺履历的卡才补，不重号）。
-- 仅把旧库那几张演示卡对齐到“履历数得出当前段”的新口径；新库无旧卡，本句空过。
INSERT INTO t_flog_close_stage
  (id, card_id, biz_no, stage, attempt_no, action, active_flag, op_by, op_time, del_flag)
SELECT 930000000000000000 + c.id * 10 + s.n,
       c.id, c.biz_no, s.n, 1, 'PASS', 1,
       COALESCE(c.create_by, 'upgrade'), COALESCE(c.create_time, NOW()), 0
FROM t_flog_close_card c
JOIN (
  SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3
) s
WHERE c.del_flag = 0
  AND c.stage IS NOT NULL
  AND s.n < c.stage
  AND NOT EXISTS (
    SELECT 1 FROM t_flog_close_stage h WHERE h.card_id = c.id AND h.stage = s.n AND h.active_flag = 1
  );

-- 已了断旧卡：把了断段补齐为 active 过成行，退回额度快照无从追溯，留空只作段次对齐。
INSERT INTO t_flog_close_stage
  (id, card_id, biz_no, stage, attempt_no, action, active_flag, op_by, op_time, del_flag)
SELECT 931000000000000000 + c.id,
       c.id, c.biz_no, 3, 1, 'PASS', 1,
       COALESCE(c.update_by, c.create_by, 'upgrade'), COALESCE(c.update_time, NOW()), 0
FROM t_flog_close_card c
WHERE c.del_flag = 0 AND c.status = 2
  AND NOT EXISTS (
    SELECT 1 FROM t_flog_close_stage h WHERE h.card_id = c.id AND h.stage = 3
  );

-- 菜单与按钮权限点（勾销卡段 17..20），挂给管理员与用户两角色；与 flog.sql 中 seed 同号，已在新库执行过会自动跳过。
INSERT IGNORE INTO t_sys_permission
  (id, name, descripion, url, is_blank, pid, perms, type, icon, order_num, visible, create_by, create_time)
VALUES
  (910000000000000017, '伐区勾销事务卡', '一小班一卡，四段挨着走（报伐/核株/验更/了断）', '/flogCloseCard/view', 0, 0,
   'flog:flogCloseCard:view', 1, 'fa fa-flag-checkered', 96, 0, 'seed', NOW()),
  (910000000000000018, '勾销卡台账查询', '流转台账/勾销履历核验', '/flogCloseCard/list', 0, 910000000000000017,
   'flog:flogCloseCard:view', 2, '', 1, 0, 'seed', NOW()),
  (910000000000000019, '勾销卡开卡报伐', '开卡即报伐，没有现开空卡的口子', '/flogCloseCard/add', 0, 910000000000000017,
   'flog:flogCloseCard:add', 2, '', 2, 0, 'seed', NOW()),
  (910000000000000020, '勾销卡推进退回止步', '往前一格/退回一格/止步封死', '/flogCloseCard/pass', 0, 910000000000000017,
   'flog:flogCloseCard:edit', 2, '', 3, 0, 'seed', NOW());

INSERT IGNORE INTO t_sys_permission_role (id, role_id, permission_id, create_by, create_time)
SELECT 911000000000000100 + r.n + 10 * p.n, r.role_id, p.permission_id, 'seed', NOW()
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
