// path: crs-frontend/src/components/CourseForm.tsx
// purpose: form dung chung cho Them va Sua mon hoc, validate phia client truoc khi goi API
import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { emptyCourseForm } from '../types/course';
import type { Course, CourseFormValues } from '../types/course';

interface CourseFormProps {
  editingCourse: Course | null;
  onSubmit: (values: CourseFormValues) => Promise<void>;
  onCancel: () => void;
  submitting: boolean;
  serverError: string | null;
}

export default function CourseForm({
  editingCourse,
  onSubmit,
  onCancel,
  submitting,
  serverError,
}: CourseFormProps) {
  const [values, setValues] = useState<CourseFormValues>(emptyCourseForm);
  const [clientErrors, setClientErrors] = useState<Partial<CourseFormValues>>({});

  // Moi lan editingCourse thay doi, nap dung du lieu cua mon hoc dang sua vao form.
  useEffect(() => {
    if (editingCourse) {
      setValues({
        tenMonHoc: editingCourse.tenMonHoc,
        soTinChi: String(editingCourse.soTinChi),
        soChoToiDa: String(editingCourse.soChoToiDa),
      });
    } else {
      setValues(emptyCourseForm);
    }

    setClientErrors({});
  }, [editingCourse]);

  const validate = (): boolean => {
    const errors: Partial<CourseFormValues> = {};

    if (!values.tenMonHoc.trim()) {
      errors.tenMonHoc = 'Tên môn học không được để trống';
    }

    const soTinChi = Number(values.soTinChi);
    if (!values.soTinChi || Number.isNaN(soTinChi) || soTinChi <= 0) {
      errors.soTinChi = 'Số tín chỉ phải là số lớn hơn 0';
    }

    const soChoToiDa = Number(values.soChoToiDa);
    if (!values.soChoToiDa || Number.isNaN(soChoToiDa) || soChoToiDa <= 0) {
      errors.soChoToiDa = 'Số chỗ tối đa phải là số lớn hơn 0';
    }

    setClientErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (!validate()) {
      return;
    }

    await onSubmit(values);
  };

  return (
    <form
      onSubmit={handleSubmit}
      style={{ border: '1px solid #ddd', padding: 16, borderRadius: 8, marginBottom: 16 }}
    >
      <h3>{editingCourse ? 'Sửa môn học' : 'Thêm môn học mới'}</h3>

      <div style={{ marginBottom: 8 }}>
        <label htmlFor="tenMonHoc">Tên môn học</label>
        <br />
        <input
          id="tenMonHoc"
          type="text"
          value={values.tenMonHoc}
          onChange={(event) => setValues({ ...values, tenMonHoc: event.target.value })}
        />
        {clientErrors.tenMonHoc && (
          <p style={{ color: '#b91c1c', margin: 0 }}>{clientErrors.tenMonHoc}</p>
        )}
      </div>

      <div style={{ marginBottom: 8 }}>
        <label htmlFor="soTinChi">Số tín chỉ</label>
        <br />
        <input
          id="soTinChi"
          type="number"
          value={values.soTinChi}
          onChange={(event) => setValues({ ...values, soTinChi: event.target.value })}
        />
        {clientErrors.soTinChi && (
          <p style={{ color: '#b91c1c', margin: 0 }}>{clientErrors.soTinChi}</p>
        )}
      </div>

      <div style={{ marginBottom: 8 }}>
        <label htmlFor="soChoToiDa">Số chỗ tối đa</label>
        <br />
        <input
          id="soChoToiDa"
          type="number"
          value={values.soChoToiDa}
          onChange={(event) => setValues({ ...values, soChoToiDa: event.target.value })}
        />
        {clientErrors.soChoToiDa && (
          <p style={{ color: '#b91c1c', margin: 0 }}>{clientErrors.soChoToiDa}</p>
        )}
      </div>

      {serverError && <p style={{ color: '#b91c1c' }}>{serverError}</p>}

      <button type="submit" disabled={submitting}>
        {submitting ? 'Đang lưu...' : editingCourse ? 'Cập nhật' : 'Thêm mới'}
      </button>

      {editingCourse && (
        <button type="button" onClick={onCancel} style={{ marginLeft: 8 }}>
          Huỷ
        </button>
      )}
    </form>
  );
}
