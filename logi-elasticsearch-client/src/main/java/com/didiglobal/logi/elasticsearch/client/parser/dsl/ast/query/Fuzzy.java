package com.didiglobal.logi.elasticsearch.client.parser.dsl.ast.query;

import com.didiglobal.logi.elasticsearch.client.parser.dsl.ast.common.KeyWord;
import com.didiglobal.logi.elasticsearch.client.parser.dsl.ast.common.Node;
import com.didiglobal.logi.elasticsearch.client.parser.dsl.visitor.basic.Visitor;


public class Fuzzy extends KeyWord {
    public Node n;

    public Fuzzy(String name) {
        super(name);
    }

    @Override
    public void accept(Visitor vistor) {
        vistor.visit(this);
    }
}