package com.scliang.iui.demo;

import com.scliang.iui.base.BasePresenter;
import com.scliang.iui.base.ICallback;
import com.scliang.iui.base.IPresenter;
import com.scliang.iui.base.IView;
import com.scliang.iui.utils.DataException;

public final class DemoContract {
    public interface IDemoView extends IView {
        void checkVersionSuccess(VersionBean data);
        void checkVersionFail(DataException error);
    }

    public interface IDemoPresenter extends IPresenter {
        void checkVersion();
    }

    /*
     * Impl
     */
    public static final class DemoPresenter extends BasePresenter<IVersionModel, IDemoView> implements IDemoPresenter {
        public DemoPresenter(IDemoView view) {
            super(VersionModel.class, view);
        }

        @Override
        public void checkVersion() {
            getModel().checkVersion("", true, new ICallback<VersionBean>() {
                @Override
                public void onStart() {
                    getView().showLoading();
                }

                @Override
                public void onSuccess(VersionBean data) {
                    getView().checkVersionSuccess(data);
                }

                @Override
                public void onError(DataException error) {
                    getView().checkVersionFail(error);
                }

                @Override
                public void onCompleted() {
                    getView().dismissLoading();
                }
            });
        }
    }
}
