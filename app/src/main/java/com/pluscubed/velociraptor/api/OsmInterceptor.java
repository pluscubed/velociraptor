package com.pluscubed.velociraptor.api;

import com.pluscubed.velociraptor.BuildConfig;

import java.io.IOException;
import java.util.List;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

final class OsmInterceptor implements Interceptor {

    private OsmApiEndpoint endpoint;

    public OsmApiEndpoint getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(OsmApiEndpoint endpoint) {
        this.endpoint = endpoint;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        List<String> pathSegments = request.url().pathSegments();
        String url = endpoint.baseUrl + pathSegments.get(pathSegments.size() - 1) + "?" + request.url().encodedQuery();
        HttpUrl newUrl = HttpUrl.parse(url);
        request = request.newBuilder()
                .url(newUrl)
                .addHeader("User-Agent", "Velociraptor/" + BuildConfig.VERSION_NAME)
                .build();

        long start = System.currentTimeMillis();
        try {
            Response proceed = chain.proceed(request);
            if (!proceed.isSuccessful()) {
                throw new IOException(proceed.toString());
            } else {
                endpoint.timeTaken = (int) (System.currentTimeMillis() - start);
            }
            return proceed;
        } finally {
            endpoint.timeTakenTimestamp = System.currentTimeMillis();
        }
    }
}
