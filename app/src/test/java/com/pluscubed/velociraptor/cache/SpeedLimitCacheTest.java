package com.pluscubed.velociraptor.cache;

import android.os.Build;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pluscubed.velociraptor.BuildConfig;
import com.pluscubed.velociraptor.api.ApiResponse;
import com.pluscubed.velociraptor.api.osmapi.Coord;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import rx.observers.TestSubscriber;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = Build.VERSION_CODES.M)
public class SpeedLimitCacheTest {

    SpeedLimitCache instance;
    private File cache;
    private ApiResponse response;

    @Before
    public void setUp() throws Exception {
        cache = new File("cache.json");
        instance = SpeedLimitCache.getInstance(cache);

        response = new ApiResponse();
        response.coords = new ArrayList<>();

        //Alta/Charleston - Huff/Charleston
        response.coords.add(new Coord(37.420915, -122.085359));
        response.coords.add(new Coord(37.420774, -122.082945));

        response.timestamp = System.currentTimeMillis();
        response.roadNames = new String[]{"Charleston Road", null};
        response.speedLimit = 35;
    }

    @Test
    public void put_singleResponse() throws Exception {
        instance.put(response);

        assertThat(instance.responses.size(), is(1));

        ObjectMapper mapper = new ObjectMapper();
        List<ApiResponse> responses = mapper.readValue(cache, new TypeReference<List<ApiResponse>>() {
        });
        assertThat(responses.get(0), is(response));
    }

    @Test
    public void get_coordOnRoad() throws Exception {
        instance.responses.add(response);

        TestSubscriber<ApiResponse> testSubscriber = new TestSubscriber<>();
        instance.get(null, new Coord(37.420902, -122.084073)).subscribe(testSubscriber);

        testSubscriber.assertValue(response);
    }

    @Test
    public void get_coordNotOnRoad() throws Exception {
        instance.responses.add(response);

        TestSubscriber<ApiResponse> testSubscriber = new TestSubscriber<>();
        instance.get(null, new Coord(37.419188, -122.085396)).subscribe(testSubscriber);

        testSubscriber.assertNoValues();
        testSubscriber.assertCompleted();
    }

    @Test
    public void get_oneResponseValidWithPreviousName() throws Exception {
        instance.responses.add(response);

        ApiResponse otherResponse = new ApiResponse();
        otherResponse.coords = new ArrayList<>();
        //Same as normal
        otherResponse.coords.add(new Coord(37.420915, -122.085359));
        otherResponse.coords.add(new Coord(37.420774, -122.082945));
        otherResponse.timestamp = System.currentTimeMillis();
        otherResponse.roadNames = new String[]{"Not Charleston Road", "Definitely Not"};
        otherResponse.speedLimit = 15;
        instance.responses.add(otherResponse);

        TestSubscriber<ApiResponse> testSubscriber = new TestSubscriber<>();
        instance.get(new String[]{"Charleston Road", null}, new Coord(37.420902, -122.084073))
                .subscribe(testSubscriber);

        testSubscriber.assertValue(response);
    }

    @Test
    public void get_oldCache() throws Exception {
        //One second older than 1 week
        response.timestamp = System.currentTimeMillis() - (6_048_0000_0000L + 1000L);
        instance.responses.add(response);

        TestSubscriber<ApiResponse> testSubscriber = new TestSubscriber<>();
        instance.get(new String[]{"Charleston Road", null}, new Coord(37.420902, -122.084073))
                .subscribe(testSubscriber);

        testSubscriber.assertNoValues();
        testSubscriber.assertCompleted();
    }


    @After
    public void tearDown() throws Exception {
        instance.responses.clear();
        cache.delete();
    }
}