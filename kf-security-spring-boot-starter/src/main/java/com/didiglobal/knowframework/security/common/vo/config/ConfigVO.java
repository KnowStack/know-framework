package com.didiglobal.knowframework.security.common.vo.config;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "配置信息")
public class ConfigVO {
    @ApiModelProperty("配置ID")
    private Integer id;

    @ApiModelProperty("配置组/模块")
    private String  valueGroup;

    @ApiModelProperty("配置名称")
    private String  valueName;

    @ApiModelProperty("值")
    private String  value;

    @ApiModelProperty("状态(1 正常；2 禁用)")
    private Integer status;

    @ApiModelProperty("备注")
    private String  memo;

    @ApiModelProperty("创建时间")
    private Date createTime;

    @ApiModelProperty("修改时间")
    private Date updateTime;

    @ApiModelProperty("操作者")
    private String operator;
}
