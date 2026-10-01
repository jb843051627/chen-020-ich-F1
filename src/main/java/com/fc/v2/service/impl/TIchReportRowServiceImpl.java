package com.fc.v2.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fc.v2.mapper.auto.TIchReportRowMapper;
import com.fc.v2.model.auto.TIchReportRow;
import com.fc.v2.model.custom.IchReceipt;
import com.fc.v2.service.ITIchReportRowService;

/**
 * 履职情况报送核收行 Service业务层处理（batch-process 形状：整批提交）
 *
 * 一报一个批号，进门定死；每行自动占行次；同一批号重复进门只比对不记账；
 * 收下认代号，同代号多行时数累加，累过年度满额的那一行就地退回并写明原因；
 * 行数越上限整报退回一条不留；回执三数与落地行同一回算出；退回满两个报送
 * 年度的转往期层，现行批面不再露脸、存量仍翻得着。
 *
 * @author fuce
 * @date 2026-09-14
 */
@Service
public class TIchReportRowServiceImpl implements ITIchReportRowService {

    /** 单批行数上限（处里划定）：进门前整批过秤，越线整报退回、一条不留 */
    private static final int MAX_ROWS = 500;

    /** 年度授徒时数满额（省里给这一年定的线）：同一传承人一报之内累计累过此线，超的那一行就地退回 */
    private static final BigDecimal YEAR_HOURS_CAP = new BigDecimal("100");

    /** 退回行压满几个报送年度后转往期 */
    private static final int ARCHIVE_AFTER_YEARS = 2;

    /** 未收 */
    private static final int STATUS_PENDING = 0;
    /** 收下 */
    private static final int STATUS_OK = 1;
    /** 退回 */
    private static final int STATUS_FAIL = 2;
    /** 已转往期（现行批面不露脸，存量仍翻得着） */
    private static final int STATUS_ARCHIVED = 3;

    @javax.annotation.Resource
    private TIchReportRowMapper ichReportRowMapper;

    @Override
    public TIchReportRow selectTIchReportRowById(Long id) {
        return this.ichReportRowMapper.selectById(id);
    }

    /**
     * 整批提交，唯一的导入口。整批落库要么全落要么全不落后半批卡壳，
     * 故加事务：绝不留半批在库里把同一批号顶成"已进门"。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int submitBatch(String batchNo, List<TIchReportRow> rows) {
        // 空批次：一行不进，回执那头自会落"空报"
        if (rows == null || rows.isEmpty()) {
            return 0;
        }
        // 一报一个批号，进门就定死：以进门批号为准，行上原带的号不顶事
        String no = resolveBatchNo(batchNo, rows);
        if (no == null) {
            return 0;
        }
        // 整批过秤：越线整报退回、一条不留
        if (rows.size() > MAX_ROWS) {
            return -1;
        }
        // 同一个批号第二次进门：只比对不记账——去向一字不改，行不增也不减，
        // 头一遍落定的结果不被这一遍抹回去；确实要改动，先办撤报
        if (countCurrentRows(no) > 0) {
            return 0;
        }
        // 同一传承人在一报里占几行，时数往一块儿累（卷面按本报累计，批与批不互挤）
        Map<String, BigDecimal> running = new HashMap<String, BigDecimal>();
        int accepted = 0;
        int seq = 0;
        for (TIchReportRow r : rows) {
            if (r == null) {
                continue;
            }
            seq++;
            r.setBatchNo(no);
            // 每一行进批自动占一个行次，按先后排定；退回的行也带着原行次
            r.setRowNo(Integer.valueOf(seq));
            r.setDelFlag(Integer.valueOf(0));
            String code = r.getItemCode() == null ? "" : r.getItemCode().trim();
            if (code.isEmpty()) {
                // 收下认传承人的代号：没代号对不上，退回并写明为什么
                reject(r, "未填传承人代号，对不上号，本行退回");
                continue;
            }
            BigDecimal hours = r.getHoursNum() == null ? BigDecimal.ZERO : r.getHoursNum();
            BigDecimal total = running.containsKey(code) ? running.get(code).add(hours) : hours;
            if (total.compareTo(YEAR_HOURS_CAP) > 0) {
                // 累过年度满额：超的这一行就地退回，同一报里其余各行照走不误
                reject(r, "累计授徒时数" + total.stripTrailingZeros().toPlainString()
                        + "已过年定满额" + YEAR_HOURS_CAP.stripTrailingZeros().toPlainString()
                        + "，本行退回");
                continue;
            }
            running.put(code, total);
            r.setStatus(Integer.valueOf(STATUS_OK));
            this.ichReportRowMapper.insert(r);
            accepted++;
        }
        return accepted;
    }

    @Override
    public List<TIchReportRow> listErrors(String batchNo) {
        if (batchNo == null || batchNo.trim().isEmpty()) {
            return new ArrayList<TIchReportRow>();
        }
        return this.ichReportRowMapper.selectList(new QueryWrapper<TIchReportRow>()
                .eq("del_flag", 0)
                .eq("batch_no", batchNo.trim())
                .eq("status", STATUS_FAIL)
                .orderByAsc("row_no"));
    }

    @Override
    public IchReceipt receipt(String batchNo) {
        IchReceipt receipt = new IchReceipt();
        receipt.setBatchNo(batchNo);
        if (batchNo == null || batchNo.trim().isEmpty()) {
            receipt.setLabel(IchReceipt.LABEL_EMPTY);
            return receipt;
        }
        // 三数从库中落地行同一回算出：总行=落库+退件，抽任何一行复核都对着同一本账，
        // 屏上临时数一遍不作数
        List<TIchReportRow> rows = this.ichReportRowMapper.selectList(new QueryWrapper<TIchReportRow>()
                .eq("del_flag", 0)
                .eq("batch_no", batchNo.trim())
                .in("status", STATUS_PENDING, STATUS_OK, STATUS_FAIL));
        int ok = 0;
        int fail = 0;
        for (TIchReportRow row : rows) {
            if (row.getStatus() != null && row.getStatus().intValue() == STATUS_OK) {
                ok++;
            } else if (row.getStatus() != null && row.getStatus().intValue() == STATUS_FAIL) {
                fail++;
            }
        }
        receipt.setTotalRows(rows.size());
        receipt.setAcceptedRows(ok);
        receipt.setRejectedRows(fail);
        // "核讫"是给有数的报准备的；全报寻不出能收的一行，落"空报"
        receipt.setLabel(ok > 0 ? IchReceipt.LABEL_VERIFIED : IchReceipt.LABEL_EMPTY);
        return receipt;
    }

    @Override
    public List<TIchReportRow> listUnreconciled(String batchNo) {
        if (batchNo == null || batchNo.trim().isEmpty()) {
            return new ArrayList<TIchReportRow>();
        }
        // 差行=现行批面上去向未定（非收下非退回）的行：回执三数接不上，就差在这几行
        return this.ichReportRowMapper.selectList(new QueryWrapper<TIchReportRow>()
                .eq("del_flag", 0)
                .eq("batch_no", batchNo.trim())
                .and(w -> w.isNull("status").or()
                        .notIn("status", STATUS_OK, STATUS_FAIL, STATUS_ARCHIVED))
                .orderByAsc("row_no"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int withdraw(String batchNo) {
        if (batchNo == null || batchNo.trim().isEmpty()) {
            return 0;
        }
        // 撤报那一步手续：整批注销（留痕不真删），批号腾出后才可重新进门
        TIchReportRow update = new TIchReportRow();
        update.setDelFlag(Integer.valueOf(1));
        return this.ichReportRowMapper.update(update, new UpdateWrapper<TIchReportRow>()
                .eq("batch_no", batchNo.trim())
                .eq("del_flag", 0));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int archiveStaleRejected(Date now) {
        if (now == null) {
            return 0;
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(now);
        cal.add(Calendar.YEAR, -ARCHIVE_AFTER_YEARS);
        String cutoff = ts(cal.getTime());
        // 退回满两个报送年度仍无去向的，转到往期那一层去待着
        TIchReportRow update = new TIchReportRow();
        update.setStatus(Integer.valueOf(STATUS_ARCHIVED));
        return this.ichReportRowMapper.update(update, new UpdateWrapper<TIchReportRow>()
                .eq("del_flag", 0)
                .eq("status", STATUS_FAIL)
                .le("create_time", cutoff));
    }

    @Override
    public List<TIchReportRow> listArchived(String batchNo) {
        if (batchNo == null || batchNo.trim().isEmpty()) {
            return new ArrayList<TIchReportRow>();
        }
        return this.ichReportRowMapper.selectList(new QueryWrapper<TIchReportRow>()
                .eq("del_flag", 0)
                .eq("batch_no", batchNo.trim())
                .eq("status", STATUS_ARCHIVED)
                .orderByAsc("row_no"));
    }

    /** 退回一行并写明为什么，随批落库（带着原行次） */
    private void reject(TIchReportRow r, String reason) {
        r.setStatus(Integer.valueOf(STATUS_FAIL));
        r.setRemark(reason);
        this.ichReportRowMapper.insert(r);
    }

    /** 进门批号定死：参数为准；参数空才回看行上原带的号；都没有则立不了批 */
    private static String resolveBatchNo(String batchNo, List<TIchReportRow> rows) {
        if (batchNo != null && !batchNo.trim().isEmpty()) {
            return batchNo.trim();
        }
        for (TIchReportRow r : rows) {
            if (r != null && r.getBatchNo() != null && !r.getBatchNo().trim().isEmpty()) {
                return r.getBatchNo().trim();
            }
        }
        return null;
    }

    /** 现行批面上的落地行数（未收/收下/退回；已转往期的不算，免得绊住后来的新报） */
    private int countCurrentRows(String batchNo) {
        return this.ichReportRowMapper.selectCount(new QueryWrapper<TIchReportRow>()
                .eq("del_flag", 0)
                .eq("batch_no", batchNo)
                .in("status", STATUS_PENDING, STATUS_OK, STATUS_FAIL));
    }

    /**
     * 时段比较统一走**墙钟字符串**（yyyy-MM-dd HH:mm:ss）+ 字符串绑定：
     * JDBC 的 serverTimezone 与本机时区不对称，直接把 java.util.Date 作参数会整体偏移，
     * 与库内既有写法同口径。
     */
    private static String ts(Date d) {
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(d);
    }
}
