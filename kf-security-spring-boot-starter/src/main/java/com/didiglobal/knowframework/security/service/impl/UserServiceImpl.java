package com.didiglobal.knowframework.security.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.didiglobal.knowframework.security.common.PagingData;
import com.didiglobal.knowframework.security.common.Result;
import com.didiglobal.knowframework.security.common.dto.user.UserBriefQueryDTO;
import com.didiglobal.knowframework.security.common.dto.user.UserDTO;
import com.didiglobal.knowframework.security.common.dto.user.UserQueryDTO;
import com.didiglobal.knowframework.security.common.enums.ResultCode;
import com.didiglobal.knowframework.security.common.po.UserPO;
import com.didiglobal.knowframework.security.common.po.UserProjectPO;
import com.didiglobal.knowframework.security.common.vo.project.ProjectBriefVO;
import com.didiglobal.knowframework.security.common.vo.role.AssignInfoVO;
import com.didiglobal.knowframework.security.common.vo.role.RoleBriefVO;
import com.didiglobal.knowframework.security.common.vo.user.UserBasicVO;
import com.didiglobal.knowframework.security.common.vo.user.UserBriefVO;
import com.didiglobal.knowframework.security.dao.ProjectDao;
import com.didiglobal.knowframework.security.dao.UserDao;
import com.didiglobal.knowframework.security.dao.UserProjectDao;
import com.didiglobal.knowframework.security.exception.KfSecurityException;
import com.didiglobal.knowframework.security.util.CopyBeanUtil;
import com.didiglobal.knowframework.security.common.entity.user.User;
import com.didiglobal.knowframework.security.common.entity.user.UserBrief;
import com.didiglobal.knowframework.security.common.enums.user.UserCheckType;
import com.didiglobal.knowframework.security.common.vo.user.UserVO;
import com.didiglobal.knowframework.security.service.DeptService;
import com.didiglobal.knowframework.security.service.PermissionService;
import com.didiglobal.knowframework.security.service.RolePermissionService;
import com.didiglobal.knowframework.security.service.RoleService;
import com.didiglobal.knowframework.security.service.UserRoleService;
import com.didiglobal.knowframework.security.service.UserService;
import com.google.common.collect.Lists;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * @author cjm
 */
@Service("kfSecurityUserServiceImpl")
public class UserServiceImpl implements UserService {

    private static final Pattern P_USER_NAME    = Pattern.compile("^[0-9a-zA-Z_]{5,50}$");
    private static final Pattern P_USER_PHONE   = Pattern.compile("^(13[0-9]|14[01456879]|15[0-35-9]|16[2567]|17[0-8]|18[0-9]|19[0-35-9])\\d{8}$");
    private static final Pattern P_USER_MAIL    = Pattern.compile("^\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*$");

    @Autowired
    private UserDao userDao;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private RolePermissionService rolePermissionService;

    @Autowired
    private DeptService deptService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private UserRoleService userRoleService;
    @Autowired
    private UserProjectDao userProjectDao;
    
    @Autowired
    private ProjectDao projectDao;
    @Override
    public Result<Void> check(Integer type, String value) {
        if(UserCheckType.USER_NAME.getCode() == type){
            return userNameCheck(value);
        }else if(UserCheckType.USER_PHONE.getCode() == type){
            return userPhoneCheck(value);
        }else if(UserCheckType.USER_MAIL.getCode() == type){
            return userMailCheck(value);
        }

        return Result.fail("???????????????????????????");
    }

    @Override
    public PagingData<UserVO> getUserPage(UserQueryDTO queryDTO) {
        List<Integer> userIdList = null;
        // ???????????????id??????
        if(queryDTO.getRoleId() != null) {
            // ????????????????????????IdList
            userIdList = userRoleService.getUserIdListByRoleId(queryDTO.getRoleId());
            if(CollectionUtils.isEmpty(userIdList)){
                return new PagingData<>(new Page<>(queryDTO.getPage(), queryDTO.getSize()));
            }
        }

        IPage<User> pageInfo = userDao.selectPageByUserIdList(queryDTO, userIdList);
        List<UserVO> userVOList = Lists.newArrayList();
        //???????????????????????????
        final List<Integer> userIds = pageInfo.getRecords().stream().map(User::getId)
                .collect(Collectors.toList());
        //??????????????????????????????userId
        final List<UserProjectPO> userProjectList = userProjectDao.selectProjectListByUserIdList(userIds);

        final List<Integer> projectIds = userProjectList.stream().map(UserProjectPO::getProjectId)
                .distinct().collect(Collectors.toList());
        final List<ProjectBriefVO> projects = CopyBeanUtil.copyList(projectDao.selectProjectBriefByProjectIds(projectIds),
                ProjectBriefVO.class);
        final Map<Integer, ProjectBriefVO> projectId2ProjectMap = projects.stream()
                .collect(Collectors.toMap(ProjectBriefVO::getId, i -> i));
        //?????????userid-???project
        final Map<Integer, Set<ProjectBriefVO>> userId2ProjectListMap = userProjectList.stream()
                .filter(i->projectId2ProjectMap.containsKey(i.getProjectId()))
                .collect(Collectors.groupingBy(UserProjectPO::getUserId
                        , Collectors.mapping(i -> projectId2ProjectMap.get(i.getProjectId()),
                                Collectors.toSet())
                ));
        //???????????????????????????
        final Map<Integer, List<RoleBriefVO>> userId2RoleListMap = roleService.getRoleBriefListByUserIds(
                userIds);

        List<User> userList = pageInfo.getRecords();

        // ????????????????????????
        for (User user : userList) {
            UserVO userVo = CopyBeanUtil.copy(user, UserVO.class);
            // ??????????????????
            userVo.setRoleList(userId2RoleListMap.getOrDefault(user.getId(),Collections.emptyList()));
            userVo.setUpdateTime(user.getUpdateTime());
            userVo.setCreateTime(user.getCreateTime());
            // ??????????????????
            privacyProcessing(userVo);
            userVOList.add(userVo);
            //??????????????????
            userVo.setProjectList(Lists.newArrayList(userId2ProjectListMap.getOrDefault(user.getId(),Collections.emptySet())));
        }
        return new PagingData<>(userVOList, pageInfo);
    }
    @Override
    public PagingData<UserBriefVO> getUserBriefPage(UserBriefQueryDTO queryDTO) {
        // ?????????????????????idList
        List<Integer> deptIdList = deptService.
                getDeptIdListByParentIdAndDeptName(queryDTO.getDeptId(), queryDTO.getDeptName());
        // ????????????
        IPage<UserBrief> pageInfo = userDao.selectBriefPageByDeptIdList(queryDTO, deptIdList);
        List<UserBriefVO> userBriefVOList = CopyBeanUtil.copyList(pageInfo.getRecords(), UserBriefVO.class);
        return new PagingData<>(userBriefVOList, pageInfo);
    }

    @Override
    public UserVO getUserDetailByUserId(Integer userId) throws KfSecurityException {
        User user = userDao.selectByUserId(userId);
        if (user == null) {
            throw new KfSecurityException(ResultCode.USER_NOT_EXISTS);
        }
        UserVO userVo = CopyBeanUtil.copy(user, UserVO.class);

        // ????????????id????????????List
        List<RoleBriefVO> roleBriefVOList = roleService.getRoleBriefListByUserId(userId);
        // ??????????????????
        userVo.setRoleList(roleBriefVOList);

        List<Integer> roleIdList = roleBriefVOList.stream().map(RoleBriefVO::getId).collect(Collectors.toList());
        // ????????????idList????????????idList
        List<Integer> hasPermissionIdList = rolePermissionService.getPermissionIdListByRoleIdList(roleIdList);
        // ???????????????
        userVo.setPermissionTreeVO(permissionService.buildPermissionTreeWithHas(hasPermissionIdList));
        //??????????????????
        final List<ProjectBriefVO> projectBriefVOS = userProjectDao.selectProjectIdListByUserIdList(
                Collections.singletonList(userVo.getId()))
            .stream()
            .map(projectDao::selectByProjectId)
            .map(project -> CopyBeanUtil.copy(project, ProjectBriefVO.class))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        userVo.setProjectList(projectBriefVOS);
        userVo.setUpdateTime(user.getUpdateTime());
        userVo.setCreateTime(user.getCreateTime());
        return userVo;
    }
    
    /**
     * ????????????ID???????????????????????????????????????
     *
     * @param userIds ???????????? userId ?????????
     * @return UserBasicVO??????
     */
    @Override
    public List<UserBasicVO> getUserBasicListByUserIdList(List<Integer> userIds) {
        return CopyBeanUtil.copyList(userDao.selectBriefListByUserIdList(userIds),UserBasicVO.class);
    }
    
    @Override
    public Result<List<UserVO>> getUserDetailByUserIds(List<Integer> ids) {
        if (!CollectionUtils.isEmpty(ids)) {
            return Result.buildSucc(Lists.newArrayList());
        }
        final List<UserVO> userVOS = ids.stream()
            .distinct()
            .map(userDao::selectByUserId)
            .filter(Objects::nonNull)
            .map(user -> {
                final UserVO userVO = CopyBeanUtil.copy(user, UserVO.class);
                userVO.setUpdateTime(user.getUpdateTime());
                userVO.setCreateTime(user.getCreateTime());
                return userVO;
            })
            .collect(Collectors.toList());
        //??????????????????????????????userId
        final List<UserProjectPO> userProjectList = userProjectDao.selectProjectListByUserIdList(ids);

        final List<Integer> projectIds = userProjectList.stream().map(UserProjectPO::getProjectId)
                .distinct().collect(Collectors.toList());
        final List<ProjectBriefVO> projects = CopyBeanUtil.copyList(projectDao.selectProjectBriefByProjectIds(projectIds),
                ProjectBriefVO.class);
        final Map<Integer, ProjectBriefVO> projectId2ProjectMap = projects.stream()
                .collect(Collectors.toMap(ProjectBriefVO::getId, i -> i));
        //?????????userid-???project
        final Map<Integer, Set<ProjectBriefVO>> userId2ProjectListMap = userProjectList.stream()
                .collect(Collectors.groupingBy(UserProjectPO::getUserId
                        , Collectors.mapping(i -> projectId2ProjectMap.get(i.getProjectId()),
                                Collectors.toSet())));
        for (UserVO userVO : userVOS) {
            // ????????????id????????????List
            List<RoleBriefVO> roleBriefVOList = roleService.getRoleBriefListByUserId(
                userVO.getId());
            // ??????????????????
            userVO.setRoleList(roleBriefVOList);
            
            List<Integer> roleIdList = roleBriefVOList.stream().map(RoleBriefVO::getId)
                .collect(Collectors.toList());
            // ????????????idList????????????idList
            List<Integer> hasPermissionIdList = rolePermissionService.getPermissionIdListByRoleIdList(
                roleIdList);
            // ???????????????
            userVO.setPermissionTreeVO(
                permissionService.buildPermissionTreeWithHas(hasPermissionIdList));
            //??????????????????
            userVO.setProjectList(Lists.newArrayList(userId2ProjectListMap.get(userVO.getId())));
        }
        
        return Result.buildSucc(userVOS);
    }
    @Override
    public Result<Void> deleteByUserId(Integer userId) {
        if(userId == null) {
            return Result.fail("userId is null!");
        }

        boolean success = userDao.deleteByUserId(userId);
        if(success){
            userRoleService.deleteByUserIdOrRoleId(userId, null);
        }

        return success ? Result.success() : Result.fail();
    }

    @Override
    public UserBriefVO getUserBriefByUserName(String userName) {
        if(StringUtils.isEmpty(userName)) {
            return null;
        }
        User user = userDao.selectByUsername(userName);
        return CopyBeanUtil.copy(user, UserBriefVO.class);
    }

    @Override
    public User getUserByUserName(String userName){
        return userDao.selectByUsername(userName);
    }

    @Override
    public List<UserBriefVO> getUserBriefListByUserIdList(List<Integer> userIdList) {
        if(CollectionUtils.isEmpty(userIdList)) {
            return new ArrayList<>();
        }
        List<UserBrief> userBriefList = userDao.selectBriefListByUserIdList(userIdList);
        final List<UserBriefVO> userBriefVOS = CopyBeanUtil.copyList(userBriefList,
            UserBriefVO.class);
        for (UserBriefVO userBriefVO : userBriefVOS) {
            final User user = userDao.selectByUserId(userBriefVO.getId());
            userBriefVO.setEmail(user.getEmail());
            userBriefVO.setPhone(user.getPhone());
            final List<String> roles = roleService.getRoleBriefListByUserId(
                    userBriefVO.getId())
                .stream()
                .map(RoleBriefVO::getRoleName)
                .collect(Collectors.toList());
            userBriefVO.setRoleList(roles);
        }
        return userBriefVOS;
    }

    @Override
    public List<UserBriefVO> getUserBriefListByUsernameOrRealName(String name) {
        List<UserBrief> userBriefList = userDao.selectBriefListByNameAndDescOrderByCreateTime(name);
        return CopyBeanUtil.copyList(userBriefList, UserBriefVO.class);
    }

    @Override
    public List<UserBriefVO> getAllUserBriefListOrderByCreateTime(boolean isAsc) {
        List<UserBrief> userBriefList = userDao.selectBriefListOrderByCreateTime(isAsc);
        return CopyBeanUtil.copyList(userBriefList, UserBriefVO.class);
    }

    @Override
    public List<Integer> getUserIdListByUsernameOrRealName(String name) {
        return userDao.selectUserIdListByUsernameOrRealName(name);
    }

    @Override
    public List<UserBriefVO> getAllUserBriefList() {
        List<UserBrief> userBriefList = userDao.selectAllBriefList();
        return CopyBeanUtil.copyList(userBriefList, UserBriefVO.class);
    }

    @Override
    public List<UserBriefVO> getUserBriefListByDeptId(Integer deptId) {
        // ????????????id???????????????????????????????????????????????????????????????
        List<Integer> deptIdList = deptService.getDeptIdListByParentId(deptId);
        List<UserBrief> userBriefList = userDao.selectBriefListByDeptIdList(deptIdList);
        return CopyBeanUtil.copyList(userBriefList, UserBriefVO.class);
    }

    @Override
    public List<AssignInfoVO> getAssignDataByUserId(Integer userId) throws KfSecurityException {
        if(userId == null) {
            throw new KfSecurityException(ResultCode.USER_ID_CANNOT_BE_NULL);
        }
        // ??????????????????
        List<RoleBriefVO> roleBriefVOList = roleService.getAllRoleBriefList();
        // ??????????????????????????????
        Set<Integer> hasRoleIdSet = new HashSet<>(userRoleService.getRoleIdListByUserId(userId));

        List<AssignInfoVO> list = new ArrayList<>();
        for(RoleBriefVO roleBriefVO : roleBriefVOList) {
            AssignInfoVO data = new AssignInfoVO();
            data.setName(roleBriefVO.getRoleName());
            data.setId(roleBriefVO.getId());
            data.setHas(hasRoleIdSet.contains(roleBriefVO.getId()));
            list.add(data);
        }
        return list;
    }

    @Override
    public List<UserBriefVO> getUserBriefListByRoleId(Integer roleId) {
        // ?????????????????????????????????id
        List<Integer> userIdList = userRoleService.getUserIdListByRoleId(roleId);
        List<UserBrief> userBriefList = userDao.selectBriefListByUserIdList(userIdList);
        return CopyBeanUtil.copyList(userBriefList, UserBriefVO.class);
    }

    @Override
    public Result<Void> addUser(UserDTO userDTO, String operator) {
        if(null != userDao.selectByUsername(userDTO.getUserName())){
            return Result.fail(ResultCode.USER_ACCOUNT_ALREADY_EXIST);
        }

        try {
            UserPO userPO = CopyBeanUtil.copy(userDTO, UserPO.class);
            if(userDao.addUser(userPO) > 0){
                userRoleService.updateUserRoleByUserId(userPO.getId(), userDTO.getRoleIds());
            }
        } catch (Exception e) {
            return Result.fail(ResultCode.USER_ACCOUNT_INSERT_FAIL);
        }

        return Result.success();
    }

    @Override
    public Result<Void> editUser(UserDTO userDTO, String operator) {
        User user = userDao.selectByUsername(userDTO.getUserName());
        if(null == user){
            return Result.fail(ResultCode.USER_ACCOUNT_NOT_EXIST);
        }

        try {
            UserPO userPO = CopyBeanUtil.copy(userDTO, UserPO.class);
            userPO.setId(user.getId());
            if(userDao.editUser(userPO) > 0){
                userRoleService.updateUserRoleByUserId(userPO.getId(), userDTO.getRoleIds());
            }
        } catch (Exception e) {
            return Result.fail(ResultCode.USER_ACCOUNT_UPDATE_FAIL);
        }

        return Result.success();
    }

    /**
     * ????????????
     *
     * @param userVo ??????????????????????????????
     */
    private void privacyProcessing(UserVO userVo) {
        String phone = userVo.getPhone();
        userVo.setPhone(phone.replaceAll("(\\d{3})\\d{4}(\\d{4})","$1****$2"));
    }

    private Result<Void> userNameCheck(String userName){
        if(!P_USER_NAME.matcher(userName).matches()){
            return Result.fail(ResultCode.USER_NAME_FORMAT_ERROR);
        }

        if(null != userDao.selectByUsername(userName)){
            return Result.fail(ResultCode.USER_NAME_EXISTS);
        }

        return Result.success();
    }

    private Result<Void> userPhoneCheck(String userPhone){
        if(!P_USER_PHONE.matcher(userPhone).matches()){
            return Result.fail(ResultCode.USER_NAME_FORMAT_ERROR);
        }

        if(null != userDao.selectByUserPhone(userPhone)){
            return Result.fail(ResultCode.USER_PHONE_EXIST);
        }

        return Result.success();
    }

    private Result<Void> userMailCheck(String userMail){
        if(!P_USER_MAIL.matcher(userMail).matches()){
            return Result.fail(ResultCode.USER_EMAIL_FORMAT_ERROR);
        }

        if(null != userDao.selectByUserMail(userMail)){
            return Result.fail(ResultCode.USER_EMAIL_EXIST);
        }

        return Result.success();
    }
}