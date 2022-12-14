package com.didiglobal.knowframework.security.common.vo.dept;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Tolerate;

import java.util.List;

/**
 * @author cjm
 */
@Data
@Builder
@ApiModel(description = "部门树信息")
public class DeptTreeVO {

    @ApiModelProperty(value = "部门id", dataType = "Integer", required = false)
    private Integer id;

    @ApiModelProperty(value = "部门名", dataType = "String", required = false)
    private String deptName;

    @ApiModelProperty(value = "描述", dataType = "String", required = false)
    private String description;

    @ApiModelProperty(value = "父部门id（根部门parentId为0）", dataType = "Integer", required = false)
    private Integer parentId;

    @ApiModelProperty(value = "是否是叶子部门", dataType = "Boolean", required = false)
    private Boolean leaf;
    
    @ApiModelProperty(value = "孩子部门", dataType = "List<DeptTreeVO>", required = false)
    private List<DeptTreeVO> childList;

    @Tolerate
    public DeptTreeVO() {
        // 空构造函数需要加上@Tolerate注解，这样就不会和@Builder冲突了
    }
}
