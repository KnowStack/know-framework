package com.didiglobal.knowframework.security.common.po;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author cjm
 *
 * 用户信息
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName(value = "kf_security_user")
public class UserPO extends BasePO {

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
