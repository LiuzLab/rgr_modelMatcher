alter table user
    add column created_at timestamp;
alter table user
    add column modified_at timestamp;
alter table user
    add column enabled_at timestamp after enabled;
alter table user
    add column contact_email_verified_at timestamp after contact_email_verified;
alter table gene
    add column created_at timestamp;
alter table user_ontology_term
    add column created_at timestamp;
alter table user_organ
    add column created_at timestamp;
alter table term
    add column created_at timestamp;
alter table access_token
    add column created_at timestamp;
alter table verification_token
    add column created_at timestamp;
alter table password_reset_token
    add column created_at timestamp;