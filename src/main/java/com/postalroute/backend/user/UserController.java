package com.postalroute.backend.user;

import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

@RestController
public class UserController {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping("/api/signup") // 회원가입
    public User signup(@RequestBody User user) {
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new RuntimeException("이미 사용 중인 아이디입니다.");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    @PostMapping("/api/login") // 로그인
    public User login(@RequestBody User loginRequest, HttpSession session){
        User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new RuntimeException("아이디가 존재하지 않습니다."));

        if(!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new RuntimeException("비밀번호가 일치하지 않습니다.");
        }

        // 로그인 성공 - 이 브라우저(세션)에 "지금 로그인한 사람은 이 id다"라고 서버가 기억해둠.
        // 이후 다른 요청들은 이 세션을 보고 "누가 요청했는지" 알 수 있게 됨.
        session.setAttribute("userId", user.getId());

        return user;
    }

    @PostMapping("/api/logout") // 로그아웃 - 서버가 기억해둔 로그인 상태를 지움
    public void logout(HttpSession session) {
        session.invalidate();
    }

    @PostMapping("/api/find-id") // ID 찾기
    public User find_id(@RequestBody User findIdrequset){
        return userRepository.findByNameAndPhoneNumber(findIdrequset.getName(),findIdrequset.getPhoneNumber())
                .orElseThrow(() -> new RuntimeException("일치하는 회원이 존재하지 않습니다."));
    }


    @PostMapping("/api/find-pw/verify") // 비밀번호 찾기 1단계
    public User find_pw_verify(@RequestBody User request) {
        User user = userRepository.findByNameAndUsernameAndPhoneNumber(request.getName(), request.getUsername(), request.getPhoneNumber())
                .orElseThrow(() -> new RuntimeException("일치하는 회원이 존재하지 않습니다."));

        user.setPassword(null);
        return user;
    }

    @PostMapping("/api/find-pw") // 비밀번호 찾기 1단계 -> 비밀번호 변경
    public User find_pw(@RequestBody User findPwrequset){
        User user = userRepository.findByNameAndUsernameAndPhoneNumber(findPwrequset.getName(), findPwrequset.getUsername(), findPwrequset.getPhoneNumber())
                .orElseThrow(() -> new RuntimeException("일치하는 회원이 존재하지 않습니다."));

        user.setPassword(passwordEncoder.encode(findPwrequset.getPassword()));
        userRepository.save(user);

        user.setPassword(null);
        return user;
    }


}


