// export const URL = 'http://TIMW0322.wamas.com';
export const URL = 'http://TIMW0357.wamas.com';

export const SIM_URL = URL + ':8888'

export const SERVER_URL = URL + ':8080';

// Base address of the backend API. New code takes the URL only from here.
export const API_BASE_URL = SERVER_URL + '/api';

// Where the backend serves the dialog JSON files (static files, not under /api).
export const DIALOGS_BASE_URL = SERVER_URL + '/api/dialogs';
