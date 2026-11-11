package com.postalroute.backend.route;

import com.postalroute.backend.vehicle.VehicleCapacity;

import java.util.List;

public class VehicleRoute {
    private VehicleCapacity vehicle;            // 배정된 차량 (몇 톤인지)
    private List<Destination> orderedDestinations; // 방문 순서대로 정렬된 배송지들
    private List<Double> legDistances; // 각 구간 거리(km) — 이전 지점에서 여기까지
    // 표에 맞는 톤수가 하나도 없어서 어쩔 수 없이 제일 큰 차로 배정한 경우 true
    // (실제로는 이 차로도 다 못 실을 수 있다는 뜻 - 화면에 경고로 보여줄 때 씀)
    private boolean capacityUncertain;

    public VehicleRoute(VehicleCapacity vehicle, List<Destination> orderedDestinations, List<Double> legDistances,
                         boolean capacityUncertain) {
        this.vehicle = vehicle;
        this.orderedDestinations = orderedDestinations;
        this.legDistances = legDistances;
        this.capacityUncertain = capacityUncertain;
    }

    public VehicleCapacity getVehicle() {
        return vehicle;
    }

    public List<Destination> getOrderedDestinations() {
        return orderedDestinations;
    }

    public List<Double> getLegDistances() {
        return legDistances;
    }

    public boolean isCapacityUncertain() {
        return capacityUncertain;
    }
}