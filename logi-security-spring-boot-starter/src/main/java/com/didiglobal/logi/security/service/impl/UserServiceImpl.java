package com.didiglobal.logi.security.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.didiglobal.logi.security.common.PagingData;
import com.didiglobal.logi.security.common.entity.*;
import com.didiglobal.logi.security.common.vo.dept.DeptVo;
import com.didiglobal.logi.security.common.vo.role.AssignDataVo;
import com.didiglobal.logi.security.common.vo.role.RoleVo;
import com.didiglobal.logi.security.common.vo.user.UserQueryVo;
import com.didiglobal.logi.security.common.enums.ResultCode;
import com.didiglobal.logi.security.common.vo.user.UserVo;
import com.didiglobal.logi.security.exception.SecurityException;
import com.didiglobal.logi.security.mapper.*;
import com.didiglobal.logi.security.service.DeptService;
import com.didiglobal.logi.security.service.PermissionService;
import com.didiglobal.logi.security.service.RoleService;
import com.didiglobal.logi.security.util.CopyBeanUtil;
import com.didiglobal.logi.security.service.UserService;

import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;

/**
 * @author cjm
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private UserRoleMapper userRoleMapper;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private RolePermissionMapper rolePermissionMapper;

    @Autowired
    private DeptService deptService;

    @Override
    public PagingData<UserVo> getUserPage(UserQueryVo queryVo) {
        QueryWrapper<User> userWrapper = new QueryWrapper<>();
        // 分页查询
        IPage<User> userPage = new Page<>(queryVo.getPage(), queryVo.getSize());

        // 是否有角色条件
        if (queryVo.getRoleId() != null) {
            Role role = roleMapper.selectById(queryVo.getRoleId());
            if (role == null) {
                // 数据库没该角色名字
                return new PagingData<>(userPage);
            }
            // 根据角色id查找用户idList
            QueryWrapper<UserRole> wrapper = new QueryWrapper<>();
            wrapper.select("user_id").eq("role_id", role.getId());
            List<Object> userIdList = userRoleMapper.selectObjs(wrapper);
            // 只获取拥有该角色的用户信息
            userWrapper.in("id", userIdList);
        }

        List<Integer> deptIdList = deptService.getChildDeptIdListByParentId(queryVo.getDeptId());
        userWrapper
                .in(queryVo.getDeptId() != null, "dept_id", deptIdList)
                .like(queryVo.getUsername() != null, "username", queryVo.getUsername())
                .like(queryVo.getRealName() != null, "real_name", queryVo.getRealName());

        userMapper.selectPage(userPage, userWrapper);
        // 转成vo
        List<UserVo> userVoList = CopyBeanUtil.copyList(userPage.getRecords(), UserVo.class);

        // 获取所有角色，并转换成 roleId-Role对象 形式
        QueryWrapper<Role> roleWrapper = new QueryWrapper<>();
        roleWrapper.select("id", "role_name");
        Map<Integer, Role> roleMap = roleMapper.selectList(roleWrapper)
                .stream().collect(Collectors.toMap(Role::getId, role -> role));

        QueryWrapper<UserRole> userRoleWrapper = new QueryWrapper<>();
        for (int i = 0; i < userVoList.size(); i++) {
            UserVo userVo = userVoList.get(i);
            // 查询用户关联的角色
            userRoleWrapper.select("role_id").eq("user_id", userVo.getId());
            List<Object> roleIdList = userRoleMapper.selectObjs(userRoleWrapper);

            List<RoleVo> roleVoList = new ArrayList<>();
            for (Object roleId : roleIdList) {
                Role role = roleMap.get((Integer) roleId);
                roleVoList.add(CopyBeanUtil.copy(role, RoleVo.class));
            }
            // 设置角色信息
            userVo.setRoleVoList(roleVoList);
            userRoleWrapper.clear();

            // 查找用户所在部门信息
            userVo.setDeptInfo(deptService.spliceDeptInfo(userPage.getRecords().get(i).getDeptId()));
            userVo.setUpdateTime(userPage.getRecords().get(i).getUpdateTime().getTime());
            // 隐私信息处理
            privacyProcessing(userVo);
        }
        return new PagingData<>(userVoList, userPage);
    }

    @Override
    public UserVo getDetailById(Integer userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new SecurityException(ResultCode.USER_ACCOUNT_NOT_EXIST);
        }

        // 根据用户id获取角色idList
        QueryWrapper<UserRole> userRoleWrapper = new QueryWrapper<>();
        userRoleWrapper.select("role_id").eq("user_id", userId);
        List<Object> roleIdList = userRoleMapper.selectObjs(userRoleWrapper);

        Set<Integer> permissionHasSet = new HashSet<>();
        QueryWrapper<RolePermission> rolePermissionWrapper = new QueryWrapper<>();

        List<RoleVo> roleVoList = new ArrayList<>();
        QueryWrapper<Role> roleWrapper = new QueryWrapper<>();
        for (Object roleId : roleIdList) {
            // 获取角色信息
            roleWrapper.clear();
            roleWrapper.select("id", "role_name").eq("id", roleId);
            Role role = roleMapper.selectOne(roleWrapper);
            roleVoList.add(CopyBeanUtil.copy(role, RoleVo.class));

            // 查询该角色拥有的权限idList
            rolePermissionWrapper.select("permission_id").eq("role_id", roleId);
            List<Object> permissionIdList = rolePermissionMapper.selectObjs(rolePermissionWrapper);

            // 添加到用户拥有的所有权限集合
            for (Object permissionId : permissionIdList) {
                permissionHasSet.add((Integer) permissionId);
            }

            rolePermissionWrapper.clear();
        }
        UserVo userVo = CopyBeanUtil.copy(user, UserVo.class);
        // 设置角色信息
        userVo.setRoleVoList(roleVoList);
        // 构建权限树
        userVo.setPermissionVo(permissionService.buildPermissionTree(permissionHasSet));
        // 查找用户所在部门信息
        userVo.setDeptInfo(deptService.spliceDeptInfo(user.getDeptId()));
        userVo.setUpdateTime(user.getUpdateTime().getTime());
        return userVo;
    }

    @Override
    public List<UserVo> getListByDeptId(Integer deptId) {
        QueryWrapper<User> userWrapper = new QueryWrapper<>();
        List<Integer> deptIdList = deptService.getChildDeptIdListByParentId(deptId);
        // 根据部门id查找用户，该部门的子部门的用户都属于该部门
        userWrapper.select("id", "username", "real_name").in("dept_id", deptIdList);
        List<User> userList = userMapper.selectList(userWrapper);
        return CopyBeanUtil.copyList(userList, UserVo.class);
    }

    @Override
    public List<AssignDataVo> getAssignDataByUserId(Integer userId, String roleName) {
        if(userId == null) {
            throw new SecurityException(ResultCode.USER_ID_CANNOT_BE_NULL);
        }
        // 查询所有的角色，并根据角色添加时间排序（倒序）
        QueryWrapper<Role> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("id", "role_name")
                .like(!StringUtils.isEmpty(roleName), "role_name", roleName);
        List<Role> roleList = roleMapper.selectList(queryWrapper);

        // 先获取该用户已拥有的角色，并转为set
        QueryWrapper<UserRole> userRoleWrapper = new QueryWrapper<>();
        userRoleWrapper.select("role_id").eq("user_id", userId);
        List<Object> roleIdList = userRoleMapper.selectObjs(userRoleWrapper);
        Set<Object> hasRoleIdSet = new HashSet<>(roleIdList);

        // 封装List<AssignDataVo>
        List<AssignDataVo> list = new ArrayList<>();
        for(Role role : roleList) {
            AssignDataVo data = new AssignDataVo();
            data.setName(role.getRoleName());
            data.setId(role.getId());
            data.setHas(hasRoleIdSet.contains(role.getId()));
            list.add(data);
        }
        return list;
    }

    @Override
    public List<UserVo> getListByRoleId(Integer roleId) {
        if(roleId == null) {
            throw new SecurityException(ResultCode.ROLE_ID_CANNOT_BE_NULL);
        }
        // 先获取拥有该角色的用户id
        QueryWrapper<UserRole> userRoleWrapper = new QueryWrapper<>();
        userRoleWrapper.eq("role_id", roleId);
        List<Object> userIdList = userRoleMapper.selectObjs(userRoleWrapper);

        // 封装List<User>
        QueryWrapper<User> userWrapper = new QueryWrapper<>();
        userWrapper.select("id", "username", "real_name").in("id", userIdList);
        List<User> userList = userMapper.selectList(userWrapper);
        return CopyBeanUtil.copyList(userList, UserVo.class);
    }

    /**
     * 隐私处理
     *
     * @param userVo 返回给页面的用户信息
     */
    private void privacyProcessing(UserVo userVo) {
        String phone = userVo.getPhone();
        userVo.setPhone(phone.replaceAll("(\\d{3})\\d{4}(\\d{4})","$1****$2"));
    }
}