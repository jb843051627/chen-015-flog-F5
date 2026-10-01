package com.fc.v2.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fc.v2.model.auto.TFlogApplyCard;
import com.fc.v2.model.custom.flog.FlogCardSaveDto;
import com.fc.v2.model.custom.flog.FlogCardSaveVo;
import com.fc.v2.model.custom.flog.FlogQuotaView;

import java.util.List;

/**
 * 伐区采伐申请建档卡 Service接口
 *
 * 业务铁律：开不开得出卡，业务服务层这一条保存方法说了算。
 * 控制器不得另开通道，旧版本人工不可覆写，编号、顺位、核验串、额度全由本层回填。
 *
 * @author fuce
 * @date 2026-09-12
 */
public interface ITFlogApplyCardService {

    /** 按主键查询（含辖域核验） */
    TFlogApplyCard selectTFlogApplyCardById(Long id);

    /** 按条件查询列表：强制只摆现行版本、未删除，并按账号辖域收口 */
    List<TFlogApplyCard> selectTFlogApplyCardList(Wrapper<TFlogApplyCard> queryWrapper);

    /**
     * 唯一保存口（原实体入参，照样能调）。
     * 同一家（底档字号）同一个小班二次上报，并成一行、升一版；首次上报则立卡。
     * 缺栏、蓄积为零/负、超额、底档已收回等，逐项点名回执。
     */
    FlogCardSaveVo insertTFlogApplyCard(TFlogApplyCard record);

    /**
     * 另一种送法（JSON 报文）：与上面同名重载，内部走同一套规矩，没有第二条通道。
     */
    FlogCardSaveVo insertTFlogApplyCard(FlogCardSaveDto dto);

    /** 旧修改口不再各自为政，一律并到唯一保存口，杜绝躲过校验的通道 */
    FlogCardSaveVo updateTFlogApplyCard(TFlogApplyCard record);

    /** 逻辑删除（压死卡不退、未压死卡占额原样退回） */
    int deleteTFlogApplyCardByIds(String ids);

    /** 按主键逻辑删除 */
    int deleteTFlogApplyCardById(Long id);

    /** 核对成形：0待核对 -> 1已核对（成形后不可覆写，要改只能升版） */
    FlogCardSaveVo checkCard(Long id);

    /** 压死了断：-> 2已压死，占用额度原样退回本年账 */
    FlogCardSaveVo pressCard(Long id);

    /** 同屏额度：已扣与尚存同一回计算给出，页面不许自己数 */
    FlogQuotaView quotaView(String siteNo, Integer quotaYear);

    /** 旧版翻检：一张卡名下各版本，只供事后看，不可改 */
    List<TFlogApplyCard> selectHistoryVersions(String billNo);
}
