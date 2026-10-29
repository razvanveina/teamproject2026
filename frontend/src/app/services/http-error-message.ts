import { HttpErrorResponse } from '@angular/common/http';

// Turns an HTTP error into a short message for the page.
export function httpErrorMessage(error: HttpErrorResponse, notFoundMessage: string): string {
  if (error.status === 0) {
    return 'Serverul nu răspunde.';
  }
  if (error.status === 404) {
    return notFoundMessage;
  }
  return `A apărut o eroare (status ${error.status}). Încearcă din nou.`;
}
