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

export default axiosClient;
