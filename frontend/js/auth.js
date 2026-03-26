/* =====================================================
   AttendEase – auth.js
   (handles both sign-up and sign-in pages)
   ===================================================== */

(function () {
  /* ── Theme management ── */
  const THEMES = ['dark','light','blue','purple','ocean','forest','sunset'];

  function applyTheme(t) {
    document.body.setAttribute('data-theme', t);
    localStorage.setItem('theme', t);
  }

  function initTheme() {
    const saved = localStorage.getItem('theme') || 'dark';
    applyTheme(saved);
  }

  function initThemeSwitcher() {
    const cycleBtn     = document.getElementById('themeCycleBtn');
    const dropdown     = document.getElementById('themeDropdown');
    const themeButtons = document.querySelectorAll('[data-theme-val]');

    if (cycleBtn) {
      cycleBtn.addEventListener('click', (e) => {
        e.stopPropagation();
        dropdown.classList.toggle('open');
      });
    }

    themeButtons.forEach(btn => {
      btn.addEventListener('click', () => {
        applyTheme(btn.dataset.themeVal);
        if (dropdown) dropdown.classList.remove('open');
      });
    });

    document.addEventListener('click', () => {
      if (dropdown) dropdown.classList.remove('open');
    });
  }

  initTheme();
  initThemeSwitcher();

  /* ── Password toggle ── */
  const togglePw  = document.getElementById('togglePw');
  const pwInput   = document.getElementById('password');
  if (togglePw && pwInput) {
    togglePw.addEventListener('click', () => {
      pwInput.type = pwInput.type === 'password' ? 'text' : 'password';
      togglePw.textContent = pwInput.type === 'password' ? '👁' : '🙈';
    });
  }

  /* ── Show / hide error ── */
  function showError(msg) {
    const el = document.getElementById('errorMsg');
    if (el) el.textContent = msg;
  }
  function clearError() { showError(''); }

  function setLoading(loading) {
    const btn = document.getElementById('submitBtn');
    if (!btn) return;
    btn.disabled = loading;
    btn.textContent = loading
      ? (btn.dataset.loadingText || 'Please wait…')
      : (btn.dataset.defaultText || btn.textContent);
  }

  /* ── Sign-Up form ── */
  const signupForm = document.getElementById('signupForm');
  if (signupForm) {
    let selectedRole = 'STUDENT';

    // role toggle buttons
    document.querySelectorAll('.role-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        document.querySelectorAll('.role-btn').forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
        selectedRole = btn.dataset.role;
      });
    });

    const submitBtn = document.getElementById('submitBtn');
    if (submitBtn) {
      submitBtn.dataset.defaultText = 'Create Account';
      submitBtn.dataset.loadingText = 'Creating account…';
    }

    signupForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      clearError();

      const fullName = document.getElementById('fullName').value.trim();
      const email    = document.getElementById('email').value.trim();
      const password = document.getElementById('password').value;

      if (!fullName) { showError('Please enter your full name.'); return; }
      if (!email)    { showError('Please enter your email.'); return; }
      if (password.length < 6) { showError('Password must be at least 6 characters.'); return; }

      setLoading(true);
      try {
        const data = await api.post('/auth/register', { fullName, email, password, role: selectedRole });
        localStorage.setItem('token', data.token);
        localStorage.setItem('user', JSON.stringify(data.user || { email, fullName, role: selectedRole }));
        window.location.href = 'role-select.html';
      } catch (err) {
        showError(err.message || 'Registration failed. Please try again.');
      } finally {
        setLoading(false);
      }
    });
  }

  /* ── Sign-In form ── */
  const signinForm = document.getElementById('signinForm');
  if (signinForm) {
    const submitBtn = document.getElementById('submitBtn');
    if (submitBtn) {
      submitBtn.dataset.defaultText = 'Sign In';
      submitBtn.dataset.loadingText = 'Signing in…';
    }

    signinForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      clearError();

      const email    = document.getElementById('email').value.trim();
      const password = document.getElementById('password').value;

      if (!email)    { showError('Please enter your email.'); return; }
      if (!password) { showError('Please enter your password.'); return; }

      setLoading(true);
      try {
        const data = await api.post('/auth/login', { email, password });
        localStorage.setItem('token', data.token);
        localStorage.setItem('user', JSON.stringify(data.user || { email }));
        window.location.href = 'role-select.html';
      } catch (err) {
        showError(err.message || 'Invalid email or password.');
      } finally {
        setLoading(false);
      }
    });
  }
})();
