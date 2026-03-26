/* =====================================================
   AttendEase – api.js  (API client)
   ===================================================== */

const API_BASE = 'http://localhost:8080/api';

const api = {
  async _request(method, path, data, token) {
    const headers = { 'Content-Type': 'application/json' };
    if (token) headers['Authorization'] = `Bearer ${token}`;

    const opts = { method, headers };
    if (data !== undefined) opts.body = JSON.stringify(data);

    const res = await fetch(`${API_BASE}${path}`, opts);

    let body;
    const ct = res.headers.get('content-type') || '';
    if (ct.includes('application/json')) {
      body = await res.json();
    } else {
      body = await res.text();
    }

    if (!res.ok) {
      const msg = (body && body.message) || (typeof body === 'string' ? body : `HTTP ${res.status}`);
      throw new Error(msg);
    }
    return body;
  },

  post(path, data, token)        { return this._request('POST',   path, data, token); },
  get(path, token)               { return this._request('GET',    path, undefined, token); },
  put(path, data, token)         { return this._request('PUT',    path, data, token); },
  delete(path, token)            { return this._request('DELETE', path, undefined, token); },
};

/* ── Auth helpers ── */
function getToken() { return localStorage.getItem('token'); }
function getUser()  { return JSON.parse(localStorage.getItem('user') || '{}'); }

function requireAuth() {
  if (!getToken()) {
    window.location.href = 'signin.html';
    return false;
  }
  return true;
}

/* ── Mock data (used when backend is unavailable) ── */
function getTodayDate() { return new Date().toISOString().slice(0, 10); }

const MOCK = {
  studentStats: { attended: 7, total: 10, rate: 70 },
  sessions: [
    { id: 1, courseName: 'Computer Science 101', courseCode: 'CS101',
      sessionDate: getTodayDate(),
      startTime: '09:00', endTime: '10:30', status: 'ACTIVE' },
    { id: 2, courseName: 'Mathematics', courseCode: 'MATH201',
      sessionDate: getTodayDate(),
      startTime: '11:00', endTime: '12:30', status: 'UPCOMING' },
  ],
  lecturerStats: { hosted: 5, active: 1, completed: 4 },
  lecturerSessions: [
    { id: 1, courseName: 'Computer Science 101', courseCode: 'CS101',
      sessionDate: getTodayDate(),
      startTime: '09:00', endTime: '10:30', status: 'ACTIVE',
      attendeeCount: 18 },
    { id: 2, courseName: 'Computer Science 101', courseCode: 'CS101',
      sessionDate: '2024-01-10', startTime: '09:00', endTime: '10:30',
      status: 'COMPLETED', attendeeCount: 22 },
  ],
  courses: [
    { id: 1, courseName: 'Computer Science 101', courseCode: 'CS101' },
    { id: 2, courseName: 'Mathematics', courseCode: 'MATH201' },
  ],
  attendance: [
    { studentName: 'Alice Smith',   email: 'alice@example.com', joinTime: '09:05', status: 'PRESENT' },
    { studentName: 'Bob Johnson',   email: 'bob@example.com',   joinTime: '09:12', status: 'LATE'    },
    { studentName: 'Carol White',   email: 'carol@example.com', joinTime: null,    status: 'ABSENT'  },
  ],
};
