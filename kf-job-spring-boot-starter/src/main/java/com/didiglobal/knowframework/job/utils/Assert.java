package com.didiglobal.knowframework.job.utils;

public class Assert {

    /**
     * 断言,判断非空.
     *
     * @param obj 需要判空的对象
     * @param msg 错误信息
     */
    public static void notNull(Object obj, String msg) {
        if (obj == null) {
            throw new IllegalArgumentException(msg);
        }
    }

    /**
     * isTrue.
     * @param test 测试
     * @param msg 消息
     */
    public static void isTrue(boolean test, String msg) {
        if (!test) {
            throw new IllegalStateException(msg);
        }
    }


    public static void isFalse(boolean test, String message) {
        isTrue(!test, message);
    }

}