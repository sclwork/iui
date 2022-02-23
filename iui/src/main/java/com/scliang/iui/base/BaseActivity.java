package com.scliang.iui.base;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.CallSuper;
import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;

import com.scliang.iui.utils.DialogUtils;
import com.scliang.iui.event.EmptyEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public abstract class BaseActivity<P extends IPresenter, VDB extends ViewDataBinding> extends AppCompatActivity implements IView {
    protected P mPresenter;
    protected VDB mBinding;

    @Override
    public final Context getContext() {
        return this;
    }

    @Override
    public void showLoading() {
        DialogUtils.showSimpleLoading(getSupportFragmentManager());
    }

    @Override
    public void dismissLoading() {
        DialogUtils.dismissSimpleLoading();
    }

    @Override
    public final void setContentView(View view) {
//        super.setContentView(view);
    }

    @Override
    public final void setContentView(int layoutResID) {
//        super.setContentView(layoutResID);
    }

    @Override
    public final void setContentView(View view, ViewGroup.LayoutParams params) {
//        super.setContentView(view, params);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public final void onEmptyEvent(EmptyEvent event) {
    }

    @Override
    @CallSuper
    protected final void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setContentView(getLayoutId());
        mBinding = DataBindingUtil.setContentView(this, getLayoutId());
        mBinding.setLifecycleOwner(this);
        mPresenter = createPresenter();
        EventBus.getDefault().register(this);
        onCreateHere(savedInstanceState);
    }

    protected void onCreateHere(@Nullable Bundle savedInstanceState) {
    }

    @Override
    @CallSuper
    protected final void onDestroy() {
        if (mPresenter != null) {
            mPresenter.detachView();
            mPresenter = null;
        }
        EventBus.getDefault().unregister(this);
        onDestroyHere();
        super.onDestroy();
    }

    protected void onDestroyHere() {
    }

    protected abstract P createPresenter();

    protected abstract @LayoutRes int getLayoutId();
}
