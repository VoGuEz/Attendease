/* =====================================================
   AttendEase – lecturer.js
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
        if (dropdown) dropdown.classList.toggle('open');
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

  /* ── Tabs ── */
  document.querySelectorAll('.tab-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
      document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));
      btn.classList.add('active');
      const contentId = `tabContent${btn.dataset.tab.charAt(0).toUpperCase() + btn.dataset.tab.slice(1)}`;
      document.getElementById(contentId)?.classList.add('active');
    });
  });

  /* ── Stats ── */
  function updateStats(hosted, active, completed) {
    document.getElementById('statHosted').textContent    = hosted;
    document.getElementById('statActive').textContent    = active;
    document.getElementById('statCompleted').textContent = completed;
  }

  /* ── Sessions list ── */
  function renderSessions(sessions) {
    const list = document.getElementById('sessionsList');
    if (!list) return;

    if (!sessions || sessions.length === 0) {
      list.innerHTML = `
        <div class="empty-state">
          <span class="empty-icon">📅</span>
          <p>No sessions yet — host your first session!</p>
        </div>`;
      return;
    }

    list.innerHTML = sessions.map(s => `
      <div class="session-card">
        <div class="session-info">
          <span class="session-name">${s.courseName || 'Session'} (${s.courseCode || ''})</span>
          <span class="session-meta">📅 ${s.sessionDate} &nbsp; ⏱ ${s.startTime} – ${s.endTime}
            ${s.attendeeCount !== undefined ? ` &nbsp; 👥 ${s.attendeeCount} students` : ''}</span>
        </div>
        <div class="session-actions">
          <span class="session-badge badge-${(s.status||'').toLowerCase()}">${s.status}</span>
          <button class="btn-sm" data-session-id="${s.id}" data-session-name="${s.courseName}">View</button>
        </div>
      </div>
    `).join('');

    list.querySelectorAll('[data-session-id]').forEach(btn => {
      btn.addEventListener('click', () => openAttendanceModal(btn.dataset.sessionId, btn.dataset.sessionName));
    });
  }

  /* ── Attendance modal ── */
  const attendanceModal     = document.getElementById('attendanceModal');
  const attendanceModalBody = document.getElementById('attendanceModalBody');
  const attendanceModalTitle = document.getElementById('attendanceModalTitle');

  document.getElementById('attendanceModalClose')?.addEventListener('click', () => {
    if (attendanceModal) attendanceModal.style.display = 'none';
  });

  async function openAttendanceModal(sessionId, name) {
    if (!attendanceModal) return;
    attendanceModal.style.display = 'flex';
    if (attendanceModalTitle) attendanceModalTitle.textContent = `Attendance – ${name}`;
    if (attendanceModalBody) attendanceModalBody.innerHTML = '<p><span class="spinner"></span> Loading…</p>';

    const token = getToken();
    try {
      const records = await api.get(`/attendance/session/${sessionId}`, token);
      renderAttendanceTable(records);
    } catch {
      renderAttendanceTable(MOCK.attendance);
    }
  }

  function renderAttendanceTable(records) {
    if (!attendanceModalBody) return;
    if (!records || records.length === 0) {
      attendanceModalBody.innerHTML = '<p class="text-muted" style="padding:1rem">No attendance records yet.</p>';
      return;
    }
    attendanceModalBody.innerHTML = `
      <table class="attendance-table">
        <thead>
          <tr>
            <th>Student</th>
            <th>Email</th>
            <th>Join Time</th>
            <th>Status</th>
          </tr>
        </thead>
        <tbody>
          ${records.map(r => `
            <tr>
              <td>${r.studentName || '—'}</td>
              <td>${r.email || '—'}</td>
              <td>${r.joinTime || '—'}</td>
              <td><span class="session-badge badge-${(r.status||'').toLowerCase()}">${r.status || '—'}</span></td>
            </tr>
          `).join('')}
        </tbody>
      </table>`;
  }

  /* ── Host session modal ── */
  const hostModal       = document.getElementById('hostModal');
  const hostSessionForm = document.getElementById('hostSessionForm');
  const courseSelect    = document.getElementById('courseSelect');
  const newCourseFields = document.getElementById('newCourseFields');

  document.getElementById('hostSessionBtn')?.addEventListener('click', () => {
    if (hostModal) hostModal.style.display = 'flex';
    // set today's date as default
    const dateInput = document.getElementById('sessionDate');
    if (dateInput) dateInput.value = new Date().toISOString().slice(0,10);
    loadCourses();
  });

  document.getElementById('hostModalClose')?.addEventListener('click', () => {
    if (hostModal) hostModal.style.display = 'none';
    if (hostSessionForm) hostSessionForm.reset();
    if (newCourseFields) newCourseFields.style.display = 'none';
  });

  courseSelect?.addEventListener('change', () => {
    if (newCourseFields) {
      newCourseFields.style.display = courseSelect.value === '__new__' ? 'block' : 'none';
    }
  });

  async function loadCourses() {
    const token = getToken();
    try {
      const courses = await api.get('/courses', token);
      populateCourseDropdown(courses);
    } catch {
      populateCourseDropdown(MOCK.courses);
    }
  }

  function populateCourseDropdown(courses) {
    if (!courseSelect) return;
    courseSelect.innerHTML = '<option value="">Select Course…</option>';
    courses.forEach(c => {
      const opt = document.createElement('option');
      opt.value = c.id;
      opt.textContent = `${c.courseName} (${c.courseCode})`;
      courseSelect.appendChild(opt);
    });
    const newOpt = document.createElement('option');
    newOpt.value = '__new__';
    newOpt.textContent = '+ Create new course';
    courseSelect.appendChild(newOpt);
  }

  hostSessionForm?.addEventListener('submit', async (e) => {
    e.preventDefault();
    const token       = getToken();
    const errorMsg    = document.getElementById('hostErrorMsg');
    const submitBtn   = hostSessionForm.querySelector('[type="submit"]');
    if (errorMsg) errorMsg.textContent = '';

    const courseId    = courseSelect?.value;
    const sessionDate = document.getElementById('sessionDate')?.value;
    const startTime   = document.getElementById('startTime')?.value;
    const endTime     = document.getElementById('endTime')?.value;

    if (!sessionDate || !startTime || !endTime) {
      if (errorMsg) errorMsg.textContent = 'Please fill in all fields.';
      return;
    }

    if (submitBtn) submitBtn.disabled = true;

    try {
      let resolvedCourseId = courseId;

      if (courseId === '__new__') {
        const newCourseName = document.getElementById('newCourseName')?.value.trim();
        const newCourseCode = document.getElementById('newCourseCode')?.value.trim();
        if (!newCourseName || !newCourseCode) {
          if (errorMsg) errorMsg.textContent = 'Please enter course name and code.';
          if (submitBtn) submitBtn.disabled = false;
          return;
        }
        const course = await api.post('/courses', { courseName: newCourseName, courseCode: newCourseCode }, token);
        resolvedCourseId = course.id;
      }

      await api.post('/sessions', {
        courseId: Number(resolvedCourseId),
        sessionDate,
        startTime,
        endTime,
        status: 'ACTIVE',
      }, token);

      if (hostModal) hostModal.style.display = 'none';
      if (hostSessionForm) hostSessionForm.reset();
      if (newCourseFields) newCourseFields.style.display = 'none';
      loadData();
    } catch (err) {
      if (errorMsg) errorMsg.textContent = err.message || 'Failed to create session.';
    } finally {
      if (submitBtn) submitBtn.disabled = false;
    }
  });

  /* ── Load data ── */
  async function loadData() {
    const token = getToken();
    try {
      const [sessions, stats] = await Promise.all([
        api.get('/sessions', token),
        api.get('/sessions/stats', token),
      ]);
      const hosted    = sessions.length;
      const active    = sessions.filter(s => s.status === 'ACTIVE').length;
      const completed = sessions.filter(s => s.status === 'COMPLETED').length;
      updateStats(hosted, active, completed);
      renderSessions(sessions);
    } catch {
      updateStats(MOCK.lecturerStats.hosted, MOCK.lecturerStats.active, MOCK.lecturerStats.completed);
      renderSessions(MOCK.lecturerSessions);
    }
  }

  loadData();
})();
