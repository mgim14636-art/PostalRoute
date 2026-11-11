package com.postalroute.backend.company;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/companies")
public class CompanyAliasController {

    private final CompanyAliasRepository companyAliasRepository;

    public CompanyAliasController(CompanyAliasRepository companyAliasRepository) {
        this.companyAliasRepository = companyAliasRepository;
    }

    // 지금 로그인된 사용자의 id를 세션에서 꺼내옴. 로그인 안 돼있으면 null.
    private Long currentUserId(HttpSession session) {
        return (Long) session.getAttribute("userId");
    }

    // 등록된 업체 전체 목록 (업체 관리 화면에서 씀) - 내가 등록한 것만 보임
    @GetMapping
    public ResponseEntity<?> listAll(HttpSession session) {
        Long userId = currentUserId(session);
        if (userId == null) {
            return unauthorized();
        }
        return ResponseEntity.ok(companyAliasRepository.findByUserId(userId));
    }

    // 업체명으로 검색 (예: "흙향" 치면 등록해둔 주소를 찾아줌) - 내가 등록한 것만 검색됨
    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestParam String keyword, HttpSession session) {
        Long userId = currentUserId(session);
        if (userId == null) {
            return unauthorized();
        }
        if (keyword == null || keyword.isBlank()) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(companyAliasRepository.findByUserIdAndNameContaining(userId, keyword));
    }

    // 새 업체(이름+주소) 등록 - 내 목록 안에서 이름이나 주소가 이미 있으면 막음
    @PostMapping
    public ResponseEntity<?> register(@RequestBody CompanyAlias companyAlias, HttpSession session) {
        Long userId = currentUserId(session);
        if (userId == null) {
            return unauthorized();
        }
        if (companyAliasRepository.findByUserIdAndName(userId, companyAlias.getName()).isPresent()) {
            return conflict("동일한 이름이 이미 등록되어 있습니다.");
        }
        if (companyAliasRepository.findByUserIdAndAddressAndDetailAddress(
                userId, companyAlias.getAddress(), companyAlias.getDetailAddress()).isPresent()) {
            return conflict("동일한 주소가 이미 등록되어 있습니다.");
        }
        companyAlias.setUserId(userId);
        return ResponseEntity.ok(companyAliasRepository.save(companyAlias));
    }

    // 기존 업체 정보 수정 - 내가 등록한 업체만 수정할 수 있음
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody CompanyAlias updated, HttpSession session) {
        Long userId = currentUserId(session);
        if (userId == null) {
            return unauthorized();
        }

        CompanyAlias existing = companyAliasRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("해당 업체를 찾을 수 없습니다."));
        if (!userId.equals(existing.getUserId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "본인이 등록한 업체만 수정할 수 있습니다."));
        }

        Optional<CompanyAlias> sameName = companyAliasRepository.findByUserIdAndName(userId, updated.getName());
        if (sameName.isPresent() && !sameName.get().getId().equals(id)) {
            return conflict("동일한 이름이 이미 등록되어 있습니다.");
        }
        Optional<CompanyAlias> sameAddress = companyAliasRepository.findByUserIdAndAddressAndDetailAddress(
                userId, updated.getAddress(), updated.getDetailAddress());
        if (sameAddress.isPresent() && !sameAddress.get().getId().equals(id)) {
            return conflict("동일한 주소가 이미 등록되어 있습니다.");
        }

        existing.setName(updated.getName());
        existing.setAddress(updated.getAddress());
        existing.setDetailAddress(updated.getDetailAddress());
        return ResponseEntity.ok(companyAliasRepository.save(existing));
    }

    // 업체 삭제 - 내가 등록한 업체만 지울 수 있음
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, HttpSession session) {
        Long userId = currentUserId(session);
        if (userId == null) {
            return unauthorized();
        }

        CompanyAlias existing = companyAliasRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("해당 업체를 찾을 수 없습니다."));
        if (!userId.equals(existing.getUserId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "본인이 등록한 업체만 삭제할 수 있습니다."));
        }

        companyAliasRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    private ResponseEntity<?> conflict(String message) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", message));
    }

    private ResponseEntity<?> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "로그인이 필요합니다."));
    }
}
