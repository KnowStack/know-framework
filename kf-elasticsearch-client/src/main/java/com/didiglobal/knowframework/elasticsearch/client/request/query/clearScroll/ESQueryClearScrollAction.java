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

package com.didiglobal.knowframework.elasticsearch.client.request.query.clearScroll;

import com.didiglobal.knowframework.elasticsearch.client.response.query.clearScroll.ESQueryClearScrollResponse;
import org.elasticsearch.action.Action;
import org.elasticsearch.client.ElasticsearchClient;

/**
 *
 */
public class ESQueryClearScrollAction extends Action<ESQueryClearScrollRequest, ESQueryClearScrollResponse, ESQueryClearScrollRequestBuilder> {

    public static final ESQueryClearScrollAction INSTANCE = new ESQueryClearScrollAction();
    public static final String NAME = "query:clear/scroll";

    private ESQueryClearScrollAction() {
        super(NAME);
    }

    @Override
    public ESQueryClearScrollResponse newResponse() {
        return new ESQueryClearScrollResponse();
    }

    @Override
    public ESQueryClearScrollRequestBuilder newRequestBuilder(ElasticsearchClient client) {
        return new ESQueryClearScrollRequestBuilder(client, this);
    }
}
