package com.didiglobal.knowframework.elasticsearch.client.parser.dsl.ast.aggr;

import com.didiglobal.knowframework.elasticsearch.client.parser.dsl.ast.common.KeyWord;
import com.didiglobal.knowframework.elasticsearch.client.parser.dsl.ast.common.multi.NodeMap;
import com.didiglobal.knowframework.elasticsearch.client.parser.dsl.visitor.basic.Visitor;

public class Avg extends KeyWord {

    public NodeMap m = new NodeMap();

    public Avg(String name) {
        super(name);
    }

    @Override
    public void accept(Visitor vistor) {
        vistor.visit(this);
    }


}
