package com.pluscubed.velociraptor.api.raptor;

import com.pluscubed.velociraptor.BuildConfig;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class LimitInterceptor implements Interceptor {

    private Callback callback;

    public LimitInterceptor(Callback endpoint) {
        this.callback = endpoint;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        request = request.newBuilder()
                .addHeader("User-Agent", "Velociraptor/" + BuildConfig.VERSION_NAME)
                .build();

        long start = System.currentTimeMillis();
        try {
            Response proceed = chain.proceed(request);
            if (!proceed.isSuccessful()) {
                throw new IOException(proceed.code() + ": " + proceed.toString());
            } else {
                callback.updateTimeTaken((int) (System.currentTimeMillis() - start));
            }
            return proceed;
        } finally {
            callback.updateTimestamp(System.currentTimeMillis());
        }
    }


    public interface Callback {
        void updateTimeTaken(int timeTaken);

        void updateTimestamp(long timeTakenTimestamp);
    }
}
