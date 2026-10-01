package com.fc.v2.service;

import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.model.auto.TFlogCloseCard;
import com.fc.v2.model.custom.flog.FlogCloseCardVo;
import com.fc.v2.model.custom.flog.FlogCloseLedgerVo;
import com.fc.v2.model.custom.flog.FlogCloseOpenDto;
import com.fc.v2.model.custom.flog.FlogClosePassDto;

/**
 * 伐区勾销事务卡 Service接口（state-machine 形状：一小班一张卡，四段挨着走）。
 *
 * 往前推进一格、往回退一格各只有一个落点，全收在 advance/rollback 这一对方法里；
 * 卡此刻停在哪一段，由没过的过口履历算成，不看屏上摆的、不看经办人心里认的。
 * 整条道上没有现开卡、就地涂一句的口子。
 *
 * @author fuce
 * @date 2026-09-14
 */
public interface ITFlogCloseCardService {

    /** 按主键查询单据（含辖域核验） */
    TFlogCloseCard selectTFlogCloseCardById(Long id);

    /** 列表查询（流转台账，按账号辖域收口） */
    List<TFlogCloseCard> selectTFlogCloseCardList(QueryWrapper<TFlogCloseCard> queryWrapper);

    /** 起卡：一小班一张活卡，卡一起就停在报伐口待过；头一张没了断/止步，第二张立不起来 */
    FlogCloseCardVo open(FlogCloseOpenDto dto);

    /**
     * 旧入参旧返回（一直可调）：按系统算定的当前道口空口往前推一格，附言随这笔履历走。
     * 报伐/核株/验更缺格会被当场点回；了断无格可填，走得通。
     */
    TFlogCloseCard advance(Long id, String remark);

    /**
     * 同方法名下叠的新用法：带着该段该报的格子过口。
     * 送的段次与系统算的不符——跳段的、倒着走的、同一段第二遍的——一概不算过成
     * （第二遍只作没这回事，不另落一句记事）。
     */
    FlogCloseCardVo advance(FlogClosePassDto dto);

    /** 旧入参旧返回（一直可调）：往回退一格，旧那一段过口笔沉底，退回去重做 */
    TFlogCloseCard rollback(Long id, String remark);

    /** 同方法名下叠的新用法：段次须正是回退后的落点，不然不算退成 */
    FlogCloseCardVo rollback(Long id, Integer stage, String reason);

    /** 止步：在当前道口写下“这一段不过了”，卡就此止步，过后谁也续不进去（同小班可另起一张） */
    FlogCloseCardVo halt(Long id, Integer stage, String reason);

    /** 勾销履历：逐笔翻检并当场核验，缺页/多笔/手动补/指纹对不上即点破 */
    FlogCloseLedgerVo ledger(Long id);

    /** 旧修改口：勾销卡没有就地改记事的地方，改口只能回退重做（一律回绝） */
    boolean updateContent(Long id, String remark);

    /** 旧删除口：勾销卡不许抽走，了断的年底要查；走不下去请止步留痕（一律回绝） */
    boolean remove(Long id);
}
