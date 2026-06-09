-- PostgreSQL schema for 出差车票管理系统
-- Core tables are designed for multi-tenant scale and monthly partitioning.

create table tenants (
  id bigserial primary key,
  tenant_code varchar(64) not null unique,
  name varchar(160) not null,
  status varchar(24) not null default 'active',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table departments (
  id bigserial primary key,
  tenant_id bigint not null references tenants(id),
  parent_id bigint references departments(id),
  name varchar(160) not null,
  path varchar(512) not null,
  created_at timestamptz not null default now()
);

create index idx_departments_tenant_path on departments (tenant_id, path);

create table employees (
  id bigserial primary key,
  tenant_id bigint not null references tenants(id),
  department_id bigint references departments(id),
  employee_no varchar(64) not null,
  name varchar(80) not null,
  mobile varchar(32),
  email varchar(160),
  status varchar(24) not null default 'active',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (tenant_id, employee_no)
);

create index idx_employees_tenant_department on employees (tenant_id, department_id);
create index idx_employees_tenant_name on employees (tenant_id, name);

create table travel_tickets (
  id bigserial not null,
  tenant_id bigint not null references tenants(id),
  employee_id bigint not null references employees(id),
  ticket_no varchar(96) not null,
  external_source varchar(48),
  external_ticket_id varchar(128),
  travel_type varchar(32) not null default 'train',
  departure_city varchar(80) not null,
  arrival_city varchar(80) not null,
  carrier_no varchar(64) not null,
  seat_class varchar(64),
  depart_at timestamptz,
  arrive_at timestamptz,
  amount numeric(12, 2) not null,
  currency char(3) not null default 'CNY',
  status varchar(32) not null default 'pending_review',
  risk_level varchar(16) not null default 'none',
  version integer not null default 1,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  primary key (id, created_at),
  unique (tenant_id, ticket_no, created_at)
) partition by range (created_at);

create index idx_tickets_tenant_status_created on travel_tickets (tenant_id, status, created_at desc);
create index idx_tickets_tenant_employee_created on travel_tickets (tenant_id, employee_id, created_at desc);
create index idx_tickets_route_created on travel_tickets (tenant_id, departure_city, arrival_city, created_at desc);
create index idx_tickets_external_id on travel_tickets (tenant_id, external_source, external_ticket_id);

create table ticket_attachments (
  id bigserial primary key,
  tenant_id bigint not null references tenants(id),
  ticket_id bigint not null,
  object_key varchar(512) not null,
  file_name varchar(255) not null,
  mime_type varchar(120) not null,
  file_size bigint not null,
  ocr_status varchar(32) not null default 'pending',
  ocr_payload jsonb,
  created_at timestamptz not null default now()
);

create index idx_attachments_ticket on ticket_attachments (tenant_id, ticket_id);
create index idx_attachments_ocr_status on ticket_attachments (tenant_id, ocr_status, created_at);

create table approval_instances (
  id bigserial primary key,
  tenant_id bigint not null references tenants(id),
  ticket_id bigint not null,
  employee_id bigint not null references employees(id),
  status varchar(32) not null default 'pending',
  current_node varchar(96),
  amount numeric(12, 2) not null,
  version integer not null default 1,
  submitted_at timestamptz not null default now(),
  finished_at timestamptz
);

create index idx_approval_tenant_status on approval_instances (tenant_id, status, submitted_at desc);
create index idx_approval_ticket on approval_instances (tenant_id, ticket_id);

create table approval_tasks (
  id bigserial primary key,
  tenant_id bigint not null references tenants(id),
  approval_id bigint not null references approval_instances(id),
  approver_id bigint not null references employees(id),
  status varchar(32) not null default 'pending',
  action varchar(32),
  comment text,
  due_at timestamptz,
  created_at timestamptz not null default now(),
  completed_at timestamptz
);

create index idx_tasks_approver_status on approval_tasks (tenant_id, approver_id, status, created_at desc);

create table risk_events (
  id bigserial primary key,
  tenant_id bigint not null references tenants(id),
  ticket_id bigint not null,
  rule_code varchar(64) not null,
  risk_level varchar(16) not null,
  message varchar(512) not null,
  payload jsonb,
  status varchar(32) not null default 'open',
  created_at timestamptz not null default now(),
  resolved_at timestamptz
);

create index idx_risk_tenant_status on risk_events (tenant_id, status, created_at desc);
create index idx_risk_ticket on risk_events (tenant_id, ticket_id);

create table reimbursements (
  id bigserial primary key,
  tenant_id bigint not null references tenants(id),
  ticket_id bigint not null,
  approval_id bigint references approval_instances(id),
  erp_batch_no varchar(96),
  status varchar(32) not null default 'pending_push',
  amount numeric(12, 2) not null,
  pushed_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index idx_reimbursements_tenant_status on reimbursements (tenant_id, status, created_at desc);

create table audit_logs (
  id bigserial primary key,
  tenant_id bigint not null references tenants(id),
  actor_id bigint,
  resource_type varchar(64) not null,
  resource_id varchar(96) not null,
  action varchar(64) not null,
  before_value jsonb,
  after_value jsonb,
  trace_id varchar(96),
  created_at timestamptz not null default now()
);

create index idx_audit_resource on audit_logs (tenant_id, resource_type, resource_id, created_at desc);
create index idx_audit_trace on audit_logs (trace_id);

-- Example monthly partition. Create future partitions with an automated migration job.
create table travel_tickets_2026_06 partition of travel_tickets
for values from ('2026-06-01') to ('2026-07-01');
