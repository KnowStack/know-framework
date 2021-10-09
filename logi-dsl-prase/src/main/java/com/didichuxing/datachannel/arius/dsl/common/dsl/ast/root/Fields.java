package com.didichuxing.datachannel.arius.dsl.common.dsl.ast.root;

import com.didichuxing.datachannel.arius.dsl.common.dsl.ast.common.KeyWord;
import com.didichuxing.datachannel.arius.dsl.common.dsl.ast.common.Node;
import com.didichuxing.datachannel.arius.dsl.common.dsl.visitor.basic.Visitor;

/**
 * @author D10865
 */
public class Fields extends KeyWord {
    public Node n;

    public Fields(String name) {
        super(name);
    }

    @Override
    public void accept(Visitor vistor) {
        vistor.visit(this);
    }
}