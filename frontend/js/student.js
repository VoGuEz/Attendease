/* =====================================================
   AttendEase – student.js
   ===================================================== */

(function () {
  /* ── Auth guard ── */
  if (!requireAuth()) return;

  /* ── Theme ── */
  const THEMES = ['dark','light','blue','purple','ocean','forest','sunset'];
  let themeIdx = 0;

  function applyTheme(t) {
    document.body.setAttribute('data-theme', t);
    localStorage.setItem('theme', t);
    const btn = document.getElementById('themeToggleBtn');
    if (btn) btn.textContent = (t === 'dark') ? '☀' : '🌙';
  }

  function initTheme() {
    const saved = localStorage.getItem('theme') || 'dark';
    themeIdx = THEMES.indexOf(saved);
    if (themeIdx < 0) themeIdx = 0;
    applyTheme(THEMES[themeIdx]);
  }

  function initThemeSwitcher() {
    const toggleBtn = document.getElementById('themeToggleBtn');
    const dropdown  = document.getElementById('themeDropdown');

    if (toggleBtn) {
      toggleBtn.addEventListener('click', (e) => {
        e.stopPropagation();
        if (dropdown) {
          dropdown.classList.toggle('open');
        } else {
          themeIdx = (themeIdx + 1) % THEMES.length;
          applyTheme(THEMES[themeIdx]);
        }
      });
    }

    document.querySelectorAll('[data-theme-val]').forEach(btn => {
      btn.addEventListener('click', () => {
        applyTheme(btn.dataset.themeVal);
        themeIdx = THEMES.indexOf(btn.dataset.themeVal);
        if (dropdown) dropdown.classList.remove('open');
      });
    });

    document.addEventListener('click', () => {
      if (dropdown) dropdown.classList.remove('open');
    });
  }

  initTheme();
  initThemeSwitcher();

  /* ── User info ── */
  const user = getUser();
  const emailEl = document.getElementById('headerEmail');
  if (emailEl) emailEl.textContent = user.email || '';

  /* ── Sign out ── */
  document.getElementById('signOutBtn')?.addEventListener('click', () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    window.location.href = 'signin.html';
  });

  /* ── Geolocation ── */
  let userLat = null, userLng = null;
  const locIcon = document.getElementById('locationIcon');
  const locText = document.getElementById('locationText');

  function setLocationEnabled(lat, lng) {
    userLat = lat; userLng = lng;
    if (locIcon) { locIcon.textContent = '✓'; locIcon.className = 'loc-icon loc-on'; }
    if (locText) locText.textContent = `Location enabled (${lat.toFixed(4)}, ${lng.toFixed(4)})`;
    document.getElementById('requestLocationBtn')?.classList.add('hidden');
  }

  function requestLocation() {
    if (!navigator.geolocation) {
      if (locText) locText.textContent = 'Geolocation not supported';
      return;
    }
    navigator.geolocation.getCurrentPosition(
      pos => setLocationEnabled(pos.coords.latitude, pos.coords.longitude),
      ()  => { if (locText) locText.textContent = 'Location access denied'; }
    );
  }

  document.getElementById('requestLocationBtn')?.addEventListener('click', requestLocation);
  // Try to get location on load
  if (navigator.geolocation) {
    navigator.geolocation.getCurrentPosition(
      pos => setLocationEnabled(pos.coords.latitude, pos.coords.longitude),
      () => {}
    );
  }

  /* ── Stats ── */
  function updateStats(attended, total, rate) {
    document.getElementById('statAttended').textContent = attended;
    document.getElementById('statTotal').textContent    = total;
    document.getElementById('statRate').textContent     = `${rate}%`;

    const ratio = document.getElementById('progressRatio');
    if (ratio) ratio.textContent = `${attended} / ${total}`;

    const bar = document.getElementById('progressBar');
    if (bar) {
      setTimeout(() => { bar.style.width = `${rate}%`; }, 100);
    }

    const msg = document.getElementById('progressMsg');
    if (msg) {
      if (total === 0)    msg.textContent = '🚀 No sessions yet — get started!';
      else if (rate >= 90) msg.textContent = '🌟 Outstanding! Keep it up!';
      else if (rate >= 75) msg.textContent = '✅ Great attendance! Almost there!';
      else if (rate >= 50) msg.textContent = '📈 Good progress! Keep going!';
      else                msg.textContent = '⚠️ Attendance is low — try to join more sessions.';
    }
  }

  /* ── Sessions list ── */
  function renderSessions(sessions) {
    const list = document.getElementById('sessionsList');
    if (!list) return;

    if (!sessions || sessions.length === 0) {
      list.innerHTML = `
        <div class="empty-state">
          <span class="empty-icon">📅</span>
          <p>No active sessions right now</p>
        </div>`;
      return;
    }

    list.innerHTML = sessions.map(s => `
      <div class="session-card" id="session-${s.id}">
        <div class="session-info">
          <span class="session-name">${s.courseName || 'Session'} (${s.courseCode || ''})</span>
          <span class="session-meta">📅 ${s.sessionDate} &nbsp; ⏱ ${s.startTime} – ${s.endTime}</span>
        </div>
        <div class="session-actions">
          <span class="session-badge badge-${(s.status||'').toLowerCase()}">${s.status}</span>
          <button class="btn-sm" data-session-id="${s.id}" data-session-name="${s.courseName}">Join</button>
        </div>
      </div>
    `).join('');

    list.querySelectorAll('[data-session-id]').forEach(btn => {
      btn.addEventListener('click', () => openJoinModal(btn.dataset.sessionId, btn.dataset.sessionName));
    });
  }

  /* ── Join modal ── */
  let pendingJoinId = null;
  const joinModal   = document.getElementById('joinModal');

  function openJoinModal(sessionId, name) {
    pendingJoinId = sessionId;
    const msg = document.getElementById('joinModalMsg');
    if (msg) msg.textContent = `Are you sure you want to join "${name}"?`;
    if (joinModal) joinModal.style.display = 'flex';
  }

  document.getElementById('joinCancelBtn')?.addEventListener('click', () => {
    if (joinModal) joinModal.style.display = 'none';
    pendingJoinId = null;
  });

  document.getElementById('joinConfirmBtn')?.addEventListener('click', async () => {
    if (!pendingJoinId) return;
    const token = getToken();
    const btn   = document.getElementById('joinConfirmBtn');
    if (btn) btn.disabled = true;
    try {
      await api.post('/attendance/join', {
        sessionId: Number(pendingJoinId),
        latitude:  userLat,
        longitude: userLng,
      }, token);
      if (joinModal) joinModal.style.display = 'none';
      loadData(); // refresh
    } catch (err) {
      alert('Could not join session: ' + (err.message || 'Unknown error'));
    } finally {
      if (btn) btn.disabled = false;
      pendingJoinId = null;
    }
  });

  /* ── Load data ── */
  async function loadData() {
    const token = getToken();
    const userId = (getUser()).id;

    try {
      // Try real API first
      const [stats, sessions] = await Promise.all([
        api.get(`/attendance/statistics/${userId}`, token),
        api.get('/sessions/active', token),
      ]);
      updateStats(stats.attended || 0, stats.total || 0, stats.rate || 0);
      renderSessions(sessions);
    } catch {
      // Fall back to mock data
      updateStats(MOCK.studentStats.attended, MOCK.studentStats.total, MOCK.studentStats.rate);
      renderSessions(MOCK.sessions);
    }
  }

  loadData();
})();
