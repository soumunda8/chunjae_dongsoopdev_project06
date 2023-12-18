package com.edutech.team26.biz;

import com.edutech.team26.component.CaptchaSetting;
import com.edutech.team26.component.MailComponent;
import com.edutech.team26.constant.MemberCode;
import com.edutech.team26.constant.MemberRole;
import com.edutech.team26.domain.Member;
import com.edutech.team26.dto.MemberDTO;
import com.edutech.team26.dto.MemberJoinDTO;
import com.edutech.team26.dto.MemberSecurityDTO;
import com.edutech.team26.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Log4j2
@Service
@Transactional
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    @Value("${server.port}")
    private String serverPost;

    private final ModelMapper modelMapper;

    private final MemberRepository memberRepository;

    private final PasswordEncoder passwordEncoder;

    private final MailComponent mailComponent;

    private final CaptchaSetting captchaSetting;

    @Override
    public boolean memberDupValidation(String email) throws Exception {

        boolean pass = true;

        Optional<Member> checkMember = memberRepository.findByEmail(email);
        if(checkMember.isPresent()){
            pass = false;
        }

        return pass;

    }

    @Override
    public boolean join(MemberJoinDTO memberJoinDTO) throws Exception {

        Optional<Member> findMember = memberRepository.findByEmail(memberJoinDTO.getEmail());
        if(findMember.isPresent()){
            throw new IllegalStateException("이미 가입된 회원입니다.");
        }

        String uuid = UUID.randomUUID().toString();

        Member member = modelMapper.map(memberJoinDTO, Member.class);
        member.changePassword(passwordEncoder.encode(memberJoinDTO.getPassword()));

        // 사용자 등록시 아래 사용
        member.changeEmailAuthYn(false);
        member.changeEmailAuthKey(uuid);
        member.addRole(MemberRole.USER);
        member.changeUserStatus(MemberCode.MEMBER_STATUS_REQ);

        // 관리자 등록시 아래 사용
        /*member.changeEmailAuthYn(true);
        member.changeEmailAuthKey("");
        member.addRole(MemberRole.ADMIN);
        member.changeUserStatus(MemberCode.MEMBER_STATUS_ING);*/

        memberRepository.save(member);

        // 회원가입 후 키 인증 메일 보내기
        /*String subject = "[LMS] 회원이 되신 것을 환영합니다.";
        String text = "<h2>LMS 회원가입을 축하드립니다.</h2><br /><hr /><br />";
        text += "<p>" + memberJoinDTO.getUserName() + "님의 아래 링크를 클릭하셔서 가입을 완료 하세요.</p>";
        text += "<div><a target='_blank' href='http://localhost:" + serverPost + "/member/email-auth/" + uuid + "'>가입 완료</a></div>";
        mailComponent.sendMail(memberJoinDTO.getEmail(), subject, text);*/

        return true;

    }

    @Override
    public boolean modifyInfo(Long mno, MemberDTO memberDTO) throws Exception {
        Optional<Member> optionalMember = memberRepository.findById(mno);
        if (optionalMember.isEmpty()) {
            return false;
        }

        Member member = optionalMember.get();
        member.changeModify(memberDTO);
        memberRepository.save(member);

        return true;
    }

    @Override
    public boolean withdraw(Long mno) throws Exception {
        Optional<Member> optionalMember = memberRepository.findById(mno);
        if (optionalMember.isEmpty()) {
            return false;
        }

        Member member = optionalMember.get();
        member.changeUserStatus(MemberCode.MEMBER_STATUS_WITHDRAW);
        memberRepository.save(member);

        return true;
    }

    @Override
    public boolean emailAuth(String uuid) {
        Optional<Member> optionalMember = memberRepository.findByEmailAuthKey(uuid);
        if (!optionalMember.isPresent()) {
            return false;
        }

        Member member = optionalMember.get();

        if (member.isEmailAuthYn()) {
            return false;
        }

        member.changeUserStatus(MemberCode.MEMBER_STATUS_ING);
        member.changeEmailAuthYn(true);
        member.changeEmailAuthKey("");
        member.changeEmailAuthTime(LocalDateTime.now());
        memberRepository.save(member);

        return true;
    }

    @Override
    public boolean verifyRecaptcha(String recaptcha) {

        final String SECRET_KEY = captchaSetting.getSecret(); // 비밀키 호출
        final String RE_URL = captchaSetting.getUrl(); // 인증할 URL

        try {
            URL obj = new URL(RE_URL);
            HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();
            con.setRequestMethod("POST");

            String postParams = "secret=" + SECRET_KEY + "&response=" + recaptcha;
            con.setDoOutput(true);

            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.writeBytes(postParams);
            wr.flush();
            wr.close();

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            JsonReader jsonReader = Json.createReader(new StringReader(response.toString()));
            JsonObject jsonObject = jsonReader.readObject();
            jsonReader.close();
            return jsonObject.getBoolean("success"); //최종 Return 값 : true or false

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

    }

    @Override
    public boolean updateLoginDate() throws Exception {
        MemberSecurityDTO member = (MemberSecurityDTO) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        memberRepository.updateLoginDate(member.getMno());
        return true;
    }

    @Override
    public MemberDTO getMemberInfo(Long mno) {
        Member member = memberRepository.findByMno(mno);
        MemberDTO memberDTO = modelMapper.map(member, MemberDTO.class);
        return memberDTO;
    }

}