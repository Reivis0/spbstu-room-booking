import { apiClient } from './client';
import { User } from '../types';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  access_token: string;
  refresh_token: string;
  profile: {
    id: string;
    email: string;
    role: string;
  };
}

export interface ProfileResponse {
  id: string;
  email: string;
  firstname: string;
  lastname: string;
  role: string;
}

export const authApi = {
  login: async (credentials: LoginRequest): Promise<LoginResponse> => {
    const response = await apiClient.post<LoginResponse>('/auth/login', credentials);
    if (response.data.access_token) {
      localStorage.setItem('accessToken', response.data.access_token);
      console.log('[Auth] Access token saved to localStorage');
    }
    if (response.data.refresh_token) {
      localStorage.setItem('refreshToken', response.data.refresh_token);
      console.log('[Auth] Refresh token saved to localStorage');
    }
    return response.data;
  },

  getProfile: async (): Promise<User> => {
    const response = await apiClient.get<ProfileResponse>('/auth/profile');
    const profile = response.data;
    return {
      id: profile.id,
      email: profile.email,
      firstname: profile.firstname,
      lastname: profile.lastname,
      name: `${profile.firstname} ${profile.lastname}`,
      role: profile.role as 'student' | 'teacher' | 'admin',
    };
  },

  refreshToken: async (refreshToken: string): Promise<{ access_token: string }> => {
    const response = await apiClient.post<{ access_token: string }>('/auth/refresh', {
      refresh_token: refreshToken,
    });
    if (response.data.access_token) {
      localStorage.setItem('accessToken', response.data.access_token);
      console.log('[Auth] Access token refreshed');
    }
    return response.data;
  },

  logout: (): void => {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
  },
};

