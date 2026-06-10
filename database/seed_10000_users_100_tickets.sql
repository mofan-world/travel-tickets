-- Seed 10,000 users and 1,000,000 tickets for load testing.
-- This script is idempotent for the generated email/ticket number ranges.
-- Generated users:
--   loaduser00001@travel.local ... loaduser10000@travel.local
--   password: loadtest123
-- Generated tickets:
--   LT-00001-001 ... LT-10000-100

\timing on

set client_encoding = 'UTF8';

begin;

set local synchronous_commit = off;

with seed_users as (
  select generate_series(1, 10000) as user_no
)
insert into app_users (
  tenant_id,
  name,
  company,
  email,
  password_hash,
  created_at,
  updated_at
)
select
  10001,
  U&'\538B\6D4B\7528\6237' || lpad(user_no::text, 5, '0'),
  U&'\793A\4F8B\96C6\56E2',
  'loaduser' || lpad(user_no::text, 5, '0') || '@travel.local',
  '6bcb248df28adbe9ed8be142d232c6d2820785afa243fabe3a571fb50b8cf768',
  now(),
  now()
from seed_users
on conflict (email) do update set
  tenant_id = excluded.tenant_id,
  name = excluded.name,
  company = excluded.company,
  password_hash = excluded.password_hash,
  updated_at = now();

with seed_users as (
  select generate_series(1, 10000) as user_no
),
seed_tickets as (
  select generate_series(1, 100) as ticket_no
),
ticket_source as (
  select
    10001::bigint as tenant_id,
    (900000000 + u.user_no)::bigint as employee_id,
    U&'\538B\6D4B\7528\6237' || lpad(u.user_no::text, 5, '0') as employee_name,
    (U&'\538B\6D4B\90E8\95E8' || ((u.user_no % 20) + 1)) as department,
    'LT-' || lpad(u.user_no::text, 5, '0') || '-' || lpad(t.ticket_no::text, 3, '0') as ticket_no,
    case (t.ticket_no % 6)
      when 0 then 'HIGH_SPEED_RAIL'
      when 1 then 'TRAIN'
      when 2 then 'EMU'
      when 3 then 'INTERCITY'
      when 4 then 'FLIGHT'
      else 'OTHER'
    end as travel_type,
    (array[
      U&'\5317\4EAC',
      U&'\4E0A\6D77',
      U&'\5E7F\5DDE',
      U&'\6DF1\5733',
      U&'\676D\5DDE',
      U&'\6210\90FD',
      U&'\6B66\6C49',
      U&'\5357\4EAC',
      U&'\897F\5B89',
      U&'\91CD\5E86'
    ])[((u.user_no + t.ticket_no) % 10) + 1] as departure_city,
    (array[
      U&'\5929\6D25',
      U&'\82CF\5DDE',
      U&'\53A6\95E8',
      U&'\9752\5C9B',
      U&'\90D1\5DDE',
      U&'\957F\6C99',
      U&'\5408\80A5',
      U&'\5B81\6CE2',
      U&'\798F\5DDE',
      U&'\6606\660E'
    ])[((u.user_no + t.ticket_no * 3) % 10) + 1] as arrival_city,
    case (t.ticket_no % 5)
      when 0 then 'G' || (1000 + (u.user_no % 9000))::text
      when 1 then 'D' || (1000 + (u.user_no % 9000))::text
      when 2 then 'C' || (1000 + (u.user_no % 9000))::text
      when 3 then 'MU' || (1000 + (u.user_no % 9000))::text
      else 'CA' || (1000 + (u.user_no % 9000))::text
    end as carrier_no,
    case (t.ticket_no % 4)
      when 0 then U&'\4E8C\7B49\5EA7'
      when 1 then U&'\4E00\7B49\5EA7'
      when 2 then U&'\5546\52A1\5EA7'
      else U&'\7ECF\6D4E\8231'
    end as seat_class,
    U&'\6279\91CF\538B\6D4B\51FA\5DEE\7533\8BF7-' || lpad(u.user_no::text, 5, '0') as trip_purpose,
    case (t.ticket_no % 10)
      when 0 then 'MISSING'
      else 'UPLOADED'
    end as attachment_status,
    timestamptz '2026-01-01 08:00:00+08'
      + (((u.user_no - 1) * 100 + t.ticket_no) * interval '1 minute') as depart_at,
    timestamptz '2026-01-01 08:00:00+08'
      + (((u.user_no - 1) * 100 + t.ticket_no) * interval '1 minute')
      + interval '3 hours' as arrive_at,
    (80 + ((u.user_no * 37 + t.ticket_no * 11) % 1800))::numeric(12, 2) as amount,
    case (t.ticket_no % 6)
      when 0 then 'PENDING_REVIEW'
      when 1 then 'APPROVED'
      when 2 then 'REJECTED'
      when 3 then 'REIMBURSED'
      when 4 then 'EXCEPTION'
      else 'MISSING_ATTACHMENT'
    end as status,
    case
      when (t.ticket_no % 10) = 0 then 'MEDIUM'
      when (80 + ((u.user_no * 37 + t.ticket_no * 11) % 1800)) >= 1000 then 'HIGH'
      else 'NONE'
    end as risk_level
  from seed_users u
  cross join seed_tickets t
)
insert into travel_tickets (
  tenant_id,
  employee_id,
  employee_name,
  department,
  ticket_no,
  external_source,
  external_ticket_id,
  travel_type,
  departure_city,
  arrival_city,
  carrier_no,
  seat_class,
  trip_purpose,
  attachment_status,
  depart_at,
  arrive_at,
  amount,
  currency,
  status,
  risk_level,
  version,
  created_at,
  updated_at
)
select
  tenant_id,
  employee_id,
  employee_name,
  department,
  ticket_no,
  'LOAD_TEST',
  ticket_no,
  travel_type,
  departure_city,
  arrival_city,
  carrier_no,
  seat_class,
  trip_purpose,
  attachment_status,
  depart_at,
  arrive_at,
  amount,
  'CNY',
  status,
  risk_level,
  0,
  now(),
  now()
from ticket_source
on conflict (tenant_id, ticket_no) do update set
  employee_id = excluded.employee_id,
  employee_name = excluded.employee_name,
  department = excluded.department,
  external_source = excluded.external_source,
  external_ticket_id = excluded.external_ticket_id,
  travel_type = excluded.travel_type,
  departure_city = excluded.departure_city,
  arrival_city = excluded.arrival_city,
  carrier_no = excluded.carrier_no,
  seat_class = excluded.seat_class,
  trip_purpose = excluded.trip_purpose,
  attachment_status = excluded.attachment_status,
  depart_at = excluded.depart_at,
  arrive_at = excluded.arrive_at,
  amount = excluded.amount,
  currency = excluded.currency,
  status = excluded.status,
  risk_level = excluded.risk_level,
  updated_at = now();

analyze app_users;
analyze travel_tickets;

commit;

select
  count(*) as load_test_users
from app_users
where email like 'loaduser%@travel.local';

select
  count(*) as load_test_tickets,
  count(distinct employee_id) as load_test_employees,
  min(ticket_no) as min_ticket_no,
  max(ticket_no) as max_ticket_no
from travel_tickets
where tenant_id = 10001
  and ticket_no like 'LT-%';
