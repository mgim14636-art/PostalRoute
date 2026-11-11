package com.postalroute.backend.history;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/route/history")
public class RouteHistoryController {

    private final RouteHistoryRepository routeHistoryRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RouteHistoryController(RouteHistoryRepository routeHistoryRepository) {
        this.routeHistoryRepository = routeHistoryRepository;
    }

    private Long currentUserId(HttpSession session) {
        return (Long) session.getAttribute("userId");
    }

    // 이력 목록 (요약만) - 내가 계산했던 것만, 최신순
    // start/end(yyyy-MM-dd)를 안 주면 "오늘" 하루치만 돌려줌 - 이력이 수천 건 쌓여도 한 번에 다 안 불러오기 위함
    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            HttpSession session) {
        Long userId = currentUserId(session);
        if (userId == null) {
            return unauthorized();
        }

        LocalDate startDate = (start != null && !start.isBlank()) ? LocalDate.parse(start) : LocalDate.now();
        LocalDate endDate = (end != null && !end.isBlank()) ? LocalDate.parse(end) : LocalDate.now();
        LocalDateTime rangeStart = startDate.atStartOfDay();
        LocalDateTime rangeEnd = endDate.atTime(23, 59, 59);

        List<RouteHistory> histories = routeHistoryRepository
                .findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(userId, rangeStart, rangeEnd);
        List<Map<String, Object>> summaries = histories.stream().map(h -> {
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("id", h.getId());
            summary.put("createdAt", h.getCreatedAt());
            summary.put("destinationCount", h.getDestinationCount());
            summary.put("vehicleCount", h.getVehicleCount());
            summary.put("totalDistanceKm", h.getTotalDistanceKm());
            return summary;
        }).toList();

        return ResponseEntity.ok(summaries);
    }

    // 이력 상세 (그 날 계산된 차량별 전체 경로까지) - 본인이 만든 이력만 볼 수 있음
    @GetMapping("/{id}")
    public ResponseEntity<?> detail(@PathVariable Long id, HttpSession session) {
        Long userId = currentUserId(session);
        if (userId == null) {
            return unauthorized();
        }

        RouteHistory history = routeHistoryRepository.findById(id).orElse(null);
        if (history == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "해당 이력을 찾을 수 없습니다."));
        }
        if (!userId.equals(history.getUserId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "본인이 계산한 이력만 볼 수 있습니다."));
        }

        JsonNode vehicleRoutes = objectMapper.readTree(history.getResultJson());

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("id", history.getId());
        detail.put("createdAt", history.getCreatedAt());
        detail.put("destinationCount", history.getDestinationCount());
        detail.put("vehicleCount", history.getVehicleCount());
        detail.put("totalDistanceKm", history.getTotalDistanceKm());
        detail.put("vehicleRoutes", vehicleRoutes);
        return ResponseEntity.ok(detail);
    }

    // 이력 삭제 - 본인이 계산한 이력만 지울 수 있음
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, HttpSession session) {
        Long userId = currentUserId(session);
        if (userId == null) {
            return unauthorized();
        }

        RouteHistory history = routeHistoryRepository.findById(id).orElse(null);
        if (history == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "해당 이력을 찾을 수 없습니다."));
        }
        if (!userId.equals(history.getUserId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "본인이 계산한 이력만 삭제할 수 있습니다."));
        }

        routeHistoryRepository.delete(history);
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<?> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "로그인이 필요합니다."));
    }
}
