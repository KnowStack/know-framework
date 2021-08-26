package com.didiglobal.logi.security.service.impl;

import com.alibaba.fastjson.JSON;
import com.didiglobal.logi.security.common.dto.permission.PermissionDTO;
import com.didiglobal.logi.security.common.entity.Permission;
import com.didiglobal.logi.security.common.enums.ResultCode;
import com.didiglobal.logi.security.common.vo.permission.PermissionTreeVO;
import com.didiglobal.logi.security.dao.PermissionDao;
import com.didiglobal.logi.security.exception.LogiSecurityException;
import com.didiglobal.logi.security.service.PermissionService;
import com.didiglobal.logi.security.service.RolePermissionService;
import com.didiglobal.logi.security.util.CopyBeanUtil;
import com.didiglobal.logi.security.util.MathUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * @author cjm
 */
@Service
public class PermissionServiceImpl implements PermissionService {

    @Autowired
    private PermissionDao permissionDao;

    @Autowired
    private RolePermissionService rolePermissionService;

    private PermissionTreeVO buildPermissionTree(Set<Integer> permissionHasSet) throws LogiSecurityException {
        // 获取全部权限，根据level小到大排序
        List<Permission> permissionList = permissionDao.selectAllAndAscOrderByLevel();

        // 创建一个虚拟根节点
        PermissionTreeVO root = PermissionTreeVO
                .builder().leaf(false).has(true).id(0).childList(new ArrayList<>()).build();

        // 转成树
        Map<Integer, PermissionTreeVO> parentMap = new HashMap<>(permissionList.size());
        parentMap.put(0, root);
        for(Permission permission : permissionList) {
            PermissionTreeVO permissionTreeVO = CopyBeanUtil.copy(permission, PermissionTreeVO.class);
            if(!permission.getLeaf()) {
                permissionTreeVO.setChildList(new ArrayList<>());
            }
            PermissionTreeVO parent = parentMap.get(permission.getParentId());
            if (parent == null) {
                // 如果parent为null，则需要查看下数据库权限表的数据是否有误
                // 1.可能出现了本来该是父节点的节点（有其他子节点的parent为它），但该节点parent为其他子节点的情况（数据异常）
                // 2.也可能是level填写错了（因为前面根据level大小排序）
                throw new LogiSecurityException(ResultCode.PERMISSION_DATA_ERROR);
            }
            // 父权限拥有，子权限才肯定拥有
            permissionTreeVO.setHas(parent.getHas() && permissionHasSet.contains(permission.getId()));
            parent.getChildList().add(permissionTreeVO);
            parentMap.put(permissionTreeVO.getId(), permissionTreeVO);
        }
        return root;
    }

    @Override
    public PermissionTreeVO buildPermissionTreeWithHas(List<Integer> permissionHasList) {
        PermissionTreeVO permissionTreeVO = null;
        try {
            permissionTreeVO = buildPermissionTree(new HashSet<>(permissionHasList));
        } catch (LogiSecurityException e) {
            e.printStackTrace();
        }
        return permissionTreeVO;
    }

    @Override
    public PermissionTreeVO buildPermissionTree() {
        return buildPermissionTreeWithHas(new ArrayList<>());
    }

    @Override
    public PermissionTreeVO buildPermissionTreeByRoleId(Integer roleId) {
        // 获取该角色拥有的全部权限id
        List<Integer> permissionIdList = rolePermissionService.getPermissionIdListByRoleId(roleId);
        return buildPermissionTreeWithHas(permissionIdList);
    }

    public static void main(String[] args) {
        List<PermissionDTO> permissionDTOList = new ArrayList<>();

        PermissionDTO permissionDTO = new PermissionDTO();
        permissionDTO.setPermissionName("一级部门1");
        PermissionDTO permissionDTO2 = new PermissionDTO();
        PermissionDTO permissionDTO3 = new PermissionDTO();
        PermissionDTO permissionDTO4 = new PermissionDTO();
        permissionDTO2.setPermissionName("二级部门1");
        permissionDTO3.setPermissionName("二级部门2");
        permissionDTO4.setPermissionName("三级部门3");
        permissionDTO.getChildPermissionList().add(permissionDTO2);
        permissionDTO.getChildPermissionList().add(permissionDTO3);
        permissionDTO.getChildPermissionList().add(permissionDTO4);
        permissionDTOList.add(permissionDTO);

        PermissionDTO permissionDTO11 = new PermissionDTO();
        permissionDTO11.setPermissionName("一级部门11");
        PermissionDTO permissionDTO22 = new PermissionDTO();
        PermissionDTO permissionDTO33 = new PermissionDTO();
        PermissionDTO permissionDTO44 = new PermissionDTO();
        permissionDTO22.setPermissionName("二级部门11");
        permissionDTO33.setPermissionName("二级部门22");
        permissionDTO44.setPermissionName("三级部门33");
        permissionDTO11.getChildPermissionList().add(permissionDTO22);
        permissionDTO11.getChildPermissionList().add(permissionDTO33);
        permissionDTO11.getChildPermissionList().add(permissionDTO44);
        permissionDTOList.add(permissionDTO11);

        PermissionDTO permissionDTO111 = new PermissionDTO();
        permissionDTO111.setPermissionName("三级部门111");
        permissionDTO22.getChildPermissionList().add(permissionDTO111);


        String s = JSON.toJSONString(permissionDTO);
        System.out.println(s);
        PermissionServiceImpl permissionService = new PermissionServiceImpl();
        permissionService.savePermission(permissionDTOList);
    }

    @Override
    public void savePermission(List<PermissionDTO> permissionDTOList) {
        if(CollectionUtils.isEmpty(permissionDTOList)) {
            return;
        }

        List<Permission> permissionList = new ArrayList<>();

        Map<PermissionDTO, Integer> permissionDTOMap = new HashMap<>();

        Queue<PermissionDTO> queue = new LinkedList<>();
        PermissionDTO permissionDTO = new PermissionDTO();
        permissionDTO.setChildPermissionList(permissionDTOList);
        queue.offer(permissionDTO);

        int level = 0;
        while(!queue.isEmpty()) {
            int size = queue.size();
            while(size-- > 0) {
                PermissionDTO dto = queue.poll();
                if(dto == null) {
                    continue;
                }
                Permission permission = CopyBeanUtil.copy(dto, Permission.class);
                if(level == 0) {
                    permission.setId(0);
                } else {
                    permission.setLevel(level);
                    permission.setId(Integer.parseInt(MathUtil.getRandomNumber(5) + "" + System.currentTimeMillis() % 1000));
                    permission.setParentId(permissionDTOMap.get(dto));
                    permissionList.add(permission);
                }
                permission.setLeaf(false);
                if(CollectionUtils.isEmpty(dto.getChildPermissionList())) {
                    permission.setLeaf(true);
                    continue;
                }

                for(PermissionDTO temp : dto.getChildPermissionList()) {
                    permissionDTOMap.put(temp, permission.getId());
                    queue.offer(temp);
                }
            }
            level++;
        }
        permissionDao.insertBatch(permissionList);
    }
}
