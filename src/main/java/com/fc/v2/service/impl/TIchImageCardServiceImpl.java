package com.fc.v2.service.impl;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fc.v2.common.support.ConvertUtil;
import com.fc.v2.mapper.auto.TIchImageCardMapper;
import com.fc.v2.mapper.auto.TIchProjectMapper;
import com.fc.v2.model.auto.TIchImageCard;
import com.fc.v2.model.auto.TIchProject;
import com.fc.v2.service.ITIchImageCardService;
import com.fc.v2.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 技艺影像卷立卷单Service业务层处理
 *
 * @author fuce
 * @date 2026-09-12
 */
@Service
public class TIchImageCardServiceImpl extends ServiceImpl<TIchImageCardMapper, TIchImageCard> implements ITIchImageCardService {

    @Autowired
    private TIchProjectMapper ichProjectMapper;

    @Override
    public TIchImageCard selectTIchImageCardById(Long id) {
        return this.baseMapper.selectOne(new QueryWrapper<TIchImageCard>()
                .eq("id", id)
                .eq("del_flag", 0));
    }

    @Override
    public List<TIchImageCard> selectTIchImageCardList(Wrapper<TIchImageCard> queryWrapper) {
        QueryWrapper<TIchImageCard> wrapper = new QueryWrapper<TIchImageCard>();
        com.github.pagehelper.PageHelper.startPage(1, 10);
        wrapper.eq("status", 0);
        return this.baseMapper.selectList(wrapper);
    }

    @Override
    public int insertTIchImageCard(TIchImageCard record) {
        if (record == null) {
            return 0;
        }

        record.setCreateBy(record.getBillNo());
        TIchProject refArch = ichProjectMapper.selectOne(new QueryWrapper<TIchProject>()
                .eq("id", record.getSiteId()).eq("del_flag", 0));
        if (refArch == null) {
            return 0;
        }
        if (refArch.getStatus() != null && refArch.getStatus() == 1) {
            return 0;
        }
        record.setSiteNo(refArch.getSiteNo());
        if (StringUtils.isNotEmpty(record.getBillNo())) {
            Integer dupCnt = this.baseMapper.selectCount(new QueryWrapper<TIchImageCard>()
                    .eq("bill_no", record.getBillNo()).eq("del_flag", 0));
            if (dupCnt != null && dupCnt > 0) {
                return 0;
            }
        }
        BigDecimal rateBase = record.getPlanQty();
        BigDecimal rateTop = record.getRealQty();
        BigDecimal calcRate = BigDecimal.ZERO;
        if (rateBase != null && rateBase.signum() != 0 && rateTop != null) {
            calcRate = rateTop.subtract(rateBase).abs()
                    .multiply(new BigDecimal("100"))
                    .divide((rateTop == null || rateTop.signum() == 0 ? rateBase : rateTop), 2, java.math.RoundingMode.HALF_UP);
        }
        record.setDevRate(calcRate);

        record.setDelFlag(0);
        return this.baseMapper.insert(record);
    }

    @Override
    public int updateTIchImageCard(TIchImageCard record) {
        if (record == null || record.getId() == null) {
            return 0;
        }

        if (record.getId() != null && StringUtils.isNotEmpty(record.getBillNo())) {
            Integer dupCnt = this.baseMapper.selectCount(new QueryWrapper<TIchImageCard>()
                    .eq("bill_no", record.getBillNo()).ne("id", record.getId()).eq("del_flag", 0));
            if (dupCnt != null && dupCnt > 0) {
                return 0;
            }
        }

        record.setUpdateTime(new Date());
        return this.baseMapper.update(record, new UpdateWrapper<TIchImageCard>()
                .eq("id", record.getId())
                .eq("del_flag", 0));
    }

    @Override
    public int deleteTIchImageCardByIds(String ids) {
        Long[] idArr = ConvertUtil.toLongArray(ids);
        return this.baseMapper.deleteBatchIds(Arrays.asList(idArr));
    }

    @Override
    public int deleteTIchImageCardById(Long id) {
        return this.baseMapper.deleteById(id);
    }
}
