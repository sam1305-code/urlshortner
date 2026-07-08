package com.example.urlshortener.analytics.repository;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.stereotype.Repository;

import com.example.urlshortener.analytics.model.ClickEvent;

@Repository
public class InMemoryClickEventRepository implements ClickEventRepository {

    private final Queue<ClickEvent> clickEvents = new ConcurrentLinkedQueue<>();

    @Override
    public void save(ClickEvent clickEvent) {
        clickEvents.add(clickEvent);
    }

    @Override
    public long countByShortCode(String shortCode) {
        return clickEvents.stream()
                .filter(event -> event.shortCode().equals(shortCode))
                .count();
    }

    @Override
    public List<ClickEvent> findByShortCode(String shortCode) {
        return clickEvents.stream()
                .filter(event -> event.shortCode().equals(shortCode))
                .toList();
    }
}
