package com.fc.v2.model.auto;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.Date;

/**
 * 履职情况报送批与回执对象 t_ich_report_batch
 *
 * @author fuce
 * @date 2026-09-30
 */
@TableName("t_ich_report_batch")
@ApiModel(value = "TIchReportBatch", description = "履职情况报送批与回执")
public class TIchReportBatch implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 报送批号（进门定死，一报一号；撤报后同一号可再进门另立一批） */
    @TableField("batch_no")
    @ApiModelProperty(value = "报送批号")
    private String batchNo;

    /** 报送年度（归档算满两个报送年度用） */
    @TableField("report_year")
    @ApiModelProperty(value = "报送年度")
    private Integer reportYear;

    /** 回执·本批总行数 */
    @TableField("total_num")
    @ApiModelProperty(value = "回执·本批总行数")
    private Integer totalNum;

    /** 回执·落库数（收下） */
    @TableField("accept_num")
    @ApiModelProperty(value = "回执·落库数（收下）")
    private Integer acceptNum;

    /** 回执·退件数（退回）；落库数+退件数须抵得上总行数 */
    @TableField("reject_num")
    @ApiModelProperty(value = "回执·退件数（退回）")
    private Integer rejectNum;

    /** 回执字样 空报/核讫（核讫只给有数的报，一行没有落空报） */
    @TableField("receipt")
    @ApiModelProperty(value = "回执字样 空报/核讫")
    private String receipt;

    /** 批情形 0现行落定 1已撤报 */
    @TableField("status")
    @ApiModelProperty(value = "批情形 0现行落定 1已撤报")
    private Integer status;

    /** 删除标记 0正常 1删除 */
    @TableField("del_flag")
    @ApiModelProperty(value = "删除标记 0正常 1删除")
    private Integer delFlag;

    /** 创建者 */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    @ApiModelProperty(value = "创建者")
    private String createBy;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    /** 更新者 */
    @TableField(value = "update_by", fill = FieldFill.UPDATE)
    @ApiModelProperty(value = "更新者")
    private String updateBy;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "更新时间")
    private Date updateTime;

    /** 备注 */
    @TableField("remark")
    @ApiModelProperty(value = "备注")
    private String remark;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(Integer delFlag) {
        this.delFlag = delFlag;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
