document.addEventListener('DOMContentLoaded', async () => {
    // Security check: redirects to login if you aren't a Lecturer
    const user = requireRole('LECTURER');
    if (!user) return;

    // Update UI
    document.getElementById('user-name').textContent = user.name || 'Lecturer';
    const avatar = (user.name || 'L').charAt(0).toUpperCase();
    document.getElementById('user-avatar').textContent = avatar;

    try {
        await Promise.all([
            loadStats(),
            loadActiveSessions(),
            loadCourses()
        ]);
    } catch (err) {
        console.error("Load error:", err);
    }
});

async function loadCourses() {
    const list = document.getElementById('courses-list');
    try {
        const courses = await apiRequest('/courses');
        list.innerHTML = courses.map(c => `
            <div class="card">
                <h4>${c.name}</h4>
                <p>${c.code}</p>
            </div>
        `).join('');
    } catch (err) {
        list.innerHTML = '<p>Failed to load courses.</p>';
    }
}