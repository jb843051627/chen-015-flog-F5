package com.fc.v2.service;

import java.util.List;

import com.fc.v2.model.auto.TFlogSurveyDesign;
import com.fc.v2.model.auto.TFlogSurveyResult;
import com.fc.v2.model.auto.TFlogSurveyTree;
import com.fc.v2.model.custom.flog.FlogSurveyDesignDto;
import com.fc.v2.model.custom.flog.FlogSurveyRecheckDto;
import com.fc.v2.model.custom.flog.FlogSurveySubmitVo;

/**
 * 伐区调查设计单 Service接口。
 *
 * 从检尺两数到出材量再到定档只走界册服务那条求值方法；本口只负责递数、落单、钉结论。
 * 结论钉过不再改口；整改复验重算一遍追加一行，两回各留各的代号。
 *
 * @author fuce
 * @date 2026-09-30
 */
public interface ITFlogSurveyDesignService {

    /** 首评提交：一张单子一个小班，同事务算清并钉死结论，给回钉的那一份 */
    FlogSurveySubmitVo submit(FlogSurveyDesignDto dto);

    /** 整改复验：沿用原单小班/类别/面积/出材率/设计日，重算一遍追加结论行（不合并不改口） */
    FlogSurveySubmitVo recheck(FlogSurveyRecheckDto dto);

    /** 按主键查单（越权当场回绝） */
    TFlogSurveyDesign selectById(Long id);

    /** 设计单列表（数据权限内；按字号/小班/类别过滤） */
    List<TFlogSurveyDesign> listDesigns(String siteNo, String subcompNo, Integer applyScope);

    /** 按单号查单（越权当场回绝） */
    TFlogSurveyDesign selectByDesignNo(String designNo);

    /** 该单各轮结论（轮次升序；钉死的行一字不改） */
    List<TFlogSurveyResult> listResults(Long designId);

    /** 该单某一轮的逐株检尺账（含同一回算出的逐株材积）；轮次空着取最近一轮 */
    List<TFlogSurveyTree> listTrees(Long designId, Integer roundNo);
}
