package com.didichuxing.datachannel.arius.dsl.common.dsl.parser.aggr;

import com.alibaba.fastjson.JSONObject;
import com.didichuxing.datachannel.arius.dsl.common.dsl.ast.aggr.AvgBucket;
import com.didichuxing.datachannel.arius.dsl.common.dsl.ast.common.KeyWord;
import com.didichuxing.datachannel.arius.dsl.common.dsl.ast.common.multi.NodeMap;
import com.didichuxing.datachannel.arius.dsl.common.dsl.parser.DslParser;
import com.didichuxing.datachannel.arius.dsl.common.dsl.parser.ParserType;
import com.didichuxing.datachannel.arius.dsl.common.dsl.util.ConstValue;

public class AvgBucketParser extends DslParser {

    public AvgBucketParser(ParserType type) {
        super(type);
    }

    @Override
    public KeyWord parse(String name, Object obj) throws Exception {
        AvgBucket node = new AvgBucket(name);
        NodeMap.toString2ValueWithField(parserType, (JSONObject) obj, node.m, ConstValue.FIELD);
        return node;
    }
}