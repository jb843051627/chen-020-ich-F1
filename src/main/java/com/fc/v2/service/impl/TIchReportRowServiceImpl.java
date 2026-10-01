package com.fc.v2.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TIchReportBatchMapper;
import com.fc.v2.mapper.auto.TIchReportRowMapper;
import com.fc.v2.model.auto.TIchReportBatch;
import com.fc.v2.model.auto.TIchReportRow;
import com.fc.v2.model.custom.ich.IchCompareResult;
import com.fc.v2.model.custom.ich.IchImportCmd;
import com.fc.v2.model.custom.ich.IchImportResult;
import com.fc.v2.model.custom.ich.IchReconcileResult;
import com.fc.v2.service.ITIchReportRowService;

/**
 * 履职情况报送核收行 Service业务层处理（batch-process 形状：整批提交）
 *
 * 落库只有进门那一处（{@link #importBatch}）：批与行同一次记账，
 * 回执三个数与逐行去向同一回算出。撤报、归档只改标记不添行；
 * 重复进门只比对，一字不写。
 *
 * 核收规矩全部在纯逻辑 {@link ReportBatchJudge} 里，本类只管进出库。
 * 库的进出收在 protected 小方法里，单测用内存缝子类顶替，不必起库。
 *
 * @author fuce
 * @date 2026-09-14
 */
@Service
public class TIchReportRowServiceImpl implements ITIchReportRowService {

    /** 一报行数上限（处里划定的数目，进门前整批过秤） */
    public static final int MAX_ROWS_PER_BATCH = 500;

    /** 批情形：现行落定 */
    public static final int BATCH_STATUS_ACTIVE = 0;
    /** 批情形：已撤报 */
    public static final int BATCH_STATUS_WITHDRAWN = 1;

    /** 往期标记：现行 */
    public static final int PAST_FLAG_CURRENT = 0;
    /** 往期标记：往期 */
    public static final int PAST_FLAG_PAST = 1;

    @javax.annotation.Resource
    private TIchReportRowMapper ichReportRowMapper;

    @javax.annotation.Resource
    private TIchReportBatchMapper ichReportBatchMapper;

    @Override
    public TIchReportRow selectTIchReportRowById(Long id) {
        return this.ichReportRowMapper.selectById(id);
    }

    /**
     * 整批提交：{@link #importBatch} 的简式入口，同一处进门逻辑。
     */
    @Override
    public int submitBatch(String batchNo, List<TIchReportRow> rows) {
        IchImportCmd cmd = new IchImportCmd();
        cmd.setBatchNo(batchNo);
        cmd.setRows(rows);
        IchImportResult result = importBatch(cmd);
        if (result.getOutcome() == IchImportResult.Outcome.REJECTED_OVERSIZE
                || result.getOutcome() == IchImportResult.Outcome.INVALID) {
            return -1;
        }
        // RECORDED 与 DUPLICATE_COMPARED 都回该批落定的收下数：重复提交不得重复入库
        return result.getAcceptNum() == null ? 0 : result.getAcceptNum().intValue();
    }

    /**
     * 进门（唯一导入入口）。批与行在同一事务里落定。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public IchImportResult importBatch(IchImportCmd cmd) {
        if (cmd == null || cmd.getBatchNo() == null || cmd.getBatchNo().trim().isEmpty()) {
            return IchImportResult.invalid(cmd == null ? null : cmd.getBatchNo(), "报头不齐：批号空缺，进不了门");
        }
        String batchNo = cmd.getBatchNo().trim();
        List<TIchReportRow> incoming = cmd.getRows() == null
                ? new ArrayList<TIchReportRow>() : cmd.getRows();

        // 同一批号第二次进门：只着手比对不着手记账，对照完就收手
        TIchReportBatch active = loadActiveBatch(batchNo);
        if (active != null) {
            IchCompareResult compare = ReportBatchJudge.compare(batchNo, loadBatchRows(batchNo), incoming);
            return IchImportResult.duplicate(batchNo, active.getAcceptNum(), active.getRejectNum(),
                    active.getTotalNum(), active.getReceipt(), compare);
        }

        // 进门前整批过一遍秤：越线便整报退回、一条不留
        if (incoming.size() > MAX_ROWS_PER_BATCH) {
            return IchImportResult.oversize(batchNo, incoming.size(), MAX_ROWS_PER_BATCH);
        }

        // 核收：逐行定行次、定去向，同一代号时数往一块儿累，回执同一回算出
        IchImportResult result = ReportBatchJudge.judge(batchNo, incoming, cmd.getYearCapHours());

        TIchReportBatch batch = new TIchReportBatch();
        batch.setBatchNo(batchNo);
        batch.setReportYear(cmd.getReportYear());
        batch.setTotalNum(result.getTotalNum());
        batch.setAcceptNum(result.getAcceptNum());
        batch.setRejectNum(result.getRejectNum());
        batch.setReceipt(result.getReceipt());
        batch.setStatus(Integer.valueOf(BATCH_STATUS_ACTIVE));
        batch.setDelFlag(Integer.valueOf(0));
        if (cmd.getOperator() != null) {
            batch.setCreateBy(cmd.getOperator());
        }
        insertBatch(batch);

        for (TIchReportRow r : result.getRows()) {
            r.setPastFlag(Integer.valueOf(PAST_FLAG_CURRENT));
            r.setDelFlag(Integer.valueOf(0));
            if (cmd.getOperator() != null) {
                r.setCreateBy(cmd.getOperator());
            }
            insertRow(r);
        }
        result.setBatchId(batch.getId());
        return result;
    }

    /**
     * 撤报：确实要改动先办这一步。现行批转已撤报，其行尽数作废。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean withdrawBatch(String batchNo) {
        if (batchNo == null || batchNo.trim().isEmpty()) {
            return false;
        }
        String no = batchNo.trim();
        TIchReportBatch active = loadActiveBatch(no);
        if (active == null) {
            return false;
        }
        active.setStatus(Integer.valueOf(BATCH_STATUS_WITHDRAWN));
        updateBatch(active);
        for (TIchReportRow r : loadBatchRows(no)) {
            r.setDelFlag(Integer.valueOf(1));
            updateRow(r);
        }
        return true;
    }

    /**
     * 往期归档：满两个报送年度仍无去向的退回行转往期，现行批面不再露脸。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int archiveStaleReturnedRows(int currentYear) {
        Map<String, Integer> yearByBatch = new HashMap<String, Integer>();
        for (TIchReportBatch b : loadActiveBatches()) {
            yearByBatch.put(b.getBatchNo(), b.getReportYear());
        }
        int archived = 0;
        for (TIchReportRow r : loadLiveReturnedRows()) {
            Integer reportYear = yearByBatch.get(r.getBatchNo());
            if (ReportBatchJudge.isStaleReturned(reportYear, currentYear)) {
                r.setPastFlag(Integer.valueOf(PAST_FLAG_PAST));
                updateRow(r);
                archived++;
            }
        }
        return archived;
    }

    /**
     * 对账：回执三个数与库里逐行去向对一遍，接不平当场列行。
     */
    @Override
    public IchReconcileResult reconcileBatch(String batchNo) {
        if (batchNo == null || batchNo.trim().isEmpty()) {
            return ReportBatchJudge.reconcile(null, null);
        }
        String no = batchNo.trim();
        return ReportBatchJudge.reconcile(loadActiveBatch(no), loadBatchRows(no));
    }

    @Override
    public TIchReportBatch selectActiveBatch(String batchNo) {
        if (batchNo == null || batchNo.trim().isEmpty()) {
            return null;
        }
        return loadActiveBatch(batchNo.trim());
    }

    /**
     * 该批次的失败行明细：只露现行（往期行不在此露面），按原行次升序。
     */
    @Override
    public List<TIchReportRow> listErrors(String batchNo) {
        List<TIchReportRow> errors = new ArrayList<TIchReportRow>();
        if (batchNo == null) {
            return errors;
        }
        for (TIchReportRow r : loadBatchRows(batchNo.trim())) {
            if (r.getStatus() != null && r.getStatus().intValue() == ReportBatchJudge.STATUS_RETURNED
                    && r.getPastFlag() != null && r.getPastFlag().intValue() == PAST_FLAG_CURRENT) {
                errors.add(r);
            }
        }
        return errors;
    }

    /**
     * 该批次的往期行明细：老报存量翻起来仍旧找得着，按原行次升序。
     */
    @Override
    public List<TIchReportRow> listPastRows(String batchNo) {
        List<TIchReportRow> past = new ArrayList<TIchReportRow>();
        if (batchNo == null) {
            return past;
        }
        for (TIchReportRow r : loadBatchRows(batchNo.trim())) {
            if (r.getPastFlag() != null && r.getPastFlag().intValue() == PAST_FLAG_PAST) {
                past.add(r);
            }
        }
        return past;
    }

    /**
     * 卷面：该批某传承人代号收下的授徒时数累计。
     */
    @Override
    public BigDecimal sumAcceptedHours(String batchNo, String itemCode) {
        if (batchNo == null || itemCode == null) {
            return BigDecimal.ZERO;
        }
        String code = itemCode.trim();
        BigDecimal sum = BigDecimal.ZERO;
        for (TIchReportRow r : loadBatchRows(batchNo.trim())) {
            if (r.getStatus() != null && r.getStatus().intValue() == ReportBatchJudge.STATUS_ACCEPTED
                    && r.getItemCode() != null && code.equals(r.getItemCode().trim())
                    && r.getHoursNum() != null) {
                sum = sum.add(r.getHoursNum());
            }
        }
        return sum;
    }

    /* ---------- 库的进出缝（单测用内存子类顶替，不必起库） ---------- */

    /** 现行落定的那一批（同号撤报再进门会另立一批，取最新落定的现行批） */
    protected TIchReportBatch loadActiveBatch(String batchNo) {
        List<TIchReportBatch> list = this.ichReportBatchMapper.selectList(new QueryWrapper<TIchReportBatch>()
                .eq("batch_no", batchNo)
                .eq("status", BATCH_STATUS_ACTIVE)
                .eq("del_flag", 0)
                .orderByDesc("id"));
        return list.isEmpty() ? null : list.get(0);
    }

    /** 该批库里全部在行（含往期，不含已删），按原行次升序 */
    protected List<TIchReportRow> loadBatchRows(String batchNo) {
        return this.ichReportRowMapper.selectList(new QueryWrapper<TIchReportRow>()
                .eq("batch_no", batchNo)
                .eq("del_flag", 0)
                .orderByAsc("row_no"));
    }

    /** 全部现行落定批（归档算报送年度用） */
    protected List<TIchReportBatch> loadActiveBatches() {
        return this.ichReportBatchMapper.selectList(new QueryWrapper<TIchReportBatch>()
                .eq("status", BATCH_STATUS_ACTIVE)
                .eq("del_flag", 0));
    }

    /** 全部仍压着的退回行（现行、未删、未归档） */
    protected List<TIchReportRow> loadLiveReturnedRows() {
        return this.ichReportRowMapper.selectList(new QueryWrapper<TIchReportRow>()
                .eq("status", ReportBatchJudge.STATUS_RETURNED)
                .eq("del_flag", 0)
                .eq("past_flag", PAST_FLAG_CURRENT));
    }

    /** 落一批（进门那一处才调） */
    protected void insertBatch(TIchReportBatch batch) {
        this.ichReportBatchMapper.insert(batch);
    }

    /** 落一行（进门那一处才调） */
    protected void insertRow(TIchReportRow row) {
        this.ichReportRowMapper.insert(row);
    }

    /** 改一批（撤报改情形） */
    protected void updateBatch(TIchReportBatch batch) {
        this.ichReportBatchMapper.updateById(batch);
    }

    /** 改一行（撤报作废、归档转往期） */
    protected void updateRow(TIchReportRow row) {
        this.ichReportRowMapper.updateById(row);
    }
}
