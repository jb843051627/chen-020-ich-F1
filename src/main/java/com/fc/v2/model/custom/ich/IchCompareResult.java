package com.fc.v2.model.custom.ich;

import java.util.ArrayList;
import java.util.List;

/**
 * 履职情况报送·重复进门比对结果。同一批号第二次进门，只把来的一行一行
 * 与库里已落下的行对照一遍：去向一字不改，行不增也不减。
 *
 * @author fuce
 * @date 2026-09-30
 */
public class IchCompareResult {

    /** 报送批号 */
    private String batchNo;

    /** 库里已落下的行数 */
    private int storedCount;

    /** 这一遍报来的行数 */
    private int incomingCount;

    /** 逐行对上的行数 */
    private int matchedCount;

    /** 两遍是否一模一样 */
    private boolean identical;

    /** 差在哪几行（一模一样时为空） */
    private List<Diff> diffs = new ArrayList<Diff>();

    /**
     * 一行对不上的明细
     */
    public static class Diff {
        /** 对不上：行上的字段与库里不一致 */
        public static final String KIND_FIELD_MISMATCH = "FIELD_MISMATCH";
        /** 报里有、库里没有这一行 */
        public static final String KIND_ONLY_INCOMING = "ONLY_INCOMING";
        /** 库里有、报里少了这一行 */
        public static final String KIND_ONLY_STORED = "ONLY_STORED";

        /** 原报行次 */
        private Integer rowNo;

        /** 差的种类 */
        private String kind;

        /** 对不上的字段名（KIND_FIELD_MISMATCH 时） */
        private List<String> fields = new ArrayList<String>();

        /** 库里那行的写照 */
        private String storedDesc;

        /** 报来那行的写照 */
        private String incomingDesc;

        /** 一句话说法 */
        private String message;

        public Integer getRowNo() {
            return rowNo;
        }

        public void setRowNo(Integer rowNo) {
            this.rowNo = rowNo;
        }

        public String getKind() {
            return kind;
        }

        public void setKind(String kind) {
            this.kind = kind;
        }

        public List<String> getFields() {
            return fields;
        }

        public void setFields(List<String> fields) {
            this.fields = fields;
        }

        public String getStoredDesc() {
            return storedDesc;
        }

        public void setStoredDesc(String storedDesc) {
            this.storedDesc = storedDesc;
        }

        public String getIncomingDesc() {
            return incomingDesc;
        }

        public void setIncomingDesc(String incomingDesc) {
            this.incomingDesc = incomingDesc;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public int getStoredCount() {
        return storedCount;
    }

    public void setStoredCount(int storedCount) {
        this.storedCount = storedCount;
    }

    public int getIncomingCount() {
        return incomingCount;
    }

    public void setIncomingCount(int incomingCount) {
        this.incomingCount = incomingCount;
    }

    public int getMatchedCount() {
        return matchedCount;
    }

    public void setMatchedCount(int matchedCount) {
        this.matchedCount = matchedCount;
    }

    public boolean isIdentical() {
        return identical;
    }

    public void setIdentical(boolean identical) {
        this.identical = identical;
    }

    public List<Diff> getDiffs() {
        return diffs;
    }

    public void setDiffs(List<Diff> diffs) {
        this.diffs = diffs;
    }
}
