package com.didichuxing.datachannel.arius.dsl.common.dsl.ast.root;

import com.didichuxing.datachannel.arius.dsl.common.dsl.ast.common.KeyWord;
import com.didichuxing.datachannel.arius.dsl.common.dsl.ast.common.Node;
import com.didichuxing.datachannel.arius.dsl.common.dsl.visitor.basic.Visitor;

/**
 * @author D10865
 * 存储scroll_id关键字  或 scrollId关键字的结果
 * {"scroll":"60s","scroll_id":"cXVlcnlBbmRGZXRjaDsxOzEyNjEzOTpXYW9YS2dlQVM1YU9hZFJXVFNZa2x3OzA7"}
 * {"scroll":"1m","scrollId":"cXVlcnlUaGVuRmV0Y2g7ODsyMjcwNTg5MzoyWF"}
 */
public class ScrollId extends KeyWord {
    public Node n;

    public ScrollId(String name) {
        super(name);
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}