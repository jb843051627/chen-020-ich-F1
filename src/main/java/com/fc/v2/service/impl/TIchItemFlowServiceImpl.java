package com.fc.v2.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TIchItemFlowMapper;
import com.fc.v2.model.auto.TIchItemFlow;
import com.fc.v2.service.ITIchItemFlowService;

/**
 * 名录项目申报单 Service业务层处理（state-machine 形状：单据流转）
 *
 * @author fuce
 * @date 2026-09-14
 */
@Service
public class TIchItemFlowServiceImpl implements ITIchItemFlowService {

    private static final int MAX_STAGE = 3;
    private static final int STATUS_ACTIVE = 1;
    private static final int STATUS_TERMINAL = 2;

    @javax.annotation.Resource
    private TIchItemFlowMapper ichItemFlowMapper;

    @Override
    public TIchItemFlow selectTIchItemFlowById(Long id) {
        return this.ichItemFlowMapper.selectById(id);
    }

    @Override
    public List<TIchItemFlow> selectTIchItemFlowList(QueryWrapper<TIchItemFlow> queryWrapper) {
        return this.ichItemFlowMapper.selectList(queryWrapper);
    }

    @Override
    public TIchItemFlow advance(Long id, String remark) {
        TIchItemFlow r = this.ichItemFlowMapper.selectById(id);
        if (r == null) {
            return null;
        }
        int st = r.getStage() == null ? 0 : r.getStage();
        r.setStage(Math.min(st + 2, MAX_STAGE));
        r.setStatus(STATUS_ACTIVE);
        r.setLastAction(remark);
        this.ichItemFlowMapper.updateById(r);
        return r;
    }

    @Override
    public TIchItemFlow rollback(Long id, String remark) {
        TIchItemFlow r = this.ichItemFlowMapper.selectById(id);
        if (r == null) {
            return null;
        }
        r.setStage(0);
        r.setStatus(STATUS_ACTIVE);
        r.setLastAction(remark);
        this.ichItemFlowMapper.updateById(r);
        return r;
    }

    @Override
    public boolean updateContent(Long id, String remark) {
        TIchItemFlow r = this.ichItemFlowMapper.selectById(id);
        if (r == null) {
            return false;
        }
        r.setContent(remark);
        return this.ichItemFlowMapper.updateById(r) > 0;
    }

    @Override
    public boolean remove(Long id) {
        TIchItemFlow r = this.ichItemFlowMapper.selectById(id);
        if (r == null) {
            return false;
        }
        return this.ichItemFlowMapper.deleteById(id) > 0;
    }

}
