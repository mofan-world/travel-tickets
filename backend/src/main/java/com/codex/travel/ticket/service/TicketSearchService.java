package com.codex.travel.ticket.service;

import com.codex.travel.ticket.common.PageResult;
import com.codex.travel.ticket.document.TravelTicketDocument;
import com.codex.travel.ticket.dto.TicketSearchResponse;
import com.codex.travel.ticket.repository.TravelTicketSearchRepository;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TicketSearchService {

    private final ObjectProvider<TravelTicketSearchRepository> searchRepository;

    public TicketSearchService(ObjectProvider<TravelTicketSearchRepository> searchRepository) {
        this.searchRepository = searchRepository;
    }

    public PageResult<TicketSearchResponse> search(Long tenantId, String keyword, int page, int size) {
        TravelTicketSearchRepository repository = requiredRepository();
        PageRequest pageRequest = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 200),
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<TravelTicketDocument> documents = StringUtils.hasText(keyword)
                ? repository.findByTenantIdAndRouteContaining(tenantId, keyword.trim(), pageRequest)
                : repository.findByTenantId(tenantId, pageRequest);

        return new PageResult<>(
                documents.getContent().stream().map(TicketSearchResponse::from).toList(),
                documents.getNumber(),
                documents.getSize(),
                documents.getTotalElements());
    }

    private TravelTicketSearchRepository requiredRepository() {
        TravelTicketSearchRepository repository = searchRepository.getIfAvailable();
        if (repository == null) {
            throw new IllegalStateException("Elasticsearch is disabled. Set ES_ENABLED=true and ES_URIS to enable ticket search.");
        }
        return repository;
    }
}
