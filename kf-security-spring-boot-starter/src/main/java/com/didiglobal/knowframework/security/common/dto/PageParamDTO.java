package com.didiglobal.knowframework.security.common.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 分页信息
 *
 * @author cjm
 */
@Data
@ApiModel(description = "分页查找条件信息")
public class PageParamDTO {

    @ApiModelProperty(value = "当前页", dataType = "Integer", required = true)
    private int page = 1;
    
    @ApiModelProperty(value = "每页大小", dataType = "Integer", required = true)
    private int size = 10;
}
