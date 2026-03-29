const API_BASE =
  window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1'
    ? 'http://localhost:8080/api'
    : 'https://attendease-production-e306.up.railway.app/api';

function getToken() {
  return localStorage.getItem('token');
}

function getUser() {
  const user = localStorage.getItem('user');
  return user ? JSON.parse(user) : null;
}

function setAuth(token, user) {
  localStorage.setItem('token', token);
  localStorage.setItem('user', JSON.stringify(user));
}

function clearAuth() {
  localStorage.removeItem('token');
  localStorage.removeItem('user');
}

async function apiRequest(endpoint, method = 'GET', body = null) {
  const headers = { 'Content-Type': 'application/json' };
  const token = getToken();
  if (token) headers['Authorization'] = `Bearer ${token}`;

  const options = { method, headers };
  if (body) options.body = JSON.stringify(body);

  try {
    const response = await fetch(`${API_BASE}${endpoint}`, options);
    
    // Check for 401 (Unauthorized)
    if (response.status === 401) {
      clearAuth();
      window.location.href = 'index.html';
      throw new Error('Session expired. Please log in again.');
    }

    const data = await response.json();

    if (!response.ok) {
      // FIX: If the error message mentions JWT or Signature, it's a broken token.
      // We must clear it, otherwise the user is stuck in an error loop.
      if (data.message && (data.message.includes('JWT') || data.message.includes('signature'))) {
        console.warn("Invalid JWT detected. Clearing session...");
        clearAuth();
        window.location.href = 'index.html';
      }
      throw new Error(data.message || 'Request failed');
    }
    
    return data;
  } catch (error) {
    console.error("API Request Error:", error);
    throw error;
  }
}

function requireAuth() {
  const token = getToken();
  const user = getUser();
  if (!token || !user) {
    window.location.href = 'index.html';
    return null;
  }
  return user;
}

function requireRole(role) {
  const user = requireAuth();
  if (!user) return null;
  if (user.role !== role) {
    window.location.href = 'dashboard.html';
    return null;
  }
  return user;
}

function logout() {
  clearAuth();
  window.location.href = 'index.html';
}