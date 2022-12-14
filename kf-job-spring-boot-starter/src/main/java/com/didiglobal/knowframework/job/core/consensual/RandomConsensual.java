package com.didiglobal.knowframework.job.core.consensual;

import com.didiglobal.knowframework.job.common.domain.LogITask;
import com.didiglobal.knowframework.job.core.task.TaskLockService;
import com.didiglobal.knowframework.log.ILog;
import com.didiglobal.knowframework.log.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 随机算法.
 *
 * @author ds
 */
@Service
public class RandomConsensual extends AbstractConsensual {
    private static final ILog logger     = LogFactory.getLog(RandomConsensual.class);

    @Autowired
    private TaskLockService taskLockService;

    @Override
    public String getName() {
        return ConsensualEnum.RANDOM.name();
    }

    @Override
    public boolean tryClaim(LogITask logITask) {
        if (taskLockService.tryAcquire(logITask.getTaskCode())) {
            logITask.setTaskCallback(taskCode -> {
                logger.info("class=RandomConsensual||method=tryClaim||msg=release task lock "
                        + "taskCode {}", taskCode);
                taskLockService.tryRelease(taskCode);
            });
            return true;
        }
        return false;
    }
}
