package io.provenly.commons.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Standard paginated response wrapper.
 * Used for endpoints that return lists of items with pagination.
 *
 * @param <T> The type of items in the page
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PageResponse<T> {

    /**
     * The list of items in this page.
     */
    private List<T> content;

    /**
     * Current page number (0-indexed).
     */
    private int page;

    /**
     * Number of items per page.
     */
    private int size;

    /**
     * Total number of items across all pages.
     */
    private long totalElements;

    /**
     * Total number of pages.
     */
    private int totalPages;

    /**
     * Whether this is the first page.
     */
    private boolean first;

    /**
     * Whether this is the last page.
     */
    private boolean last;

    /**
     * Whether there are more pages after this one.
     */
    private boolean hasNext;

    /**
     * Whether there are pages before this one.
     */
    private boolean hasPrevious;

    /**
     * Create a page response from content and pagination info.
     */
    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = (int) Math.ceil((double) totalElements / size);
        
        return PageResponse.<T>builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .first(page == 0)
                .last(page >= totalPages - 1)
                .hasNext(page < totalPages - 1)
                .hasPrevious(page > 0)
                .build();
    }

    /**
     * Create an empty page response.
     */
    public static <T> PageResponse<T> empty(int page, int size) {
        return PageResponse.<T>builder()
                .content(List.of())
                .page(page)
                .size(size)
                .totalElements(0)
                .totalPages(0)
                .first(true)
                .last(true)
                .hasNext(false)
                .hasPrevious(false)
                .build();
    }

    /**
     * Check if this page is empty.
     */
    public boolean isEmpty() {
        return content == null || content.isEmpty();
    }

    /**
     * Get the number of items in this page.
     */
    public int getNumberOfElements() {
        return content != null ? content.size() : 0;
    }
}

