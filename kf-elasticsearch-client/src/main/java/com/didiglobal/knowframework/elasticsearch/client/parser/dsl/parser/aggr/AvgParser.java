package com.didiglobal.knowframework.elasticsearch.client.parser.dsl.parser.aggr;

import com.alibaba.fastjson.JSONObject;
import com.didiglobal.knowframework.elasticsearch.client.parser.dsl.ast.aggr.Avg;
import com.didiglobal.knowframework.elasticsearch.client.parser.dsl.ast.common.KeyWord;
import com.didiglobal.knowframework.elasticsearch.client.parser.dsl.parser.DslParser;
import com.didiglobal.knowframework.elasticsearch.client.parser.dsl.parser.ParserType;
import com.didiglobal.knowframework.elasticsearch.client.parser.dsl.util.ConstValue;
import com.didiglobal.knowframework.elasticsearch.client.parser.dsl.ast.common.multi.NodeMap;

public class AvgParser extends DslParser {

    public AvgParser(ParserType type) {
        super(type);
    }

    @Override
    public KeyWord parse(String name, Object obj) throws Exception {
        Avg node = new Avg(name);
        NodeMap.toString2ValueWithField(parserType, (JSONObject) obj, node.m, ConstValue.FIELD);
        return node;
    }
}
