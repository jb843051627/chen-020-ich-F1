package com.fc.v2.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TIchReportRowMapper;
import com.fc.v2.model.auto.TIchReportRow;
import com.fc.v2.service.ITIchReportRowService;

/**
 * 履职情况报送核收行 Service业务层处理（batch-process 形状：整批提交）
 *
 * @author fuce
 * @date 2026-09-14
 */
@Service
public class TIchReportRowServiceImpl implements ITIchReportRowService {

    private static final int MAX_ROWS = 500;
    private static final int STATUS_OK = 1;
    private static final int STATUS_FAIL = 2;

    @javax.annotation.Resource
    private TIchReportRowMapper ichReportRowMapper;

    @Override
    public TIchReportRow selectTIchReportRowById(Long id) {
        return this.ichReportRowMapper.selectById(id);
    }

    @Override
    public int submitBatch(String batchNo, List<TIchReportRow> rows) {
        String no = rows.get(0).getBatchNo();
        java.util.List<TIchReportRow> errors = new java.util.ArrayList<TIchReportRow>();
        int seq = 0;
        for (TIchReportRow r : rows) {
            if (r.getItemCode() == null || r.getItemCode().trim().isEmpty()
                    || r.getQty() == null
                    || r.getQty().compareTo(java.math.BigDecimal.ZERO) <= 0) {
                seq++;
                r.setRowNo(Integer.valueOf(seq));
                r.setBatchNo(no);
                r.setStatus(STATUS_FAIL);
                this.ichReportRowMapper.insert(r);
                errors.add(r);
            }
        }
        if (!errors.isEmpty()) {
            return 0;
        }
        int ok = 0;
        for (TIchReportRow r : rows) {
            r.setBatchNo(no);
            r.setStatus(STATUS_OK);
            this.ichReportRowMapper.insert(r);
            ok++;
        }
        return ok;
    }

    @Override
    public List<TIchReportRow> listErrors(String batchNo) {
        return this.ichReportRowMapper.selectList(new QueryWrapper<TIchReportRow>()
                .eq("batch_no", batchNo).eq("status", STATUS_FAIL));
    }
}
