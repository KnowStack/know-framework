package com.didiglobal.logi.security.extend;

import com.didiglobal.logi.security.properties.LogiSecurityProper;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component("logiSecurityLoginExtendBeanTool")
public class LoginExtendBeanTool {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private LogiSecurityProper logiSecurityProper;

    private static final String DEFAULT_BEAN_NAME = "logiSecurityDefaultLoginExtendImpl";

    private LoginExtend getCustomLoginExtendImplBean() {
        String customBeanName = logiSecurityProper.getLoginExtendBeanName();
        try {
            return (LoginExtend) applicationContext.getBean(customBeanName);
        } catch (NoSuchBeanDefinitionException e) {
            throw new UnsupportedOperationException("未能找到自定义的LoginExtend实现类的bean，使用默认bean");
        }
    }

    private LoginExtend getDefaultLoginExtendImplBean() {
        return (LoginExtend) applicationContext.getBean(DEFAULT_BEAN_NAME);
    }

    public LoginExtend getLoginExtendImpl() {
        LoginExtend loginExtend;
        try {
            loginExtend = getCustomLoginExtendImplBean();
        } catch (UnsupportedOperationException e) {
            // 如果用户没有自己实现ResourceExtend接口，则用默认的
            loginExtend = getDefaultLoginExtendImplBean();
        }
        return loginExtend;
    }

}