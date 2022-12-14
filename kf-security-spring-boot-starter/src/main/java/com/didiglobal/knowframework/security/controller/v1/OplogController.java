package com.didiglobal.knowframework.security.controller.v1;

import com.didiglobal.knowframework.security.common.PagingData;
import com.didiglobal.knowframework.security.common.PagingResult;
import com.didiglobal.knowframework.security.common.Result;
import com.didiglobal.knowframework.security.common.constant.Constants;
import com.didiglobal.knowframework.security.common.dto.oplog.OplogQueryDTO;
import com.didiglobal.knowframework.security.common.vo.oplog.OplogVO;
import com.didiglobal.knowframework.security.service.OplogService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author cjm
 */
@RestController
@Api(value = "kf-security-oplog相关API接口", tags = "kf-security-操作日志相关API接口")
@RequestMapping(Constants.API_PREFIX_V1 + "/oplog")
public class OplogController {

    @Autowired
    private OplogService oplogService;

    @PostMapping("/page")
    @ApiOperation(value = "查询操作日志列表", notes = "分页和条件查询")
    public PagingResult<OplogVO> page(@RequestBody OplogQueryDTO queryDTO) {
        PagingData<OplogVO> pageOplog = oplogService.getOplogPage(queryDTO);
        return PagingResult.success(pageOplog);
    }

    @GetMapping("/{id}")
    @ApiOperation(value = "获取操作日志详情", notes = "根据操作日志id获取操作日志详情")
    @ApiImplicitParam(name = "id", value = "操作日志id", dataType = "int", required = true)
    public Result<OplogVO> get(@PathVariable Integer id) {
        OplogVO oplogVO = oplogService.getOplogDetailByOplogId(id);
        return Result.success(oplogVO);
    }

    @GetMapping("/type/list")
    @ResponseBody
    @ApiOperation(value = "获取日志的模块列表", notes = "")
    public Result<List<String>> types() {
        return Result.buildSucc(oplogService.listTargetType());
    }
}
