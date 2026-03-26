document.addEventListener('DOMContentLoaded', () => {
  const loginForm = document.getElementById('login-form');
  const signupForm = document.getElementById('signup-form');

  if (loginForm) initLogin();
  if (signupForm) initSignup();
});

function showMsg(id, text, type) {
  const el = document.getElementById(id);
  if (!el) return;
  el.textContent = text;
  el.className = `msg ${type}`;
}

function setLoading(btn, loading) {
  if (loading) {
    btn.dataset.originalText = btn.innerHTML;
    btn.innerHTML = '<span class="spinner"></span>';
    btn.disabled = true;
  } else {
    btn.innerHTML = btn.dataset.originalText || btn.innerHTML;
    btn.disabled = false;
  }
}

/* ===== LOGIN ===== */
function initLogin() {
  const form = document.getElementById('login-form');
  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    const btn = form.querySelector('button[type="submit"]');
    const email = document.getElementById('email').value.trim();
    const password = document.getElementById('password').value;

    if (!email || !password) {
      showMsg('auth-msg', 'Please fill in all fields.', 'error');
      return;
    }

    setLoading(btn, true);
    try {
      const data = await apiRequest('/auth/login', 'POST', { email, password });
      setAuth(data.token, data.user);
      showMsg('auth-msg', 'Login successful! Redirecting...', 'success');
      setTimeout(() => {
        window.location.href = 'dashboard.html';
      }, 600);
    } catch (err) {
      showMsg('auth-msg', err.message, 'error');
    } finally {
      setLoading(btn, false);
    }
  });
}

/* ===== SIGNUP ===== */
function initSignup() {
  const form = document.getElementById('signup-form');
  let selectedRole = 'student';

  document.querySelectorAll('.role-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      selectedRole = btn.dataset.role;
      document.querySelectorAll('.role-btn').forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
    });
  });

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    const btn = form.querySelector('button[type="submit"]');
    const fullName = document.getElementById('fullName').value.trim();
    const email = document.getElementById('email').value.trim();
    const password = document.getElementById('password').value;
    const confirmPassword = document.getElementById('confirmPassword')?.value;

    if (!fullName || !email || !password) {
      showMsg('auth-msg', 'Please fill in all fields.', 'error');
      return;
    }

    if (password.length < 6) {
      showMsg('auth-msg', 'Password must be at least 6 characters.', 'error');
      return;
    }

    if (confirmPassword !== undefined && password !== confirmPassword) {
      showMsg('auth-msg', 'Passwords do not match.', 'error');
      return;
    }

    setLoading(btn, true);
    try {
      const data = await apiRequest('/auth/register', 'POST', { email, password, fullName, role: selectedRole });
      setAuth(data.token, data.user);
      showMsg('auth-msg', 'Account created! Redirecting...', 'success');
      setTimeout(() => {
        window.location.href = 'dashboard.html';
      }, 600);
    } catch (err) {
      showMsg('auth-msg', err.message, 'error');
    } finally {
      setLoading(btn, false);
    }
  });
}
