package com.fc.v2.model.custom;

import java.io.Serializable;

/**
 * 报送回执：一报只落三个数——本批总行数、落库数、退件数，外加"核讫/空报"字样。
 * 三数与库中落地行同一回算出（落库数+退件数必须抵得上总行数），经得起抽行复核；
 * 全报寻不出能收的一行落"空报"，"核讫"只给有数的报。
 *
 * @author fuce
 * @date 2026-10-01
 */
public class IchReceipt implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 有数可核的报才用 */
    public static final String LABEL_VERIFIED = "核讫";
    /** 全报寻不出能收的一行 */
    public static final String LABEL_EMPTY = "空报";

    /** 履职情况报送批号 */
    private String batchNo;

    /** 本批总行数 */
    private int totalRows;

    /** 落库数（收下的行数） */
    private int acceptedRows;

    /** 退件数（退回的行数） */
    private int rejectedRows;

    /** 回执字样：核讫 / 空报 */
    private String label;

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public int getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(int totalRows) {
        this.totalRows = totalRows;
    }

    public int getAcceptedRows() {
        return acceptedRows;
    }

    public void setAcceptedRows(int acceptedRows) {
        this.acceptedRows = acceptedRows;
    }

    public int getRejectedRows() {
        return rejectedRows;
    }

    public void setRejectedRows(int rejectedRows) {
        this.rejectedRows = rejectedRows;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    /** 落库数+退件数是否抵得上总行数；抵不上就去查差在哪几行 */
    public boolean isBalanced() {
        return acceptedRows + rejectedRows == totalRows;
    }

    @Override
    public String toString() {
        return "IchReceipt{batchNo=" + batchNo + ", totalRows=" + totalRows
                + ", acceptedRows=" + acceptedRows + ", rejectedRows=" + rejectedRows
                + ", label=" + label + "}";
    }
}
