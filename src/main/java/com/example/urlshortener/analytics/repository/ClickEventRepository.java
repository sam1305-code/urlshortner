package com.example.urlshortener.analytics.repository;

import java.util.List;

import com.example.urlshortener.analytics.model.ClickEvent;

public interface ClickEventRepository {

    void save(ClickEvent clickEvent);

    long countByShortCode(String shortCode);

    List<ClickEvent> findByShortCode(String shortCode);
}
