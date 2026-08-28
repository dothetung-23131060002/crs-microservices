// path: crs-frontend/src/types/course.ts
// purpose: interface khop voi CourseDTO ben course-service (Buoi 2-3)

export interface Course {
  id: number;
  tenMonHoc: string;
  soTinChi: number;
  soChoToiDa: number;
  soChoConLai: number;
}

// Khop voi cau truc Page<CourseDTO> ma Spring Data JPA tra ve (Buoi 3, muc A)
export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number; // trang hien tai (bat dau tu 0)
  size: number;
}

// Kieu du lieu rieng cho form. Course co id va cac gia tri so, con form can giu
// chuoi rong de controlled input co the bieu dien trang thai chua nhap.
export interface CourseFormValues {
  tenMonHoc: string;
  soTinChi: string;
  soChoToiDa: string;
}

export const emptyCourseForm: CourseFormValues = {
  tenMonHoc: '',
  soTinChi: '',
  soChoToiDa: '',
};
