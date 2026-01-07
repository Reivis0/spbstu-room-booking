import { create } from 'zustand';
import { User } from '../types';
import { authApi } from '../api/auth';

interface AuthState {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (email: string, password: string) => Promise<void>;
  logout: () => void;
  setUser: (user: User | null) => void;
  loadProfile: () => Promise<void>;
  checkAuth: () => void;
}

export const useAuthStore = create<AuthState>((set, get) => ({
  user: null,
  isAuthenticated: false,
  isLoading: false,

  login: async (email: string, password: string) => {
    try {
      set({ isLoading: true });
      const loginResponse = await authApi.login({ email, password });
      console.log('[Auth Store] Login successful, token saved');
      
      if (loginResponse.profile) {
        const user: User = {
          id: loginResponse.profile.id,
          email: loginResponse.profile.email,
          firstname: '',
          lastname: '',
          name: loginResponse.profile.email,
          role: loginResponse.profile.role as 'student' | 'teacher' | 'admin',
        };
        try {
          const fullProfile = await authApi.getProfile();
          set({ user: fullProfile, isAuthenticated: true });
          console.log('[Auth Store] Full profile loaded');
        } catch (error) {
          console.warn('[Auth Store] Failed to load full profile, using basic info');
          set({ user, isAuthenticated: true });
        }
      } else {
        await get().loadProfile();
        console.log('[Auth Store] Profile loaded');
      }
    } catch (error) {
      console.error('[Auth Store] Login error:', error);
      set({ isLoading: false });
      throw error;
    } finally {
      set({ isLoading: false });
    }
  },

  logout: () => {
    authApi.logout();
    set({ user: null, isAuthenticated: false });
    if (window.location.pathname !== '/') {
      window.location.href = '/';
    }
  },

  setUser: (user) => set({ user, isAuthenticated: !!user }),

  loadProfile: async () => {
    try {
      const token = localStorage.getItem('accessToken');
      if (!token) {
        console.warn('[Auth Store] No token found, cannot load profile');
        set({ user: null, isAuthenticated: false });
        return;
      }
      console.log('[Auth Store] Loading profile with token');
      const user = await authApi.getProfile();
      set({ user, isAuthenticated: true });
      console.log('[Auth Store] Profile loaded successfully:', user);
    } catch (error: any) {
      console.error('[Auth Store] Profile load error:', error);
      if (error?.response?.status === 401 || error?.response?.status === 403) {
        authApi.logout();
        set({ user: null, isAuthenticated: false });
      } else {
        set({ user: null, isAuthenticated: false });
      }
      throw error;
    }
  },

  checkAuth: () => {
    const token = localStorage.getItem('accessToken');
    console.log('[Auth Store] Checking auth, token exists:', !!token);
    if (token) {
      get().loadProfile().catch((error) => {
        console.error('[Auth Store] Failed to load profile on checkAuth:', error);
        if (error?.response?.status === 401 || error?.response?.status === 403) {
          console.log('[Auth Store] Unauthorized, clearing token');
          authApi.logout();
        } else {
          console.warn('[Auth Store] Network or other error, keeping token');
        }
      });
    } else {
      console.log('[Auth Store] No token, setting unauthenticated');
      set({ user: null, isAuthenticated: false });
    }
  },
}));


