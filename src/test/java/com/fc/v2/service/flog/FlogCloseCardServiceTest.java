package com.fc.v2.service.flog;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Calendar;
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
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.fc.v2.common.exception.biz.FlogBizException;
import com.fc.v2.mapper.auto.TFlogApplyCardMapper;
import com.fc.v2.mapper.auto.TFlogCardOpMapper;
import com.fc.v2.mapper.auto.TFlogCloseCardMapper;
import com.fc.v2.mapper.auto.TFlogCloseJournalMapper;
import com.fc.v2.mapper.auto.TFlogQuotaYearMapper;
import com.fc.v2.mapper.auto.TFlogSurveyDesignMapper;
import com.fc.v2.mapper.auto.TFlogSurveyResultMapper;
import com.fc.v2.mapper.auto.TFlogSurveyTreeMapper;
import com.fc.v2.model.auto.TFlogApplyCard;
import com.fc.v2.model.auto.TFlogCloseCard;
import com.fc.v2.model.auto.TFlogCloseJournal;
import com.fc.v2.model.auto.TFlogQuotaYear;
import com.fc.v2.model.custom.flog.FlogCloseCardVo;
import com.fc.v2.model.custom.flog.FlogCloseLedgerVo;
import com.fc.v2.model.custom.flog.FlogCloseOpenDto;
import com.fc.v2.model.custom.flog.FlogClosePassDto;
import com.fc.v2.service.impl.TFlogApplyCardServiceImpl;
import com.fc.v2.service.impl.TFlogCloseCardServiceImpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 伐区勾销事务卡规则自测：H2 内存库 + 真实 MyBatis-Plus mapper，了断挂真建档卡压死服务。
 * 覆盖：四段挨着行、关口点回、跳段倒走不算、同段第二遍不记事、回退沉底、
 * 止步封卡、了断封死且不抽卡、一小班一活卡、履历指纹链缺页多笔手补点破、额度原样退回。
 */
class FlogCloseCardServiceTest {

    private DataSource ds;
    private TFlogCloseCardServiceImpl service;
    private TFlogCloseCardMapper cardMapper;
    private TFlogCloseJournalMapper journalMapper;
    private TFlogApplyCardMapper applyMapper;
    private TFlogQuotaYearMapper quotaMapper;
    private TFlogCardOpMapper cardOpMapper;

    private static final String SITE = "DC00";

    @BeforeEach
    void setUp() throws Exception {
        org.h2.Driver.load();
        ds = org.h2.jdbcx.JdbcConnectionPool.create(
                "jdbc:h2:mem:closecard;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
            for (String sql : DDL) {
                st.execute(sql);
            }
        }
        MybatisConfiguration cfg = new MybatisConfiguration();
        // 每条语句后清一级缓存：手工篡改库里数据后，下一遍 select 必须读库，不许拿缓存旧行
        cfg.setLocalCacheScope(org.apache.ibatis.session.LocalCacheScope.STATEMENT);
        cfg.addMapper(TFlogCloseCardMapper.class);
        cfg.addMapper(TFlogCloseJournalMapper.class);
        cfg.addMapper(TFlogSurveyDesignMapper.class);
        cfg.addMapper(TFlogSurveyResultMapper.class);
        cfg.addMapper(TFlogSurveyTreeMapper.class);
        cfg.addMapper(TFlogApplyCardMapper.class);
        cfg.addMapper(TFlogQuotaYearMapper.class);
        cfg.addMapper(TFlogCardOpMapper.class);
        MybatisSqlSessionFactoryBean fb = new MybatisSqlSessionFactoryBean();
        fb.setDataSource(ds);
        fb.setConfiguration(cfg);
        fb.setGlobalConfig(new GlobalConfig().setDbConfig(new GlobalConfig.DbConfig()));
        SqlSessionFactory sf = fb.getObject();
        SqlSession session = sf.openSession(true);
        cardMapper = session.getMapper(TFlogCloseCardMapper.class);
        journalMapper = session.getMapper(TFlogCloseJournalMapper.class);
        TFlogSurveyDesignMapper designMapper = session.getMapper(TFlogSurveyDesignMapper.class);
        TFlogSurveyResultMapper resultMapper = session.getMapper(TFlogSurveyResultMapper.class);
        TFlogSurveyTreeMapper treeMapper = session.getMapper(TFlogSurveyTreeMapper.class);
        applyMapper = session.getMapper(TFlogApplyCardMapper.class);
        quotaMapper = session.getMapper(TFlogQuotaYearMapper.class);
        cardOpMapper = session.getMapper(TFlogCardOpMapper.class);

        FlogDataScope passScope = new FlogDataScope() {
            @Override
            public void assertVisible(Long userId, String siteNo) {
                // 测试无 Shiro：放行
            }

            @Override
            public boolean isSuperior() {
                return true;
            }
        };

        // 真建档卡服务：了断那一下走它的压死事务（锁额、退额、续流水）
        TFlogApplyCardServiceImpl applyService = new TFlogApplyCardServiceImpl();
        setField(applyService, "baseMapper", applyMapper);
        setField(applyService, "flogStandDocMapper", null);
        setField(applyService, "quotaYearMapper", quotaMapper);
        setField(applyService, "cardOpMapper", cardOpMapper);
        setField(applyService, "dataScope", passScope);

        service = new TFlogCloseCardServiceImpl();
        setField(service, "closeCardMapper", cardMapper);
        setField(service, "journalMapper", journalMapper);
        setField(service, "surveyDesignMapper", designMapper);
        setField(service, "surveyResultMapper", resultMapper);
        setField(service, "surveyTreeMapper", treeMapper);
        setField(service, "applyCardMapper", applyMapper);
        setField(service, "applyCardService", applyService);
        setField(service, "dataScope", passScope);
    }

    @AfterEach
    void tearDown() throws Exception {
        ((org.h2.jdbcx.JdbcConnectionPool) ds).dispose();
    }

    // ------------------------------------------------------------------
    // 1. 四段挨着行走通：卡停的道口系统算成，了断摘卡退额，履历核得过
    // ------------------------------------------------------------------
    @Test
    void testFullLineToClose() {
        seedDesign("D-1", SITE, "S1", "块一", bd("10"), 100, bd("50"));
        seedApplyCard("FK-1", SITE, "S1", bd("100"));
        seedQuota(SITE, bd("500"), bd("100"));

        Long id = open(SITE, "S1").getId();
        assertEquals(0, stage(id), "新卡停在报伐口");

        // 缺格当场点回
        assertThrows(FlogBizException.class, () -> service.advance(pass(id, 0)));
        passReport(id, 0, "D-1", 100, bd("50"), null, null, null);
        assertEquals(1, stage(id));

        // 核株：差异没出格，不给说法也过
        FlogCloseCardVo chk = passCheck(id, 1, null);
        assertEquals(2, chk.getPassedCount().intValue(), "已过报伐、核株两段");
        assertEquals(2, stage(id));

        // 验更：面积小当场点回
        FlogBizException areaEx = assertThrows(FlogBizException.class,
                () -> passRenew(id, 2, bd("9.99"), bd("90")));
        assertTrue(areaEx.getMessage().contains("更新造林面积"));
        passRenew(id, 2, bd("10"), bd("90"));
        assertEquals(3, stage(id));

        // 了断
        FlogCloseCardVo closed = service.advance(pass(id, 3));
        assertEquals(3, closed.getStatus().intValue());
        assertEquals(3, stage(id));
        TFlogCloseCard card = cardMapper.selectById(id);
        assertEquals(3, card.getStatus().intValue());
        assertEquals("FK-1", card.getBillNo());
        assertNotNull(card.getClosedTime());
        assertNull(card.getActiveAnchor(), "了断后活卡锚撤下");

        // 建档卡被压死、额度原样退回、压死流水续一笔
        TFlogApplyCard apply = applyMapper.selectOne(new QueryWrapper<TFlogApplyCard>().eq("bill_no", "FK-1"));
        assertEquals(2, apply.getStatus().intValue());
        TFlogQuotaYear quota = quotaMapper.selectOne(new QueryWrapper<TFlogQuotaYear>().eq("site_no", SITE));
        assertEquals(0, quota.getOccupiedQty().compareTo(bd("0")), "占着的 100 立方米原样退回");
        Integer ops = cardOpMapper.selectCount(new QueryWrapper<com.fc.v2.model.auto.TFlogCardOp>()
                .eq("bill_no", "FK-1").eq("change_type", 3));
        assertEquals(1, ops.intValue(), "压死流水一笔");

        // 履历四笔过口、指纹链核得过
        FlogCloseLedgerVo ledger = service.ledger(id);
        assertTrue(ledger.isIntact(), ledger.getBreakReason());
        assertEquals(4, ledger.getLines().size());
        TFlogCloseJournal last = ledger.getLines().get(3);
        assertEquals(3, last.getStage().intValue());
        assertEquals(0, last.getRefundQty().compareTo(bd("100")));
    }

    // ------------------------------------------------------------------
    // 2. 跳段的、倒着走的一概不算过成
    // ------------------------------------------------------------------
    @Test
    void testNoSkipping() {
        seedDesign("D-2", SITE, "S2", "块二", bd("10"), 100, bd("50"));
        Long id = open(SITE, "S2").getId();

        FlogClosePassDto jump = pass(id, 2);
        jump.setRenewArea(bd("10"));
        jump.setRenewSurvival(bd("90"));
        assertThrows(FlogBizException.class, () -> service.advance(jump));
        assertEquals(0, stage(id), "跳段没走成，卡还压在报伐");
        assertEquals(0, journalCount(id), "跳段不落一笔");

        passReport(id, 0, "D-2", 100, bd("50"), null, null, null);
        assertEquals(1, stage(id));
        // 倒着走回报伐（已在册）：不是异常，是当作第二遍空过，绝不记事
        FlogCloseCardVo dup = passReport(id, 0, "D-2", 90, bd("40"), null, null, null);
        assertTrue(dup.getDuplicate());
        assertEquals(1, stage(id));
        assertEquals(1, journalCount(id), "第二遍不往卡上落一句记事");
    }

    // ------------------------------------------------------------------
    // 3. 同段过第二遍只认头一遍（山里回来一人报两回）
    // ------------------------------------------------------------------
    @Test
    void testSecondReportNeverWritten() {
        seedDesign("D-3", SITE, "S3", "块三", bd("10"), 100, bd("50"));
        Long id = open(SITE, "S3").getId();
        passReport(id, 0, "D-3", 100, bd("50"), null, null, null);
        passCheck(id, 1, null);
        assertEquals(2, stage(id));

        // 第二天又来报一遍核株（带了新说法）：照样不落
        FlogCloseCardVo again = passCheck(id, 1, "补一句说法也不收");
        assertTrue(again.getDuplicate());
        assertEquals(2, journalCount(id), "仍是报伐、核株两笔");
        List<TFlogCloseJournal> checkLines = journalMapper.selectList(new QueryWrapper<TFlogCloseJournal>()
                .eq("card_id", id).eq("stage", 1).eq("action", 1));
        assertEquals(1, checkLines.size());
        assertNull(checkLines.get(0).getDiffReason(), "头一遍没说法就是没说法，第二句补不上");
    }

    // ------------------------------------------------------------------
    // 4. 回退改口：旧那笔沉底留库，与后补的不摆在一处，遍次挨号
    // ------------------------------------------------------------------
    @Test
    void testRollbackSinksAndRedo() {
        seedDesign("D-4", SITE, "S4", "块四", bd("10"), 100, bd("50"));
        seedApplyCard("FK-4", SITE, "S4", bd("80"));
        seedQuota(SITE, bd("500"), bd("80"));
        Long id = open(SITE, "S4").getId();
        passReport(id, 0, "D-4", 100, bd("50"), null, null, null);
        passCheck(id, 1, null);
        passRenew(id, 2, bd("10"), bd("90"));
        assertEquals(3, stage(id));

        // 退一格回验更：验更头一笔沉底
        FlogCloseCardVo rb = service.rollback(id, 2, "面积复测改数");
        assertEquals(2, rb.getStage().intValue());
        assertEquals(2, stage(id));
        TFlogCloseJournal sink = lastJournal(id);
        assertEquals(3, sink.getAction().intValue());
        assertEquals(2, sink.getStage().intValue());
        assertEquals(3, sink.getRefSeq().intValue(), "沉底指住验更那笔（流水序3）");

        // 重过验更：第 2 遍；过罢卡停到了断口
        FlogCloseCardVo redo = passRenew(id, 2, bd("12"), bd("95"));
        assertEquals(3, redo.getStage().intValue());
        TFlogCloseJournal redoLine = lastJournal(id);
        assertEquals(2, redoLine.getAttemptNo().intValue());
        assertEquals(0, redoLine.getRenewArea().compareTo(bd("12")));

        // 了断走到底：沉底的旧笔仍在库里（不删），整链核得过，存活笔只有四笔过口+一笔沉底
        service.advance(pass(id, 3));
        FlogCloseLedgerVo ledger = service.ledger(id);
        assertTrue(ledger.isIntact(), ledger.getBreakReason());
        assertEquals(6, ledger.getLines().size(), "4 过口 + 1 沉底 + 1 了断（了断是第4段过口）= 6");
        // 第 3 笔（旧验更）标记已沉，第 4 笔（沉底）本身也不算活笔
        assertFalse(ledger.getAlive().get(2));
        assertFalse(ledger.getAlive().get(3));
        assertTrue(ledger.getAlive().get(4));

        // 想再退到别的落点不行；了断卡根本退不动
        assertThrows(FlogBizException.class, () -> service.rollback(id, 1, null));
        assertThrows(FlogBizException.class, () -> service.rollback(id, null, null));
    }

    // ------------------------------------------------------------------
    // 5. 回退只准落一格：乱指落点不算退成；头一段无可退
    // ------------------------------------------------------------------
    @Test
    void testRollbackSingleLanding() {
        seedDesign("D-5", SITE, "S5", "块五", bd("10"), 100, bd("50"));
        Long id = open(SITE, "S5").getId();
        assertThrows(FlogBizException.class, () -> service.rollback(id, null, null), "头一道口前没有段可退");
        passReport(id, 0, "D-5", 100, bd("50"), null, null, null);
        // 停在核株(1)，回退落点只可能是报伐(0)，指成核株(1)不算
        assertThrows(FlogBizException.class, () -> service.rollback(id, 1, null));
        FlogCloseCardVo ok = service.rollback(id, 0, "实采数要改");
        assertEquals(0, ok.getStage().intValue());
    }

    // ------------------------------------------------------------------
    // 6. 止步：写下“这一段不过了”即封卡，谁也续不进；同小班可另起
    // ------------------------------------------------------------------
    @Test
    void testHaltSealsCard() {
        seedDesign("D-6", SITE, "S6", "块六", bd("10"), 100, bd("50"));
        Long id = open(SITE, "S6").getId();
        assertThrows(FlogBizException.class, () -> service.halt(id, 0, null), "止步缘故不能空");
        FlogCloseCardVo halted = service.halt(id, 0, "权属在诉，这一段不过了");
        assertEquals(2, halted.getStatus().intValue());
        assertEquals(0, halted.getStage().intValue());

        TFlogCloseCard card = cardMapper.selectById(id);
        assertEquals(2, card.getStatus().intValue());
        assertNull(card.getActiveAnchor());
        // 续不进去
        assertThrows(FlogBizException.class, () -> service.halt(id, 0, "再止一回"));
        assertThrows(FlogBizException.class, () -> service.advance(pass(id, 0)));
        assertThrows(FlogBizException.class, () -> service.rollback(id, null, null));

        // 履历含止步笔且核得过（止在报伐口，头一笔即止步）
        FlogCloseLedgerVo ledger = service.ledger(id);
        assertTrue(ledger.isIntact(), ledger.getBreakReason());
        assertEquals(1, ledger.getLines().size());
        assertEquals(2, ledger.getLines().get(0).getAction().intValue());

        // 止步后同小班许另起一张；新卡与旧卡各是各
        FlogCloseCardVo again = open(SITE, "S6");
        assertNotNull(again.getId());
        assertTrue(!again.getId().equals(id));
        assertEquals(2, cardMapper.selectCount(new QueryWrapper<TFlogCloseCard>()
                .eq("site_no", SITE).eq("subcomp_no", "S6")).intValue());
    }

    // ------------------------------------------------------------------
    // 7. 了断即封死：不许改记事、不许抽走
    // ------------------------------------------------------------------
    @Test
    void testCloseImmutableAndNotDeletable() {
        seedDesign("D-7", SITE, "S7", "块七", bd("10"), 100, bd("50"));
        seedApplyCard("FK-7", SITE, "S7", bd("60"));
        seedQuota(SITE, bd("500"), bd("60"));
        Long id = open(SITE, "S7").getId();
        passReport(id, 0, "D-7", 100, bd("50"), null, null, null);
        passCheck(id, 1, null);
        passRenew(id, 2, bd("10"), bd("90"));
        service.advance(pass(id, 3));

        assertFalse(service.updateContent(id, "改一句"));
        assertFalse(service.remove(id));
        assertThrows(FlogBizException.class, () -> service.advance(pass(id, 3)));
        assertThrows(FlogBizException.class, () -> service.halt(id, 3, "了断还止什么"));

        TFlogCloseCard card = cardMapper.selectById(id);
        assertEquals(0, card.getDelFlag().intValue(), "卡不许抽走，年底要查得着");
        // 了断后同小班可另起一张
        assertNotNull(open(SITE, "S7"));
    }

    // ------------------------------------------------------------------
    // 8. 一小班同时只许一张活卡
    // ------------------------------------------------------------------
    @Test
    void testOneActiveCardPerSubcomp() {
        open(SITE, "S8");
        assertThrows(FlogBizException.class, () -> open(SITE, "S8"));
        // 别的小班照起
        assertNotNull(open(SITE, "S8-OTHER"));
        // 前卡止步，同小班第二张立得起来
        List<TFlogCloseCard> cards = cardMapper.selectList(new QueryWrapper<TFlogCloseCard>()
                .eq("site_no", SITE).eq("subcomp_no", "S8"));
        service.halt(cards.get(0).getId(), 0, "先挂起");
        assertNotNull(open(SITE, "S8"));
    }

    // ------------------------------------------------------------------
    // 9. 核株差异格：等于线不算过线；过线必须给说法
    // ------------------------------------------------------------------
    @Test
    void testDiffGridBoundary() {
        // 设计 100 株 / 50 立方米。105 株 = 差 5%，正好压线，不给说法也过
        seedDesign("D-9A", SITE, "S9A", "块九甲", bd("10"), 100, bd("50"));
        Long a = open(SITE, "S9A").getId();
        passReport(a, 0, "D-9A", 105, bd("50"), null, null, null);
        passCheck(a, 1, null);
        assertEquals(2, stage(a));
        service.halt(a, 2, "甲卡挂起");

        // 106 株 = 差 6%，过格，不给说法当场点回；给了说法才过
        seedDesign("D-9B", SITE, "S9B", "块九乙", bd("10"), 100, bd("50"));
        Long b = open(SITE, "S9B").getId();
        passReport(b, 0, "D-9B", 106, bd("50"), null, null, null);
        FlogBizException ex = assertThrows(FlogBizException.class, () -> passCheck(b, 1, null));
        assertTrue(ex.getMessage().contains("说法"), ex.getMessage());
        passCheck(b, 1, "边角雪压木六株未入设计");
        TFlogCloseJournal checkLine = journalMapper.selectList(new QueryWrapper<TFlogCloseJournal>()
                .eq("card_id", b).eq("stage", 1).eq("action", 1)).get(0);
        assertEquals(0, checkLine.getTreeDiffRate().compareTo(bd("0.06")));
        assertEquals("边角雪压木六株未入设计", checkLine.getDiffReason());

        // 蓄积差过线同理
        seedDesign("D-9C", SITE, "S9C", "块九丙", bd("10"), 100, bd("50"));
        Long c = open(SITE, "S9C").getId();
        passReport(c, 0, "D-9C", 100, bd("53"), null, null, null);
        assertThrows(FlogBizException.class, () -> passCheck(c, 1, null));
    }

    // ------------------------------------------------------------------
    // 10. 验更关口：成活率也得像样
    // ------------------------------------------------------------------
    @Test
    void testRenewGuards() {
        seedDesign("D-10", SITE, "S10", "块十", bd("8"), 10, bd("20"));
        Long id = open(SITE, "S10").getId();
        passReport(id, 0, "D-10", 10, bd("20"), null, null, null);
        passCheck(id, 1, null);
        assertThrows(FlogBizException.class, () -> passRenew(id, 2, bd("8"), bd("101")));
        assertThrows(FlogBizException.class, () -> passRenew(id, 2, null, bd("90")));
        passRenew(id, 2, bd("8.5"), bd("85"));
        assertEquals(3, stage(id));
    }

    // ------------------------------------------------------------------
    // 11. 报伐：设计单号、同小班、钉档结论，缺一不认
    // ------------------------------------------------------------------
    @Test
    void testReportDesignGuards() {
        seedDesign("D-11", SITE, "S11", "块十一", bd("10"), 100, bd("50"));
        Long id = open(SITE, "S11").getId();
        // 张冠李戴：另一小班的设计单
        seedDesign("D-11X", SITE, "S11X", "别处", bd("10"), 100, bd("50"));
        FlogBizException ex = assertThrows(FlogBizException.class,
                () -> passReport(id, 0, "D-11X", 100, bd("50"), null, null, null));
        assertTrue(ex.getMessage().contains("对不上"));
        // 查无单号
        assertThrows(FlogBizException.class,
                () -> passReport(id, 0, "NO-SUCH", 100, bd("50"), null, null, null));
        // 正常认下
        passReport(id, 0, "D-11", 100, bd("50"), null, null, null);
        assertNotNull(cardMapper.selectById(id).getDesignId());
    }

    // ------------------------------------------------------------------
    // 12. 了断摘不出在册现行建档卡：落不下
    // ------------------------------------------------------------------
    @Test
    void testCloseWithoutApplyCardRejected() {
        seedDesign("D-12", SITE, "S12", "块十二", bd("10"), 100, bd("50"));
        Long id = open(SITE, "S12").getId();
        passReport(id, 0, "D-12", 100, bd("50"), null, null, null);
        passCheck(id, 1, null);
        passRenew(id, 2, bd("10"), bd("90"));
        assertThrows(FlogBizException.class, () -> service.advance(pass(id, 3)),
                "没东西可摘、没额度可退，了断落不下");
        assertEquals(3, stage(id));
        assertEquals(1, cardMapper.selectById(id).getStatus().intValue(), "卡仍在办，不算了断");
        assertEquals(3, journalCount(id), "了断没落，履历只到验更");
    }

    // ------------------------------------------------------------------
    // 13. 勾销履历：改一笔、缺一页、凭空补一笔、改卡面，抽见一处就算没走成
    // ------------------------------------------------------------------
    @Test
    void testLedgerTamperDetected() throws Exception {
        // (a) 改过一笔的格子：指纹算不拢
        Long a = open(SITE, "S13A").getId();
        seedDesign("D-13A", SITE, "S13A", "块十三甲", bd("10"), 100, bd("50"));
        passReport(a, 0, "D-13A", 100, bd("50"), null, null, null);
        passCheck(a, 1, null);
        try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
            st.execute("UPDATE t_flog_close_journal SET actual_trees=99 WHERE card_id=" + a + " AND seq=1");
        }
        FlogCloseLedgerVo tampered = service.ledger(a);
        assertFalse(tampered.isIntact());
        assertEquals(1, tampered.getBreakSeq().intValue());
        assertTrue(tampered.getBreakReason().contains("指纹"));

        // (b) 缺一页：删掉第 1 笔，序断
        Long b = open(SITE, "S13B").getId();
        seedDesign("D-13B", SITE, "S13B", "块十三乙", bd("10"), 100, bd("50"));
        passReport(b, 0, "D-13B", 100, bd("50"), null, null, null);
        passCheck(b, 1, null);
        try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
            st.execute("DELETE FROM t_flog_close_journal WHERE card_id=" + b + " AND seq=1");
        }
        FlogCloseLedgerVo missing = service.ledger(b);
        assertFalse(missing.isIntact());
        assertTrue(missing.getBreakReason().contains("缺页"));

        // (c) 凭空补一笔同段在册过口（没有沉底笔）：指纹链接得严丝合缝，仍该点破“多出来”
        Long cid = open(SITE, "S13C").getId();
        seedDesign("D-13C", SITE, "S13C", "块十三丙", bd("10"), 100, bd("50"));
        passReport(cid, 0, "D-13C", 100, bd("50"), null, null, null);
        TFlogCloseJournal realLast = lastJournal(cid);
        TFlogCloseJournal fake = new TFlogCloseJournal();
        fake.setCardId(cid);
        fake.setBizNo(realLast.getBizNo());
        fake.setSeq(2);
        fake.setStage(0);
        fake.setAttemptNo(2);
        fake.setAction(1);
        fake.setDesignNo("D-13C");
        fake.setActualTrees(106);
        fake.setActualVolume(bd("50"));
        fake.setOpBy(realLast.getOpBy());
        fake.setOpTime(realLast.getOpTime());
        fake.setDelFlag(0);
        fake.setPrevFp(realLast.getFp());
        fake.setFp((String) invokePrivate(service, "fingerprint",
                new Class<?>[]{TFlogCloseJournal.class, String.class}, fake, realLast.getFp()));
        journalMapper.insert(fake);

        FlogCloseLedgerVo extra = service.ledger(cid);
        assertFalse(extra.isIntact());
        assertEquals(2, extra.getBreakSeq().intValue());
        assertTrue(extra.getBreakReason().contains("凭空多出"), extra.getBreakReason());
    }

    @Test
    void testCardFaceTamperDetected() throws Exception {
        seedDesign("D-14", SITE, "S14", "块十四", bd("10"), 100, bd("50"));
        Long id = open(SITE, "S14").getId();
        passReport(id, 0, "D-14", 100, bd("50"), null, null, null);
        // 卡面 stage 被人手拨到 3，履历还在核株口
        try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
            st.execute("UPDATE t_flog_close_card SET stage=3 WHERE id=" + id);
        }
        FlogCloseLedgerVo vo = service.ledger(id);
        assertFalse(vo.isIntact());
        assertTrue(vo.getBreakReason().contains("对不上"));
    }

    // ------------------------------------------------------------------
    // 辅助
    // ------------------------------------------------------------------

    private FlogCloseCardVo open(String site, String sub) {
        FlogCloseOpenDto dto = new FlogCloseOpenDto();
        dto.setSiteNo(site);
        dto.setSubcompNo(sub);
        return service.open(dto);
    }

    private int stage(Long id) {
        return cardMapper.selectById(id).getStage();
    }

    private int journalCount(Long id) {
        return journalMapper.selectCount(new QueryWrapper<TFlogCloseJournal>().eq("card_id", id));
    }

    private TFlogCloseJournal lastJournal(Long id) {
        return journalMapper.selectOne(new QueryWrapper<TFlogCloseJournal>()
                .eq("card_id", id).orderByDesc("seq").last("limit 1"));
    }

    private FlogClosePassDto pass(Long id, int stage) {
        FlogClosePassDto dto = new FlogClosePassDto();
        dto.setId(id);
        dto.setStage(stage);
        return dto;
    }

    private FlogCloseCardVo passReport(Long id, int stage, String designNo, Integer trees, BigDecimal vol,
                                       String unused1, Object unused2, Object unused3) {
        FlogClosePassDto dto = pass(id, stage);
        dto.setDesignNo(designNo);
        dto.setActualTrees(trees);
        dto.setActualVolume(vol);
        return service.advance(dto);
    }

    private FlogCloseCardVo passCheck(Long id, int stage, String reason) {
        FlogClosePassDto dto = pass(id, stage);
        dto.setDiffReason(reason);
        return service.advance(dto);
    }

    private FlogCloseCardVo passRenew(Long id, int stage, BigDecimal area, BigDecimal survival) {
        FlogClosePassDto dto = pass(id, stage);
        dto.setRenewArea(area);
        dto.setRenewSurvival(survival);
        return service.advance(dto);
    }

    private void setField(Object target, String name, Object value) throws Exception {
        java.lang.reflect.Field f = null;
        Class<?> c = target.getClass();
        while (c != null && f == null) {
            try {
                f = c.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            }
        }
        assertNotNull(f, "字段 " + name + " 找不到");
        f.setAccessible(true);
        f.set(target, value);
    }

    private Object invokePrivate(Object target, String name, Class<?>[] types, Object... args) throws Exception {
        java.lang.reflect.Method m = target.getClass().getDeclaredMethod(name, types);
        m.setAccessible(true);
        return m.invoke(target, args);
    }

    private BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    /** 调查设计单 + 首评钉档结论 + 逐株账（trees 株，总蓄积 vol）。 */
    private void seedDesign(String designNo, String site, String sub, String block,
                            BigDecimal area, int trees, BigDecimal vol) {
        Long designId = idSeq[0]++;
        Long resultId = idSeq[0]++;
        try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
            st.execute("INSERT INTO t_flog_survey_design (id, design_no, site_no, subcomp_no, cut_block_no,"
                    + " apply_scope, area, yield_rate, survey_by, design_date, latest_result_id, del_flag,"
                    + " create_time) VALUES (" + designId + ", '" + designNo + "', '" + site + "', '" + sub
                    + "', '" + block + "', 2, " + area.toPlainString() + ", 0.7, 'tester', CURRENT_TIMESTAMP, "
                    + resultId + ", 0, CURRENT_TIMESTAMP)");
            st.execute("INSERT INTO t_flog_survey_result (id, design_id, round_no, recheck_flag, total_volume,"
                    + " yield_per_ha, grade_level, basis_missing, rule_code, pinned_by, pinned_time, del_flag,"
                    + " create_time) VALUES (" + resultId + ", " + designId + ", 1, 0, " + vol.toPlainString()
                    + ", 3.5, 1, 0, 'GY-X', 'tester', CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP)");
            for (int i = 1; i <= trees; i++) {
                st.execute("INSERT INTO t_flog_survey_tree (id, design_id, tree_no, round_no, dbh, height,"
                        + " tree_volume, del_flag) VALUES (" + idSeq[0]++ + ", " + designId + ", " + i
                        + ", 1, 20, 10, " + vol.divide(new BigDecimal(trees), 6, java.math.RoundingMode.HALF_UP)
                        .toPlainString() + ", 0)");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private final long[] idSeq = {1000L};

    private void seedApplyCard(String billNo, String site, String sub, BigDecimal qty) {
        TFlogApplyCard card = new TFlogApplyCard();
        card.setBillNo(billNo);
        card.setNodeNo(1);
        card.setSiteNo(site);
        card.setSubcompNo(sub);
        card.setCutBlockNo("块");
        card.setCutMode(1);
        card.setQty(qty);
        card.setFineAmt(bd("10"));
        card.setGradeLevel(2);
        card.setCurrentFlag(1);
        card.setStatus(0);
        card.setDelFlag(0);
        card.setRegisterBy("tester");
        card.setRegisterTime(new Date());
        card.setCreateBy("tester");
        card.setCreateTime(new Date());
        applyMapper.insert(card);
    }

    private void seedQuota(String site, BigDecimal total, BigDecimal occupied) {
        TFlogQuotaYear q = new TFlogQuotaYear();
        q.setSiteNo(site);
        q.setQuotaYear(Calendar.getInstance().get(Calendar.YEAR));
        q.setTotalQty(total);
        q.setOccupiedQty(occupied);
        q.setStatus(0);
        q.setDelFlag(0);
        quotaMapper.insert(q);
    }

    private static final String[] DDL = {
        "DROP TABLE IF EXISTS t_flog_close_journal",
        "DROP TABLE IF EXISTS t_flog_close_card",
        "DROP TABLE IF EXISTS t_flog_card_op",
        "DROP TABLE IF EXISTS t_flog_quota_year",
        "DROP TABLE IF EXISTS t_flog_apply_card",
        "DROP TABLE IF EXISTS t_flog_survey_tree",
        "DROP TABLE IF EXISTS t_flog_survey_result",
        "DROP TABLE IF EXISTS t_flog_survey_design",
        "CREATE TABLE t_flog_close_card ("
                + "id bigint PRIMARY KEY, biz_no varchar(64), site_no varchar(64), subcomp_no varchar(64),"
                + "cut_block_no varchar(64), design_id bigint, design_no varchar(64), apply_card_id bigint,"
                + "bill_no varchar(64), stage int, status int, halt_reason varchar(500),"
                + "closed_by varchar(64), closed_time datetime, active_anchor varchar(160),"
                + "del_flag int DEFAULT 0, create_by varchar(64), create_time datetime,"
                + "update_by varchar(64), update_time datetime, remark varchar(500),"
                + "CONSTRAINT uk_close_active UNIQUE(active_anchor))",
        "CREATE TABLE t_flog_close_journal ("
                + "id bigint PRIMARY KEY, card_id bigint, biz_no varchar(64), seq int, stage int,"
                + "attempt_no int, action int, ref_seq int, design_no varchar(64), actual_trees int,"
                + "actual_volume decimal(14,4), design_trees int, design_volume decimal(14,4),"
                + "tree_diff_rate decimal(8,4), vol_diff_rate decimal(8,4), diff_reason varchar(500),"
                + "cut_area decimal(12,2), renew_area decimal(12,2), renew_survival decimal(5,2),"
                + "occupy_qty decimal(12,2), refund_qty decimal(12,2), op_by varchar(64), op_time datetime,"
                + "prev_fp varchar(64), fp varchar(64), del_flag int DEFAULT 0,"
                + "create_by varchar(64), create_time datetime, update_by varchar(64), update_time datetime,"
                + "remark varchar(500), CONSTRAINT uk_closejrn UNIQUE(card_id, seq))",
        "CREATE TABLE t_flog_survey_design ("
                + "id bigint PRIMARY KEY, design_no varchar(64), site_no varchar(64), subcomp_no varchar(64),"
                + "cut_block_no varchar(64), apply_scope int, area decimal(12,2), yield_rate decimal(8,4),"
                + "survey_by varchar(64), design_date datetime, latest_result_id bigint, del_flag int DEFAULT 0,"
                + "create_by varchar(64), create_time datetime, update_by varchar(64), update_time datetime,"
                + "remark varchar(500), CONSTRAINT uk_sd_no UNIQUE(design_no))",
        "CREATE TABLE t_flog_survey_tree ("
                + "id bigint PRIMARY KEY, design_id bigint, tree_no int, round_no int, dbh decimal(8,2),"
                + "height decimal(8,2), tree_volume decimal(14,6), del_flag int DEFAULT 0,"
                + "create_by varchar(64), create_time datetime, update_by varchar(64), update_time datetime)",
        "CREATE TABLE t_flog_survey_result ("
                + "id bigint PRIMARY KEY, design_id bigint, round_no int, recheck_flag int,"
                + "total_volume decimal(14,4), yield_per_ha decimal(14,4), grade_level int, basis_missing int,"
                + "rule_code varchar(64), rule_eff_start datetime, vol_table_code varchar(64),"
                + "vol_table_eff_start datetime, pinned_by varchar(64), pinned_time datetime, del_flag int DEFAULT 0,"
                + "create_by varchar(64), create_time datetime, update_by varchar(64), update_time datetime,"
                + "remark varchar(500))",
        "CREATE TABLE t_flog_apply_card ("
                + "id bigint PRIMARY KEY, bill_no varchar(64), node_no int, site_id bigint, site_no varchar(64),"
                + "subcomp_no varchar(64), cut_block_no varchar(64), cut_mode int, clear_trees int,"
                + "grad_years int, grad_interval int, select_intensity decimal(5,2), select_keep_trees int,"
                + "tend_method varchar(64), tend_per_ha decimal(12,2), renew_mode varchar(64),"
                + "renew_finish_year int, reform_tree varchar(64), register_by varchar(64),"
                + "register_time datetime, current_flag int, qty decimal(12,2), fine_amt decimal(12,2),"
                + "content varchar(255), grade_level int, status int, del_flag int DEFAULT 0,"
                + "create_by varchar(64), create_time datetime, update_by varchar(64), update_time datetime,"
                + "remark varchar(500))",
        "CREATE TABLE t_flog_quota_year ("
                + "id bigint PRIMARY KEY, site_no varchar(64), quota_year int, total_qty decimal(14,2),"
                + "occupied_qty decimal(14,2), status int, del_flag int DEFAULT 0,"
                + "create_by varchar(64), create_time datetime, update_by varchar(64), update_time datetime,"
                + "remark varchar(500), CONSTRAINT uk_qy UNIQUE(site_no, quota_year))",
        "CREATE TABLE t_flog_card_op ("
                + "id bigint PRIMARY KEY, card_id bigint, bill_no varchar(64), node_no int, change_type int,"
                + "qty decimal(12,2), occupy_qty decimal(12,2), op_by varchar(64), op_time datetime,"
                + "del_flag int DEFAULT 0, create_by varchar(64), create_time datetime,"
                + "update_by varchar(64), update_time datetime, remark varchar(500))"
    };
}
