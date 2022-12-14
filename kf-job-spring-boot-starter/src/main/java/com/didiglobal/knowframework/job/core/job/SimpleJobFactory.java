package com.didiglobal.knowframework.job.core.job;

import com.didiglobal.knowframework.job.core.WorkerSingleton;
import com.didiglobal.knowframework.job.common.domain.LogIJob;
import com.didiglobal.knowframework.job.common.domain.LogITask;
import com.didiglobal.knowframework.job.common.enums.JobStatusEnum;
import com.didiglobal.knowframework.job.utils.IdWorker;
import com.didiglobal.knowframework.log.ILog;
import com.didiglobal.knowframework.log.LogFactory;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;

/**
 * simple job factory.
 *
 * @author ds
 */
@Component
public class SimpleJobFactory implements JobFactory {

    private static final ILog logger     = LogFactory.getLog(SimpleJobFactory.class);

    private Map<String, Job> jobMap = new HashMap<>();

    @Override
    public void addJob(String className, Job job) {
        jobMap.put(className, job);

        logger.info("class=SimpleJobFactory||method=addJob||className={}||jobMap={}",
                className, jobMap.toString());
    }

    @Override
    public LogIJob newJob(LogITask logITask) {
        Job job = jobMap.get(logITask.getClassName());
        if(null == job) {
            return null;
        }
        LogIJob logIJob = new LogIJob();
        logIJob.setJobCode(IdWorker.getIdStr());
        logIJob.setTaskCode(logITask.getTaskCode());
        logIJob.setTaskId(logITask.getId());
        logIJob.setTaskName(logITask.getTaskName());
        logIJob.setTaskDesc(logITask.getTaskDesc());
        logIJob.setClassName(logITask.getClassName());
        logIJob.setWorkerCode(WorkerSingleton.getInstance().getLogIWorker().getWorkerCode());
        logIJob.setWorkerIp(WorkerSingleton.getInstance().getLogIWorker().getIp());
        logIJob.setTryTimes(logITask.getRetryTimes() == null ? 1 : logITask.getRetryTimes());
        logIJob.setStatus(JobStatusEnum.STARTED.getValue());
        logIJob.setTimeout(logITask.getTimeout());
        logIJob.setJob(job);
        logIJob.setTaskCallback(logITask.getTaskCallback());
        logIJob.setAppName(logITask.getAppName());
        return logIJob;
    }

}
