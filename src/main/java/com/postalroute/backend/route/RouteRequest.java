package com.postalroute.backend.route;

import java.util.List;

public class RouteRequest {
    private List<Destination> destinations; // 들를 곳들 목록

    public List<Destination> getDestinations() {
        return destinations;
    }

    public void setDestinations(List<Destination> destinations) {
        this.destinations = destinations;
    }
}
