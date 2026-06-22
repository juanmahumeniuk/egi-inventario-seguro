import { apiFetch, USE_MOCK } from './api';

export interface LoginResponse {
  username: string;
  token: string;
  role: string;
}

export const authService = {
  async login(username: string, password: string): Promise<LoginResponse> {
    if (USE_MOCK) {
      // Simulate Active Directory/LDAP query delay in backend
      await new Promise(resolve => setTimeout(resolve, 800));
      
      const lowerUser = username.trim().toLowerCase();
      if ((lowerUser === 'admin' && password === 'admin123') || lowerUser === 'admin' || lowerUser === 'tecnico' || lowerUser === 'itu') {
        const mockResponse: LoginResponse = {
          username: username.trim(),
          token: `mock_jwt_token_${Math.random().toString(36).substring(2)}`,
          role: lowerUser === 'admin' ? 'ADMIN' : 'TECNICO'
        };
        localStorage.setItem('itu_auth_token', mockResponse.token);
        return mockResponse;
      } else {
        // Fallback for educational ease of testing, allow login but with regular role
        const mockResponse: LoginResponse = {
          username: username.trim(),
          token: `mock_jwt_token_user`,
          role: 'USER'
        };
        localStorage.setItem('itu_auth_token', mockResponse.token);
        return mockResponse;
      }
    }

    // Call real Spring Boot backend endpoint which does the OpenLDAP auth
    // and returns JWT if successful.
    const response = await apiFetch<LoginResponse>('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username, password })
    });
    
    localStorage.setItem('itu_auth_token', response.token);
    return response;
  },

  logout(): void {
    localStorage.removeItem('itu_auth_token');
    localStorage.removeItem('itu_logged_user');
  }
};
