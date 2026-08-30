// path: crs-frontend/src/pages/CoursesPage.tsx
// purpose: trang danh sach mon hoc cong khai, chuyen tu App.tsx cua Buoi 6
// phoi hop SearchBox + CourseList + Pagination + useCourses, khong co Form Them/Sua/Xoa
import { useCallback, useState } from 'react';
import { useCourses } from '../api/useCourses';
import CourseList from '../components/CourseList';
import Pagination from '../components/Pagination';
import SearchBox from '../components/SearchBox';

function CoursesPage() {
  const [keyword, setKeyword] = useState('');
  const [page, setPage] = useState(0);
  const { courses, totalPages, state, errorMessage, refetch } = useCourses(keyword, page);

  const handleSearch = useCallback((newKeyword: string) => {
    setKeyword(newKeyword);
    setPage(0); // Moi lan tim kiem moi, luon quay ve trang dau.
  }, []);

  return (
    <div style={{ padding: 24, fontFamily: 'sans-serif', maxWidth: 800, margin: '0 auto' }}>
      <h1>Danh sách môn học</h1>
      <SearchBox onSearch={handleSearch} />

      <div style={{ marginTop: 16 }}>
        <CourseList courses={courses} state={state} errorMessage={errorMessage} onRetry={refetch} />
      </div>

      <Pagination currentPage={page} totalPages={totalPages} onPageChange={setPage} />
    </div>
  );
}

export default CoursesPage;
