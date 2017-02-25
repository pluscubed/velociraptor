package com.pluscubed.velociraptor.cache;

import com.google.auto.value.AutoValue;
import com.pluscubed.velociraptor.api.LimitResponse;
import com.pluscubed.velociraptor.api.osmapi.Coord;
import com.squareup.sqldelight.RowMapper;

import java.util.ArrayList;
import java.util.List;

@AutoValue
public abstract class LimitCacheWay implements WayModel {
    public static final Factory<LimitCacheWay> FACTORY = new WayModel.Factory<>(AutoValue_LimitCacheWay::new);

    public static final RowMapper<LimitCacheWay> SELECT_BY_COORD = FACTORY.select_by_coordMapper();

    public static List<LimitCacheWay> fromResponse(LimitResponse response) {
        List<LimitCacheWay> ways = new ArrayList<>();
        for (int i = 0; i < response.coords().size() - 1; i++) {
            Coord coord1 = response.coords().get(i);
            Coord coord2 = response.coords().get(i + 1);

            double clat = (coord1.lat + coord2.lat) / 2;
            double clon = (coord1.lon + coord2.lon) / 2;

            LimitCacheWay way = new AutoValue_LimitCacheWay(clat, clon,
                    (long) response.speedLimit(), response.timestamp(),
                    coord1.lat, coord1.lon, coord2.lat, coord2.lon, response.roadName());
            ways.add(way);
        }
        return ways;
    }

    public LimitResponse.Builder toResponse() {
        LimitResponse.Builder response = LimitResponse.builder();
        response.setSpeedLimit((int) maxspeed());
        response.setTimestamp(timestamp());
        response.setRoadName(road());
        return response;
    }
}
