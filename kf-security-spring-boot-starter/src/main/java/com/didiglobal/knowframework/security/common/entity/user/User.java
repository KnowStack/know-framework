package com.didiglobal.knowframework.security.common.entity.user;

import com.didiglobal.knowframework.security.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author cjm
 *
 * 用户信息
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class User extends BaseEntity {

    /**
     * 用户账号
     */
    private String userName;

    /**
     * 用户密码
     */
    private String pw;

    /**
     * 密码盐
     */
    private String salt;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 手机
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 所属部门id
     */
    private Integer deptId;
}
