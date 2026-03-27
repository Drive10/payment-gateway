alter table payment_outbox_events
    add column if not exists message_headers text;
