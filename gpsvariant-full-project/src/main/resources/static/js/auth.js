document.addEventListener('DOMContentLoaded', () => {

    // =========================
    // Password Show / Hide
    // =========================
    document.querySelectorAll('[data-password-toggle]').forEach(button => {
        button.addEventListener('click', () => {
            const targetId = button.getAttribute('data-password-toggle');
            const input = document.getElementById(targetId);

            if (!input) return;

            const visible = input.type === 'text';

            input.type = visible ? 'password' : 'text';
            button.setAttribute(
                'aria-label',
                visible ? 'Show password' : 'Hide password'
            );
            button.textContent = visible ? 'Show' : 'Hide';
        });
    });


    // =========================
    // Get Form Fields
    // =========================
    const password = document.getElementById('password');
    const confirmPassword = document.getElementById('confirmPassword');
    const username = document.getElementById('username');
    const email = document.getElementById('email');


    // =========================
    // Password Validation
    // =========================
    function validatePassword() {
        if (!password) return true;

        const value = password.value;

        if (!value) {
            password.setCustomValidity('Password is required');
            return false;
        }

        if (value.length < 8 || value.length > 72) {
            password.setCustomValidity(
                'Password must be between 8 and 72 characters'
            );
            return false;
        }

        password.setCustomValidity('');
        return true;
    }


    // =========================
    // Confirm Password Validation
    // =========================
    function validateConfirmPassword() {
        if (!confirmPassword) return true;

        if (!confirmPassword.value) {
            confirmPassword.setCustomValidity(
                'Please confirm your password'
            );
            return false;
        }

        if (password && password.value !== confirmPassword.value) {
            confirmPassword.setCustomValidity(
                'Passwords do not match'
            );
            return false;
        }

        confirmPassword.setCustomValidity('');
        return true;
    }


    // =========================
    // Username Validation
    // =========================
    function validateUsername() {
        if (!username) return true;

        const value = username.value.trim();

        if (!value) {
            username.setCustomValidity(
                'Username is required'
            );
            return false;
        }

        if (value.length < 4 || value.length > 50) {
            username.setCustomValidity(
                'Username must be between 4 and 50 characters'
            );
            return false;
        }

        username.setCustomValidity('');
        return true;
    }


    // =========================
    // Email Validation
    // =========================
    function validateEmail() {
        if (!email) return true;

        const value = email.value.trim();

        // Keep the trimmed value in the input
        email.value = value;

        if (!value) {
            email.setCustomValidity(
                'Email is required'
            );
            return false;
        }

        if (value.length > 120) {
            email.setCustomValidity(
                'Email is too long'
            );
            return false;
        }

        /*
         * Standard email validation.
         *
         * This accepts:
         * pankajry23@gmail.com
         *
         * and rejects:
         * pankajry23gmail.com
         * pankajry23@
         * @gmail.com
         */
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

        if (!emailRegex.test(value)) {
            email.setCustomValidity(
                'Enter a valid email address'
            );
            return false;
        }

        email.setCustomValidity('');
        return true;
    }


    // =========================
    // Password Input Events
    // =========================
    [password, confirmPassword]
        .filter(Boolean)
        .forEach(input => {
            input.addEventListener('input', () => {
                validatePassword();
                validateConfirmPassword();
            });
        });


    // =========================
    // Username Input Event
    // =========================
    if (username) {
        username.addEventListener('input', validateUsername);
    }


    // =========================
    // Email Input Event
    // =========================
    if (email) {
        email.addEventListener('input', validateEmail);

        // Also validate when user leaves the field
        email.addEventListener('blur', validateEmail);
    }


    // =========================
    // Form Submit Validation
    // =========================
    document.querySelectorAll('form[data-auth-form]').forEach(form => {

        form.addEventListener('submit', event => {

            const usernameValid = validateUsername();
            const emailValid = validateEmail();
            const passwordValid = validatePassword();
            const confirmPasswordValid = validateConfirmPassword();

            /*
             * Stop submission if any custom validation fails.
             */
            if (
                !usernameValid ||
                !emailValid ||
                !passwordValid ||
                !confirmPasswordValid ||
                !form.checkValidity()
            ) {
                event.preventDefault();
                form.reportValidity();
                return;
            }


            // =========================
            // Disable Submit Button
            // =========================
            const button = form.querySelector(
                'button[type="submit"]'
            );

            if (!button) return;

            button.disabled = true;
            button.textContent =
                button.dataset.loadingText || 'Please wait...';
        });
    });

});
