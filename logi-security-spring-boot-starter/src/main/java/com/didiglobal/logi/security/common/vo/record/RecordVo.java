package com.didiglobal.logi.security.common.vo.record;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.sql.Timestamp;

/**
 * @author cjm
 */
@Data
@ApiModel(description = "操作日志信息")
public class RecordVo {

    @ApiModelProperty(value = "操作日志id", dataType = "Integer", required = false)
    private Integer id;

    /**
     * 操作者ip
     */
    @ApiModelProperty(value = "操作者ip", dataType = "String", required = false)
    private String recordIp;

    /**
     * 操作者用户账号
     */
    @ApiModelProperty(value = "操作者用户账号", dataType = "String", required = false)
    private String recordUsername;

    /**
     * 操作日志页面
     */
    @ApiModelProperty(value = "操作日志页面", dataType = "String", required = false)
    private String recordPage;

    /**
     * 操作类型
     */
    @ApiModelProperty(value = "操作类型", dataType = "String", required = false)
    private String recordType;

    /**
     * 操作对象
     */
    @ApiModelProperty(value = "操作对象", dataType = "String", required = false)
    private String target;

    /**
     * 对象分类
     */
    @ApiModelProperty(value = "对象分类", dataType = "String", required = false)
    private String targetType;

    /**
     * 详情
     */
    @ApiModelProperty(value = "操作日志详情", dataType = "String", required = false)
    private String detail;

    @ApiModelProperty(value = "记录时间（时间戳ms）", dataType = "Long", required = false)
    private Long createTime;
}
