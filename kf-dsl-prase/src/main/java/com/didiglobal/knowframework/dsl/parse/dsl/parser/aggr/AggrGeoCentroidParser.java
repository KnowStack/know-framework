package com.didiglobal.knowframework.dsl.parse.dsl.parser.aggr;

import com.alibaba.fastjson.JSONObject;
import com.didiglobal.knowframework.dsl.parse.dsl.ast.aggr.AggrGeoCentroid;
import com.didiglobal.knowframework.dsl.parse.dsl.ast.common.KeyWord;
import com.didiglobal.knowframework.dsl.parse.dsl.ast.common.multi.NodeMap;
import com.didiglobal.knowframework.dsl.parse.dsl.parser.DslParser;
import com.didiglobal.knowframework.dsl.parse.dsl.parser.ParserType;
import com.didiglobal.knowframework.dsl.parse.dsl.util.ConstValue;

public class AggrGeoCentroidParser extends DslParser {

    public AggrGeoCentroidParser(ParserType type) {
        super(type);
    }

    @Override
    public KeyWord parse(String name, Object obj) throws Exception {
        AggrGeoCentroid node = new AggrGeoCentroid(name);
        NodeMap nm = new NodeMap();

        NodeMap.toString2ValueWithField(parserType, (JSONObject) obj, nm, ConstValue.FIELD);

        node.n = nm;
        return node;
    }
}
