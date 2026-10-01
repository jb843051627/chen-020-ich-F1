package com.fc.v2.service;

import java.math.BigDecimal;
import java.util.List;

import com.fc.v2.model.auto.TIchReportBatch;
import com.fc.v2.model.auto.TIchReportRow;
import com.fc.v2.model.custom.ich.IchImportCmd;
import com.fc.v2.model.custom.ich.IchImportResult;
import com.fc.v2.model.custom.ich.IchReconcileResult;

/**
 * 履职情况报送核收行 Service接口（batch-process 形状：整批提交，无单条增删改）
 *
 * 门规：
 * 1. 一报一个批号，进门就定死；一年里来几报都行，这一报的行不许混进那一报。
 * 2. 导入只发生在进门那一处（{@link #importBatch}，{@link #submitBatch} 是它的简式入口），
 *    别的地方不再导第二回；重复进门只比对不记账，要改动先办撤报。
 * 3. 每一行只有一个去向：未收、收下、退回；退回的行写清缘由、带着原行次。
 * 4. 回执只落三个数——总行数、落库数、退件数，与逐行去向同一回算出，经得起复核。
 *
 * @author fuce
 * @date 2026-09-14
 */
public interface ITIchReportRowService {

    /** 按主键回查单行 */
    TIchReportRow selectTIchReportRowById(Long id);

    /**
     * 整批提交（{@link #importBatch} 的简式入口，同一处进门逻辑）：逐行校验，
     * 合法的入库、非法的记入失败明细（保留原始行号），返回**成功行数**。
     * 同一批次重复提交不得重复入库（只比对，返回该批已落定的成功行数）；
     * 行数超过单批上限返回 -1 且整批不入库；空批次返回 0。
     */
    int submitBatch(String batchNo, List<TIchReportRow> rows);

    /**
     * 进门（唯一导入入口）。
     * 批号头一遍进门：整批过秤（越上限整报退回、一条不留）→逐行核收（行次、去向、
     * 累加满额）→批与行同一次落定，回执三个数与逐行去向同一回算出。
     * 同一批号第二次进门：只比对不记账，头一遍落定的结果不被抹回去。
     */
    IchImportResult importBatch(IchImportCmd cmd);

    /**
     * 撤报：确实要改动先办这一步。现行批转"已撤报"，其行尽数作废（逻辑删除）；
     * 撤报后同一批号可再进门另立一批。批不在现行册返回 false。
     */
    boolean withdrawBatch(String batchNo);

    /**
     * 往期归档：退回的行不许一直压着——满两个报送年度仍无去向的转往期，
     * 现行批面上不再露脸，往期存量翻起来仍旧找得着。返回本次归档行数。
     */
    int archiveStaleReturnedRows(int currentYear);

    /**
     * 对账：回执三个数与库里逐行去向对一遍；接不平当场把差在哪几行列出来。
     */
    IchReconcileResult reconcileBatch(String batchNo);

    /** 现行批回执（批号查现行落定的那一批；查无返回 null） */
    TIchReportBatch selectActiveBatch(String batchNo);

    /** 该批次的失败行明细（按行号升序，只露现行；往期行不在此露面） */
    List<TIchReportRow> listErrors(String batchNo);

    /** 该批次的往期行明细（按行号升序；老报存量翻起来仍旧找得着） */
    List<TIchReportRow> listPastRows(String batchNo);

    /** 卷面：该批某传承人代号收下的授徒时数累计（同一代号多行往一块儿累） */
    BigDecimal sumAcceptedHours(String batchNo, String itemCode);
}
