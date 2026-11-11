package com.postalroute.backend.history;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.LocalDateTime;

// 동선 최적화를 계산할 때마다 자동으로 한 줄씩 쌓이는 이력
@Entity
public class RouteHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;            // 이 계산을 실행한 사용자
    private LocalDateTime createdAt; // 계산한 시각

    private int destinationCount;    // 입력했던 집하 동선 개수
    private int vehicleCount;        // 배정된 차량 대수
    private double totalDistanceKm;  // 전체 이동 거리 합

    @Column(columnDefinition = "LONGTEXT")
    private String resultJson; // /api/route/optimize가 돌려줬던 결과(차량별 경로) 그대로 저장해둠 - 상세보기용

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public int getDestinationCount() {
        return destinationCount;
    }

    public void setDestinationCount(int destinationCount) {
        this.destinationCount = destinationCount;
    }

    public int getVehicleCount() {
        return vehicleCount;
    }

    public void setVehicleCount(int vehicleCount) {
        this.vehicleCount = vehicleCount;
    }

    public double getTotalDistanceKm() {
        return totalDistanceKm;
    }

    public void setTotalDistanceKm(double totalDistanceKm) {
        this.totalDistanceKm = totalDistanceKm;
    }

    public String getResultJson() {
        return resultJson;
    }

    public void setResultJson(String resultJson) {
        this.resultJson = resultJson;
    }
}
