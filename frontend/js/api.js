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
  console.log("Saving Auth to LocalStorage...", { token: !!token, user });
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
      console.warn("Unauthorized (401). Clearing session...");
      clearAuth();
      window.location.href = 'index.html';
      return;
    }

    const data = await response.json();

    if (!response.ok) {
      // Fix for the "Purple Bar" loop / Broken JWTs
      if (data.message && (data.message.includes('JWT') || data.message.includes('signature'))) {
        console.error("JWT Signature Error. Logging out.");
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

  console.log("--- Auth Check ---");
  console.log("Token present:", !!token);
  console.log("User in storage:", user);

  if (!token || !user) {
    console.warn("No session found. Redirecting to login...");
    window.location.href = 'index.html';
    return null;
  }

  // Handle various role formats (e.g., "LECTURER", "Lecturer", " lecturer ")
  const userRole = (user.role || "").trim().toUpperCase();
  const targetRole = expectedRole.trim().toUpperCase();

  if (userRole !== targetRole) {
    console.error(`Access Denied. Expected: ${targetRole}, but User is: ${userRole}`);
    // If they are a student trying to access lecturer, send them to student dashboard instead
    window.location.href = userRole === 'STUDENT' ? 'student.html' : 'index.html';
    return null;
  }

  return user;
}

function logout() {
  clearAuth();
  window.location.href = 'index.html';
}