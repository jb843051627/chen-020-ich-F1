package com.fc.v2.service;

import java.util.Date;
import java.util.List;

import com.fc.v2.model.auto.TIchReportRow;
import com.fc.v2.model.custom.IchReceipt;

/**
 * 履职情况报送核收行 Service接口（batch-process 形状：整批提交，无单条增删改）
 *
 * 进门规矩：
 * 一报一个批号，进门就定死，这一报的行不许混进那一报；每一行进批自动占一个行次，
 * 按先后排定，退回的行也带着原行次。导入只发生在 submitBatch 这一处，别的地方不再导第二回。
 * 每一行只有一个去向：未收(0)、收下(1)、退回(2)；收下认传承人的代号，名字像不算对上。
 * 同一传承人在一报里占多行，行上的时数往一块儿累；累过年度满额，超的那一行就地退回
 * 并写明为什么，同一报里其余各行照走不误。同一个批号第二次进门只比对不记账：
 * 去向一字不改，行不增也不减；确实要改动，先办 withdraw 撤报那一步手续。
 *
 * @author fuce
 * @date 2026-09-14
 */
public interface ITIchReportRowService {

    /** 按主键回查单行 */
    TIchReportRow selectTIchReportRowById(Long id);

    /**
     * 整批提交（唯一的导入口）：逐行校验，收下的入库、退回的带原行次记入失败明细，
     * 返回**本次新收下的行数**。
     * 空批次返回 0 且一行不进；行数超过单批上限返回 -1 且整批不入库（一条不留）；
     * 同一批号已有落地行时，本次只比对不记账，返回 0，库中一字不改。
     */
    int submitBatch(String batchNo, List<TIchReportRow> rows);

    /** 该批次的失败行明细（按行号升序，退回行带着原行次） */
    List<TIchReportRow> listErrors(String batchNo);

    /**
     * 回执：只落三个数——本批总行数、落库数、退件数，后两项加起来抵得上前一项；
     * 三数从库中落地行同一回算出（不是屏上临时数一遍），抽任何一行复核都对得上。
     * 全报寻不出能收的一行落"空报"；"核讫"只给有数的报。
     */
    IchReceipt receipt(String batchNo);

    /**
     * 对账：列出回执三数接不平的差行（落地行里去往不是收下/退回的，即总行数
     * 抵不上落库+退件的那几行）。返回空即接平；不空则差在哪几行当场列出，
     * 不拿"大体相符"抹过去。
     */
    List<TIchReportRow> listUnreconciled(String batchNo);

    /**
     * 撤报：确实要改动已落地的批，先办这一步——整批注销（留痕不真删），
     * 批号腾出后才可重新进门；返回注销行数。
     */
    int withdraw(String batchNo);

    /**
     * 往期归档：退回满两个报送年度仍无去向的行转到往期那一层，现行批面上不再露脸，
     * 存量翻起来仍旧找得着（兼容老报），明年的新报不再被它绊住。返回本次转档行数。
     */
    int archiveStaleRejected(Date now);

    /** 往期层翻旧档：该批已转往期的行（按行号升序） */
    List<TIchReportRow> listArchived(String batchNo);
}
