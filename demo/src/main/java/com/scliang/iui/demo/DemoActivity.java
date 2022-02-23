package com.scliang.iui.demo;

import android.os.Bundle;

import androidx.annotation.Nullable;

import com.scliang.iui.base.BaseActivity;
import com.scliang.iui.demo.databinding.ActivityDemoBinding;
import com.scliang.iui.utils.DataException;
import com.scliang.iui.utils.DialogUtils;

import java.util.Locale;

public class DemoActivity extends BaseActivity<DemoContract.IDemoPresenter, ActivityDemoBinding> implements DemoContract.IDemoView {
    @Override
    protected DemoContract.IDemoPresenter createPresenter() {
        return new DemoContract.DemoPresenter(this);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_demo;
    }

    @Override
    protected void onCreateHere(@Nullable Bundle savedInstanceState) {
        super.onCreateHere(savedInstanceState);
        mBinding.btn1.setOnClickListener(v -> mPresenter.checkVersion());
    }

    @Override
    public void checkVersionSuccess(VersionBean data) {
        mBinding.tv1.setText(data.getRawValue());
    }

    @Override
    public void checkVersionFail(DataException error) {
        String text = String.format(Locale.CHINESE, "%s - %s - %s", error.getCode(), error.getMessage(), error.getData());
        mBinding.tv1.setText(text);
        DialogUtils.toast(text);
    }
}