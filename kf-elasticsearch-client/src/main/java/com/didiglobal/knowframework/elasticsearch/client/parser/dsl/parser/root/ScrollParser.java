package com.didiglobal.knowframework.elasticsearch.client.parser.dsl.parser.root;

import com.didiglobal.knowframework.elasticsearch.client.parser.dsl.ast.common.KeyWord;
import com.didiglobal.knowframework.elasticsearch.client.parser.dsl.ast.root.Scroll;
import com.didiglobal.knowframework.elasticsearch.client.parser.dsl.parser.DslParser;
import com.didiglobal.knowframework.elasticsearch.client.parser.dsl.parser.ParserType;
import com.didiglobal.knowframework.elasticsearch.client.parser.dsl.ast.common.value.ObjectNode;

/**
 * 解析scroll关键字
 * {"scroll":"60s","scroll_id":"cXVlcnlBbmRGZXRjaDsxOzEyNjEzOTpXYW9YS2dlQVM1YU9hZFJXVFNZa2x3OzA7"}
 */
public class ScrollParser extends DslParser {

    public ScrollParser(ParserType type) {
        super(type);
    }

    @Override
    public KeyWord parse(String name, Object obj) throws Exception {
        Scroll node = new Scroll(name);
        node.n = new ObjectNode(obj);

        return node;
    }

}
