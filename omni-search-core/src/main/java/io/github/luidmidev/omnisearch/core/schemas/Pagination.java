package io.github.luidmidev.omnisearch.core.schemas;


import lombok.Data;

@Data
public class Pagination {

    private static final Pagination UNPAGINATED = new Pagination(null, null);

    private final Integer pageNumber;
    private final Integer pageSize;

    public Pagination(Integer pageNumber, Integer pageSize) {
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
    }

    public Pagination(int pageNumber, int pageSize) {
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
    }

    public boolean isPaginated() {
        return pageNumber != null && pageSize != null && pageSize > 0;
    }

    public boolean isUnpaginated() {
        return !isPaginated();
    }

    public int getOffset() {
        if (isUnpaginated()) {
            throw new UnsupportedOperationException("Unpaginated does not have an offset");
        }
        return getPageNumber() * getPageSize();
    }

    public static Pagination unpaginated() {
        return UNPAGINATED;
    }
}
