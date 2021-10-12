package com.didiglobal.logi.dsl.parse.dsl.ast.root;

import com.didiglobal.logi.dsl.parse.dsl.ast.common.KeyWord;
import com.didiglobal.logi.dsl.parse.dsl.ast.common.Node;
import com.didiglobal.logi.dsl.parse.dsl.visitor.basic.Visitor;

/**
 * @author D10865
 *
 * * 存储ignore_unavailable关键字的结果
 *  {"index":["arius_dsl_log_2018-09-20"],"ignore_unavailable":true}
 */
public class IgnoreUnavailable extends KeyWord {
    public Node n;

    public IgnoreUnavailable(String name) {
        super(name);
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}