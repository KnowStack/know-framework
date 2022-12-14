package com.didiglobal.knowframework.elasticsearch.client.utils;

import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson.JSONException;
import org.apache.commons.lang3.StringUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

public class JsonUtils {

    public static Map<String, String> flat(JSONObject obj) {
        Map<String, String> ret = new HashMap<>();

        if (obj == null) {
            return ret;
        }

        for (String key : obj.keySet()) {
            Object o = obj.get(key);

            if (o instanceof JSONObject) {
                Map<String, String> m = flat((JSONObject) o);
                for (String k : m.keySet()) {
                    ret.put(key.replaceAll("\\.", "#") + "." + k, m.get(k));
                }
            } else {
                if (null == o) {
                    ret.put(key.replaceAll("\\.", "#"), null);
                } else {
                    ret.put(key.replaceAll("\\.", "#"), o.toString());
                }

            }
        }

        return ret;
    }

    public static Map<String, Object> flatObject(JSONObject obj) {
        Map<String, Object> ret = new HashMap<>();

        if (obj == null) {
            return ret;
        }

        for (String key : obj.keySet()) {
            Object o = obj.get(key);

            if (o instanceof JSONObject) {
                Map<String, Object> m = flatObject((JSONObject) o);
                for (String k : m.keySet()) {
                    ret.put(key.replaceAll("\\.", "#") + "." + k, m.get(k));
                }
            } else {
                ret.put(key.replaceAll("\\.", "#"), o);
            }
        }

        return ret;
    }

    public static JSONObject reFlat(Map<String, String> m) {
        JSONObject ret = new JSONObject();
        for (String key : m.keySet()) {
            // 增加一个value
            String[] subKeys = key.split("\\.");
            for (int i = 0; i < subKeys.length; i++) {
                subKeys[i] = subKeys[i].replace("#", ".");
            }

            JSONObject obj = ret;
            int i;
            for (i = 0; i < subKeys.length - 1; i++) {
                String subKey = subKeys[i];

                // 逐层增加
                if (!obj.containsKey(subKey)) {
                    obj.put(subKey, new JSONObject());
                }

                obj = obj.getJSONObject(subKey);
            }

            String value = m.get(key);
            if (value != null && value.startsWith("[") && value.endsWith("]")) {
                // 看是否可以转化成jsonArray
                //这里应该进行try catch否则会导致解析json中的正则失败
                JSONArray array = null;
                try {
                    array = JSONArray.parseArray(value);
                } catch (JSONException e) {
                    //pass
                }
                if (array == null) {
                    obj.put(subKeys[i], value);
                } else {
                    obj.put(subKeys[i], array);
                }

            } else {
                obj.put(subKeys[i], value);
            }
        }

        return ret;
    }


    /**
     * 将一维的kv对转化为嵌套的kv对
     * 例如:
     * a.b.c:"test"
     * <p>
     * 转化为:
     * <p>
     * {
     * "a": {
     * "b": {
     * "c": "test"
     * }
     * }
     * }
     *
     * @param map map
     * @return Map
     */
    public static Map<String, Object> formatMap(Map<String, Object> map) {
        Map<String, Object> result = new HashMap<>();
        for (String longKey : map.keySet()) {
            if (longKey.contains(".")) {
                String firstKey = StringUtils.substringBefore(longKey, ".");
                Map<String, Object> innerMap;
                if (result.containsKey(firstKey)) {
                    innerMap = (Map<String, Object>) result.get(firstKey);
                } else {
                    innerMap = new HashMap<>();
                }
                String lastKey = StringUtils.substringAfter(longKey, ".");
                innerMap.put(lastKey, map.get(longKey));
                result.put(firstKey, formatMap(innerMap));
            } else {
                result.put(longKey, map.get(longKey));
            }
        }
        return result;
    }

}
