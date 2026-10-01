package com.fc.v2.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fc.v2.common.exception.biz.FlogBizException;
import com.fc.v2.common.support.ConvertUtil;
import com.fc.v2.mapper.auto.TFlogApplyCardMapper;
import com.fc.v2.mapper.auto.TFlogCardOpMapper;
import com.fc.v2.mapper.auto.TFlogQuotaYearMapper;
import com.fc.v2.mapper.auto.TFlogStandDocMapper;
import com.fc.v2.model.auto.TFlogApplyCard;
import com.fc.v2.model.auto.TFlogCardOp;
import com.fc.v2.model.auto.TFlogQuotaYear;
import com.fc.v2.model.auto.TFlogStandDoc;
import com.fc.v2.model.custom.flog.FlogCardSaveDto;
import com.fc.v2.model.custom.flog.FlogCardSaveVo;
import com.fc.v2.model.custom.flog.FlogCutMode;
import com.fc.v2.model.custom.flog.FlogQuotaVo;
import com.fc.v2.model.custom.flog.FlogQuotaView;
import com.fc.v2.service.ITFlogApplyCardService;
import com.fc.v2.service.flog.FlogDataScope;
import com.fc.v2.shiro.util.ShiroUtils;
import com.fc.v2.util.MD5Util;
import com.fc.v2.util.SnowflakeIdWorker;
import com.fc.v2.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 伐区采伐申请建档卡Service业务层处理
 *
 * 开不开得出卡，只由本层这一条保存口说了算：必填栏照采伐方式当场点、
 * 取用顺位系统折算回填、核验串随版本拼、额度同一回计算扣退。界面没有第二条通道。
 *
 * @author fuce
 * @date 2026-09-12
 */
@Service
public class TFlogApplyCardServiceImpl extends ServiceImpl<TFlogApplyCardMapper, TFlogApplyCard>
        implements ITFlogApplyCardService {

    /** 进展：待核对 */
    private static final int STATUS_PENDING = 0;
    /** 进展：已核对（成形，覆不掉） */
    private static final int STATUS_CHECKED = 1;
    /** 进展：已压死（占额原样退回） */
    private static final int STATUS_PRESSED = 2;

    /** 现行版本 */
    private static final int CURRENT_YES = 1;
    /** 历史版本 */
    private static final int CURRENT_NO = 0;

    /** 办理动作：立卡 */
    private static final int OP_REGISTER = 1;
    /** 办理动作：改版 */
    private static final int OP_REVISE = 2;
    /** 办理动作：压死 */
    private static final int OP_PRESS = 3;
    /** 办理动作：核对成形 */
    private static final int OP_CHECK = 4;

    @Autowired
    private TFlogStandDocMapper flogStandDocMapper;

    @Autowired
    private TFlogQuotaYearMapper quotaYearMapper;

    @Autowired
    private TFlogCardOpMapper cardOpMapper;

    @Autowired
    private FlogDataScope dataScope;

    @Override
    public TFlogApplyCard selectTFlogApplyCardById(Long id) {
        TFlogApplyCard card = this.baseMapper.selectOne(new QueryWrapper<TFlogApplyCard>()
                .eq("id", id)
                .eq("del_flag", 0));
        if (card != null) {
            dataScope.assertVisible(currentUserId(), card.getSiteNo());
        }
        return card;
    }

    @Override
    public List<TFlogApplyCard> selectTFlogApplyCardList(Wrapper<TFlogApplyCard> queryWrapper) {
        QueryWrapper<TFlogApplyCard> wrapper =
                queryWrapper instanceof QueryWrapper ? (QueryWrapper<TFlogApplyCard>) queryWrapper : new QueryWrapper<>();
        // 列表只摆现行版本，旧版本另存一层供事后翻检，不许并列挑
        wrapper.eq("current_flag", CURRENT_YES).eq("del_flag", 0);
        // 翻看界：上级账号全辖；本站只看本辖；跨县协办只摆受托那家
        if (!dataScope.isSuperior()) {
            List<String> siteNos = dataScope.visibleSiteNos(currentUserId());
            if (siteNos.isEmpty()) {
                return new ArrayList<>();
            }
            wrapper.in("site_no", siteNos);
        }
        wrapper.orderByDesc("create_time");
        return this.baseMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FlogCardSaveVo insertTFlogApplyCard(TFlogApplyCard record) {
        return saveCard(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FlogCardSaveVo insertTFlogApplyCard(FlogCardSaveDto dto) {
        return saveCard(toEntity(dto));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FlogCardSaveVo updateTFlogApplyCard(TFlogApplyCard record) {
        // 旧修改口不再各自为政，一律并到唯一保存口：同号同小班升版，绝无原地覆写
        return saveCard(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FlogCardSaveVo checkCard(Long id) {
        TFlogApplyCard card = requireCurrentCard(id);
        if (card.getStatus() != null && card.getStatus() == STATUS_PRESSED) {
            throw new FlogBizException("卡号 " + card.getBillNo() + " 已压死，不能再核对");
        }
        if (card.getStatus() != null && card.getStatus() == STATUS_CHECKED) {
            throw new FlogBizException("卡号 " + card.getBillNo() + " 已核对成形，成形的版覆不掉；要改请重录一版");
        }
        // 钉住成形：条件更新只许“待核对”落成“已核对”，并发两笔只有一笔抢得到，防重复钉版
        Date versionDay = card.getCreateTime() == null ? new Date() : card.getCreateTime();
        String content = StringUtils.isEmpty(card.getContent()) ? buildContent(card, versionDay) : card.getContent();
        int rows = this.baseMapper.update(null, new UpdateWrapper<TFlogApplyCard>()
                .eq("id", card.getId())
                .eq("current_flag", CURRENT_YES)
                .eq("status", STATUS_PENDING)
                .set("status", STATUS_CHECKED)
                .set("content", content));
        if (rows == 0) {
            throw new FlogBizException("卡号 " + card.getBillNo() + " 刚被另一笔核对或顶版，本笔请刷新后再看");
        }
        card.setStatus(STATUS_CHECKED);
        card.setContent(content);
        writeOp(card, OP_CHECK, card.getQty(), card.getQty());
        return buildVo(card, card.getQty(), BigDecimal.ZERO, "卡号 " + card.getBillNo() + " 已核对成形，本版钉住");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FlogCardSaveVo pressCard(Long id) {
        TFlogApplyCard card = requireCurrentCard(id);
        int year = chargeYearOf(card);
        // 先锁年度账行，把这一家该年的扣/退串行化，杜绝并发两笔各退各的
        TFlogQuotaYear quota = lockQuotaYear(card.getSiteNo(), year);
        // 锁内复查，防重复压死重复退
        TFlogApplyCard fresh = this.baseMapper.selectById(card.getId());
        if (fresh == null || (fresh.getDelFlag() != null && fresh.getDelFlag() == 1)) {
            throw new FlogBizException("卡已不在册，压不了");
        }
        if (fresh.getStatus() != null && fresh.getStatus() == STATUS_PRESSED) {
            throw new FlogBizException("卡号 " + card.getBillNo() + " 已经压死，不许重复了断");
        }
        // 压死：条件更新只许非压死落成压死，锁内防重复退
        int rows = this.baseMapper.update(null, new UpdateWrapper<TFlogApplyCard>()
                .eq("id", card.getId())
                .eq("current_flag", CURRENT_YES)
                .ne("status", STATUS_PRESSED)
                .set("status", STATUS_PRESSED));
        if (rows == 0) {
            throw new FlogBizException("卡号 " + card.getBillNo() + " 已经压死，不许重复了断");
        }
        card.setStatus(STATUS_PRESSED);
        BigDecimal refund = nullToZero(card.getQty());
        applyOccupied(quota, refund.negate());
        writeOp(card, OP_PRESS, refund, BigDecimal.ZERO);
        return buildVo(card, quota, BigDecimal.ZERO, refund.negate(),
                "卡号 " + card.getBillNo() + " 已压死，占用额度 " + refund + " 立方米原样退回");
    }

    @Override
    public FlogQuotaView quotaView(String siteNo, Integer quotaYear) {
        if (StringUtils.isEmpty(siteNo)) {
            throw new FlogBizException("没给底档字号，翻不出额度账");
        }
        dataScope.assertVisible(currentUserId(), siteNo);
        int year = quotaYear == null ? yearOf(new Date()) : quotaYear;
        TFlogStandDoc doc = requireStandDocByNo(siteNo);
        TFlogQuotaYear quota = requireQuotaYear(siteNo, year);

        // 权威已扣列即账；占额清单只作同屏摆示，不再靠页面/现算累加
        List<TFlogApplyCard> occupying = listOccupyCards(siteNo, year);
        BigDecimal total = nullToZero(quota.getTotalQty());
        BigDecimal occupied = nullToZero(quota.getOccupiedQty());
        BigDecimal remain = total.subtract(occupied);

        FlogQuotaVo vo = new FlogQuotaVo();
        vo.setSiteNo(siteNo);
        vo.setSiteName(doc.getSiteName());
        vo.setQuotaYear(year);
        vo.setTotalQty(total);
        vo.setOccupiedQty(occupied);
        vo.setRemainQty(remain);

        // 占额清单上把立卡人与最近一笔办理人分开摆
        List<Long> cardIds = new ArrayList<>();
        for (TFlogApplyCard c : occupying) {
            cardIds.add(c.getId());
        }
        Map<Long, TFlogCardOp> lastOpMap = new HashMap<>();
        if (!cardIds.isEmpty()) {
            List<TFlogCardOp> ops = cardOpMapper.selectList(new QueryWrapper<TFlogCardOp>()
                    .in("card_id", cardIds)
                    .eq("del_flag", 0)
                    .orderByDesc("op_time"));
            for (TFlogCardOp op : ops) {
                lastOpMap.putIfAbsent(op.getCardId(), op);
            }
        }
        List<FlogQuotaView.OccupyLine> lines = new ArrayList<>();
        for (TFlogApplyCard c : occupying) {
            FlogQuotaView.OccupyLine line = new FlogQuotaView.OccupyLine();
            line.setBillNo(c.getBillNo());
            line.setNodeNo(c.getNodeNo());
            line.setSubcompNo(c.getSubcompNo());
            line.setCutBlockNo(c.getCutBlockNo());
            line.setGradeLevel(c.getGradeLevel());
            line.setStatus(c.getStatus());
            line.setQty(c.getQty());
            line.setRegisterBy(c.getRegisterBy());
            line.setRegisterTime(c.getRegisterTime());
            TFlogCardOp lastOp = lastOpMap.get(c.getId());
            if (lastOp != null) {
                line.setLastOpBy(lastOp.getOpBy());
                line.setLastOpTime(lastOp.getOpTime());
            }
            lines.add(line);
        }
        lines.sort(Comparator.comparing(FlogQuotaView.OccupyLine::getBillNo,
                Comparator.nullsLast(Comparator.naturalOrder())));
        return new FlogQuotaView(vo, lines);
    }

    @Override
    public List<TFlogApplyCard> selectHistoryVersions(String billNo) {
        if (StringUtils.isEmpty(billNo)) {
            return new ArrayList<>();
        }
        List<TFlogApplyCard> versions = this.baseMapper.selectList(new QueryWrapper<TFlogApplyCard>()
                .eq("bill_no", billNo)
                .eq("del_flag", 0)
                .orderByAsc("node_no"));
        if (!versions.isEmpty()) {
            dataScope.assertVisible(currentUserId(), versions.get(0).getSiteNo());
        }
        return versions;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteTFlogApplyCardByIds(String ids) {
        Long[] idArr = ConvertUtil.toLongArray(ids);
        List<TFlogApplyCard> cards = new ArrayList<>();
        List<String> sites = new ArrayList<>();
        for (Long id : idArr) {
            TFlogApplyCard card = this.baseMapper.selectById(id);
            if (card == null || (card.getDelFlag() != null && card.getDelFlag() == 1)) {
                continue;
            }
            dataScope.assertVisible(currentUserId(), card.getSiteNo());
            cards.add(card);
            if (!sites.contains(card.getSiteNo())) {
                sites.add(card.getSiteNo());
            }
        }
        if (cards.isEmpty()) {
            return 0;
        }
        // 跨档批量删：与保存口同一锁序，先排序锁齐相关年度账行再退额
        Map<String, TFlogQuotaYear> locked = lockQuotaYears(sites);
        int rows = 0;
        for (TFlogApplyCard card : cards) {
            // 现行且未压死的卡占着账，删除即把那笔原样退回其归属年度账
            if (card.getCurrentFlag() != null && card.getCurrentFlag() == CURRENT_YES
                    && !isPressed(card.getStatus())) {
                TFlogQuotaYear q = locked.get(card.getSiteNo() + "@" + chargeYearOf(card));
                if (q != null) {
                    applyOccupied(q, nullToZero(card.getQty()).negate());
                }
            }
            rows += this.baseMapper.update(null, new UpdateWrapper<TFlogApplyCard>()
                    .eq("id", card.getId())
                    .set("del_flag", 1));
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteTFlogApplyCardById(Long id) {
        return deleteTFlogApplyCardByIds(String.valueOf(id));
    }

    // ------------------------------------------------------------------
    // 唯一保存口
    // ------------------------------------------------------------------

    private FlogCardSaveVo saveCard(TFlogApplyCard input) {
        if (input == null) {
            throw new FlogBizException("送上来的卡是空的，没东西可立");
        }
        String opBy = ShiroUtils.getLoginName();
        Date now = new Date();
        int year = yearOf(now);

        // 1) 底档以字号为准核：单凭名号认同一家不算数
        TFlogStandDoc doc = requireStandDoc(input.getSiteId());
        dataScope.assertVisible(currentUserId(), doc.getSiteNo());
        if (doc.getStatus() != null && doc.getStatus() == 1) {
            // 年初被收回的档从此不再接纳新卡（含新报与重录），早年挂过的仍翻得到
            throw new FlogBizException("底档字号 " + doc.getSiteNo() + "（" + doc.getSiteName() + "）年初已收回，不再接纳新卡");
        }

        // 2) 照采伐方式当场点必填栏，缺一格都点名
        validateFields(input, doc);

        // 3) 先认一认重录源卡（只为摸清要锁哪几本账；锁内还会再认一遍）
        TFlogApplyCard srcHint = null;
        if (input.getReviseId() != null) {
            srcHint = this.baseMapper.selectById(input.getReviseId());
            if (srcHint == null || (srcHint.getDelFlag() != null && srcHint.getDelFlag() == 1)) {
                throw new FlogBizException("重录源卡 " + input.getReviseId() + " 不存在，旧版不许拿来覆写");
            }
            dataScope.assertVisible(currentUserId(), srcHint.getSiteNo());
        }

        // 4) 卡与额度走同一条线：把相关底档的全部年度账行按（年度, 字号）排序逐一 FOR UPDATE 锁齐，
        //    锁内再认旧版、核超额。并发两笔（含两片伐区同扣一笔、跨年重录退旧账）在锁上排队，
        //    谁先谁后落得清、扣过的赖不掉、同一小班不会立出两张现行卡；全局排序加锁防互锁死等。
        List<String> lockSites = new ArrayList<>();
        lockSites.add(doc.getSiteNo());
        if (srcHint != null && !lockSites.contains(srcHint.getSiteNo())) {
            lockSites.add(srcHint.getSiteNo());
        }
        Map<String, TFlogQuotaYear> locked = lockQuotaYears(lockSites);

        // 5) 锁内认旧版：认底档字号配小班号这一对，不论先后；带源卡id按源卡认（如改挂别家档）
        TFlogApplyCard oldCurrent = null;
        if (input.getReviseId() != null) {
            oldCurrent = this.baseMapper.selectById(input.getReviseId());
            if (oldCurrent == null || (oldCurrent.getDelFlag() != null && oldCurrent.getDelFlag() == 1)
                    || oldCurrent.getCurrentFlag() == null || oldCurrent.getCurrentFlag() != CURRENT_YES) {
                throw new FlogBizException("重录源卡 " + input.getReviseId()
                        + " 刚被另一笔重录顶下现行位，旧版只能翻检；请按最新版再录一版");
            }
            dataScope.assertVisible(currentUserId(), oldCurrent.getSiteNo());
        } else {
            oldCurrent = this.baseMapper.selectOne(new QueryWrapper<TFlogApplyCard>()
                    .eq("site_no", doc.getSiteNo())
                    .eq("subcomp_no", input.getSubcompNo())
                    .eq("current_flag", CURRENT_YES)
                    .eq("del_flag", 0)
                    .last("limit 1"));
        }

        String billNo;
        int nodeNo;
        String registerBy;
        Date registerTime;
        if (oldCurrent == null) {
            billNo = nextBillNo(year);
            nodeNo = 1;
            registerBy = opBy;
            registerTime = now;
        } else {
            billNo = oldCurrent.getBillNo();
            nodeNo = (oldCurrent.getNodeNo() == null ? 1 : oldCurrent.getNodeNo()) + 1;
            // 立卡人立卡一刻钉住，重录多少回都不挪窝
            registerBy = StringUtils.isNotEmpty(oldCurrent.getRegisterBy())
                    ? oldCurrent.getRegisterBy() : opBy;
            registerTime = oldCurrent.getRegisterTime() == null ? now : oldCurrent.getRegisterTime();
        }

        // 6) 取用顺位系统自己折算回填，不留人手口子
        int gradeLevel = calcGradeLevel(input.getQty(), doc);

        // 7) 锁内核额度。账按“当前版本录入那年”归：升版先退旧版那年那笔，新版占本年新笔。
        BigDecimal oldRelease = BigDecimal.ZERO;
        if (oldCurrent != null && !isPressed(oldCurrent.getStatus())) {
            TFlogQuotaYear oldQuota = locked.get(oldCurrent.getSiteNo() + "@" + chargeYearOf(oldCurrent));
            if (oldQuota == null) {
                throw new FlogBizException("卡号 " + billNo + " 旧版占用的 "
                        + chargeYearOf(oldCurrent) + " 年度账缺失，账对不平，本笔驳回");
            }
            oldRelease = nullToZero(oldCurrent.getQty());
            applyOccupied(oldQuota, oldRelease.negate());
        }
        int chargeYear = year;
        TFlogQuotaYear quota = locked.get(doc.getSiteNo() + "@" + chargeYear);
        if (quota == null) {
            throw new FlogBizException("底档字号 " + doc.getSiteNo() + " " + chargeYear
                    + " 年度额度账尚未下达，没数可扣，卡立不住");
        }
        BigDecimal occupied = nullToZero(quota.getOccupiedQty());
        BigDecimal total = nullToZero(quota.getTotalQty());
        BigDecimal afterOccupy = occupied.add(input.getQty());
        if (afterOccupy.compareTo(total) > 0) {
            throw new FlogBizException("卡号 " + billNo + " 本卡拟采 " + input.getQty()
                    + " 立方米，" + doc.getSiteNo() + " " + chargeYear + " 年新报已越过可占面子（本年可占 "
                    + total + "，尚存 " + total.subtract(occupied) + "），当场点这张卡的名，卡立不住");
        }

        // 6) 旧版退下另存，内容原样留着（含旧核验串、旧日子），不被填充器碰
        if (oldCurrent != null) {
            this.baseMapper.update(null, new UpdateWrapper<TFlogApplyCard>()
                    .eq("id", oldCurrent.getId())
                    .set("current_flag", CURRENT_NO));
        }

        // 7) 落新版行：编号、字号、顺位、串全由本层给，手塞的一律不存
        TFlogApplyCard card = new TFlogApplyCard();
        card.setBillNo(billNo);
        card.setNodeNo(nodeNo);
        card.setSiteId(doc.getId());
        card.setSiteNo(doc.getSiteNo());
        card.setSubcompNo(input.getSubcompNo().trim());
        card.setCutBlockNo(input.getCutBlockNo().trim());
        card.setCutMode(input.getCutMode());
        card.setClearTrees(input.getClearTrees());
        card.setGradYears(input.getGradYears());
        card.setGradInterval(input.getGradInterval());
        card.setSelectIntensity(input.getSelectIntensity());
        card.setSelectKeepTrees(input.getSelectKeepTrees());
        card.setTendMethod(input.getTendMethod());
        card.setTendPerHa(input.getTendPerHa());
        card.setRenewMode(input.getRenewMode());
        card.setRenewFinishYear(input.getRenewFinishYear());
        card.setReformTree(input.getReformTree());
        card.setQty(input.getQty());
        card.setFineAmt(input.getFineAmt());
        card.setGradeLevel(gradeLevel);
        card.setCurrentFlag(CURRENT_YES);
        card.setStatus(STATUS_PENDING);
        card.setDelFlag(0);
        card.setRegisterBy(registerBy);
        card.setRegisterTime(registerTime);
        // 核验串在卡走到“钉住（已核对）”那一步才成形：立卡、重录时这一格先空着，
        // 到核对成形按本版日子拼钉；重录一版日子挪到新版那天，旧版那一行留原样。
        card.setContent(null);
        card.setRemark(input.getRemark());
        this.baseMapper.insert(card);

        // 8) 权威已扣列在同一事务锁行改写：新版按新拟采占本年（旧版那笔已在上面退回其录入年）
        applyOccupied(quota, card.getQty());

        // 9) 办理流水续一笔，办理人单记一栏，与立卡人两不混
        int opType = oldCurrent == null ? OP_REGISTER : OP_REVISE;
        writeOp(card, opType, card.getQty(), card.getQty());

        String msg = oldCurrent == null
                ? "卡号 " + billNo + " 立卡，从 " + chargeYear + " 年度额度划走 " + card.getQty() + " 立方米"
                : "卡号 " + billNo + " 重录第 " + nodeNo + " 版，旧版退档留检；旧版 " + oldRelease
                + " 立方米退回其年度账，本年新占 " + card.getQty() + " 立方米";
        return buildVo(card, quota, card.getQty(), card.getQty(), msg);
    }

    /**
     * 照采伐方式逐栏点必填。方式没选、走皆伐没给面积等，一次把缺的格子点全。
     */
    private void validateFields(TFlogApplyCard r, TFlogStandDoc doc) {
        List<String> missing = new ArrayList<>();

        if (StringUtils.isEmpty(r.getSubcompNo())) {
            missing.add("小班号");
        }
        if (StringUtils.isEmpty(r.getCutBlockNo())) {
            missing.add("伐区名号");
        }
        FlogCutMode mode = FlogCutMode.of(r.getCutMode());
        if (mode == null) {
            missing.add("采伐方式（主伐-皆伐/渐伐/择伐、抚育采伐、更新采伐、低质低效林改造六选一）");
        }
        // 蓄积填零、填负数一样起不来
        if (r.getQty() == null) {
            missing.add("本卡合并后拟采蓄积");
        } else if (r.getQty().compareTo(BigDecimal.ZERO) <= 0) {
            throw new FlogBizException("本卡合并后拟采蓄积填的是 " + r.getQty()
                    + "，蓄积为零或为负卡立不住，须填正数");
        }

        if (mode != null) {
            switch (mode) {
                case CLEAR:
                    // 皆伐得给面积（走通用栏新报拟采面积）与株数
                    if (!FlogCutMode.positive(r.getFineAmt())) {
                        missing.add("皆伐·本卡新报拟采面积");
                    }
                    if (!FlogCutMode.positive(r.getClearTrees())) {
                        missing.add("皆伐·株数");
                    }
                    break;
                case GRADUAL:
                    if (!FlogCutMode.positive(r.getGradYears())) {
                        missing.add("渐伐·分年次数");
                    }
                    if (!FlogCutMode.positive(r.getGradInterval())) {
                        missing.add("渐伐·间隔年数");
                    }
                    break;
                case SELECTIVE:
                    if (!FlogCutMode.positive(r.getSelectIntensity())) {
                        missing.add("择伐·择伐强度");
                    }
                    if (!FlogCutMode.positive(r.getSelectKeepTrees())) {
                        missing.add("择伐·保留株数");
                    }
                    break;
                case TENDING:
                    if (StringUtils.isEmpty(r.getTendMethod())) {
                        missing.add("抚育采伐·抚育办法");
                    }
                    if (!FlogCutMode.positive(r.getTendPerHa())) {
                        missing.add("抚育采伐·每公顷采出量");
                    }
                    break;
                case RENEWAL:
                    if (StringUtils.isEmpty(r.getRenewMode())) {
                        missing.add("更新采伐·更新方式");
                    }
                    if (!FlogCutMode.positive(r.getRenewFinishYear())) {
                        missing.add("更新采伐·完成年度");
                    }
                    break;
                case REFORM:
                    if (StringUtils.isEmpty(r.getReformTree())) {
                        missing.add("低质低效林改造·目标树种");
                    }
                    break;
                default:
                    break;
            }
        }

        if (doc.getTh1Max() == null || doc.getTh2Max() == null || doc.getTh3Max() == null) {
            throw new FlogBizException("底档字号 " + doc.getSiteNo() + " 档内轻/中/重三条线没压全，折不出取用顺位");
        }
        if (!missing.isEmpty()) {
            throw new FlogBizException("卡立不住，缺了便起不来的格子：" + String.join("、", missing)
                    + "。请补齐再报，别丢一句“数不像话”搪塞");
        }
    }

    /**
     * 挨着界往高里数：与中度线一样高落重度；越过重度线也一律算重度。
     * qty &lt; th1 轻度；th1 &lt;= qty &lt; th2 中度；qty &gt;= th2 重度（含越线）。
     */
    private int calcGradeLevel(BigDecimal qty, TFlogStandDoc doc) {
        if (qty.compareTo(doc.getTh2Max()) >= 0) {
            return 3;
        }
        if (qty.compareTo(doc.getTh1Max()) >= 0) {
            return 2;
        }
        return 1;
    }

    /**
     * 核验串：卡号 + 版本序 + 字号 + 小班 + 蓄积 + 本版日子，再压一截指纹。
     * 两版共用一串便是错的——版本序与日子都在串里。
     */
    private String buildContent(TFlogApplyCard card, Date versionDate) {
        String day = new SimpleDateFormat("yyyyMMdd").format(versionDate);
        String raw = card.getBillNo() + "|V" + card.getNodeNo() + "|" + card.getSiteNo() + "|"
                + card.getSubcompNo() + "|" + card.getQty() + "|" + day;
        String fp = MD5Util.encode(raw).substring(0, 8).toUpperCase();
        return "FK[" + card.getBillNo() + "]-V" + card.getNodeNo() + "-" + day + "-" + fp;
    }

    private void writeOp(TFlogApplyCard card, int changeType, BigDecimal qty, BigDecimal occupyQty) {
        TFlogCardOp op = new TFlogCardOp();
        op.setCardId(card.getId());
        op.setBillNo(card.getBillNo());
        op.setNodeNo(card.getNodeNo());
        op.setChangeType(changeType);
        op.setQty(qty);
        op.setOccupyQty(occupyQty);
        op.setOpBy(ShiroUtils.getLoginName());
        op.setOpTime(new Date());
        op.setDelFlag(0);
        cardOpMapper.insert(op);
    }

    private FlogCardSaveVo buildVo(TFlogApplyCard card, BigDecimal occupyQty, BigDecimal delta, String msg) {
        int year = chargeYearOf(card);
        TFlogQuotaYear quota = quotaYearMapper.selectOne(new QueryWrapper<TFlogQuotaYear>()
                .eq("site_no", card.getSiteNo())
                .eq("quota_year", year)
                .eq("del_flag", 0)
                .last("limit 1"));
        return buildVo(card, quota, occupyQty, delta, msg);
    }

    /**
     * 给回里的本年可占/已扣/尚存一律取自额度账权威列（与扣退同一口径），页面不自算。
     */
    private FlogCardSaveVo buildVo(TFlogApplyCard card, TFlogQuotaYear quota, BigDecimal occupyQty,
                                   BigDecimal delta, String msg) {
        BigDecimal total = quota == null ? BigDecimal.ZERO : nullToZero(quota.getTotalQty());
        BigDecimal occupied = quota == null ? BigDecimal.ZERO : nullToZero(quota.getOccupiedQty());
        FlogCardSaveVo vo = new FlogCardSaveVo();
        vo.setBillNo(card.getBillNo());
        vo.setNodeNo(card.getNodeNo());
        vo.setSiteNo(card.getSiteNo());
        vo.setSubcompNo(card.getSubcompNo());
        vo.setCutMode(card.getCutMode());
        vo.setQty(card.getQty());
        vo.setGradeLevel(card.getGradeLevel());
        vo.setStatus(card.getStatus());
        vo.setContent(card.getContent());
        vo.setRegisterBy(card.getRegisterBy());
        vo.setRegisterTime(card.getRegisterTime());
        vo.setOpBy(ShiroUtils.getLoginName());
        vo.setOccupyQty(occupyQty);
        vo.setDeltaQty(delta);
        vo.setYearTotalQty(total);
        // 已扣与尚存同一回计算，页面自己数的一律不算
        vo.setOccupiedQty(occupied);
        vo.setRemainQty(total.subtract(occupied));
        vo.setMessage(msg);
        return vo;
    }

    /**
     * 锁年度账行（FOR UPDATE）：同一档同年的扣/退在事务里串行，
     * 两片伐区并报也排得出先后，扣过的不会再被第二回当空额重占。
     */
    private TFlogQuotaYear lockQuotaYear(String siteNo, int year) {
        TFlogQuotaYear quota = quotaYearMapper.selectOne(new QueryWrapper<TFlogQuotaYear>()
                .eq("site_no", siteNo)
                .eq("quota_year", year)
                .eq("del_flag", 0)
                .last("limit 1 for update"));
        if (quota == null) {
            throw new FlogBizException("底档字号 " + siteNo + " " + year + " 年度额度账尚未下达，没数可扣，卡立不住");
        }
        return quota;
    }

    /**
     * 把若干底档名下所有年度账行按（年度, 字号）排序后逐行 FOR UPDATE 锁齐，
     * 返回 siteNo@year -> 账行。跨年重录退旧账、同档并发报卡都在这同一把锁序内。
     */
    private Map<String, TFlogQuotaYear> lockQuotaYears(List<String> siteNos) {
        List<String> distinct = new ArrayList<>();
        for (String s : siteNos) {
            if (!distinct.contains(s)) {
                distinct.add(s);
            }
        }
        List<TFlogQuotaYear> rows = quotaYearMapper.selectList(new QueryWrapper<TFlogQuotaYear>()
                .in("site_no", distinct)
                .eq("del_flag", 0)
                .orderByAsc("quota_year", "site_no")
                .last("for update"));
        Map<String, TFlogQuotaYear> map = new HashMap<>();
        for (TFlogQuotaYear q : rows) {
            map.put(q.getSiteNo() + "@" + q.getQuotaYear(), q);
        }
        // 至少本年新账得在；缺账时由后续核额点名
        return map;
    }

    /**
     * 权威已扣列锁行增减：delta 正为划走、负为退回；不得越过可占面子、不得退成负数。
     */
    private void applyOccupied(TFlogQuotaYear quota, BigDecimal delta) {
        BigDecimal after = nullToZero(quota.getOccupiedQty()).add(delta);
        if (after.compareTo(BigDecimal.ZERO) < 0) {
            // 账对不上时宁可挡住，不许把额度退穿——正是“扣过的不认账”那类旧弊的防线
            throw new FlogBizException(quota.getSiteNo() + " " + quota.getQuotaYear()
                    + " 年度账已扣合计将被退成负数，账对不平，本笔驳回");
        }
        quotaYearMapper.update(null, new UpdateWrapper<TFlogQuotaYear>()
                .eq("id", quota.getId())
                .set("occupied_qty", after));
        quota.setOccupiedQty(after);
    }

    /** 当前版本占用归属年度：该版本录入那年（跨年仅压死/翻检，不挪账）。 */
    private int chargeYearOf(TFlogApplyCard card) {
        Date t = card.getCreateTime() == null ? card.getRegisterTime() : card.getCreateTime();
        return yearOf(t == null ? new Date() : t);
    }

    private boolean isPressed(Integer status) {
        return status != null && status == STATUS_PRESSED;
    }

    private TFlogApplyCard requireCurrentCard(Long id) {
        if (id == null) {
            throw new FlogBizException("没给卡号，办不了这一笔");
        }
        TFlogApplyCard card = this.baseMapper.selectOne(new QueryWrapper<TFlogApplyCard>()
                .eq("id", id)
                .eq("current_flag", CURRENT_YES)
                .eq("del_flag", 0));
        if (card == null) {
            throw new FlogBizException("这张卡不存在、已删或不是现行版本，旧版只能翻检不能办理");
        }
        dataScope.assertVisible(currentUserId(), card.getSiteNo());
        return card;
    }

    private TFlogStandDoc requireStandDoc(Long siteId) {
        if (siteId == null) {
            throw new FlogBizException("卡立不住，缺了便起不来的格子：所属编制单位在册底档");
        }
        TFlogStandDoc doc = flogStandDocMapper.selectOne(new QueryWrapper<TFlogStandDoc>()
                .eq("id", siteId)
                .eq("del_flag", 0)
                .last("limit 1"));
        if (doc == null) {
            throw new FlogBizException("底档 id " + siteId + " 查无在册底档，卡挂不上去");
        }
        return doc;
    }

    private TFlogStandDoc requireStandDocByNo(String siteNo) {
        TFlogStandDoc doc = flogStandDocMapper.selectOne(new QueryWrapper<TFlogStandDoc>()
                .eq("site_no", siteNo)
                .eq("del_flag", 0)
                .last("limit 1"));
        if (doc == null) {
            throw new FlogBizException("底档字号 " + siteNo + " 查无在册底档");
        }
        return doc;
    }

    private TFlogQuotaYear requireQuotaYear(String siteNo, int year) {
        TFlogQuotaYear quota = quotaYearMapper.selectOne(new QueryWrapper<TFlogQuotaYear>()
                .eq("site_no", siteNo)
                .eq("quota_year", year)
                .eq("del_flag", 0)
                .last("limit 1"));
        if (quota == null) {
            throw new FlogBizException("底档字号 " + siteNo + " " + year + " 年度额度账尚未下达，没数可扣，卡立不住");
        }
        return quota;
    }

    private List<TFlogApplyCard> listOccupyCards(String siteNo, int year) {
        // 权威账是 quota_year.occupied_qty；这里只取该年占着额度的现行未压死卡作同屏摆示，
        // 归年口径与扣退一致：按当前版本录入那年。
        List<TFlogApplyCard> all = this.baseMapper.selectList(new QueryWrapper<TFlogApplyCard>()
                .eq("site_no", siteNo)
                .eq("current_flag", CURRENT_YES)
                .ne("status", STATUS_PRESSED)
                .eq("del_flag", 0));
        List<TFlogApplyCard> rs = new ArrayList<>();
        for (TFlogApplyCard c : all) {
            if (chargeYearOf(c) == year) {
                rs.add(c);
            }
        }
        return rs;
    }

    private String nextBillNo(int year) {
        // 建档卡号自动生成、撞号重试；人手塞的号在调用处根本不进门
        for (int i = 0; i < 10; i++) {
            String no = generateBillNo(year, i);
            Integer cnt = this.baseMapper.selectCount(new QueryWrapper<TFlogApplyCard>()
                    .eq("bill_no", no)
                    .eq("del_flag", 0));
            if (cnt == null || cnt == 0) {
                return no;
            }
        }
        throw new FlogBizException("建档卡号连撞，缓一缓再报");
    }

    private String generateBillNo(int year, int salt) {
        long tail = Math.abs((SnowflakeIdWorker.getUUID() + "-" + salt).hashCode()) % 100000000L;
        return "FK" + year + "-" + String.format("%08d", tail);
    }

    private TFlogApplyCard toEntity(FlogCardSaveDto dto) {
        TFlogApplyCard e = new TFlogApplyCard();
        e.setReviseId(dto.getReviseId());
        if (dto.getSiteId() != null) {
            e.setSiteId(dto.getSiteId());
        }
        e.setSubcompNo(dto.getSubcompNo());
        e.setCutBlockNo(dto.getCutBlockNo());
        e.setCutMode(dto.getCutMode());
        e.setQty(dto.getQty());
        e.setFineAmt(dto.getFineAmt());
        e.setClearTrees(dto.getClearTrees());
        e.setGradYears(dto.getGradYears());
        e.setGradInterval(dto.getGradInterval());
        e.setSelectIntensity(dto.getSelectIntensity());
        e.setSelectKeepTrees(dto.getSelectKeepTrees());
        e.setTendMethod(dto.getTendMethod());
        e.setTendPerHa(dto.getTendPerHa());
        e.setRenewMode(dto.getRenewMode());
        e.setRenewFinishYear(dto.getRenewFinishYear());
        e.setReformTree(dto.getReformTree());
        e.setRemark(dto.getRemark());
        return e;
    }

    private Long currentUserId() {
        try {
            return ShiroUtils.getUserId();
        } catch (Exception e) {
            return null;
        }
    }

    private int yearOf(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal.get(Calendar.YEAR);
    }

    private BigDecimal nullToZero(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
