package com.scliang.iui.utils;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.scliang.iui.base.BaseBean;
import com.scliang.iui.base.ICallback;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;
import retrofit2.Callback;
import retrofit2.Converter;
import retrofit2.Response;
import retrofit2.Retrofit;

public final class DataUtils {
    public static <S> S newApi(Class<S> SERVICE, String baseUrl) {
        if (TextUtils.isEmpty(baseUrl)) { return null; }
        return instance().newRetrofit(baseUrl).create(SERVICE);
    }

    public static <D> void post(retrofit2.Call<D> call, ICallback<D> callback) {
        if (call == null) { return; }
        instance().postCall(call, callback);
    }

    private OkHttpClient newClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.connectTimeout(TIMEOUT_CONNECT, TimeUnit.SECONDS);
        builder.writeTimeout(TIMEOUT_WRITE, TimeUnit.SECONDS);
        builder.readTimeout(TIMEOUT_READ, TimeUnit.SECONDS);
        builder.addInterceptor(new LogUtils.DataLoggingInterceptor());
        return builder.build();
    }

    private Retrofit newRetrofit(String baseUrl) {
        return newRetrofit(baseUrl, new DataConverterFactory());
    }

    private Retrofit newRetrofit(String baseUrl, Converter.Factory converterFactory) {
        Retrofit.Builder builder = new Retrofit.Builder();
        builder.baseUrl(baseUrl);
        builder.callFactory(new DataCallFactory(newClient()));
        builder.addConverterFactory(converterFactory);
        return builder.build();
    }

    private <D> void postCall(@NonNull retrofit2.Call<D> call, ICallback<D> callback) {
        if (callback != null) { mUiHandler.post(callback::onStart); }
        call.enqueue(new Callback<D>() {
            @Override
            public void onResponse(@NonNull retrofit2.Call<D> call, @NonNull Response<D> response) {
                D data = response.body();
                if (data == null) {
                    ResponseBody eBody = response.errorBody();
                    DataException error = new DataException(String.valueOf(response.code()),
                            eBody == null ? "" : eBody.toString(), response.message());
                    if (callback != null) {
                        mUiHandler.post(() -> callback.onError(error));
                        mUiHandler.post(callback::onCompleted);
                    }
                } else {
                    if (callback != null) {
                        mUiHandler.post(() -> callback.onSuccess(data));
                        mUiHandler.post(callback::onCompleted);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull retrofit2.Call<D> call, @NonNull Throwable t) {
                DataException error = new DataException("-1", t.getMessage());
                if (callback != null) {
                    mUiHandler.post(() -> callback.onError(error));
                    mUiHandler.post(callback::onCompleted);
                }
            }
        });
    }

    private static final class DataCallFactory implements Call.Factory {
        public DataCallFactory(Call.Factory delegate) {
            this.delegate = delegate;
        }

        @NonNull
        @Override
        public Call newCall(@NonNull Request request) {
            return delegate.newCall(request);
        }

        private final Call.Factory delegate;
    }

    private static final class DataConverterFactory extends Converter.Factory {
        private final Gson gson;

        public DataConverterFactory() {
            gson = new Gson();
        }

        @Override
        public Converter<?, RequestBody> requestBodyConverter(@NonNull Type type,
                                                              @NonNull Annotation[] parameterAnnotations,
                                                              @NonNull Annotation[] methodAnnotations,
                                                              @NonNull Retrofit retrofit) {
            TypeAdapter<?> adapter = gson.getAdapter(TypeToken.get(type));
            return new DataRequestBodyConverter<>(gson, adapter);
        }

        @Override
        public Converter<ResponseBody, ?> responseBodyConverter(@NonNull Type type,
                                                                @NonNull Annotation[] annotations,
                                                                @NonNull Retrofit retrofit) {
            TypeAdapter<?> adapter = gson.getAdapter(TypeToken.get(type));
            return new DataResponseBodyConverter<>(gson, adapter, type);
        }

        private static class DataRequestBodyConverter<T> implements Converter<T, RequestBody> {
            private static final MediaType MEDIA_TYPE = MediaType.get("application/json; charset=UTF-8");
            private static final Charset UTF_8 = StandardCharsets.UTF_8;

            private final Gson gson;
            private final TypeAdapter<T> adapter;

            DataRequestBodyConverter(Gson gson, TypeAdapter<T> adapter) {
                this.gson = gson;
                this.adapter = adapter;
            }

            @Override
            public RequestBody convert(@NonNull T value) throws IOException {
                Buffer buffer = new Buffer();
                Writer writer = new OutputStreamWriter(buffer.outputStream(), UTF_8);
                JsonWriter jsonWriter = gson.newJsonWriter(writer);
                adapter.write(jsonWriter, value);
                jsonWriter.close();
                return RequestBody.create(MEDIA_TYPE, buffer.readByteString());
            }
        }

        private static class DataResponseBodyConverter<T> implements Converter<ResponseBody, T> {
            private final Gson gson;
            private final Type type;
            private final TypeAdapter<T> adapter;

            DataResponseBodyConverter(Gson gson, TypeAdapter<T> adapter, @NonNull Type type) {
                this.gson = gson;
                this.type = type;
                this.adapter = adapter;
            }

            @Override
            public T convert(@NonNull ResponseBody value) throws IOException {
                String resStr = "";
                long contentLength = value.contentLength();
                BufferedSource source = value.source();
                if (source.isOpen()) {
                    source.request(Long.MAX_VALUE);
                    Buffer buffer = source.getBuffer();
                    Charset charset = StandardCharsets.UTF_8;
                    MediaType contentType = value.contentType();
                    if (contentType != null) {
                        charset = contentType.charset(StandardCharsets.UTF_8);
                    }
                    if (contentLength != 0 && charset != null) {
                        resStr = buffer.clone().readString(charset);
                    }
                }
                try {
                    JsonReader jsonReader = gson.newJsonReader(value.charStream());
                    T result = adapter.read(jsonReader);
                    if (jsonReader.peek() != JsonToken.END_DOCUMENT) {
                        throw new JsonIOException("JSON document was not fully consumed.");
                    }
                    if (result instanceof BaseBean) {
                        try {
                            Field rvField = BaseBean.class.getDeclaredField("rawValue");
                            rvField.setAccessible(true);
                            rvField.set(result, resStr);
                        } catch (NoSuchFieldException | IllegalAccessException ignored) { }
                    }
                    return result;
                } catch (Exception e) {
                    T result = gson.fromJson("{}", type);
                    if (result instanceof BaseBean) {
                        try {
                            Field rvField = BaseBean.class.getDeclaredField("rawValue");
                            rvField.setAccessible(true);
                            rvField.set(result, resStr);
                        } catch (NoSuchFieldException | IllegalAccessException noSuchFieldException) {
                            throw new IOException(resStr);
                        }
                    } else {
                        throw new IOException(resStr);
                    }
                    return result;
                } finally {
                    value.close();
                }
            }
        }
    }

    private final Handler mUiHandler;

    private DataUtils() { mUiHandler = new Handler(Looper.getMainLooper()); }
    private static DataUtils instance() { return DataUtils.SingletonHolder.INSTANCE; }
    private static class SingletonHolder { private static final DataUtils INSTANCE = new DataUtils(); }

    private static final int TIMEOUT_CONNECT = 30; // sec
    private static final int TIMEOUT_WRITE   = 30; // sec
    private static final int TIMEOUT_READ    = 30; // sec
}
