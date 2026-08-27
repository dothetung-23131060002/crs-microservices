// path: crs-frontend/src/components/SearchBox.tsx
// purpose: o nhap tim kiem mon hoc, co debounce de tranh goi API lien tuc khi go phim
import { useEffect, useState } from 'react';

interface SearchBoxProps {
  onSearch: (keyword: string) => void;
  placeholder?: string;
}

export default function SearchBox({ onSearch, placeholder }: SearchBoxProps) {
  const [inputValue, setInputValue] = useState('');

  useEffect(() => {
    const timer = window.setTimeout(() => {
      onSearch(inputValue.trim());
    }, 400);

    return () => window.clearTimeout(timer);
  }, [inputValue, onSearch]);

  return (
    <input
      type="text"
      value={inputValue}
      onChange={(event) => setInputValue(event.target.value)}
      placeholder={placeholder ?? 'Tìm kiếm theo tên môn học...'}
      style={{
        width: '100%',
        maxWidth: 400,
        padding: '8px 12px',
        fontSize: 14,
        border: '1px solid #ccc',
        borderRadius: 6,
      }}
    />
  );
}
