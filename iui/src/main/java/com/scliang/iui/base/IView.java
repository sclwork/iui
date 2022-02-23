package com.scliang.iui.base;

import android.content.Context;

public interface IView {
    Context getContext();

    void showLoading();
    void dismissLoading();
}
