package com.codex.travel.ticket.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

import com.codex.travel.ticket.common.PageResult;
import com.codex.travel.ticket.document.TravelTicketDocument;
import com.codex.travel.ticket.dto.ApprovalActionRequest;
import com.codex.travel.ticket.dto.CreateTicketRequest;
import com.codex.travel.ticket.dto.DashboardSummaryResponse;
import com.codex.travel.ticket.dto.RiskEventResponse;
import com.codex.travel.ticket.dto.TicketResponse;
import com.codex.travel.ticket.entity.TravelTicket;
import com.codex.travel.ticket.enums.RiskLevel;
import com.codex.travel.ticket.enums.TicketStatus;
import com.codex.travel.ticket.repository.TravelTicketRepository;
import com.codex.travel.ticket.repository.TravelTicketSearchRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class TicketService {

    private static final Logger log = LoggerFactory.getLogger(TicketService.class);

    private final TravelTicketRepository ticketRepository;
    private final ObjectProvider<TravelTicketSearchRepository> searchRepository;

    public TicketService(TravelTicketRepository ticketRepository, ObjectProvider<TravelTicketSearchRepository> searchRepository) {
        this.ticketRepository = ticketRepository;
        this.searchRepository = searchRepository;
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "tickets", allEntries = true),
            @CacheEvict(cacheNames = "dashboardSummary", key = "#p0"),
            @CacheEvict(cacheNames = "riskEvents", key = "#p0")
    })
    public TicketResponse create(Long tenantId, CreateTicketRequest request) {
        if (ticketRepository.existsByTenantIdAndTicketNo(tenantId, request.ticketNo())) {
            throw new IllegalArgumentException("ticketNo already exists in current tenant");
        }

        TravelTicket ticket = new TravelTicket();
        ticket.setTenantId(tenantId);
        ticket.setEmployeeId(request.employeeId());
        ticket.setTicketNo(request.ticketNo());
        ticket.setExternalSource(request.externalSource());
        ticket.setExternalTicketId(request.externalTicketId());
        ticket.setTravelType(StringUtils.hasText(request.travelType()) ? request.travelType().toUpperCase(Locale.ROOT) : "TRAIN");
        ticket.setDepartureCity(request.departureCity());
        ticket.setArrivalCity(request.arrivalCity());
        ticket.setCarrierNo(request.carrierNo());
        ticket.setSeatClass(request.seatClass());
        ticket.setDepartAt(request.departAt());
        ticket.setArriveAt(request.arriveAt());
        ticket.setAmount(request.amount());
        ticket.setCurrency(StringUtils.hasText(request.currency()) ? request.currency() : "CNY");
        ticket.setRiskLevel(evaluateRisk(request));

        TravelTicket saved = ticketRepository.save(ticket);
        index(saved);
        return TicketResponse.from(saved);
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "ticketDetail", key = "#p0 + ':' + #p1")
    public TicketResponse get(Long tenantId, Long ticketId) {
        return TicketResponse.from(loadTicket(tenantId, ticketId));
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "tickets", key = "#p0 + ':' + (#p1 == null ? 'ALL' : #p1.name()) + ':' + #p2 + ':' + #p3")
    public PageResult<TicketResponse> list(Long tenantId, TicketStatus status, int page, int size) {
        PageRequest pageRequest = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 200),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<TravelTicket> tickets = status == null
                ? ticketRepository.findByTenantId(tenantId, pageRequest)
                : ticketRepository.findByTenantIdAndStatus(tenantId, status, pageRequest);

        return new PageResult<>(
                tickets.getContent().stream().map(TicketResponse::from).toList(),
                tickets.getNumber(),
                tickets.getSize(),
                tickets.getTotalElements());
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "tickets", allEntries = true),
            @CacheEvict(cacheNames = "ticketDetail", key = "#p0 + ':' + #p1"),
            @CacheEvict(cacheNames = "dashboardSummary", key = "#p0"),
            @CacheEvict(cacheNames = "riskEvents", key = "#p0")
    })
    public TicketResponse applyApprovalAction(Long tenantId, Long ticketId, ApprovalActionRequest request) {
        TravelTicket ticket = loadTicket(tenantId, ticketId);
        TicketStatus nextStatus = switch (request.action().toLowerCase(Locale.ROOT)) {
            case "approve" -> TicketStatus.APPROVED;
            case "reject" -> TicketStatus.REJECTED;
            case "return" -> TicketStatus.MISSING_ATTACHMENT;
            default -> throw new IllegalArgumentException("unsupported approval action: " + request.action());
        };

        ticket.setStatus(nextStatus);
        if (nextStatus == TicketStatus.APPROVED) {
            ticket.setRiskLevel(RiskLevel.NONE);
        }

        TravelTicket saved = ticketRepository.save(ticket);
        index(saved);
        return TicketResponse.from(saved);
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "riskEvents", key = "#p0")
    public java.util.List<RiskEventResponse> listRiskEvents(Long tenantId) {
        return ticketRepository.findTop20ByTenantIdAndRiskLevelNotOrderByCreatedAtDesc(tenantId, RiskLevel.NONE)
                .stream()
                .map(ticket -> new RiskEventResponse(
                        ticket.getId(),
                        ticket.getTicketNo(),
                        ticket.getDepartureCity() + " -> " + ticket.getArrivalCity(),
                        ticket.getCarrierNo(),
                        ticket.getRiskLevel(),
                        riskMessage(ticket),
                        ticket.getCreatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "dashboardSummary", key = "#p0")
    public DashboardSummaryResponse dashboardSummary(Long tenantId) {
        long total = ticketRepository.countByTenantId(tenantId);
        long riskCount = ticketRepository.countByTenantIdAndRiskLevelNot(tenantId, RiskLevel.NONE);
        BigDecimal pendingAmount = ticketRepository.sumAmountByTenantIdAndStatus(tenantId, TicketStatus.PENDING_REVIEW);
        double riskRate = total == 0 ? 0 : (double) riskCount / total;
        return new DashboardSummaryResponse(total, pendingAmount, riskRate, 18.6);
    }

    @Transactional(readOnly = true)
    public long reindexTenant(Long tenantId) {
        TravelTicketSearchRepository repository = searchRepository.getIfAvailable();
        if (repository == null) {
            throw new IllegalStateException("Elasticsearch is disabled. Set ES_ENABLED=true and ES_URIS to rebuild the ticket index.");
        }

        int page = 0;
        long indexed = 0;
        Page<TravelTicket> tickets;
        do {
            tickets = ticketRepository.findByTenantId(tenantId, PageRequest.of(page, 500, Sort.by(Sort.Direction.ASC, "id")));
            List<TravelTicketDocument> documents = tickets.getContent()
                    .stream()
                    .map(TravelTicketDocument::from)
                    .toList();
            repository.saveAll(documents);
            indexed += documents.size();
            page++;
        } while (tickets.hasNext());
        return indexed;
    }

    private TravelTicket loadTicket(Long tenantId, Long ticketId) {
        return ticketRepository.findByTenantIdAndId(tenantId, ticketId)
                .orElseThrow(() -> new IllegalArgumentException("ticket not found"));
    }

    private RiskLevel evaluateRisk(CreateTicketRequest request) {
        if (request.amount().compareTo(new BigDecimal("1000")) >= 0) {
            return RiskLevel.HIGH;
        }
        if (request.departAt() == null) {
            return RiskLevel.MEDIUM;
        }
        return RiskLevel.NONE;
    }

    private String riskMessage(TravelTicket ticket) {
        return switch (ticket.getRiskLevel()) {
            case HIGH, CRITICAL -> "high amount or strict policy review required";
            case MEDIUM -> "missing travel time or attachment information";
            case LOW -> "minor policy warning";
            case NONE -> "no risk";
        };
    }

    private void index(TravelTicket ticket) {
        TravelTicketSearchRepository repository = searchRepository.getIfAvailable();
        if (repository == null) {
            log.debug("skip indexing ticket {} because Elasticsearch repository is disabled", ticket.getId());
            return;
        }
        try {
            repository.save(TravelTicketDocument.from(ticket));
        } catch (Exception ex) {
            log.warn("failed to index ticket {} for tenant {}", ticket.getId(), ticket.getTenantId(), ex);
        }
    }
}
