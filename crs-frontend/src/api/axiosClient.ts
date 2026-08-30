// path: crs-frontend/src/api/axiosClient.ts
// purpose: axios instance duy nhat cua frontend, tro ve api-gateway va tu dong dinh kem
// Authorization header neu co token trong localStorage.
// LUU Y: day la ban toi gian - Buoi 8 se bo sung Response Interceptor de tu dong
// dang xuat khi gap loi 401 (token het han), khong can sua lai doan code duoi day.
import axios from 'axios';

const axiosClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

axiosClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('crs_token');

  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  return config;
});

// Response Interceptor - MOI o Buoi 8
axiosClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (axios.isAxiosError(error) && error.response?.status === 401) {
      localStorage.removeItem('crs_token');
      localStorage.removeItem('crs_user');

      // Dung window.location thay vi useNavigate() vi day la file thuan TypeScript,
      // khong phai component - khong the dung React Hook o day.
      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    }

    return Promise.reject(error);
  },
);

export default axiosClient;
