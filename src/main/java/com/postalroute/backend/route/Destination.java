package com.postalroute.backend.route;

public class Destination {
    private String name;            // 배송지 이름/주소
    private String companyName;     // 등록된 업체명으로 주소를 찾아온 경우에만 채워짐 (결과 화면에 "(업체명)"으로 표시)
    private double latitude;        // 배송지 위도
    private double longitude;       // 배송지 경도
    private int flatPalletCount;    // 이 배송지에서 실을 평파렛 개수
    private int rollPalletCount;    // 이 배송지에서 실을 롤파렛 개수
    private String requestedTime;   // 방문 희망시간 (없으면 null)

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public int getFlatPalletCount() {
        return flatPalletCount;
    }

    public void setFlatPalletCount(int flatPalletCount) {
        this.flatPalletCount = flatPalletCount;
    }

    public int getRollPalletCount() {
        return rollPalletCount;
    }

    public void setRollPalletCount(int rollPalletCount) {
        this.rollPalletCount = rollPalletCount;
    }

    public String getRequestedTime() {
        return requestedTime;
    }

    public void setRequestedTime(String requestedTime) {
        this.requestedTime = requestedTime;
    }
}