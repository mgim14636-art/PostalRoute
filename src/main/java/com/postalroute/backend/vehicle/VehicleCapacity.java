package com.postalroute.backend.vehicle;

import java.util.LinkedHashMap;
import java.util.Map;

// 우체국 "평-롤파렛 혼재시 적재 기준" 표를 그대로 데이터화
public enum VehicleCapacity {

    // key 0(평파렛 0개일 때 롤파렛 최대치)은 나주우체국 "차종별 운송용기 적재수량" 기준표(2018)의
    // 롤파렛 만재 범위 중 큰 쪽 숫자를 사용함 (예: 4.5톤 "6~7개" -> 7)
    TON_2_5(2.5, Map.of(0, 5, 1, 4, 2, 2, 3, 1)),
    TON_4_5(4.5, Map.of(0, 7, 1, 7, 2, 5, 3, 4, 4, 2, 5, 1)),
    // flat=8(5.0톤), flat=10(8.0톤)은 기준표의 "평파렛 만재" 상한까지만 맞춰서 추가함
    // (평-롤 혼재 표에는 원래 없던 칸이라, 롤파렛은 0으로만 채워둠)
    TON_5_0(5.0, mapOf(0, 10, 1, 11, 2, 9, 3, 8, 4, 6, 5, 5, 6, 3, 7, 2, 8, 0)),
    TON_8_0(8.0, mapOf(0, 14, 1, 14, 2, 12, 3, 11, 4, 9, 5, 8, 6, 6, 7, 5, 8, 3, 9, 2, 10, 0)),
    TON_11_0(11.0, mapOf(
            0, 18, 1, 18, 2, 16, 3, 15, 4, 13, 5, 12, 6, 10,
            7, 9, 8, 7, 9, 6, 10, 4, 11, 3, 12, 1)),
    TON_18_0(18.0, mapOf(
            0, 20, 1, 22, 2, 20, 3, 19, 4, 17, 5, 16, 6, 14, 7, 13,
            8, 11, 9, 10, 10, 8, 11, 7, 12, 5, 13, 4, 14, 2)),
    TON_25_0(25.0, mapOf(
            0, 21, 1, 23, 2, 21, 3, 20, 4, 18, 5, 17, 6, 15, 7, 14, 8, 12,
            9, 11, 10, 9, 11, 8, 12, 6, 13, 5, 14, 3, 15, 2));

    private final double tons;
    // key: 평파렛 개수, value: 그만큼 실었을 때 같이 실을 수 있는 최대 롤파렛 개수
    private final Map<Integer, Integer> flatToMaxRoll;

    VehicleCapacity(double tons, Map<Integer, Integer> flatToMaxRoll) {
        this.tons = tons;
        this.flatToMaxRoll = flatToMaxRoll;
    }

    public double getTons() {
        return tons;
    }

    // 평파렛 flatCount개, 롤파렛 rollCount개가 이 차량에 실릴 수 있는지 확인
    public boolean canLoad(int flatCount, int rollCount) {
        if (flatCount == 0 && rollCount == 0) {
            return true;
        }
        Integer maxRoll = flatToMaxRoll.get(flatCount);
        if (maxRoll == null) {
            return false; // 표에 없는 평파렛 개수 = 이 차량으로는 그만큼 못 실음
        }
        return rollCount <= maxRoll;
    }

    private static Map<Integer, Integer> mapOf(int... kv) {
        Map<Integer, Integer> map = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            map.put(kv[i], kv[i + 1]);
        }
        return map;
    }
}
