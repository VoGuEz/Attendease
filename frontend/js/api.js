const API_BASE =
  window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1'
    ? 'http://localhost:8080/api'
    : 'https://attendease-production-e306.up.railway.app/api';

function getToken() { return localStorage.getItem('token'); }

function getUser() {
  const user = localStorage.getItem('user');
  try {
    return user ? JSON.parse(user) : null;
  } catch (e) {
    console.error("User data corrupted in LocalStorage");
    return null;
  }
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
    
    if (response.status === 401) {
      console.warn("Unauthorized request. Redirecting to login...");
      clearAuth();
      window.location.href = 'index.html';
      return;
    }

    const data = await response.json();

    if (!response.ok) {
      if (data.message && (data.message.includes('JWT') || data.message.includes('signature'))) {
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

function requireRole(expectedRole) {
  const token = getToken();
  const user = getUser();

  console.log("Checking Auth Status...");
  console.log("Token exists:", !!token);
  console.log("User Object:", user);

  if (!token || !user) {
    console.warn("No valid session found. Redirecting...");
    window.location.href = 'index.html';
    return null;
  }

  // Check role strictly but handle case-sensitivity
  const userRole = user.role ? user.role.toUpperCase() : "";
  if (userRole !== expectedRole.toUpperCase()) {
    console.error(`Role Mismatch! Expected: ${expectedRole}, Got: ${userRole}`);
    window.location.href = 'index.html';
    return null;
  }

  return user;
}

function logout() {
  clearAuth();
  window.location.href = 'index.html';
}