package com.didiglobal.knowframework.elasticsearch.client.parser.dsl.ast.common.logic;

import com.didiglobal.knowframework.elasticsearch.client.parser.dsl.ast.common.KeyWord;
import com.didiglobal.knowframework.elasticsearch.client.parser.dsl.ast.common.Node;
import com.didiglobal.knowframework.elasticsearch.client.parser.dsl.visitor.basic.Visitor;

public class And extends KeyWord {
    public Node n;

    public And(String name) {
        super(name);
    }

    @Override
    public void accept(Visitor vistor) {
        vistor.visit(this);
    }
}
