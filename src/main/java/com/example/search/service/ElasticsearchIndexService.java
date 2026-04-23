package com.example.search.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.DeleteResponse;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.example.search.domain.DocumentEntity;
import com.example.search.dto.SearchResultItem;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ElasticsearchIndexService {

    private final ElasticsearchClient client;
    private final String indexName;

    public ElasticsearchIndexService(ElasticsearchClient client,
                                     @Value("${search.elasticsearch.index}") String indexName) {
        this.client = client;
        this.indexName = indexName;
    }

    @PostConstruct
    public void init() throws IOException {
        boolean exists = client.indices().exists(e -> e.index(indexName)).value();
        if (!exists) {
            client.indices().create(c -> c
                    .index(indexName)
                    .mappings(m -> m
                            .properties("id", p -> p.keyword(k -> k))
                            .properties("tenantId", p -> p.keyword(k -> k))
                            .properties("title", p -> p.text(t -> t))
                            .properties("content", p -> p.text(t -> t))
                    ));
        }
    }

    public void index(DocumentEntity entity) throws IOException {
        Map<String, Object> doc = Map.of(
                "id", entity.getId(),
                "tenantId", entity.getTenantId(),
                "title", entity.getTitle(),
                "content", entity.getContent()
        );

        IndexResponse ignored = client.index(i -> i
                .index(indexName)
                .id(entity.getId())
                .document(doc)
                .refresh(Refresh.WaitFor));
    }

    public void delete(String id) throws IOException {
        try {
            DeleteResponse ignored = client.delete(d -> d
                    .index(indexName)
                    .id(id)
                    .refresh(Refresh.WaitFor));
        } catch (ElasticsearchException ex) {
            if (ex.status() != 404) {
                throw ex;
            }
        }
    }

    public List<SearchResultItem> search(String tenantId, String query, int size) throws IOException {
        Query tenantFilter = Query.of(q -> q.term(t -> t.field("tenantId").value(FieldValue.of(tenantId))));

        SearchResponse<Map> response = client.search(s -> s
                        .index(indexName)
                        .size(size)
                        .query(q -> q.bool(b -> b
                                .must(m -> m.multiMatch(mm -> mm
                                        .query(query)
                                        .fields("title^2", "content")
                                        .fuzziness("AUTO")))
                                .filter(tenantFilter)))
                        .highlight(h -> h
                                .fields("title", hf -> hf)
                                .fields("content", hf -> hf)),
                Map.class);

        List<SearchResultItem> results = new ArrayList<>();
        for (Hit<Map> hit : response.hits().hits()) {
            Map source = hit.source();

            String title = source != null && source.get("title") != null
                    ? String.valueOf(source.get("title"))
                    : "";

            String content = source != null && source.get("content") != null
                    ? String.valueOf(source.get("content"))
                    : "";

            String snippet = buildSnippet(hit, content);

            results.add(new SearchResultItem(
                    hit.id(),
                    tenantId,
                    title,
                    snippet,
                    hit.score() != null ? hit.score() : 0.0d
            ));
        }
        return results;
    }

    private String buildSnippet(Hit<Map> hit, String content) {
        if (hit.highlight() != null) {
            List<String> contentHighlights = hit.highlight().get("content");
            if (contentHighlights != null && !contentHighlights.isEmpty()) {
                return contentHighlights.get(0);
            }

            List<String> titleHighlights = hit.highlight().get("title");
            if (titleHighlights != null && !titleHighlights.isEmpty()) {
                return titleHighlights.get(0);
            }
        }

        if (content == null || content.isBlank()) {
            return "";
        }

        return content.length() <= 180 ? content : content.substring(0, 180) + "...";
    }
}
