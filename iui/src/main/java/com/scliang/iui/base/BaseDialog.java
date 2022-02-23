package com.scliang.iui.base;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.scliang.iui.event.EmptyEvent;
import com.scliang.iui.R;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public abstract class BaseDialog<P extends IPresenter, VDB extends ViewDataBinding> extends DialogFragment implements IView {
    protected P mPresenter;
    protected VDB mBinding;

    @Override
    public int show(@NonNull FragmentTransaction transaction, @Nullable String tag) {
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.SimpleDialog);
        try { return super.show(transaction, tag); } catch (Exception e) { return -1; }
    }

    @Override
    public void show(@NonNull FragmentManager manager, @Nullable String tag) {
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.SimpleDialog);
        try { super.show(manager, tag); } catch (Exception ignored) {}
    }

    @Override
    public void showNow(@NonNull FragmentManager manager, @Nullable String tag) {
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.SimpleDialog);
        try { super.showNow(manager, tag); } catch (Exception ignored) {}
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
    public void onDestroyView() {
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
