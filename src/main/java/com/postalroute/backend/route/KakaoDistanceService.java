package com.postalroute.backend.route;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component // Spring이 관리하는 객체로 등록
public class KakaoDistanceService {

    @Value("${kakao.api.key}") // application.properties에 적어둔 카카오 API 키를 자동으로 가져옴
    private String kakaoApiKey;

    private final RestTemplate restTemplate = new RestTemplate(); // 외부 API 호출용 도구

    // 같은 두 좌표 쌍을 또 물어보지 않게 캐시해둠 (distanceKm/travelMinutes가 둘 다 이 캐시를 거쳐감 -
    // 카카오 응답 하나에 거리/시간이 같이 들어있어서, 좌표 쌍이 같으면 한 번만 물어보면 됨)
    private final Map<String, JsonNode> summaryCache = new ConcurrentHashMap<>();

    // 카카오가 "거리가 너무 멀어서 계산 못 함" 같은 에러를 줬을 때 대신 쓰는 값.
    // 일부러 아주 크게 잡아서, "사실상 갈 수 없는 곳"으로 취급되게 함(그래서 그 쪽으로는 안 보내짐).
    private static final double UNREACHABLE_KM = 9999.0;
    private static final double UNREACHABLE_MINUTES = 99999.0;

    // 두 좌표 사이의 실제 도로거리(km)를 카카오 모빌리티 API로 계산
    public double distanceKm(double startLat, double startLon, double endLat, double endLon) {
        if (Math.abs(startLat - endLat) < 0.0001 && Math.abs(startLon - endLon) < 0.0001) {
            return 0.0;
        }
        JsonNode summary = fetchSummary(startLat, startLon, endLat, endLon);
        if (summary == null) {
            return UNREACHABLE_KM;
        }
        try {
            double distanceMeters = summary.get("distance").asDouble();
            return distanceMeters / 1000.0; // 미터 → km로 변환
        } catch (Exception e) {
            throw new RuntimeException("거리 계산 실패", e);
        }
    }

    // 두 좌표 사이의 실제 이동 시간(분)을 카카오 모빌리티 API로 계산
    // (시간이 비는 틈에 시간 없는 배송지를 끼워 넣을 수 있는지 확인할 때 씀)
    public double travelMinutes(double startLat, double startLon, double endLat, double endLon) {
        if (Math.abs(startLat - endLat) < 0.0001 && Math.abs(startLon - endLon) < 0.0001) {
            return 0.0;
        }
        JsonNode summary = fetchSummary(startLat, startLon, endLat, endLon);
        if (summary == null) {
            return UNREACHABLE_MINUTES;
        }
        try {
            double durationSeconds = summary.get("duration").asDouble();
            return durationSeconds / 60.0; // 초 → 분으로 변환
        } catch (Exception e) {
            throw new RuntimeException("이동 시간 계산 실패", e);
        }
    }

    // distanceKm/travelMinutes가 공통으로 쓰는 부분: 카카오에 길찾기 요청해서 summary만 꺼내옴.
    // 카카오가 "거리 제한 초과" 같은 4xx 에러를 주면, 전체 요청을 죽이지 않고 null을 돌려줌.
    private JsonNode fetchSummary(double startLat, double startLon, double endLat, double endLon) {
        String cacheKey = startLat + "," + startLon + "->" + endLat + "," + endLon;
        JsonNode cached = summaryCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        String url = String.format(
                "https://apis-navi.kakaomobility.com/v1/directions?origin=%f,%f&destination=%f,%f",
                startLon, startLat, endLon, endLat);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + kakaoApiKey);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            String json = restTemplate.exchange(url, HttpMethod.GET, entity, String.class).getBody();
            JsonNode root = new ObjectMapper().readTree(json);
            JsonNode summary = root.get("routes").get(0).get("summary");
            summaryCache.put(cacheKey, summary);
            return summary;
        } catch (HttpClientErrorException e) {
            // 예: "경로의 총 거리가 허용된 최대 거리를 초과했습니다" 같은 경우
            return null;
        }
    }
}