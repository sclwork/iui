package com.scliang.iui.simple;

import androidx.databinding.ViewDataBinding;

import com.scliang.iui.base.BaseActivity;

public abstract class SimpleBaseActivity<VDB extends ViewDataBinding> extends BaseActivity<SimpleContract.ISimplePresenter, VDB> implements SimpleContract.ISimpleView {
    @Override
    protected SimpleContract.ISimplePresenter createPresenter() {
        return new SimpleContract.SimplePresenter(this);
    }
}
