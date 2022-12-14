package com.didiglobal.knowframework.job;

import com.didiglobal.knowframework.job.core.job.Job;
import com.didiglobal.knowframework.job.core.job.JobFactory;
import com.didiglobal.knowframework.job.mapper.LogITaskMapper;
import com.didiglobal.knowframework.job.annotation.Task;
import com.didiglobal.knowframework.job.common.enums.TaskStatusEnum;
import com.didiglobal.knowframework.job.common.po.LogITaskPO;
import com.didiglobal.knowframework.job.utils.CronExpression;
import com.didiglobal.knowframework.job.utils.IdWorker;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.didiglobal.knowframework.log.ILog;
import com.didiglobal.knowframework.log.LogFactory;
import org.apache.commons.lang3.StringUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class TaskBeanPostProcessor implements BeanPostProcessor {

    private static final ILog logger     = LogFactory.getLog(TaskBeanPostProcessor.class);

    private static Map<String, LogITaskPO> taskMap = new HashMap<>();

    @Autowired
    private LogITaskMapper logITaskMapper;

    @Autowired
    private JobFactory jobFactory;

    @Autowired
    private LogIJobProperties logIJobProperties;

    @PostConstruct
    public void init(){
        logger.info("class=TaskBeanPostProcessor||method=init");
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        try {
            if(!logIJobProperties.getEnable()){
                return bean;
            }

            Class<?> beanClass = AopUtils.getTargetClass(bean);
            // add job to jobFactory
            if (bean instanceof Job) {
                jobFactory.addJob(beanClass.getCanonicalName(), (Job) bean);
            } else {
                return bean;
            }

            // check and register to db

            Task taskAnnotation = beanClass.getAnnotation(Task.class);
            if (taskAnnotation == null || !taskAnnotation.autoRegister()) {
                return bean;
            }
            // check
            if (!check(taskAnnotation)) {
                logger.error("class=TaskBeanPostProcessor||method=blacklist||url=||msg=invalid schedule {}",
                        taskAnnotation.toString());
            }

            if(!contains(beanClass.getCanonicalName())){
                LogITaskPO task = getNewLogTask(beanClass, taskAnnotation);
                task.setTaskCode(IdWorker.getIdStr());
                task.setStatus(TaskStatusEnum.RUNNING.getValue());
                task.setNodeNameWhiteListStr(StringUtils.EMPTY);
                logITaskMapper.insert(task);
            }else {
                LogITaskPO task = taskMap.get(beanClass.getCanonicalName());
                task = updateLogTask(task, beanClass, taskAnnotation);
                logITaskMapper.updateByCode(task);
            }
        }catch (Exception e){
            logger.error("class=TaskBeanPostProcessor||method=postProcessAfterInitialization||beanName={}||msg=exception",
                    beanName, e);
        }

        return bean;
    }

    /*********************************************** private method ***********************************************/
    private boolean check(Task schedule) {
        return CronExpression.isValidExpression(schedule.cron());
    }

    private LogITaskPO getNewLogTask(Class<?> beanClass, Task schedule) {
        LogITaskPO logITaskPO = new LogITaskPO();
        logITaskPO.setTaskName(schedule.name());
        logITaskPO.setTaskDesc(schedule.description());
        logITaskPO.setCron(schedule.cron());
        logITaskPO.setClassName(beanClass.getCanonicalName());
        logITaskPO.setParams("");
        logITaskPO.setRetryTimes(schedule.retryTimes());
        logITaskPO.setLastFireTime(new Timestamp(System.currentTimeMillis()));
        logITaskPO.setTimeout(schedule.timeout());
        logITaskPO.setSubTaskCodes("");
        logITaskPO.setConsensual(schedule.consensual().name());
        logITaskPO.setTaskWorkerStr("");
        logITaskPO.setAppName(logIJobProperties.getAppName());
        logITaskPO.setOwner(schedule.owner());
        return logITaskPO;
    }

    private LogITaskPO updateLogTask(LogITaskPO logITaskPO, Class<?> beanClass, Task schedule){
        logITaskPO.setTaskName(schedule.name());
        logITaskPO.setTaskDesc(schedule.description());
        logITaskPO.setCron(schedule.cron());
        logITaskPO.setClassName(beanClass.getCanonicalName());
        logITaskPO.setParams("");
        logITaskPO.setRetryTimes(schedule.retryTimes());
        logITaskPO.setTimeout(schedule.timeout());
        logITaskPO.setConsensual(schedule.consensual().name());
        logITaskPO.setAppName(logIJobProperties.getAppName());
        logITaskPO.setOwner(schedule.owner());
        return logITaskPO;
    }

    private boolean contains(String className) {
        if (taskMap.isEmpty()) {
            List<LogITaskPO> logITaskPOS = logITaskMapper.selectByAppName(logIJobProperties.getAppName());
            taskMap = logITaskPOS.stream().collect(Collectors.toMap(LogITaskPO::getClassName,
                    Function.identity()));
        }
        return taskMap.containsKey(className);
    }
}