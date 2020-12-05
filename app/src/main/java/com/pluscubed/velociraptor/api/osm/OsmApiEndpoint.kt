package com.pluscubed.velociraptor.api.osm

class OsmApiEndpoint(val baseUrl: String) : Comparable<OsmApiEndpoint> {

    var service: OsmService? = null
        get() {
            if (field == null) {
                throw Exception("OSM Service not set")
            }
            return field
        }
    var timeTaken: Int = 0

    override fun toString(): String {
        val time: String
        if (timeTaken == Integer.MAX_VALUE) {
            time = "error"
        } else if (timeTaken == 0) {
            time = "pending"
        } else {
            time = timeTaken.toString() + "ms"
        }

        return this.baseUrl + " - " + time
    }

    override fun compareTo(other: OsmApiEndpoint): Int {
        return timeTaken.compareTo(other.timeTaken);
    }
}
