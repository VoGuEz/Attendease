document.addEventListener('DOMContentLoaded', async () => {
  const user = requireRole('LECTURER');
  if (!user) return;

  // Initialize UI
  document.getElementById('user-name').textContent = user.name || 'Lecturer';
  document.getElementById('sidebar-user-name').textContent = user.name || 'Lecturer';
  const avatar = (user.name || 'L').charAt(0).toUpperCase();
  document.getElementById('user-avatar').textContent = avatar;
  document.getElementById('sidebar-avatar').textContent = avatar;

  // Load initial data
  try {
    await Promise.all([
      loadStats(),
      loadActiveSessions(),
      loadCourses()
    ]);
  } catch (err) {
    console.error("Initialization error:", err);
  }

  // Handle Create Course Form
  const createForm = document.getElementById('form-create-course');
  if (createForm) {
    createForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const btn = e.target.querySelector('button[type="submit"]');
      const name = document.getElementById('course-name').value;
      const code = document.getElementById('course-code').value;
      const description = document.getElementById('course-desc')?.value || '';

      btn.disabled = true;
      try {
        await apiRequest('/courses', 'POST', { name, code, description });
        closeModal('modal-create-course');
        await loadCourses();
        await loadStats();
      } catch (err) {
        alert(err.message);
      } finally {
        btn.disabled = false;
      }
    });
  }
});

async function loadStats() {
  try {
    const courses = await apiRequest('/courses');
    document.getElementById('stat-courses').textContent = courses.length;
  } catch (err) { console.error(err); }
}

async function loadCourses() {
  const list = document.getElementById('courses-list');
  try {
    const courses = await apiRequest('/courses');
    if (courses.length === 0) {
      list.innerHTML = '<p class="empty-state">No courses yet. Click "New Course" to start.</p>';
      return;
    }
    list.innerHTML = courses.map(c => `
      <div class="card">
        <h4>${c.name}</h4>
        <p class="text-muted">${c.code}</p>
        <div style="margin-top:10px;">
          <button class="btn btn-sm" onclick="window.location.href='course-details.html?id=${c.id}'">Manage</button>
        </div>
      </div>
    `).join('');
  } catch (err) {
    list.innerHTML = '<p class="error">Failed to load courses.</p>';
  }
}

async function loadActiveSessions() {
  const list = document.getElementById('active-sessions-list');
  try {
    const sessions = await apiRequest('/attendance/active');
    if (sessions.length === 0) {
      list.innerHTML = '<p class="empty-state">No live sessions currently.</p>';
      return;
    }
    // Session rendering logic...
  } catch (err) {
    list.innerHTML = '<p class="error">Failed to load sessions.</p>';
  }
}