function initTheme() {
    const savedTheme = localStorage.getItem('theme') || 'system';
    applyTheme(savedTheme);
    
    document.querySelectorAll('.theme-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            const theme = btn.dataset.themeBtn;
            localStorage.setItem('theme', theme);
            applyTheme(theme);
        });
    });
}

function applyTheme(theme) {
    const html = document.documentElement;
    
    document.querySelectorAll('.theme-btn').forEach(btn => {
        btn.classList.remove('active');
    });
    
    if (theme === 'system') {
        document.querySelector('[data-theme-btn="system"]').classList.add('active');
        
        if (window.matchMedia('(prefers-color-scheme: dark)').matches) {
            html.setAttribute('data-theme', 'dark');
        } else {
            html.setAttribute('data-theme', 'light');
        }
        
        window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', e => {
            if (localStorage.getItem('theme') === 'system') {
                html.setAttribute('data-theme', e.matches ? 'dark' : 'light');
            }
        });
    } else if (theme === 'dark') {
        document.querySelector('[data-theme-btn="dark"]').classList.add('active');
        html.setAttribute('data-theme', 'dark');
    } else if (theme === 'light') {
        document.querySelector('[data-theme-btn="light"]').classList.add('active');
        html.setAttribute('data-theme', 'light');
    }
}