package com.scliang.iui.base;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public abstract class BasePresenter<M extends IModel, V extends IView> implements IPresenter {
    private final M mModel;
    private final M mProxyModel;
    private final V mProxyView;
    private final WeakReference<V> mView;

    @SuppressWarnings("unchecked")
    public BasePresenter(@NotNull Class<? extends IModel> model, @NotNull V view) {
        M m; try { m = (M) model.newInstance(); } catch (Exception e) { m = null; }
        mModel = m;
        mProxyModel = (M) Proxy.newProxyInstance(
                model.getClassLoader(),
                model.getInterfaces(),
                new ModelHandler());
        mView = new WeakReference<>(view);
        mProxyView = (V) Proxy.newProxyInstance(
                view.getClass().getClassLoader(),
                view.getClass().getInterfaces(),
                new ViewHandler());
    }

    public final M getModel() {
        return mProxyModel;
    }

    public final V getView() {
        return mProxyView;
    }

    @Override
    public final void detachView() {
        mView.clear();
        mProxyModel.cancelRequests();
    }

    private class ModelHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (mModel != null) {
                return method.invoke(mModel, args);
            } else {
                return null;
            }
        }
    }

    private class ViewHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            V view = mView.get();
            if (view != null) {
                return method.invoke(view, args);
            } else {
                return null;
            }
        }
    }
}
