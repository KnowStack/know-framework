package com.didiglobal.logi.auvjob.core;

import com.didiglobal.logi.auvjob.common.bean.TaskInfo;

/**
 *
 * 随机算法
 * @author dengshan
 */
public class RandomConsensual implements Consensual {

  private NodeManager nodeManager;

  @Override
  public boolean canClaim(TaskInfo taskInfo) {

    return true;
  }
}
