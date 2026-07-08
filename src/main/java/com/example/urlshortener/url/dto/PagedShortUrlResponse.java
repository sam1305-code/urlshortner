package com.example.urlshortener.url.dto;

import java.util.List;

public record PagedShortUrlResponse(
        List<ShortUrlResponse> items,
        int page,
        int size,
        long totalItems,
        int totalPages) {
}
