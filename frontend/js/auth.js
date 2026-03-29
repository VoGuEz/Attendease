document.addEventListener('DOMContentLoaded', () => {
    const loginForm = document.getElementById('login-form');
    const authMsg = document.getElementById('auth-msg');

    if (loginForm) {
        loginForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            
            const email = document.getElementById('email').value.trim();
            const password = document.getElementById('password').value;
            const btn = e.target.querySelector('button[type="submit"]');

            btn.disabled = true;
            btn.innerHTML = 'Signing in...';

            try {
                const data = await apiRequest('/auth/login', 'POST', { email, password });

                if (data && data.token) {
                    // This saves the data so the dashboard button knows you are a Lecturer
                    setAuth(data.token, data.user);
                    
                    const role = data.user.role.toUpperCase();
                    if (role === 'LECTURER') {
                        window.location.href = 'lecturer.html';
                    } else {
                        window.location.href = 'student.html';
                    }
                }
            } catch (err) {
                authMsg.textContent = err.message || 'Login failed.';
                authMsg.className = 'msg error';
                btn.disabled = false;
                btn.innerHTML = 'Sign In';
            }
        });
    }
});