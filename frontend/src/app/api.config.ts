// The backend server. To work on the shared server on Alex's laptop use 'http://TIMW0342.wamas.com:8080'.
export const SERVER_URL = 'http://localhost:8080';

// Base address of the backend API. New code takes the URL only from here.
export const API_BASE_URL = SERVER_URL + '/api';

// Where the backend serves the dialog JSON files (static files, not under /api).
export const DIALOGS_BASE_URL = SERVER_URL + '/api/dialogs';
