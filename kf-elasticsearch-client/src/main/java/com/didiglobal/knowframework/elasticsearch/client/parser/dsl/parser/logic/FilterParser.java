package com.didiglobal.knowframework.elasticsearch.client.parser.dsl.parser.logic;

import com.alibaba.fastjson.JSON;
import com.didiglobal.knowframework.elasticsearch.client.parser.dsl.ast.common.KeyWord;
import com.didiglobal.knowframework.elasticsearch.client.parser.dsl.parser.DslParser;
import com.didiglobal.knowframework.elasticsearch.client.parser.dsl.parser.ParserType;
import com.didiglobal.knowframework.elasticsearch.client.parser.dsl.ast.common.logic.Filter;
import com.didiglobal.knowframework.elasticsearch.client.parser.dsl.ast.common.multi.NodeList;

public class FilterParser extends DslParser {

    public FilterParser(ParserType type) {
        super(type);
    }

    @Override
    public KeyWord parse(String name, Object obj) throws Exception {
        Filter node = new Filter(name);
        node.n = NodeList.toNodeList(parserType, (JSON) obj, true);
        return node;
    }
}
