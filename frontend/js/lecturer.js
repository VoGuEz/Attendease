document.addEventListener('DOMContentLoaded', async () => {
    // 1. Security Check - If this fails, it redirects to index.html
    const user = requireRole('LECTURER');
    if (!user) return;

    // 2. Update UI with User Info
    document.getElementById('user-name').textContent = user.name || 'Lecturer';
    document.getElementById('sidebar-user-name').textContent = user.name || 'Lecturer';
    const avatar = (user.name || 'L').charAt(0).toUpperCase();
    document.getElementById('user-avatar').textContent = avatar;
    document.getElementById('sidebar-avatar').textContent = avatar;

    // 3. Initial Data Load
    try {
        await Promise.all([
            loadStats(),
            loadActiveSessions(),
            loadCourses()
        ]);
    } catch (err) {
        console.error("Initialization error:", err);
    }

    // 4. Handle Course Creation
    const createCourseForm = document.getElementById('form-create-course');
    if (createCourseForm) {
        createCourseForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const name = document.getElementById('course-name').value;
            const code = document.getElementById('course-code').value;
            const description = document.getElementById('course-desc')?.value || '';

            try {
                await apiRequest('/courses', 'POST', { name, code, description });
                alert('Course created successfully!');
                closeModal('modal-create-course');
                loadCourses(); // Refresh list
            } catch (err) {
                alert(err.message);
            }
        });
    }
});

async function loadStats() {
    // Basic stats logic
    const courses = await apiRequest('/courses');
    document.getElementById('stat-courses').textContent = courses.length;
}

async function loadCourses() {
    const list = document.getElementById('courses-list');
    try {
        const courses = await apiRequest('/courses');
        if (courses.length === 0) {
            list.innerHTML = '<p class="empty">No courses created yet.</p>';
            return;
        }
        list.innerHTML = courses.map(c => `
            <div class="card course-card">
                <h4>${c.name}</h4>
                <p class="text-muted">${c.code}</p>
                <button class="btn btn-sm" onclick="window.location.href='course-details.html?id=${c.id}'">View Details</button>
            </div>
        `).join('');
    } catch (err) {
        list.innerHTML = '<p class="error">Failed to load courses.</p>';
    }
}

async function loadActiveSessions() {
    const list = document.getElementById('active-sessions-list');
    // Implementation for active sessions...
    list.innerHTML = '<p class="empty-state">No live sessions currently.</p>';
}