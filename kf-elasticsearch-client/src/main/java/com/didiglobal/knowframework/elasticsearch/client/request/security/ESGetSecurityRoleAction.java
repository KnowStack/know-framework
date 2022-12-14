package com.didiglobal.knowframework.elasticsearch.client.request.security;

import com.didiglobal.knowframework.elasticsearch.client.response.security.ESGetSecurityRoleResponse;
import org.elasticsearch.action.Action;
import org.elasticsearch.client.ElasticsearchClient;

/**
 * author weizijun
 * date：2019-07-11
 */
public class ESGetSecurityRoleAction extends Action<ESGetSecurityRoleRequest, ESGetSecurityRoleResponse, ESGetSecurityRoleRequestBuilder> {
    public static final ESGetSecurityRoleAction INSTANCE = new ESGetSecurityRoleAction();
    public static final String NAME = "cluster:admin/security/role/get";

    private ESGetSecurityRoleAction() {
        super(NAME);
    }

    @Override
    public ESGetSecurityRoleResponse newResponse() {
        return new ESGetSecurityRoleResponse();
    }

    @Override
    public ESGetSecurityRoleRequestBuilder newRequestBuilder(ElasticsearchClient client) {
        return new ESGetSecurityRoleRequestBuilder(client, this);
    }
}
