create table if not exists travel_tickets (
  id bigserial primary key,
  tenant_id bigint not null,
  employee_id bigint not null,
  ticket_no varchar(96) not null,
  external_source varchar(48),
  external_ticket_id varchar(128),
  travel_type varchar(32) not null default 'TRAIN',
  departure_city varchar(80) not null,
  arrival_city varchar(80) not null,
  carrier_no varchar(64) not null,
  seat_class varchar(64),
  depart_at timestamptz,
  arrive_at timestamptz,
  amount numeric(12, 2) not null,
  currency char(3) not null default 'CNY',
  status varchar(32) not null default 'PENDING_REVIEW',
  risk_level varchar(16) not null default 'NONE',
  version integer not null default 0,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (tenant_id, ticket_no)
);

create index if not exists idx_tickets_tenant_status_created
  on travel_tickets (tenant_id, status, created_at desc);

create index if not exists idx_tickets_tenant_employee_created
  on travel_tickets (tenant_id, employee_id, created_at desc);

create index if not exists idx_tickets_route_created
  on travel_tickets (tenant_id, departure_city, arrival_city, created_at desc);

create index if not exists idx_tickets_external_id
  on travel_tickets (tenant_id, external_source, external_ticket_id);

create index if not exists idx_tickets_risk_created
  on travel_tickets (tenant_id, risk_level, created_at desc);
