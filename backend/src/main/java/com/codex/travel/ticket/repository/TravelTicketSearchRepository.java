package com.codex.travel.ticket.repository;

import com.codex.travel.ticket.document.TravelTicketDocument;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface TravelTicketSearchRepository extends ElasticsearchRepository<TravelTicketDocument, String> {

    Page<TravelTicketDocument> findByTenantId(Long tenantId, Pageable pageable);

    Page<TravelTicketDocument> findByTenantIdAndRouteContaining(Long tenantId, String keyword, Pageable pageable);
}
