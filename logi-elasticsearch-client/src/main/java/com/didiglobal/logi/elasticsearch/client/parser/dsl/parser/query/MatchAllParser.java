package com.didiglobal.logi.elasticsearch.client.parser.dsl.parser.query;

import com.didiglobal.logi.elasticsearch.client.parser.dsl.ast.common.KeyWord;
import com.didiglobal.logi.elasticsearch.client.parser.dsl.ast.common.value.ValueNode;
import com.didiglobal.logi.elasticsearch.client.parser.dsl.ast.query.MatchAll;
import com.didiglobal.logi.elasticsearch.client.parser.dsl.parser.DslParser;
import com.didiglobal.logi.elasticsearch.client.parser.dsl.parser.ParserType;

public class MatchAllParser extends DslParser {
    public MatchAllParser(ParserType type) {
        super(type);
    }

    @Override
    public KeyWord parse(String name, Object root) throws Exception {
        MatchAll node = new MatchAll(name);
        node.n = ValueNode.getValueNode(root);
        return node;
    }
}