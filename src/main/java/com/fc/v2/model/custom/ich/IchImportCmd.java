package com.fc.v2.model.custom.ich;

import java.math.BigDecimal;
import java.util.List;

import com.fc.v2.model.auto.TIchReportRow;

/**
 * 履职情况报送·进门报文。一报一个批号，进门就定死；
 * 行按到达先后排，行上的批号以报头为准（这一报的行不许混进那一报）。
 *
 * @author fuce
 * @date 2026-09-30
 */
public class IchImportCmd {

    /** 报送批号（必填，进门定死） */
    private String batchNo;

    /** 报送年度（归档算"满两个报送年度"用，可空；空则该批不参与归档） */
    private Integer reportYear;

    /** 本报各行，按到达先后排列；每行只需带 itemCode/hoursNum/qty */
    private List<TIchReportRow> rows;

    /** 省里给这一年定的授徒时数满额；空则取 ReportBatchJudge.DEFAULT_YEAR_CAP_HOURS */
    private BigDecimal yearCapHours;

    /** 经手人（落 create_by，可空） */
    private String operator;

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public Integer getReportYear() {
        return reportYear;
    }

    public void setReportYear(Integer reportYear) {
        this.reportYear = reportYear;
    }

    public List<TIchReportRow> getRows() {
        return rows;
    }

    public void setRows(List<TIchReportRow> rows) {
        this.rows = rows;
    }

    public BigDecimal getYearCapHours() {
        return yearCapHours;
    }

    public void setYearCapHours(BigDecimal yearCapHours) {
        this.yearCapHours = yearCapHours;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }
}
