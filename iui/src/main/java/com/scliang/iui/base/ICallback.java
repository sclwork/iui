package com.scliang.iui.base;

import com.scliang.iui.utils.DataException;

public interface ICallback<D> {
    void onStart();
    void onSuccess(D data);
    void onError(DataException error);
    void onCompleted();
}
