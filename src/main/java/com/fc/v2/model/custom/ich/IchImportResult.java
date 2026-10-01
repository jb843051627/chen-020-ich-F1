package com.fc.v2.model.custom.ich;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fc.v2.model.auto.TIchReportRow;

/**
 * 履职情况报送·进门结果。回执三个数（总行数/落库数/退件数）与逐行去向
 * 是同一回核收算出来的，不是屏上临时数的。
 *
 * @author fuce
 * @date 2026-09-30
 */
public class IchImportResult {

    /** 进门落点 */
    public enum Outcome {
        /** 已落定：批与行同一次记账，回执已出 */
        RECORDED,
        /** 重复进门：同一批号已有落定批，只比对不记账，库里一字未改 */
        DUPLICATE_COMPARED,
        /** 整报退回：行数越过处里划定的上限，一条不留 */
        REJECTED_OVERSIZE,
        /** 报头不齐：批号空缺等，未进门 */
        INVALID
    }

    /** 进门落点 */
    private Outcome outcome;

    /** 报送批号 */
    private String batchNo;

    /** 落定批主键（RECORDED 时回） */
    private Long batchId;

    /** 回执·本批总行数 */
    private Integer totalNum;

    /** 回执·落库数（收下） */
    private Integer acceptNum;

    /** 回执·退件数（退回） */
    private Integer rejectNum;

    /** 回执字样 空报/核讫 */
    private String receipt;

    /** 单批行数上限（REJECTED_OVERSIZE 时带出） */
    private Integer maxRows;

    /** 逐行核收结果（RECORDED 时：行次/去向/退回缘由都已落定） */
    private List<TIchReportRow> rows = new ArrayList<TIchReportRow>();

    /** 卷面：各传承人代号收下的授徒时数累计（同一代号多行往一块儿累） */
    private Map<String, BigDecimal> acceptedHours = new LinkedHashMap<String, BigDecimal>();

    /** 比对结果（DUPLICATE_COMPARED 时带出） */
    private IchCompareResult compare;

    /** 一句话说法 */
    private String message;

    public static IchImportResult invalid(String batchNo, String message) {
        IchImportResult r = new IchImportResult();
        r.setOutcome(Outcome.INVALID);
        r.setBatchNo(batchNo);
        r.setMessage(message);
        return r;
    }

    public static IchImportResult oversize(String batchNo, int total, int maxRows) {
        IchImportResult r = new IchImportResult();
        r.setOutcome(Outcome.REJECTED_OVERSIZE);
        r.setBatchNo(batchNo);
        r.setTotalNum(Integer.valueOf(total));
        r.setAcceptNum(Integer.valueOf(0));
        r.setRejectNum(Integer.valueOf(0));
        r.setMaxRows(Integer.valueOf(maxRows));
        r.setMessage("本批" + total + "行，越处里划定的" + maxRows + "行上限，整报退回、一条不留");
        return r;
    }

    public static IchImportResult duplicate(String batchNo, Integer acceptNum, Integer rejectNum,
                                            Integer totalNum, String receipt, IchCompareResult compare) {
        IchImportResult r = new IchImportResult();
        r.setOutcome(Outcome.DUPLICATE_COMPARED);
        r.setBatchNo(batchNo);
        r.setTotalNum(totalNum);
        r.setAcceptNum(acceptNum);
        r.setRejectNum(rejectNum);
        r.setReceipt(receipt);
        r.setCompare(compare);
        r.setMessage("批号" + batchNo + "头一遍已落定，这一遍只比对不记账；要改动先办撤报");
        return r;
    }

    public Outcome getOutcome() {
        return outcome;
    }

    public void setOutcome(Outcome outcome) {
        this.outcome = outcome;
    }

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public Long getBatchId() {
        return batchId;
    }

    public void setBatchId(Long batchId) {
        this.batchId = batchId;
    }

    public Integer getTotalNum() {
        return totalNum;
    }

    public void setTotalNum(Integer totalNum) {
        this.totalNum = totalNum;
    }

    public Integer getAcceptNum() {
        return acceptNum;
    }

    public void setAcceptNum(Integer acceptNum) {
        this.acceptNum = acceptNum;
    }

    public Integer getRejectNum() {
        return rejectNum;
    }

    public void setRejectNum(Integer rejectNum) {
        this.rejectNum = rejectNum;
    }

    public String getReceipt() {
        return receipt;
    }

    public void setReceipt(String receipt) {
        this.receipt = receipt;
    }

    public Integer getMaxRows() {
        return maxRows;
    }

    public void setMaxRows(Integer maxRows) {
        this.maxRows = maxRows;
    }

    public List<TIchReportRow> getRows() {
        return rows;
    }

    public void setRows(List<TIchReportRow> rows) {
        this.rows = rows;
    }

    public Map<String, BigDecimal> getAcceptedHours() {
        return acceptedHours;
    }

    public void setAcceptedHours(Map<String, BigDecimal> acceptedHours) {
        this.acceptedHours = acceptedHours;
    }

    public IchCompareResult getCompare() {
        return compare;
    }

    public void setCompare(IchCompareResult compare) {
        this.compare = compare;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
