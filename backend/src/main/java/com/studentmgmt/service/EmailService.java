package com.studentmgmt.service;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.studentmgmt.dto.StudentDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class EmailService {

    private final Resend resend;
    private final String fromEmail;

    public EmailService(
            @Value("${resend.api-key}") String apiKey,
            @Value("${resend.from-email}") String fromEmail) {
        this.resend = new Resend(apiKey);
        this.fromEmail = fromEmail;
    }

    public void sendInvite(String toEmail, String studentName) {
        String html = """
                <div style="font-family: 'Segoe UI', Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 40px 20px;">
                  <div style="background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); border-radius: 12px; padding: 32px; text-align: center; margin-bottom: 24px;">
                    <h1 style="color: #ffffff; margin: 0; font-size: 24px;">Welcome to Student Management System</h1>
                  </div>
                  <div style="background: #ffffff; border: 1px solid #e5e7eb; border-radius: 12px; padding: 32px;">
                    <p style="font-size: 16px; color: #374151; margin-top: 0;">Hello <strong>%s</strong>,</p>
                    <p style="font-size: 15px; color: #4b5563; line-height: 1.6;">
                      We are pleased to inform you that you have been successfully enrolled in our Student Management System.
                      Your records are now active and being managed through our platform.
                    </p>
                    <p style="font-size: 15px; color: #4b5563; line-height: 1.6;">
                      If you have any questions or need assistance, please don't hesitate to reach out to your administrator.
                    </p>
                    <hr style="border: none; border-top: 1px solid #e5e7eb; margin: 24px 0;">
                    <p style="font-size: 13px; color: #9ca3af; margin-bottom: 0; text-align: center;">
                      This is an automated message from Student Management System.
                    </p>
                  </div>
                </div>
                """.formatted(studentName);

        CreateEmailOptions params = CreateEmailOptions.builder()
                .from(fromEmail)
                .to(toEmail)
                .subject("Welcome to Student Management System")
                .html(html)
                .build();

        try {
            resend.emails().send(params);
            log.info("Invite email sent to {}", toEmail);
        } catch (ResendException e) {
            log.error("Failed to send invite email to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Failed to send email to " + toEmail + ": " + e.getMessage());
        }
    }

    public record BulkInviteResult(int sent, int failed, List<String> errors) {}

    public BulkInviteResult sendBulkInvites(List<StudentDto> students) {
        int sent = 0;
        int failed = 0;
        List<String> errors = new java.util.ArrayList<>();

        for (StudentDto student : students) {
            if (!student.isActive()) {
                errors.add(student.getEmail() + ": student is inactive");
                failed++;
                continue;
            }
            try {
                sendInvite(student.getEmail(), student.getFirstName() + " " + student.getLastName());
                sent++;
            } catch (Exception e) {
                failed++;
                errors.add(student.getEmail() + ": " + e.getMessage());
            }
        }

        return new BulkInviteResult(sent, failed, errors);
    }
}
