// API Configuration
const API_BASE_URL = 'http://localhost:8080/api';
let authToken = localStorage.getItem('token') || null;
let currentUser = JSON.parse(localStorage.getItem('currentUser')) || null;

console.log('API initialized - Token:', authToken ? 'EXISTS' : 'MISSING');

// ===== UTILITY FUNCTIONS =====
function setAuthToken(token, user) {
  console.log('Setting auth token...');
  authToken = token;
  currentUser = user;
  
  try {
    localStorage.setItem('token', token);
    localStorage.setItem('currentUser', JSON.stringify(user));
    console.log('✅ Token and user saved to localStorage');
    console.log('User:', user.email, '| Role:', user.role);
  } catch (error) {
    console.error('❌ Error saving to localStorage:', error);
  }
}

function clearAuthToken() {
  console.log('Clearing auth token...');
  authToken = null;
  currentUser = null;
  localStorage.removeItem('token');
  localStorage.removeItem('currentUser');
  console.log('✅ Token cleared');
}

function getHeaders() {
  const headers = {'Content-Type': 'application/json'};
  if (authToken) {
    headers['Authorization'] = `Bearer ${authToken}`;
  }
  return headers;
}

async function apiCall(endpoint, method = 'GET', body = null) {
  const url = `${API_BASE_URL}${endpoint}`;
  const options = {
    method,
    headers: getHeaders()
  };

  if (body) {
    options.body = JSON.stringify(body);
  }

  try {
    console.log(`📤 ${method} ${endpoint}`);
    console.log('Headers:', options.headers);
    console.log('Body:', body);
    
    const response = await fetch(url, options);
    
    console.log('Response status:', response.status);
    
    if (response.status === 401) {
      console.error('❌ Unauthorized - clearing token');
      clearAuthToken();
      window.location.href = 'login.html';
      return null;
    }

    const data = await response.json();
    
    console.log('Response data:', data);

    if (!response.ok) {
      console.error(`❌ API Error: ${response.status} - ${data.message}`);
      throw new Error(data.message || `HTTP ${response.status}`);
    }

    console.log(`✅ Success:`, data);
    return data;
  } catch (error) {
    console.error('❌ API Error:', error.message);
    throw error;
  }
}

// ===== AUTH ENDPOINTS =====
async function signup(fullname, email, password, role) {
  return apiCall('/auth/signup', 'POST', {fullname, email, password, role});
}

async function login(email, password) {
  console.log('🔐 Attempting login for:', email);
  return apiCall('/auth/login', 'POST', {email, password});
}

// ===== PROFILE ENDPOINTS =====
async function getProfile() {
  return apiCall('/profile', 'GET');
}

async function updateProfile(fullname, email) {
  return apiCall('/profile', 'PUT', {fullname, email});
}

// ===== SESSION ENDPOINTS =====
async function createSession(title, durationMinutes) {
  return apiCall('/sessions', 'POST', {title, durationMinutes});
}

async function getSessionByCode(code) {
  return apiCall(`/sessions/code/${code}`, 'GET');
}

async function getUserSessions() {
  return apiCall('/sessions', 'GET');
}

async function getSessionById(id) {
  return apiCall(`/sessions/${id}`, 'GET');
}

async function endSession(sessionId) {
  return apiCall(`/sessions/${sessionId}/end`, 'PUT');
}

async function getLecturerKpis() {
  return apiCall('/sessions/kpis/lecturer', 'GET');
}

// ===== ATTENDANCE ENDPOINTS =====
async function joinSession(sessionCode, fullname, indexNumber, level, latitude, longitude) {
  return apiCall('/attendance/join', 'POST', {
    sessionCode,
    fullname,
    indexNumber,
    level,
    latitude,
    longitude
  });
}

async function getSessionAttendance(sessionId) {
  return apiCall(`/attendance/session/${sessionId}`, 'GET');
}

async function getUserAttendance() {
  return apiCall('/attendance', 'GET');
}

async function exportAttendanceCSV(sessionId) {
  const url = `${API_BASE_URL}/attendance/session/${sessionId}/csv`;
  const headers = getHeaders();
  try {
    const response = await fetch(url, {headers});
    if (response.ok) {
      const blob = await response.blob();
      const downloadUrl = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = downloadUrl;
      a.download = `attendance-${sessionId}.csv`;
      a.click();
      window.URL.revokeObjectURL(downloadUrl);
    }
  } catch (error) {
    console.error('CSV Export Error:', error);
  }
}

async function getStudentKpis() {
  try {
    console.log('Fetching student KPIs...');
    const response = await apiCall('/profile/kpi/student', 'GET');
    console.log('Student KPIs received:', response);
    return response;
  } catch (error) {
    console.error('Error fetching student KPIs:', error);
    return {
      attended: 0,
      totalSessions: 0,
      attendanceRate: '0%',
      status: 'Unable to load'
    };
  }
}

// ===== UI HELPERS =====
function showMessage(elementId, message, type = 'error') {
  const el = document.getElementById(elementId);
  if (!el) return;
  el.textContent = message;
  el.className = `form-message ${type}`;
  el.style.display = 'block';
}

function clearMessage(elementId) {
  const el = document.getElementById(elementId);
  if (!el) return;
  el.textContent = '';
  el.className = 'form-message';
  el.style.display = 'none';
}

function showAlert(type, message) {
  const alert = document.createElement('div');
  alert.className = `alert show ${type}`;
  alert.textContent = message;
  document.body.insertBefore(alert, document.body.firstChild);
  setTimeout(() => alert.remove(), 5000);
}