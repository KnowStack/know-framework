package com.didiglobal.knowframework.job.core.consensual;

import com.alibaba.fastjson.JSON;
import com.didiglobal.knowframework.job.core.WorkerSingleton;
import com.didiglobal.knowframework.job.LogIJobProperties;
import com.didiglobal.knowframework.job.common.domain.LogIWorker;
import com.didiglobal.knowframework.job.common.po.LogIWorkerBlacklistPO;
import com.didiglobal.knowframework.job.common.domain.LogITask;
import com.didiglobal.knowframework.job.mapper.LogIWorkerBlacklistMapper;
import com.didiglobal.knowframework.log.ILog;
import com.didiglobal.knowframework.log.LogFactory;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 随机算法.
 *
 * @author ds
 */
@Service
public abstract class AbstractConsensual implements Consensual {

    private static final ILog logger     = LogFactory.getLog(AbstractConsensual.class);

    @Autowired
    private LogIWorkerBlacklistMapper logIWorkerBlacklistMapper;

    private static final String BLACKLIST_KEY = "BlacklistKey";

    private Cache<Object, Set<String>> blacklistCache = CacheBuilder.newBuilder()
            .expireAfterWrite(2, TimeUnit.MINUTES).build();

    @Override
    public boolean canClaim(LogITask logITask, LogIJobProperties logIJobProperties) {
        if (inBlacklist()) {
            return false;
        }
        if (!inWhiteList(logITask, logIJobProperties)) return false;
        return tryClaim(logITask);
    }

    /**
     *
     * logITask 是否关联可执行机器组，如有，则须检查当前 worker 是否在关联的机器组中，如不是，则过滤掉
     * @param logITask 任务实例
     * @param logIJobProperties 任务配置
     * @return true：当前执行节点在白名单 false：当前执行节点不在白名单
     */
    private boolean inWhiteList(LogITask logITask, LogIJobProperties logIJobProperties) {
        String nodeNameWhiteListString = logITask.getNodeNameWhiteListStr();
        if(StringUtils.isNotBlank(nodeNameWhiteListString)) {
            String nodeName = logIJobProperties.getNodeName();
            Set<String> nodeNameWhiteSet = JSON.parseObject(nodeNameWhiteListString, Set.class);
            if(!nodeNameWhiteSet.contains(nodeName)) {
                if(logger.isInfoEnabled()) {
                    logger.info(
                            String.format(
                                    "class=AbstractConsensual||method=canClaim||msg=task execute skip this node, because this node %s not in this task's nodeNameWhiteList %s, task info is %s",
                                    nodeName,
                                    nodeNameWhiteListString,
                                    JSON.toJSONString(logITask)
                            )
                    );
                }
                return false;
            }
        }
        return true;
    }

    public abstract boolean tryClaim(LogITask logITask);

    //###################################### private ################################################

    private boolean inBlacklist() {
        Set<String> blacklist = blacklist();
        LogIWorker logIWorker = WorkerSingleton.getInstance().getLogIWorker();
        return blacklist.contains(logIWorker.getWorkerCode());
    }

    private Set<String> blacklist() {
        Set<String> blacklist = new HashSet<>();
        try {
            blacklist = blacklistCache.get(BLACKLIST_KEY, () -> {
                List<LogIWorkerBlacklistPO> logIWorkerBlacklistPOS = logIWorkerBlacklistMapper.selectAll();
                return logIWorkerBlacklistPOS.stream().map(LogIWorkerBlacklistPO::getWorkerCode)
                        .collect(Collectors.toSet());
            });
        } catch (ExecutionException e) {
            logger.error("class=AbstractConsensual||method=blacklist||url=||msg=", e);
        }
        return blacklist;
    }
}
