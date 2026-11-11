package com.postalroute.backend.route;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class KakaoGeocodingService {

    @Value("${kakao.api.key}")
    private String kakaoApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public double[] geocode(String address) {
        // .build().toUri() 로 "완성된 URI 객체"를 직접 만듦.
        // (문자열 URL로 넘기면 RestTemplate이 또 한 번 인코딩을 해버려서 %20이 %2520처럼
        //  두 번 깨지는 문제가 있었음 - URI 객체로 넘기면 그 과정을 건너뜀)
        java.net.URI uri = UriComponentsBuilder.fromUriString("https://dapi.kakao.com/v2/local/search/address.json")
                .queryParam("query", address)
                .encode(java.nio.charset.StandardCharsets.UTF_8)
                .build()
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + kakaoApiKey);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        String json = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class).getBody();

        try {
            JsonNode root = new ObjectMapper().readTree(json);
            JsonNode doc = root.get("documents").get(0);
            double longitude = doc.get("x").asDouble();
            double latitude = doc.get("y").asDouble();
            return new double[]{latitude, longitude};
        } catch (Exception e) {
            throw new RuntimeException("주소를 좌표로 변환하는데 실패했습니다: " + address, e);
        }
    }
}