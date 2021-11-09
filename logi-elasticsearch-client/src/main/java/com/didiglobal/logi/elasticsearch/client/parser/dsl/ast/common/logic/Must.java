package com.didiglobal.logi.elasticsearch.client.parser.dsl.ast.common.logic;

import com.didiglobal.logi.elasticsearch.client.parser.dsl.ast.common.KeyWord;
import com.didiglobal.logi.elasticsearch.client.parser.dsl.ast.common.Node;
import com.didiglobal.logi.elasticsearch.client.parser.dsl.visitor.basic.Visitor;

public class Must extends KeyWord {
    public Node n;

    public Must(String name) {
        super(name);
    }

    @Override
    public void accept(Visitor vistor) {
        vistor.visit(this);
    }
}