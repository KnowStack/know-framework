package com.didiglobal.knowframework.dsl.parse.dsl.ast;

import com.didiglobal.knowframework.dsl.parse.dsl.ast.common.Node;
import com.didiglobal.knowframework.dsl.parse.dsl.ast.common.multi.NodeMap;
import com.didiglobal.knowframework.dsl.parse.dsl.visitor.basic.Visitor;

public class DslNode extends Node {
    public NodeMap m = new NodeMap();

    @Override
    public void accept(Visitor vistor) {
        vistor.visit(this);
    }

}
