package com.codex.travel.ticket.repository;

import com.codex.travel.ticket.entity.ExceptionLog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExceptionLogRepository extends JpaRepository<ExceptionLog, Long> {

    Page<ExceptionLog> findByTenantId(Long tenantId, Pageable pageable);
}
