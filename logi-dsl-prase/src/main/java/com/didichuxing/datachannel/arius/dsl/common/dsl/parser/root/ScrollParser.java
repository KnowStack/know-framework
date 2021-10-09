package com.didichuxing.datachannel.arius.dsl.common.dsl.parser.root;

import com.didichuxing.datachannel.arius.dsl.common.dsl.ast.common.KeyWord;
import com.didichuxing.datachannel.arius.dsl.common.dsl.ast.common.value.ObjectNode;
import com.didichuxing.datachannel.arius.dsl.common.dsl.ast.root.Scroll;
import com.didichuxing.datachannel.arius.dsl.common.dsl.parser.DslParser;
import com.didichuxing.datachannel.arius.dsl.common.dsl.parser.ParserType;

/**
 * @author D10865
 *
 * 解析scroll关键字
 *
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