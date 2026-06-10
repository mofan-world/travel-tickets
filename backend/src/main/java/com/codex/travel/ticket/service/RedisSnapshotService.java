package com.codex.travel.ticket.service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import com.codex.travel.ticket.common.PageResult;
import com.codex.travel.ticket.dto.AuthResponse;
import com.codex.travel.ticket.dto.DashboardSummaryResponse;
import com.codex.travel.ticket.dto.RiskEventResponse;
import com.codex.travel.ticket.dto.TicketResponse;
import com.codex.travel.ticket.entity.AppUser;
import com.codex.travel.ticket.entity.TravelTicket;
import com.codex.travel.ticket.enums.TicketStatus;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisSnapshotService {

    private static final Logger log = LoggerFactory.getLogger(RedisSnapshotService.class);
    private static final Duration SNAPSHOT_TTL = Duration.ofHours(12);
    private static final Duration READ_MODEL_TTL = Duration.ofMinutes(5);

    private final ObjectProvider<StringRedisTemplate> redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisSnapshotService(ObjectProvider<StringRedisTemplate> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void writeTicket(TravelTicket ticket) {
        StringRedisTemplate redis = redisTemplate.getIfAvailable();
        if (redis == null) {
            return;
        }
        try {
            String key = ticketKey(ticket.getTenantId(), ticket.getId());
            String tenantKey = tenantTicketKey(ticket.getTenantId());
            redis.opsForValue().set(key, objectMapper.writeValueAsString(TicketResponse.from(ticket)), SNAPSHOT_TTL);
            redis.opsForZSet().add(tenantKey, String.valueOf(ticket.getId()), System.currentTimeMillis());
            redis.expire(tenantKey, SNAPSHOT_TTL);
        } catch (Exception ex) {
            log.warn("failed to write ticket snapshot to redis, ticketId={}", ticket.getId(), ex);
        }
    }

    public void deleteTicket(Long tenantId, Long ticketId) {
        StringRedisTemplate redis = redisTemplate.getIfAvailable();
        if (redis == null) {
            return;
        }
        try {
            redis.delete(ticketKey(tenantId, ticketId));
            redis.opsForZSet().remove(tenantTicketKey(tenantId), String.valueOf(ticketId));
        } catch (Exception ex) {
            log.warn("failed to delete ticket snapshot from redis, ticketId={}", ticketId, ex);
        }
    }

    public Optional<PageResult<TicketResponse>> readTicketPage(Long tenantId, TicketStatus status, int page, int size) {
        return read(ticketPageKey(tenantId, status, page, size), new TypeReference<>() {
        });
    }

    public void writeTicketPage(Long tenantId, TicketStatus status, int page, int size, PageResult<TicketResponse> result) {
        writeReadModel(ticketPageKey(tenantId, status, page, size), result);
    }

    public Optional<PageResult<RiskEventResponse>> readRiskEvents(Long tenantId, int page, int size) {
        return read(riskEventsKey(tenantId, page, size), new TypeReference<>() {
        });
    }

    public void writeRiskEvents(Long tenantId, int page, int size, PageResult<RiskEventResponse> result) {
        writeReadModel(riskEventsKey(tenantId, page, size), result);
    }

    public Optional<DashboardSummaryResponse> readDashboardSummary(Long tenantId) {
        return read(dashboardSummaryKey(tenantId), new TypeReference<>() {
        });
    }

    public void writeDashboardSummary(Long tenantId, DashboardSummaryResponse result) {
        writeReadModel(dashboardSummaryKey(tenantId), result);
    }

    public void bumpTenantVersion(Long tenantId) {
        StringRedisTemplate redis = redisTemplate.getIfAvailable();
        if (redis == null) {
            return;
        }
        try {
            redis.opsForValue().increment(tenantVersionKey(tenantId));
        } catch (Exception ex) {
            log.warn("failed to bump tenant redis read model version, tenantId={}", tenantId, ex);
        }
    }

    public void writeUser(AppUser user) {
        StringRedisTemplate redis = redisTemplate.getIfAvailable();
        if (redis == null) {
            return;
        }
        try {
            redis.opsForValue().set(
                    "travel-ticket:user:" + user.getId(),
                    objectMapper.writeValueAsString(AuthResponse.from(user)),
                    SNAPSHOT_TTL);
        } catch (Exception ex) {
            log.warn("failed to write user snapshot to redis, userId={}", user.getId(), ex);
        }
    }

    private <T> Optional<T> read(String key, TypeReference<T> typeReference) {
        StringRedisTemplate redis = redisTemplate.getIfAvailable();
        if (redis == null) {
            return Optional.empty();
        }
        try {
            String value = redis.opsForValue().get(key);
            if (value == null) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(value, typeReference));
        } catch (Exception ex) {
            log.warn("failed to read redis read model cache, key={}", key, ex);
            redis.delete(key);
            return Optional.empty();
        }
    }

    private void writeReadModel(String key, Object value) {
        StringRedisTemplate redis = redisTemplate.getIfAvailable();
        if (redis == null) {
            return;
        }
        try {
            redis.opsForValue().set(key, objectMapper.writeValueAsString(value), READ_MODEL_TTL);
        } catch (Exception ex) {
            log.warn("failed to write redis read model cache, key={}", key, ex);
        }
    }

    private String ticketPageKey(Long tenantId, TicketStatus status, int page, int size) {
        return "travel-ticket:read:v1:tickets:" + tenantId + ":" + tenantVersion(tenantId) + ":"
                + (status == null ? "ALL" : status.name()) + ":" + page + ":" + size;
    }

    private String riskEventsKey(Long tenantId, int page, int size) {
        return "travel-ticket:read:v1:risk-events:" + tenantId + ":" + tenantVersion(tenantId) + ":" + page + ":" + size;
    }

    private String dashboardSummaryKey(Long tenantId) {
        return "travel-ticket:read:v2:dashboard-summary:" + tenantId + ":" + tenantVersion(tenantId);
    }

    private String tenantVersion(Long tenantId) {
        StringRedisTemplate redis = redisTemplate.getIfAvailable();
        if (redis == null) {
            return "0";
        }
        try {
            String version = redis.opsForValue().get(tenantVersionKey(tenantId));
            return version == null ? "0" : version;
        } catch (Exception ex) {
            log.warn("failed to read tenant redis read model version, tenantId={}", tenantId, ex);
            return "0";
        }
    }

    private String tenantVersionKey(Long tenantId) {
        return "travel-ticket:tenant:" + tenantId + ":version";
    }

    private String ticketKey(Long tenantId, Long ticketId) {
        return "travel-ticket:ticket:" + tenantId + ":" + ticketId;
    }

    private String tenantTicketKey(Long tenantId) {
        return "travel-ticket:tenant:" + tenantId + ":tickets";
    }
}
