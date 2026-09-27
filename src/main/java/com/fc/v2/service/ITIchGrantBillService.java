package com.fc.v2.service;

import com.fc.v2.model.auto.TIchGrantBill;

/**
 * 工坊认定核准单 Service接口（approval-chain 形状：多阶段签批，无增删改查入口）
 *
 * @author fuce
 * @date 2026-09-14
 */
public interface ITIchGrantBillService {

    /** 按主键回查单据 */
    TIchGrantBill selectTIchGrantBillById(Long id);

    /** 签批一票：返回更新后的单据；被拒返回 null */
    TIchGrantBill approve(Long id, String approver, String comment);

    /** 否决：返回更新后的单据；被拒返回 null */
    TIchGrantBill reject(Long id, String approver, String comment);

    /** 退回上一环节：返回更新后的单据；被拒返回 null */
    TIchGrantBill rollback(Long id, String comment);
}
