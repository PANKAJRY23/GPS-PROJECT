document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('[data-password-toggle]').forEach(button => {
        button.addEventListener('click', () => {
            const targetId = button.getAttribute('data-password-toggle');
            const input = document.getElementById(targetId);
            if (!input) return;

            const visible = input.type === 'text';
            input.type = visible ? 'password' : 'text';
            button.setAttribute('aria-label', visible ? 'Show password' : 'Hide password');
            button.textContent = visible ? 'Show' : 'Hide';
        });
    });

    const password = document.getElementById('password');
    const confirmPassword = document.getElementById('confirmPassword');
    const username = document.getElementById('username');
    const email = document.getElementById('email');

    function validatePassword() {
        if (!password) return true;
        const value = password.value;
        if (!value) {
            password.setCustomValidity('Password is required');
            return false;
        }
        if (value.length < 8 || value.length > 72) {
            password.setCustomValidity('Password must be between 8 and 72 characters');
            return false;
        }
        password.setCustomValidity('');
        return true;
    }

    function validateConfirmPassword() {
        if (!confirmPassword) return true;
        if (!confirmPassword.value) {
            confirmPassword.setCustomValidity('Please confirm your password');
            return false;
        }
        if (password && password.value !== confirmPassword.value) {
            confirmPassword.setCustomValidity('Passwords do not match');
            return false;
        }
        confirmPassword.setCustomValidity('');
        return true;
    }

    function validateUsername() {
        if (!username) return true;
        const value = username.value.trim();
        if (!value) username.setCustomValidity('Username is required');
        else if (value.length < 4 || value.length > 50) username.setCustomValidity('Username must be between 4 and 50 characters');
        else username.setCustomValidity('');
        return username.checkValidity();
    }

    function validateEmail() {
        if (!email) return true;
        const value = email.value.trim();
        if (!value) email.setCustomValidity('Email is required');
        else if (value.length > 120) email.setCustomValidity('Email is too long');
        else if (!email.validity.valid) email.setCustomValidity('Enter a valid email address');
        else email.setCustomValidity('');
        return email.checkValidity();
    }

    [password, confirmPassword].filter(Boolean).forEach(input => input.addEventListener('input', () => {
        validatePassword();
        validateConfirmPassword();
    }));
    if (username) username.addEventListener('input', validateUsername);
    if (email) email.addEventListener('input', validateEmail);

    document.querySelectorAll('form[data-auth-form]').forEach(form => {
        form.addEventListener('submit', event => {
            validateUsername();
            validateEmail();
            validatePassword();
            validateConfirmPassword();

            if (!form.checkValidity()) {
                event.preventDefault();
                form.reportValidity();
                return;
            }

            const button = form.querySelector('button[type="submit"]');
            if (!button) return;
            button.disabled = true;
            button.textContent = button.dataset.loadingText || 'Please wait...';
        });
    });
});
