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
      btn.innerHTML = '<span class="spinner"></span> Signing in...';

      try {
        const data = await apiRequest('/auth/login', 'POST', { email, password });
        
        if (data && data.token && data.user) {
          setAuth(data.token, data.user);
          
          // Redirect based on role
          if (data.user.role.toUpperCase() === 'LECTURER') {
            window.location.href = 'lecturer.html';
          } else {
            window.location.href = 'student.html';
          }
        }
      } catch (err) {
        authMsg.textContent = err.message;
        authMsg.className = 'msg error';
        btn.disabled = false;
        btn.innerHTML = 'Sign In';
      }
    });
  }
});