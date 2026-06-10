package com.codex.travel.ticket.service;

import java.time.Duration;

import com.codex.travel.ticket.dto.AuthResponse;
import com.codex.travel.ticket.dto.TicketResponse;
import com.codex.travel.ticket.entity.AppUser;
import com.codex.travel.ticket.entity.TravelTicket;
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

    private String ticketKey(Long tenantId, Long ticketId) {
        return "travel-ticket:ticket:" + tenantId + ":" + ticketId;
    }

    private String tenantTicketKey(Long tenantId) {
        return "travel-ticket:tenant:" + tenantId + ":tickets";
    }
}
