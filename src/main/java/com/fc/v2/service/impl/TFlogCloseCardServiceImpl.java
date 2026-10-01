package com.fc.v2.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fc.v2.common.exception.biz.FlogBizException;
import com.fc.v2.mapper.auto.TFlogApplyCardMapper;
import com.fc.v2.mapper.auto.TFlogCloseCardMapper;
import com.fc.v2.mapper.auto.TFlogCloseJournalMapper;
import com.fc.v2.mapper.auto.TFlogSurveyDesignMapper;
import com.fc.v2.mapper.auto.TFlogSurveyResultMapper;
import com.fc.v2.mapper.auto.TFlogSurveyTreeMapper;
import com.fc.v2.model.auto.TFlogApplyCard;
import com.fc.v2.model.auto.TFlogCloseCard;
import com.fc.v2.model.auto.TFlogCloseJournal;
import com.fc.v2.model.auto.TFlogSurveyDesign;
import com.fc.v2.model.auto.TFlogSurveyResult;
import com.fc.v2.model.custom.flog.FlogCardSaveVo;
import com.fc.v2.model.custom.flog.FlogCloseCardVo;
import com.fc.v2.model.custom.flog.FlogCloseLedgerVo;
import com.fc.v2.model.custom.flog.FlogCloseOpenDto;
import com.fc.v2.model.custom.flog.FlogClosePassDto;
import com.fc.v2.service.ITFlogApplyCardService;
import com.fc.v2.service.ITFlogCloseCardService;
import com.fc.v2.service.flog.FlogDataScope;
import com.fc.v2.shiro.util.ShiroUtils;
import com.fc.v2.util.MD5Util;
import com.fc.v2.util.SnowflakeIdWorker;
import com.fc.v2.util.StringUtils;

/**
 * 伐区勾销事务卡 Service业务层处理（state-machine 形状：单据流转）。
 *
 * 往前推进一格、往回退一格各只有一个落点，全收在 advance/rollback 这一对方法里。
 * 卡此刻停在哪一段，照“没过的过口履历”自己算成，再用条件更新钉住；
 * 屏上摆的、经办人心里认的都当不得真，改没改成只看这对方法的给回。
 *
 * 履历只追加：过口/止步各续一笔，回退改口把旧那笔用一笔“沉底”事件沉掉，旧行原样留库。
 * 一笔一枚指纹、首尾相扣；止步封卡、了断封卡；勾销卡不许抽走，要动只能重新起一张。
 *
 * @author fuce
 * @date 2026-09-14
 */
@Service
public class TFlogCloseCardServiceImpl implements ITFlogCloseCardService {

    /** 道口：报伐 */
    private static final int STAGE_REPORT = 0;
    /** 道口：核株 */
    private static final int STAGE_CHECK = 1;
    /** 道口：验更 */
    private static final int STAGE_RENEW = 2;
    /** 道口：了断 */
    private static final int STAGE_CLOSE = 3;
    private static final int STAGE_TOTAL = 4;
    private static final String[] STAGE_NAMES = {"报伐", "核株", "验更", "了断"};

    /** 落定：在办 */
    private static final int STATUS_ACTIVE = 1;
    /** 落定：止步（这一段不过了，过后续不进去） */
    private static final int STATUS_HALT = 2;
    /** 落定：已了断（各栏封死） */
    private static final int STATUS_CLOSED = 3;
    private static final String[] STATUS_NAMES = {"", "在办", "止步", "了断"};

    /** 履历笔目：过口 */
    private static final int ACTION_PASS = 1;
    /** 履历笔目：止步 */
    private static final int ACTION_HALT = 2;
    /** 履历笔目：沉底（回退改口，旧过口笔被沉掉但留库） */
    private static final int ACTION_SINK = 3;

    /** 核株：站里定的那一格——实采与设计相差比率过此线，就得留一句说法（等于线不算过） */
    private static final BigDecimal DIFF_GRID = new BigDecimal("0.05");

    /** 建档卡：现行版本 */
    private static final int APPLY_CURRENT_YES = 1;
    /** 建档卡：已压死 */
    private static final int APPLY_PRESSED = 2;

    @Autowired
    private TFlogCloseCardMapper closeCardMapper;

    @Autowired
    private TFlogCloseJournalMapper journalMapper;

    @Autowired
    private TFlogSurveyDesignMapper surveyDesignMapper;

    @Autowired
    private TFlogSurveyResultMapper surveyResultMapper;

    @Autowired
    private TFlogSurveyTreeMapper surveyTreeMapper;

    @Autowired
    private TFlogApplyCardMapper applyCardMapper;

    @Autowired
    private ITFlogApplyCardService applyCardService;

    @Autowired
    private FlogDataScope dataScope;

    @Override
    public TFlogCloseCard selectTFlogCloseCardById(Long id) {
        TFlogCloseCard card = this.closeCardMapper.selectOne(new QueryWrapper<TFlogCloseCard>()
                .eq("id", id)
                .eq("del_flag", 0));
        if (card != null) {
            dataScope.assertVisible(currentUserId(), card.getSiteNo());
        }
        return card;
    }

    @Override
    public List<TFlogCloseCard> selectTFlogCloseCardList(QueryWrapper<TFlogCloseCard> queryWrapper) {
        QueryWrapper<TFlogCloseCard> wrapper = queryWrapper == null ? new QueryWrapper<>() : queryWrapper;
        wrapper.eq("del_flag", 0);
        if (!dataScope.isSuperior()) {
            List<String> siteNos = dataScope.visibleSiteNos(currentUserId());
            if (siteNos.isEmpty()) {
                return new ArrayList<>();
            }
            wrapper.in("site_no", siteNos);
        }
        wrapper.orderByDesc("create_time");
        return this.closeCardMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FlogCloseCardVo open(FlogCloseOpenDto dto) {
        if (dto == null) {
            throw new FlogBizException("送上来的是空的，没卡可起");
        }
        if (StringUtils.isEmpty(dto.getSiteNo()) || StringUtils.isEmpty(dto.getSubcompNo())) {
            throw new FlogBizException("起卡得钉住底档字号与小班号这一对，缺一个都立不起来");
        }
        String siteNo = dto.getSiteNo().trim();
        String subcompNo = dto.getSubcompNo().trim();
        dataScope.assertVisible(currentUserId(), siteNo);

        // 锁这一小班名下所有勾销卡再数活卡：并发两笔起卡在锁上排队，不会同时立出两张活卡
        List<TFlogCloseCard> same = this.closeCardMapper.selectList(new QueryWrapper<TFlogCloseCard>()
                .eq("site_no", siteNo)
                .eq("subcomp_no", subcompNo)
                .eq("del_flag", 0)
                .last("for update"));
        for (TFlogCloseCard c : same) {
            if (c.getStatus() != null && c.getStatus() == STATUS_ACTIVE) {
                throw new FlogBizException("小班 " + siteNo + "/" + subcompNo
                        + " 已压着一张活卡 " + c.getBizNo() + "（停在" + STAGE_NAMES[stageOf(c)]
                        + "）；一张小班同时只许一张活卡，头一张了断或止步后才许另起");
            }
        }

        TFlogCloseCard card = new TFlogCloseCard();
        card.setBizNo(nextBizNo());
        card.setSiteNo(siteNo);
        card.setSubcompNo(subcompNo);
        card.setCutBlockNo(StringUtils.isEmpty(dto.getCutBlockNo()) ? null : dto.getCutBlockNo().trim());
        card.setStage(STAGE_REPORT);
        card.setStatus(STATUS_ACTIVE);
        // 活卡锚唯一：上面那遍数活卡挡得住顺序两笔，并发两笔由这一枚唯一索引顶回一笔
        card.setActiveAnchor(activeAnchor(siteNo, subcompNo));
        card.setDelFlag(0);
        card.setRemark(dto.getRemark());
        try {
            this.closeCardMapper.insert(card);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            throw new FlogBizException("小班 " + siteNo + "/" + subcompNo
                    + " 刚被另一笔立了活卡；一张小班同时只许一张活卡，头一张了断或止步后才许另起");
        }

        return buildVo(card, STAGE_REPORT, null, null,
                "勾销卡 " + card.getBizNo() + " 已起，小班 " + siteNo + "/" + subcompNo
                        + "，头一道口报伐待过；四段只准挨着行");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TFlogCloseCard advance(Long id, String remark) {
        // 旧入参照样能调：空口推当前一格。报伐/核株/验更有该报的格子，缺了当场点回；了断无格，走得通。
        FlogClosePassDto dto = new FlogClosePassDto();
        dto.setId(id);
        dto.setRemark(remark);
        advance(dto);
        return this.closeCardMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FlogCloseCardVo advance(FlogClosePassDto dto) {
        if (dto == null || dto.getId() == null) {
            throw new FlogBizException("没认出是哪张卡，这一口过不了");
        }
        TFlogCloseCard card = lockCard(dto.getId());
        Derived d = derive(card);
        if (d.status != STATUS_ACTIVE) {
            throw new FlogBizException("勾销卡 " + card.getBizNo() + " 已" + STATUS_NAMES[d.status]
                    + "，各栏封死，续不进去、改不动；要办只能重新起一张卡");
        }
        int s = d.stage;
        if (dto.getStage() != null && dto.getStage() != s) {
            int claimed = dto.getStage();
            // 同一段过第二遍（那段头一遍已在册）：库里只认头一遍，本笔不往卡上落一句记事，
            // 照原样把卡给回；真要改口，先回退到那一段重做
            if (claimed >= 0 && claimed < STAGE_TOTAL && claimed < s && d.alivePass[claimed] != null) {
                FlogCloseCardVo dup = buildVo(card, s, null, null,
                        STAGE_NAMES[claimed] + " 头一遍已经过成，库里只认头一遍，第二遍不再往卡上落记事；"
                                + "真要改口，先回退到" + STAGE_NAMES[claimed] + "重做，旧那笔沉底留库");
                dup.setDuplicate(Boolean.TRUE);
                return dup;
            }
            // 跳段的、倒着走到已沉底段的，一概不算过成
            throw new FlogBizException("卡眼下停在" + STAGE_NAMES[s] + "，"
                    + (claimed >= 0 && claimed < STAGE_TOTAL ? STAGE_NAMES[claimed] : ("道口" + claimed))
                    + " 还没轮到；四段只准挨着行，跳段、倒走都不算过成");
        }

        TFlogCloseJournal j = newPassJournal(card, s, attemptOf(card.getId(), s), dto.getRemark());
        switch (s) {
            case STAGE_REPORT:
                passReport(card, dto, j);
                break;
            case STAGE_CHECK:
                passCheck(card, d, dto, j);
                break;
            case STAGE_RENEW:
                passRenew(card, dto, j);
                break;
            case STAGE_CLOSE:
                return passClose(card, d, j);
            default:
                throw new FlogBizException("道口 " + s + " 不认");
        }

        // 条件更新只许在办的卡从 s 落成 s+1；并发第二笔抢不到，重报落不上第二句
        int rows = this.closeCardMapper.update(null, new UpdateWrapper<TFlogCloseCard>()
                .eq("id", card.getId())
                .eq("status", STATUS_ACTIVE)
                .eq("stage", s)
                .set("stage", s + 1));
        if (rows == 0) {
            throw new FlogBizException("卡 " + card.getBizNo() + " 刚被另一笔挪动，本笔请刷新后再看");
        }
        card.setStage(s + 1);
        appendJournal(j, d);
        return buildVo(card, s + 1, null, null,
                "卡号 " + card.getBizNo() + " " + STAGE_NAMES[s] + " 已过，下一道口"
                        + (s + 1 < STAGE_TOTAL ? STAGE_NAMES[s + 1] : "——四段走完即了断") + "待过");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TFlogCloseCard rollback(Long id, String remark) {
        rollback(id, null, remark);
        return this.closeCardMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FlogCloseCardVo rollback(Long id, Integer stage, String reason) {
        if (id == null) {
            throw new FlogBizException("没认出是哪张卡，退不了");
        }
        TFlogCloseCard card = lockCard(id);
        Derived d = derive(card);
        if (d.status != STATUS_ACTIVE) {
            throw new FlogBizException("勾销卡 " + card.getBizNo() + " 已" + STATUS_NAMES[d.status]
                    + "，封了卡就没有回退这一说");
        }
        int s = d.stage;
        if (s <= STAGE_REPORT) {
            throw new FlogBizException("卡 " + card.getBizNo() + " 还压在头一道口报伐，前头没有段可退");
        }
        int target = s - 1;
        if (stage != null && stage != target) {
            // 回退只有一个落点：当前道口的头一段。想退到别处不算退成
            throw new FlogBizException("回退只落得回" + STAGE_NAMES[target] + "，"
                    + (stage >= 0 && stage < STAGE_TOTAL ? STAGE_NAMES[stage] : ("道口" + stage))
                    + " 不是这一回的落点");
        }
        TFlogCloseJournal old = d.alivePass[target];
        if (old == null) {
            throw new FlogBizException("回退无着：" + STAGE_NAMES[target] + " 那笔过口履历翻不出来，账对不上，不敢退");
        }

        // 沉底事件：指住被沉掉的那笔过口；旧行原样留库，不删不改，与后补的不摆在一处
        TFlogCloseJournal sink = new TFlogCloseJournal();
        sink.setCardId(card.getId());
        sink.setBizNo(card.getBizNo());
        sink.setStage(target);
        sink.setAttemptNo(old.getAttemptNo());
        sink.setAction(ACTION_SINK);
        sink.setRefSeq(old.getSeq());
        sink.setRemark(reason);
        appendJournal(sink, d);

        int rows = this.closeCardMapper.update(null, new UpdateWrapper<TFlogCloseCard>()
                .eq("id", card.getId())
                .eq("status", STATUS_ACTIVE)
                .eq("stage", s)
                .set("stage", target));
        if (rows == 0) {
            throw new FlogBizException("卡 " + card.getBizNo() + " 刚被另一笔挪动，本笔请刷新后再看");
        }
        card.setStage(target);
        return buildVo(card, target, null, null,
                "卡号 " + card.getBizNo() + " 已退回" + STAGE_NAMES[target] + "：原那笔过口记事沉底留库，"
                        + "不与后补的摆在一处；这一段按第 " + (old.getAttemptNo() + 1) + " 遍重做");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FlogCloseCardVo halt(Long id, Integer stage, String reason) {
        if (id == null) {
            throw new FlogBizException("没认出是哪张卡，止不了");
        }
        if (StringUtils.isEmpty(reason)) {
            throw new FlogBizException("止步得留下一句“这一段不过了”的缘故，空白止不住");
        }
        TFlogCloseCard card = lockCard(id);
        Derived d = derive(card);
        if (d.status == STATUS_CLOSED) {
            throw new FlogBizException("勾销卡 " + card.getBizNo() + " 已了断，了断没有止步这一说");
        }
        if (d.status == STATUS_HALT) {
            throw new FlogBizException("勾销卡 " + card.getBizNo() + " 已止步，止步卡续不进去；要办另起一张卡");
        }
        int s = d.stage;
        if (stage != null && stage != s) {
            throw new FlogBizException("卡眼下停在" + STAGE_NAMES[s] + "，止步只能写在这一段上");
        }

        TFlogCloseJournal j = new TFlogCloseJournal();
        j.setCardId(card.getId());
        j.setBizNo(card.getBizNo());
        j.setStage(s);
        j.setAttemptNo(attemptOf(card.getId(), s));
        j.setAction(ACTION_HALT);
        j.setRemark(reason.trim());
        appendJournal(j, d);

        int rows = this.closeCardMapper.update(null, new UpdateWrapper<TFlogCloseCard>()
                .eq("id", card.getId())
                .eq("status", STATUS_ACTIVE)
                .eq("stage", s)
                .set("status", STATUS_HALT)
                .set("halt_reason", reason.trim())
                .set("active_anchor", null));
        if (rows == 0) {
            throw new FlogBizException("卡 " + card.getBizNo() + " 刚被另一笔挪动，本笔请刷新后再看");
        }
        card.setStatus(STATUS_HALT);
        card.setStage(s);
        card.setHaltReason(reason.trim());
        return buildVo(card, s, null, null,
                "卡号 " + card.getBizNo() + " 在" + STAGE_NAMES[s] + " 止步：「" + reason.trim()
                        + "」。卡就此止步，过后谁也续不进去；这一小班要办，另起一张卡");
    }

    @Override
    public FlogCloseLedgerVo ledger(Long id) {
        if (id == null) {
            throw new FlogBizException("没认出是哪张卡，翻不出履历");
        }
        TFlogCloseCard card = this.closeCardMapper.selectOne(new QueryWrapper<TFlogCloseCard>()
                .eq("id", id)
                .eq("del_flag", 0));
        if (card == null) {
            throw new FlogBizException("这张勾销卡不存在或已被抽走");
        }
        dataScope.assertVisible(currentUserId(), card.getSiteNo());

        List<TFlogCloseJournal> lines = this.journalMapper.selectList(new QueryWrapper<TFlogCloseJournal>()
                .eq("card_id", id)
                .eq("del_flag", 0)
                .orderByAsc("seq"));

        FlogCloseLedgerVo vo = new FlogCloseLedgerVo();
        vo.setCard(card);
        Map<Integer, TFlogCloseJournal> bySeq = new HashMap<>();
        Set<Integer> sunkRefs = new HashSet<>();
        // 每段已见的在册过口笔：同段冒出第二笔在册过口，就是“凭空多出来”（改口必有沉底笔）
        Map<Integer, TFlogCloseJournal> alivePassSeen = new HashMap<>();
        // 每段过口遍次须从 1 挨号
        Map<Integer, Integer> passCountByStage = new HashMap<>();
        String prevFp = null;
        boolean halted = false;
        for (TFlogCloseJournal j : lines) {
            bySeq.put(j.getSeq(), j);
            boolean alive = !(j.getAction() != null && j.getAction() == ACTION_SINK);
            String problem = null;

            // 1) 断号/乱序：序得从 1 挨着号，缺一页这里就对不上
            if (j.getSeq() == null || j.getSeq() != vo.getLines().size() + 1) {
                problem = "第 " + (vo.getLines().size() + 1) + " 笔缺页或乱序，翻见的却是流水序 " + j.getSeq();
            }
            // 2) 指纹链：上一枚接不上、本枚算不拢，都是过后动过或手补
            if (problem == null) {
                String expectPrev = vo.getLines().isEmpty() ? null
                        : vo.getLines().get(vo.getLines().size() - 1).getFp();
                if (!eqFp(prevFp, j.getPrevFp()) || !eqFp(expectPrev, j.getPrevFp())) {
                    problem = "流水序 " + j.getSeq() + " 的上一枚指纹接不上，链断了";
                } else if (!eqFp(fingerprint(j, j.getPrevFp()), j.getFp())) {
                    problem = "流水序 " + j.getSeq() + " 的指纹与所记各栏算不拢，这一笔过后被改过或补过";
                }
            }
            // 3) 止步之后不许再有任何一笔
            if (problem == null && halted) {
                problem = "止步（流水序 " + (j.getSeq() - 1) + "）之后又冒出一笔，止步卡续不进去才对";
            }
            // 4) 过口：遍次挨号、段次在四段内、同段不得两笔在册
            if (problem == null && j.getAction() != null && j.getAction() == ACTION_PASS) {
                int st = j.getStage() == null ? -1 : j.getStage();
                int seen = passCountByStage.getOrDefault(st, 0) + 1;
                passCountByStage.put(st, seen);
                if (j.getAttemptNo() == null || j.getAttemptNo() != seen) {
                    problem = "流水序 " + j.getSeq() + "（" + stageName(st) + "）标的遍次是 "
                            + j.getAttemptNo() + "，按序应是第 " + seen + " 遍，遍次断了号";
                } else if (st < 0 || st >= STAGE_TOTAL) {
                    problem = "流水序 " + j.getSeq() + " 的段次 " + st + " 不在四段之内";
                } else if (alivePassSeen.containsKey(st)) {
                    problem = stageName(st) + " 段凭空多出一笔在册过口（流水序 " + j.getSeq()
                            + "）：改口必有沉底笔把流水序 " + alivePassSeen.get(st).getSeq() + " 沉掉";
                } else {
                    alivePassSeen.put(st, j);
                }
            }
            // 5) 沉底：必须指得住一笔同段过口，且一笔只许沉一回
            if (problem == null && j.getAction() != null && j.getAction() == ACTION_SINK) {
                TFlogCloseJournal ref = j.getRefSeq() == null ? null : bySeq.get(j.getRefSeq());
                if (ref == null || (ref.getAction() != null && ref.getAction() != ACTION_PASS)
                        || !safeEq(ref.getStage(), j.getStage())) {
                    problem = "流水序 " + j.getSeq() + " 的沉底指不住一笔同段过口，凭空多出来半句";
                } else if (sunkRefs.contains(j.getRefSeq())) {
                    problem = "流水序 " + j.getRefSeq() + " 那笔过口被沉了两回，多出来的不算数";
                } else {
                    sunkRefs.add(j.getRefSeq());
                    alivePassSeen.remove(ref.getStage());
                }
            }
            // 6) 止步笔只许写在那时头一道没在册过口的段上（在办卡上不能跳着/倒着止）
            if (problem == null && j.getAction() != null && j.getAction() == ACTION_HALT) {
                int expectOpen = STAGE_CLOSE;
                for (int s = 0; s < STAGE_TOTAL; s++) {
                    if (!alivePassSeen.containsKey(s)) {
                        expectOpen = s;
                        break;
                    }
                }
                if (j.getStage() == null || j.getStage() != expectOpen) {
                    problem = "止步笔（流水序 " + j.getSeq() + "）写在" + stageName(j.getStage())
                            + "，可那时停着的道口是" + STAGE_NAMES[expectOpen] + "，倒着/跳着止的不算";
                }
                halted = true;
            }

            if (problem != null && vo.isIntact()) {
                vo.setIntact(false);
                vo.setBreakSeq(j.getSeq());
                vo.setBreakReason(problem);
            }
            vo.getLines().add(j);
            vo.getAlive().add(alive);
            prevFp = j.getFp();
        }

        // 被沉底笔指住的过口也不在册（沉底事件本身不算活笔），与后补的不摆在一处
        for (int i = 0; i < vo.getLines().size(); i++) {
            TFlogCloseJournal j = vo.getLines().get(i);
            if (j.getAction() != null && j.getAction() == ACTION_PASS && sunkRefs.contains(j.getSeq())) {
                vo.getAlive().set(i, false);
            }
        }

        // 7) 卡面与履历算成的落点对不上，也是动过
        if (vo.isIntact()) {
            Derived calc = derive(card, lines);
            if (card.getStatus() == null || card.getStatus() != calc.status
                    || card.getStage() == null || card.getStage() != calc.stage) {
                vo.setIntact(false);
                vo.setBreakReason("卡面停在「" + (card.getStatus() == null ? "?" : STATUS_NAMES[card.getStatus()])
                        + "/" + stageName(card.getStage()) + "」，履历算出来却是「"
                        + STATUS_NAMES[calc.status] + "/" + STAGE_NAMES[calc.stage]
                        + "」，两面对不上，这张卡算没走成");
            }
        }
        return vo;
    }

    @Override
    public boolean updateContent(Long id, String remark) {
        // 勾销卡没有就地涂一句的地方：记事只随过口/止步/沉底走，改口只能回退重做
        return false;
    }

    @Override
    public boolean remove(Long id) {
        // 抽走了年底就查不着这片小班那年怎么收的——勾销卡任何落定下都不许删
        return false;
    }

    // ------------------------------------------------------------------
    // 四段关口
    // ------------------------------------------------------------------

    /** 报伐：把设计单上钉过的号、小班号、实采株数、实采蓄积报上来。 */
    private void passReport(TFlogCloseCard card, FlogClosePassDto dto, TFlogCloseJournal j) {
        List<String> missing = new ArrayList<>();
        if (StringUtils.isEmpty(dto.getDesignNo())) {
            missing.add("设计单上钉过的号");
        }
        if (dto.getActualTrees() == null || dto.getActualTrees() <= 0) {
            missing.add("实采株数");
        }
        if (dto.getActualVolume() == null || dto.getActualVolume().compareTo(BigDecimal.ZERO) <= 0) {
            missing.add("实采蓄积");
        }
        if (!missing.isEmpty()) {
            throw new FlogBizException("报伐口过不去，缺了：" + String.join("、", missing));
        }
        TFlogSurveyDesign design = this.surveyDesignMapper.selectOne(new QueryWrapper<TFlogSurveyDesign>()
                .eq("design_no", dto.getDesignNo().trim())
                .eq("del_flag", 0)
                .last("limit 1"));
        if (design == null) {
            throw new FlogBizException("设计单号 " + dto.getDesignNo() + " 翻不出钉档的调查设计单，报伐不认");
        }
        if (!card.getSiteNo().equals(design.getSiteNo()) || !card.getSubcompNo().equals(design.getSubcompNo())) {
            throw new FlogBizException("设计单 " + design.getDesignNo() + " 钉的是 " + design.getSiteNo() + "/"
                    + design.getSubcompNo() + "，与本卡小班 " + card.getSiteNo() + "/" + card.getSubcompNo()
                    + " 对不上，张冠李戴不认");
        }
        if (design.getLatestResultId() == null) {
            throw new FlogBizException("设计单 " + design.getDesignNo() + " 还没钉过定档结论，"
                    + "核株那笔设计数没有着落，先去钉档再回报伐");
        }
        TFlogSurveyResult result = this.surveyResultMapper.selectById(design.getLatestResultId());
        if (result == null || (result.getDelFlag() != null && result.getDelFlag() == 1)
                || !design.getId().equals(result.getDesignId())) {
            throw new FlogBizException("设计单 " + design.getDesignNo() + " 指向的定档结论翻不实，报伐不认");
        }

        j.setDesignNo(design.getDesignNo());
        j.setActualTrees(dto.getActualTrees());
        j.setActualVolume(dto.getActualVolume());

        // 认下钉过号的设计单；伐区名号空着就照钉档补，供后两段与了断摘卡对名
        card.setDesignId(design.getId());
        card.setDesignNo(design.getDesignNo());
        if (StringUtils.isEmpty(card.getCutBlockNo()) && StringUtils.isNotEmpty(design.getCutBlockNo())) {
            card.setCutBlockNo(design.getCutBlockNo());
            this.closeCardMapper.update(null, new UpdateWrapper<TFlogCloseCard>()
                    .eq("id", card.getId())
                    .set("design_id", design.getId())
                    .set("design_no", design.getDesignNo())
                    .set("cut_block_no", design.getCutBlockNo()));
        } else {
            this.closeCardMapper.update(null, new UpdateWrapper<TFlogCloseCard>()
                    .eq("id", card.getId())
                    .set("design_id", design.getId())
                    .set("design_no", design.getDesignNo()));
        }
    }

    /** 核株：实采与设计两笔对着看，差过站里那格就要一句说法。 */
    private void passCheck(TFlogCloseCard card, Derived d, FlogClosePassDto dto, TFlogCloseJournal j) {
        TFlogCloseJournal report = d.alivePass[STAGE_REPORT];
        if (report == null || StringUtils.isEmpty(report.getDesignNo())) {
            throw new FlogBizException("报伐那一笔履历翻不出来，核株没有对看的底子");
        }
        TFlogSurveyDesign design = this.surveyDesignMapper.selectOne(new QueryWrapper<TFlogSurveyDesign>()
                .eq("design_no", report.getDesignNo())
                .eq("del_flag", 0)
                .last("limit 1"));
        if (design == null || design.getLatestResultId() == null) {
            throw new FlogBizException("报伐认下的设计单 " + report.getDesignNo() + " 或其钉档结论翻不实了");
        }
        TFlogSurveyResult result = this.surveyResultMapper.selectById(design.getLatestResultId());
        if (result == null || (result.getDelFlag() != null && result.getDelFlag() == 1)) {
            throw new FlogBizException("设计单 " + design.getDesignNo() + " 的钉档结论翻不实，核株对不了");
        }
        Integer designTrees = this.surveyTreeMapper.selectCount(new QueryWrapper<com.fc.v2.model.auto.TFlogSurveyTree>()
                .eq("design_id", design.getId())
                .eq("round_no", result.getRoundNo())
                .eq("del_flag", 0));
        BigDecimal designVolume = result.getTotalVolume() == null ? BigDecimal.ZERO : result.getTotalVolume();
        int trees = designTrees == null ? 0 : designTrees;

        BigDecimal treeRate = diffRate(report.getActualTrees() == null ? 0 : report.getActualTrees(), trees);
        BigDecimal volRate = diffRate(report.getActualVolume(), designVolume);
        boolean over = treeRate.compareTo(DIFF_GRID) > 0 || volRate.compareTo(DIFF_GRID) > 0;
        if (over && StringUtils.isEmpty(dto.getDiffReason())) {
            throw new FlogBizException("实采与设计对不上且差过站里那格（" + gridPercent()
                    + "）：株数 " + report.getActualTrees() + " 对设计 " + trees + "（差 " + percent(treeRate)
                    + "），蓄积 " + report.getActualVolume() + " 对设计 " + designVolume + "（差 "
                    + percent(volRate) + "）——得留一句说法，空口过不了核株");
        }

        j.setDesignNo(design.getDesignNo());
        j.setActualTrees(report.getActualTrees());
        j.setActualVolume(report.getActualVolume());
        j.setDesignTrees(trees);
        j.setDesignVolume(designVolume);
        j.setTreeDiffRate(treeRate);
        j.setVolDiffRate(volRate);
        j.setDiffReason(over ? dto.getDiffReason().trim() : null);
    }

    /** 验更：更新造林面积不得小于采伐面积，再看成活；采伐面积照设计单钉住，不许手填。 */
    private void passRenew(TFlogCloseCard card, FlogClosePassDto dto, TFlogCloseJournal j) {
        if (card.getDesignId() == null) {
            throw new FlogBizException("报伐没认下设计单，采伐面积无据，验更过不去");
        }
        TFlogSurveyDesign design = this.surveyDesignMapper.selectById(card.getDesignId());
        if (design == null || (design.getDelFlag() != null && design.getDelFlag() == 1)) {
            throw new FlogBizException("认下的设计单翻不实了，采伐面积钉不住");
        }
        List<String> missing = new ArrayList<>();
        if (dto.getRenewArea() == null || dto.getRenewArea().compareTo(BigDecimal.ZERO) <= 0) {
            missing.add("更新造林面积");
        }
        if (dto.getRenewSurvival() == null || dto.getRenewSurvival().compareTo(BigDecimal.ZERO) <= 0) {
            missing.add("更新造林成活率");
        }
        if (!missing.isEmpty()) {
            throw new FlogBizException("验更口过不去，缺了：" + String.join("、", missing));
        }
        if (dto.getRenewSurvival().compareTo(new BigDecimal("100")) > 0) {
            throw new FlogBizException("成活率 " + dto.getRenewSurvival() + "% 不像话，至多 100");
        }
        BigDecimal cutArea = design.getArea();
        if (dto.getRenewArea().compareTo(cutArea) < 0) {
            throw new FlogBizException("更新造林面积 " + dto.getRenewArea() + " 公顷比采伐面积 " + cutArea
                    + " 公顷小，更新面积不得小于采伐面积，当场点回，验更过不成");
        }
        j.setDesignNo(design.getDesignNo());
        j.setCutArea(cutArea);
        j.setRenewArea(dto.getRenewArea());
        j.setRenewSurvival(dto.getRenewSurvival());
    }

    /** 了断：把这一片小班从在册处摘下来，占着的额度原样退回。 */
    private FlogCloseCardVo passClose(TFlogCloseCard card, Derived d, TFlogCloseJournal j) {
        List<TFlogApplyCard> currents = this.applyCardMapper.selectList(new QueryWrapper<TFlogApplyCard>()
                .eq("site_no", card.getSiteNo())
                .eq("subcomp_no", card.getSubcompNo())
                .eq("current_flag", APPLY_CURRENT_YES)
                .eq("del_flag", 0));
        List<TFlogApplyCard> onHook = new ArrayList<>();
        for (TFlogApplyCard c : currents) {
            if (c.getStatus() == null || c.getStatus() != APPLY_PRESSED) {
                onHook.add(c);
            }
        }
        if (onHook.isEmpty()) {
            throw new FlogBizException("小班 " + card.getSiteNo() + "/" + card.getSubcompNo()
                    + " 在册处翻不出一张现行未压死的采伐建档卡，没东西可摘、没额度可退，了断落不下");
        }
        if (onHook.size() > 1) {
            throw new FlogBizException("小班 " + card.getSiteNo() + "/" + card.getSubcompNo()
                    + " 竟挂着 " + onHook.size() + " 张现行未压死建档卡，摘不准，先把建档卡的账了清");
        }
        TFlogApplyCard apply = onHook.get(0);

        // 退额走建档卡那条压死事务（同一事务内）：额度锁行、卡状态、办理流水都由它钉，本处不另写账
        FlogCardSaveVo pressVo = this.applyCardService.pressCard(apply.getId());
        BigDecimal refund = pressVo.getQty() == null ? BigDecimal.ZERO : pressVo.getQty();

        card.setApplyCardId(apply.getId());
        card.setBillNo(apply.getBillNo());
        j.setDesignNo(card.getDesignNo());
        j.setOccupyQty(refund);
        j.setRefundQty(refund);
        appendJournal(j, d);

        int rows = this.closeCardMapper.update(null, new UpdateWrapper<TFlogCloseCard>()
                .eq("id", card.getId())
                .eq("status", STATUS_ACTIVE)
                .eq("stage", STAGE_CLOSE)
                .set("status", STATUS_CLOSED)
                .set("apply_card_id", apply.getId())
                .set("bill_no", apply.getBillNo())
                .set("closed_by", currentLoginName())
                .set("closed_time", new Date())
                .set("active_anchor", null));
        if (rows == 0) {
            throw new FlogBizException("卡 " + card.getBizNo() + " 刚被另一笔挪动，了断请刷新后再看");
        }
        card.setStatus(STATUS_CLOSED);
        card.setClosedBy(currentLoginName());
        card.setClosedTime(new Date());
        return buildVo(card, STAGE_CLOSE, refund, refund,
                "卡号 " + card.getBizNo() + " 已了断：建档卡 " + apply.getBillNo()
                        + " 从在册处摘下，占着的额度 " + refund + " 立方米原样退回；各栏随即封死，"
                        + "卡不抽、记事不改，年底照此查");
    }

    // ------------------------------------------------------------------
    // 履历与落点推算
    // ------------------------------------------------------------------

    /**
     * 卡此刻的落点：照没过的过口履历自己算成，不读卡面那两格（卡面只作落钉后的回执）。
     * 止步笔之后即止步；四笔过口都在册即了断；否则在办，停在“已过几段”那一道口。
     */
    private Derived derive(TFlogCloseCard card) {
        return derive(card, this.journalMapper.selectList(new QueryWrapper<TFlogCloseJournal>()
                .eq("card_id", card.getId())
                .eq("del_flag", 0)
                .orderByAsc("seq")));
    }

    private Derived derive(TFlogCloseCard card, List<TFlogCloseJournal> lines) {
        Derived d = new Derived();
        d.alivePass = new TFlogCloseJournal[STAGE_TOTAL];
        Set<Integer> sunk = new HashSet<>();
        int haltedStage = -1;
        for (TFlogCloseJournal j : lines) {
            if (j.getAction() != null && j.getAction() == ACTION_SINK && j.getRefSeq() != null) {
                sunk.add(j.getRefSeq());
            }
        }
        for (TFlogCloseJournal j : lines) {
            if (j.getAction() != null && j.getAction() == ACTION_HALT) {
                haltedStage = j.getStage() == null ? 0 : j.getStage();
            }
            if (j.getAction() != null && j.getAction() == ACTION_PASS
                    && !sunk.contains(j.getSeq()) && j.getStage() != null
                    && j.getStage() >= 0 && j.getStage() < STAGE_TOTAL) {
                d.alivePass[j.getStage()] = j;
            }
        }
        int passed = 0;
        for (int s = 0; s < STAGE_TOTAL; s++) {
            if (d.alivePass[s] != null) {
                passed++;
            }
        }
        d.passedCount = passed;
        // 落点取“头一个没有在册过口的段”：四段挨着行，跳段落的过口不算数。
        // 停在了断口（前三段过、了断没过）仍是在办；唯了断段也有在册过口笔，才算已了断。
        if (d.alivePass[STAGE_CLOSE] != null) {
            d.status = STATUS_CLOSED;
            d.stage = STAGE_CLOSE;
        } else if (haltedStage >= 0) {
            d.status = STATUS_HALT;
            d.stage = haltedStage;
        } else {
            int firstOpen = STAGE_CLOSE;
            for (int s = 0; s < STAGE_TOTAL; s++) {
                if (d.alivePass[s] == null) {
                    firstOpen = s;
                    break;
                }
            }
            d.status = STATUS_ACTIVE;
            d.stage = firstOpen;
        }
        return d;
    }

    private int attemptOf(Long cardId, int stage) {
        // 该段已有几笔过口（含已沉底的；沉掉的与后补的各算各的遍次），新笔挨号
        Integer n = this.journalMapper.selectCount(new QueryWrapper<TFlogCloseJournal>()
                .eq("card_id", cardId)
                .eq("stage", stage)
                .eq("action", ACTION_PASS)
                .eq("del_flag", 0));
        return (n == null ? 0 : n) + 1;
    }

    private TFlogCloseJournal newPassJournal(TFlogCloseCard card, int stage, int attemptNo, String remark) {
        TFlogCloseJournal j = new TFlogCloseJournal();
        j.setCardId(card.getId());
        j.setBizNo(card.getBizNo());
        j.setStage(stage);
        j.setAttemptNo(attemptNo);
        j.setAction(ACTION_PASS);
        j.setRemark(remark);
        return j;
    }

    /** 续一笔：流水序接末笔，指纹按上一枚首尾相扣。 */
    private void appendJournal(TFlogCloseJournal j, Derived d) {
        TFlogCloseJournal last = this.journalMapper.selectOne(new QueryWrapper<TFlogCloseJournal>()
                .eq("card_id", j.getCardId())
                .eq("del_flag", 0)
                .orderByDesc("seq")
                .last("limit 1"));
        int seq = last == null ? 1 : (last.getSeq() == null ? 1 : last.getSeq() + 1);
        String prevFp = last == null ? null : last.getFp();
        j.setSeq(seq);
        j.setPrevFp(prevFp);
        j.setOpBy(currentLoginName());
        // 落账时刻截到秒：库面 datetime 只到秒，指纹也按秒入串，读回重算才合得上
        Date opTime = truncateToSecond(new Date());
        j.setOpTime(opTime);
        j.setDelFlag(0);
        j.setFp(fingerprint(j, prevFp));
        this.journalMapper.insert(j);
    }

    /**
     * 一笔一枚指纹：把所记各栏按固定次序拼成明串，链入上一枚指纹后压 MD5。
     * 任何一栏事后被手改、或凭空补一笔，重算都合不上。
     */
    private String fingerprint(TFlogCloseJournal j, String prevFp) {
        String time = j.getOpTime() == null ? "" : new SimpleDateFormat("yyyyMMddHHmmss").format(j.getOpTime());
        String raw = String.join("|",
                String.valueOf(j.getCardId()),
                String.valueOf(j.getSeq()),
                String.valueOf(j.getStage()),
                String.valueOf(j.getAttemptNo()),
                String.valueOf(j.getAction()),
                j.getRefSeq() == null ? "" : String.valueOf(j.getRefSeq()),
                nz(j.getDesignNo()),
                j.getActualTrees() == null ? "" : String.valueOf(j.getActualTrees()),
                j.getActualVolume() == null ? "" : j.getActualVolume().stripTrailingZeros().toPlainString(),
                j.getDesignTrees() == null ? "" : String.valueOf(j.getDesignTrees()),
                j.getDesignVolume() == null ? "" : j.getDesignVolume().stripTrailingZeros().toPlainString(),
                j.getTreeDiffRate() == null ? "" : j.getTreeDiffRate().stripTrailingZeros().toPlainString(),
                j.getVolDiffRate() == null ? "" : j.getVolDiffRate().stripTrailingZeros().toPlainString(),
                nz(j.getDiffReason()),
                j.getCutArea() == null ? "" : j.getCutArea().stripTrailingZeros().toPlainString(),
                j.getRenewArea() == null ? "" : j.getRenewArea().stripTrailingZeros().toPlainString(),
                j.getRenewSurvival() == null ? "" : j.getRenewSurvival().stripTrailingZeros().toPlainString(),
                j.getOccupyQty() == null ? "" : j.getOccupyQty().stripTrailingZeros().toPlainString(),
                j.getRefundQty() == null ? "" : j.getRefundQty().stripTrailingZeros().toPlainString(),
                nz(j.getOpBy()),
                time,
                nz(j.getRemark()),
                prevFp == null ? "" : prevFp);
        return MD5Util.encode(raw).toUpperCase();
    }

    // ------------------------------------------------------------------
    // 杂项
    // ------------------------------------------------------------------

    private TFlogCloseCard lockCard(Long id) {
        TFlogCloseCard card = this.closeCardMapper.selectOne(new QueryWrapper<TFlogCloseCard>()
                .eq("id", id)
                .eq("del_flag", 0)
                .last("limit 1 for update"));
        if (card == null) {
            throw new FlogBizException("这张勾销卡不存在或已被抽走");
        }
        dataScope.assertVisible(currentUserId(), card.getSiteNo());
        return card;
    }

    private int stageOf(TFlogCloseCard card) {
        int s = card.getStage() == null ? 0 : card.getStage();
        return s < 0 ? 0 : Math.min(s, STAGE_CLOSE);
    }

    private BigDecimal diffRate(int actual, int design) {
        if (design <= 0) {
            return actual > 0 ? BigDecimal.ONE : BigDecimal.ZERO;
        }
        return new BigDecimal(Math.abs(actual - design))
                .divide(new BigDecimal(design), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal diffRate(BigDecimal actual, BigDecimal design) {
        BigDecimal a = actual == null ? BigDecimal.ZERO : actual;
        BigDecimal d = design == null ? BigDecimal.ZERO : design;
        if (d.compareTo(BigDecimal.ZERO) <= 0) {
            return a.compareTo(BigDecimal.ZERO) > 0 ? BigDecimal.ONE : BigDecimal.ZERO;
        }
        return a.subtract(d).abs().divide(d, 4, RoundingMode.HALF_UP);
    }

    private String percent(BigDecimal rate) {
        return rate.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP).toPlainString() + "%";
    }

    private String gridPercent() {
        return percent(DIFF_GRID);
    }

    private String stageName(Integer s) {
        return s != null && s >= 0 && s < STAGE_TOTAL ? STAGE_NAMES[s] : "?";
    }

    private boolean eqFp(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private boolean safeEq(Integer a, Integer b) {
        return a == null ? b == null : a.equals(b);
    }

    private String nz(String s) {
        return s == null ? "" : s;
    }

    private String activeAnchor(String siteNo, String subcompNo) {
        return siteNo + "#" + subcompNo;
    }

    private Date truncateToSecond(Date t) {
        return new Date((t.getTime() / 1000L) * 1000L);
    }

    private FlogCloseCardVo buildVo(TFlogCloseCard card, int stage, BigDecimal occupy, BigDecimal refund,
                                    String message) {
        Derived d = derive(card);
        FlogCloseCardVo vo = new FlogCloseCardVo();
        vo.setId(card.getId());
        vo.setBizNo(card.getBizNo());
        vo.setSiteNo(card.getSiteNo());
        vo.setSubcompNo(card.getSubcompNo());
        vo.setCutBlockNo(card.getCutBlockNo());
        vo.setPassedCount(d.passedCount);
        vo.setStage(stage);
        vo.setStageName(stageName(stage));
        vo.setStatus(card.getStatus() == null ? STATUS_ACTIVE : card.getStatus());
        vo.setStatusName(STATUS_NAMES[vo.getStatus()]);
        vo.setDesignNo(card.getDesignNo());
        vo.setBillNo(card.getBillNo());
        vo.setHaltReason(card.getHaltReason());
        vo.setClosedBy(card.getClosedBy());
        vo.setClosedTime(card.getClosedTime());
        vo.setOccupyQty(occupy);
        vo.setRefundQty(refund);
        vo.setDuplicate(Boolean.FALSE);
        vo.setMessage(message);
        return vo;
    }

    private String nextBizNo() {
        int year = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
        for (int i = 0; i < 10; i++) {
            String no = "GX" + year + "-"
                    + String.format("%08d", Math.abs((SnowflakeIdWorker.getUUID() + "-" + i).hashCode()) % 100000000L);
            Integer cnt = this.closeCardMapper.selectCount(new QueryWrapper<TFlogCloseCard>()
                    .eq("biz_no", no)
                    .eq("del_flag", 0));
            if (cnt == null || cnt == 0) {
                return no;
            }
        }
        throw new FlogBizException("勾销卡号连撞，缓一缓再报");
    }

    private Long currentUserId() {
        try {
            return ShiroUtils.getUserId();
        } catch (Exception e) {
            return null;
        }
    }

    private String currentLoginName() {
        try {
            return ShiroUtils.getLoginName();
        } catch (Exception e) {
            return null;
        }
    }

    /** 履历算出的落点。 */
    private static class Derived {
        private int stage;
        private int status;
        private int passedCount;
        /** 四段各自在册（未沉底）的过口笔 */
        private TFlogCloseJournal[] alivePass;
    }
}
