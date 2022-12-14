package com.didiglobal.knowframework.security.extend;

import com.didiglobal.knowframework.security.common.Result;
import com.didiglobal.knowframework.security.common.dto.account.AccountLoginDTO;
import com.didiglobal.knowframework.security.common.vo.user.UserBriefVO;
import com.didiglobal.knowframework.security.exception.KfSecurityException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * 登陆扩展接口
 */
public interface LoginExtend {

    /**
     * 验证登录信息（验证前密码先用Base64解码再用RSA解密）
     * 登录前会检查账户激活状态
     * @param loginDTO 登陆信息
     * @param request 请求信息
     * @return token
     * @throws KfSecurityException 登录错误
     */
    UserBriefVO verifyLogin(AccountLoginDTO loginDTO, HttpServletRequest request, HttpServletResponse response) throws KfSecurityException;

    /**
     * 登出接口
     * @param request
     * @param response
     * @return
     */
    Result<Boolean> logout(HttpServletRequest request, HttpServletResponse response);

    /**
     * 检查登陆
     */
    boolean interceptorCheck(HttpServletRequest request, HttpServletResponse response,
                             String requestMappingValue,
                             List<String> whiteMappingValues) throws IOException;

}