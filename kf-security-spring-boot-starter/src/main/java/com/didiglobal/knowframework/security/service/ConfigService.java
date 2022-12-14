package com.didiglobal.knowframework.security.service;

import com.didiglobal.knowframework.security.common.PagingData;
import com.didiglobal.knowframework.security.common.Result;
import com.didiglobal.knowframework.security.common.dto.config.ConfigDTO;
import com.didiglobal.knowframework.security.common.dto.config.ConfigQueryDTO;
import com.didiglobal.knowframework.security.common.vo.config.ConfigVO;

import java.util.List;

public interface ConfigService {
    /**
     * 新增配置
     * @param configInfoDTO 配置信息
     * @param user 操作人
     * @return 成功 true
     */
    Result<Integer> addConfig(ConfigDTO configInfoDTO, String user);

    /**
     * 新增配置
     */
    Result<Integer> addConfig(String  valueGroup, String  valueName, String  value, String user);

    /**
     * 删除配置
     * @param configId 配置id
     * @param user 操作人
     * @return 成功 true  失败 false
     */
    Result<Void> delConfig(Integer configId, String user);

    /**
     * 编辑配置
     * @param configInfoDTO 配置内容
     * @param user 操作人
     * @return 成功 true  失败 false
     *
     */
    Result<Void> editConfig(ConfigDTO configInfoDTO, String user);

    /**
     * 使能配置
     * @param configId 配置id
     * @param status 状态
     * @param user 操作人
     * @return 成功 true  失败 false
     *
     */
    Result<Void> switchConfig(Integer configId, Integer status, String user);

    /**
     * 分页获取用户信息
     * @param queryDTO 条件信息
     * @return 用户信息list
     */
    PagingData<ConfigVO> pagingConfig(ConfigQueryDTO queryDTO);

    /**
     * 根据查询条件返回ConfigVOVO列表
     * @param param 查询条件
     * @return 配置列表
     *
     * 如果不存在,返回空列表
     */
    List<ConfigVO> queryByCondt(ConfigDTO param);

    /**
     * 获取本appName下的所有组名
     * @return
     */
    List<String> listGroups();

    /**
     * 根据组名，获取该组名下所有的配置
     * @param group
     * @return
     */
    List<ConfigVO> listConfigByGroup(String group);

    /**
     * 查询指定配置
     * @param configId 配置id
     * @return 配置信息  不存在返回null
     */
    ConfigVO getConfigById(Integer configId);

    /**
     * 获取String类型配置
     * @param group 配置组
     * @param name 配置项
     * @param defaultValue 默认值
     * @return 如果查到转换后返回,转换报错或者没有查到则返回默认值
     */
    String stringSetting(String group, String name, String defaultValue);

    /**
     * 获取bool类型配置
     * @param group 配置组
     * @param name 配置项
     * @param defaultValue 默认值
     * @return 如果查到转换后返回,转换报错或者没有查到则返回默认值
     */
    Boolean booleanSetting(String group, String name, Boolean defaultValue);

    /**
     * 获取int类型配置
     * @param group 配置组
     * @param name 配置项
     * @param defaultValue 默认值
     * @return 如果查到转换后返回,转换报错或者没有查到则返回默认值
     */
    Integer intSetting(String group, String name, Integer defaultValue);

    /**
     * 获取long类型配置
     * @param group 配置组
     * @param name 配置项
     * @param defaultValue 默认值
     * @return 如果查到转换后返回,转换报错或者没有查到则返回默认值
     */
    Long longSetting(String group, String name, Long defaultValue);

    /**
     * 获取double类型配置
     * @param group 配置组
     * @param name 配置项
     * @param defaultValue 默认值
     * @return 如果查到转换后返回,转换报错或者没有查到则返回默认值
     */
    Double doubleSetting(String group, String name, Double defaultValue);

    <T> T objectSetting(String group, String name, T defaultValue, Class<T> clazz);
}
