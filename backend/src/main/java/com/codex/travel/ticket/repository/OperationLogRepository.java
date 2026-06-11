package com.codex.travel.ticket.repository;

import com.codex.travel.ticket.entity.OperationLog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OperationLogRepository extends JpaRepository<OperationLog, Long> {

    Page<OperationLog> findByTenantId(Long tenantId, Pageable pageable);
}
