package com.fc.v2.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fc.v2.model.auto.TFlogStandDoc;

import java.util.List;

/**
 * 编制单位在册底档 Service接口
 *
 * 一档只管一家；底档字号自动生成（各经营类别各有排法），手塞的号不存；
 * 年初收回的档不再接纳新卡，早年挂过的卡仍翻得到。
 *
 * @author fuce
 * @date 2026-09-29
 */
public interface ITFlogStandDocService {

    /** 列底档（按账号辖域收口） */
    List<TFlogStandDoc> selectList(Wrapper<TFlogStandDoc> queryWrapper);

    /** 立户：字号由系统按经营类别自动排，名号重复（一家立两户）当场驳回。仅上级账号可立户 */
    TFlogStandDoc register(TFlogStandDoc doc);

    /** 年初收回：在册 -> 已收回，从此不接新卡 */
    TFlogStandDoc recall(Long id);

    TFlogStandDoc selectById(Long id);
}
