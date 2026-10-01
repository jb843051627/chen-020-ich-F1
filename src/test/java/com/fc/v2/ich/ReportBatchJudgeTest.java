package com.fc.v2.ich;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fc.v2.model.auto.TIchReportBatch;
import com.fc.v2.model.auto.TIchReportRow;
import com.fc.v2.model.custom.ich.IchCompareResult;
import com.fc.v2.model.custom.ich.IchImportResult;
import com.fc.v2.model.custom.ich.IchReconcileResult;
import com.fc.v2.service.impl.ReportBatchJudge;

/**
 * 核收纯逻辑：行次、三态、累加满额、回执字样、比对、对账、归档边界。
 */
public class ReportBatchJudgeTest {

    private static TIchReportRow row(String code, String hours, String qty) {
        TIchReportRow r = new TIchReportRow();
        r.setItemCode(code);
        r.setHoursNum(hours == null ? null : new BigDecimal(hours));
        r.setQty(qty == null ? null : new BigDecimal(qty));
        return r;
    }

    @Test
    public void 行次按到达先后排定_退回行也带原行次() {
        IchImportResult r = ReportBatchJudge.judge("PC2026-01",
                Arrays.asList(row("CRH001", "40", "3"), row("", "10", "1"), row("CRH002", "20", "2")), null);
        assertEquals(IchImportResult.Outcome.RECORDED, r.getOutcome());
        assertEquals(Integer.valueOf(1), r.getRows().get(0).getRowNo());
        assertEquals(Integer.valueOf(2), r.getRows().get(1).getRowNo());
        assertEquals(Integer.valueOf(3), r.getRows().get(2).getRowNo());
        // 第2行代号空缺被退回，行次照旧带着
        assertEquals(ReportBatchJudge.STATUS_RETURNED, r.getRows().get(1).getStatus().intValue());
        assertEquals(Integer.valueOf(2), r.getRows().get(1).getRowNo());
        assertTrue(r.getRows().get(1).getRemark().contains("代号"));
    }

    @Test
    public void 收下认代号不认名音_缺行要件退回_其余各行照走不误() {
        IchImportResult r = ReportBatchJudge.judge("PC2026-02",
                Arrays.asList(
                        row("CRH001", "40", "3"),   // 收
                        row(null, "10", "1"),        // 退：代号空缺（名音相近不算）
                        row("CRH002", null, "1"),    // 退：时数缺失
                        row("CRH003", "-1", "1"),    // 退：时数为负
                        row("CRH004", "10", null),   // 退：场数缺失
                        row("CRH005", "10", "-2"),   // 退：场数为负
                        row("CRH006", "0", "0")),    // 收：今年没开课没展演也是实数
                null);
        assertEquals(Integer.valueOf(7), r.getTotalNum());
        assertEquals(Integer.valueOf(2), r.getAcceptNum());
        assertEquals(Integer.valueOf(5), r.getRejectNum());
        assertEquals(ReportBatchJudge.STATUS_ACCEPTED, r.getRows().get(0).getStatus().intValue());
        assertEquals(ReportBatchJudge.STATUS_ACCEPTED, r.getRows().get(6).getStatus().intValue());
        for (int i = 1; i <= 5; i++) {
            assertEquals(ReportBatchJudge.STATUS_RETURNED, r.getRows().get(i).getStatus().intValue());
            assertTrue(r.getRows().get(i).getRemark() != null && !r.getRows().get(i).getRemark().isEmpty());
        }
    }

    @Test
    public void 同一代号多行时数往一块儿累_卷面记六十五() {
        IchImportResult r = ReportBatchJudge.judge("PC2026-03",
                Arrays.asList(row("CRH001", "40", "3"), row("CRH001", "25", "2")), null);
        assertEquals(Integer.valueOf(2), r.getAcceptNum());
        // 头一行四十、第二行二十五，卷面记六十五
        assertEquals(0, new BigDecimal("65").compareTo(r.getAcceptedHours().get("CRH001")));
    }

    @Test
    public void 累过年度满额_超的那一行就地退回并写明为什么_同报其余各行照走() {
        IchImportResult r = ReportBatchJudge.judge("PC2026-04",
                Arrays.asList(
                        row("CRH001", "40", "3"),   // 收：累计40
                        row("CRH001", "25", "2"),   // 退：累计65超满额60
                        row("CRH002", "10", "1")),  // 收：别人照走不误
                new BigDecimal("60"));
        assertEquals(ReportBatchJudge.STATUS_ACCEPTED, r.getRows().get(0).getStatus().intValue());
        assertEquals(ReportBatchJudge.STATUS_RETURNED, r.getRows().get(1).getStatus().intValue());
        assertTrue(r.getRows().get(1).getRemark().contains("65"));
        assertTrue(r.getRows().get(1).getRemark().contains("60"));
        assertEquals(ReportBatchJudge.STATUS_ACCEPTED, r.getRows().get(2).getStatus().intValue());
        // 超的那一行退回后不挤占累计：卷面只记收下的四十
        assertEquals(0, new BigDecimal("40").compareTo(r.getAcceptedHours().get("CRH001")));
        assertEquals(Integer.valueOf(2), r.getAcceptNum());
        assertEquals(Integer.valueOf(1), r.getRejectNum());
    }

    @Test
    public void 超额退回的行不占累计_后到的行接着前一笔收下的数走() {
        IchImportResult r = ReportBatchJudge.judge("PC2026-05",
                Arrays.asList(
                        row("CRH001", "40", "1"),   // 收：累计40
                        row("CRH001", "25", "1"),   // 退：65超60，不占累计
                        row("CRH001", "15", "1")),  // 收：40+15=55
                new BigDecimal("60"));
        assertEquals(ReportBatchJudge.STATUS_ACCEPTED, r.getRows().get(2).getStatus().intValue());
        assertEquals(0, new BigDecimal("55").compareTo(r.getAcceptedHours().get("CRH001")));
    }

    @Test
    public void 全报寻不出能收的一行_回执落空报_核讫只给有数的报() {
        IchImportResult empty = ReportBatchJudge.judge("PC2026-06",
                Arrays.asList(row("", "10", "1"), row(null, "5", "1")), null);
        assertEquals(ReportBatchJudge.RECEIPT_EMPTY, empty.getReceipt());
        assertEquals(Integer.valueOf(0), empty.getAcceptNum());
        assertEquals(Integer.valueOf(2), empty.getRejectNum());

        IchImportResult zero = ReportBatchJudge.judge("PC2026-07", new ArrayList<TIchReportRow>(), null);
        assertEquals(ReportBatchJudge.RECEIPT_EMPTY, zero.getReceipt());
        assertEquals(Integer.valueOf(0), zero.getTotalNum());

        IchImportResult ok = ReportBatchJudge.judge("PC2026-08",
                Arrays.asList(row("CRH001", "10", "1")), null);
        assertEquals(ReportBatchJudge.RECEIPT_VERIFIED, ok.getReceipt());
    }

    @Test
    public void 回执三个数同一回算出_落库加退件抵得上总行() {
        IchImportResult r = ReportBatchJudge.judge("PC2026-09",
                Arrays.asList(row("CRH001", "10", "1"), row("", "5", "1"), row("CRH002", "5", "1")), null);
        assertEquals(Integer.valueOf(3), r.getTotalNum());
        assertEquals(Integer.valueOf(2), r.getAcceptNum());
        assertEquals(Integer.valueOf(1), r.getRejectNum());
        assertEquals(r.getTotalNum().intValue(),
                r.getAcceptNum().intValue() + r.getRejectNum().intValue());
    }

    @Test
    public void 重复进门比对_一模一样就是一模一样() {
        List<TIchReportRow> stored = new ArrayList<TIchReportRow>();
        TIchReportRow s1 = row("CRH001", "40.0", "3");
        s1.setRowNo(Integer.valueOf(1));
        stored.add(s1);
        TIchReportRow s2 = row("CRH002", "20", "2");
        s2.setRowNo(Integer.valueOf(2));
        stored.add(s2);

        // 40 与 40.00 是同一个数，不算对不上
        IchCompareResult c = ReportBatchJudge.compare("PC2026-10", stored,
                Arrays.asList(row("CRH001", "40.00", "3"), row("CRH002", "20", "2")));
        assertTrue(c.isIdentical());
        assertEquals(2, c.getMatchedCount());
        assertTrue(c.getDiffs().isEmpty());
    }

    @Test
    public void 重复进门比对_差在哪几行列哪几行() {
        List<TIchReportRow> stored = new ArrayList<TIchReportRow>();
        TIchReportRow s1 = row("CRH001", "40", "3");
        s1.setRowNo(Integer.valueOf(1));
        stored.add(s1);
        TIchReportRow s2 = row("CRH002", "20", "2");
        s2.setRowNo(Integer.valueOf(2));
        stored.add(s2);

        IchCompareResult c = ReportBatchJudge.compare("PC2026-11", stored,
                Arrays.asList(row("CRH001", "45", "3"), row("CRH009", "20", "2"), row("CRH003", "1", "1")));
        assertFalse(c.isIdentical());
        assertEquals(3, c.getDiffs().size());
        // 第1行时数对不上
        assertEquals(Integer.valueOf(1), c.getDiffs().get(0).getRowNo());
        assertEquals(IchCompareResult.Diff.KIND_FIELD_MISMATCH, c.getDiffs().get(0).getKind());
        assertEquals(Arrays.asList("hours_num"), c.getDiffs().get(0).getFields());
        // 第2行代号对不上
        assertEquals(Arrays.asList("item_code"), c.getDiffs().get(1).getFields());
        // 第3行报里有、库里没有
        assertEquals(IchCompareResult.Diff.KIND_ONLY_INCOMING, c.getDiffs().get(2).getKind());

        // 报里少一行：库里有、报里缺
        IchCompareResult c2 = ReportBatchJudge.compare("PC2026-11", stored,
                Arrays.asList(row("CRH001", "40", "3")));
        assertEquals(1, c2.getDiffs().size());
        assertEquals(IchCompareResult.Diff.KIND_ONLY_STORED, c2.getDiffs().get(0).getKind());
        assertEquals(Integer.valueOf(2), c2.getDiffs().get(0).getRowNo());
    }

    @Test
    public void 对账_接得平就是平_接不平当场列行() {
        TIchReportBatch batch = new TIchReportBatch();
        batch.setBatchNo("PC2026-12");
        batch.setTotalNum(Integer.valueOf(3));
        batch.setAcceptNum(Integer.valueOf(2));
        batch.setRejectNum(Integer.valueOf(1));

        List<TIchReportRow> rows = new ArrayList<TIchReportRow>();
        TIchReportRow r1 = row("CRH001", "10", "1");
        r1.setRowNo(Integer.valueOf(1));
        r1.setStatus(Integer.valueOf(ReportBatchJudge.STATUS_ACCEPTED));
        rows.add(r1);
        TIchReportRow r2 = row("CRH002", "10", "1");
        r2.setRowNo(Integer.valueOf(2));
        r2.setStatus(Integer.valueOf(ReportBatchJudge.STATUS_ACCEPTED));
        rows.add(r2);
        TIchReportRow r3 = row("CRH003", "10", "1");
        r3.setRowNo(Integer.valueOf(3));
        r3.setStatus(Integer.valueOf(ReportBatchJudge.STATUS_RETURNED));
        rows.add(r3);

        IchReconcileResult ok = ReportBatchJudge.reconcile(batch, rows);
        assertTrue(ok.isBalanced());

        // 抽一行出来还是未收：去向与回执不是同一回算的，当场点出第几行
        rows.get(1).setStatus(Integer.valueOf(ReportBatchJudge.STATUS_PENDING));
        IchReconcileResult bad = ReportBatchJudge.reconcile(batch, rows);
        assertFalse(bad.isBalanced());
        assertEquals(1, bad.getOffRows().size());
        assertEquals(Integer.valueOf(2), bad.getOffRows().get(0).getRowNo());
        assertTrue(bad.getMessage().contains("第2行"));

        // 回执数被人改过：两头接不平
        rows.get(1).setStatus(Integer.valueOf(ReportBatchJudge.STATUS_ACCEPTED));
        batch.setAcceptNum(Integer.valueOf(1));
        IchReconcileResult bad2 = ReportBatchJudge.reconcile(batch, rows);
        assertFalse(bad2.isBalanced());
        assertTrue(bad2.getMessage().contains("回执"));
    }

    @Test
    public void 归档边界_满两个报送年度才转往期() {
        assertFalse(ReportBatchJudge.isStaleReturned(Integer.valueOf(2024), 2025));
        assertTrue(ReportBatchJudge.isStaleReturned(Integer.valueOf(2024), 2026));
        assertTrue(ReportBatchJudge.isStaleReturned(Integer.valueOf(2024), 2027));
        // 老报没记年度的不动，兼容老报
        assertFalse(ReportBatchJudge.isStaleReturned(null, 2030));
    }
}
