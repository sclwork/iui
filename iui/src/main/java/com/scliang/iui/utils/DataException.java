package com.scliang.iui.utils;

public final class DataException extends Exception {
    private final String code;
    private final String data;

    public DataException(String code, String message) {
        this(code, "", message);
    }

    public DataException(String code, String data, String message) {
        super(message);
        this.code = code;
        this.data = data;
    }

    public String getCode() {
        return code;
    }

    public String getData() {
        return data;
    }
}
