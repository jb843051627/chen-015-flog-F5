package com.fc.v2.service.flog;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.sql.DataSource;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.fc.v2.common.exception.biz.FlogBizException;
import com.fc.v2.mapper.auto.TFlogSurveyDesignMapper;
import com.fc.v2.mapper.auto.TFlogSurveyResultMapper;
import com.fc.v2.mapper.auto.TFlogSurveyTreeMapper;
import com.fc.v2.mapper.auto.TFlogVolumeTableMapper;
import com.fc.v2.mapper.auto.TFlogYieldRuleMapper;
import com.fc.v2.model.auto.TFlogSurveyDesign;
import com.fc.v2.model.auto.TFlogSurveyResult;
import com.fc.v2.model.auto.TFlogSurveyTree;
import com.fc.v2.model.auto.TFlogVolumeTable;
import com.fc.v2.model.auto.TFlogYieldRule;
import com.fc.v2.model.custom.flog.FlogSurveyCalcDto;
import com.fc.v2.model.custom.flog.FlogSurveyCalcVo;
import com.fc.v2.model.custom.flog.FlogSurveyDesignDto;
import com.fc.v2.model.custom.flog.FlogSurveyRecheckDto;
import com.fc.v2.model.custom.flog.FlogSurveySubmitVo;
import com.fc.v2.service.impl.TFlogSurveyDesignServiceImpl;
import com.fc.v2.service.impl.TFlogYieldRuleServiceImpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 界册并版 + 检尺三级链规则自测：H2 内存库 + 真实 MyBatis-Plus mapper。
 * 覆盖：设计日取界（不看今天）、交棒日左含右不含、让位/撤旗不取、序位并线、
 * 代号决胜、林地类别隔离、挨着界就高、重界空缺随中落、无线可依不凑邻近、
 * 非法检尺退回、钉档不改口、复验追加各留代号、同一回计算一致。
 */
class FlogSurveyEvalServiceTest {

    private DataSource ds;
    private TFlogYieldRuleServiceImpl ruleService;
    private TFlogSurveyDesignServiceImpl designService;
    private TFlogYieldRuleMapper ruleMapper;
    private TFlogVolumeTableMapper tableMapper;
    private TFlogSurveyDesignMapper designMapper;
    private TFlogSurveyTreeMapper treeMapper;
    private TFlogSurveyResultMapper resultMapper;

    private static final int SCOPE_GOODS = 2;
    private static final int SCOPE_TEND = 11;
    private static final int SCOPE_RENEW = 12;
    private static final int SCOPE_REFORM = 13;

    @BeforeEach
    void setUp() throws Exception {
        org.h2.Driver.load();
        ds = org.h2.jdbcx.JdbcConnectionPool.create(
                "jdbc:h2:mem:survey;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
            for (String sql : new String[] {
                "DROP TABLE IF EXISTS t_flog_yield_rule",
                "DROP TABLE IF EXISTS t_flog_volume_table",
                "DROP TABLE IF EXISTS t_flog_survey_design",
                "DROP TABLE IF EXISTS t_flog_survey_tree",
                "DROP TABLE IF EXISTS t_flog_survey_result",
                "CREATE TABLE t_flog_yield_rule ("
                        + "id bigint PRIMARY KEY, rule_code varchar(64), rule_name varchar(128), apply_scope int,"
                        + "th1_max decimal(12,2), th2_max decimal(12,2), th3_max decimal(12,2),"
                        + "eff_start datetime, eff_end datetime, priority int, present_flag int, status int,"
                        + "del_flag int DEFAULT 0, create_by varchar(64), create_time datetime,"
                        + "update_by varchar(64), update_time datetime, remark varchar(500))",
                "CREATE TABLE t_flog_volume_table ("
                        + "id bigint PRIMARY KEY, table_code varchar(64), table_name varchar(128),"
                        + "param_a decimal(20,10), exp_d decimal(8,4), exp_h decimal(8,4),"
                        + "eff_start datetime, eff_end datetime, priority int, present_flag int, status int,"
                        + "del_flag int DEFAULT 0, create_by varchar(64), create_time datetime,"
                        + "update_by varchar(64), update_time datetime, remark varchar(500))",
                "CREATE TABLE t_flog_survey_design ("
                        + "id bigint PRIMARY KEY, design_no varchar(64), site_no varchar(64),"
                        + "subcomp_no varchar(64), cut_block_no varchar(64), apply_scope int,"
                        + "area decimal(12,2), yield_rate decimal(8,4), survey_by varchar(64),"
                        + "design_date datetime, latest_result_id bigint, del_flag int DEFAULT 0,"
                        + "create_by varchar(64), create_time datetime, update_by varchar(64),"
                        + "update_time datetime, remark varchar(500), CONSTRAINT uk_sd_no UNIQUE(design_no))",
                "CREATE TABLE t_flog_survey_tree ("
                        + "id bigint PRIMARY KEY, design_id bigint, tree_no int, round_no int,"
                        + "dbh decimal(8,2), height decimal(8,2), tree_volume decimal(14,6),"
                        + "del_flag int DEFAULT 0, create_by varchar(64), create_time datetime,"
                        + "update_by varchar(64), update_time datetime)",
                "CREATE TABLE t_flog_survey_result ("
                        + "id bigint PRIMARY KEY, design_id bigint, round_no int, recheck_flag int,"
                        + "total_volume decimal(14,4), yield_per_ha decimal(14,4), grade_level int,"
                        + "basis_missing int, rule_code varchar(64), rule_eff_start datetime,"
                        + "vol_table_code varchar(64), vol_table_eff_start datetime,"
                        + "pinned_by varchar(64), pinned_time datetime, del_flag int DEFAULT 0,"
                        + "create_by varchar(64), create_time datetime, update_by varchar(64),"
                        + "update_time datetime, remark varchar(500))"
            }) {
                st.execute(sql);
            }
        }
        MybatisConfiguration cfg = new MybatisConfiguration();
        cfg.addMapper(TFlogYieldRuleMapper.class);
        cfg.addMapper(TFlogVolumeTableMapper.class);
        cfg.addMapper(TFlogSurveyDesignMapper.class);
        cfg.addMapper(TFlogSurveyTreeMapper.class);
        cfg.addMapper(TFlogSurveyResultMapper.class);
        MybatisSqlSessionFactoryBean fb = new MybatisSqlSessionFactoryBean();
        fb.setDataSource(ds);
        fb.setConfiguration(cfg);
        fb.setGlobalConfig(new GlobalConfig().setDbConfig(new GlobalConfig.DbConfig()));
        SqlSessionFactory sf = fb.getObject();
        SqlSession session = sf.openSession(true);
        ruleMapper = session.getMapper(TFlogYieldRuleMapper.class);
        tableMapper = session.getMapper(TFlogVolumeTableMapper.class);
        designMapper = session.getMapper(TFlogSurveyDesignMapper.class);
        treeMapper = session.getMapper(TFlogSurveyTreeMapper.class);
        resultMapper = session.getMapper(TFlogSurveyResultMapper.class);

        seedRules();
        seedTables();

        ruleService = new TFlogYieldRuleServiceImpl();
        setField(ruleService, "flogYieldRuleMapper", ruleMapper);
        setField(ruleService, "flogVolumeTableMapper", tableMapper);

        designService = new TFlogSurveyDesignServiceImpl();
        setField(designService, "designMapper", designMapper);
        setField(designService, "treeMapper", treeMapper);
        setField(designService, "resultMapper", resultMapper);
        setField(designService, "yieldRuleService", ruleService);
        // 测试里没有 Shiro 上下文：数据权限放成全通行，只验业务口径
        setField(designService, "dataScope", new FlogDataScope() {
            @Override
            public void assertVisible(Long userId, String siteNo) {
                // 放行
            }

            @Override
            public boolean isSuperior() {
                return true;
            }
        });
    }

    private void seedRules() {
        // 商品林：年初版（交棒 2025-09-01）、入秋版、入秋高序位线
        rule("GY-2025-A", "商品林年初版", SCOPE_GOODS, "2025-01-01 00:00:00", "2025-09-01 00:00:00",
                10, 1, 1, bd("20"), bd("40"), bd("80"));
        rule("GY-2025-B", "商品林入秋版", SCOPE_GOODS, "2025-09-01 00:00:00", null,
                10, 1, 0, bd("25"), bd("50"), bd("100"));
        rule("GY-2025-X", "商品林入秋高序位线", SCOPE_GOODS, "2025-09-01 00:00:00", null,
                20, 1, 0, bd("22"), bd("45"), bd("90"));
        // 公益林抚育：纸质线，重界空着
        rule("GY-FY-PAPER", "公益林抚育纸质线", SCOPE_TEND, "2024-01-01 00:00:00", null,
                10, 1, 0, bd("15"), bd("30"), null);
        // 公益林更新：同序位两条，代号靠后者说话
        rule("GX-A", "公益林更新甲线", SCOPE_RENEW, "2025-01-01 00:00:00", null,
                10, 1, 0, bd("18"), bd("36"), bd("72"));
        rule("GX-B", "公益林更新乙线", SCOPE_RENEW, "2025-01-01 00:00:00", null,
                10, 1, 0, bd("16"), bd("32"), bd("64"));
        // 低改：旧线撤旗，2025-03-01 前空当
        rule("GD-OLD", "低改旧线（已撤下）", SCOPE_REFORM, "2024-01-01 00:00:00", null,
                10, 0, 0, bd("10"), bd("20"), bd("40"));
        rule("GD-2025", "低改现行线", SCOPE_REFORM, "2025-03-01 00:00:00", null,
                10, 1, 0, bd("12"), bd("24"), bd("48"));
    }

    private void seedTables() {
        // V = a * D^b * H^c：年初版 0.0001 D^2 H，入秋版 0.0002 D^2 H（同株两版材积差一倍）
        volumeTable("VT-2025-A", "年初版", bd("0.0001"), bd("2"), bd("1"),
                "2025-01-01 00:00:00", "2025-09-01 00:00:00", 10);
        volumeTable("VT-2025-B", "入秋版", bd("0.0002"), bd("2"), bd("1"),
                "2025-09-01 00:00:00", null, 10);
    }

    private long ruleSeq = 1;
    private long tableSeq = 1;

    private void rule(String code, String name, int scope, String start, String end,
                      int priority, int present, int status,
                      BigDecimal th1, BigDecimal th2, BigDecimal th3) {
        TFlogYieldRule r = new TFlogYieldRule();
        r.setId(1000L + ruleSeq++);
        r.setRuleCode(code);
        r.setRuleName(name);
        r.setApplyScope(scope);
        r.setTh1Max(th1);
        r.setTh2Max(th2);
        r.setTh3Max(th3);
        r.setEffStart(d(start));
        r.setEffEnd(end == null ? null : d(end));
        r.setPriority(priority);
        r.setPresentFlag(present);
        r.setStatus(status);
        r.setDelFlag(0);
        ruleMapper.insert(r);
    }

    private void volumeTable(String code, String name, BigDecimal a, BigDecimal b, BigDecimal c,
                             String start, String end, int priority) {
        TFlogVolumeTable t = new TFlogVolumeTable();
        t.setId(2000L + tableSeq++);
        t.setTableCode(code);
        t.setTableName(name);
        t.setParamA(a);
        t.setExpD(b);
        t.setExpH(c);
        t.setEffStart(d(start));
        t.setEffEnd(end == null ? null : d(end));
        t.setPriority(priority);
        t.setPresentFlag(1);
        t.setStatus(end == null ? 0 : 1);
        t.setDelFlag(0);
        tableMapper.insert(t);
    }

    @AfterEach
    void tearDown() {
        ((org.h2.jdbcx.JdbcConnectionPool) ds).dispose();
    }

    // ---- 取界口径 ----------------------------------------------------

    /** 1. 看调查设计那一日不看今天：去年秋设计、今年补结论，请回的是年初版。交棒日不含当日。 */
    @Test
    void testPickByDesignDateAndHandoffBoundary() {
        // 交棒前最后一刻：旧版
        FlogSurveyCalcVo before = evaluate(SCOPE_GOODS, "2025-08-31 23:59:59", "0.01", "1",
                tree("10", "10"));
        assertEquals("GY-2025-A", before.getRuleCode());
        assertEquals("VT-2025-A", before.getVolTableCode());
        // 旧版 V=0.0001*100*10=0.1；出材量 0.1/0.01=10，落轻档
        assertEquals(0, before.getTotalVolume().compareTo(bd("0.1")));
        assertEquals(0, before.getYieldPerHa().compareTo(bd("10")));
        assertEquals(1, before.getGradeLevel().intValue());

        // 交棒日零点开口：新版 + 高序位线
        FlogSurveyCalcVo on = evaluate(SCOPE_GOODS, "2025-09-01 00:00:00", "0.01", "1",
                tree("10", "10"));
        assertEquals("GY-2025-X", on.getRuleCode(), "同场序位 20 压过 10");
        assertEquals("VT-2025-B", on.getVolTableCode());
        // 新版 V=0.0002*100*10=0.2；出材量 20，低于 X 的轻界 22，落轻
        assertEquals(0, on.getYieldPerHa().compareTo(bd("20")));
        assertEquals(1, on.getGradeLevel().intValue());

        // 今年开春补去年 8 月的设计结论：仍是旧版，绝不取入秋后新版
        FlogSurveyCalcVo retro = evaluate(SCOPE_GOODS, "2025-08-20 09:00:00", "0.01", "1",
                tree("10", "10"));
        assertEquals("GY-2025-A", retro.getRuleCode());
        assertEquals("VT-2025-A", retro.getVolTableCode());
    }

    /** 2. 序位分不出高下，认代号排在后头那条。 */
    @Test
    void testTieBreakByCodeDesc() {
        FlogSurveyCalcVo v = evaluate(SCOPE_RENEW, "2025-06-01 00:00:00", "0.01", "1",
                tree("10", "10"));
        assertEquals("GX-B", v.getRuleCode());
        // 乙线轻界 16：出材量 10 落轻
        assertEquals(1, v.getGradeLevel().intValue());
    }

    /** 3. 林地类别各管各的，借不到别类的界；空当写明无线可依，不拿邻近日子凑。 */
    @Test
    void testScopeIsolationAndGapNoBasis() {
        // 低改 2025-02 月：旧线撤了旗、新线未开口——无线可依
        FlogSurveyCalcVo gap = evaluate(SCOPE_REFORM, "2025-02-15 00:00:00", "0.01", "1",
                tree("10", "10"));
        assertTrue(gap.isBasisMissing());
        assertNull(gap.getGradeLevel());
        assertNull(gap.getRuleCode());
        assertTrue(gap.getMessage().contains("无线可依"));
        // 材积表在场，蓄积仍算出
        assertEquals("VT-2025-A", gap.getVolTableCode());
        assertNotNull(gap.getTotalVolume());

        // 低改 2025-03-01 开口当日：新线在场
        FlogSurveyCalcVo open = evaluate(SCOPE_REFORM, "2025-03-01 00:00:00", "0.01", "1",
                tree("10", "10"));
        assertFalse(open.isBasisMissing());
        assertEquals("GD-2025", open.getRuleCode());

        // 商品林 2024 年一条界都没有（不许把 2025 年初版拿来凑）
        FlogSurveyCalcVo none = evaluate(SCOPE_GOODS, "2024-12-31 23:59:59", "0.01", "1",
                tree("10", "10"));
        assertTrue(none.isBasisMissing());
        assertNull(none.getRuleCode());
    }

    /** 4. 挨着界往高里数：与中界一样高落重，越过重界仍重。 */
    @Test
    void testGradeAgainstUpperBound() {
        // 高序位线 X：22/45/90，入秋后表 B。出材量 = 0.0002*D^2*10/0.01 = 0.2*D^2
        assertEquals(1, gradeAt(SCOPE_GOODS, "2025-09-01 00:00:00", "10"));  // 20 < 22 轻
        assertEquals(2, gradeAt(SCOPE_GOODS, "2025-09-01 00:00:00", "12"));  // 28.8 中
        assertEquals(3, gradeAt(SCOPE_GOODS, "2025-09-01 00:00:00", "15"));  // 45 = 中界 → 重
        assertEquals(3, gradeAt(SCOPE_GOODS, "2025-09-01 00:00:00", "25"));  // 125 > 90 → 仍重
    }

    /** 5. 纸质线重界空着=不设重档：到中界及以上随中界落中档。 */
    @Test
    void testMissingHeavyFallsToMedium() {
        // 抚育纸质线 15/30/null；年初表 A：出材量=0.1*D^2（area 0.01）
        assertEquals(1, gradeAt(SCOPE_TEND, "2025-06-01 00:00:00", "10"));   // 10 轻
        assertEquals(2, gradeAt(SCOPE_TEND, "2025-06-01 00:00:00", "20"));   // 40，越过中界仍中档
        FlogSurveyCalcVo eq = evaluate(SCOPE_TEND, "2025-06-01 00:00:00", "0.01", "1", tree("20", "10"));
        assertEquals(2, eq.getGradeLevel().intValue());
        assertNull(ruleService.pickRule(SCOPE_TEND, d("2025-06-01 00:00:00")).getTh3Max());
    }

    /** 6. 材积表版也翻不出时，界与表两路缺失都点明，仍不评定。 */
    @Test
    void testVolumeTableMissingAlsoNoBasis() {
        // 抚育线 2024 年已在场，但材积表 2025 年才开口
        FlogSurveyCalcVo v = evaluate(SCOPE_TEND, "2024-06-01 00:00:00", "0.01", "1",
                tree("10", "10"));
        assertTrue(v.isBasisMissing());
        assertNull(v.getVolTableCode());
        assertNull(v.getTotalVolume());
        assertTrue(v.getMessage().contains("材积表"));
    }

    /** 7. 非法检尺数不进链，逐一点名退回。 */
    @Test
    void testInvalidMeasuresRejected() {
        assertBad(SCOPE_GOODS, "2025-09-01 00:00:00", "0.01", "1", null, "株数为零");
        assertBad(SCOPE_GOODS, "2025-09-01 00:00:00", "0.01", "1",
                java.util.Collections.singletonList(tree("-1", "10")), "胸径");
        assertBad(SCOPE_GOODS, "2025-09-01 00:00:00", "0.01", "1",
                java.util.Collections.singletonList(tree("10", "-2")), "树高");
        assertBad(SCOPE_GOODS, "2025-09-01 00:00:00", "0.01", "1",
                java.util.Collections.singletonList(tree("9999", "10")), "四位九");
        assertBad(SCOPE_GOODS, "2025-09-01 00:00:00", "0", "1",
                java.util.Collections.singletonList(tree("10", "10")), "面积");
        assertBad(SCOPE_GOODS, "2025-09-01 00:00:00", "0.01", "1.2",
                java.util.Collections.singletonList(tree("10", "10")), "出材率");
        assertBad(99, "2025-09-01 00:00:00", "0.01", "1",
                java.util.Collections.singletonList(tree("10", "10")), "林地类别");
    }

    // ---- 钉档与复验 --------------------------------------------------

    /** 8. 提交即钉：档位、界代号、界启用日、材积表版压在结论上，钉过不改口。 */
    @Test
    void testSubmitPinsResult() throws Exception {
        FlogSurveyDesignDto dto = designDto("SD-1", SCOPE_GOODS, "2025-08-15 00:00:00", tree("10", "10"));
        FlogSurveySubmitVo vo = designService.submit(dto);
        assertEquals(1, vo.getRoundNo().intValue());
        assertEquals(0, vo.getRecheckFlag().intValue());
        assertEquals("GY-2025-A", vo.getRuleCode());
        assertEquals(1, vo.getGradeLevel().intValue());

        List<TFlogSurveyResult> rows = designService.listResults(vo.getDesignId());
        assertEquals(1, rows.size());
        TFlogSurveyResult pinned = rows.get(0);
        assertEquals("GY-2025-A", pinned.getRuleCode());
        assertEquals("VT-2025-A", pinned.getVolTableCode());
        assertEquals(d("2025-01-01 00:00:00"), pinned.getRuleEffStart());
        assertEquals(1, pinned.getGradeLevel().intValue());
        assertEquals(0, pinned.getBasisMissing().intValue());

        // 库里结论行不随后续界面动作改口：内存对象怎么改，库里那行仍是钉下的 1
        pinned.setGradeLevel(3);
        try (Connection c = ds.getConnection(); Statement st = c.createStatement();
                java.sql.ResultSet rs = st.executeQuery(
                        "SELECT grade_level FROM t_flog_survey_result WHERE design_id=" + vo.getDesignId())) {
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1));
        }
    }

    /** 9. 整改复验重算追加一行：两回各留各的代号，头一行原样，不并条。 */
    @Test
    void testRecheckAppendsSeparateResult() {
        FlogSurveySubmitVo first = designService.submit(
                designDto("SD-2", SCOPE_GOODS, "2025-09-01 00:00:00", tree("10", "10"))); // 出材 20 轻
        assertEquals(1, first.getGradeLevel().intValue());

        // 整改后重新检尺：单株 D=15 → 出材 45 = 中界 → 重
        FlogSurveyRecheckDto rc = new FlogSurveyRecheckDto();
        rc.setDesignId(first.getDesignId());
        rc.setTrees(java.util.Collections.singletonList(tree("15", "10")));
        FlogSurveySubmitVo second = designService.recheck(rc);
        assertEquals(2, second.getRoundNo().intValue());
        assertEquals(1, second.getRecheckFlag().intValue());
        assertEquals(3, second.getGradeLevel().intValue());
        assertEquals("GY-2025-X", second.getRuleCode());

        List<TFlogSurveyResult> rows = designService.listResults(first.getDesignId());
        assertEquals(2, rows.size());
        assertEquals(1, rows.get(0).getRoundNo().intValue());
        assertEquals(1, rows.get(0).getGradeLevel().intValue());
        assertEquals("GY-2025-X", rows.get(0).getRuleCode());
        assertEquals(2, rows.get(1).getRoundNo().intValue());
        assertEquals(3, rows.get(1).getGradeLevel().intValue());
        // 各留各的代号字段，不靠拼
        assertNotNull(rows.get(0).getRuleCode());
        assertNotNull(rows.get(1).getRuleCode());

        // 两轮检尺账各记各的，逐株材积与各自表版同回算出
        List<TFlogSurveyTree> t1 = designService.listTrees(first.getDesignId(), 1);
        List<TFlogSurveyTree> t2 = designService.listTrees(first.getDesignId(), 2);
        assertEquals(1, t1.size());
        assertEquals(1, t2.size());
        assertEquals(0, t1.get(0).getTreeVolume().compareTo(bd("0.2")));
        assertEquals(0, t2.get(0).getTreeVolume().compareTo(bd("0.45")));
    }

    /** 10. 非法检尺连单都不钉；复验时非法，旧结论一行不添。 */
    @Test
    void testInvalidDoesNotPin() {
        FlogSurveyDesignDto bad = designDto("SD-BAD", SCOPE_GOODS, "2025-09-01 00:00:00",
                tree("9999", "10"));
        assertThrows(FlogBizException.class, () -> designService.submit(bad));
        assertEquals(0, designMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<TFlogSurveyDesign>()
                        .eq("design_no", "SD-BAD")).intValue());

        FlogSurveySubmitVo ok = designService.submit(
                designDto("SD-3", SCOPE_GOODS, "2025-09-01 00:00:00", tree("10", "10")));
        int beforeResults = designService.listResults(ok.getDesignId()).size();
        FlogSurveyRecheckDto rc = new FlogSurveyRecheckDto();
        rc.setDesignId(ok.getDesignId());
        rc.setTrees(java.util.Collections.singletonList(tree("-5", "10")));
        assertThrows(FlogBizException.class, () -> designService.recheck(rc));
        assertEquals(beforeResults, designService.listResults(ok.getDesignId()).size());
    }

    /** 11. 同号不许立两张。 */
    @Test
    void testDuplicateDesignNo() {
        designService.submit(designDto("SD-DUP", SCOPE_GOODS, "2025-09-01 00:00:00", tree("10", "10")));
        assertThrows(FlogBizException.class, () -> designService
                .submit(designDto("SD-DUP", SCOPE_GOODS, "2025-09-01 00:00:00", tree("10", "10"))));
    }

    // ---- 旧调用口照旧可用 --------------------------------------------

    /** 12. 按代号取线的旧方法签名不变：在场给档，让位/撤下给 0。 */
    @Test
    void testLegacyEvaluateByCode() {
        assertEquals(2, ruleService.evaluate("GY-2025-A", bd("20"), d("2025-08-31 00:00:00")));
        assertEquals(1, ruleService.evaluate("GY-2025-A", bd("19.99"), d("2025-08-31 00:00:00")));
        // 入秋后该代号已让位
        assertEquals(0, ruleService.evaluate("GY-2025-A", bd("20"), d("2025-09-01 00:00:00")));
        // 撤旗的线取不出
        assertEquals(0, ruleService.evaluate("GD-OLD", bd("5"), d("2025-06-01 00:00:00")));
        // 越界入参
        assertEquals(0, ruleService.evaluate("GY-2025-A", bd("-1"), d("2025-08-31 00:00:00")));

        assertEquals(3, ruleService.evaluateTop(bd("45"), SCOPE_GOODS, d("2025-09-01 00:00:00")));
        assertEquals(0, ruleService.evaluateTop(bd("45"), SCOPE_GOODS, d("2024-01-01 00:00:00")));

        assertTrue(ruleService.usable(ruleId("GY-2025-A"), d("2025-08-31 23:59:59")));
        assertFalse(ruleService.usable(ruleId("GY-2025-A"), d("2025-09-01 00:00:00")));
        assertEquals(2, ruleService.listAvailable(SCOPE_GOODS, d("2025-09-01 00:00:00")).size());
        // 同序位两条都在场，count 的是线数（2）；定档只取代号靠后那条 GX-B
        assertEquals(2, ruleService.countAvailable(SCOPE_RENEW, d("2025-06-01 00:00:00")));
        assertEquals("GX-B", ruleService.pickRule(SCOPE_RENEW, d("2025-06-01 00:00:00")).getRuleCode());
    }

    // ---- 辅助 --------------------------------------------------------

    private int gradeAt(int scope, String at, String dbh) {
        return evaluate(scope, at, "0.01", "1", tree(dbh, "10")).getGradeLevel().intValue();
    }

    private void assertBad(int scope, String at, String area, String rate,
                           List<FlogSurveyCalcDto.TreeMeasure> trees, String hint) {
        FlogSurveyCalcDto dto = new FlogSurveyCalcDto();
        dto.setApplyScope(scope);
        dto.setDesignDate(d(at));
        dto.setArea(area == null ? null : bd(area));
        dto.setYieldRate(rate == null ? null : bd(rate));
        dto.setTrees(trees);
        FlogBizException ex = assertThrows(FlogBizException.class, () -> ruleService.evaluate(dto));
        assertTrue(ex.getMessage().contains(hint), "应点明[" + hint + "]，实际：" + ex.getMessage());    }

    private FlogSurveyCalcVo evaluate(int scope, String at, String area, String rate,
                                      FlogSurveyCalcDto.TreeMeasure... trees) {
        FlogSurveyCalcDto dto = new FlogSurveyCalcDto();
        dto.setApplyScope(scope);
        dto.setDesignDate(d(at));
        dto.setArea(bd(area));
        dto.setYieldRate(bd(rate));
        dto.setTrees(java.util.Arrays.asList(trees));
        return ruleService.evaluate(dto);
    }

    private FlogSurveyDesignDto designDto(String no, int scope, String at,
                                          FlogSurveyCalcDto.TreeMeasure... trees) {
        FlogSurveyDesignDto dto = new FlogSurveyDesignDto();
        dto.setDesignNo(no);
        dto.setSiteNo("DC00");
        dto.setSubcompNo("S-" + no);
        dto.setCutBlockNo("伐区-" + no);
        dto.setApplyScope(scope);
        dto.setArea(bd("0.01"));
        dto.setYieldRate(bd("1"));
        dto.setSurveyBy("调查员甲");
        dto.setDesignDate(d(at));
        List<FlogSurveyCalcDto.TreeMeasure> list = new ArrayList<>();
        for (FlogSurveyCalcDto.TreeMeasure t : trees) {
            list.add(t);
        }
        dto.setTrees(list);
        return dto;
    }

    private FlogSurveyCalcDto.TreeMeasure tree(String dbh, String height) {
        return new FlogSurveyCalcDto.TreeMeasure(bd(dbh), bd(height));
    }

    private Long ruleId(String code) {
        TFlogYieldRule r = ruleMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<TFlogYieldRule>()
                        .eq("rule_code", code).last("limit 1"));
        return r.getId();
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        java.lang.reflect.Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    private static Date d(String s) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(s);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }
}
