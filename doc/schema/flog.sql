-- flog 林木采伐限额与采伐许可证管理 -- schema (chen-015)
-- 列名与基线实体契约（@TableName/@TableField）逐列对齐，改列必须同步实体。
-- 库：chen_015

CREATE TABLE IF NOT EXISTS t_flog_apply_card (
  id bigint NOT NULL COMMENT '主键',
  bill_no varchar(64) DEFAULT NULL COMMENT '伐区采伐申请建档卡号',
  node_no int DEFAULT NULL COMMENT '本卡版本序',
  site_id bigint DEFAULT NULL COMMENT '所属编制单位在册底档',
  site_no varchar(64) DEFAULT NULL COMMENT '所属单位底档字号',
  subcomp_no varchar(64) DEFAULT NULL COMMENT '小班号',
  cut_block_no varchar(64) DEFAULT NULL COMMENT '伐区名号',
  cut_mode int DEFAULT NULL COMMENT '采伐方式 1皆伐 2渐伐 3择伐 4抚育采伐 5更新采伐 6低质低效林改造',
  clear_trees int DEFAULT NULL COMMENT '皆伐专属：株数（面积走通用栏新报拟采面积 fine_amt）',
  grad_years int DEFAULT NULL COMMENT '渐伐专属：分年次数',
  grad_interval int DEFAULT NULL COMMENT '渐伐专属：间隔年数',
  select_intensity decimal(5,2) DEFAULT NULL COMMENT '择伐专属：择伐强度(%)',
  select_keep_trees int DEFAULT NULL COMMENT '择伐专属：保留株数',
  tend_method varchar(64) DEFAULT NULL COMMENT '抚育采伐专属：抚育办法',
  tend_per_ha decimal(12,2) DEFAULT NULL COMMENT '抚育采伐专属：每公顷采出量(立方米/公顷)',
  renew_mode varchar(64) DEFAULT NULL COMMENT '更新采伐专属：更新方式',
  renew_finish_year int DEFAULT NULL COMMENT '更新采伐专属：完成年度',
  reform_tree varchar(64) DEFAULT NULL COMMENT '低质低效林改造专属：目标树种',
  register_by varchar(64) DEFAULT NULL COMMENT '立卡人（立卡一刻钉住，重录不挪）',
  register_time datetime DEFAULT NULL COMMENT '立卡时刻',
  current_flag int DEFAULT NULL COMMENT '版本现行标记 1现行 0历史',
  qty decimal(12,2) DEFAULT NULL COMMENT '本卡合并后拟采蓄积(立方米)',
  fine_amt decimal(12,2) DEFAULT NULL COMMENT '本卡新报拟采面积(公顷)',
  content varchar(255) DEFAULT NULL COMMENT '核验串（随版本序走）',
  grade_level int DEFAULT NULL COMMENT '拟采强度取用顺位 1轻度 2中度 3重度',
  status int DEFAULT NULL COMMENT '进展 0待核对 1已核对 2已压死',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者（最近一版录单人）',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  KEY idx_card_site_subcomp (site_no, subcomp_no, current_flag),
  KEY idx_card_bill (bill_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='伐区采伐申请建档卡';

CREATE TABLE IF NOT EXISTS t_flog_close_card (
  id bigint NOT NULL COMMENT '主键',
  biz_no varchar(64) DEFAULT NULL COMMENT '伐区勾销事务卡号',
  stage int DEFAULT NULL COMMENT '当前段次 0..3（报伐/核株/验更/了断）',
  status int DEFAULT NULL COMMENT '卡的落定 0未起 1在办 2已了断',
  content varchar(255) DEFAULT NULL COMMENT '勾销记事',
  last_action varchar(64) DEFAULT NULL COMMENT '最近一次过口动作',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='伐区勾销事务卡';

CREATE TABLE IF NOT EXISTS t_flog_declare_row (
  id bigint NOT NULL COMMENT '主键',
  batch_no varchar(64) DEFAULT NULL COMMENT '伐区申报清册批次号',
  row_no int DEFAULT NULL COMMENT '原清册行次（进门顺序钉死，不许挪册）',
  site_no varchar(64) DEFAULT NULL COMMENT '申报编制单位底档字号',
  subcomp_no varchar(64) DEFAULT NULL COMMENT '小班号',
  cut_mode int DEFAULT NULL COMMENT '采伐方式 1皆伐 2渐伐 3择伐 4抚育采伐 5更新采伐 6低质低效林改造',
  forest_kind int DEFAULT NULL COMMENT '林种性质 1公益林 2商品林',
  dispute_flag int DEFAULT 0 COMMENT '权属是否在诉 0无 1有',
  area decimal(12,2) DEFAULT NULL COMMENT '本行申报拟采面积(公顷)',
  survey_flag int DEFAULT 0 COMMENT '附没附伐区调查设计材料 0未附 1已附',
  item_code varchar(64) DEFAULT NULL COMMENT '被打回的前置项代号',
  qty decimal(12,2) DEFAULT NULL COMMENT '本行申报拟采蓄积(立方米)',
  recv_qty decimal(12,2) DEFAULT NULL COMMENT '本行实际核收蓄积(立方米，不得大过申报)',
  period_year int DEFAULT NULL COMMENT '归属执行期年度（进门那年）',
  page_flag int DEFAULT 0 COMMENT '账页 0现行页 1往期页（收满两个执行期转往期）',
  merge_of_row_no int DEFAULT NULL COMMENT '重复行并账：本行并进的头行行次（头行本格为空）',
  status int DEFAULT NULL COMMENT '行落地情形 0待核 1已收 2挂起',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注（挂起缘故一句话落此）',
  PRIMARY KEY (id),
  KEY idx_declrow_batch_row (batch_no, row_no),
  KEY idx_declrow_batch_site_sub (batch_no, site_no, subcomp_no),
  KEY idx_declrow_page (page_flag, period_year),
  KEY idx_declrow_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='伐区申报清册核收行';

-- 清册批次头：一册一行，回执三数（总行/已收/挂起）在收册同一回计算里落此，
-- 界面不许另点；重送只认头一遍，整册退回/空册各有落法。
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

-- 差异清单：同批次号重送不覆盖旧账，蓄积真要改走这一路；落账后改本行这两格。
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

CREATE TABLE IF NOT EXISTS t_flog_permit_bill (
  id bigint NOT NULL COMMENT '主键',
  bill_no varchar(64) DEFAULT NULL COMMENT '采伐许可证核签单号',
  node_no int DEFAULT NULL COMMENT '当前所在级 0..3',
  sign_mode int DEFAULT NULL COMMENT '同级核签办法 0任一人 1两名点齐',
  need_count int DEFAULT NULL COMMENT '本级应签人数',
  sign_count int DEFAULT NULL COMMENT '本级已签人数',
  status int DEFAULT NULL COMMENT '报批情形 0在核 1已核讫 2已打回',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='采伐许可证逐级核签单';

CREATE TABLE IF NOT EXISTS t_flog_quota_task (
  id bigint NOT NULL COMMENT '主键',
  item_no varchar(64) DEFAULT NULL COMMENT '额度事项号',
  due_at datetime DEFAULT NULL COMMENT '本桩事该动手那天的最后一刻',
  amount decimal(12,2) DEFAULT NULL COMMENT '开口前预先提醒的自然日数',
  content varchar(255) DEFAULT NULL COMMENT '事由与受文林场记要',
  status int DEFAULT NULL COMMENT '条目情形 0未开口 1已落地 2办不下去',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='年采伐限额年度执行额度下达与结余收回事项';

CREATE TABLE IF NOT EXISTS t_flog_stand_doc (
  id bigint NOT NULL COMMENT '主键',
  site_no varchar(64) DEFAULT NULL COMMENT '编制单位在册底档字号',
  site_name varchar(128) DEFAULT NULL COMMENT '编制单位全称',
  site_type varchar(32) DEFAULT NULL COMMENT '经营类别',
  road_name varchar(128) DEFAULT NULL COMMENT '管辖林业站(省—市—县)',
  th1_max decimal(12,2) DEFAULT NULL COMMENT '单卡拟采蓄积轻度线上限(立方米)',
  th2_max decimal(12,2) DEFAULT NULL COMMENT '中度线上限(立方米)',
  th3_max decimal(12,2) DEFAULT NULL COMMENT '重度线上限(立方米)',
  status int DEFAULT NULL COMMENT '底档情形 0在册 1已收回',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='编制单位在册底档';

CREATE TABLE IF NOT EXISTS t_flog_yield_rule (
  id bigint NOT NULL COMMENT '主键',
  rule_code varchar(64) NOT NULL COMMENT '可采强度界限值代号',
  rule_name varchar(128) DEFAULT NULL COMMENT '线名',
  apply_scope int NOT NULL COMMENT '管哪一类林地 2商品林 11公益林·抚育 12公益林·更新 13公益林·低质低效林改造',
  th1_max decimal(12,2) DEFAULT NULL COMMENT '轻度线上限(立方米/公顷)，空着=不设轻档',
  th2_max decimal(12,2) DEFAULT NULL COMMENT '中度线上限(立方米/公顷)，空着=不设中档',
  th3_max decimal(12,2) DEFAULT NULL COMMENT '重度线上限(立方米/公顷)，早年纸质录入的几条原本空着，空=不设重档，随中界往下落',
  eff_start datetime NOT NULL COMMENT '启用之日（含）',
  eff_end datetime DEFAULT NULL COMMENT '交棒之日(不含)；空着=至今未让位',
  priority int NOT NULL DEFAULT 0 COMMENT '抢线顺位(数值越大越先开口)',
  present_flag int NOT NULL DEFAULT 1 COMMENT '在不在场的旗子 1在场 0撤下（撤下的现行求值不取，只留册回查）',
  status int NOT NULL DEFAULT 0 COMMENT '线的情形 0现行 1已让位（让了位的只供回查，现行求值不许再见）',
  del_flag int NOT NULL DEFAULT 0 COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  KEY idx_rule_scope_eff (apply_scope, del_flag, present_flag, status, eff_start, eff_end),
  KEY idx_rule_code (rule_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='可采强度界限值线（界册，每版留痕）';

-- 材积表版本册：二元立木材积式 V = param_a * D^exp_d * H^exp_h。
-- 换版只准添新版、给旧版填交棒日，不许原地改系数；从检尺到蓄积只认同一版（见表注释与服务层）。
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

-- 伐区调查设计单头：一张单子一个小班；取界看 design_date（调查设计那一日），不看今天。
-- 定档结论不并在头上，另起一表逐轮追加（首评/整改复验各留各的代号）。
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

-- 山场检尺逐株账（三级链头一级）：胸径(厘米)、树高(米)与株序原样记账；
-- tree_volume 是同一回三级计算里算出的逐株材积，不许回写取另一版材积表。
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

-- 定档结论行：评定结果连同所依界代号、界启用之日、材积表版一并钉死，钉过不再改口。
-- 整改复验重算一遍即追加一行：两回结论各留各的代号，合成一条不算对。
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

-- 年度额度账：一档一年一行，年初批下本年可占的立方米总数。
CREATE TABLE IF NOT EXISTS t_flog_quota_year (
  id bigint NOT NULL COMMENT '主键',
  site_no varchar(64) NOT NULL COMMENT '单位底档字号',
  quota_year int NOT NULL COMMENT '额度年度',
  total_qty decimal(14,2) NOT NULL DEFAULT 0.00 COMMENT '本年可占总数(立方米，年初批下)',
  occupied_qty decimal(14,2) NOT NULL DEFAULT 0.00 COMMENT '已扣合计(立方米，权威账：立卡/改版/压死随事务锁行改写，不许页面数)',
  status int DEFAULT 0 COMMENT '情形 0执行中 1已收回',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  UNIQUE KEY uk_quota_site_year (site_no, quota_year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='年度采伐额度账';

-- 卡办理流水：办一笔续一笔，办理人记在这里；立卡人另钉在卡上 register_by，两不混。
-- change_type 1立卡(占额) 2改版(占额差额) 3压死(退额) 4核对成形(占额不变)；occupy_qty 为本笔之后该卡占着的额度。
CREATE TABLE IF NOT EXISTS t_flog_card_op (
  id bigint NOT NULL COMMENT '主键',
  card_id bigint NOT NULL COMMENT '建档卡id',
  bill_no varchar(64) DEFAULT NULL COMMENT '建档卡号',
  node_no int DEFAULT NULL COMMENT '办理时所在版本序',
  change_type int DEFAULT NULL COMMENT '办理动作 1立卡 2改版 3压死',
  qty decimal(12,2) DEFAULT NULL COMMENT '本笔涉及蓄积(立方米)',
  occupy_qty decimal(12,2) DEFAULT NULL COMMENT '本笔之后该卡占用额度(立方米)',
  op_by varchar(64) DEFAULT NULL COMMENT '办理人（每笔各记各的）',
  op_time datetime DEFAULT NULL COMMENT '办理时刻',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  KEY idx_op_card (card_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='建档卡办理流水';

-- 账号可见底档授权：本站本辖(grant_type=1)可挂几家；跨县协办(grant_type=2)只把受托那一家摆给受托人。
-- 上级账号（管理员角色）不看此表，全辖尽览；此表无名号即什么都翻不着。
CREATE TABLE IF NOT EXISTS t_flog_site_grant (
  id bigint NOT NULL COMMENT '主键',
  user_id bigint NOT NULL COMMENT '受权账号id',
  site_no varchar(64) NOT NULL COMMENT '可见底档字号',
  grant_type int NOT NULL COMMENT '授权来路 1本站本辖 2跨县受托',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  UNIQUE KEY uk_grant_user_site (user_id, site_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='账号底档可见授权';

-- 初始底档数据（在册、已收回两类各一条）
-- t_flog_stand_doc 三条种子底档：id=0 在册(青屏)、id=1 已收回(白石)、id=2 在册(临县受托样例)。
-- 单卡拟采蓄积三线上限取 40/120/300 立方米（等于上限算高一档）。
INSERT IGNORE INTO t_flog_stand_doc (id, site_no, site_name, site_type, road_name, th1_max, th2_max, th3_max, status, del_flag, create_by, create_time)
VALUES (0, 'DC00', '青屏县编制单位在册底档（青屏国有林场，在册）', '国有林场', '青屏省—青屏市—青屏县', 40.00, 120.00, 300.00, 0, 0, 'seed', NOW()),
       (1, 'DC01', '青屏县编制单位在册底档（白石集体林场，已收回）', '集体林场', '青屏省—青屏市—青屏县', 40.00, 120.00, 300.00, 1, 0, 'seed', NOW()),
       (2, 'DC02', '临溪县编制单位在册底档（临溪个人承包山，跨县受托样例）', '个人承包山', '临溪省—临溪市—临溪县', 40.00, 120.00, 300.00, 0, 0, 'seed', NOW());

-- DC00/DC02 本年额度账种子（本年可占 500 立方米）。
INSERT IGNORE INTO t_flog_quota_year (id, site_no, quota_year, total_qty, status, del_flag, create_by, create_time)
VALUES (0, 'DC00', YEAR(NOW()), 500.00, 0, 0, 'seed', NOW()),
       (1, 'DC02', YEAR(NOW()), 500.00, 0, 0, 'seed', NOW());

-- 账号 fuce(433236479427350528)：本站本辖 DC00；跨县受托只看得见 DC02 一家。DC01 不在册上，翻不着。
INSERT IGNORE INTO t_flog_site_grant (id, user_id, site_no, grant_type, del_flag, create_by, create_time)
VALUES (0, 433236479427350528, 'DC00', 1, 0, 'seed', NOW()),
       (1, 433236479427350528, 'DC02', 2, 0, 'seed', NOW());

-- 界册种子：每版留痕，让位版只供回查。交棒日口径全站一套——旧的管到交棒日前一刻，新的交棒日开口。
-- 商品林(2)：年初一版、入秋换一版；另有一条高序位线与新版同场（序位大的说话）。
INSERT IGNORE INTO t_flog_yield_rule
  (id, rule_code, rule_name, apply_scope, th1_max, th2_max, th3_max, eff_start, eff_end, priority, present_flag, status, del_flag, create_by, create_time, remark)
VALUES
  (920000000000000101, 'GY-2025-A', '商品林年初版', 2, 20.00, 40.00, 80.00,
   '2025-01-01 00:00:00', '2025-09-01 00:00:00', 10, 1, 1, 0, 'seed', NOW(), '入秋换版后让位，只供回查'),
  (920000000000000102, 'GY-2025-B', '商品林入秋版', 2, 25.00, 50.00, 100.00,
   '2025-09-01 00:00:00', NULL, 10, 1, 0, 0, 'seed', NOW(), '现行'),
  (920000000000000103, 'GY-2025-X', '商品林入秋专项高序位线', 2, 22.00, 45.00, 90.00,
   '2025-09-01 00:00:00', NULL, 20, 1, 0, 0, 'seed', NOW(), '与入秋版同场，序位大者说话'),
-- 公益林·抚育(11)：早年纸质设计簿录进来的一条，重界原本空着——空着是不设这一档，随中界往下落。
  (920000000000000111, 'GY-FY-PAPER', '公益林抚育纸质线', 11, 15.00, 30.00, NULL,
   '2024-01-01 00:00:00', NULL, 10, 1, 0, 0, 'seed', NOW(), '纸质录入，重界空缺=不设重档'),
-- 公益林·更新(12)：两条同序位同场，序位分不出高下时认代号排在后头的（GX-B）。
  (920000000000000121, 'GX-A', '公益林更新甲线', 12, 18.00, 36.00, 72.00,
   '2025-01-01 00:00:00', NULL, 10, 1, 0, 0, 'seed', NOW(), '同序位并线'),
  (920000000000000122, 'GX-B', '公益林更新乙线', 12, 16.00, 32.00, 64.00,
   '2025-01-01 00:00:00', NULL, 10, 1, 0, 0, 'seed', NOW(), '同序位并线，代号靠后者说话'),
-- 公益林·低质低效林改造(13)：2025-03-01 才开口；此前在场的一条已撤旗——空当期写明无线可依，不许拿邻近日子凑。
  (920000000000000131, 'GD-OLD', '低改旧线（已撤下）', 13, 10.00, 20.00, 40.00,
   '2024-01-01 00:00:00', NULL, 10, 0, 0, 0, 'seed', NOW(), '撤旗留册，现行求值不取'),
  (920000000000000132, 'GD-2025', '低改现行线', 13, 12.00, 24.00, 48.00,
   '2025-03-01 00:00:00', NULL, 10, 1, 0, 0, 'seed', NOW(), '2025-03-01 前为无线空当');

-- 材积表版本册：年初一版、入秋换一版（V=a*D^b*H^c）。同一份检尺，蓄积与强度评定只许取同一版。
INSERT IGNORE INTO t_flog_volume_table
  (id, table_code, table_name, param_a, exp_d, exp_h, eff_start, eff_end, priority, present_flag, status, del_flag, create_by, create_time, remark)
VALUES
  (920000000000000201, 'VT-2025-A', '二元材积表年初版', 0.0000600000, 1.8000, 1.0000,
   '2025-01-01 00:00:00', '2025-09-01 00:00:00', 10, 1, 1, 0, 'seed', NOW(), '入秋换版后让位'),
  (920000000000000202, 'VT-2025-B', '二元材积表入秋版', 0.0000580000, 1.8200, 1.0000,
   '2025-09-01 00:00:00', NULL, 10, 1, 0, 0, 'seed', NOW(), '现行版');

-- 菜单与按钮权限点（flog 段），挂给管理员(488243256161730560)与用户(488289006124007424)两角色。
INSERT IGNORE INTO t_sys_permission
  (id, name, descripion, url, is_blank, pid, perms, type, icon, order_num, visible, create_by, create_time)
VALUES
  (910000000000000001, '采伐额度账', '伐区采伐建档卡与年度额度', '/FlogApplyCardController/view', 0, 0,
   'flog:flogApplyCard:view', 1, 'fa fa-tree', 90, 0, 'seed', NOW()),
  (910000000000000002, '建档卡集合', '建档卡集合', '/FlogApplyCardController/list', 0, 910000000000000001,
   'flog:flogApplyCard:list', 2, '', 1, 0, 'seed', NOW()),
  (910000000000000003, '建档卡立卡重录', '立卡/重录', '/FlogApplyCardController/add', 0, 910000000000000001,
   'flog:flogApplyCard:add', 2, '', 2, 0, 'seed', NOW()),
  (910000000000000004, '建档卡核对压死', '核对成形/压死/重录', '/FlogApplyCardController/edit', 0, 910000000000000001,
   'flog:flogApplyCard:edit', 2, '', 3, 0, 'seed', NOW()),
  (910000000000000005, '建档卡删除', '逻辑删除', '/FlogApplyCardController/remove', 0, 910000000000000001,
   'flog:flogApplyCard:remove', 2, '', 4, 0, 'seed', NOW()),
  (910000000000000006, '编制单位底档', '在册底档册', '/FlogStandDocController/view', 0, 0,
   'flog:flogStandDoc:view', 1, 'fa fa-book', 91, 0, 'seed', NOW()),
  (910000000000000007, '底档立户收回', '立户/年初收回', '/FlogStandDocController/add', 0, 910000000000000006,
   'flog:flogStandDoc:add', 2, '', 1, 0, 'seed', NOW()),
  (910000000000000008, '年度额度下达', '年初批本年可占', '/FlogQuotaController/view', 0, 0,
   'flog:flogQuota:view', 1, 'fa fa-balance-scale', 92, 0, 'seed', NOW()),
  (910000000000000009, '额度批数', '下达/改批年度总额', '/FlogQuotaController/issue', 0, 910000000000000008,
   'flog:flogQuota:add', 2, '', 1, 0, 'seed', NOW()),
  (910000000000000010, '申报清册核收', '伐区申报清册逐行核收', '/FlogDeclareRowController/view', 0, 0,
   'flog:flogDeclareRow:view', 1, 'fa fa-file-text-o', 93, 0, 'seed', NOW()),
  (910000000000000011, '清册逐行查询', '现行页/挂起/往期', '/FlogDeclareRowController/list', 0, 910000000000000010,
   'flog:flogDeclareRow:list', 2, '', 1, 0, 'seed', NOW()),
  (910000000000000012, '清册收册递行', '整册提交/逐行递入/回执', '/FlogDeclareRowController/submit', 0, 910000000000000010,
   'flog:flogDeclareRow:add', 2, '', 2, 0, 'seed', NOW()),
  (910000000000000013, '往期结转与差异办理', '收满两期转往期/差异清单入账', '/FlogDeclareRowController/turn', 0, 910000000000000010,
   'flog:flogDeclareRow:edit', 2, '', 3, 0, 'seed', NOW()),
  (910000000000000014, '可采强度界册', '界册各版只供翻检', '/FlogYieldRuleController/view', 0, 0,
   'flog:flogYieldRule:view', 1, 'fa fa-ruler', 94, 0, 'seed', NOW()),
  (910000000000000015, '伐区调查设计', '检尺三级链一处算清并钉档', '/FlogSurveyDesignController/view', 0, 0,
   'flog:flogSurveyDesign:view', 1, 'fa fa-file-text', 95, 0, 'seed', NOW()),
  (910000000000000016, '调查设计提交复验', '提交钉档/整改复验重算追加', '/FlogSurveyDesignController/submit', 0, 910000000000000015,
   'flog:flogSurveyDesign:add', 2, '', 1, 0, 'seed', NOW());

-- 两角色都有的：建档卡查看/立卡/核对压死/删除(1..5)、底档册查看(6)。
INSERT IGNORE INTO t_sys_permission_role (id, role_id, permission_id, create_by, create_time)
SELECT 911000000000000000 + r.n + 10 * p.n, r.role_id, p.permission_id, 'seed', NOW()
FROM (
  SELECT 0 AS n, 488243256161730560 AS role_id
  UNION ALL SELECT 1, 488289006124007424
) r
CROSS JOIN (
  SELECT 1 AS n, 910000000000000001 AS permission_id
  UNION ALL SELECT 2, 910000000000000002
  UNION ALL SELECT 3, 910000000000000003
  UNION ALL SELECT 4, 910000000000000004
  UNION ALL SELECT 5, 910000000000000005
  UNION ALL SELECT 6, 910000000000000006
  UNION ALL SELECT 10, 910000000000000010
  UNION ALL SELECT 11, 910000000000000011
  UNION ALL SELECT 12, 910000000000000012
  UNION ALL SELECT 13, 910000000000000013
  UNION ALL SELECT 14, 910000000000000014
  UNION ALL SELECT 15, 910000000000000015
  UNION ALL SELECT 16, 910000000000000016
) p;

-- 仅上级账号（管理员）：底档立户收回(7)、额度下达查看(8)、额度批数(9)。
INSERT IGNORE INTO t_sys_permission_role (id, role_id, permission_id, create_by, create_time)
VALUES
  (912000000000000007, 488243256161730560, 910000000000000007, 'seed', NOW()),
  (912000000000000008, 488243256161730560, 910000000000000008, 'seed', NOW()),
  (912000000000000009, 488243256161730560, 910000000000000009, 'seed', NOW());

