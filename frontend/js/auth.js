document.addEventListener('DOMContentLoaded', () => {
    const loginForm = document.getElementById('login-form');
    const authMsg = document.getElementById('auth-msg');

    if (loginForm) {
        loginForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            
            const email = document.getElementById('email').value.trim();
            const password = document.getElementById('password').value;
            const btn = e.target.querySelector('button[type="submit"]');

            // UI feedback
            btn.disabled = true;
            btn.innerHTML = '<span class="spinner"></span> Signing in...';
            authMsg.textContent = '';

            try {
                // Send request to Railway backend
                const data = await apiRequest('/auth/login', 'POST', { email, password });

                if (data && data.token) {
                    // CRITICAL: This is what fills your empty LocalStorage table
                    setAuth(data.token, data.user);
                    
                    // Redirect based on role
                    const role = data.user.role.toUpperCase();
                    if (role === 'LECTURER') {
                        window.location.href = 'lecturer.html';
                    } else {
                        window.location.href = 'student.html';
                    }
                }
            } catch (err) {
                authMsg.textContent = err.message || 'Login failed. Please try again.';
                authMsg.className = 'msg error';
                btn.disabled = false;
                btn.innerHTML = 'Sign In';
            }
        });
    }
});