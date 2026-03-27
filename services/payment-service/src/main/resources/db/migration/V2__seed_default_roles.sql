insert into roles (name)
values ('ADMIN')
on conflict (name) do nothing;

insert into roles (name)
values ('USER')
on conflict (name) do nothing;
