package com.fc.v2.service.flog;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Calendar;
import java.util.List;

import javax.sql.DataSource;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.fc.v2.common.exception.biz.FlogBizException;
import com.fc.v2.mapper.auto.TFlogApplyCardMapper;
import com.fc.v2.mapper.auto.TFlogCardOpMapper;
import com.fc.v2.mapper.auto.TFlogCloseCardMapper;
import com.fc.v2.mapper.auto.TFlogCloseStageMapper;
import com.fc.v2.mapper.auto.TFlogQuotaYearMapper;
import com.fc.v2.mapper.auto.TFlogStandDocMapper;
import com.fc.v2.mapper.auto.TFlogSurveyDesignMapper;
import com.fc.v2.mapper.auto.TFlogSurveyResultMapper;
import com.fc.v2.mapper.auto.TFlogSurveyTreeMapper;
import com.fc.v2.model.auto.TFlogApplyCard;
import com.fc.v2.model.auto.TFlogCloseCard;
import com.fc.v2.model.auto.TFlogCloseStage;
import com.fc.v2.model.auto.TFlogQuotaYear;
import com.fc.v2.model.custom.flog.FlogCloseOpenDto;
import com.fc.v2.model.custom.flog.FlogClosePassDto;
import com.fc.v2.model.custom.flog.FlogCloseRollbackDto;
import com.fc.v2.service.impl.TFlogApplyCardServiceImpl;
import com.fc.v2.service.impl.TFlogCloseCardServiceImpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 勾销事务卡四段流转自测：H2 内存库 + 真实 MyBatis-Plus mapper，
 * 了断退额接真实建档卡压死口。覆盖：
 * 四段挨着走、跳段不放行、同段第二遍只认头一遍、退回沉底旧名目、
 * 止步封死、了断摘册退额、一小班一活卡、卡永不删、
 * 当前段系统照履历算（手摆的 stage 不作数）、履历核验点破缺页/篡改。
 */
class FlogCloseCardServiceTest {

    private DataSource ds;
    private SqlSession session;
    private TFlogCloseCardServiceImpl service;
    private TFlogCloseCardMapper cardMapper;
    private TFlogCloseStageMapper stageMapper;
    private TFlogApplyCardMapper applyMapper;
    private TFlogQuotaYearMapper quotaMapper;

    @BeforeEach
    void setUp() throws Exception {
        org.h2.Driver.load();
        ds = org.h2.jdbcx.JdbcConnectionPool.create(
                "jdbc:h2:mem:closecard;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
            for (String sql : new String[] {
                "DROP TABLE IF EXISTS t_flog_close_stage",
                "DROP TABLE IF EXISTS t_flog_close_card",
                "DROP TABLE IF EXISTS t_flog_card_op",
                "DROP TABLE IF EXISTS t_flog_apply_card",
                "DROP TABLE IF EXISTS t_flog_quota_year",
                "DROP TABLE IF EXISTS t_flog_stand_doc",
                "DROP TABLE IF EXISTS t_flog_survey_tree",
                "DROP TABLE IF EXISTS t_flog_survey_result",
                "DROP TABLE IF EXISTS t_flog_survey_design",
                "CREATE TABLE t_flog_close_card ("
                        + "id bigint PRIMARY KEY, biz_no varchar(64), site_no varchar(64), subcomp_no varchar(64),"
                        + "cut_block_no varchar(64), design_no varchar(64), design_trees int,"
                        + "design_volume decimal(14,4), cut_area decimal(12,2), actual_trees int,"
                        + "actual_volume decimal(14,4), diff_tolerance decimal(8,4), diff_reason varchar(500),"
                        + "renew_area decimal(12,2), survive_rate decimal(6,2), occupy_qty decimal(14,2),"
                        + "quota_year int, apply_card_id bigint, live_key varchar(128), stage int, status int, content varchar(255),"
                        + "last_action varchar(64), del_flag int DEFAULT 0, create_by varchar(64),"
                        + "create_time datetime, update_by varchar(64), update_time datetime, remark varchar(500),"
                        + "CONSTRAINT uk_close_biz UNIQUE(biz_no), CONSTRAINT uk_close_live UNIQUE(live_key))",
                "CREATE TABLE t_flog_close_stage ("
                        + "id bigint PRIMARY KEY, card_id bigint, biz_no varchar(64), stage int, attempt_no int,"
                        + "action varchar(32), active_flag int, design_no varchar(64), actual_trees int,"
                        + "actual_volume decimal(14,4), design_trees int, design_volume decimal(14,4),"
                        + "diff_value decimal(14,4), renew_area decimal(12,2), survive_rate decimal(6,2),"
                        + "refund_qty decimal(14,2), note varchar(500), op_by varchar(64), op_time datetime,"
                        + "del_flag int DEFAULT 0, create_by varchar(64), create_time datetime,"
                        + "update_by varchar(64), update_time datetime)",
                "CREATE TABLE t_flog_survey_design ("
                        + "id bigint PRIMARY KEY, design_no varchar(64), site_no varchar(64),"
                        + "subcomp_no varchar(64), cut_block_no varchar(64), apply_scope int,"
                        + "area decimal(12,2), yield_rate decimal(8,4), survey_by varchar(64),"
                        + "design_date datetime, latest_result_id bigint, del_flag int DEFAULT 0,"
                        + "create_by varchar(64), create_time datetime, update_by varchar(64),"
                        + "update_time datetime, remark varchar(500), CONSTRAINT uk_sd_no UNIQUE(design_no))",
                "CREATE TABLE t_flog_survey_result ("
                        + "id bigint PRIMARY KEY, design_id bigint, round_no int, recheck_flag int,"
                        + "total_volume decimal(14,4), yield_per_ha decimal(14,4), grade_level int,"
                        + "basis_missing int, rule_code varchar(64), rule_eff_start datetime,"
                        + "vol_table_code varchar(64), vol_table_eff_start datetime,"
                        + "pinned_by varchar(64), pinned_time datetime, del_flag int DEFAULT 0,"
                        + "create_by varchar(64), create_time datetime, update_by varchar(64),"
                        + "update_time datetime, remark varchar(500))",
                "CREATE TABLE t_flog_survey_tree ("
                        + "id bigint PRIMARY KEY, design_id bigint, tree_no int, round_no int,"
                        + "dbh decimal(8,2), height decimal(8,2), tree_volume decimal(14,6),"
                        + "del_flag int DEFAULT 0, create_by varchar(64), create_time datetime,"
                        + "update_by varchar(64), update_time datetime)",
                "CREATE TABLE t_flog_stand_doc ("
                        + "id bigint PRIMARY KEY, site_no varchar(64), site_name varchar(128),"
                        + "site_type varchar(32), road_name varchar(128), th1_max decimal(12,2),"
                        + "th2_max decimal(12,2), th3_max decimal(12,2), status int, del_flag int DEFAULT 0,"
                        + "create_by varchar(64), create_time datetime, update_by varchar(64),"
                        + "update_time datetime, remark varchar(500))",
                "CREATE TABLE t_flog_quota_year ("
                        + "id bigint PRIMARY KEY, site_no varchar(64), quota_year int,"
                        + "total_qty decimal(14,2), occupied_qty decimal(14,2), status int,"
                        + "del_flag int DEFAULT 0, create_by varchar(64), create_time datetime,"
                        + "update_by varchar(64), update_time datetime, remark varchar(500),"
                        + "CONSTRAINT uk_qy UNIQUE(site_no, quota_year))",
                "CREATE TABLE t_flog_card_op ("
                        + "id bigint PRIMARY KEY, card_id bigint, bill_no varchar(64), node_no int,"
                        + "change_type int, qty decimal(12,2), occupy_qty decimal(12,2), op_by varchar(64),"
                        + "op_time datetime, del_flag int DEFAULT 0, create_by varchar(64),"
                        + "create_time datetime, update_by varchar(64), update_time datetime, remark varchar(500))",
                "CREATE TABLE t_flog_apply_card ("
                        + "id bigint PRIMARY KEY, bill_no varchar(64), node_no int, site_id bigint,"
                        + "site_no varchar(64), subcomp_no varchar(64), cut_block_no varchar(64), cut_mode int,"
                        + "clear_trees int, grad_years int, grad_interval int, select_intensity decimal(5,2),"
                        + "select_keep_trees int, tend_method varchar(64), tend_per_ha decimal(12,2),"
                        + "renew_mode varchar(64), renew_finish_year int, reform_tree varchar(64),"
                        + "register_by varchar(64), register_time datetime, current_flag int,"
                        + "qty decimal(12,2), fine_amt decimal(12,2), content varchar(255), grade_level int,"
                        + "status int, del_flag int DEFAULT 0, create_by varchar(64), create_time datetime,"
                        + "update_by varchar(64), update_time datetime, remark varchar(500))"
            }) {
                st.execute(sql);
            }
        }
        MybatisConfiguration cfg = new MybatisConfiguration();
        cfg.addMapper(TFlogCloseCardMapper.class);
        cfg.addMapper(TFlogCloseStageMapper.class);
        cfg.addMapper(TFlogSurveyDesignMapper.class);
        cfg.addMapper(TFlogSurveyResultMapper.class);
        cfg.addMapper(TFlogSurveyTreeMapper.class);
        cfg.addMapper(TFlogStandDocMapper.class);
        cfg.addMapper(TFlogQuotaYearMapper.class);
        cfg.addMapper(TFlogCardOpMapper.class);
        cfg.addMapper(TFlogApplyCardMapper.class);
        MybatisSqlSessionFactoryBean fb = new MybatisSqlSessionFactoryBean();
        fb.setDataSource(ds);
        fb.setConfiguration(cfg);
        fb.setGlobalConfig(new GlobalConfig().setDbConfig(new GlobalConfig.DbConfig()));
        SqlSessionFactory sf = fb.getObject();
        session = sf.openSession(true);
        cardMapper = session.getMapper(TFlogCloseCardMapper.class);
        stageMapper = session.getMapper(TFlogCloseStageMapper.class);
        applyMapper = session.getMapper(TFlogApplyCardMapper.class);
        quotaMapper = session.getMapper(TFlogQuotaYearMapper.class);
        TFlogSurveyDesignMapper designMapper = session.getMapper(TFlogSurveyDesignMapper.class);
        TFlogSurveyResultMapper resultMapper = session.getMapper(TFlogSurveyResultMapper.class);
        TFlogSurveyTreeMapper treeMapper = session.getMapper(TFlogSurveyTreeMapper.class);
        TFlogStandDocMapper standDocMapper = session.getMapper(TFlogStandDocMapper.class);
        TFlogCardOpMapper opMapper = session.getMapper(TFlogCardOpMapper.class);

        seed(designMapper, resultMapper, treeMapper, standDocMapper);

        // 真实建档卡服务：了断退额走它的压死口，不另造假账。
        // 它继承 MyBatis-Plus ServiceImpl，直接 new 时 baseMapper 不会被 Spring 注入，反射补上。
        TFlogApplyCardServiceImpl applyService = new TFlogApplyCardServiceImpl();
        setField(applyService, "baseMapper", applyMapper);
        setField(applyService, "flogStandDocMapper", standDocMapper);
        setField(applyService, "quotaYearMapper", quotaMapper);
        setField(applyService, "cardOpMapper", opMapper);
        setField(applyService, "dataScope", passScope());

        service = new TFlogCloseCardServiceImpl();
        setField(service, "closeCardMapper", cardMapper);
        setField(service, "closeStageMapper", stageMapper);
        setField(service, "surveyDesignMapper", designMapper);
        setField(service, "surveyTreeMapper", treeMapper);
        setField(service, "surveyResultMapper", resultMapper);
        setField(service, "applyCardService", applyService);
        // 测试里没有 Shiro 上下文：数据权限放成全通行，只验业务口径
        setField(service, "dataScope", passScope());
    }

    private FlogDataScope passScope() {
        return new FlogDataScope() {
            @Override
            public void assertVisible(Long userId, String siteNo) {
                // 放行
            }

            @Override
            public boolean isSuperior() {
                return true;
            }
        };
    }

    private void seed(TFlogSurveyDesignMapper designMapper,
                      TFlogSurveyResultMapper resultMapper,
                      TFlogSurveyTreeMapper treeMapper,
                      TFlogStandDocMapper standDocMapper) throws Exception {
        int year = Calendar.getInstance().get(Calendar.YEAR);
        try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
            st.execute("INSERT INTO t_flog_stand_doc (id, site_no, site_name, th1_max, th2_max, th3_max, status, del_flag, create_time)"
                    + " VALUES (0, 'DC00', '青屏林场', 40, 120, 300, 0, 0, NOW())");
            st.execute("INSERT INTO t_flog_quota_year (id, site_no, quota_year, total_qty, occupied_qty, status, del_flag, create_time)"
                    + " VALUES (0, 'DC00', " + year + ", 500, 40, 0, 0, NOW())");
            // 建档卡：现行、已核对（非压死），占着 40 立方米（只有 S-1 这片有在册建档卡）
            st.execute("INSERT INTO t_flog_apply_card (id, bill_no, node_no, site_id, site_no, subcomp_no,"
                    + " cut_block_no, cut_mode, current_flag, qty, fine_amt, grade_level, status, del_flag,"
                    + " register_by, register_time, create_time)"
                    + " VALUES (1000, 'FK" + year + "-00000001', 1, 0, 'DC00', 'S-1', '山口一号', 4,"
                    + " 1, 40, 5, 1, 1, 0, 'zhang', NOW(), NOW())");
            // 每个用到的小班一张钉过结论的设计单：100 立方米、10 公顷（株数账不影响本套规则，留空）
            for (int n = 1; n <= 22; n++) {
                String sub = "S-" + n;
                int designId = 100 + n;
                int resultId = 200 + n;
                st.execute("INSERT INTO t_flog_survey_design (id, design_no, site_no, subcomp_no, cut_block_no,"
                        + " apply_scope, area, yield_rate, design_date, latest_result_id, del_flag, create_time)"
                        + " VALUES (" + designId + ", 'SD-" + sub + "', 'DC00', '" + sub + "', '伐区" + sub + "',"
                        + " 2, 10, 0.7, NOW(), " + resultId + ", 0, NOW())");
                st.execute("INSERT INTO t_flog_survey_result (id, design_id, round_no, recheck_flag, total_volume,"
                        + " basis_missing, pinned_by, pinned_time, del_flag)"
                        + " VALUES (" + resultId + ", " + designId + ", 1, 0, 100, 0, 'survey-li', NOW(), 0)");
            }
        }
    }

    @AfterEach
    void tearDown() {
        ((org.h2.jdbcx.JdbcConnectionPool) ds).dispose();
    }

    // ----------------------------------------------------------------
    // 1. 四段挨着走：开卡即报伐，停在核株；三段关口依次过成；了断摘册退额
    // ----------------------------------------------------------------
    @Test
    void testFourStagesInOrderAndCloseRefund() {
        FlogCloseMoveVoTag opened = openCard("S-1", 100, bd("100"));
        assertEquals(1, opened.getStage(), "报伐过成，停在核株段");
        assertEquals(TFlogCloseCard.STATUS_ACTIVE, opened.getStatus());

        // 核株：实采=设计，差在格内，不用说法
        FlogCloseMoveVoTag v1 = advanceCheck(opened.getId(), 100, bd("100"), null);
        assertEquals(2, v1.getStage());

        // 验更：更新面积等于采伐面积（不小即可），成活九成
        FlogCloseMoveVoTag v2 = advanceRenew(opened.getId(), bd("10"), bd("90"));
        assertEquals(3, v2.getStage());

        // 了断：摘建档卡的册，40 立方米原样退回年度账
        FlogClosePassDto closeDto = new FlogClosePassDto();
        closeDto.setId(opened.getId());
        FlogCloseMoveVoTag v3 = new FlogCloseMoveVoTag(service.advance(closeDto));
        assertEquals(TFlogCloseCard.STATUS_TERMINAL, v3.getStatus());
        assertEquals(0, v3.getRefundQty().compareTo(bd("40")), "占额 40 原样退回");
        assertEquals(0, v3.getOccupiedQty().compareTo(bd("0")), "年度账已扣归零");
        assertEquals(0, v3.getRemainQty().compareTo(bd("500")));

        TFlogApplyCard apply = applyMapper.selectById(1000L);
        assertEquals(2, apply.getStatus(), "建档卡随了断压死摘册");
        TFlogQuotaYear q = quotaMapper.selectOne(new QueryWrapper<TFlogQuotaYear>().eq("site_no", "DC00"));
        assertEquals(0, q.getOccupiedQty().compareTo(bd("0")));

        // 卡永不物理删除
        assertNotNull(cardMapper.selectById(opened.getId()));
        // 履历四道 active 过成、段段相连
        List<TFlogCloseStage> hist = service.verifyHistory(opened.getId());
        long activePass = hist.stream().filter(h -> h.getActiveFlag() == 1 && "PASS".equals(h.getAction())).count();
        assertEquals(4, activePass);
    }

    // ----------------------------------------------------------------
    // 2. 跳段不放行：停在核株时报验更的数，那道关口压根不开——当场挡下，更新数不落
    // ----------------------------------------------------------------
    @Test
    void testOnlyCurrentStageGatesOpen() {
        FlogCloseMoveVoTag opened = openCard("S-2", 98, bd("99"));

        FlogClosePassDto rogue = new FlogClosePassDto();
        rogue.setId(opened.getId());
        rogue.setRenewArea(bd("5"));       // 想跳过核株直接验更
        rogue.setSurviveRate(bd("80"));
        FlogBizException ex = assertThrows(FlogBizException.class, () -> service.advance(rogue));
        assertTrue(ex.getMessage().contains("核株段") || ex.getMessage().contains("跳段"));
        TFlogCloseCard card = cardMapper.selectById(opened.getId());
        assertEquals(Integer.valueOf(1), card.getStage(), "没过成，仍停在核株段");
        assertNull(card.getRenewArea(), "验更的数不许越过核株落库");
        assertNull(card.getSurviveRate());
    }

    // ----------------------------------------------------------------
    // 3. 同段第二遍只认头一遍：核株过成后再报，半句记事都不落
    // ----------------------------------------------------------------
    @Test
    void testSecondPassOfSameStageIgnored() {
        FlogCloseMoveVoTag opened = openCard("S-3", 100, bd("100"));
        advanceCheck(opened.getId(), 100, bd("100"), null);
        long rowsAfterFirst = stageMapper.selectCount(new QueryWrapper<TFlogCloseStage>()
                .eq("card_id", opened.getId()));

        FlogBizException ex = assertThrows(FlogBizException.class,
                () -> advanceCheck(opened.getId(), 105, bd("120"), "山里又算了一回"));
        assertTrue(ex.getMessage().contains("头一遍"));

        long rowsAfterSecond = stageMapper.selectCount(new QueryWrapper<TFlogCloseStage>()
                .eq("card_id", opened.getId()));
        assertEquals(rowsAfterFirst, rowsAfterSecond, "第二遍连一笔记事都不许续");
        TFlogCloseCard card = cardMapper.selectById(opened.getId());
        assertEquals(Integer.valueOf(100), card.getActualTrees(), "头一遍的数不被第二遍覆写");
    }

    // ----------------------------------------------------------------
    // 4. 核株差额越过差格：没说法挡下；补一句说法才过
    // ----------------------------------------------------------------
    @Test
    void testDiffOverToleranceNeedsReason() {
        FlogCloseMoveVoTag opened = openCard("S-4", 100, bd("100"));
        FlogBizException ex = assertThrows(FlogBizException.class,
                () -> advanceCheck(opened.getId(), 120, bd("120"), null));
        assertTrue(ex.getMessage().contains("差格"));

        FlogCloseMoveVoTag v = advanceCheck(opened.getId(), 120, bd("120"), "山场边角雪压木多采，已现场拍照");
        assertEquals(2, v.getStage());
        TFlogCloseCard card = cardMapper.selectById(opened.getId());
        assertEquals("山场边角雪压木多采，已现场拍照", card.getDiffReason());
    }

    // ----------------------------------------------------------------
    // 5. 验更：更新面积比采伐面积小，当场点出来
    // ----------------------------------------------------------------
    @Test
    void testRenewAreaSmallerRejected() {
        FlogCloseMoveVoTag opened = openCard("S-5", 100, bd("100"));
        advanceCheck(opened.getId(), 100, bd("100"), null);
        assertThrows(FlogBizException.class,
                () -> advanceRenew(opened.getId(), bd("9.99"), bd("90")));
        TFlogCloseCard card = cardMapper.selectById(opened.getId());
        assertEquals(Integer.valueOf(2), card.getStage(), "没过成就还停在验更段口");
        assertNull(card.getRenewArea());
    }

    // ----------------------------------------------------------------
    // 6. 退回：核株名目尽数沉底，卡头恢复报伐两数；沉底的与后补的不摆一处
    // ----------------------------------------------------------------
    @Test
    void testRollbackSinksStageAndRepass() {
        FlogCloseMoveVoTag opened = openCard("S-6", 100, bd("100"));
        advanceCheck(opened.getId(), 120, bd("120"), "雪压木");
        TFlogCloseCard before = cardMapper.selectById(opened.getId());
        assertEquals(Integer.valueOf(120), before.getActualTrees());

        FlogCloseRollbackDto back = new FlogCloseRollbackDto();
        back.setId(opened.getId());
        back.setReason("核株数抄串了");
        FlogCloseMoveVoTag v = new FlogCloseMoveVoTag(service.rollback(back));
        assertEquals(1, v.getStage(), "退回一格，又停在核株段");

        // 核株还能再往回退一格，落回报伐段（头一段的唯一落点）：旧报伐两数随头一道沉掉
        FlogCloseRollbackDto backDeclare = new FlogCloseRollbackDto();
        backDeclare.setId(opened.getId());
        FlogCloseMoveVoTag vd = new FlogCloseMoveVoTag(service.rollback(backDeclare));
        assertEquals(0, vd.getStage(), "退回报伐段");
        TFlogCloseCard atDeclare = cardMapper.selectById(opened.getId());
        assertNull(atDeclare.getActualTrees(), "旧报伐数沉底清空，等重报");

        // 报伐段是头一道，再往前没有落点；但卡抽不走
        assertThrows(FlogBizException.class, () -> {
            FlogCloseRollbackDto b2 = new FlogCloseRollbackDto();
            b2.setId(opened.getId());
            service.rollback(b2);
        });

        // 重过报伐关口（设计单已钉死，只准重报实采两数），再重走核株
        FlogCloseMoveVoTag repass0 = advanceCheck(opened.getId(), 103, bd("103"), null);
        assertEquals(1, repass0.getStage(), "报伐重做过成，又停在核株段");

        TFlogCloseCard card = cardMapper.selectById(opened.getId());
        assertEquals(Integer.valueOf(103), card.getActualTrees(), "卡头落的是重报的新数");
        assertNull(card.getDiffReason(), "旧核株说法随旧道沉掉");

        List<TFlogCloseStage> hist = stageMapper.selectList(new QueryWrapper<TFlogCloseStage>()
                .eq("card_id", opened.getId()).orderByAsc("id"));
        long sunkPass = hist.stream().filter(h -> h.getStage() == 1 && "PASS".equals(h.getAction())
                && h.getActiveFlag() == 0).count();
        assertEquals(1, sunkPass, "头一道核株过成行沉底");
        long retreatActive = hist.stream().filter(h -> "RETREAT".equals(h.getAction()) && h.getActiveFlag() == 1).count();
        assertEquals(0, retreatActive, "退回行本身不作数，不能数进当前段");

        // 退回后重做：新轮次另起一道，与沉底的不摆在一处
        FlogCloseMoveVoTag again = advanceCheck(opened.getId(), 101, bd("101"), null);
        assertEquals(2, again.getStage());
        TFlogCloseStage activeCheck = stageMapper.selectOne(new QueryWrapper<TFlogCloseStage>()
                .eq("card_id", opened.getId()).eq("stage", 1).eq("active_flag", 1).eq("action", "PASS"));
        assertEquals(Integer.valueOf(2), activeCheck.getAttemptNo(), "后补的是第 2 轮");
        assertEquals(Integer.valueOf(101), activeCheck.getActualTrees());
    }

    // ----------------------------------------------------------------
    // 7. 止步：写下“不过了”即封死，推进、退回都续不进；卡还在
    // ----------------------------------------------------------------
    @Test
    void testHaltSealsCard() {
        FlogCloseMoveVoTag opened = openCard("S-7", 100, bd("100"));
        FlogCloseMoveVoTag h = new FlogCloseMoveVoTag(service.halt(opened.getId(), "权属有诉，先挂着"));
        assertEquals(TFlogCloseCard.STATUS_HALTED, h.getStatus());

        assertThrows(FlogBizException.class,
                () -> advanceCheck(opened.getId(), 100, bd("100"), null));
        FlogCloseRollbackDto back = new FlogCloseRollbackDto();
        back.setId(opened.getId());
        assertThrows(FlogBizException.class, () -> service.rollback(back));
        assertNotNull(cardMapper.selectById(opened.getId()), "止步卡也不许抽走");
    }

    // ----------------------------------------------------------------
    // 8. 一小班一张活卡：在办时第二张立不起来；止步/了断后可另起
    // ----------------------------------------------------------------
    @Test
    void testOneLiveCardPerSubcomp() {
        FlogCloseMoveVoTag first = openCard("S-8", 100, bd("100"));
        assertThrows(FlogBizException.class, () -> openCard("S-8", 100, bd("100")));

        service.halt(first.getId(), "不办了");
        FlogCloseMoveVoTag second = openCard("S-8", 100, bd("100"));
        assertNotNull(second.getId(), "头一张止步后，才许另起第二张");
    }

    // ----------------------------------------------------------------
    // 9. 卡头 stage 是摆示：手改成花脸，系统仍照履历算，照常推进
    // ----------------------------------------------------------------
    @Test
    void testStageComputedFromHistoryNotHeader() throws Exception {
        FlogCloseMoveVoTag opened = openCard("S-9", 100, bd("100"));
        try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
            st.execute("UPDATE t_flog_close_card SET stage=99 WHERE id=" + opened.getId());
        }
        // 屏上摆的、心里认的都不作数：系统照履历知道还停在核株
        FlogCloseMoveVoTag v = advanceCheck(opened.getId(), 100, bd("100"), null);
        assertEquals(2, v.getStage());
    }

    // ----------------------------------------------------------------
    // 10. 履历核验：篡改卡头数、抽掉中间一页，都点破“没走成”
    // ----------------------------------------------------------------
    @Test
    void testVerifyHistoryDetectsTamperAndGap() throws Exception {
        FlogCloseMoveVoTag opened = openCard("S-10", 100, bd("100"));
        advanceCheck(opened.getId(), 100, bd("100"), null);
        advanceRenew(opened.getId(), bd("10"), bd("90"));
        FlogClosePassDto closeDto = new FlogClosePassDto();
        closeDto.setId(opened.getId());
        service.advance(closeDto);
        service.verifyHistory(opened.getId()); // 走到底，核验先得过

        // 了断后改一句卡头数 → 从了断往回一对便破。
        // 手动直改走的是库外通道，先清掉本会话一级缓存，模拟年底另开一屏翻检（生产本就是新会话）。
        try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
            st.execute("UPDATE t_flog_close_card SET actual_volume=999 WHERE id=" + opened.getId());
        }
        session.clearCache();
        assertThrows(FlogBizException.class, () -> service.verifyHistory(opened.getId()));

        // 另开一张走两段的卡，把核株那页手动沉底（缺页）→ 也点破
        FlogCloseMoveVoTag other = openCard("S-11", 100, bd("100"));
        advanceCheck(other.getId(), 100, bd("100"), null);
        try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
            st.execute("UPDATE t_flog_close_stage SET active_flag=0 WHERE card_id=" + other.getId()
                    + " AND stage=1 AND action='PASS'");
        }
        session.clearCache();
        assertThrows(FlogBizException.class, () -> service.verifyHistory(other.getId()));
    }

    // ----------------------------------------------------------------
    // 11. 旧入参旧返回一直可调：advance(id, remark) 与 rollback(id, remark)
    // ----------------------------------------------------------------
    @Test
    void testLegacySignaturesStillWork() {
        // 旧推进口：差额越格时 remark 当说法；旧退出口：退回后把系统重算过段次的卡给回
        FlogCloseMoveVoTag legacy = openCard("S-13", 120, bd("120"));
        TFlogCloseCard moved = service.advance(legacy.getId(), "旧送法给的一句说法");
        assertEquals(Integer.valueOf(2), moved.getStage());
        TFlogCloseCard back = service.rollback(legacy.getId(), "旧送法退回");
        assertEquals(Integer.valueOf(1), back.getStage());
        assertEquals(Integer.valueOf(120), back.getActualTrees(), "恢复报伐数");
    }

    // ----------------------------------------------------------------
    // 12. 报伐关口：设计单不存在、不是同一片小班、缺栏，卡都立不起来
    // ----------------------------------------------------------------
    @Test
    void testOpenGuards() {
        FlogCloseOpenDto noDesign = dto("DC00", "S-20", "SD-NOPE", 100, bd("100"));
        assertThrows(FlogBizException.class, () -> service.open(noDesign));

        FlogCloseOpenDto otherSub = dto("DC00", "S-OTHER", "SD-1", 100, bd("100"));
        assertThrows(FlogBizException.class, () -> service.open(otherSub));

        FlogCloseOpenDto missing = dto("DC00", "S-21", "SD-S-21", null, bd("100"));
        assertThrows(FlogBizException.class, () -> service.open(missing));

        FlogCloseOpenDto nonPositive = dto("DC00", "S-22", "SD-S-22", 100, bd("0"));
        assertThrows(FlogBizException.class, () -> service.open(nonPositive));
    }

    // ---------------------------------------------------------------- helpers

    private FlogCloseMoveVoTag openCard(String sub, Integer trees, BigDecimal vol) {
        return new FlogCloseMoveVoTag(service.open(dto("DC00", sub, "SD-" + sub, trees, vol)));
    }

    private FlogCloseMoveVoTag advanceCheck(Long id, Integer trees, BigDecimal vol, String reason) {
        return new FlogCloseMoveVoTag(service.advance(passCheck(id, trees, vol, reason)));
    }

    private FlogCloseMoveVoTag advanceRenew(Long id, BigDecimal area, BigDecimal rate) {
        FlogClosePassDto d = new FlogClosePassDto();
        d.setId(id);
        d.setRenewArea(area);
        d.setSurviveRate(rate);
        return new FlogCloseMoveVoTag(service.advance(d));
    }

    private FlogClosePassDto passCheck(Long id, Integer trees, BigDecimal vol, String reason) {
        FlogClosePassDto d = new FlogClosePassDto();
        d.setId(id);
        d.setActualTrees(trees);
        d.setActualVolume(vol);
        d.setDiffReason(reason);
        return d;
    }

    private FlogCloseOpenDto dto(String site, String sub, String designNo, Integer trees, BigDecimal vol) {
        FlogCloseOpenDto d = new FlogCloseOpenDto();
        d.setSiteNo(site);
        d.setSubcompNo(sub);
        d.setDesignNo(designNo);
        d.setActualTrees(trees);
        d.setActualVolume(vol);
        return d;
    }

    private BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    private void setField(Object target, String name, Object value) throws Exception {
        Class<?> clazz = target.getClass();
        java.lang.reflect.Field f = null;
        while (clazz != null) {
            try {
                f = clazz.getDeclaredField(name);
                break;
            } catch (NoSuchFieldException ignore) {
                clazz = clazz.getSuperclass();
            }
        }
        if (f == null) {
            throw new NoSuchFieldException(name);
        }
        f.setAccessible(true);
        f.set(target, value);
    }

    /** 断言用薄包装：VO 在主代码里，测试中少打几个全限定名。 */
    private static class FlogCloseMoveVoTag {
        private final com.fc.v2.model.custom.flog.FlogCloseMoveVo vo;

        FlogCloseMoveVoTag(com.fc.v2.model.custom.flog.FlogCloseMoveVo vo) {
            this.vo = vo;
        }

        Long getId() {
            return vo.getId();
        }

        int getStage() {
            return vo.getStage();
        }

        int getStatus() {
            return vo.getStatus();
        }

        BigDecimal getRefundQty() {
            return vo.getRefundQty();
        }

        BigDecimal getOccupiedQty() {
            return vo.getOccupiedQty();
        }

        BigDecimal getRemainQty() {
            return vo.getRemainQty();
        }
    }
}
