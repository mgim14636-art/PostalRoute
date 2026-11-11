package com.postalroute.backend.route;

import com.postalroute.backend.history.RouteHistory;
import com.postalroute.backend.history.RouteHistoryRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class RouteController {

    private final KakaoGeocodingService kakaoGeocodingService;
    private final DestinationClusterer destinationClusterer;
    private final RouteBuilder routeBuilder;
    private final RouteHistoryRepository routeHistoryRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RouteController(KakaoGeocodingService kakaoGeocodingService,
                           DestinationClusterer destinationClusterer,
                           RouteBuilder routeBuilder,
                           RouteHistoryRepository routeHistoryRepository) {
        this.kakaoGeocodingService = kakaoGeocodingService;
        this.destinationClusterer = destinationClusterer;
        this.routeBuilder = routeBuilder;
        this.routeHistoryRepository = routeHistoryRepository;
    }

    // 주소 하나를 좌표로 바꿔주는 API (화면에서 주소 입력할 때마다 호출할 것)
    @PostMapping("/api/geocode")
    public Map<String, Double> geocode(@RequestBody Map<String, String> request) {
        double[] coords = kakaoGeocodingService.geocode(request.get("address"));
        Map<String, Double> result = new HashMap<>();
        result.put("latitude", coords[0]);
        result.put("longitude", coords[1]);
        return result;
    }

    // 진짜 최적화 실행하는 API
    @PostMapping("/api/route/optimize")
    public List<VehicleRoute> optimize(@RequestBody RouteRequest request, HttpSession session) {
        List<List<Destination>> clusters = destinationClusterer.cluster(request.getDestinations());

        List<VehicleRoute> results = new ArrayList<>();
        for (List<Destination> cluster : clusters) {
            VehicleRoute route = routeBuilder.buildRoute(cluster);
            results.add(route);
        }

        // 로그인한 사용자면, 계산할 때마다 이력에 자동으로 남겨둠
        Long userId = (Long) session.getAttribute("userId");
        if (userId != null) {
            saveHistory(userId, request, results);
        }

        return results;
    }

    private void saveHistory(Long userId, RouteRequest request, List<VehicleRoute> results) {
        double totalDistanceKm = 0;
        for (VehicleRoute route : results) {
            for (Double leg : route.getLegDistances()) {
                totalDistanceKm += leg;
            }
        }

        RouteHistory history = new RouteHistory();
        history.setUserId(userId);
        history.setCreatedAt(LocalDateTime.now());
        history.setDestinationCount(request.getDestinations().size());
        history.setVehicleCount(results.size());
        history.setTotalDistanceKm(totalDistanceKm);
        history.setResultJson(objectMapper.writeValueAsString(results));
        routeHistoryRepository.save(history);
    }
}