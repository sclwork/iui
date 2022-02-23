package com.scliang.iui.utils;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import java.io.EOFException;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import okhttp3.Connection;
import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.internal.http.HttpHeaders;
import okio.Buffer;
import okio.BufferedSource;

public final class LogUtils {
    public interface OnLoggerListener {
        void onLogI(String buildType, String tag, String msg);
        void onLogD(String buildType, String tag, String msg);
        void onLogE(String buildType, String tag, String msg);
        void onLogV(String buildType, String tag, String msg);
        void onLogW(String buildType, String tag, String msg);
    }

    public static void init(String buildType) {
        BuildType = buildType;
        Loggable = !"release".equalsIgnoreCase(buildType);
        com.orhanobut.logger.Logger.addLogAdapter(new DataLogger.LogAdapter(Loggable));
    }

    public static boolean isLoggable() {
        return Loggable;
    }

    public static void setOnLoggerListener(OnLoggerListener listener) {
        sOnLoggerListener = new SoftReference<>(listener);
    }

    public static void i(String msg, Object... args) {
        StringBuilder sb = new StringBuilder();
        for (Object arg : args) if (arg != null) sb.append(";").append(arg);
        callOnLoggerListener(1, BuildType, msg + sb.toString());
        if (isLoggable()) Log.i(Tag, msg + sb.toString());
    }

    public static void d(String msg, Object... args) {
        StringBuilder sb = new StringBuilder();
        for (Object arg : args) if (arg != null) sb.append(";").append(arg);
        callOnLoggerListener(2, BuildType, msg + sb.toString());
        if (isLoggable()) Log.d(Tag, msg + sb.toString());
    }

    public static void e(String msg, Object... args) {
        StringBuilder sb = new StringBuilder();
        for (Object arg : args) if (arg != null) sb.append(";").append(arg);
        callOnLoggerListener(3, BuildType, msg + sb.toString());
        if (isLoggable()) Log.e(Tag, msg + sb.toString());
    }

    public static void v(String msg, Object... args) {
        StringBuilder sb = new StringBuilder();
        for (Object arg : args) if (arg != null) sb.append(";").append(arg);
        callOnLoggerListener(4, BuildType, msg + sb.toString());
        if (isLoggable()) Log.v(Tag, msg + sb.toString());
    }

    public static void w(String msg, Object... args) {
        StringBuilder sb = new StringBuilder();
        for (Object arg : args) if (arg != null) sb.append(";").append(arg);
        callOnLoggerListener(5, BuildType, msg + sb.toString());
        if (isLoggable()) Log.w(Tag, msg + sb.toString());
    }

    public static final class DataLoggingInterceptor implements Interceptor {
        private static final String sFormatLine =
                "===========================================================================================";
        public static final String sLogStartFlag = "==scliang==log==start==";
        public static final String sLogEndFlag = "==scliang==log==end==";
        private static final Charset UTF8 = StandardCharsets.UTF_8;

        public interface Logger {
            void reset();
            void log(String message);
        }

        public DataLoggingInterceptor() {
            this.logger = new DataLogger();
        }

        public DataLoggingInterceptor(Logger logger) {
            this.logger = logger;
        }

        private final Logger logger;

        @NotNull
        @Override public Response intercept(@NonNull Chain chain) throws IOException {
            Request request = chain.request();

            logger.reset();
            logger.log(sLogStartFlag);
            logger.log(sFormatLine);

            RequestBody requestBody = request.body();
            boolean hasRequestBody = requestBody != null;

            Connection connection = chain.connection();
            Protocol protocol = connection != null ? connection.protocol() : Protocol.HTTP_1_1;
            String requestStartMessage = request.method() + " " + request.url() + " " + protocol;
            logger.log(requestStartMessage);

            if (hasRequestBody) {
                if (requestBody.contentType() != null) {
                    logger.log("Content-Type: " + requestBody.contentType());
                }
                if (requestBody.contentLength() != -1) {
                    logger.log("Content-Length: " + requestBody.contentLength());
                }
            }

            Headers rHeaders = request.headers();
            for (int i = 0, count = rHeaders.size(); i < count; i++) {
                String name = rHeaders.name(i);
                if (!"Content-Type".equalsIgnoreCase(name) && !"Content-Length".equalsIgnoreCase(name)) {
                    logger.log(name + ": " + rHeaders.value(i));
                }
            }

            if (!hasRequestBody) {
                logger.log("END " + request.method());
            } else if (bodyEncoded(request.headers())) {
                logger.log("END " + request.method() + " (encoded body omitted)");
            } else {
                Buffer buffer = new Buffer();
                requestBody.writeTo(buffer);

                Charset charset = UTF8;
                MediaType contentType = requestBody.contentType();
                if (contentType != null) {
                    charset = contentType.charset(UTF8);
                }

                if (charset != null) {
                    logger.log(sFormatLine);
                    if (isPlaintext(buffer)) {
                        try {
                            String requestStr = URLDecoder.decode(buffer.readString(charset), "UTF-8");
                            String[] strs = requestStr.split("&");
                            for (String str : strs) {
                                logger.log(str);
                            }
                        } catch (Exception e) {
                            logger.log(buffer.readString(charset));
                        }
                        logger.log("END " + request.method()
                                + " (" + requestBody.contentLength() + "-byte body)");
                    } else {
                        logger.log("END " + request.method() + " (binary "
                                + requestBody.contentLength() + "-byte body omitted)");
                    }
                }
            }

            logger.log(sFormatLine);

            long startNs = System.nanoTime();
            Response response;
            try {
                response = chain.proceed(request);
            } catch (Exception e) {
                logger.log("HTTP FAILED: " + e);
                logger.log(sLogEndFlag);
                throw e;
            }

            long tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs);
            ResponseBody responseBody = response.body();
            if (responseBody != null) {
                long contentLength = responseBody.contentLength();
                logger.log(response.code() + " " + response.message() + " "
                        + response.request().url() + " (" + tookMs + "ms)");

                Headers headers = response.headers();
                for (int i = 0, count = headers.size(); i < count; i++) {
                    logger.log(headers.name(i) + ": " + headers.value(i));
                }

                if (!HttpHeaders.hasBody(response)) {
                    logger.log("END HTTP");
                } else if (bodyEncoded(response.headers())) {
                    logger.log("END HTTP (encoded body omitted)");
                } else {
                    BufferedSource source = responseBody.source();
                    if (source.isOpen()) {
                        source.request(Long.MAX_VALUE);
                        Buffer buffer = source.buffer();

                        Charset charset = UTF8;
                        MediaType contentType = responseBody.contentType();
                        if (contentType != null) {
                            charset = contentType.charset(UTF8);
                        }

                        if (!isPlaintext(buffer)) {
                            logger.log("END HTTP (binary " + buffer.size() + "-byte body omitted)");
                            logger.log(sFormatLine);
                            logger.log(sLogEndFlag);
                            return response;
                        }

                        if (contentLength != 0 && charset != null) {
                            logger.log(sFormatLine);
                            logger.log(buffer.clone().readString(charset));
                        }

                        logger.log("END HTTP (" + buffer.size() + "-byte body)");
                    } else {
                        logger.log("END HTTP");
                    }
                }
            }

            logger.log(sFormatLine);
            logger.log(sLogEndFlag);

            return response;
        }

        private static boolean isPlaintext(Buffer buffer) {
            try {
                Buffer prefix = new Buffer();
                long byteCount = buffer.size() < 64 ? buffer.size() : 64;
                buffer.copyTo(prefix, 0, byteCount);
                for (int i = 0; i < 16; i++) {
                    if (prefix.exhausted()) {
                        break;
                    }
                    int codePoint = prefix.readUtf8CodePoint();
                    if (Character.isISOControl(codePoint) && !Character.isWhitespace(codePoint)) {
                        return false;
                    }
                }
                return true;
            } catch (EOFException e) {
                return false;
            }
        }

        private boolean bodyEncoded(Headers headers) {
            String contentEncoding = headers.get("Content-Encoding");
            return contentEncoding != null && !contentEncoding.equalsIgnoreCase("identity");
        }
    }

    private static final class DataLogger implements DataLoggingInterceptor.Logger {
        private final StringBuilder mMessage = new StringBuilder();

        private static class LogAdapter extends com.orhanobut.logger.AndroidLogAdapter {
            private final boolean mDebuggable;

            public LogAdapter(boolean debuggable) {
                super();
                mDebuggable = debuggable;
            }

            @Override
            public boolean isLoggable(int priority, @Nullable String tag) {
                return mDebuggable;
            }
        }

        @Override
        public void log(String message) {
            if (message.startsWith(DataLoggingInterceptor.sLogStartFlag)) {
                mMessage.setLength(0);
                return;
            }
            if (message.startsWith(DataLoggingInterceptor.sLogEndFlag)) {
                com.orhanobut.logger.Logger.d(mMessage.toString());
                return;
            }
            if ((message.startsWith("{") && message.endsWith("}"))
                    || (message.startsWith("[") && message.endsWith("]"))) {
                message = formatJson(decodeUnicode(message));
            }
            try { mMessage.append(message.concat("\n"));
            } catch (Exception ignored) {}
        }

        @Override
        public void reset() {
            mMessage.setLength(0);
        }

        private static String formatJson(String jsonStr) {
            if (null == jsonStr || "".equals(jsonStr)) return "";
            StringBuilder sb = new StringBuilder();
            char last = '\0';
            char current = '\0';
            int indent = 0;
            for (int i = 0; i < jsonStr.length(); i++) {
                last = current;
                current = jsonStr.charAt(i);
                switch (current) {
                    case '{':
                    case '[':
                        sb.append(current);
                        sb.append('\n');
                        indent++;
                        addIndentBlank(sb, indent);
                        break;
                    case '}':
                    case ']':
                        sb.append('\n');
                        indent--;
                        addIndentBlank(sb, indent);
                        sb.append(current);
                        break;
                    case ',':
                        sb.append(current);
                        if (last != '\\') {
                            sb.append('\n');
                            addIndentBlank(sb, indent);
                        }
                        break;
                    default:
                        sb.append(current);
                }
            }
            return sb.toString();
        }

        private static void addIndentBlank(StringBuilder sb, int indent) {
            for (int i = 0; i < indent; i++) {
                sb.append('\t');
            }
        }

        private static String decodeUnicode(String theString) {
            char aChar;
            int len = theString.length();
            StringBuilder outBuffer = new StringBuilder(len);
            for (int x = 0; x < len; ) {
                aChar = theString.charAt(x++);
                if (aChar == '\\') {
                    aChar = theString.charAt(x++);
                    if (aChar == 'u') {
                        int value = 0;
                        for (int i = 0; i < 4; i++) {
                            aChar = theString.charAt(x++);
                            switch (aChar) {
                                case '0':
                                case '1':
                                case '2':
                                case '3':
                                case '4':
                                case '5':
                                case '6':
                                case '7':
                                case '8':
                                case '9':
                                    value = (value << 4) + aChar - '0';
                                    break;
                                case 'a':
                                case 'b':
                                case 'c':
                                case 'd':
                                case 'e':
                                case 'f':
                                    value = (value << 4) + 10 + aChar - 'a';
                                    break;
                                case 'A':
                                case 'B':
                                case 'C':
                                case 'D':
                                case 'E':
                                case 'F':
                                    value = (value << 4) + 10 + aChar - 'A';
                                    break;
                                default:
                                    throw new IllegalArgumentException(
                                            "Malformed   \\uxxxx   encoding.");
                            }
                        }
                        outBuffer.append((char) value);
                    } else {
                        if (aChar == 't')
                            aChar = '\t';
                        else if (aChar == 'r')
                            aChar = '\r';
                        else if (aChar == 'n')
                            aChar = '\n';
                        else if (aChar == 'f')
                            aChar = '\f';
                        outBuffer.append(aChar);
                    }
                } else {
                    outBuffer.append(aChar);
                }
            }
            return outBuffer.toString();
        }
    }

    private static final String Tag = "iUi-LOG";
    private static String BuildType;
    private static boolean Loggable;

    private LogUtils() { }
    private static SoftReference<OnLoggerListener> sOnLoggerListener;

    private static void callOnLoggerListener(int type, String buildType, String msg) {
        if (sOnLoggerListener == null) {
            return;
        }

        OnLoggerListener listener = sOnLoggerListener.get();

        if (listener == null) {
            return;
        }

        switch (type) {
            case 1: listener.onLogI(buildType, LogUtils.Tag, msg); break;
            case 2: listener.onLogD(buildType, LogUtils.Tag, msg); break;
            case 3: listener.onLogE(buildType, LogUtils.Tag, msg); break;
            case 4: listener.onLogV(buildType, LogUtils.Tag, msg); break;
            case 5: listener.onLogW(buildType, LogUtils.Tag, msg); break;
        }
    }
}
