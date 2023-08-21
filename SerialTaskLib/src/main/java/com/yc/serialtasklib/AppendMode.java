package com.yc.serialtasklib;

public enum AppendMode {
    /**
     * 正常
     */
    Normal,
    /**
     * 替换
     */
    Replace,
    /**
     * 弃置
     */
    Discard,
    /**
     * 精确替换
     */
    ReplaceStrict,
    /**
     * 精确弃置
     */
    DiscardStrict;

    private AppendMode() {

    }
}
