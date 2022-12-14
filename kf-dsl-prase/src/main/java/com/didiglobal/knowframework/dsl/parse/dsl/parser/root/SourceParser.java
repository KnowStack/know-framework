package com.didiglobal.knowframework.dsl.parse.dsl.parser.root;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.didiglobal.knowframework.dsl.parse.dsl.ast.common.KeyWord;
import com.didiglobal.knowframework.dsl.parse.dsl.ast.root.Source;
import com.didiglobal.knowframework.dsl.parse.dsl.ast.common.key.StringNode;
import com.didiglobal.knowframework.dsl.parse.dsl.ast.common.multi.NodeList;
import com.didiglobal.knowframework.dsl.parse.dsl.ast.common.multi.NodeMap;
import com.didiglobal.knowframework.dsl.parse.dsl.ast.common.value.ObjectNode;
import com.didiglobal.knowframework.dsl.parse.dsl.ast.common.key.FieldNode;
import com.didiglobal.knowframework.dsl.parse.dsl.parser.DslParser;
import com.didiglobal.knowframework.dsl.parse.dsl.parser.ParserType;

public class SourceParser extends DslParser {

    public static final String INCLUDES_STR = "includes";
    public static final String EXCLUDES_STR = "excludes";

    public SourceParser(ParserType type) {
        super(type);
    }


    @Override
    public KeyWord parse(String name, Object root) throws Exception {
        Source node = new Source(name);

        if(root instanceof Boolean) {
            node.n = new ObjectNode(root);
            return node;
        }

        if(!(root instanceof JSONObject) && !(root instanceof JSONArray)) {
            node.n = new FieldNode(root);
            return node;
        }

        if(root instanceof  JSONArray) {
            node.n = new NodeList();
            NodeList.toFieldList((JSONArray) root, (NodeList) node.n);
            return node;
        }

        if(root instanceof  JSONObject) {
            JSONObject jsonObject = (JSONObject) root;
            NodeMap nm = new NodeMap();

            process("include", jsonObject, nm);
            process("includes", jsonObject, nm);
            process("exclude", jsonObject, nm);
            process("excludes", jsonObject, nm);

            node.n = nm;
        }

        return node;
    }


    private void process(String key, JSONObject root, NodeMap nm) throws Exception {
        if (root.containsKey(key)) {
            Object o = root.get(key);
            if (o instanceof JSONArray) {
                NodeList nl = new NodeList();
                NodeList.toFieldList((JSONArray) o, nl);
                nm.m.put(new StringNode(key), nl);

            } else {
                nm.m.put(new StringNode(key), new FieldNode(o));
            }
        }
    }
}
