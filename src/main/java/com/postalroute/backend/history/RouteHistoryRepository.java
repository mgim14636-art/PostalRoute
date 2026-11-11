package com.postalroute.backend.history;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface RouteHistoryRepository extends JpaRepository<RouteHistory, Long> {

    // 특정 사용자가 계산했던 이력들을 최신순으로
    List<RouteHistory> findByUserIdOrderByCreatedAtDesc(Long userId);

    // 특정 사용자가, 지정한 기간 안에 계산했던 이력들만 최신순으로
    // (이력이 수천 개로 늘어나도 한 번에 다 안 불러오고 기간만큼만 가져오기 위함)
    List<RouteHistory> findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            Long userId, LocalDateTime start, LocalDateTime end);
}
