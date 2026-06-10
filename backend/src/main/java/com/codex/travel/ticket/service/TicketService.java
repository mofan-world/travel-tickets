package com.codex.travel.ticket.service;

import java.math.BigDecimal;
import java.util.ArrayList;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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
    public TicketResponse get(Long tenantId, Long ticketId) {
        return TicketResponse.from(loadTicket(tenantId, ticketId));
    }

    @Transactional(readOnly = true)
    public PageResult<TicketResponse> list(
            Long tenantId,
            TicketStatus status,
            String keyword,
            String city,
            String travelType,
            int page,
            int size) {
        int normalizedPage = Math.max(page, 0);
        int normalizedSize = Math.min(Math.max(size, 1), 200);
        if (hasQueryFilters(keyword, city, travelType)) {
            return loadTicketPage(tenantId, status, keyword, city, travelType, normalizedPage, normalizedSize);
        }
        return redisSnapshotService.readTicketPage(tenantId, status, normalizedPage, normalizedSize)
                .orElseGet(() -> loadTicketPage(tenantId, status, normalizedPage, normalizedSize));
    }

    private PageResult<TicketResponse> loadTicketPage(Long tenantId, TicketStatus status, int page, int size) {
        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<TravelTicket> tickets = status == null
                ? ticketRepository.findByTenantId(tenantId, pageRequest)
                : ticketRepository.findByTenantIdAndStatus(tenantId, status, pageRequest);

        PageResult<TicketResponse> result = new PageResult<>(
                tickets.getContent().stream().map(TicketResponse::from).toList(),
                tickets.getNumber(),
                tickets.getSize(),
                tickets.getTotalElements());
        redisSnapshotService.writeTicketPage(tenantId, status, page, size, result);
        return result;
    }

    private PageResult<TicketResponse> loadTicketPage(
            Long tenantId,
            TicketStatus status,
            String keyword,
            String city,
            String travelType,
            int page,
            int size) {
        Page<TravelTicket> tickets = ticketRepository.findAll(
                ticketSpecification(tenantId, status, keyword, city, travelType, false),
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return new PageResult<>(
                tickets.getContent().stream().map(TicketResponse::from).toList(),
                tickets.getNumber(),
                tickets.getSize(),
                tickets.getTotalElements());
    }

    @Transactional
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
    public void delete(Long tenantId, Long ticketId) {
        TravelTicket ticket = loadTicket(tenantId, ticketId);
        ticketRepository.delete(ticket);
        deleteAfterCommit(tenantId, ticketId);
    }

    @Transactional
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
    public PageResult<RiskEventResponse> listRiskEvents(
            Long tenantId,
            TicketStatus status,
            String keyword,
            String city,
            String travelType,
            int page,
            int size) {
        int normalizedPage = Math.max(page, 0);
        int normalizedSize = Math.min(Math.max(size, 1), 100);
        if (status != null || hasQueryFilters(keyword, city, travelType)) {
            return loadRiskEvents(tenantId, status, keyword, city, travelType, normalizedPage, normalizedSize);
        }
        return redisSnapshotService.readRiskEvents(tenantId, normalizedPage, normalizedSize)
                .orElseGet(() -> loadRiskEvents(tenantId, normalizedPage, normalizedSize));
    }

    private PageResult<RiskEventResponse> loadRiskEvents(Long tenantId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<TravelTicket> tickets = ticketRepository.findByTenantIdAndRiskLevelNot(tenantId, RiskLevel.NONE, pageRequest);

        PageResult<RiskEventResponse> result = new PageResult<>(
                tickets.getContent().stream().map(this::toRiskEventResponse).toList(),
                tickets.getNumber(),
                tickets.getSize(),
                tickets.getTotalElements());
        redisSnapshotService.writeRiskEvents(tenantId, page, size, result);
        return result;
    }

    private PageResult<RiskEventResponse> loadRiskEvents(
            Long tenantId,
            TicketStatus status,
            String keyword,
            String city,
            String travelType,
            int page,
            int size) {
        Page<TravelTicket> tickets = ticketRepository.findAll(
                ticketSpecification(tenantId, status, keyword, city, travelType, true),
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return new PageResult<>(
                tickets.getContent().stream().map(this::toRiskEventResponse).toList(),
                tickets.getNumber(),
                tickets.getSize(),
                tickets.getTotalElements());
    }

    private Specification<TravelTicket> ticketSpecification(
            Long tenantId,
            TicketStatus status,
            String keyword,
            String city,
            String travelType,
            boolean riskOnly) {
        return (root, query, criteriaBuilder) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("tenantId"), tenantId));
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            if (riskOnly) {
                predicates.add(criteriaBuilder.notEqual(root.get("riskLevel"), RiskLevel.NONE));
            }
            if (StringUtils.hasText(city) && !"全部".equals(city.trim())) {
                String normalizedCity = city.trim();
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.equal(root.get("departureCity"), normalizedCity),
                        criteriaBuilder.equal(root.get("arrivalCity"), normalizedCity)));
            }
            if (StringUtils.hasText(travelType)) {
                predicates.add(criteriaBuilder.equal(root.get("travelType"), normalizeTravelType(travelType)));
            }
            if (StringUtils.hasText(keyword)) {
                String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("employeeName")), pattern),
                        criteriaBuilder.like(root.get("employeeId").as(String.class), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("department")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("ticketNo")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("carrierNo")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("departureCity")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("arrivalCity")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("tripPurpose")), pattern)));
            }
            return criteriaBuilder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    private boolean hasQueryFilters(String keyword, String city, String travelType) {
        return StringUtils.hasText(keyword)
                || (StringUtils.hasText(city) && !"全部".equals(city.trim()))
                || StringUtils.hasText(travelType);
    }

    private RiskEventResponse toRiskEventResponse(TravelTicket ticket) {
        return new RiskEventResponse(
                ticket.getId(),
                ticket.getTicketNo(),
                ticket.getEmployeeName(),
                ticket.getDepartment(),
                ticket.getDepartureCity() + " -> " + ticket.getArrivalCity(),
                ticket.getCarrierNo(),
                ticket.getRiskLevel(),
                ticket.getAttachmentStatus(),
                riskMessage(ticket),
                ticket.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse dashboardSummary(Long tenantId) {
        return redisSnapshotService.readDashboardSummary(tenantId)
                .orElseGet(() -> loadDashboardSummary(tenantId));
    }

    private DashboardSummaryResponse loadDashboardSummary(Long tenantId) {
        long total = ticketRepository.countByTenantId(tenantId);
        long riskCount = ticketRepository.countByTenantIdAndRiskLevelNot(tenantId, RiskLevel.NONE);
        long approvedCount = ticketRepository.countByTenantIdAndStatusIn(
                tenantId,
                List.of(TicketStatus.APPROVED, TicketStatus.REIMBURSED));
        BigDecimal pendingAmount = ticketRepository.sumAmountByTenantIdAndStatusIn(
                tenantId,
                List.of(TicketStatus.PENDING_REVIEW, TicketStatus.MISSING_ATTACHMENT, TicketStatus.EXCEPTION));
        double riskRate = total == 0 ? 0 : (double) riskCount / total;
        DashboardSummaryResponse result = new DashboardSummaryResponse(total, pendingAmount, riskRate, approvedCount, 18.6);
        redisSnapshotService.writeDashboardSummary(tenantId, result);
        return result;
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
            redisSnapshotService.bumpTenantVersion(ticket.getTenantId());
            redisSnapshotService.writeTicket(ticket);
            index(ticket);
        });
    }

    private void deleteAfterCommit(Long tenantId, Long ticketId) {
        afterCommit(() -> {
            redisSnapshotService.bumpTenantVersion(tenantId);
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
