package com.flowmind.content.adapter;

import com.flowmind.content.dto.ContentGenerateResponse;
import com.flowmind.content.port.LocalContentRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class InMemoryLocalContentRepository implements LocalContentRepository {
    private final List<ContentGenerateResponse> records = new CopyOnWriteArrayList<>();

    @Override
    public void save(ContentGenerateResponse response) {
        records.add(response);
    }
}
