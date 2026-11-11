package com.postalroute.backend.company;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

// "흙향" 같은 업체 이름(별명)과 실제 주소를 짝지어서 저장해두는 주소록 한 줄
@Entity
public class CompanyAlias {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;          // 업체명(별명), 예: "흙향"
    private String address;       // 실제 주소, 예: "전라남도 나주시 산포면 산포로 371"
    private String detailAddress; // 상세주소(동/호수 등), 없으면 null
    private Long userId;          // 이 업체를 등록한 사용자(User)의 id - 이 값으로 "내 업체만" 걸러서 보여줌

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getDetailAddress() {
        return detailAddress;
    }

    public void setDetailAddress(String detailAddress) {
        this.detailAddress = detailAddress;
    }
}
