package com.didiglobal.knowframework.job.core.job.impl;

import com.alibaba.fastjson.JSON;
import com.didiglobal.knowframework.job.common.po.*;
import com.didiglobal.knowframework.job.core.WorkerSingleton;
import com.didiglobal.knowframework.job.core.job.JobExecutor;
import com.didiglobal.knowframework.job.core.job.JobFactory;
import com.didiglobal.knowframework.job.core.job.JobManager;
import com.didiglobal.knowframework.job.core.task.TaskLockService;
import com.didiglobal.knowframework.job.mapper.*;
import com.didiglobal.knowframework.job.utils.BeanUtil;
import com.didiglobal.knowframework.job.LogIJobProperties;
import com.didiglobal.knowframework.job.common.TaskResult;
import com.didiglobal.knowframework.job.common.domain.LogIJob;
import com.didiglobal.knowframework.job.common.domain.LogITask;
import com.didiglobal.knowframework.job.common.enums.JobStatusEnum;
import com.didiglobal.knowframework.job.common.enums.TaskWorkerStatusEnum;
import com.didiglobal.knowframework.job.core.job.JobContext;
import com.didiglobal.knowframework.job.utils.ThreadUtil;
import com.didiglobal.knowframework.log.ILog;
import com.didiglobal.knowframework.log.LogFactory;
import com.didiglobal.knowframework.observability.Observability;
import com.didiglobal.knowframework.observability.common.constant.Constant;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import static com.didiglobal.knowframework.job.common.TaskResult.FAIL_CODE;
import static com.didiglobal.knowframework.job.common.TaskResult.RUNNING_CODE;

/**
 * job manager impl.
 *
 * @author ds
 */
@Service
public class JobManagerImpl implements JobManager {
    private static final ILog logger     = LogFactory.getLog(JobManagerImpl.class);
    // ????????????????????????
    private static final int TRY_MAX_TIMES = 3;
    // ???????????????????????????sleep ?????? ???
    private static final int STOP_SLEEP_SECONDS = 3;

    // ??????????????????????????? ???
    private static final Long CHECK_BEFORE_INTERVAL = 60L;
    // ??????????????????????????? ???
    private static final Long RENEW_INTERVAL = 60L;

    private static final Long ONE_HOUR = 3600L;

    private JobFactory jobFactory;
    private LogIJobMapper logIJobMapper;
    private LogIJobLogMapper logIJobLogMapper;
    private LogITaskMapper logITaskMapper;
    private LogIWorkerMapper logIWorkerMapper;
    private JobExecutor jobExecutor;
    private TaskLockService taskLockService;
    private LogITaskLockMapper logITaskLockMapper;
    private LogIJobProperties logIJobProperties;

    private ConcurrentHashMap<LogIJob, Future> jobFutureMap = new ConcurrentHashMap<>();

    private final Cache<String, String> execuedJob = CacheBuilder.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES).maximumSize(1000).build();

    private Tracer tracer = Observability.getTracer(JobManagerImpl.class.getName());

    private static volatile Long timeoutTaskNumber = 0L;

    /**
     * construct
     * @param jobFactory job
     * @param logIJobMapper mapper
     * @param logIJobLogMapper mapper
     * @param logITaskMapper mapper
     * @param logIWorkerMapper logIWorkerMapper
     * @param jobExecutor jobExecutor
     * @param taskLockService service
     * @param logITaskLockMapper mapper
     * @param logIJobProperties ????????????
     */
    @Autowired
    public JobManagerImpl(JobFactory jobFactory,
                          LogIJobMapper logIJobMapper,
                          LogIJobLogMapper logIJobLogMapper,
                          LogITaskMapper logITaskMapper,
                          LogIWorkerMapper logIWorkerMapper,
                          JobExecutor jobExecutor, TaskLockService taskLockService,
                          LogITaskLockMapper logITaskLockMapper, LogIJobProperties logIJobProperties) {
        this.jobFactory = jobFactory;
        this.logIJobMapper = logIJobMapper;
        this.logIJobLogMapper = logIJobLogMapper;
        this.logITaskMapper = logITaskMapper;
        this.logIWorkerMapper = logIWorkerMapper;
        this.jobExecutor = jobExecutor;
        this.taskLockService = taskLockService;
        this.logITaskLockMapper = logITaskLockMapper;
        this.logIJobProperties = logIJobProperties;
        initialize();
    }

    private void initialize() {
        new Thread(new JobFutureHandler(), "JobFutureHandler Thread").start();
        new Thread(new LockRenewHandler(), "LockRenewHandler Thread").start();
        new Thread(new LogCleanHandler(this.logIJobProperties.getLogExpire()),
                "LogCleanHandler Thread").start();
    }

    @Override
    public Future<Object> start(LogITask logITask) {
        // ??????job??????
        LogIJob logIJob = jobFactory.newJob(logITask);
        if(null == logIJob){
            logger.error("class=JobHandler||method=start||classname={}||msg=logIJob is null", logITask.getClassName());
            return null;
        }

        LogIJobPO job = logIJob.getAuvJob();
        logIJobMapper.insert(job);

        Future jobFuture = jobExecutor.submit(new JobHandler(logIJob, logITask));
        jobFutureMap.put(logIJob, jobFuture);

        // ??????auvJobLog
        LogIJobLogPO logIJobLogPO = logIJob.getAuvJobLog();
        logIJobLogMapper.insert(logIJobLogPO);
        return jobFuture;
    }

    @Override
    public Integer runningJobSize() {
        return jobFutureMap.size();
    }

    @Override
    public boolean stopByTaskCode(String taskCode) {
        for (Map.Entry<LogIJob, Future> jobFuture : jobFutureMap.entrySet()) {
            LogIJob logIJob = jobFuture.getKey();
            if (Objects.equals(taskCode, logIJob.getTaskCode())) {
                return stopJob(logIJob, jobFuture.getValue());
            }
        }
        return true;
    }

    @Override
    public boolean stopByJobCode(String jobCode) {
        for (Map.Entry<LogIJob, Future> jobFuture : jobFutureMap.entrySet()) {
            LogIJob logIJob = jobFuture.getKey();
            if (Objects.equals(jobCode, logIJob.getJobCode())) {
                return stopJob(logIJob, jobFuture.getValue());
            }
        }
        return true;
    }

    @Override
    public int stopAll() {
        AtomicInteger succeedNum = new AtomicInteger();

        for (Map.Entry<LogIJob, Future> jobFuture : jobFutureMap.entrySet()) {
            LogIJob logIJob = jobFuture.getKey();
            if (stopJob(logIJob, jobFuture.getValue())) {
                succeedNum.addAndGet(1);
            }
        }

        return succeedNum.get();
    }

    @Override
    public List<LogIJob> getJobs() {
        List<LogIJobPO> logIJobPOS = logIJobMapper.selectByAppName(logIJobProperties.getAppName());
        if (CollectionUtils.isEmpty(logIJobPOS)) {
            return null;
        }
        List<LogIJob> logIJobDTOS = logIJobPOS.stream().map(logIJobPO -> BeanUtil.convertTo(logIJobPO, LogIJob.class))
                .collect(Collectors.toList());
        return logIJobDTOS;
    }

    /**
     * job ????????????.
     */
    class JobHandler implements Callable {

        private LogIJob logIJob;

        private LogITask logITask;

        public JobHandler(LogIJob logIJob, LogITask logITask) {
            this.logIJob  = logIJob;
            this.logITask = logITask;
        }

        @Override
        public Object call() {
            TaskResult object = null;

            logger.info("class=JobHandler||method=call||msg=start job {} with classname {}",
                    logIJob.getJobCode(), logIJob.getClassName());

            Span span = tracer.spanBuilder(
                    String.format("%s.%s", this.getClass().getName(), "call")
            ).startSpan();
            try(Scope scope = span.makeCurrent()) {

                span.setAttribute(Constant.ATTRIBUTE_KEY_SPAN_KIND, Constant.ATTRIBUTE_VALUE_SPAN_KIND_CRON_TASK);
                span.setAttribute(Constant.ATTRIBUTE_KEY_JOB_CLASS_NAME, logIJob.getClassName());
                span.setAttribute(Constant.ATTRIBUTE_KEY_TASK_NAME, logIJob.getTaskName());

                logIJob.setStartTime(new Timestamp(System.currentTimeMillis()));
                logIJob.setStatus(JobStatusEnum.SUCCEED.getValue());
                logIJob.setResult(new TaskResult(RUNNING_CODE, "task job is running!"));
                logIJob.setError("");

                //???????????????????????????
                LogIJobLogPO logIJobLogPO = logIJob.getAuvJobLog();
                logIJobLogMapper.updateByCode(logIJobLogPO);

                List<LogIWorkerPO>  logIWorkerPOS = logIWorkerMapper.selectByAppName(logIJobProperties.getAppName());
                List<String>        workCodes     = new ArrayList<>();

                if(CollectionUtils.isEmpty(logIWorkerPOS)){
                    workCodes.add(logIJob.getWorkerIp());
                }else {
                    workCodes.addAll(logIWorkerPOS.stream().map(LogIWorkerPO::getWorkerCode).collect(Collectors.toList()));
                }

                JobContext jobContext = new JobContext(logITask.getParams(), workCodes, logIJob.getWorkerCode());

                object = logIJob.getJob().execute(jobContext);

                logIJob.setResult(object);
                logIJob.setEndTime(new Timestamp(System.currentTimeMillis()));
                span.setStatus(StatusCode.OK);
            } catch (InterruptedException e) {
                // ????????????????????? ????????????/????????????
                logIJob.setStatus(JobStatusEnum.CANCELED.getValue());
                TaskResult taskResult = new TaskResult(FAIL_CODE, "task job be canceld!");
                logIJob.setResult(taskResult);
                String error = printStackTraceAsString(e);
                logIJob.setError(printStackTraceAsString(e));
                logger.error("class=JobHandler||method=call||classname={}||msg={}", logIJob.getClassName(), error);
                span.setStatus(StatusCode.ERROR, JSON.toJSONString(taskResult));
            } catch (Exception e) {
                // ????????????????????????
                logIJob.setStatus(JobStatusEnum.FAILED.getValue());
                TaskResult taskResult = new TaskResult(FAIL_CODE, "task job has exception when running!" + e);
                logIJob.setResult(taskResult);
                String error = printStackTraceAsString(e);
                logIJob.setError(printStackTraceAsString(e));
                logger.error("class=JobHandler||method=call||classname=||msg={}", logIJob.getClassName(), error);
                span.setStatus(StatusCode.ERROR, JSON.toJSONString(taskResult));
            } finally {
                try {
                    //???????????????????????????
                    LogIJobLogPO logIJobLogPO = logIJob.getAuvJobLog();
                    logIJobLogMapper.updateByCode(logIJobLogPO);

                    // job callback, ???????????????
                    if (logIJob.getTaskCallback() != null) {
                        logIJob.getTaskCallback().callback(logIJob.getTaskCode());
                    }
                } finally {
                    span.end();
                }
            }
            return object;
        }
    }

    /**
     * Job ??????????????????????????????????????????????????????????????????????????????????????????.
     */
    class JobFutureHandler implements Runnable {
        private static final long JOB_FUTURE_CLEAN_INTERVAL = 10L;

        public JobFutureHandler() {
        }

        @Override
        public void run() {
            while (true) {
                try {
                    // ??????????????????????????????
                    ThreadUtil.sleep(JOB_FUTURE_CLEAN_INTERVAL, TimeUnit.SECONDS);

                    logger.debug("class=JobFutureHandler||method=run||msg=check running jobs at regular "
                            + "time {}", JOB_FUTURE_CLEAN_INTERVAL);

                    // ??????????????????????????????????????????
                    jobFutureMap.forEach(((jobInfo, future) -> {
                        // job?????????????????????
                        if (future.isDone()) {
                            reorganizeFinishedJob(jobInfo);
                            return;
                        }

                        // ????????????
                        Long timeout = jobInfo.getTimeout();
                        if (timeout <= 0) {
                            return;
                        }

                        Long startTime = jobInfo.getStartTime().getTime();
                        Long now = System.currentTimeMillis();
                        Long between = (now - startTime) / 1000;

                        if (between > timeout && !future.isDone()) {
                            jobInfo.setStatus(JobStatusEnum.CANCELED.getValue());
                            future.cancel(true);
                            timeoutTaskNumber++;
                        }
                    }));
                } catch (Exception e) {
                    logger.error("class=JobFutureHandler||method=run||msg=exception!", e);
                }
            }
        }
    }

    /**
     * ????????????????????????.
     *
     * @param logIJob logIJob
     */
    @Transactional(rollbackFor = Exception.class)
    public void reorganizeFinishedJob(LogIJob logIJob) {
        // ????????????
        jobFutureMap.remove(logIJob);

        execuedJob.put(logIJob.getTaskCode(), logIJob.getTaskCode());

        if (JobStatusEnum.CANCELED.getValue().equals(logIJob.getStatus())) {
            logIJob.setResult(new TaskResult(FAIL_CODE, "task job be canceld!"));
            logIJob.setError("task job be canceld!");
            LogIJobLogPO logIJobLogPO = logIJob.getAuvJobLog();
            logIJobLogMapper.updateByCode(logIJobLogPO);
        }

        // ??????auvJob
        logIJobMapper.deleteByCode(logIJob.getJobCode());

        // ??????????????????
        LogITaskPO logITaskPO = logITaskMapper.selectByCode(logIJob.getTaskCode(), logIJobProperties.getAppName());
        List<LogITask.TaskWorker> taskWorkers = BeanUtil.convertToList(logITaskPO.getTaskWorkerStr(),
                LogITask.TaskWorker.class);

        long currentTime = System.currentTimeMillis();

        if (!CollectionUtils.isEmpty(taskWorkers)) {
            taskWorkers.sort((o1, o2) -> o1.getLastFireTime().after(o2.getLastFireTime()) ? 1 : -1);

            Iterator<LogITask.TaskWorker> iter = taskWorkers.iterator();
            while (iter.hasNext()) {
                LogITask.TaskWorker taskWorker = iter.next();
                if (TaskWorkerStatusEnum.WAITING.getValue().equals(taskWorker.getStatus())
                        && taskWorker.getLastFireTime().getTime() + 12 * ONE_HOUR * 1000 < currentTime) {
                    iter.remove();
                }

                if (Objects.equals(taskWorker.getWorkerCode(), WorkerSingleton.getInstance()
                        .getLogIWorker().getWorkerCode())) {
                    taskWorker.setStatus(TaskWorkerStatusEnum.WAITING.getValue());
                }
            }
        }
        logITaskPO.setTaskWorkerStr(BeanUtil.convertToJson(taskWorkers));
        logITaskMapper.updateTaskWorkStrByCode(logITaskPO);
    }

    /**
     * ???????????????.
     */
    class LockRenewHandler implements Runnable {
        private static final long JOB_INTERVAL = 10L;

        public LockRenewHandler() {
        }

        @Override
        public void run() {
            while (true) {
                try {
                    logger.debug("class=LockRenewHandler||method=run||msg=check need renew lock at "
                            + "regular time {}", JOB_INTERVAL);

                    // ?????????
                    List<LogITaskLockPO> logITaskLockPOS = logITaskLockMapper.selectByWorkerCode(WorkerSingleton
                            .getInstance().getLogIWorker().getWorkerCode(), logIJobProperties.getAppName());

                    if (!CollectionUtils.isEmpty(logITaskLockPOS)) {
                        long current = System.currentTimeMillis() / 1000;

                        for (LogITaskLockPO logITaskLockPO : logITaskLockPOS) {
                            long exTime = (logITaskLockPO.getCreateTime().getTime() / 1000)
                                    + logITaskLockPO.getExpireTime();

                            if (null != execuedJob.getIfPresent(logITaskLockPO.getTaskCode())) {
                                // ??????
                                if (current < exTime && current > exTime - CHECK_BEFORE_INTERVAL) {
                                    logger.info("class=TaskLockServiceImpl||method=run||msg=update lock "
                                                    + "expireTime id={}, expireTime={}", logITaskLockPO.getId(),
                                            logITaskLockPO.getExpireTime());
                                    logITaskLockMapper.update(
                                            logITaskLockPO.getId(),
                                            logITaskLockPO.getExpireTime() + RENEW_INTERVAL);
                                }
                                continue;
                            }

                            // ??????????????????????????????????????????
                            if(current > exTime){
                                logger.info("class=TaskLockServiceImpl||method=run||msg=lock clean "
                                        + "lockInfo={}", BeanUtil.convertToJson(logITaskLockPO));
                                logITaskLockMapper.deleteById(logITaskLockPO.getId());
                            }


                            // ????????????worker????????????
                            LogITaskPO logITaskPO = logITaskMapper
                                    .selectByCode(logITaskLockPO.getTaskCode(), logIJobProperties.getAppName());
                            if (logITaskPO != null) {
                                List<LogITask.TaskWorker> taskWorkers = BeanUtil.convertToList(
                                        logITaskPO.getTaskWorkerStr(), LogITask.TaskWorker.class);

                                if (!CollectionUtils.isEmpty(taskWorkers)) {
                                    for (LogITask.TaskWorker taskWorker : taskWorkers) {
                                        if (Objects.equals(taskWorker.getWorkerCode(), WorkerSingleton.getInstance()
                                                .getLogIWorker().getWorkerCode())) {
                                            taskWorker.setStatus(TaskWorkerStatusEnum.WAITING.getValue());
                                        }
                                    }
                                }
                                logITaskPO.setTaskWorkerStr(BeanUtil.convertToJson(taskWorkers));

                                logger.info("class=TaskLockServiceImpl||method=run||msg=update task workers "
                                        + "status taskInfo={}", BeanUtil.convertToJson(logITaskPO));

                                logITaskMapper.updateTaskWorkStrByCode(logITaskPO);
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.error("class=LockRenewHandler||method=run||msg=exception!", e);
                }

                ThreadUtil.sleep(JOB_INTERVAL, TimeUnit.SECONDS);
            }
        }
    }

    /**
     * ??????????????????.
     */
    class LogCleanHandler implements Runnable {
        // ?????????????????????
        private static final long JOB_LOG_DEL_INTERVAL = 3600L;
        // ??????????????????[????????????7???]
        private Integer logExpire = 7;

        public LogCleanHandler(Integer logExpire) {
            if (logExpire != null) {
                this.logExpire = logExpire;
            }
        }

        @Override
        public void run() {
            while (true) {
                try {
                    // ??????????????????????????????
                    ThreadUtil.sleep( JOB_LOG_DEL_INTERVAL, TimeUnit.SECONDS);

                    logger.info("class=LogCleanHandler||method=run||msg=clean auv_job_log regular"
                            + " time {}", JOB_LOG_DEL_INTERVAL );

                    String    appName    = logIJobProperties.getAppName();
                    Timestamp deleteTime = new Timestamp(System.currentTimeMillis() - logExpire * 24 * 3600 * 1000);

                    int deleteRowTotal    = logIJobLogMapper.selectCountByAppNameAndCreateTime(appName, deleteTime);
                    int deleteRowPerTimes = deleteRowTotal / 60;
                    int deleteRowReal     = 0;

                    for(int i = 0; i < 60; i++){
                        // ????????????
                        int count = logIJobLogMapper.deleteByCreateTime(deleteTime, appName, deleteRowPerTimes);
                        deleteRowReal += count;
                    }

                    logger.info("class=LogCleanHandler||method=run||msg=clean log deleteRowTotal={}, deleteRowReal={}",
                            deleteRowTotal, deleteRowReal);
                } catch (Exception e) {
                    logger.error("class=LogCleanHandler||method=run||msg=exception", e);
                }
            }
        }
    }

    private boolean stopJob(LogIJob logIJob, Future future) {
        int tryTime = 0;
        while (tryTime < TRY_MAX_TIMES) {
            if (future.isDone()) {
                logIJob.setStatus(JobStatusEnum.CANCELED.getValue());
                if (logIJob.getTaskCallback() != null) {
                    logIJob.getTaskCallback().callback(logIJob.getTaskCode());
                }
                reorganizeFinishedJob(logIJob);
                return true;
            }
            future.cancel(true);
            tryTime++;
            ThreadUtil.sleep(STOP_SLEEP_SECONDS, TimeUnit.SECONDS);
        }

        return false;
    }

    private String printStackTraceAsString(Exception e) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        e.printStackTrace(printWriter);
        String error = stringWriter.toString();

        Timestamp timestamp = new Timestamp(System.currentTimeMillis());

        return timestamp.toString() + "  " + error;
    }

    public Long getTimeoutTaskNumber() {
        return timeoutTaskNumber;
    }

}
