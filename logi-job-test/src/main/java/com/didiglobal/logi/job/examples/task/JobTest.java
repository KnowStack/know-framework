package com.didiglobal.logi.job.examples.task;

import com.didiglobal.logi.job.annotation.Task;
import com.didiglobal.logi.job.core.job.Job;
import com.didiglobal.logi.job.core.job.JobContext;
import com.didiglobal.logi.job.utils.ThreadUtil;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@Task(name = "cc", description = "hello cc", cron = "0 /5 * * * ? ", autoRegister = true)
public class JobTest implements Job {
  private static final Logger logger = LoggerFactory.getLogger(JobTest.class);

  @Override
  public Object execute(JobContext jobContext) {
    for (int i = 0; i < 20; i++) {
      ThreadUtil.sleep(1, TimeUnit.SECONDS);
      logger.info("hihi");
      System.out.println("hello world");
    }
    return null;
  }

}