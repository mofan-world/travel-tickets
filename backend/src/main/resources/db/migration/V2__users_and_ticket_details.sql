create table if not exists app_users (
  id bigserial primary key,
  tenant_id bigint not null,
  name varchar(80) not null,
  company varchar(120) not null,
  email varchar(160) not null,
  password_hash varchar(128) not null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (email)
);

create index if not exists idx_app_users_tenant_created
  on app_users (tenant_id, created_at desc);

alter table travel_tickets
  add column if not exists employee_name varchar(80) not null default '',
  add column if not exists department varchar(120) not null default '',
  add column if not exists trip_purpose varchar(240) not null default '',
  add column if not exists attachment_status varchar(32) not null default 'UPLOADED';

create index if not exists idx_tickets_tenant_employee_name
  on travel_tickets (tenant_id, employee_name);
