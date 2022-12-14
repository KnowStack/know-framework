package com.didiglobal.knowframework.security.common.entity.project;

import com.didiglobal.knowframework.security.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author cjm
 *
 * 项目信息
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class Project extends BaseEntity {

    /**
     * 项目名
     */
    private String projectName;

    /**
     * 项目编号
     */
    private String projectCode;

    /**
     * 描述
     */
    private String description;

    /**
     * 运行状态（启动 or 停用）
     */
    private Boolean running;

    /**
     * 所属部门id
     */
    private Integer deptId;
}
