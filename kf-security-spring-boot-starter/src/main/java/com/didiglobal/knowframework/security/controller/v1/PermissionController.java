package com.didiglobal.knowframework.security.controller.v1;

import com.didiglobal.knowframework.security.common.Result;
import com.didiglobal.knowframework.security.common.constant.Constants;
import com.didiglobal.knowframework.security.common.dto.permission.PermissionDTO;
import com.didiglobal.knowframework.security.common.vo.permission.PermissionTreeVO;
import com.didiglobal.knowframework.security.service.PermissionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author cjm
 */
@RestController
@Api(value = "kf-security-permission相关API接口", tags = "kf-security-权限相关API接口")
@RequestMapping(Constants.API_PREFIX_V1 + "/permission")
public class PermissionController {

    @Autowired
    private PermissionService permissionService;

    @GetMapping("/tree")
    @ApiOperation(value = "获取所有权限", notes = "以树的形式返回所有权限")
    public Result<PermissionTreeVO> tree() {
        PermissionTreeVO permissionTreeVO = permissionService.buildPermissionTree();
        return Result.success(permissionTreeVO);
    }

    @PostMapping("/import")
    @ApiOperation(value = "权限信息导入", notes = "权限信息导入")
    public Result<String> imports(
            @RequestBody @ApiParam(name = "permissionDTOList", value = "权限信息List") List<PermissionDTO> list) {
        permissionService.savePermission(list);
        return Result.success();
    }
}
