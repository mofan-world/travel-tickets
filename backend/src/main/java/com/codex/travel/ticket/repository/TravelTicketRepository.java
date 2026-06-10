package com.codex.travel.ticket.repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.codex.travel.ticket.entity.TravelTicket;
import com.codex.travel.ticket.enums.RiskLevel;
import com.codex.travel.ticket.enums.TicketStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TravelTicketRepository extends JpaRepository<TravelTicket, Long> {

    Page<TravelTicket> findByTenantId(Long tenantId, Pageable pageable);

    Page<TravelTicket> findByTenantIdAndStatus(Long tenantId, TicketStatus status, Pageable pageable);

    Optional<TravelTicket> findByTenantIdAndId(Long tenantId, Long id);

    boolean existsByTenantIdAndTicketNo(Long tenantId, String ticketNo);

    boolean existsByTenantIdAndTicketNoAndIdNot(Long tenantId, String ticketNo, Long id);

    long countByTenantId(Long tenantId);

    long countByTenantIdAndRiskLevelNot(Long tenantId, RiskLevel riskLevel);

    long countByTenantIdAndStatusIn(Long tenantId, Collection<TicketStatus> statuses);

    Page<TravelTicket> findByTenantIdAndRiskLevelNot(Long tenantId, RiskLevel riskLevel, Pageable pageable);

    List<TravelTicket> findTop20ByTenantIdAndRiskLevelNotOrderByCreatedAtDesc(Long tenantId, RiskLevel riskLevel);

    @Query("select coalesce(sum(t.amount), 0) from TravelTicket t where t.tenantId = :tenantId and t.status = :status")
    BigDecimal sumAmountByTenantIdAndStatus(@Param("tenantId") Long tenantId, @Param("status") TicketStatus status);

    @Query("select coalesce(sum(t.amount), 0) from TravelTicket t where t.tenantId = :tenantId and t.status in :statuses")
    BigDecimal sumAmountByTenantIdAndStatusIn(@Param("tenantId") Long tenantId, @Param("statuses") Collection<TicketStatus> statuses);
}
