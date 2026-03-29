// ===== Edit/Delete Course (Continued) =====

async function confirmDeleteCourse(courseId, courseName) {
  showConfirmDialog(
    'Delete Course?',
    `Are you sure you want to delete "${courseName}"? This will also delete all its sessions and attendance records.`,
    async () => {
      try {
        await apiRequest(`/courses/${courseId}`, 'DELETE');
        showToast('Course deleted successfully!', 'success');
        await loadCourses();
        updateStats();
        // If we were viewing sessions for this course, go back to overview
        if (activeSection === 'sessions') showSection('overview');
      } catch (err) {
        showToast(err.message, 'error');
      }
    }
  );
}

// ===== Edit/Delete Session =====

function openEditSession(sessionId, date, start, end) {
  document.getElementById('edit-session-id').value = sessionId;
  document.getElementById('edit-session-date').value = date;
  document.getElementById('edit-session-start').value = start;
  document.getElementById('edit-session-end').value = end;
  openModal('modal-edit-session');
}

async function handleEditSession(e) {
  e.preventDefault();
  const btn = e.target.querySelector('button[type="submit"]');
  const sessionId = document.getElementById('edit-session-id').value;
  const data = {
    sessionDate: document.getElementById('edit-session-date').value,
    startTime: document.getElementById('edit-session-start').value,
    endTime: document.getElementById('edit-session-end').value
  };

  btn.disabled = true;
  btn.innerHTML = '<span class="spinner"></span>';
  try {
    await apiRequest(`/sessions/${sessionId}`, 'PUT', data);
    closeModal('modal-edit-session');
    showToast('Session updated successfully!', 'success');
    await loadAll();
  } catch (err) {
    showToast(err.message, 'error');
  } finally {
    btn.disabled = false;
    btn.innerHTML = 'Save Changes';
  }
}

async function confirmDeleteSession(sessionId) {
  showConfirmDialog(
    'Delete Session?',
    'Are you sure you want to delete this session? Attendance data for this session will be lost.',
    async () => {
      try {
        await apiRequest(`/sessions/${sessionId}`, 'DELETE');
        showToast('Session deleted successfully!', 'success');
        await loadAll();
      } catch (err) {
        showToast(err.message, 'error');
      }
    }
  );
}

// ===== Helper: Search/Filters =====
function filterCards(containerId, query) {
  const container = document.getElementById(containerId);
  const cards = container.querySelectorAll('.item-card');
  const q = query.toLowerCase();
  cards.forEach(card => {
    const text = card.innerText.toLowerCase();
    card.style.display = text.includes(q) ? '' : 'none';
  });
}

function filterTableRows(containerId, query) {
  const container = document.getElementById(containerId);
  const rows = container.querySelectorAll('tbody tr');
  const q = query.toLowerCase();
  rows.forEach(row => {
    const text = row.innerText.toLowerCase();
    row.style.display = text.includes(q) ? '' : 'none';
  });
}