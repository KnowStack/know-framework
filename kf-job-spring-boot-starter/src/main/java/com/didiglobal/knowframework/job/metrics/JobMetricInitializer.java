package com.didiglobal.knowframework.job.metrics;

import com.didiglobal.knowframework.job.core.job.JobExecutor;
import com.didiglobal.knowframework.job.core.job.JobManager;
import com.didiglobal.knowframework.observability.conponent.metrics.BaseMetricInitializer;
import com.didiglobal.knowframework.observability.conponent.metrics.Meter;
import com.didiglobal.knowframework.observability.conponent.metrics.Metric;
import com.didiglobal.knowframework.observability.conponent.metrics.MetricUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import javax.annotation.PostConstruct;

@Component
public class JobMetricInitializer extends BaseMetricInitializer {

    @Autowired
    private JobExecutor jobExecutor;

    @Autowired
    private JobManager jobManager;

    @PostConstruct
    public void register() {

        /*
         * logi-job metrics
         */
        super.registerMetric(
                "job.rejected.task.number",
                "进程启动以来，因线程池队列满导致待执行的定时任务提交时被拒绝的数量。单位：个，类型：Double",
                MetricUnit.METRIC_UNIT_NUMBER,
                new Meter() {
                    @Override
                    public Metric getMetric() {
                        return new Metric(jobExecutor.getRejectedTaskNumber().doubleValue());
                    }
                }
        );

        super.registerMetric(
                "job.thread.pool.queue.size",
                "执行定时任务的线程池的当前队列大小。单位：个，类型：Double",
                MetricUnit.METRIC_UNIT_NUMBER,
                new Meter() {
                    @Override
                    public Metric getMetric() {
                        return new Metric(jobExecutor.getThreadPoolQueueSize().doubleValue());
                    }
                }

        );

        super.registerMetric(
                "job.thread.pool.thread.size.max",
                "执行定时任务的线程池的最大线程数。单位：个，类型：Double",
                MetricUnit.METRIC_UNIT_NUMBER,
                new Meter() {
                    @Override
                    public Metric getMetric() {
                        return new Metric(jobExecutor.getThreadPoolThreadMaxSize().doubleValue());
                    }
                }
        );

        super.registerMetric(
                "job.thread.pool.task.running.size",
                "正在执行的任务数。单位：个，类型：Double",
                MetricUnit.METRIC_UNIT_NUMBER,
                new Meter() {
                    @Override
                    public Metric getMetric() {
                        return new Metric(jobManager.runningJobSize().doubleValue());
                    }
                }
        );

        super.registerMetric(
                "job.thread.pool.task.timeout.number",
                "进程启动以来执行超时的任务数。单位：个，类型：Double",
                MetricUnit.METRIC_UNIT_NUMBER,
                new Meter() {
                    @Override
                    public Metric getMetric() {
                        return new Metric(jobManager.getTimeoutTaskNumber().doubleValue());
                    }
                }
        );

    }

}

