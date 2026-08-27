// path: crs-frontend/src/api/useCourses.ts
// purpose: custom hook quan ly viec goi GET /api/courses (tim kiem + phan trang)
// va 4 trang thai Loading/Success/Empty/Error, tach rieng khoi component hien thi
import { useCallback, useEffect, useState } from 'react';
import axios from 'axios';
import { getCourses } from './courseApi';
import type { ApiErrorResponse } from '../types/apiError';
import type { Course } from '../types/course';

export type LoadState = 'loading' | 'success' | 'empty' | 'error';

export function useCourses(keyword: string, page: number, size = 10) {
  const [courses, setCourses] = useState<Course[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [state, setState] = useState<LoadState>('loading');
  const [errorMessage, setErrorMessage] = useState('');

  const fetchCourses = useCallback(async () => {
    setState('loading');
    setErrorMessage('');

    try {
      const response = await getCourses(keyword, page, size);
      const data = response.data;

      setCourses(data.content);
      setTotalPages(data.totalPages);
      setState(data.content.length === 0 ? 'empty' : 'success');
    } catch (error) {
      let message = 'Đã xảy ra lỗi không xác định, vui lòng thử lại.';

      if (axios.isAxiosError<ApiErrorResponse>(error)) {
        if (error.response?.data?.message) {
          message = error.response.data.message;
        } else if (!error.response) {
          // Khong nhan duoc response nao: Gateway hoac course-service co the dang tat.
          message = 'Không kết nối được tới hệ thống. Vui lòng thử lại sau.';
        }
      }

      setCourses([]);
      setTotalPages(0);
      setErrorMessage(message);
      setState('error');
    }
  }, [keyword, page, size]);

  useEffect(() => {
    void fetchCourses();
  }, [fetchCourses]);

  return { courses, totalPages, state, errorMessage, refetch: fetchCourses };
}
