package com.postalroute.backend.route;

import com.postalroute.backend.vehicle.VehicleCapacity;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class RouteBuilder {

    private final KakaoDistanceService kakaoDistanceService;

    public RouteBuilder(KakaoDistanceService kakaoDistanceService) {
        this.kakaoDistanceService = kakaoDistanceService;
    }

    // DestinationClusterer가 이미 시간순으로 정해준 경로를 받아서,
    // 차량 톤수만 배정하고 구간별 거리를 계산한다 (순서는 더 이상 안 바꿈 - 시간이 순서를 강제하니까)
    public VehicleRoute buildRoute(List<Destination> orderedDestinations) {
        // 1. 총 파렛트 양 구하기
        int totalFlat = 0, totalRoll = 0;
        for (Destination d : orderedDestinations) {
            totalFlat += d.getFlatPalletCount();
            totalRoll += d.getRollPalletCount();
        }

        // 2. 딱 맞는 가장 작은 차량 고르기
        VehicleCapacity assignedVehicle = null;
        for (VehicleCapacity vehicle : VehicleCapacity.values()) {
            if (vehicle.canLoad(totalFlat, totalRoll)) {
                assignedVehicle = vehicle;
                break;
            }
        }
        // 표에 맞는 톤수가 하나도 없으면, 어쩔 수 없이 제일 큰 차로 배정하되
        // "이건 확실하지 않다"는 표시를 남겨서 화면에서 경고로 보여줄 수 있게 함
        boolean capacityUncertain = assignedVehicle == null;
        if (capacityUncertain) {
            VehicleCapacity[] all = VehicleCapacity.values();
            assignedVehicle = all[all.length - 1];
        }

        // 3. 구간별 거리 계산 (첫 스탑은 시작점이라 거리 0, 그다음부터 이전 스탑 기준 거리)
        List<Double> legDistances = new ArrayList<>();
        Destination prev = null;
        for (Destination d : orderedDestinations) {
            if (prev == null) {
                legDistances.add(0.0);
            } else {
                legDistances.add(kakaoDistanceService.distanceKm(
                        prev.getLatitude(), prev.getLongitude(), d.getLatitude(), d.getLongitude()));
            }
            prev = d;
        }

        return new VehicleRoute(assignedVehicle, orderedDestinations, legDistances, capacityUncertain);
    }
}
