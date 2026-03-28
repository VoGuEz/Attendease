const THEMES = [
  { id: 'dark',   label: 'Dark',   color: '#6366f1', bg: '#0f172a' },
  { id: 'light',  label: 'Light',  color: '#6366f1', bg: '#f8fafc' },
  { id: 'blue',   label: 'Blue',   color: '#3b82f6', bg: '#0c1e35' },
  { id: 'purple', label: 'Purple', color: '#9333ea', bg: '#1a0533' },
  { id: 'ocean',  label: 'Ocean',  color: '#06b6d4', bg: '#042f2e' },
  { id: 'forest', label: 'Forest', color: '#22c55e', bg: '#052e16' },
  { id: 'sunset', label: 'Sunset', color: '#f97316', bg: '#1c0505' },
  { id: 'ruby',   label: 'Ruby',   color: '#ef4444', bg: '#2a0b14' },
  { id: 'gold',   label: 'Gold',   color: '#fbbf24', bg: '#231403' },
  { id: 'aurora',    label: 'Aurora',    color: '#2dd4bf', bg: '#071a2f' },
  { id: 'galaxy',    label: 'Galaxy',    color: '#8b5cf6', bg: '#0a0015' },
  { id: 'sakura',    label: 'Sakura',    color: '#f472b6', bg: '#1f0a14' },
  { id: 'midnight',  label: 'Midnight',  color: '#38bdf8', bg: '#020617' },
  { id: 'lavender',  label: 'Lavender',  color: '#c084fc', bg: '#1a1025' },
  { id: 'mint',      label: 'Mint',      color: '#34d399', bg: '#041f1a' },
  { id: 'arctic',    label: 'Arctic',    color: '#0284c7', bg: '#f0f9ff' },
  { id: 'neon',      label: 'Neon',      color: '#e879f9', bg: '#0a0a0a' },
  { id: 'copper',    label: 'Copper',    color: '#d97706', bg: '#1a0e08' },
];

const WALLPAPERS = [
  { id: 'none',     label: 'None',        emoji: null,  preview: '🚫' },
  { id: 'pencils',  label: 'Pencils',     emoji: '✏️',  preview: '✏️' },
  { id: 'books',    label: 'Books',       emoji: '📚',  preview: '📚' },
  { id: 'stars',    label: 'Stars',       emoji: '⭐',  preview: '⭐' },
  { id: 'leaves',   label: 'Leaves',      emoji: '🍃',  preview: '🍃' },
  { id: 'music',    label: 'Music Notes', emoji: '🎵',  preview: '🎵' },
  { id: 'coffee',   label: 'Coffee',      emoji: '☕',  preview: '☕' },
  { id: 'clouds',   label: 'Clouds',      emoji: '☁️',  preview: '☁️' },
  { id: 'bubbles',  label: 'Bubbles',     emoji: '🫧',  preview: '🫧' },
  { id: 'lightning', label: 'Lightning',  emoji: '⚡',  preview: '⚡' },
  { id: 'crystals',  label: 'Crystals',   emoji: '💎',  preview: '💎' },
  { id: 'flames',    label: 'Flames',     emoji: '🔥',  preview: '🔥' },
  { id: 'planets',   label: 'Planets',    emoji: '🪐',  preview: '🪐' },
  { id: 'cherry',    label: 'Cherry Blossom', emoji: '🌸', preview: '🌸' },
  { id: 'aurora',    label: 'Aurora',     emoji: '✨',  preview: '✨' },
  { id: 'ocean',     label: 'Ocean Wave', emoji: '🌊',  preview: '🌊' },
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
  if (!dropdown) return;
  dropdown.classList.toggle('open');
  if (dropdown.classList.contains('open')) {
    const rect = btn.getBoundingClientRect();
    dropdown.style.top = (rect.bottom + 8) + 'px';
    dropdown.style.right = (window.innerWidth - rect.right) + 'px';
  }
}

document.addEventListener('click', (e) => {
  const dropdown = document.getElementById('theme-dropdown');
  if (dropdown && !e.target.closest('.theme-switcher')) {
    dropdown.classList.remove('open');
  }
});

/* ===========================
   Wallpaper Functions
   =========================== */

const WALLPAPER_ITEM_COUNT = 22;

function getCurrentWallpaper() {
  return localStorage.getItem('wallpaper') || 'none';
}

function applyWallpaper(wallpaperId) {
  localStorage.setItem('wallpaper', wallpaperId);

  const existing = document.getElementById('wallpaper-layer');
  if (existing) existing.remove();

  document.querySelectorAll('.wallpaper-option').forEach(el => {
    el.classList.toggle('active', el.dataset.wallpaper === wallpaperId);
  });

  if (wallpaperId === 'none') return;

  const wp = WALLPAPERS.find(w => w.id === wallpaperId);
  if (!wp || !wp.emoji) return;

  const layer = document.createElement('div');
  layer.id = 'wallpaper-layer';

  for (let i = 0; i < WALLPAPER_ITEM_COUNT; i++) {
    const span = document.createElement('span');
    span.className = 'wallpaper-item';
    span.textContent = wp.emoji;
    const size = 1 + Math.random() * 1.8;
    const left = Math.random() * 100;
    const top = Math.random() * 100;
    const duration = 9 + Math.random() * 10;
    const delay = -(Math.random() * 12);
    const rotate = Math.random() * 360;
    span.style.cssText =
      `left:${left}vw;top:${top}vh;font-size:${size}rem;` +
      `animation-duration:${duration}s;animation-delay:${delay}s;` +
      `--wp-rotate:${rotate}deg;`;
    layer.appendChild(span);
  }

  document.body.prepend(layer);
}

function renderWallpaperPicker(containerId) {
  const container = document.getElementById(containerId);
  if (!container) return;

  const currentWp = getCurrentWallpaper();
  container.innerHTML = WALLPAPERS.map(w =>
    `<div class="wallpaper-option ${w.id === currentWp ? 'active' : ''}"
          data-wallpaper="${w.id}"
          title="${w.label}">
       <span class="wp-emoji">${w.preview}</span>
       <span class="wp-label">${w.label}</span>
     </div>`
  ).join('');

  container.addEventListener('click', (e) => {
    const option = e.target.closest('.wallpaper-option');
    if (option && option.dataset.wallpaper) {
      applyWallpaper(option.dataset.wallpaper);
    }
  });
}

function initWallpaper() {
  applyWallpaper(getCurrentWallpaper());
}

initTheme();
initWallpaper();
