package com.gpsvariant.service.auth;

import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final JavaMailSender mailSender;

    /*
     * Default expiry time for password reset link.
     * Can be overridden from application.properties.
     */
    @Value("${app.password-reset.expiry-minutes:15}")
    private long expiryMinutes;

    /*
     * Email address from application.properties
     */
    @Value("${app.mail.from:${spring.mail.username:}}")
    private String fromEmail;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;


    public MailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }


    /*
     * ============================================================
     * PASSWORD RESET EMAIL
     * ============================================================
     *
     * This method uses the expiry configured in
     * application.properties.
     *
     * Example:
     *
     * mailService.sendPasswordResetEmail(
     *     user.getEmail(),
     *     user.getUsername(),
     *     resetUrl
     * );
     *
     */
    public void sendPasswordResetEmail(
            String email,
            String username,
            String resetUrl) {

        sendPasswordResetEmail(
                email,
                username,
                resetUrl,
                expiryMinutes
        );
    }


    /*
     * ============================================================
     * PASSWORD RESET EMAIL WITH EXPLICIT EXPIRY
     * ============================================================
     *
     * This is the method your current code is trying to call:
     *
     * mailService.sendPasswordResetEmail(
     *     user.getEmail(),
     *     user.getUsername(),
     *     resetUrl,
     *     expiryMinutes
     * );
     *
     */
    public void sendPasswordResetEmail(
            String email,
            String username,
            String resetUrl,
            long expiryMinutes) {

        if (!mailEnabled) {
            log.warn("Password-reset email is disabled. No email sent to {}", email);
            return;
        }

        if (fromEmail == null || fromEmail.isBlank()) {
            throw new IllegalStateException("MAIL_FROM or MAIL_USERNAME must be configured");
        }

        try {

            MimeMessage message =
                    mailSender.createMimeMessage();

            MimeMessageHelper helper =
                    new MimeMessageHelper(
                            message,
                            true,
                            "UTF-8"
                    );


            /*
             * ----------------------------------------------------
             * EMAIL DETAILS
             * ----------------------------------------------------
             */

            helper.setFrom(fromEmail);

            helper.setTo(email);

            helper.setSubject(
                    "GPSVariant - Reset Your Password"
            );


            /*
             * ----------------------------------------------------
             * EMAIL HTML
             * ----------------------------------------------------
             */

            String html =
                    buildHtml(
                            username,
                            resetUrl,
                            expiryMinutes
                    );


            helper.setText(
                    html,
                    true
            );


            /*
             * ----------------------------------------------------
             * SEND EMAIL
             * ----------------------------------------------------
             */

            mailSender.send(message);


            log.info("Password reset email sent successfully to {}", email);


        } catch (MessagingException e) {

            log.error("Failed to create password reset email", e);

            throw new RuntimeException(
                    "Unable to send password reset email",
                    e
            );

        } catch (Exception e) {

            log.error("Failed to send password reset email", e);

            throw new RuntimeException(
                    "Unable to send password reset email",
                    e
            );
        }
    }


    /*
     * ============================================================
     * BUILD PASSWORD RESET EMAIL HTML
     * ============================================================
     */

    private String buildHtml(
            String username,
            String resetUrl,
            long expiryMinutes) {

        return """
                <!DOCTYPE html>
                <html>

                <head>

                    <meta charset="UTF-8">

                    <title>
                        GPSVariant Password Reset
                    </title>

                </head>

                <body style="
                    margin:0;
                    padding:0;
                    background:#f4f7fb;
                    font-family:Arial,Helvetica,sans-serif;
                    color:#172033;
                ">

                    <div style="
                        max-width:560px;
                        margin:40px auto;
                        background:#ffffff;
                        border-radius:16px;
                        padding:36px;
                        box-shadow:
                            0 10px 35px
                            rgba(15,23,42,.10);
                    ">

                        <!-- LOGO / APPLICATION NAME -->

                        <div style="
                            font-size:24px;
                            font-weight:800;
                            color:#155eef;
                            margin-bottom:24px;
                        ">

                            GPSVariant

                        </div>


                        <!-- TITLE -->

                        <h2 style="
                            margin:0 0 12px;
                            color:#172033;
                        ">

                            Reset your password

                        </h2>


                        <!-- GREETING -->

                        <p style="
                            line-height:1.6;
                        ">

                            Hello %s,

                        </p>


                        <!-- MESSAGE -->

                        <p style="
                            line-height:1.6;
                        ">

                            We received a request to reset your
                            GPSVariant password.

                            This password reset link will expire
                            in %d minutes.

                        </p>


                        <!-- RESET BUTTON -->

                        <p style="
                            margin:28px 0;
                        ">

                            <a href="%s"
                               style="
                                   display:inline-block;
                                   background:#155eef;
                                   color:#ffffff;
                                   text-decoration:none;
                                   padding:13px 22px;
                                   border-radius:10px;
                                   font-weight:700;
                               ">

                                Reset Password

                            </a>

                        </p>


                        <!-- SECURITY MESSAGE -->

                        <p style="
                            font-size:13px;
                            color:#667085;
                            line-height:1.6;
                        ">

                            If you did not request a password reset,
                            you can safely ignore this email.

                        </p>


                        <!-- FOOTER -->

                        <p style="
                            margin-top:30px;
                            padding-top:20px;
                            border-top:1px solid #eeeeee;
                            font-size:12px;
                            color:#98a2b3;
                        ">

                            This is an automated email from GPSVariant.
                            Please do not reply to this email.

                        </p>

                    </div>

                </body>

                </html>
                """.formatted(
                        escapeHtml(username),
                        expiryMinutes,
                        escapeHtml(resetUrl)
                );
    }


    /*
     * ============================================================
     * HTML ESCAPE
     * ============================================================
     *
     * Prevents username/reset URL from injecting HTML into
     * the email.
     *
     */

    private String escapeHtml(String value) {

        if (value == null) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }


    /*
     * ============================================================
     * OPTIONAL SIMPLE TEXT EMAIL
     * ============================================================
     *
     * This can be useful later for other email notifications.
     *
     */

    public void sendSimpleEmail(
            String to,
            String subject,
            String text) {

        if (!mailEnabled) {
            log.warn("Email is disabled. Skipping email to {}", to);
            return;
        }

        SimpleMailMessage message =
                new SimpleMailMessage();

        message.setFrom(fromEmail);

        message.setTo(to);

        message.setSubject(subject);

        message.setText(text);

        mailSender.send(message);
    }
}