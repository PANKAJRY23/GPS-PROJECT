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

    document.querySelectorAll('form[data-auth-form]').forEach(form => {
        form.addEventListener('submit', () => {
            const button = form.querySelector('button[type="submit"]');
            if (!button) return;
            button.disabled = true;
            button.textContent = button.dataset.loadingText || 'Please wait...';
        });
    });

    const password = document.getElementById('password');
    const confirmPassword = document.getElementById('confirmPassword');

    if (password && confirmPassword) {
        const validateMatch = () => {
            if (!confirmPassword.value) return;
            confirmPassword.setCustomValidity(
                password.value === confirmPassword.value ? '' : 'Passwords do not match'
            );
        };
        password.addEventListener('input', validateMatch);
        confirmPassword.addEventListener('input', validateMatch);
    }
});
