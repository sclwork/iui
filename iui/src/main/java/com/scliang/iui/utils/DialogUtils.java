package com.scliang.iui.utils;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.scliang.iui.R;
import com.scliang.iui.base.BaseApplication;
import com.scliang.iui.base.BaseDialog;
import com.scliang.iui.base.BasePresenter;
import com.scliang.iui.base.IModel;
import com.scliang.iui.base.IPresenter;
import com.scliang.iui.base.IView;
import com.scliang.iui.databinding.DialogSimpleLoadingBinding;

public final class DialogUtils {
    public static DialogUtils instance() { return DialogUtils.SingletonHolder.INSTANCE; }

    @SuppressLint("InflateParams")
    public static void toast(@NonNull CharSequence text) {
        View view = LayoutInflater.from(BaseApplication.instance()).inflate(R.layout.view_toast, null);
        ((TextView) view.findViewById(R.id.tv_text)).setText(text);
        Toast toast = Toast.makeText(BaseApplication.instance(), text, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.setView(view);
        toast.show();
    }

    public static void showSimpleLoading(@NonNull final FragmentManager manager) {
        instance().mUiHandler.post(() -> {
            if (instance().mLoadingDlg == null) {
                instance().mLoadingDlg = new SimpleLoadingDialog(() -> instance().mUiHandler.post(() -> instance().mLoadingDlg = null));
                instance().mLoadingDlg.show(manager, "SimpleLoadingDlg-" + System.currentTimeMillis());
            }
        });
    }

    public static void dismissSimpleLoading() {
        instance().mUiHandler.post(() -> {
            if (instance().mLoadingDlg != null) {
                instance().mLoadingDlg.dismiss();
                instance().mLoadingDlg = null;
            }
        });
    }

    public static final class SimpleContract {
        public interface ISimpleView extends IView {}
        public interface ISimpleModel extends IModel {}
        public interface ISimplePresenter extends IPresenter {}
        /*
         * Impl
         */
        public static final class SimplePresenter extends BasePresenter<ISimpleModel, ISimpleView> implements ISimplePresenter {
            public SimplePresenter(ISimpleView view) {
                super(SimpleModel.class, view);
            }
        }
        public static final class SimpleModel implements ISimpleModel {
            @Override
            public void cancelRequests() { }
        }
    }

    public static class SimpleLoadingDialog extends BaseDialog<IPresenter, DialogSimpleLoadingBinding> implements SimpleContract.ISimpleView {
        private final Runnable mDismissRunnable;
        SimpleLoadingDialog(Runnable dismissRunnable) { mDismissRunnable = dismissRunnable; }
        @Override
        protected int getLayoutId() { return R.layout.dialog_simple_loading; }
        @Override
        public void onViewCreatedHere(@NonNull View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreatedHere(view, savedInstanceState);
            setCancelable(false);
        }
        @Override
        protected IPresenter createPresenter() { return new SimpleContract.SimplePresenter(this); }
        @Override
        public void onDismiss(@NonNull DialogInterface dialog) {
            super.onDismiss(dialog);
            if (mDismissRunnable != null) mDismissRunnable.run();
        }
        @Override
        public void showLoading() { }
        @Override
        public void dismissLoading() { }
    }

    private final Handler mUiHandler;
    private DialogFragment mLoadingDlg;
    private DialogUtils() { mUiHandler = new Handler(Looper.getMainLooper()); }
    private static class SingletonHolder { private static final DialogUtils INSTANCE = new DialogUtils(); }
}
