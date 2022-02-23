package com.scliang.iui.simple;

import com.scliang.iui.base.BasePresenter;
import com.scliang.iui.base.IPresenter;
import com.scliang.iui.base.IView;

public final class SimpleContract {
    public interface ISimpleView extends IView {
    }

    public interface ISimplePresenter extends IPresenter {
    }

    /*
     * Impl
     */
    public static final class SimplePresenter extends BasePresenter<ISimpleModel, ISimpleView> implements ISimplePresenter {
        public SimplePresenter(ISimpleView view) {
            super(SimpleModel.class, view);
        }
    }
}
