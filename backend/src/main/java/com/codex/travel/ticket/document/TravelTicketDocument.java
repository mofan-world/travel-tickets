package com.codex.travel.ticket.document;

import java.math.BigDecimal;
import java.time.Instant;

import com.codex.travel.ticket.entity.TravelTicket;
import com.codex.travel.ticket.enums.RiskLevel;
import com.codex.travel.ticket.enums.TicketStatus;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "travel-ticket-v1")
public class TravelTicketDocument {

    @Id
    private String id;

    @Field(type = FieldType.Long)
    private Long tenantId;

    @Field(type = FieldType.Long)
    private Long ticketId;

    @Field(type = FieldType.Long)
    private Long employeeId;

    @Field(type = FieldType.Keyword)
    private String ticketNo;

    @Field(type = FieldType.Text)
    private String route;

    @Field(type = FieldType.Keyword)
    private String departureCity;

    @Field(type = FieldType.Keyword)
    private String arrivalCity;

    @Field(type = FieldType.Keyword)
    private String carrierNo;

    @Field(type = FieldType.Double)
    private BigDecimal amount;

    @Field(type = FieldType.Keyword)
    private TicketStatus status;

    @Field(type = FieldType.Keyword)
    private RiskLevel riskLevel;

    @Field(type = FieldType.Date, format = DateFormat.date_time)
    private Instant createdAt;

    public static TravelTicketDocument from(TravelTicket ticket) {
        TravelTicketDocument document = new TravelTicketDocument();
        document.id = ticket.getTenantId() + ":" + ticket.getId();
        document.tenantId = ticket.getTenantId();
        document.ticketId = ticket.getId();
        document.employeeId = ticket.getEmployeeId();
        document.ticketNo = ticket.getTicketNo();
        document.route = ticket.getDepartureCity() + " " + ticket.getArrivalCity() + " " + ticket.getCarrierNo();
        document.departureCity = ticket.getDepartureCity();
        document.arrivalCity = ticket.getArrivalCity();
        document.carrierNo = ticket.getCarrierNo();
        document.amount = ticket.getAmount();
        document.status = ticket.getStatus();
        document.riskLevel = ticket.getRiskLevel();
        document.createdAt = ticket.getCreatedAt();
        return document;
    }

    public String getId() {
        return id;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public Long getTicketId() {
        return ticketId;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public String getTicketNo() {
        return ticketNo;
    }

    public String getRoute() {
        return route;
    }

    public String getDepartureCity() {
        return departureCity;
    }

    public String getArrivalCity() {
        return arrivalCity;
    }

    public String getCarrierNo() {
        return carrierNo;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
