document.addEventListener('DOMContentLoaded', () => {
  const loginForm = document.getElementById('login-form');
  const signupForm = document.getElementById('signup-form');

  // Unified Password Toggle Logic
  setupPasswordToggle('toggle-password', 'password', 'toggle-password-icon');
  setupPasswordToggle('toggle-confirm', 'confirmPassword', 'toggle-confirm-icon');

  if (loginForm) initLogin();
  if (signupForm) initSignup();
});

// Helper function to handle any password field toggle
function setupPasswordToggle(btnId, inputId, iconId) {
  const btn = document.getElementById(btnId);
  const input = document.getElementById(inputId);
  const icon = document.getElementById(iconId);

  if (btn && input && icon) {
    btn.addEventListener('click', (e) => {
      e.preventDefault(); // Stop form submission
      e.stopPropagation(); // Stop event bubbling

      const isPassword = input.type === 'password';
      input.type = isPassword ? 'text' : 'password';
      icon.textContent = isPassword ? '🙈' : '👁️';
      
      // Accessibility update
      btn.setAttribute('aria-label', isPassword ? 'Hide password' : 'Show password');
      
      // Keep focus on the input for better UX
      input.focus();
    });
  }
}

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

function isValidEmailDomain(email) {
  const pattern = /^[A-Za-z0-9._%+-]+@(?:[A-Za-z0-9-]+\.)+[A-Za-z]{2,63}$/;
  if (!pattern.test(email)) return false;
  const domain = email.split('@')[1]?.toLowerCase();
  return domain && domain.includes('.') && !domain.startsWith('.') && !domain.endsWith('.');
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

  // Role selection logic
  document.querySelectorAll('.role-btn').forEach(btn => {
    btn.addEventListener('click', (e) => {
      e.preventDefault();
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

    if (!isValidEmailDomain(email.toLowerCase())) {
      showMsg('auth-msg', 'Use a valid email address.', 'error');
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
      await apiRequest('/auth/register', 'POST', { email, password, fullName, role: selectedRole });
      showMsg('auth-msg', 'Account created! Redirecting to sign in...', 'success');
      setTimeout(() => {
        window.location.href = 'index.html';
      }, 1200);
    } catch (err) {
      showMsg('auth-msg', err.message, 'error');
    } finally {
      setLoading(btn, false);
    }
  });
}