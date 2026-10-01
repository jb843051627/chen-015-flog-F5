package com.fc.v2.service.flog;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
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
import com.fc.v2.mapper.auto.TFlogDeclareBatchMapper;
import com.fc.v2.mapper.auto.TFlogDeclareDiffMapper;
import com.fc.v2.mapper.auto.TFlogDeclareRowMapper;
import com.fc.v2.model.auto.TFlogDeclareBatch;
import com.fc.v2.model.auto.TFlogDeclareDiff;
import com.fc.v2.model.auto.TFlogDeclareRow;
import com.fc.v2.model.custom.flog.FlogDeclareRowDto;
import com.fc.v2.model.custom.flog.FlogReceiptVo;
import com.fc.v2.model.custom.flog.FlogRowResultVo;
import com.fc.v2.service.impl.TFlogDeclareRowServiceImpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 清册逐行核收规则自测：H2 内存库 + 真实 MyBatis-Plus mapper，服务一条道一行行走。
 */
class FlogDeclareRowServiceTest {

    private DataSource ds;
    private TFlogDeclareRowServiceImpl service;
    private TFlogDeclareRowMapper rowMapper;
    private TFlogDeclareBatchMapper batchMapper;

    @BeforeEach
    void setUp() throws Exception {
        org.h2.Driver.load();
        ds = org.h2.jdbcx.JdbcConnectionPool.create(
                "jdbc:h2:mem:declare;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
            for (String sql : new String[] {
                "DROP TABLE IF EXISTS t_flog_declare_row",
                "DROP TABLE IF EXISTS t_flog_declare_batch",
                "DROP TABLE IF EXISTS t_flog_declare_diff",
                "CREATE TABLE t_flog_declare_row ("
                        + "id bigint PRIMARY KEY, batch_no varchar(64), row_no int, site_no varchar(64),"
                        + "subcomp_no varchar(64), cut_mode int, forest_kind int, dispute_flag int,"
                        + "area decimal(12,2), survey_flag int, item_code varchar(64),"
                        + "qty decimal(12,2), recv_qty decimal(12,2), period_year int, page_flag int,"
                        + "merge_of_row_no int, status int, del_flag int DEFAULT 0,"
                        + "create_by varchar(64), create_time datetime, update_by varchar(64),"
                        + "update_time datetime, remark varchar(500))",
                "CREATE TABLE t_flog_declare_batch ("
                        + "id bigint PRIMARY KEY, batch_no varchar(64), site_no varchar(64), period_year int,"
                        + "total_rows int, accepted_rows int, suspended_rows int, max_row_no int,"
                        + "recv_status int, recv_time datetime, del_flag int DEFAULT 0,"
                        + "create_by varchar(64), create_time datetime, update_by varchar(64),"
                        + "update_time datetime, remark varchar(500), CONSTRAINT uk_batch_no UNIQUE(batch_no))",
                "CREATE TABLE t_flog_declare_diff ("
                        + "id bigint PRIMARY KEY, batch_no varchar(64), site_no varchar(64),"
                        + "subcomp_no varchar(64), row_no int, diff_kind int, old_qty decimal(12,2),"
                        + "new_qty decimal(12,2), status int, handle_by varchar(64), handle_time datetime,"
                        + "del_flag int DEFAULT 0, create_by varchar(64), create_time datetime,"
                        + "update_by varchar(64), update_time datetime, remark varchar(500))"
            }) {
                st.execute(sql);
            }
        }
        MybatisConfiguration cfg = new MybatisConfiguration();
        cfg.addMapper(TFlogDeclareRowMapper.class);
        cfg.addMapper(TFlogDeclareBatchMapper.class);
        cfg.addMapper(TFlogDeclareDiffMapper.class);
        MybatisSqlSessionFactoryBean fb = new MybatisSqlSessionFactoryBean();
        fb.setDataSource(ds);
        fb.setConfiguration(cfg);
        fb.setGlobalConfig(new GlobalConfig().setDbConfig(new GlobalConfig.DbConfig()));
        SqlSessionFactory sf = fb.getObject();
        SqlSession session = sf.openSession(true);
        rowMapper = session.getMapper(TFlogDeclareRowMapper.class);
        batchMapper = session.getMapper(TFlogDeclareBatchMapper.class);
        TFlogDeclareDiffMapper diffMapper = session.getMapper(TFlogDeclareDiffMapper.class);

        service = new TFlogDeclareRowServiceImpl();
        java.lang.reflect.Field f1 = TFlogDeclareRowServiceImpl.class.getDeclaredField("rowMapper");
        f1.setAccessible(true);
        f1.set(service, rowMapper);
        java.lang.reflect.Field f2 = TFlogDeclareRowServiceImpl.class.getDeclaredField("batchMapper");
        f2.setAccessible(true);
        f2.set(service, batchMapper);
        java.lang.reflect.Field f3 = TFlogDeclareRowServiceImpl.class.getDeclaredField("diffMapper");
        f3.setAccessible(true);
        f3.set(service, diffMapper);
    }

    @AfterEach
    void tearDown() throws Exception {
        ((org.h2.jdbcx.JdbcConnectionPool) ds).dispose();
    }

    private FlogDeclareRowDto row(String batch, String site, String sub, int mode, int kind,
                                  int dispute, BigDecimal area, int survey, BigDecimal qty) {
        FlogDeclareRowDto d = new FlogDeclareRowDto();
        d.setBatchNo(batch);
        d.setSiteNo(site);
        d.setSubcompNo(sub);
        d.setCutMode(mode);
        d.setForestKind(kind);
        d.setDisputeFlag(dispute);
        d.setArea(area);
        d.setSurveyFlag(survey);
        d.setQty(qty);
        return d;
    }

    private TFlogDeclareRow entityRow(String site, String sub, int mode, int kind, int dispute,
                                      BigDecimal area, int survey, BigDecimal qty) {
        TFlogDeclareRow r = new TFlogDeclareRow();
        r.setSiteNo(site);
        r.setSubcompNo(sub);
        r.setCutMode(mode);
        r.setForestKind(kind);
        r.setDisputeFlag(dispute);
        r.setArea(area);
        r.setSurveyFlag(survey);
        r.setQty(qty);
        return r;
    }

    /** 1. 正常行已收，行次按进门顺序钉；同册同小班再进一行并作头行、蓄积不落第二次。 */
    @Test
    void testAcceptedAndMerge() {
        FlogRowResultVo v1 = service.processRow(row("B1", "DC00", "S-1", 4, 2, 0, null, 0, bd("40")));
        assertEquals(1, v1.getStatus());
        assertEquals(1, v1.getRowNo());
        assertEquals(bd("40"), v1.getRecvQty());

        FlogRowResultVo v2 = service.processRow(row("B1", "DC00", "S-1", 4, 2, 0, null, 0, bd("99")));
        assertEquals(2, v2.getRowNo());
        assertEquals(Integer.valueOf(1), v2.getMergedIntoRowNo());
        assertNull(v2.getRecvQty(), "并账行不许落第二次数");

        FlogReceiptVo rc = service.finalizeBatch("B1");
        assertEquals(2, rc.getTotalRows());
        assertEquals(2, rc.getAcceptedRows());
        assertEquals(0, rc.getSuspendedRows());

        // 头行账上仍是头一遍那笔蓄积
        TFlogDeclareRow head = rowMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<TFlogDeclareRow>()
                        .eq("batch_no", "B1").eq("row_no", 1));
        assertEquals(0, head.getRecvQty().compareTo(bd("40")));
    }

    /** 2. 三种前置项各按代号挂起，只挂本行，同册余下行照常入账。 */
    @Test
    void testSuspendByPrerequisite() {
        List<TFlogDeclareRow> rows = new ArrayList<>();
        rows.add(entityRow("DC00", "A", 1, 1, 0, null, 0, bd("40")));          // 公益林+皆伐
        rows.add(entityRow("DC00", "B", 4, 2, 1, null, 0, bd("40")));          // 权属在诉
        rows.add(entityRow("DC00", "C", 4, 2, 0, bd("31"), 0, bd("301")));     // 超限未附材料
        rows.add(entityRow("DC00", "D", 4, 2, 0, null, 0, bd("40")));          // 正常
        int n = service.submitBatch("B2", rows);
        assertEquals(1, n);

        FlogReceiptVo rc = service.getReceipt("B2");
        assertEquals(4, rc.getTotalRows());
        assertEquals(1, rc.getAcceptedRows());
        assertEquals(3, rc.getSuspendedRows());

        List<TFlogDeclareRow> errs = service.listErrors("B2");
        assertEquals(3, errs.size());
        assertEquals("PRE_CUT_MODE", errs.get(0).getItemCode());
        assertEquals("PRE_OWNERSHIP", errs.get(1).getItemCode());
        assertEquals("PRE_SURVEY", errs.get(2).getItemCode());
        assertTrue(errs.get(0).getRemark().contains("公益林"));
        assertTrue(errs.get(1).getRemark().length() > 0);
        assertTrue(errs.get(2).getRemark().contains("调查设计材料"));
    }

    /** 3. 等于省级线不算越；越过但附了材料的不挂；面积线同理。 */
    @Test
    void testProvinceBoundaryStrict() {
        // 300 等于线：不挂
        FlogRowResultVo a = service.processRow(row("B3A", "DC00", "E", 4, 2, 0, null, 0, bd("300")));
        assertEquals(1, a.getStatus());
        // 301 未附：挂
        FlogRowResultVo b = service.processRow(row("B3A", "DC00", "F", 4, 2, 0, null, 0, bd("301")));
        assertEquals(2, b.getStatus());
        assertEquals("PRE_SURVEY", b.getItemCode());
        // 301 已附材料：不挂
        FlogRowResultVo c = service.processRow(row("B3A", "DC00", "G", 4, 2, 0, null, 1, bd("301")));
        assertEquals(1, c.getStatus());
        // 面积越线未附：挂；面积等于线：不挂
        FlogRowResultVo d = service.processRow(row("B3A", "DC00", "H", 4, 2, 0, bd("30.01"), 0, bd("10")));
        assertEquals(2, d.getStatus());
        FlogRowResultVo e = service.processRow(row("B3A", "DC00", "I", 4, 2, 0, bd("30"), 0, bd("10")));
        assertEquals(1, e.getStatus());
    }

    /** 4. 收的不能大过报的：当场挡，行没落；少收可以，落实收数。 */
    @Test
    void testReceiveExceedDeclare() {
        FlogDeclareRowDto d = row("B4", "DC00", "J", 4, 2, 0, null, 0, bd("40"));
        d.setRecvQty(bd("50"));
        assertThrows(FlogBizException.class, () -> service.processRow(d));
        assertEquals(0, service.listCurrentRows("B4", null, null).size());

        FlogDeclareRowDto less = row("B4", "DC00", "J2", 4, 2, 0, null, 0, bd("40"));
        less.setRecvQty(bd("30"));
        FlogRowResultVo v = service.processRow(less);
        assertEquals(1, v.getStatus().intValue());
        assertEquals(0, v.getRecvQty().compareTo(bd("30")));
    }

    /** 5. 同批次号重送：不覆盖、不追第二句缘故。 */
    @Test
    void testDuplicateBatchNotOverwritten() {
        service.processRow(row("B5", "DC00", "K", 1, 1, 0, null, 0, bd("40"))); // 挂起
        service.finalizeBatch("B5");
        String firstReason = service.listErrors("B5").get(0).getRemark();

        assertThrows(FlogBizException.class,
                () -> service.processRow(row("B5", "DC00", "K", 4, 2, 0, null, 0, bd("40"))));
        assertThrows(FlogBizException.class, () ->
                service.submitBatch("B5", java.util.Collections.singletonList(
                        entityRow("DC00", "K", 4, 2, 0, null, 0, bd("40")))));

        List<TFlogDeclareRow> rows = service.listCurrentRows("B5", null, null);
        assertEquals(1, rows.size(), "重送不许落第二遍");
        assertEquals(firstReason, rows.get(0).getRemark(), "不给挂起行追第二句缘故");
    }

    /** 6. 空册回执只写空册，绝不写核讫。 */
    @Test
    void testEmptyBooklet() {
        int n = service.submitBatch("B6", new ArrayList<>());
        assertEquals(0, n);
        FlogReceiptVo rc = service.getReceipt("B6");
        assertEquals(2, rc.getRecvStatus());
        assertEquals(0, rc.getTotalRows());
        assertTrue(rc.getMessage().contains("空册"));
        assertFalse(rc.getMessage().contains("核讫"));
    }

    /** 7. 越线整册退回、一行不留。 */
    @Test
    void testOverLimitRejected() {
        List<TFlogDeclareRow> rows = new ArrayList<>();
        for (int i = 0; i < 501; i++) {
            rows.add(entityRow("DC00", "O" + i, 4, 2, 0, null, 0, bd("10")));
        }
        int n = service.submitBatch("B7", rows);
        assertEquals(-1, n);
        TFlogDeclareBatch b = batchMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<TFlogDeclareBatch>()
                        .eq("batch_no", "B7"));
        assertEquals(3, b.getRecvStatus());
        assertEquals(0, service.listCurrentRows("B7", null, null).size());
    }

    /** 8. 逐行递入越过 501：头里落的也清空、整册退回。 */
    @Test
    void testRowByRowOverLimitRejected() {
        for (int i = 0; i < 500; i++) {
            FlogRowResultVo v = service.processRow(row("B8", "DC00", "P" + i, 4, 2, 0, null, 0, bd("10")));
            assertNotNull(v.getStatus());
            assertEquals(i + 1, v.getRowNo());
        }
        FlogRowResultVo overflow = service.processRow(row("B8", "DC00", "P500", 4, 2, 0, null, 0, bd("10")));
        assertNull(overflow.getStatus());
        assertEquals(0, service.listCurrentRows("B8", null, null).size());
        TFlogDeclareBatch b = batchMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<TFlogDeclareBatch>()
                        .eq("batch_no", "B8"));
        assertEquals(3, b.getRecvStatus());
    }

    /** 9. 两个批次各走各的行，不许挪册；一册混两家当场挡。 */
    @Test
    void testBatchIsolation() {
        service.processRow(row("B9A", "DC00", "Q", 4, 2, 0, null, 0, bd("10")));
        service.processRow(row("B9B", "DC02", "Q", 4, 2, 0, null, 0, bd("10")));
        assertEquals(1, service.listCurrentRows("B9A", null, null).size());
        assertEquals(1, service.listCurrentRows("B9B", null, null).size());

        // 已认 DC00 的册里递 DC02 的行：不许挪进来
        assertThrows(FlogBizException.class,
                () -> service.processRow(row("B9A", "DC02", "R", 4, 2, 0, null, 0, bd("10"))));

        List<TFlogDeclareRow> mixed = new ArrayList<>();
        mixed.add(entityRow("DC00", "S1", 4, 2, 0, null, 0, bd("10")));
        mixed.add(entityRow("DC02", "S2", 4, 2, 0, null, 0, bd("10")));
        assertThrows(FlogBizException.class, () -> service.submitBatch("B9C", mixed));
    }

    /** 10. 收满两个执行期转往期，现行页不露，凭批次号拉得出。 */
    @Test
    void testTurnArchived() throws Exception {
        service.processRow(row("B10", "DC00", "T1", 4, 2, 0, null, 0, bd("10")));
        service.processRow(row("B10", "DC00", "T2", 4, 2, 0, null, 0, bd("10")));
        service.finalizeBatch("B10");
        try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
            st.execute("UPDATE t_flog_declare_row SET period_year=2024 WHERE row_no=1");
            st.execute("UPDATE t_flog_declare_row SET period_year=2025 WHERE row_no=2");
        }
        int moved = service.turnArchived(2026);
        assertEquals(1, moved, "2026 年收口：period_year<=2024 的转，2025 的还差一期");
        assertEquals(1, service.listCurrentRows("B10", null, null).size());
        List<TFlogDeclareRow> archived = service.listArchived("B10", null, null);
        assertEquals(1, archived.size());
        assertEquals(Integer.valueOf(2024), archived.get(0).getPeriodYear());
    }

    /** 11. 差异清单：重送走不通，改蓄积走差异，办结才入账且仍不得越申报。 */
    @Test
    void testDiffFlow() {
        service.processRow(row("B11", "DC00", "U", 4, 2, 0, null, 0, bd("40")));
        service.finalizeBatch("B11");

        // 新数大过原申报：登记不上
        assertThrows(FlogBizException.class,
                () -> service.registerDiff("B11", 1, 1, bd("41"), "想多收"));

        TFlogDeclareDiff diff = service.registerDiff("B11", 1, 1, bd("30"), "复核更正");
        assertEquals(0, diff.getStatus().intValue());
        TFlogDeclareDiff done = service.handleDiff(diff.getId(), true, "核准");
        assertEquals(1, done.getStatus().intValue());
        TFlogDeclareRow r1 = service.selectTFlogDeclareRowById(
                service.listCurrentRows("B11", null, null).get(0).getId());
        assertEquals(0, r1.getRecvQty().compareTo(bd("30")));
        // 办结过的不许翻案
        assertThrows(FlogBizException.class, () -> service.handleDiff(diff.getId(), false, null));
    }

    /** 12. 回执同库里逐行是一把尺：收讫后账被动过，查回执当场点破。 */
    @Test
    void testReceiptTamperDetected() throws Exception {
        service.processRow(row("B12", "DC00", "V", 4, 2, 0, null, 0, bd("40")));
        service.processRow(row("B12", "DC00", "W", 1, 1, 0, null, 0, bd("40"))); // 挂起
        service.finalizeBatch("B12");
        try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
            st.execute("UPDATE t_flog_declare_batch SET accepted_rows=99 WHERE batch_no='B12'");
        }
        assertThrows(FlogBizException.class, () -> service.getReceipt("B12"));
    }

    /** 13. 挂起行不许提差异；并账行差异提在头行。 */
    @Test
    void testDiffOnlyOnAcceptedHead() {
        service.processRow(row("B13", "DC00", "X", 1, 1, 0, null, 0, bd("40"))); // 挂起第1行
        service.processRow(row("B13", "DC00", "X", 1, 1, 0, null, 0, bd("40"))); // 并账第2行
        service.finalizeBatch("B13");
        assertThrows(FlogBizException.class,
                () -> service.registerDiff("B13", 1, 1, bd("30"), null));
        assertThrows(FlogBizException.class,
                () -> service.registerDiff("B13", 2, 1, bd("30"), null));
    }

    private BigDecimal bd(String v) {
        return new BigDecimal(v);
    }
}
