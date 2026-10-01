package com.fc.v2.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fc.v2.model.auto.TIchReportBatch;
import com.fc.v2.model.auto.TIchReportRow;
import com.fc.v2.model.custom.ich.IchCompareResult;
import com.fc.v2.model.custom.ich.IchImportResult;
import com.fc.v2.model.custom.ich.IchReconcileResult;

/**
 * 履职情况报送·核收纯逻辑（不碰库，不碰 Spring，可单测）。
 *
 * 规矩：
 * 1. 每一行进批自动占一个行次，按到达先后排定；退回的行也带着原行次。
 * 2. 每一行只有一个去向：收下(1)或退回(2)（未收(0)只是没核收前的占位）。
 * 3. 收下认传承人代号，逐字对上才算，名字读起来像不算对上。
 * 4. 同一代号在一报里占几行，授徒时数往一块儿累；累过省里给这一年定的满额，
 *    超的那一行就地退回并写明为什么，同报其余各行照走不误。
 * 5. 回执三个数（总行数/落库数/退件数）与逐行去向是同一回算出来的；
 *    全报寻不出能收的一行，回执落"空报"，"核讫"只给有数的报。
 *
 * @author fuce
 * @date 2026-09-30
 */
public final class ReportBatchJudge {

    /** 行落地情形：未收（仅核收前的占位，落定后的行不处于此态） */
    public static final int STATUS_PENDING = 0;
    /** 行落地情形：已收下 */
    public static final int STATUS_ACCEPTED = 1;
    /** 行落地情形：已退回 */
    public static final int STATUS_RETURNED = 2;

    /** 回执字样：空报（全报寻不出能收的一行） */
    public static final String RECEIPT_EMPTY = "空报";
    /** 回执字样：核讫（只给有数的报） */
    public static final String RECEIPT_VERIFIED = "核讫";

    /** 省定年度授徒时数满额默认值（报送令未另带时取用） */
    public static final BigDecimal DEFAULT_YEAR_CAP_HOURS = new BigDecimal("120");

    /** 退回行压满几个报送年度仍无去向，转往期 */
    public static final int STALE_REPORT_YEARS = 2;

    private ReportBatchJudge() {
    }

    /**
     * 核收一报：逐行定行次、定去向，同一回算出回执三个数与卷面累计。
     *
     * @param batchNo      报送批号（进门定死，逐行盖上，不许混进别报）
     * @param rows         来报各行，按到达先后
     * @param yearCapHours 省里给这一年定的授徒时数满额；空取默认
     * @return 进门结果（RECORDED），逐行去向与回执同一次算出
     */
    public static IchImportResult judge(String batchNo, List<TIchReportRow> rows, BigDecimal yearCapHours) {
        BigDecimal cap = yearCapHours == null ? DEFAULT_YEAR_CAP_HOURS : yearCapHours;
        List<TIchReportRow> judged = new ArrayList<TIchReportRow>();
        Map<String, BigDecimal> acceptedHours = new LinkedHashMap<String, BigDecimal>();
        int accept = 0;
        int reject = 0;
        int seq = 0;
        if (rows != null) {
            for (TIchReportRow in : rows) {
                seq++;
                TIchReportRow r = new TIchReportRow();
                r.setBatchNo(batchNo);
                r.setRowNo(Integer.valueOf(seq));
                r.setItemCode(in == null ? null : in.getItemCode());
                r.setHoursNum(in == null ? null : in.getHoursNum());
                r.setQty(in == null ? null : in.getQty());

                String reason = firstReason(in);
                if (reason == null) {
                    String code = in.getItemCode().trim();
                    BigDecimal run = acceptedHours.get(code);
                    BigDecimal after = (run == null ? BigDecimal.ZERO : run).add(in.getHoursNum());
                    if (after.compareTo(cap) > 0) {
                        reason = "累计授徒时数" + plain(after) + "超年度满额" + plain(cap);
                    } else {
                        acceptedHours.put(code, after);
                    }
                }
                if (reason == null) {
                    r.setStatus(Integer.valueOf(STATUS_ACCEPTED));
                    accept++;
                } else {
                    r.setStatus(Integer.valueOf(STATUS_RETURNED));
                    r.setRemark(reason);
                    reject++;
                }
                judged.add(r);
            }
        }

        IchImportResult result = new IchImportResult();
        result.setOutcome(IchImportResult.Outcome.RECORDED);
        result.setBatchNo(batchNo);
        result.setTotalNum(Integer.valueOf(seq));
        result.setAcceptNum(Integer.valueOf(accept));
        result.setRejectNum(Integer.valueOf(reject));
        result.setReceipt(accept > 0 ? RECEIPT_VERIFIED : RECEIPT_EMPTY);
        result.setRows(judged);
        result.setAcceptedHours(acceptedHours);
        result.setMessage(accept > 0
                ? "本批" + seq + "行，收下" + accept + "行、退回" + reject + "行，回执核讫"
                : "全报寻不出能收的一行，回执落空报");
        return result;
    }

    /**
     * 行能不能收：认代号不认名音；时数、场数须到场且不为负。能收返回 null，否则返回退回缘由。
     */
    private static String firstReason(TIchReportRow in) {
        if (in == null) {
            return "空行，无物可收";
        }
        if (in.getItemCode() == null || in.getItemCode().trim().isEmpty()) {
            return "传承人代号对不上（代号空缺，名音相近不算）";
        }
        if (in.getHoursNum() == null) {
            return "授徒时数缺失";
        }
        if (in.getHoursNum().compareTo(BigDecimal.ZERO) < 0) {
            return "授徒时数为负";
        }
        if (in.getQty() == null) {
            return "进账展演场数缺失";
        }
        if (in.getQty().compareTo(BigDecimal.ZERO) < 0) {
            return "进账展演场数为负";
        }
        return null;
    }

    /**
     * 重复进门比对：把来的一行一行与库里已落下的行对照一遍就收手。
     * 纯对照，不落任何字。
     *
     * @param stored   库里已落下的行（按原行次升序）
     * @param incoming 这一遍报来的行（按到达先后，第 i 行对库里行次 i+1）
     */
    public static IchCompareResult compare(String batchNo, List<TIchReportRow> stored, List<TIchReportRow> incoming) {
        List<TIchReportRow> s = stored == null ? new ArrayList<TIchReportRow>() : stored;
        List<TIchReportRow> in = incoming == null ? new ArrayList<TIchReportRow>() : incoming;
        IchCompareResult result = new IchCompareResult();
        result.setBatchNo(batchNo);
        result.setStoredCount(s.size());
        result.setIncomingCount(in.size());

        List<IchCompareResult.Diff> diffs = new ArrayList<IchCompareResult.Diff>();
        int matched = 0;
        int max = Math.max(s.size(), in.size());
        for (int i = 0; i < max; i++) {
            TIchReportRow storedRow = i < s.size() ? s.get(i) : null;
            TIchReportRow incomingRow = i < in.size() ? in.get(i) : null;
            int rowNo = i + 1;
            if (storedRow == null) {
                diffs.add(diff(rowNo, IchCompareResult.Diff.KIND_ONLY_INCOMING, null, incomingRow,
                        "第" + rowNo + "行报里有、库里没有"));
                continue;
            }
            if (incomingRow == null) {
                diffs.add(diff(rowNo, IchCompareResult.Diff.KIND_ONLY_STORED, storedRow, null,
                        "第" + rowNo + "行库里有、报里少了"));
                continue;
            }
            List<String> fields = new ArrayList<String>();
            if (!eqCode(storedRow.getItemCode(), incomingRow.getItemCode())) {
                fields.add("item_code");
            }
            if (!eqNum(storedRow.getHoursNum(), incomingRow.getHoursNum())) {
                fields.add("hours_num");
            }
            if (!eqNum(storedRow.getQty(), incomingRow.getQty())) {
                fields.add("qty");
            }
            if (fields.isEmpty()) {
                matched++;
            } else {
                IchCompareResult.Diff d = diff(rowNo, IchCompareResult.Diff.KIND_FIELD_MISMATCH,
                        storedRow, incomingRow, "第" + rowNo + "行对不上：" + join(fields));
                d.setFields(fields);
                diffs.add(d);
            }
        }
        result.setMatchedCount(matched);
        result.setDiffs(diffs);
        result.setIdentical(diffs.isEmpty());
        return result;
    }

    /**
     * 对账：回执三个数与库里逐行去向对一遍。接不平当场把差在哪几行列出来。
     *
     * @param batch 落定批（回执所在）
     * @param rows  该批库里全部在行（含往期，不含已删）
     */
    public static IchReconcileResult reconcile(TIchReportBatch batch, List<TIchReportRow> rows) {
        IchReconcileResult result = new IchReconcileResult();
        result.setBatchNo(batch == null ? null : batch.getBatchNo());
        List<TIchReportRow> safeRows = rows == null ? new ArrayList<TIchReportRow>() : rows;

        int actualAccept = 0;
        int actualReject = 0;
        List<TIchReportRow> offRows = new ArrayList<TIchReportRow>();
        for (TIchReportRow r : safeRows) {
            Integer st = r.getStatus();
            if (st != null && st.intValue() == STATUS_ACCEPTED) {
                actualAccept++;
            } else if (st != null && st.intValue() == STATUS_RETURNED) {
                actualReject++;
            } else {
                // 去向未定的行抵不上回执任何一数，当场列出来
                offRows.add(r);
            }
        }
        int actualTotal = safeRows.size();

        Integer receiptTotal = batch == null ? null : batch.getTotalNum();
        Integer receiptAccept = batch == null ? null : batch.getAcceptNum();
        Integer receiptReject = batch == null ? null : batch.getRejectNum();

        result.setReceiptTotal(receiptTotal);
        result.setReceiptAccept(receiptAccept);
        result.setReceiptReject(receiptReject);
        result.setActualTotal(Integer.valueOf(actualTotal));
        result.setActualAccept(Integer.valueOf(actualAccept));
        result.setActualReject(Integer.valueOf(actualReject));
        result.setOffRows(offRows);

        boolean receiptTies = receiptTotal != null && receiptAccept != null && receiptReject != null
                && receiptAccept.intValue() + receiptReject.intValue() == receiptTotal.intValue();
        boolean balanced = batch != null && receiptTies && offRows.isEmpty()
                && receiptTotal.intValue() == actualTotal
                && receiptAccept.intValue() == actualAccept
                && receiptReject.intValue() == actualReject;
        result.setBalanced(balanced);

        if (balanced) {
            result.setMessage("批" + result.getBatchNo() + "接平：总行" + actualTotal
                    + "＝收下" + actualAccept + "＋退回" + actualReject);
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("批").append(result.getBatchNo()).append("接不平：");
            if (batch == null) {
                sb.append("现行册里寻不见该批；");
            } else {
                if (!receiptTies) {
                    sb.append("回执自身抵不上（总").append(receiptTotal)
                            .append("≠收").append(receiptAccept)
                            .append("＋退").append(receiptReject).append("）；");
                }
                sb.append("回执[").append(receiptTotal).append('/').append(receiptAccept)
                        .append('/').append(receiptReject).append(']')
                        .append("，库里实数[").append(actualTotal).append('/').append(actualAccept)
                        .append('/').append(actualReject).append(']');
            }
            if (!offRows.isEmpty()) {
                sb.append("；去向未定的行：");
                for (int i = 0; i < offRows.size(); i++) {
                    if (i > 0) {
                        sb.append('、');
                    }
                    sb.append("第").append(offRows.get(i).getRowNo()).append("行");
                }
            }
            result.setMessage(sb.toString());
        }
        return result;
    }

    /**
     * 退回行该不该转往期：报送年度满 STALE_REPORT_YEARS 个年度仍无去向即该转。
     * 年度不明的批（老报没记年度）不动，兼容老报。
     */
    public static boolean isStaleReturned(Integer reportYear, int currentYear) {
        return reportYear != null && currentYear - reportYear.intValue() >= STALE_REPORT_YEARS;
    }

    private static IchCompareResult.Diff diff(int rowNo, String kind, TIchReportRow stored,
                                              TIchReportRow incoming, String message) {
        IchCompareResult.Diff d = new IchCompareResult.Diff();
        d.setRowNo(Integer.valueOf(rowNo));
        d.setKind(kind);
        d.setStoredDesc(desc(stored));
        d.setIncomingDesc(desc(incoming));
        d.setMessage(message);
        return d;
    }

    private static String desc(TIchReportRow r) {
        if (r == null) {
            return "（无此行）";
        }
        return "代号" + (r.getItemCode() == null ? "空" : r.getItemCode())
                + " 时数" + (r.getHoursNum() == null ? "空" : plain(r.getHoursNum()))
                + " 场数" + (r.getQty() == null ? "空" : plain(r.getQty()));
    }

    private static boolean eqCode(String a, String b) {
        String ta = a == null ? null : a.trim();
        String tb = b == null ? null : b.trim();
        return ta == null ? tb == null : ta.equals(tb);
    }

    private static boolean eqNum(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) {
            return a == null && b == null;
        }
        return a.compareTo(b) == 0;
    }

    private static String plain(BigDecimal v) {
        return v.stripTrailingZeros().toPlainString();
    }

    private static String join(List<String> fields) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) {
                sb.append('、');
            }
            sb.append(fields.get(i));
        }
        return sb.toString();
    }
}
