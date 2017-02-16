package com.pluscubed.velociraptor.cache;

import com.google.auto.value.AutoValue;
import com.pluscubed.velociraptor.api.ApiResponse;
import com.pluscubed.velociraptor.api.osmapi.Coord;
import com.squareup.sqldelight.RowMapper;

import java.util.ArrayList;
import java.util.List;

@AutoValue
public abstract class Way implements wayModel {
    public static final Factory<Way> FACTORY = new wayModel.Factory<>(AutoValue_Way::new);

    public static final RowMapper<Way> SELECT_BY_COORD = FACTORY.select_by_coordMapper();

    public static List<Way> fromResponse(ApiResponse response) {
        List<Way> ways = new ArrayList<>();
        for (int i = 0; i < response.coords.size() - 1; i++) {
            Coord coord1 = response.coords.get(i);
            Coord coord2 = response.coords.get(i + 1);

            double clat = (coord1.lat + coord2.lat) / 2;
            double clon = (coord1.lon + coord2.lon) / 2;

            Way way = new AutoValue_Way(clat, clon,
                    (long) response.speedLimit, response.timestamp,
                    coord1.lat, coord1.lon, coord2.lat, coord2.lon, response.roadName);
            ways.add(way);
        }
        return ways;
    }

    public ApiResponse toResponse() {
        ApiResponse response = new ApiResponse();
        response.speedLimit = (int) maxspeed();
        response.timestamp = timestamp();
        response.roadName = road();
        return response;
    }
}
