/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.didiglobal.knowframework.elasticsearch.client.request.cluster.health;

import com.didiglobal.knowframework.elasticsearch.client.request.index.stats.IndicesStatsLevel;
import com.didiglobal.knowframework.elasticsearch.client.response.cluster.ESClusterHealthResponse;
import org.elasticsearch.action.ActionRequestBuilder;
import org.elasticsearch.client.ElasticsearchClient;

public class ESClusterHealthRequestBuilder extends ActionRequestBuilder<ESClusterHealthRequest, ESClusterHealthResponse, ESClusterHealthRequestBuilder> {
    public ESClusterHealthRequestBuilder(ElasticsearchClient client, ESClusterHealthAction action) {
        super(client, action, new ESClusterHealthRequest());
    }

    public ESClusterHealthRequestBuilder setLevel(IndicesStatsLevel level) {
        request.setLevel(level);
        return this;
    }
}
