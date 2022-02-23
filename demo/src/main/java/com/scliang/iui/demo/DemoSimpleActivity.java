package com.scliang.iui.demo;

import com.scliang.iui.demo.databinding.ActivityDemoSimpleBinding;
import com.scliang.iui.simple.SimpleBaseActivity;

public class DemoSimpleActivity extends SimpleBaseActivity<ActivityDemoSimpleBinding> {
    @Override
    protected int getLayoutId() {
        return R.layout.activity_demo_simple;
    }
}
