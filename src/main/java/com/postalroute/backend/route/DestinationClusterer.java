package com.postalroute.backend.route;

import com.postalroute.backend.vehicle.VehicleCapacity;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class DestinationClusterer {

    private final KakaoDistanceService kakaoDistanceService;

    // "진짜 최선의 조합"을 전부 따져보는 건, 이번 시간대 목적지 수가 이 숫자 이하일 때만 함.
    // (6개면 최대 6! = 720가지라 순식간에 끝남. 더 많아지면 조합 수가 급격히 늘어나서 기존 방식으로 처리함)
    private static final int MAX_OPTIMAL_GROUP_SIZE = 6;

    // 시간 없는 집하지를 들렀다 갈 때, 도착해서 파렛 싣고 내리는 데 걸리는 시간(분) - 이동시간과 별개로 추가해줌
    private static final int LOADING_MINUTES = 15;

    public DestinationClusterer(KakaoDistanceService kakaoDistanceService) {
        this.kakaoDistanceService = kakaoDistanceService;
    }

    // 배송지들을 "시간"을 기준으로 차량별 경로로 묶음.
    // 같은 시간에 여러 곳을 가야 하면 그만큼 차량을 나누고,
    // 그다음 시간부터는 가장 가까운 위치에 있는 차량에게 배정한다.
    public List<List<Destination>> cluster(List<Destination> destinations) {
        List<Destination> timed = new ArrayList<>();
        List<Destination> untimed = new ArrayList<>();
        for (Destination d : destinations) {
            if (d.getRequestedTime() != null && !d.getRequestedTime().isEmpty()) {
                timed.add(d);
            } else {
                untimed.add(d);
            }
        }

        // 시간별로 그룹화 (TreeMap이라 키 기준으로 자동 오름차순 정렬됨)
        Map<String, List<Destination>> byTime = new TreeMap<>();
        for (Destination d : timed) {
            byTime.computeIfAbsent(d.getRequestedTime(), k -> new ArrayList<>()).add(d);
        }

        List<List<Destination>> vehicleRoutes = new ArrayList<>();      // 차량별 경로
        List<double[]> vehicleLastPosition = new ArrayList<>();         // 차량별 마지막 위치[위도,경도]

        boolean firstGroup = true;
        for (Map.Entry<String, List<Destination>> entry : byTime.entrySet()) {
            List<Destination> group = entry.getValue();

            if (firstGroup) {
                // 제일 이른 시간: 그룹 안 배송지 개수만큼 새 차량 생성
                for (Destination d : group) {
                    List<Destination> route = new ArrayList<>();
                    route.add(d);
                    vehicleRoutes.add(route);
                    vehicleLastPosition.add(new double[]{d.getLatitude(), d.getLongitude()});
                }
                firstGroup = false;
                continue;
            }

            // 이번 시간 그룹: 가장 가까운 차량한테 하나씩 배정 (차량 하나당 이번 시간엔 한 곳만)
            List<Destination> remaining = new ArrayList<>(group);

            // 이번 시간에 쓸 수 있는(아직 이번 시간대에 안 쓴) 차량 번호 목록
            List<Integer> availableVehicles = new ArrayList<>();
            for (int v = 0; v < vehicleRoutes.size(); v++) {
                availableVehicles.add(v);
            }

            // 안전한 범위(목적지 수가 차량 수보다 적거나 같고, 너무 많지 않을 때)에는
            // 가능한 조합을 전부 따져서 "전체 이동 거리 합이 제일 작은" 진짜 최선의 배정을 찾음.
            // (그 외의 경우 - 새 차량이 필요하거나 조합이 너무 많을 때 - 는 기존 방식 그대로 사용)
            if (remaining.size() <= availableVehicles.size() && remaining.size() <= MAX_OPTIMAL_GROUP_SIZE) {
                Map<Destination, Integer> bestAssignment =
                        findBestAssignment(remaining, availableVehicles, vehicleLastPosition);
                for (Map.Entry<Destination, Integer> assigned : bestAssignment.entrySet()) {
                    Destination d = assigned.getKey();
                    int v = assigned.getValue();
                    vehicleRoutes.get(v).add(d);
                    vehicleLastPosition.set(v, new double[]{d.getLatitude(), d.getLongitude()});
                }
                continue;
            }

            Set<Integer> usedVehicles = new HashSet<>();

            while (!remaining.isEmpty()) {
                double bestDist = Double.MAX_VALUE;
                int bestVehicle = -1;
                Destination bestDest = null;

                for (Destination d : remaining) {
                    for (int v = 0; v < vehicleRoutes.size(); v++) {
                        if (usedVehicles.contains(v)) continue;
                        double[] pos = vehicleLastPosition.get(v);
                        double dist = kakaoDistanceService.distanceKm(pos[0], pos[1], d.getLatitude(), d.getLongitude());
                        if (dist < bestDist) {
                            bestDist = dist;
                            bestVehicle = v;
                            bestDest = d;
                        }
                    }
                }

                if (bestVehicle == -1) {
                    // 배정 가능한(이번 시간에 아직 안 쓴) 차량이 없음 -> 새 차량 생성
                    Destination d = remaining.get(0);
                    List<Destination> route = new ArrayList<>();
                    route.add(d);
                    vehicleRoutes.add(route);
                    vehicleLastPosition.add(new double[]{d.getLatitude(), d.getLongitude()});
                    // 방금 만든 이 차량도 "이번 시간대에 이미 썼다"고 표시해야
                    // 같은 시간대의 다음 배송지가 또 이 차량에 배정되는 걸 막을 수 있음
                    usedVehicles.add(vehicleRoutes.size() - 1);
                    remaining.remove(d);
                } else {
                    vehicleRoutes.get(bestVehicle).add(bestDest);
                    vehicleLastPosition.set(bestVehicle, new double[]{bestDest.getLatitude(), bestDest.getLongitude()});
                    usedVehicles.add(bestVehicle);
                    remaining.remove(bestDest);
                }
            }
        }

        // 시간이 지정 안 된 배송지들은, 모든 차량 경로의 모든 위치에 넣어보고
        // "거리가 제일 적게 늘어나는 자리"에 끼워 넣음 (가까운 곳에 들렀다 가는 효과).
        // 단, 끼워 넣을 자리의 앞/뒤가 둘 다 시간이 정해진 곳이면, 그 사이 남는 시간 안에
        // 실제로 들렀다 갈 수 있는지(이동 시간이 남는 시간보다 적은지)도 같이 확인함.
        for (Destination d : untimed) {
            if (vehicleRoutes.isEmpty()) {
                List<Destination> route = new ArrayList<>();
                route.add(d);
                vehicleRoutes.add(route);
                continue;
            }

            InsertionSpot best = findBestInsertionSpot(vehicleRoutes, d, true); // 1차: 시간 맞는 자리만
            if (best == null) {
                best = findBestInsertionSpot(vehicleRoutes, d, false); // 2차: 시간 상관없이 제일 가까운 자리
            }

            vehicleRoutes.get(best.vehicleIndex).add(best.position, d);
        }

        // 한 차량에 실린 양이 제일 큰 차(25톤)로도 못 싣을 만큼 많아지면,
        // 방문 순서는 그대로 둔 채로 감당할 수 있는 만큼씩 끊어서 차량을 더 나눔
        return splitOverCapacityRoutes(vehicleRoutes);
    }

    // 차량 경로들을 돌면서, 용량을 넘는 분량은 일단 "오갈 데 없는 목적지" 명단으로 빼놓고,
    // 그 목적지들을 새 차를 만들기 전에 먼저 "기존 차 중 용량/시간이 맞는 곳"에 끼워넣어 본다.
    // (예전엔 넘치면 무조건 새 차를 만들었는데, 그러면 목적지 하나 때문에 차 한 대를 통째로
    //  더 써야 하는 낭비가 생겨서, 가까운 기존 차한테 넘겨줄 수 있으면 그쪽으로 먼저 보내도록 바꿈)
    private List<List<Destination>> splitOverCapacityRoutes(List<List<Destination>> routes) {
        List<List<Destination>> keptRoutes = new ArrayList<>();
        List<Destination> overflowDestinations = new ArrayList<>();

        // 1. 각 경로를 앞에서부터 채워보고, 원래 차로 감당되는 부분만 남기고 나머지는 전부
        //    "오갈 데 없는 목적지" 명단에 담아둔다 (여러 개가 같이 넘쳤어도 하나씩 따로 담음 -
        //    그래야 각자한테 제일 잘 맞는 자리를 따로따로 찾아줄 수 있음).
        for (List<Destination> route : routes) {
            List<List<Destination>> split = splitSingleRouteByCapacity(route);
            keptRoutes.add(split.get(0));
            for (int i = 1; i < split.size(); i++) {
                overflowDestinations.addAll(split.get(i));
            }
        }

        // 2. 오갈 데 없는 목적지들을, 기존 차 중 "용량도 되고 시간도 맞는" 곳 중에서
        //    거리가 제일 적게 늘어나는 자리에 끼워넣는다. 그런 자리가 끝까지 하나도 없을 때만
        //    어쩔 수 없이 이 목적지 하나만 싣는 새 차를 만든다.
        for (Destination d : overflowDestinations) {
            InsertionSpot spot = findBestReassignmentSpot(keptRoutes, d);
            if (spot != null) {
                keptRoutes.get(spot.vehicleIndex).add(spot.position, d);
            } else {
                List<Destination> newRoute = new ArrayList<>();
                newRoute.add(d);
                keptRoutes.add(newRoute);
            }
        }

        return keptRoutes;
    }

    // routes 중에서 d를 받아줄 수 있는 "제일 저렴한(거리가 적게 늘어나는)" 자리를 찾는다.
    // 용량이 안 되는 차는 아예 후보에서 제외하고, 시간 제약도 같이 확인한다.
    private InsertionSpot findBestReassignmentSpot(List<List<Destination>> routes, Destination d) {
        double bestExtraCost = Double.MAX_VALUE;
        InsertionSpot best = null;

        for (int v = 0; v < routes.size(); v++) {
            List<Destination> route = routes.get(v);
            int[] totals = totalPallets(route);
            if (!canAnyVehicleLoad(totals[0] + d.getFlatPalletCount(), totals[1] + d.getRollPalletCount())) {
                continue; // 이 차는 d까지 더하면 어떤 톤수로도 못 실음 -> 후보에서 제외
            }
            for (int pos = 0; pos <= route.size(); pos++) {
                if (!canReassignAt(route, pos, d)) continue;
                double extraCost = insertionCost(route, pos, d);
                if (extraCost < bestExtraCost) {
                    bestExtraCost = extraCost;
                    best = new InsertionSpot(v, pos);
                }
            }
        }
        return best;
    }

    // route에 실린 평파렛/롤파렛 총합을 구한다 ([0]=평파렛 합, [1]=롤파렛 합)
    private int[] totalPallets(List<Destination> route) {
        int flat = 0, roll = 0;
        for (Destination d : route) {
            flat += d.getFlatPalletCount();
            roll += d.getRollPalletCount();
        }
        return new int[]{flat, roll};
    }

    // route의 pos 자리에 d를 끼워넣어도 시간상 말이 되는지 확인한다.
    // d 자신에게 희망시간이 있으면, 끼워넣는 자리의 앞/뒤 순서가 그 시간과 어긋나지 않는지부터 먼저 보고
    // (예: 9시에 가야 할 곳을 10시 목적지 뒤에 끼워넣으면 안 되니까),
    // 그다음 기존 isTimeFeasible로 "실제로 그 틈에 들렀다 갈 시간이 되는지"까지 확인한다.
    private boolean canReassignAt(List<Destination> route, int pos, Destination d) {
        Integer dMinutes = parseMinutes(d.getRequestedTime());
        if (dMinutes != null) {
            if (pos > 0) {
                Integer beforeMinutes = parseMinutes(route.get(pos - 1).getRequestedTime());
                if (beforeMinutes != null && dMinutes < beforeMinutes) return false;
            }
            if (pos < route.size()) {
                Integer afterMinutes = parseMinutes(route.get(pos).getRequestedTime());
                if (afterMinutes != null && dMinutes > afterMinutes) return false;
            }
        }
        return isTimeFeasible(route, pos, d);
    }

    // 방문 순서를 유지한 채로, 앞에서부터 차곡차곡 실어보다가
    // 다음 걸 실으면 어떤 차로도 못 싣게 되는 지점에서 끊어서 새 차량으로 넘김
    private List<List<Destination>> splitSingleRouteByCapacity(List<Destination> route) {
        List<List<Destination>> subRoutes = new ArrayList<>();
        List<Destination> current = new ArrayList<>();
        int currentFlat = 0;
        int currentRoll = 0;

        for (Destination d : route) {
            int nextFlat = currentFlat + d.getFlatPalletCount();
            int nextRoll = currentRoll + d.getRollPalletCount();

            if (!current.isEmpty() && !canAnyVehicleLoad(nextFlat, nextRoll)) {
                subRoutes.add(current);
                current = new ArrayList<>();
                nextFlat = d.getFlatPalletCount();
                nextRoll = d.getRollPalletCount();
            }

            current.add(d);
            currentFlat = nextFlat;
            currentRoll = nextRoll;
        }

        if (!current.isEmpty()) {
            subRoutes.add(current);
        }
        return subRoutes;
    }

    // 가장 큰 차량(25톤)을 포함해서, 이 양을 실을 수 있는 차량이 하나라도 있는지 확인
    private boolean canAnyVehicleLoad(int flatCount, int rollCount) {
        for (VehicleCapacity vehicle : VehicleCapacity.values()) {
            if (vehicle.canLoad(flatCount, rollCount)) {
                return true;
            }
        }
        return false;
    }

    // 끼워 넣을 위치를 찾아주는 공통 로직. requireTimeFeasible=true면 시간이 안 맞는 자리는 후보에서 제외함.
    private InsertionSpot findBestInsertionSpot(List<List<Destination>> vehicleRoutes, Destination d, boolean requireTimeFeasible) {
        double bestExtraCost = Double.MAX_VALUE;
        InsertionSpot best = null;

        for (int v = 0; v < vehicleRoutes.size(); v++) {
            List<Destination> route = vehicleRoutes.get(v);
            // pos는 1부터 시작함 (0번째 자리, 즉 맨 앞은 제외).
            // route의 맨 처음은 항상 "제일 이른 시간" 배송지라서 출발점이어야 하니,
            // 시간 없는 배송지를 그 앞에 끼워 넣으면 출발 순서가 깨짐.
            for (int pos = 1; pos <= route.size(); pos++) {
                if (requireTimeFeasible && !isTimeFeasible(route, pos, d)) {
                    continue;
                }
                double extraCost = insertionCost(route, pos, d);
                if (extraCost < bestExtraCost) {
                    bestExtraCost = extraCost;
                    best = new InsertionSpot(v, pos);
                }
            }
        }
        return best;
    }

    // route의 pos 자리(앞뒤가 둘 다 시간 지정된 곳일 때)에 d를 들렀다 가도
    // 다음 시간까지 남는 시간 안에 도착할 수 있는지 확인
    private boolean isTimeFeasible(List<Destination> route, int pos, Destination d) {
        if (pos == 0 || pos == route.size()) {
            return true; // 맨 앞/맨 뒤에 추가하는 건 시간 제약이 없음 (기존 방식 그대로)
        }

        Destination before = route.get(pos - 1);
        Destination after = route.get(pos);
        Integer beforeMinutes = parseMinutes(before.getRequestedTime());
        Integer afterMinutes = parseMinutes(after.getRequestedTime());
        if (beforeMinutes == null || afterMinutes == null) {
            return true; // 앞뒤 중 하나라도 시간이 없으면 비교할 기준이 없으니 통과시킴
        }

        // before에서 d 들렀다가 after까지 가는 데 실제로 걸리는 시간(이동시간 + 그 자리에서 싣고 내리는 시간)이,
        // before~after 사이 남는 시간 안에 들어오는지 직접 비교함
        // (예전엔 "직통으로 가는 시간보다 얼마나 더 걸리는지"로 비교해서, 그만큼 기준이 느슨했음)
        int availableMinutes = afterMinutes - beforeMinutes;
        double viaMinutes = kakaoDistanceService.travelMinutes(
                before.getLatitude(), before.getLongitude(), d.getLatitude(), d.getLongitude())
                + LOADING_MINUTES
                + kakaoDistanceService.travelMinutes(
                d.getLatitude(), d.getLongitude(), after.getLatitude(), after.getLongitude());

        return viaMinutes <= availableMinutes;
    }

    // "09:00" 같은 문자열을 자정부터 몇 분 지났는지(540분)로 바꿔줌
    private Integer parseMinutes(String time) {
        if (time == null || time.isEmpty()) return null;
        String[] parts = time.split(":");
        return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
    }

    // 어느 차량(vehicleIndex)의 몇 번째 자리(position)에 끼워 넣을지 담아두는 작은 상자
    private static class InsertionSpot {
        final int vehicleIndex;
        final int position;

        InsertionSpot(int vehicleIndex, int position) {
            this.vehicleIndex = vehicleIndex;
            this.position = position;
        }
    }

    // destinations(목적지들)를 candidateVehicles(쓸 수 있는 차량들)에 하나씩 짝지어주되,
    // "전체 이동 거리 합"이 제일 작아지는 조합을 모든 경우의 수를 다 따져서 찾아줌.
    // (destinations.size() <= candidateVehicles.size() 일 때만 쓰는 함수)
    private Map<Destination, Integer> findBestAssignment(
            List<Destination> destinations, List<Integer> candidateVehicles, List<double[]> vehicleLastPosition) {

        int rowCount = destinations.size();
        int colCount = candidateVehicles.size();

        // 1. 거리표를 미리 한 번만 계산해둠 (카카오 API를 중복으로 부르지 않기 위함)
        double[][] cost = new double[rowCount][colCount];
        for (int i = 0; i < rowCount; i++) {
            Destination d = destinations.get(i);
            for (int j = 0; j < colCount; j++) {
                double[] pos = vehicleLastPosition.get(candidateVehicles.get(j));
                cost[i][j] = kakaoDistanceService.distanceKm(pos[0], pos[1], d.getLatitude(), d.getLongitude());
            }
        }

        // 2. 미리 계산해둔 거리표만 가지고(API 호출 없이) 모든 조합을 따져서 최선을 찾음
        int[] bestColumnPerRow = new int[rowCount];
        double[] bestTotalCost = {Double.MAX_VALUE};
        int[] currentColumnPerRow = new int[rowCount];
        boolean[] columnUsed = new boolean[colCount];
        searchBestAssignment(cost, 0, columnUsed, currentColumnPerRow, 0.0, bestTotalCost, bestColumnPerRow);

        Map<Destination, Integer> result = new HashMap<>();
        for (int i = 0; i < rowCount; i++) {
            result.put(destinations.get(i), candidateVehicles.get(bestColumnPerRow[i]));
        }
        return result;
    }

    // findBestAssignment가 쓰는 재귀 탐색. row(목적지)마다 아직 안 쓴 column(차량)을 하나씩 다 시도해보면서
    // 지금까지의 합이 이미 지금까지 찾은 최선보다 나쁘면 그 가지는 더 안 따져봄(가지치기)
    private void searchBestAssignment(double[][] cost, int row, boolean[] columnUsed, int[] currentColumnPerRow,
                                       double costSoFar, double[] bestTotalCost, int[] bestColumnPerRow) {
        int rowCount = cost.length;
        if (row == rowCount) {
            if (costSoFar < bestTotalCost[0]) {
                bestTotalCost[0] = costSoFar;
                System.arraycopy(currentColumnPerRow, 0, bestColumnPerRow, 0, rowCount);
            }
            return;
        }

        int colCount = cost[row].length;
        for (int col = 0; col < colCount; col++) {
            if (columnUsed[col]) continue;
            double newCost = costSoFar + cost[row][col];
            if (newCost >= bestTotalCost[0]) continue; // 이미 더 나쁘면 끝까지 안 가봐도 됨

            columnUsed[col] = true;
            currentColumnPerRow[row] = col;
            searchBestAssignment(cost, row + 1, columnUsed, currentColumnPerRow, newCost, bestTotalCost, bestColumnPerRow);
            columnUsed[col] = false;
        }
    }

    // route의 pos 위치에 d를 끼워넣으면 거리가 얼마나 늘어나는지 계산
    private double insertionCost(List<Destination> route, int pos, Destination d) {
        if (route.isEmpty()) return 0;

        if (pos == 0) {
            Destination first = route.get(0);
            return kakaoDistanceService.distanceKm(
                    d.getLatitude(), d.getLongitude(), first.getLatitude(), first.getLongitude());
        }
        if (pos == route.size()) {
            Destination last = route.get(route.size() - 1);
            return kakaoDistanceService.distanceKm(
                    last.getLatitude(), last.getLongitude(), d.getLatitude(), d.getLongitude());
        }

        Destination before = route.get(pos - 1);
        Destination after = route.get(pos);
        double originalDist = kakaoDistanceService.distanceKm(
                before.getLatitude(), before.getLongitude(), after.getLatitude(), after.getLongitude());
        double viaD = kakaoDistanceService.distanceKm(
                before.getLatitude(), before.getLongitude(), d.getLatitude(), d.getLongitude())
                + kakaoDistanceService.distanceKm(
                d.getLatitude(), d.getLongitude(), after.getLatitude(), after.getLongitude());
        return viaD - originalDist;
    }
}
