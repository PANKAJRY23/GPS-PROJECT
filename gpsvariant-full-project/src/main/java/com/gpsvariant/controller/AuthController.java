package com.gpsvariant.controller;

import com.gpsvariant.DTO.auth.ForgotPasswordRequest;
import com.gpsvariant.DTO.auth.RegistrationRequest;
import com.gpsvariant.DTO.auth.ResetPasswordRequest;
import com.gpsvariant.entity.PasswordResetToken;
import com.gpsvariant.service.auth.AuthService;

import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import org.springframework.validation.BindingResult;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;


@Controller
public class AuthController {

    private static final Logger log =
            LoggerFactory.getLogger(AuthController.class);


    private final AuthService authService;


    public AuthController(AuthService authService) {

        this.authService = authService;

    }


    // =========================================================
    // LOGIN PAGE
    // =========================================================

    @GetMapping("/login")
    public String login(

            @RequestParam(
                    value = "error",
                    required = false)
            String error,

            @RequestParam(
                    value = "logout",
                    required = false)
            String logout,

            @RequestParam(
                    value = "registered",
                    required = false)
            String registered,

            @RequestParam(
                    value = "reset",
                    required = false)
            String reset,

            Authentication authentication,

            Model model) {


        /*
         * If user is already logged in,
         * redirect to home page.
         */

        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication
                        instanceof AnonymousAuthenticationToken)) {

            return "redirect:/";

        }


        model.addAttribute(
                "loginError",
                error != null
        );


        model.addAttribute(
                "loggedOut",
                logout != null
        );


        model.addAttribute(
                "registered",
                registered != null
        );


        model.addAttribute(
                "passwordReset",
                reset != null
        );


        return "auth/login";

    }


    // =========================================================
    // REGISTER PAGE
    // =========================================================

    @GetMapping("/register")
    public String register(Model model) {


        if (!model.containsAttribute(
                "registrationRequest")) {

            model.addAttribute(
                    "registrationRequest",
                    new RegistrationRequest()
            );

        }


        return "auth/register";

    }


    // =========================================================
    // REGISTER USER
    // =========================================================

    @PostMapping("/register")
    public String register(

            @Valid
            @ModelAttribute(
                    "registrationRequest")
            RegistrationRequest request,

            BindingResult bindingResult,

            Model model) {


        /*
         * Check password confirmation
         */

        if (request.getPassword() == null
                || !request.getPassword()
                        .equals(request.getConfirmPassword())) {

            bindingResult.rejectValue(
                    "confirmPassword",
                    "password.mismatch",
                    "Passwords do not match"
            );

        }


        /*
         * Validation errors
         */

        if (bindingResult.hasErrors()) {

            return "auth/register";

        }


        try {

            authService.register(request);

        }
        catch (IllegalArgumentException ex) {

            model.addAttribute(
                    "registrationError",
                    ex.getMessage()
            );

            return "auth/register";

        }


        return "redirect:/login?registered=true";

    }


    // =========================================================
    // FORGOT PASSWORD PAGE
    // =========================================================

    @GetMapping("/forgot-password")
    public String forgotPassword(Model model) {


        if (!model.containsAttribute(
                "forgotPasswordRequest")) {

            model.addAttribute(
                    "forgotPasswordRequest",
                    new ForgotPasswordRequest()
            );

        }


        return "auth/forgot-password";

    }


    // =========================================================
    // REQUEST PASSWORD RESET
    // =========================================================

    @PostMapping("/forgot-password")
    public String forgotPassword(

            @Valid
            @ModelAttribute(
                    "forgotPasswordRequest")
            ForgotPasswordRequest request,

            BindingResult bindingResult,

            Model model) {


        /*
         * Validation
         */

        if (bindingResult.hasErrors()) {

            return "auth/forgot-password";

        }


        try {

            authService.requestPasswordReset(
                    request.getEmail()
            );

        }
        catch (Exception ex) {

            /*
             * Don't expose whether an account
             * exists or not.
             */

            log.error(
                    "Password reset email processing failed",
                    ex
            );

        }


        /*
         * Always show the same message.
         *
         * This prevents email/account enumeration.
         */

        model.addAttribute(
                "message",
                "If an account exists for that email address, "
                + "a password reset link has been sent."
        );


        return "auth/forgot-password";

    }


    // =========================================================
    // RESET PASSWORD PAGE
    // =========================================================

    @GetMapping("/reset-password")
    public String resetPasswordPage(

            @RequestParam(
                    value = "token",
                    required = false)
            String token,

            Model model) {


        /*
         * Token missing
         */

        if (token == null
                || token.trim().isEmpty()) {

            model.addAttribute(
                    "invalidToken",
                    true
            );

            return "auth/reset-password";

        }


        /*
         * Validate token
         *
         * IMPORTANT:
         *
         * findValidToken() returns PasswordResetToken,
         * NOT Optional<PasswordResetToken>.
         */

        try {

            PasswordResetToken resetToken =
                    authService.findValidToken(token);


            /*
             * Token is valid
             */

            model.addAttribute(
                    "invalidToken",
                    false
            );


            model.addAttribute(
                    "token",
                    token
            );


            model.addAttribute(
                    "resetPasswordRequest",
                    new ResetPasswordRequest()
            );


            return "auth/reset-password";

        }
        catch (RuntimeException ex) {

            /*
             * Token invalid / expired / already used
             */

            log.warn(
                    "Invalid password reset token: {}",
                    ex.getMessage()
            );


            model.addAttribute(
                    "invalidToken",
                    true
            );


            model.addAttribute(
                    "resetError",
                    ex.getMessage()
            );


            return "auth/reset-password";

        }

    }


    // =========================================================
    // RESET PASSWORD
    // =========================================================

    @PostMapping("/reset-password")
    public String resetPassword(

            @RequestParam("token")
            String token,

            @Valid
            @ModelAttribute(
                    "resetPasswordRequest")
            ResetPasswordRequest request,

            BindingResult bindingResult,

            Model model) {


        /*
         * Keep token available for the page
         * if validation fails.
         */

        model.addAttribute(
                "token",
                token
        );


        /*
         * Check password confirmation
         */

        if (request.getPassword() == null
                || !request.getPassword()
                        .equals(
                                request.getConfirmPassword()
                        )) {

            bindingResult.rejectValue(
                    "confirmPassword",
                    "password.mismatch",
                    "Passwords do not match"
            );

        }


        /*
         * Validation errors
         */

        if (bindingResult.hasErrors()) {

            return "auth/reset-password";

        }


        try {

            /*
             * Your AuthService expects THREE parameters:
             *
             * token
             * newPassword
             * confirmPassword
             */

            authService.resetPassword(
                    token,
                    request.getPassword(),
                    request.getConfirmPassword()
            );


            /*
             * Password successfully changed.
             */

            return "redirect:/login?reset=true";

        }
        catch (RuntimeException ex) {

            log.error(
                    "Password reset failed",
                    ex
            );


            model.addAttribute(
                    "resetError",
                    ex.getMessage()
            );


            return "auth/reset-password";

        }

    }

}