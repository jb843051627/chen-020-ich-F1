package com.fc.v2.ich;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fc.v2.model.auto.TIchReportBatch;
import com.fc.v2.model.auto.TIchReportRow;
import com.fc.v2.model.custom.ich.IchImportCmd;
import com.fc.v2.model.custom.ich.IchImportResult;
import com.fc.v2.model.custom.ich.IchReconcileResult;
import com.fc.v2.service.impl.ReportBatchJudge;
import com.fc.v2.service.impl.TIchReportRowServiceImpl;

/**
 * 报送流程：进门落定、重复进门只比对、撤报后再进门、超上限整报退回、
 * 往期归档、末了对账。库的进出用内存缝顶替，不必起库。
 */
public class TIchReportRowServiceImplTest {

    /** 内存缝：把 protected 的库进出换成两个 map */
    static class MemService extends TIchReportRowServiceImpl {
        final Map<Long, TIchReportBatch> batches = new LinkedHashMap<Long, TIchReportBatch>();
        final Map<Long, TIchReportRow> rows = new LinkedHashMap<Long, TIchReportRow>();
        long seq = 0;

        @Override
        protected TIchReportBatch loadActiveBatch(String batchNo) {
            TIchReportBatch found = null;
            for (TIchReportBatch b : batches.values()) {
                if (batchNo.equals(b.getBatchNo())
                        && b.getStatus() != null && b.getStatus().intValue() == BATCH_STATUS_ACTIVE
                        && b.getDelFlag() != null && b.getDelFlag().intValue() == 0) {
                    if (found == null || b.getId().longValue() > found.getId().longValue()) {
                        found = b;
                    }
                }
            }
            return found;
        }

        @Override
        protected List<TIchReportRow> loadBatchRows(String batchNo) {
            List<TIchReportRow> list = new ArrayList<TIchReportRow>();
            for (TIchReportRow r : rows.values()) {
                if (batchNo.equals(r.getBatchNo()) && r.getDelFlag() != null && r.getDelFlag().intValue() == 0) {
                    list.add(r);
                }
            }
            list.sort(new Comparator<TIchReportRow>() {
                @Override
                public int compare(TIchReportRow a, TIchReportRow b) {
                    return a.getRowNo().compareTo(b.getRowNo());
                }
            });
            return list;
        }

        @Override
        protected List<TIchReportBatch> loadActiveBatches() {
            List<TIchReportBatch> list = new ArrayList<TIchReportBatch>();
            for (TIchReportBatch b : batches.values()) {
                if (b.getStatus() != null && b.getStatus().intValue() == BATCH_STATUS_ACTIVE
                        && b.getDelFlag() != null && b.getDelFlag().intValue() == 0) {
                    list.add(b);
                }
            }
            return list;
        }

        @Override
        protected List<TIchReportRow> loadLiveReturnedRows() {
            List<TIchReportRow> list = new ArrayList<TIchReportRow>();
            for (TIchReportRow r : rows.values()) {
                if (r.getStatus() != null && r.getStatus().intValue() == ReportBatchJudge.STATUS_RETURNED
                        && r.getDelFlag() != null && r.getDelFlag().intValue() == 0
                        && r.getPastFlag() != null && r.getPastFlag().intValue() == PAST_FLAG_CURRENT) {
                    list.add(r);
                }
            }
            return list;
        }

        @Override
        protected void insertBatch(TIchReportBatch batch) {
            batch.setId(Long.valueOf(++seq));
            batches.put(batch.getId(), batch);
        }

        @Override
        protected void insertRow(TIchReportRow row) {
            row.setId(Long.valueOf(++seq));
            rows.put(row.getId(), row);
        }

        @Override
        protected void updateBatch(TIchReportBatch batch) {
            batches.put(batch.getId(), batch);
        }

        @Override
        protected void updateRow(TIchReportRow row) {
            rows.put(row.getId(), row);
        }
    }

    private static TIchReportRow row(String code, String hours, String qty) {
        TIchReportRow r = new TIchReportRow();
        r.setItemCode(code);
        r.setHoursNum(hours == null ? null : new BigDecimal(hours));
        r.setQty(qty == null ? null : new BigDecimal(qty));
        return r;
    }

    private static IchImportCmd cmd(String batchNo, Integer year, List<TIchReportRow> rows) {
        IchImportCmd cmd = new IchImportCmd();
        cmd.setBatchNo(batchNo);
        cmd.setReportYear(year);
        cmd.setRows(rows);
        return cmd;
    }

    @Test
    public void 进门落定_批与行同一次记账_回执核讫() {
        MemService svc = new MemService();
        IchImportResult r = svc.importBatch(cmd("PC2026-01", Integer.valueOf(2026),
                Arrays.asList(row("CRH001", "40", "3"), row("CRH001", "25", "2"), row("", "9", "1"))));

        assertEquals(IchImportResult.Outcome.RECORDED, r.getOutcome());
        assertEquals(ReportBatchJudge.RECEIPT_VERIFIED, r.getReceipt());
        assertEquals(Integer.valueOf(3), r.getTotalNum());
        assertEquals(Integer.valueOf(2), r.getAcceptNum());
        assertEquals(Integer.valueOf(1), r.getRejectNum());

        // 批回执落了库，三个数与进门结果是同一回算出来的
        TIchReportBatch batch = svc.selectActiveBatch("PC2026-01");
        assertNotNull(batch);
        assertEquals(Integer.valueOf(3), batch.getTotalNum());
        assertEquals(Integer.valueOf(2), batch.getAcceptNum());
        assertEquals(Integer.valueOf(1), batch.getRejectNum());
        assertEquals(ReportBatchJudge.RECEIPT_VERIFIED, batch.getReceipt());

        // 行次按到达先后排定；退回行翻出来直接带着原行次
        List<TIchReportRow> errors = svc.listErrors("PC2026-01");
        assertEquals(1, errors.size());
        assertEquals(Integer.valueOf(3), errors.get(0).getRowNo());

        // 同一代号两行往一块儿累：卷面记六十五
        assertEquals(0, new BigDecimal("65").compareTo(svc.sumAcceptedHours("PC2026-01", "CRH001")));

        // 末了一笔对账：接平
        assertTrue(svc.reconcileBatch("PC2026-01").isBalanced());
    }

    @Test
    public void 小周那一出_同一份报两天到_第二遍只比对不记账() {
        MemService svc = new MemService();
        List<TIchReportRow> sheet = Arrays.asList(
                row("CRH001", "40", "3"), row("CRH002", "30", "2"), row("CRH003", "20", "1"));
        svc.importBatch(cmd("PC2026-02", Integer.valueOf(2026), sheet));

        // 第二天快递又到，一模一样的两沓单子再塞一遍
        IchImportResult again = svc.importBatch(cmd("PC2026-02", Integer.valueOf(2026),
                Arrays.asList(row("CRH001", "40", "3"), row("CRH002", "30", "2"), row("CRH003", "20", "1"))));

        assertEquals(IchImportResult.Outcome.DUPLICATE_COMPARED, again.getOutcome());
        assertTrue(again.getCompare().isIdentical());
        // 行不增也不减：库里还是三行
        assertEquals(3, svc.loadBatchRows("PC2026-02").size());
        // 头一遍落定的去向一字不改，三位传承人的考核不会被判缺报
        for (TIchReportRow r : svc.loadBatchRows("PC2026-02")) {
            assertEquals(ReportBatchJudge.STATUS_ACCEPTED, r.getStatus().intValue());
        }
        assertTrue(svc.reconcileBatch("PC2026-02").isBalanced());
    }

    @Test
    public void 重复进门哪怕报文被改过_也只列差不改库() {
        MemService svc = new MemService();
        svc.importBatch(cmd("PC2026-03", Integer.valueOf(2026),
                Arrays.asList(row("CRH001", "40", "3"), row("CRH002", "30", "2"))));

        IchImportResult again = svc.importBatch(cmd("PC2026-03", Integer.valueOf(2026),
                Arrays.asList(row("CRH001", "99", "3"), row("CRH002", "30", "2"))));

        assertEquals(IchImportResult.Outcome.DUPLICATE_COMPARED, again.getOutcome());
        assertFalse(again.getCompare().isIdentical());
        assertEquals(1, again.getCompare().getDiffs().size());
        assertEquals(Integer.valueOf(1), again.getCompare().getDiffs().get(0).getRowNo());
        // 头一遍落定的结果不会被这一遍抹回去
        assertEquals(0, new BigDecimal("40").compareTo(svc.loadBatchRows("PC2026-03").get(0).getHoursNum()));
    }

    @Test
    public void 确实要改动_先办撤报_再进门另立一批() {
        MemService svc = new MemService();
        svc.importBatch(cmd("PC2026-04", Integer.valueOf(2026),
                Arrays.asList(row("CRH001", "40", "3"))));

        assertTrue(svc.withdrawBatch("PC2026-04"));
        // 撤报后现行册里寻不见该批
        assertNull(svc.selectActiveBatch("PC2026-04"));
        assertTrue(svc.listErrors("PC2026-04").isEmpty());

        // 同一批号再进门：另立一批，重新落定
        IchImportResult r = svc.importBatch(cmd("PC2026-04", Integer.valueOf(2026),
                Arrays.asList(row("CRH001", "45", "3"), row("CRH002", "10", "1"))));
        assertEquals(IchImportResult.Outcome.RECORDED, r.getOutcome());
        assertEquals(Integer.valueOf(2), r.getAcceptNum());
        assertEquals(0, new BigDecimal("45").compareTo(svc.sumAcceptedHours("PC2026-04", "CRH001")));
        assertTrue(svc.reconcileBatch("PC2026-04").isBalanced());

        // 撤一个不存在的批：办不成
        assertFalse(svc.withdrawBatch("PC2026-XX"));
    }

    @Test
    public void 行数越上限_整报退回一条不留_之后同号正常报还能进门() {
        MemService svc = new MemService();
        List<TIchReportRow> tooMany = new ArrayList<TIchReportRow>();
        for (int i = 0; i < TIchReportRowServiceImpl.MAX_ROWS_PER_BATCH + 1; i++) {
            tooMany.add(row("CRH" + i, "1", "1"));
        }
        IchImportResult r = svc.importBatch(cmd("PC2026-05", Integer.valueOf(2026), tooMany));
        assertEquals(IchImportResult.Outcome.REJECTED_OVERSIZE, r.getOutcome());
        // 一条不留：库里寻不见这批
        assertNull(svc.selectActiveBatch("PC2026-05"));
        assertTrue(svc.loadBatchRows("PC2026-05").isEmpty());

        // 县里头减了行再报，同号照常进门
        IchImportResult ok = svc.importBatch(cmd("PC2026-05", Integer.valueOf(2026),
                Arrays.asList(row("CRH001", "10", "1"))));
        assertEquals(IchImportResult.Outcome.RECORDED, ok.getOutcome());
    }

    @Test
    public void submitBatch旧契约_成功行数_空批零_超限负一_重复不重复入库() {
        MemService svc = new MemService();
        int ok = svc.submitBatch("PC2026-06",
                Arrays.asList(row("CRH001", "10", "1"), row("", "5", "1")));
        assertEquals(1, ok);

        // 空批次返回 0（回执落空报）
        assertEquals(0, svc.submitBatch("PC2026-07", new ArrayList<TIchReportRow>()));
        assertEquals(ReportBatchJudge.RECEIPT_EMPTY, svc.selectActiveBatch("PC2026-07").getReceipt());

        // 超上限返回 -1 且整批不入库
        List<TIchReportRow> tooMany = new ArrayList<TIchReportRow>();
        for (int i = 0; i < TIchReportRowServiceImpl.MAX_ROWS_PER_BATCH + 1; i++) {
            tooMany.add(row("CRH" + i, "1", "1"));
        }
        assertEquals(-1, svc.submitBatch("PC2026-08", tooMany));
        assertNull(svc.selectActiveBatch("PC2026-08"));

        // 同一批次重复提交不得重复入库，回的仍是头一遍落定的成功行数
        assertEquals(1, svc.submitBatch("PC2026-06",
                Arrays.asList(row("CRH001", "10", "1"), row("", "5", "1"))));
        assertEquals(2, svc.loadBatchRows("PC2026-06").size());
    }

    @Test
    public void 退回行压满两个报送年度_转往期_现行不露脸_存量翻得着() {
        MemService svc = new MemService();
        svc.importBatch(cmd("PC2024-01", Integer.valueOf(2024),
                Arrays.asList(row("CRH001", "10", "1"), row("", "5", "1"))));
        assertEquals(1, svc.listErrors("PC2024-01").size());

        // 未满两个报送年度：还压着，不动
        assertEquals(0, svc.archiveStaleReturnedRows(2025));
        assertEquals(1, svc.listErrors("PC2024-01").size());

        // 满两个报送年度：转往期，现行批面上不再露脸
        assertEquals(1, svc.archiveStaleReturnedRows(2026));
        assertTrue(svc.listErrors("PC2024-01").isEmpty());

        // 往期存量翻起来仍旧找得着，行次还在
        List<TIchReportRow> past = svc.listPastRows("PC2024-01");
        assertEquals(1, past.size());
        assertEquals(Integer.valueOf(2), past.get(0).getRowNo());

        // 归档不动去向，对账仍旧接平
        assertTrue(svc.reconcileBatch("PC2024-01").isBalanced());

        // 明年的新报不必再被它绊住：新批照进照收
        IchImportResult next = svc.importBatch(cmd("PC2026-09", Integer.valueOf(2026),
                Arrays.asList(row("CRH001", "10", "1"))));
        assertEquals(IchImportResult.Outcome.RECORDED, next.getOutcome());
    }

    @Test
    public void 对账_两处接不平_当场把差在哪几行列出来() {
        MemService svc = new MemService();
        svc.importBatch(cmd("PC2026-10", Integer.valueOf(2026),
                Arrays.asList(row("CRH001", "10", "1"), row("CRH002", "10", "1"))));
        assertTrue(svc.reconcileBatch("PC2026-10").isBalanced());

        // 有人把第2行的去向动成了未收：对账当场点出第2行，不拿"大体相符"抹过去
        TIchReportRow broken = svc.loadBatchRows("PC2026-10").get(1);
        broken.setStatus(Integer.valueOf(ReportBatchJudge.STATUS_PENDING));
        IchReconcileResult r = svc.reconcileBatch("PC2026-10");
        assertFalse(r.isBalanced());
        assertEquals(1, r.getOffRows().size());
        assertEquals(Integer.valueOf(2), r.getOffRows().get(0).getRowNo());
        assertTrue(r.getMessage().contains("第2行"));
    }
}
