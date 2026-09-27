package com.fc.v2.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fc.v2.model.auto.TIchImageCard;

import java.util.List;

/**
 * 技艺影像卷立卷单 Service接口
 *
 * @author fuce
 * @date 2026-09-12
 */
public interface ITIchImageCardService {

    /** 按主键查询 */
    TIchImageCard selectTIchImageCardById(Long id);

    /** 按条件查询列表（分页由调用方统一处理） */
    List<TIchImageCard> selectTIchImageCardList(Wrapper<TIchImageCard> queryWrapper);

    /** 新增 */
    int insertTIchImageCard(TIchImageCard record);

    /** 修改 */
    int updateTIchImageCard(TIchImageCard record);

    /** 批量删除 */
    int deleteTIchImageCardByIds(String ids);

    /** 按主键删除 */
    int deleteTIchImageCardById(Long id);
}
