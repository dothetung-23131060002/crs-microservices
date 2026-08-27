// path: crs-frontend/src/components/Pagination.tsx
// purpose: dieu huong trang, dung chung cho moi man hinh danh sach co phan trang
interface PaginationProps {
  currentPage: number; // bat dau tu 0, dung dinh dang Spring Data Pageable
  totalPages: number;
  onPageChange: (page: number) => void;
}

export default function Pagination({ currentPage, totalPages, onPageChange }: PaginationProps) {
  if (totalPages <= 1) {
    return null;
  }

  const pages = Array.from({ length: totalPages }, (_, index) => index);

  return (
    <div style={{ display: 'flex', gap: 6, marginTop: 16 }}>
      <button disabled={currentPage === 0} onClick={() => onPageChange(currentPage - 1)}>
        « Trang truoc
      </button>

      {pages.map((page) => (
        <button
          key={page}
          onClick={() => onPageChange(page)}
          style={{
            fontWeight: page === currentPage ? 'bold' : 'normal',
            textDecoration: page === currentPage ? 'underline' : 'none',
          }}
        >
          {page + 1}
        </button>
      ))}

      <button disabled={currentPage >= totalPages - 1} onClick={() => onPageChange(currentPage + 1)}>
        Trang sau »
      </button>
    </div>
  );
}
