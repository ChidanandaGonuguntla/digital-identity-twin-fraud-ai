import { ChevronLeft, ChevronRight, ChevronsLeft, ChevronsRight } from 'lucide-react';

function buildPageNumbers(current: number, totalPages: number): (number | 'ellipsis')[] {
  if (totalPages <= 1) return [0];
  if (totalPages <= 7) {
    return Array.from({ length: totalPages }, (_, index) => index);
  }

  const pages = new Set<number>([0, totalPages - 1]);
  for (let index = current - 2; index <= current + 2; index += 1) {
    if (index >= 0 && index < totalPages) pages.add(index);
  }

  const sorted = [...pages].sort((a, b) => a - b);
  const result: (number | 'ellipsis')[] = [];
  for (let index = 0; index < sorted.length; index += 1) {
    if (index > 0 && sorted[index] - sorted[index - 1] > 1) result.push('ellipsis');
    result.push(sorted[index]);
  }
  return result;
}

export function TablePagination({
  page,
  pageSize,
  totalElements,
  totalPages,
  onPageChange,
  onPageSizeChange,
  pageSizeOptions = [25, 50, 100],
  disabled = false
}: {
  page: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  onPageChange: (page: number) => void;
  onPageSizeChange?: (pageSize: number) => void;
  pageSizeOptions?: number[];
  disabled?: boolean;
}) {
  const hasPages = totalPages > 0;
  const lastPage = hasPages ? totalPages - 1 : 0;
  const start = totalElements === 0 ? 0 : page * pageSize + 1;
  const end = totalElements === 0 ? 0 : Math.min((page + 1) * pageSize, totalElements);
  const pageNumbers = hasPages ? buildPageNumbers(page, totalPages) : [];

  return (
    <div className="table-pagination">
      <div className="table-pagination-meta">
        {onPageSizeChange && (
          <label className="page-size-control">
            <span>Rows</span>
            <select
              className="field-input compact"
              value={pageSize}
              disabled={disabled}
              onChange={(event) => onPageSizeChange(Number(event.target.value))}
            >
              {pageSizeOptions.map((option) => (
                <option key={option} value={option}>{option}</option>
              ))}
            </select>
          </label>
        )}
        <span className="audit-page-label">
          Showing {start.toLocaleString()}–{end.toLocaleString()} of {totalElements.toLocaleString()}
        </span>
      </div>

      <div className="table-pagination-controls">
        <button
          type="button"
          className="ghost-button"
          disabled={disabled || !hasPages || page === 0}
          onClick={() => onPageChange(0)}
          aria-label="First page"
        >
          <ChevronsLeft size={16} />
        </button>
        <button
          type="button"
          className="ghost-button"
          disabled={disabled || !hasPages || page === 0}
          onClick={() => onPageChange(page - 1)}
          aria-label="Previous page"
        >
          <ChevronLeft size={16} />
        </button>

        <div className="page-number-group">
          {pageNumbers.map((item, index) =>
            item === 'ellipsis' ? (
              <span key={`ellipsis-${index}`} className="page-ellipsis">…</span>
            ) : (
              <button
                key={item}
                type="button"
                className={`page-number${item === page ? ' active' : ''}`}
                disabled={disabled}
                onClick={() => onPageChange(item)}
              >
                {item + 1}
              </button>
            )
          )}
        </div>

        <button
          type="button"
          className="ghost-button"
          disabled={disabled || !hasPages || page >= lastPage}
          onClick={() => onPageChange(page + 1)}
          aria-label="Next page"
        >
          <ChevronRight size={16} />
        </button>
        <button
          type="button"
          className="ghost-button"
          disabled={disabled || !hasPages || page >= lastPage}
          onClick={() => onPageChange(lastPage)}
          aria-label="Last page"
        >
          <ChevronsRight size={16} />
        </button>
      </div>
    </div>
  );
}
