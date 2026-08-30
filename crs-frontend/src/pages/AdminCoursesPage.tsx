// path: crs-frontend/src/pages/AdminCoursesPage.tsx
// purpose: rap CourseForm + CourseList + Pagination + SearchBox, xu ly Them/Sua/Xoa
// va dong bo lai danh sach sau moi thao tac thanh cong, chuyen tu App.tsx cua Buoi 7
import { useCallback, useState } from 'react';
import axios from 'axios';
import { createCourse, deleteCourse, updateCourse } from '../api/courseApi';
import { useCourses } from '../api/useCourses';
import CourseForm from '../components/CourseForm';
import CourseList from '../components/CourseList';
import Pagination from '../components/Pagination';
import SearchBox from '../components/SearchBox';
import type { ApiErrorResponse } from '../types/apiError';
import type { Course, CourseFormValues } from '../types/course';

function AdminCoursesPage() {
  const [keyword, setKeyword] = useState('');
  const [page, setPage] = useState(0);
  const [editingCourse, setEditingCourse] = useState<Course | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [formResetVersion, setFormResetVersion] = useState(0);
  const { courses, totalPages, state, errorMessage, refetch } = useCourses(keyword, page);

  const handleSearch = useCallback((newKeyword: string) => {
    setKeyword(newKeyword);
    setPage(0); // Moi lan tim kiem moi, luon quay ve trang dau.
  }, []);

  const extractErrorMessage = (error: unknown): string => {
    if (axios.isAxiosError<ApiErrorResponse>(error)) {
      const data: unknown = error.response?.data;

      // Gateway tra 401 voi body rong; Spring Security co the tra body mac dinh cho 403.
      if (error.response?.status === 401) {
        return 'Bạn chưa đăng nhập hoặc phiên đăng nhập đã hết hạn.';
      }

      if (error.response?.status === 403) {
        return 'Bạn không có quyền thực hiện thao tác này.';
      }

      if (typeof data === 'object' && data !== null) {
        const apiError = data as ApiErrorResponse;

        if (apiError.message) {
          return apiError.message;
        }

        // Validation server tra ve object co ten field lam key.
        const validationKeys = ['tenMonHoc', 'soTinChi', 'soChoToiDa'] as const;
        const firstFieldError = validationKeys.map((key) => apiError[key]).find(
          (value): value is string => typeof value === 'string',
        );

        if (firstFieldError) {
          return firstFieldError;
        }
      }

      if (typeof data === 'string' && data.trim()) {
        return data;
      }

      if (!error.response) {
        return 'Không kết nối được tới hệ thống. Vui lòng thử lại sau.';
      }
    }

    return 'Đã xảy ra lỗi, vui lòng thử lại.';
  };

  const handleFormSubmit = async (values: CourseFormValues) => {
    setSubmitting(true);
    setFormError(null);

    try {
      if (editingCourse) {
        await updateCourse(editingCourse.id, values);
      } else {
        await createCourse(values);
        // editingCourse da la null trong che do Them, nen can mot tin hieu rieng
        // de CourseForm reset ve rong sau khi tao thanh cong.
        setFormResetVersion((version) => version + 1);
      }

      setEditingCourse(null);
      await refetch();
    } catch (error) {
      setFormError(extractErrorMessage(error));
    } finally {
      setSubmitting(false);
    }
  };

  const handleEdit = (course: Course) => {
    setFormError(null);
    setEditingCourse(course);
  };

  const handleCancel = () => {
    setFormError(null);
    setEditingCourse(null);
  };

  const handleDelete = async (course: Course) => {
    if (!window.confirm(`Xoá môn học "${course.tenMonHoc}"?`)) {
      return;
    }

    try {
      await deleteCourse(course.id);

      if (editingCourse?.id === course.id) {
        setEditingCourse(null);
        setFormError(null);
      }

      // Neu vua xoa dong cuoi cung cua mot trang sau trang dau, chuyen ve trang
      // truoc de khong bi ket o mot trang rong khong con nut phan trang.
      if (courses.length === 1 && page > 0) {
        setPage(page - 1);
      } else {
        await refetch();
      }
    } catch (error) {
      window.alert(extractErrorMessage(error));
    }
  };

  return (
    <div style={{ padding: 24, fontFamily: 'sans-serif', maxWidth: 800, margin: '0 auto' }}>
      <h1>Quản lý môn học (Admin)</h1>

      <CourseForm
        key={formResetVersion}
        editingCourse={editingCourse}
        onSubmit={handleFormSubmit}
        onCancel={handleCancel}
        submitting={submitting}
        serverError={formError}
      />

      <SearchBox onSearch={handleSearch} />

      <div style={{ marginTop: 16 }}>
        <CourseList
          courses={courses}
          state={state}
          errorMessage={errorMessage}
          onRetry={refetch}
          onEdit={handleEdit}
          onDelete={handleDelete}
        />
      </div>

      <Pagination currentPage={page} totalPages={totalPages} onPageChange={setPage} />
    </div>
  );
}

export default AdminCoursesPage;
