package com.didiglobal.knowframework.security.dao.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.didiglobal.knowframework.security.common.constant.FieldConstant;
import com.didiglobal.knowframework.security.common.po.UserPO;
import com.didiglobal.knowframework.security.dao.UserDao;
import com.didiglobal.knowframework.security.util.CopyBeanUtil;
import com.didiglobal.knowframework.security.util.PWEncryptUtil;
import com.didiglobal.knowframework.security.common.dto.user.UserBriefQueryDTO;
import com.didiglobal.knowframework.security.common.dto.user.UserQueryDTO;
import com.didiglobal.knowframework.security.common.entity.user.User;
import com.didiglobal.knowframework.security.common.entity.user.UserBrief;
import com.didiglobal.knowframework.security.dao.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author cjm
 */
@Component
public class UserDaoImpl extends BaseDaoImpl<UserPO> implements UserDao {

    @Autowired
    private UserMapper userMapper;

    @Override
    public int addUser(UserPO userPO) throws Exception {
        userPO.setPw(PWEncryptUtil.encode(userPO.getPw()));
        userPO.setAppName( kfSecurityProper.getAppName());
        return userMapper.insert(userPO);
    }

    @Override
    public int editUser(UserPO userPO) throws Exception {
        userPO.setPw(PWEncryptUtil.encode(userPO.getPw()));
        return userMapper.updateById(userPO);
    }

    @Override
    public IPage<User> selectPageByUserIdList(UserQueryDTO queryDTO,
                                              List<Integer> userIdList) {
        IPage<UserPO> page = new Page<>(queryDTO.getPage(), queryDTO.getSize());
        QueryWrapper<UserPO> queryWrapper = getQueryWrapperWithAppName();
        queryWrapper
                .eq( queryDTO.getId() != null, FieldConstant.ID, queryDTO.getId())
                .like(queryDTO.getUserName() != null, FieldConstant.USER_NAME, queryDTO.getUserName())
                .like(queryDTO.getRealName() != null, FieldConstant.REAL_NAME, queryDTO.getRealName())
                .in(!CollectionUtils.isEmpty(userIdList), FieldConstant.ID, userIdList)
                .orderByDesc(FieldConstant.CREATE_TIME);
        userMapper.selectPage(page, queryWrapper);

        page.setTotal(userMapper.selectCount(queryWrapper));
        page.setRecords(decodePW(page.getRecords()));
        return CopyBeanUtil.copyPage(page, User.class);
    }

    @Override
    public IPage<UserBrief> selectBriefPageByDeptIdList(UserBriefQueryDTO queryDTO, List<Integer> deptIdList) {
        IPage<UserPO> page = new Page<>(queryDTO.getPage(), queryDTO.getSize());
        if(deptIdList != null && deptIdList.isEmpty()) {
            return CopyBeanUtil.copyPage(page, UserBrief.class);
        }
        QueryWrapper<UserPO> queryWrapper = wrapBriefQuery();
        queryWrapper
                .like(!StringUtils.isEmpty(queryDTO.getUserName()), FieldConstant.USER_NAME, queryDTO.getUserName())
                .like(!StringUtils.isEmpty(queryDTO.getRealName()), FieldConstant.REAL_NAME, queryDTO.getRealName())
                .in(deptIdList != null, FieldConstant.DEPT_ID, deptIdList);
        userMapper.selectPage(page, queryWrapper);

        page.setRecords(decodePW(page.getRecords()));
        return CopyBeanUtil.copyPage(page, UserBrief.class);
    }

    @Override
    public User selectByUserId(Integer userId) {
        if(userId == null) {return null;}

        QueryWrapper<UserPO> queryWrapper = getQueryWrapperWithAppName();
        queryWrapper.eq(FieldConstant.ID, userId);
        return CopyBeanUtil.copy(decodePW(userMapper.selectOne(queryWrapper)), User.class);
    }

    @Override
    public User selectByUserMail(String userName) {
        QueryWrapper<UserPO> queryWrapper = getQueryWrapperWithAppName();
        queryWrapper.eq(FieldConstant.EMAIL, userName);
        return CopyBeanUtil.copy(decodePW(userMapper.selectOne(queryWrapper)), User.class);
    }

    @Override
    public User selectByUserPhone(String userPhone) {
        QueryWrapper<UserPO> queryWrapper = getQueryWrapperWithAppName();
        queryWrapper.eq(FieldConstant.PHONE, userPhone);
        return CopyBeanUtil.copy(decodePW(userMapper.selectOne(queryWrapper)), User.class);
    }

    @Override
    public boolean deleteByUserId(Integer userId) {
        if(userId == null) {return false;}

        QueryWrapper<UserPO> queryWrapper = getQueryWrapperWithAppName();
        queryWrapper.eq(FieldConstant.ID, userId);

        return userMapper.delete(queryWrapper) > 0;
    }

    @Override
    public List<UserBrief> selectBriefListByUserIdList(List<Integer> userIdList) {
        if(CollectionUtils.isEmpty(userIdList)) {
            return new ArrayList<>();
        }
        QueryWrapper<UserPO> queryWrapper = wrapBriefQuery();
        queryWrapper.in("id", userIdList);
        return CopyBeanUtil.copyList(decodePW(userMapper.selectList(queryWrapper)), UserBrief.class);
    }

    @Override
    public List<UserBrief> selectBriefListByNameAndDescOrderByCreateTime(String name) {
        QueryWrapper<UserPO> queryWrapper = wrapBriefQuery();
        queryWrapper
                .like(!StringUtils.isEmpty(name), FieldConstant.USER_NAME, name)
                .or()
                .like(!StringUtils.isEmpty(name), FieldConstant.REAL_NAME, name)
                .orderByDesc(FieldConstant.CREATE_TIME);
        return CopyBeanUtil.copyList(decodePW(userMapper.selectList(queryWrapper)), UserBrief.class);
    }

    @Override
    public List<UserBrief> selectBriefListByDeptIdList(List<Integer> deptIdList) {
        if(deptIdList != null && deptIdList.isEmpty()) {
            return new ArrayList<>();
        }
        QueryWrapper<UserPO> queryWrapper = wrapBriefQuery();
        queryWrapper.in(deptIdList != null, FieldConstant.DEPT_ID, deptIdList);
        return CopyBeanUtil.copyList(decodePW(userMapper.selectList(queryWrapper)), UserBrief.class);
    }

    @Override
    public List<UserBrief> selectBriefListOrderByCreateTime(boolean isAsc) {
        QueryWrapper<UserPO> queryWrapper = wrapBriefQuery();
        if(isAsc) {
            queryWrapper.orderByAsc(FieldConstant.CREATE_TIME);
        } else {
            queryWrapper.orderByDesc(FieldConstant.CREATE_TIME);
        }
        userMapper.selectList(queryWrapper);
        return CopyBeanUtil.copyList(decodePW(userMapper.selectList(queryWrapper)), UserBrief.class);
    }

    @Override
    public List<UserBrief> selectAllBriefList() {
        QueryWrapper<UserPO> queryWrapper = wrapBriefQuery();
        return CopyBeanUtil.copyList(decodePW(userMapper.selectList(queryWrapper)), UserBrief.class);
    }

    @Override
    public List<Integer> selectUserIdListByUsernameOrRealName(String name) {
        QueryWrapper<UserPO> queryWrapper = getQueryWrapperWithAppName();
        queryWrapper.select(FieldConstant.ID)
                .like(!StringUtils.isEmpty(name), FieldConstant.USER_NAME, name)
                .or()
                .like(!StringUtils.isEmpty(name), FieldConstant.REAL_NAME, name);
        List<Object> userIdList = userMapper.selectObjs(queryWrapper);
        return userIdList.stream().map(Integer.class::cast).collect(Collectors.toList());
    }

    @Override
    public User selectByUsername(String username) {
        if(StringUtils.isEmpty(username)) {
            return null;
        }
        QueryWrapper<UserPO> queryWrapper = getQueryWrapperWithAppName();
        queryWrapper.eq(FieldConstant.USER_NAME, username);
        UserPO userPO = decodePW(userMapper.selectOne(queryWrapper));
        return CopyBeanUtil.copy(userPO, User.class);
    }

    private QueryWrapper<UserPO> wrapBriefQuery() {
        QueryWrapper<UserPO> queryWrapper = getQueryWrapperWithAppName();
        queryWrapper.select(FieldConstant.ID, FieldConstant.USER_NAME, FieldConstant.REAL_NAME, FieldConstant.DEPT_ID);
        return queryWrapper;
    }

    private List<UserPO> decodePW(List<UserPO> userPOS){
        if(CollectionUtils.isEmpty(userPOS)){return userPOS;}

        return userPOS.stream().map(u -> decodePW(u)).collect(Collectors.toList());
    }

    private UserPO decodePW(UserPO userPO){
        if(null != userPO && !StringUtils.isEmpty(userPO.getPw())){
            try {
                userPO.setPw(PWEncryptUtil.decode(userPO.getPw()));
            } catch (Exception e) {
            }
        }

        return userPO;
    }
}
