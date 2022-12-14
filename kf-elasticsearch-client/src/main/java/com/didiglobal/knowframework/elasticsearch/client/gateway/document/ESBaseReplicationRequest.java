package com.didiglobal.knowframework.elasticsearch.client.gateway.document;

import com.didiglobal.knowframework.elasticsearch.client.model.ESActionRequest;
import org.elasticsearch.common.unit.TimeValue;

public abstract class ESBaseReplicationRequest<T extends ESBaseReplicationRequest<T>> extends ESActionRequest<T> {
    protected TimeValue timeout;

    protected String index;

    protected String waitForActiveShards;

    protected String consistencyLevel;

    protected String refresh;

    /**
     * A timeout to wait if the index operation can't be performed immediately. Defaults to 1m.
     */
    @SuppressWarnings("unchecked")
    public final T timeout(TimeValue timeout) {
        this.timeout = timeout;
        return (T) this;
    }

    /**
     * A timeout to wait if the index operation can't be performed immediately. Defaults to 1m.
     */
    public final T timeout(String timeout) {
        return timeout(TimeValue.parseTimeValue(timeout, null, getClass().getSimpleName() + ".timeout"));
    }

    public TimeValue timeout() {
        return timeout;
    }

    public String index() {
        return this.index;
    }

    @SuppressWarnings("unchecked")
    public final T index(String index) {
        this.index = index;
        return (T) this;
    }

    public String getIndex() {
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
    }

    public String getWaitForActiveShards() {
        return waitForActiveShards;
    }

    public void setWaitForActiveShards(String waitForActiveShards) {
        this.waitForActiveShards = waitForActiveShards;
    }

    public String getConsistencyLevel() {
        return consistencyLevel;
    }

    public void setConsistencyLevel(String consistencyLevel) {
        this.consistencyLevel = consistencyLevel;
    }

    public String getRefresh() {
        return refresh;
    }

    public void setRefresh(String refresh) {
        this.refresh = refresh;
    }
}
