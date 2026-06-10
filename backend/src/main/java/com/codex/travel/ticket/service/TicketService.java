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
import com.codex.travel.ticket.dto.UpdateTicketRequest;
import com.codex.travel.ticket.entity.TravelTicket;
import com.codex.travel.ticket.enums.RiskLevel;
import com.codex.travel.ticket.enums.TicketStatus;
import com.codex.travel.ticket.repository.TravelTicketRepository;
import com.codex.travel.ticket.repository.TravelTicketSearchRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

@Service
public class TicketService {

    private static final Logger log = LoggerFactory.getLogger(TicketService.class);

    private final TravelTicketRepository ticketRepository;
    private final ObjectProvider<TravelTicketSearchRepository> searchRepository;
    private final RedisSnapshotService redisSnapshotService;

    public TicketService(
            TravelTicketRepository ticketRepository,
            ObjectProvider<TravelTicketSearchRepository> searchRepository,
            RedisSnapshotService redisSnapshotService) {
        this.ticketRepository = ticketRepository;
        this.searchRepository = searchRepository;
        this.redisSnapshotService = redisSnapshotService;
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
        applyCreateRequest(ticket, request);
        ticket.setRiskLevel(evaluateRisk(ticket));

        TravelTicket saved = ticketRepository.save(ticket);
        syncAfterCommit(saved);
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
    public TicketResponse update(Long tenantId, Long ticketId, UpdateTicketRequest request) {
        TravelTicket ticket = loadTicket(tenantId, ticketId);
        if (ticketRepository.existsByTenantIdAndTicketNoAndIdNot(tenantId, request.ticketNo(), ticketId)) {
            throw new IllegalArgumentException("ticketNo already exists in current tenant");
        }

        applyUpdateRequest(ticket, request);
        ticket.setRiskLevel(evaluateRisk(ticket));
        TravelTicket saved = ticketRepository.save(ticket);
        syncAfterCommit(saved);
        return TicketResponse.from(saved);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "tickets", allEntries = true),
            @CacheEvict(cacheNames = "ticketDetail", key = "#p0 + ':' + #p1"),
            @CacheEvict(cacheNames = "dashboardSummary", key = "#p0"),
            @CacheEvict(cacheNames = "riskEvents", key = "#p0")
    })
    public void delete(Long tenantId, Long ticketId) {
        TravelTicket ticket = loadTicket(tenantId, ticketId);
        ticketRepository.delete(ticket);
        deleteAfterCommit(tenantId, ticketId);
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
            case "reimburse" -> TicketStatus.REIMBURSED;
            case "exception" -> TicketStatus.EXCEPTION;
            default -> throw new IllegalArgumentException("unsupported approval action: " + request.action());
        };

        ticket.setStatus(nextStatus);
        if (nextStatus == TicketStatus.APPROVED || nextStatus == TicketStatus.REIMBURSED) {
            ticket.setRiskLevel(RiskLevel.NONE);
        } else {
            ticket.setRiskLevel(evaluateRisk(ticket));
        }

        TravelTicket saved = ticketRepository.save(ticket);
        syncAfterCommit(saved);
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
                        ticket.getEmployeeName(),
                        ticket.getDepartment(),
                        ticket.getDepartureCity() + " -> " + ticket.getArrivalCity(),
                        ticket.getCarrierNo(),
                        ticket.getRiskLevel(),
                        ticket.getAttachmentStatus(),
                        riskMessage(ticket),
                        ticket.getCreatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "dashboardSummary", key = "#p0")
    public DashboardSummaryResponse dashboardSummary(Long tenantId) {
        long total = ticketRepository.countByTenantId(tenantId);
        long riskCount = ticketRepository.countByTenantIdAndRiskLevelNot(tenantId, RiskLevel.NONE);
        BigDecimal pendingAmount = ticketRepository.sumAmountByTenantIdAndStatusIn(
                tenantId,
                List.of(TicketStatus.PENDING_REVIEW, TicketStatus.MISSING_ATTACHMENT, TicketStatus.EXCEPTION));
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

    private void applyCreateRequest(TravelTicket ticket, CreateTicketRequest request) {
        ticket.setEmployeeId(request.employeeId());
        ticket.setEmployeeName(request.employeeName().trim());
        ticket.setDepartment(request.department().trim());
        ticket.setTicketNo(request.ticketNo().trim());
        ticket.setExternalSource(request.externalSource());
        ticket.setExternalTicketId(request.externalTicketId());
        ticket.setTravelType(normalizeTravelType(request.travelType()));
        ticket.setDepartureCity(request.departureCity().trim());
        ticket.setArrivalCity(request.arrivalCity().trim());
        ticket.setCarrierNo(request.carrierNo().trim());
        ticket.setSeatClass(request.seatClass());
        ticket.setTripPurpose(request.tripPurpose().trim());
        ticket.setAttachmentStatus(normalizeAttachmentStatus(request.attachmentStatus()));
        ticket.setDepartAt(request.departAt());
        ticket.setArriveAt(request.arriveAt());
        ticket.setAmount(request.amount());
        ticket.setCurrency(StringUtils.hasText(request.currency()) ? request.currency() : "CNY");
        ticket.setStatus(request.status() == null ? TicketStatus.PENDING_REVIEW : request.status());
    }

    private void applyUpdateRequest(TravelTicket ticket, UpdateTicketRequest request) {
        ticket.setEmployeeId(request.employeeId());
        ticket.setEmployeeName(request.employeeName().trim());
        ticket.setDepartment(request.department().trim());
        ticket.setTicketNo(request.ticketNo().trim());
        ticket.setExternalSource(request.externalSource());
        ticket.setExternalTicketId(request.externalTicketId());
        ticket.setTravelType(normalizeTravelType(request.travelType()));
        ticket.setDepartureCity(request.departureCity().trim());
        ticket.setArrivalCity(request.arrivalCity().trim());
        ticket.setCarrierNo(request.carrierNo().trim());
        ticket.setSeatClass(request.seatClass());
        ticket.setTripPurpose(request.tripPurpose().trim());
        ticket.setAttachmentStatus(normalizeAttachmentStatus(request.attachmentStatus()));
        ticket.setDepartAt(request.departAt());
        ticket.setArriveAt(request.arriveAt());
        ticket.setAmount(request.amount());
        ticket.setCurrency(StringUtils.hasText(request.currency()) ? request.currency() : "CNY");
        ticket.setStatus(request.status() == null ? ticket.getStatus() : request.status());
    }

    private RiskLevel evaluateRisk(TravelTicket ticket) {
        if ("MISSING".equals(ticket.getAttachmentStatus())) {
            return RiskLevel.MEDIUM;
        }
        if (ticket.getAmount().compareTo(new BigDecimal("1000")) >= 0) {
            return RiskLevel.HIGH;
        }
        if (ticket.getDepartAt() == null) {
            return RiskLevel.MEDIUM;
        }
        return RiskLevel.NONE;
    }

    private String normalizeTravelType(String travelType) {
        return StringUtils.hasText(travelType) ? travelType.toUpperCase(Locale.ROOT) : "TRAIN";
    }

    private String normalizeAttachmentStatus(String attachmentStatus) {
        if (!StringUtils.hasText(attachmentStatus)) {
            return "UPLOADED";
        }
        String normalized = attachmentStatus.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "MISSING", "UPLOADED" -> normalized;
            default -> "UPLOADED";
        };
    }

    private String riskMessage(TravelTicket ticket) {
        return switch (ticket.getRiskLevel()) {
            case HIGH, CRITICAL -> "high amount or strict policy review required";
            case MEDIUM -> "missing travel time or attachment information";
            case LOW -> "minor policy warning";
            case NONE -> "no risk";
        };
    }

    private void syncAfterCommit(TravelTicket ticket) {
        afterCommit(() -> {
            redisSnapshotService.writeTicket(ticket);
            index(ticket);
        });
    }

    private void deleteAfterCommit(Long tenantId, Long ticketId) {
        afterCommit(() -> {
            redisSnapshotService.deleteTicket(tenantId, ticketId);
            deleteIndex(tenantId, ticketId);
        });
    }

    private void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
            return;
        }
        action.run();
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

    private void deleteIndex(Long tenantId, Long ticketId) {
        TravelTicketSearchRepository repository = searchRepository.getIfAvailable();
        if (repository == null) {
            return;
        }
        try {
            repository.deleteById(tenantId + ":" + ticketId);
        } catch (Exception ex) {
            log.warn("failed to delete ticket {} from Elasticsearch for tenant {}", ticketId, tenantId, ex);
        }
    }
}
