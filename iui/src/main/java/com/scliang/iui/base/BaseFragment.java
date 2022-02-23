package com.scliang.iui.base;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.fragment.app.Fragment;

import com.scliang.iui.utils.DialogUtils;
import com.scliang.iui.event.EmptyEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public abstract class BaseFragment<P extends IPresenter, VDB extends ViewDataBinding> extends Fragment implements IView {
    protected P mPresenter;
    protected VDB mBinding;

    @Nullable
    @Override
    public final Context getContext() {
        return super.getContext();
    }

    @Override
    public void showLoading() {
        DialogUtils.showSimpleLoading(getChildFragmentManager());
    }

    @Override
    public void dismissLoading() {
        DialogUtils.dismissSimpleLoading();
    }

    @NonNull
    @Override
    public final View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mPresenter = createPresenter();
        mBinding = DataBindingUtil.inflate(inflater, getLayoutId(), container, false);
        mBinding.setLifecycleOwner(this);
        return mBinding.getRoot();
    }

    @Override
    public final void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        EventBus.getDefault().register(this);
        onViewCreatedHere(view, savedInstanceState);
    }

    public void onViewCreatedHere(@NonNull View view, @Nullable Bundle savedInstanceState) {
    }

    @Override
    public final void onDestroyView() {
        if (mPresenter != null) {
            mPresenter.detachView();
            mPresenter = null;
        }
        mBinding.unbind();
        EventBus.getDefault().unregister(this);
        onDestroyViewHere();
        super.onDestroyView();
    }

    public void onDestroyViewHere() {
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public final void onEmptyEvent(EmptyEvent event) {
    }

    protected abstract P createPresenter();

    protected abstract @LayoutRes int getLayoutId();
}
