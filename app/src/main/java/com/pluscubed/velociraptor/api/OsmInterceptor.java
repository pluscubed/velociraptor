package com.pluscubed.velociraptor.api;

import com.pluscubed.velociraptor.BuildConfig;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

final class OsmInterceptor implements Interceptor {

    private OsmApiEndpoint endpoint;

    public OsmInterceptor(OsmApiEndpoint endpoint) {
        this.endpoint = endpoint;
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
                endpoint.timeTaken = (int) (System.currentTimeMillis() - start);
            }
            return proceed;
        } finally {
            endpoint.timeTakenTimestamp = System.currentTimeMillis();
        }
    }
}
