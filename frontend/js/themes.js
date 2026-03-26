const THEMES = [
  { id: 'dark',   label: 'Dark',   color: '#6366f1', bg: '#0f172a' },
  { id: 'light',  label: 'Light',  color: '#6366f1', bg: '#f8fafc' },
  { id: 'blue',   label: 'Blue',   color: '#3b82f6', bg: '#0c1e35' },
  { id: 'purple', label: 'Purple', color: '#9333ea', bg: '#1a0533' },
  { id: 'ocean',  label: 'Ocean',  color: '#06b6d4', bg: '#042f2e' },
  { id: 'forest', label: 'Forest', color: '#22c55e', bg: '#052e16' },
  { id: 'sunset', label: 'Sunset', color: '#f97316', bg: '#1c0505' },
];

function getCurrentTheme() {
  return localStorage.getItem('theme') || 'dark';
}

function applyTheme(themeId) {
  document.documentElement.setAttribute('data-theme', themeId);
  localStorage.setItem('theme', themeId);
  document.querySelectorAll('.theme-option').forEach(el => {
    el.classList.toggle('active', el.dataset.theme === themeId);
  });
}

function initTheme() {
  applyTheme(getCurrentTheme());
}

function renderThemeSwitcher(containerId) {
  const container = document.getElementById(containerId);
  if (!container) return;

  const current = getCurrentTheme();
  container.innerHTML = `
    <div class="theme-switcher">
      <button class="theme-btn" onclick="toggleThemeDropdown(this)" title="Change theme">
        🎨
      </button>
      <div class="theme-dropdown" id="theme-dropdown">
        ${THEMES.map(t => `
          <div class="theme-option ${t.id === current ? 'active' : ''}"
               data-theme="${t.id}"
               title="${t.label}"
               style="background: linear-gradient(135deg, ${t.bg} 50%, ${t.color} 50%);"
               onclick="applyTheme('${t.id}')">
          </div>
        `).join('')}
      </div>
    </div>
  `;
}

function toggleThemeDropdown(btn) {
  const dropdown = document.getElementById('theme-dropdown');
  if (dropdown) dropdown.classList.toggle('open');
}

document.addEventListener('click', (e) => {
  const dropdown = document.getElementById('theme-dropdown');
  if (dropdown && !e.target.closest('.theme-switcher')) {
    dropdown.classList.remove('open');
  }
});

initTheme();
