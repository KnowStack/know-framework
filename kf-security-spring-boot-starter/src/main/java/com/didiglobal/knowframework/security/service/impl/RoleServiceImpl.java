package com.didiglobal.knowframework.security.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.didiglobal.knowframework.security.common.PagingData;
import com.didiglobal.knowframework.security.common.constant.OplogConstant;
import com.didiglobal.knowframework.security.common.entity.UserRole;
import com.didiglobal.knowframework.security.common.enums.ResultCode;
import com.didiglobal.knowframework.security.common.enums.message.MessageCode;
import com.didiglobal.knowframework.security.common.vo.permission.PermissionTreeVO;
import com.didiglobal.knowframework.security.common.vo.role.AssignInfoVO;
import com.didiglobal.knowframework.security.common.vo.role.RoleBriefVO;
import com.didiglobal.knowframework.security.common.vo.role.RoleDeleteCheckVO;
import com.didiglobal.knowframework.security.common.vo.user.UserBasicVO;
import com.didiglobal.knowframework.security.common.vo.user.UserBriefVO;
import com.didiglobal.knowframework.security.dao.RoleDao;
import com.didiglobal.knowframework.security.exception.KfSecurityException;
import com.didiglobal.knowframework.security.service.*;
import com.didiglobal.knowframework.security.util.CopyBeanUtil;
import com.didiglobal.knowframework.security.util.HttpRequestUtil;
import com.didiglobal.knowframework.security.util.MathUtil;
import com.didiglobal.knowframework.security.common.entity.role.Role;
import com.didiglobal.knowframework.security.common.entity.role.RoleBrief;
import com.didiglobal.knowframework.security.common.dto.role.RoleAssignDTO;
import com.didiglobal.knowframework.security.common.dto.role.RoleQueryDTO;
import com.didiglobal.knowframework.security.common.dto.role.RoleSaveDTO;
import com.didiglobal.knowframework.security.common.vo.role.RoleVO;
import com.didiglobal.knowframework.security.common.dto.message.MessageDTO;
import com.didiglobal.knowframework.security.common.dto.oplog.OplogDTO;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;

/**
 * @author cjm
 */
@Service("kfecurityRoleServiceImpl")
public class RoleServiceImpl implements RoleService {

    @Autowired
    private RoleDao roleDao;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private OplogService oplogService;

    @Autowired
    private UserService userService;

    @Autowired
    private RolePermissionService rolePermissionService;

    @Autowired
    private UserRoleService userRoleService;
    
    @Override
    public RoleBriefVO getRoleBriefByRoleId(Integer roleId) {
        Role role = roleDao.selectByRoleId(roleId);
        return CopyBeanUtil.copy(role,RoleBriefVO.class);
    }
    
    @Override
    public RoleVO getRoleDetailByRoleId(Integer roleId) {
        Role role = roleDao.selectByRoleId(roleId);
        if(role == null) {
            return null;
        }
        // ????????????id???????????????
        PermissionTreeVO permissionTreeVO = permissionService.buildPermissionTreeByRoleId(role.getId());
        RoleVO roleVo = CopyBeanUtil.copy(role, RoleVO.class);
        roleVo.setPermissionTreeVO(permissionTreeVO);
        roleVo.setCreateTime(role.getCreateTime());
        roleVo.setUpdateTime(role.getUpdateTime());

        // ?????????????????????
        List<Integer>     userIdList      = userRoleService.getUserIdListByRoleId(roleId);
        List<UserBriefVO> userBriefVOList = userService.getUserBriefListByUserIdList(userIdList);
        List<String>      userNames       = CollectionUtils.isEmpty(userBriefVOList)
                                            ? new ArrayList<>()
                                            : userBriefVOList.stream().map(UserBriefVO::getUserName).collect(Collectors.toList());

        roleVo.setAuthedUserCnt(userIdList.size());
        roleVo.setAuthedUsers(userNames);
        return roleVo;
    }

    @Override
    public PagingData<RoleVO> getRolePage(RoleQueryDTO queryDTO) {
        IPage<Role> pageInfo = roleDao.selectPage(queryDTO);
        // ???????????? rolIds
        List<Integer> roleIds = pageInfo.getRecords().stream().map(Role::getId).collect(Collectors.toList());
        // ??????????????? userId ??? roleId ?????????
        List<UserRole> userRoles = userRoleService.getByRoleIds(roleIds);

        // ???????????????????????????
        List<Integer> userIds = userRoles.stream().map(UserRole::getUserId).distinct().collect(Collectors.toList());
        // ?????????????????????????????????
        List<UserBasicVO> userBasicVOS = userService.getUserBasicListByUserIdList(userIds);
        // ????????? map
        Map<Integer, String> userId2usernameMap = userBasicVOS.stream().collect(
                Collectors.toMap(UserBasicVO::getId, UserBasicVO::getUserName));
        // ??????
        Map<Integer/*roleId*/,/*userId*/ List<Integer>> roleId2UserIdListMap =
                userRoles.stream().collect(
                        Collectors.groupingBy(UserRole::getRoleId,
                                Collectors.mapping(UserRole::getUserId,
                                        Collectors.toList())));
        Map<Integer/*roleId*/,/*userNames*/ List<String>> roleId2UserNameListMap =
                userRoles.stream().collect(
                        Collectors.groupingBy(UserRole::getRoleId,
                                Collectors.mapping(i->userId2usernameMap.get(i.getUserId()),
                                        Collectors.toList())));
        List<RoleVO> roleVOList = new ArrayList<>();
        for(Role role : pageInfo.getRecords()) {
            RoleVO roleVO = CopyBeanUtil.copy(role, RoleVO.class);
            List<Integer> userIdList = roleId2UserIdListMap.getOrDefault(role.getId(),Collections.emptyList());
            // ???????????????????????????????????????
            roleVO.setAuthedUserCnt(userIdList.size());
            roleVO.setAuthedUsers(roleId2UserNameListMap.getOrDefault(role.getId(),Collections.emptyList()));
            roleVO.setCreateTime(role.getCreateTime());
            roleVO.setUpdateTime(role.getUpdateTime());
            roleVOList.add(roleVO);
        }
        return new PagingData<>(roleVOList, pageInfo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createRole(RoleSaveDTO roleSaveDTO, HttpServletRequest request) throws KfSecurityException {
        // ????????????
        checkParam(roleSaveDTO, false);
        // ??????????????????
        Role role = CopyBeanUtil.copy(roleSaveDTO, Role.class);
        // ?????????????????????
        UserBriefVO userBriefVO = userService.getUserBriefByUserName(HttpRequestUtil.getOperator(request));
        if(userBriefVO != null) {
            role.setLastReviser(userBriefVO.getUserName());
        }
        // ??????????????????
        role.setRoleCode("r" + MathUtil.getRandomNumber(7));
        roleDao.insert(role);
        // ????????????????????????????????????
        rolePermissionService.saveRolePermission(role.getId(), roleSaveDTO.getPermissionIdList());
        // ??????????????????
        oplogService.saveOplog(new OplogDTO(HttpRequestUtil.getOperator(request),
                OplogConstant.RM_A, OplogConstant.RM, roleSaveDTO.getRoleName(), JSON.toJSONString(roleSaveDTO)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRoleByRoleId(Integer roleId, HttpServletRequest request) throws KfSecurityException {
        Role role = roleDao.selectByRoleId(roleId);
        if(role == null) {
            return;
        }
        // ????????????????????????????????????
        List<Integer> userIdList = userRoleService.getUserIdListByRoleId(roleId);
        if(!userIdList.isEmpty()) {
            throw new KfSecurityException(ResultCode.ROLE_USER_AUTHED);
        }
        // ??????????????????????????????
        rolePermissionService.deleteRolePermissionByRoleId(roleId);
        // ????????????????????????
        roleDao.deleteByRoleId(roleId);
        // ??????????????????
        OplogDTO oplogDTO = new OplogDTO(HttpRequestUtil.getOperator(request),
                OplogConstant.RM_D, OplogConstant.RM, role.getRoleName(), JSON.toJSONString(role));
        oplogService.saveOplog(oplogDTO);
    }

    @Override
    public void deleteUserFromRole(Integer roleId, Integer userId, HttpServletRequest request) throws KfSecurityException {
        Role role = roleDao.selectByRoleId(roleId);
        if(role == null) {return;}

        // ??????????????????
        userRoleService.deleteByUserIdOrRoleId(userId, roleId);
        //??????????????????????????????????????????????????????
        Role updateLastReviserById = new Role();
        updateLastReviserById.setId(roleId);
        String operator = HttpRequestUtil.getOperator(request);
        // ??????????????????
        UserBriefVO userBriefVO = userService.getUserBriefByUserName(operator);
        if (userBriefVO != null) {
            updateLastReviserById.setLastReviser(userBriefVO.getUserName());
        }
        roleDao.update(updateLastReviserById);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRole(RoleSaveDTO saveDTO, HttpServletRequest request) throws KfSecurityException {
        if(roleDao.selectByRoleId(saveDTO.getId()) == null) {
            throw new KfSecurityException(ResultCode.ROLE_NOT_EXISTS);
        }
        checkParam(saveDTO, true);
        // ????????????????????????
        Role role = CopyBeanUtil.copy(saveDTO, Role.class);
        // ?????????????????????
        UserBriefVO userBriefVO = userService.getUserBriefByUserName(HttpRequestUtil.getOperator(request));
        if(userBriefVO != null) {
            role.setLastReviser(userBriefVO.getUserName());
        }
        roleDao.update(role);
        // ?????????????????????????????????
        rolePermissionService.updateRolePermission(role.getId(), saveDTO.getPermissionIdList());
        // ??????????????????
        oplogService.saveOplog( new OplogDTO(HttpRequestUtil.getOperator(request),
                OplogConstant.RM_E, OplogConstant.RM, saveDTO.getRoleName(), JSON.toJSONString(saveDTO)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(RoleAssignDTO assignDTO, HttpServletRequest request) throws KfSecurityException {
        String operator = HttpRequestUtil.getOperator(request);
        if(assignDTO.getFlag() == null) {
            throw new KfSecurityException(ResultCode.ROLE_ASSIGN_FLAG_IS_NULL);
        }
        if(Boolean.TRUE.equals(assignDTO.getFlag())) {
            // N??????????????????1?????????
            Integer userId = assignDTO.getId();
            // ??????old???????????????????????????
            List<Integer> oldRoleIdList = userRoleService.getRoleIdListByUserId(userId);
            // ??????????????????
            userRoleService.updateUserRoleByUserId(userId, assignDTO.getIdList());
            // ??????????????????
            UserBriefVO userBriefVO = userService.getUserBriefByUserName(operator);
            Integer oplogId = oplogService.saveOplog(new OplogDTO(operator,
                    OplogConstant.RM_E, OplogConstant.RM, userBriefVO.getUserName(),
                    "????????????????????????" + JSON.toJSONString(assignDTO)));
            // ?????????????????????????????????
            packAndSaveMessage(oplogId, oldRoleIdList, assignDTO);
        } else {
            // 1??????????????????N?????????
            Integer roleId = assignDTO.getId();
            // ??????old???????????????????????????
            List<Integer> oldUserIdList = userRoleService.getUserIdListByRoleId(roleId);
            // ??????????????????
            userRoleService.updateUserRoleByRoleId(roleId, assignDTO.getIdList());
            //??????????????????????????????????????????????????????
            Role updateLastReviserById = new Role();
            updateLastReviserById.setId(roleId);
            // ?????????????????????
            UserBriefVO userBriefVO = userService.getUserBriefByUserName(
                HttpRequestUtil.getOperator(request));
            if (userBriefVO != null) {
                updateLastReviserById.setLastReviser(userBriefVO.getUserName());
            }
            roleDao.update(updateLastReviserById);
            
            
            // ??????????????????
            Role role = roleDao.selectByRoleId(assignDTO.getId());
            Integer oplogId = oplogService.saveOplog(new OplogDTO(operator,
                    OplogConstant.RM_E, OplogConstant.RM, role.getRoleName(),
                    "????????????????????????" + JSON.toJSONString(assignDTO)));
            // ?????????????????????????????????
            packAndSaveMessage(oplogId, oldUserIdList, assignDTO);
        }
    }

    @Override
    public List<RoleBriefVO> getRoleBriefListByRoleName(String roleName) {
        List<RoleBrief> roleBriefList = roleDao.selectBriefListByRoleNameAndDescOrderByCreateTime(roleName);
        return CopyBeanUtil.copyList(roleBriefList, RoleBriefVO.class);
    }

    @Override
    public RoleDeleteCheckVO checkBeforeDelete(Integer roleId) {
        if(roleId == null) {
            return null;
        }
        RoleDeleteCheckVO roleDeleteCheckVO = new RoleDeleteCheckVO();
        roleDeleteCheckVO.setRoleId(roleId);
        // ????????????idList
        List<Integer> userIdList = userRoleService.getUserIdListByRoleId(roleId);
        if(!CollectionUtils.isEmpty(userIdList)) {
            // ????????????????????????List
            List<UserBriefVO> list = userService.getUserBriefListByUserIdList(userIdList);
            List<String> usernameList = list.stream().map(UserBriefVO::getUserName ).collect(Collectors.toList());
            roleDeleteCheckVO.setUserNameList(usernameList);
        }
        return roleDeleteCheckVO;
    }

    @Override
    public List<RoleBriefVO> getAllRoleBriefList() {
        List<RoleBrief> roleBriefList = roleDao.selectAllBrief();
        return CopyBeanUtil.copyList(roleBriefList, RoleBriefVO.class);
    }

    @Override
    public List<RoleBriefVO> getRoleBriefListByUserId(Integer userId) {
        // ???????????????????????????
        List<Integer> roleIdList = userRoleService.getRoleIdListByUserId(userId);
        if(CollectionUtils.isEmpty(roleIdList)) {
            return new ArrayList<>();
        }
        List<RoleBrief> roleBriefList =  roleDao.selectBriefListByRoleIdList(roleIdList);
        return CopyBeanUtil.copyList(roleBriefList, RoleBriefVO.class);
    }

    @Override
    public Map<Integer, List<RoleBriefVO>> getRoleBriefListByUserIds(List<Integer> userId) {
        List<UserRole> userRoleList = userRoleService.getRoleIdListByUserIds(userId);
        //??????????????????id
        final List<Integer> roleIds = userRoleList.stream().map(UserRole::getRoleId)
                .collect(Collectors.toList());
        //??????????????????
        final List<RoleBriefVO> roleBriefs = CopyBeanUtil.copyList(
                roleDao.selectBriefListByRoleIdList(roleIds), RoleBriefVO.class);
        //??????
        final Map<Integer, RoleBriefVO> roleId2RoleMap = roleBriefs.stream()
                .collect(Collectors.toMap(RoleBriefVO::getId, i -> i));
        return userRoleList.stream()
                .collect(Collectors.groupingBy(UserRole::getUserId,
                        Collectors.mapping(i -> roleId2RoleMap.get(i.getRoleId()), Collectors.toList())));
    }

    @Override
    public List<AssignInfoVO> getAssignInfoByRoleId(Integer roleId) {
        if(roleId == null) {
            return new ArrayList<>();
        }
        // ????????????List
        List<UserBriefVO> userBriefVOList = userService.getAllUserBriefList();

        // ????????????????????????????????????????????????set
        List<Integer> userIdList = userRoleService.getUserIdListByRoleId(roleId);
        Set<Integer> hasRoleUserIdSet = new HashSet<>(userIdList);

        // ??????List<AssignDataVo>
        List<AssignInfoVO> result = new ArrayList<>();
        for(UserBriefVO userBriefVO : userBriefVOList) {
            AssignInfoVO assignInfoVO = new AssignInfoVO();
            // ?????????????????????????????????
            assignInfoVO.setHas(hasRoleUserIdSet.contains(userBriefVO.getId()));
            assignInfoVO.setName(userBriefVO.getUserName());
            assignInfoVO.setId(userBriefVO.getId());
            result.add(assignInfoVO);
        }
        return result;
    }

    private void packAndSaveMessage(Integer oplogId, List<Integer> oldIdList, RoleAssignDTO roleAssignDTO) {
        List<Integer> newIdList = roleAssignDTO.getIdList();

        List<Integer> removeIdList = new ArrayList<>();
        List<Integer> addIdList = new ArrayList<>();
        // ?????????
        Set<Integer> set = MathUtil.getIntersection(oldIdList, newIdList);
        for(Integer oldId : oldIdList) {
            if(!set.contains(oldId)) {
                removeIdList.add(oldId);
            }
        }
        for(Integer newId : newIdList) {
            if(!set.contains(newId)) {
                addIdList.add(newId);
            }
        }

        if (Boolean.TRUE.equals(roleAssignDTO.getFlag())) {
            // ?????????N??????????????????1????????????oldIdList???newIdList????????????idList
            List<Integer> userIdList = new ArrayList<>();
            userIdList.add(roleAssignDTO.getId());
            // ???????????????????????? ??? ??????????????????
            saveRoleAssignMessage(oplogId, userIdList, removeIdList, userIdList, addIdList);
        } else {
            // 1??????????????????N????????????oldIdList???newIdList????????????idList
            List<Integer> roleIdList = new ArrayList<>();
            roleIdList.add(roleAssignDTO.getId());
            // ???????????????????????? ??? ??????????????????
            saveRoleAssignMessage(oplogId, removeIdList, roleIdList, addIdList, roleIdList);
        }
    }

    /**
     * ??????????????????????????????
     * @param oplogId ????????????id
     * @param removeUserIdList ????????????????????????idList
     * @param removeRoleIdList ???????????????idList
     * @param addUserIdList ?????????????????????idList
     * @param addRoleIdList ???????????????idList
     */
    private void saveRoleAssignMessage(Integer oplogId,
                                       List<Integer> removeUserIdList, List<Integer> removeRoleIdList,
                                       List<Integer> addUserIdList, List<Integer> addRoleIdList) {
        // ??????????????????
        SimpleDateFormat formatter= new SimpleDateFormat("MM-dd HH:mm");
        Date date = new Date(System.currentTimeMillis());
        String time = formatter.format(date);

        // ???????????????????????????
        String addRoleInfo = spliceRoleNameByRoleIdList(addRoleIdList);
        String removeRoleInfo = spliceRoleNameByRoleIdList(removeRoleIdList);

        List<MessageDTO> messageDTOList = new ArrayList<>();
        if(!StringUtils.isEmpty(addRoleInfo)) {
            for(Integer userId : addUserIdList) {
                MessageDTO messageDTO = new MessageDTO(userId, oplogId);
                // ???????????????
                String content = String.format(MessageCode.ROLE_ADD_MESSAGE.getContent(), time, addRoleInfo);
                messageDTO.setContent(content);
                messageDTO.setTitle(MessageCode.ROLE_ADD_MESSAGE.getTitle());
                messageDTOList.add(messageDTO);
            }
        }
        if(!StringUtils.isEmpty(removeRoleInfo)) {
            for(Integer userId : removeUserIdList) {
                MessageDTO messageDTO = new MessageDTO(userId, oplogId);
                // ???????????????
                String content = String.format(MessageCode.ROLE_REMOVE_MESSAGE.getContent(), time, removeRoleInfo);
                messageDTO.setContent(content);
                messageDTO.setTitle(MessageCode.ROLE_REMOVE_MESSAGE.getTitle());
                messageDTOList.add(messageDTO);
            }
        }

        messageService.saveMessages(messageDTOList);
    }

    private String spliceRoleNameByRoleIdList(List<Integer> roleIdList) {
        List<RoleBrief> roleBriefList = roleDao.selectBriefListByRoleIdList(roleIdList);
        if(roleBriefList.isEmpty()) {
            return null;
        }
        // ??????????????????
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < roleBriefList.size() - 1; i++) {
            sb.append(roleBriefList.get(i).getRoleName()).append(",");
        }
        sb.append(roleBriefList.get(roleBriefList.size() - 1).getRoleName());
        return sb.toString();
    }

    /**
     * ????????????????????????????????????
     * @param saveDTO ????????????
     * @param isUpdate ?????????????????????
     * @throws KfSecurityException ????????????
     */
    private void checkParam(RoleSaveDTO saveDTO, boolean isUpdate) throws KfSecurityException {
        if(StringUtils.isEmpty(saveDTO.getRoleName())) {
            throw new KfSecurityException(ResultCode.ROLE_NAME_CANNOT_BE_BLANK);
        }
        
        if(CollectionUtils.isEmpty(saveDTO.getPermissionIdList())) {
            throw new KfSecurityException(ResultCode.ROLE_PERMISSION_CANNOT_BE_NULL);
        }
        // ??????????????????????????????????????????????????????????????????old??????
        Integer roleId = isUpdate ? saveDTO.getId() : null;
        int count = roleDao.selectCountByRoleNameAndNotRoleId(saveDTO.getRoleName(), roleId);
        if(count > 0) {
            // ?????????????????????
            throw new KfSecurityException(ResultCode.ROLE_NAME_ALREADY_EXISTS);
        }
    }
}