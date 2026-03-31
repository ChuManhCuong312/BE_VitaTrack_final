package com.vitatrack.utils;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class PaginationUtil {

    public Pageable of(int page, int size) {
        return PageRequest.of(Math.max(0, page), Math.min(100, size));
    }

    public Pageable of(int page, int size, String sortBy, String direction) {
        Sort sort = "desc".equalsIgnoreCase(direction)
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        return PageRequest.of(Math.max(0, page), Math.min(100, size), sort);
    }
}
