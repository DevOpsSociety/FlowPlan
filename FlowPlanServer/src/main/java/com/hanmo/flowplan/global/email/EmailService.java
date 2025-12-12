package com.hanmo.flowplan.global.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

  private final JavaMailSender mailSender;

  public void sendInvitationEmail(String toEmail, String projectName, String inviteUrl) {
    SimpleMailMessage message = new SimpleMailMessage();
    message.setTo(toEmail);
    message.setSubject("[FlowPlan] '" + projectName + "' 프로젝트 초대");
    message.setText("안녕하세요!\n\n" +
        "'" + projectName + "' 프로젝트에 초대되었습니다.\n" +
        "아래 링크를 클릭하여 초대를 수락하세요:\n\n" +
        inviteUrl + "\n\n" +
        "(이 링크는 24시간 동안 유효합니다.)");

    mailSender.send(message);
  }
}