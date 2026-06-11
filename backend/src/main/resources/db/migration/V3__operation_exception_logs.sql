create table if not exists operation_logs (
  id bigserial primary key,
  trace_id varchar(64) not null,
  tenant_id bigint,
  module varchar(64) not null,
  operation varchar(128) not null,
  http_method varchar(12) not null,
  request_uri varchar(512) not null,
  query_string text,
  status_code integer not null,
  success boolean not null,
  elapsed_ms bigint not null,
  client_ip varchar(64),
  user_agent varchar(512),
  created_at timestamptz not null default now()
);

create index if not exists idx_operation_logs_tenant_created
  on operation_logs (tenant_id, created_at desc);

create index if not exists idx_operation_logs_module_created
  on operation_logs (module, created_at desc);

create index if not exists idx_operation_logs_status_created
  on operation_logs (status_code, created_at desc);

create table if not exists exception_logs (
  id bigserial primary key,
  trace_id varchar(64) not null,
  tenant_id bigint,
  http_method varchar(12),
  request_uri varchar(512),
  query_string text,
  status_code integer not null,
  error_code varchar(64) not null,
  exception_type varchar(200) not null,
  message text,
  stack_trace text,
  client_ip varchar(64),
  user_agent varchar(512),
  created_at timestamptz not null default now()
);

create index if not exists idx_exception_logs_tenant_created
  on exception_logs (tenant_id, created_at desc);

create index if not exists idx_exception_logs_type_created
  on exception_logs (exception_type, created_at desc);

create index if not exists idx_exception_logs_status_created
  on exception_logs (status_code, created_at desc);
