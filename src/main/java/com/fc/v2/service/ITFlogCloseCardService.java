package com.fc.v2.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.model.auto.TFlogCloseCard;
import com.fc.v2.model.auto.TFlogCloseStage;
import com.fc.v2.model.custom.flog.FlogCloseMoveVo;
import com.fc.v2.model.custom.flog.FlogCloseOpenDto;
import com.fc.v2.model.custom.flog.FlogClosePassDto;
import com.fc.v2.model.custom.flog.FlogCloseRollbackDto;

import java.util.List;

/**
 * 伐区勾销事务卡 Service接口（state-machine 形状：四段单据流转）。
 *
 * 全卡只有两条动法——{@link #advance} 往前推一格、{@link #rollback} 往回退一格，
 * 两条路各只有一个落点。卡此刻停在哪段，系统照勾销履历自己算，摆上屏的、
 * 经办人心里认的都当不得真；改没改成只看这对方法的回执。
 * 旧入参与旧返回一直可调，另起用法在同一方法名下再叠一个重载。
 *
 * @author fuce
 * @date 2026-09-14
 */
public interface ITFlogCloseCardService {

    /** 按主键查询单据（含辖域核验） */
    TFlogCloseCard selectTFlogCloseCardById(Long id);

    /** 列表查询（流转台账；段次按履历重算后摆） */
    List<TFlogCloseCard> selectTFlogCloseCardList(QueryWrapper<TFlogCloseCard> queryWrapper);

    /** 勾销履历：一张卡从报伐到当下的每一道过口（含沉底旧轮），按段次/轮序排开 */
    List<TFlogCloseStage> listHistory(Long id);

    /** 勾销履历核验：从了断往回数回报伐，缺页/多笔/对不上号当场点破，点破即抛错 */
    List<TFlogCloseStage> verifyHistory(Long id);

    /**
     * 开卡即报伐（新用法，JSON 送法）：没有“先开空卡再涂一句”的第二条道。
     * 一小班同时只许一张活卡；过了报伐关口卡才立得起来，立起来便停在核株段。
     */
    FlogCloseMoveVo open(FlogCloseOpenDto dto);

    /** 开卡即报伐（实体送法，旧入参照样走同一套规矩） */
    FlogCloseMoveVo open(TFlogCloseCard record);

    /**
     * 唯一往前口（新用法）：把当前段该过的几道关口报上来，过成则往前推进一格。
     * 跳段、同段第二遍、止步/了断后再推，一概不认。
     */
    FlogCloseMoveVo advance(FlogClosePassDto dto);

    /**
     * 唯一往前口（旧入参旧返回，照样可调）：
     * 推进一格；备注在核株段当“差过格的一句说法”用。成功返回重算段次后的卡，被拒抛业务异常。
     */
    TFlogCloseCard advance(Long id, String remark);

    /**
     * 唯一往回口（新用法）：退回上一段重做，先前那一段落过的名目尽数沉底，
     * 沉掉的与后补的不摆在一处。报伐段无处可退；止步/了断后不许退。
     */
    FlogCloseMoveVo rollback(FlogCloseRollbackDto dto);

    /** 唯一往回口（旧入参旧返回，照样可调） */
    TFlogCloseCard rollback(Long id, String remark);

    /**
     * 止步：哪一段写下“这一段不过了”，卡就此止步封死，过后谁也续不进去；
     * 这片小班要再走只能重新起一张卡。
     */
    FlogCloseMoveVo halt(Long id, String reason);
}
