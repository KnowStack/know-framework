package com.didiglobal.knowframework.elasticsearch.client.parser.dsl.parser.root;

import com.didiglobal.knowframework.elasticsearch.client.parser.dsl.ast.common.KeyWord;
import com.didiglobal.knowframework.elasticsearch.client.parser.dsl.ast.root.Version;
import com.didiglobal.knowframework.elasticsearch.client.parser.dsl.parser.DslParser;
import com.didiglobal.knowframework.elasticsearch.client.parser.dsl.parser.ParserType;
import com.didiglobal.knowframework.elasticsearch.client.parser.dsl.ast.common.value.ObjectNode;

public class VersionParser extends DslParser {
    public VersionParser(ParserType type) {
        super(type);
    }

    @Override
    public KeyWord parse(String name, Object obj) {
        Version node = new Version(name);
        node.v = new ObjectNode(obj);
        return node;
    }
}
