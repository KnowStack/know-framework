package com.didiglobal.knowframework.job.rest;

import com.didiglobal.knowframework.job.common.dto.LogITaskDTO;
import com.didiglobal.knowframework.job.utils.BeanUtil;
import com.didiglobal.knowframework.job.common.PagingResult;
import com.didiglobal.knowframework.job.common.Result;
import com.didiglobal.knowframework.job.common.domain.LogITask;
import com.didiglobal.knowframework.job.common.dto.LogITaskCopyDTO;
import com.didiglobal.knowframework.job.common.dto.LogITaskUpdateDTO;
import com.didiglobal.knowframework.job.common.dto.TaskPageQueryDTO;
import com.didiglobal.knowframework.job.common.vo.LogITaskVO;
import com.didiglobal.knowframework.job.core.consensual.ConsensualEnum;
import com.didiglobal.knowframework.job.core.task.TaskManager;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.didiglobal.knowframework.job.common.CommonUtil.isCopyTask;
import static com.didiglobal.knowframework.job.common.CommonUtil.sqlFuzzyQueryTransfer;

/**
 * task controller.
 *
 * @author ds
 */
@RestController
@RequestMapping(Constants.V1 + "/logi-job/task")
@Api(tags = "logi-job 的任务相关接口")
public class TaskController {

    @Autowired
    private TaskManager taskManager;

    @PostMapping("/{taskCode}/do")
    @ApiOperation(value = "执行调度任务", notes = "")
    public Result<Boolean> execute(@PathVariable String taskCode) {
        return taskManager.execute(taskCode, false);
    }

    @PostMapping("/list")
    @ApiOperation(value = "获取所有的调度任务", notes = "")
    public PagingResult<LogITaskVO> getAll(@RequestBody TaskPageQueryDTO taskPageQueryDTO) {
        taskPageQueryDTO.setTaskDesc(sqlFuzzyQueryTransfer(taskPageQueryDTO.getTaskDesc()));
        taskPageQueryDTO.setClassName(sqlFuzzyQueryTransfer(taskPageQueryDTO.getClassName()));

        List<LogITask> logITasks = taskManager.getPagineList(taskPageQueryDTO);
        int count = taskManager.pagineTaskConut(taskPageQueryDTO);

        return PagingResult.buildSucc(
                logITask2LogITaskVO(logITasks), count, taskPageQueryDTO.getPage(), taskPageQueryDTO.getSize()
        );
    }

    @PostMapping("/{taskCode}/{status}")
    @ApiOperation(value = "更新调度任务状态", notes = "")
    public Result<Boolean> status(@PathVariable String taskCode, @PathVariable Integer status) {
        return taskManager.updateTaskStatus(taskCode, status);
    }

    @GetMapping("/{taskCode}/detail")
    @ApiOperation(value = "调度任务详情", notes = "")
    public Result<LogITaskVO> detail(@PathVariable String taskCode) {
        return Result.buildSucc(logITask2LogITaskVO(taskManager.getByCode(taskCode)));
    }

    @PostMapping("/{taskCode}/{workerCode}/release")
    @ApiOperation(value = "恢复调度任务", notes = "")
    public Result<Boolean> release(@PathVariable String taskCode, @PathVariable String workerCode) {
        return taskManager.release(taskCode, workerCode);
    }

    @DeleteMapping("/{taskCode}")
    @ApiOperation(value = "删除调度任务", notes = "")
    public Result<Boolean> delete(@PathVariable String taskCode) {
        return taskManager.delete(taskCode);
    }

    @ApiOperation(value = "新增调度任务", notes = "")
    @RequestMapping(value = "", method = RequestMethod.POST)
    @ResponseBody
    public Result add(@RequestBody LogITaskDTO dto) {
        return taskManager.add(dto);
    }


    @PostMapping("/{taskCode}/copy")
    @ApiOperation(value = "复制调度任务", notes = "")
    public Result<Boolean> copy(@PathVariable String taskCode, @RequestBody LogITaskCopyDTO logITaskCopyDTO) {
        return taskManager.copy(taskCode, logITaskCopyDTO.getTaskDesc(),
                logITaskCopyDTO.getWorkerIps(), logITaskCopyDTO.getParam());
    }

    @PostMapping("/{taskCode}/update")
    @ApiOperation(value = "编辑调度任务", notes = "")
    public Result<Boolean> update(@PathVariable String taskCode, @RequestBody LogITaskUpdateDTO logITaskUpdateDTO) {
        return taskManager.updateWorkIpsParam(taskCode, logITaskUpdateDTO.getWorkerIps(), logITaskUpdateDTO.getParam());
    }

    /**************************************** private method ****************************************************/
    private List<LogITaskVO> logITask2LogITaskVO(List<LogITask> logITasks) {
        if (CollectionUtils.isEmpty(logITasks)) {
            return new ArrayList<>();
        }

        return logITasks.stream().map(l -> logITask2LogITaskVO(l)).collect(Collectors.toList());
    }

    private LogITaskVO logITask2LogITaskVO(LogITask logITask) {
        LogITaskVO logITaskVO = BeanUtil.convertTo(logITask, LogITaskVO.class);

        logITaskVO.setDel(0);
        if(!StringUtils.isEmpty(logITask.getTaskCode())
                && isCopyTask(logITask.getTaskCode())){
            logITaskVO.setDel(1);
        }

        if (!CollectionUtils.isEmpty(logITask.getTaskWorkers())) {
            List<String> ips = logITask.getTaskWorkers().stream().map(w -> w.getIp()).collect(Collectors.toList());

            logITaskVO.setRouting(ConsensualEnum.getByName(logITask.getConsensual()).getDesc());
            logITaskVO.setWorkerIps(ips);
        }

        return logITaskVO;
    }
}
