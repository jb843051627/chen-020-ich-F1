package com.fc.v2.model.custom.ich;

import java.util.ArrayList;
import java.util.List;

import com.fc.v2.model.auto.TIchReportRow;

/**
 * 履职情况报送·对账结果。回执三个数与逐行去向对一遍：
 * 接不平当场把差在哪几行列出来，不拿"大体相符"抹过去。
 *
 * @author fuce
 * @date 2026-09-30
 */
public class IchReconcileResult {

    /** 报送批号 */
    private String batchNo;

    /** 两头是否接平 */
    private boolean balanced;

    /** 回执·本批总行数 */
    private Integer receiptTotal;

    /** 回执·落库数 */
    private Integer receiptAccept;

    /** 回执·退件数 */
    private Integer receiptReject;

    /** 库里实际·总行数 */
    private Integer actualTotal;

    /** 库里实际·收下数 */
    private Integer actualAccept;

    /** 库里实际·退回数 */
    private Integer actualReject;

    /** 差在哪几行（去向未定、抵不上回执数的行，按行次列） */
    private List<TIchReportRow> offRows = new ArrayList<TIchReportRow>();

    /** 一句话说法（接不平时写明差在哪几行） */
    private String message;

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public boolean isBalanced() {
        return balanced;
    }

    public void setBalanced(boolean balanced) {
        this.balanced = balanced;
    }

    public Integer getReceiptTotal() {
        return receiptTotal;
    }

    public void setReceiptTotal(Integer receiptTotal) {
        this.receiptTotal = receiptTotal;
    }

    public Integer getReceiptAccept() {
        return receiptAccept;
    }

    public void setReceiptAccept(Integer receiptAccept) {
        this.receiptAccept = receiptAccept;
    }

    public Integer getReceiptReject() {
        return receiptReject;
    }

    public void setReceiptReject(Integer receiptReject) {
        this.receiptReject = receiptReject;
    }

    public Integer getActualTotal() {
        return actualTotal;
    }

    public void setActualTotal(Integer actualTotal) {
        this.actualTotal = actualTotal;
    }

    public Integer getActualAccept() {
        return actualAccept;
    }

    public void setActualAccept(Integer actualAccept) {
        this.actualAccept = actualAccept;
    }

    public Integer getActualReject() {
        return actualReject;
    }

    public void setActualReject(Integer actualReject) {
        this.actualReject = actualReject;
    }

    public List<TIchReportRow> getOffRows() {
        return offRows;
    }

    public void setOffRows(List<TIchReportRow> offRows) {
        this.offRows = offRows;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
