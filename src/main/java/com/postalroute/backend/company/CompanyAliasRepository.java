package com.postalroute.backend.company;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompanyAliasRepository extends JpaRepository<CompanyAlias, Long> {

    // 특정 사용자가 등록한 업체 전체 목록
    List<CompanyAlias> findByUserId(Long userId);

    // 특정 사용자가 등록한 업체 중에서, 업체명에 입력한 글자가 들어가는 것들을 찾아줌
    List<CompanyAlias> findByUserIdAndNameContaining(Long userId, String keyword);

    // 이름/주소 중복 확인용 (같은 사용자 안에서만 중복인지 확인함 - 다른 사람끼리는 겹쳐도 됨)
    Optional<CompanyAlias> findByUserIdAndName(Long userId, String name);

    // 주소가 같아도 상세주소(동/호수)가 다르면 다른 곳으로 봄 (같은 건물 다른 호수 등록 가능하게)
    Optional<CompanyAlias> findByUserIdAndAddressAndDetailAddress(Long userId, String address, String detailAddress);
}
