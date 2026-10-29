-- Baseline: the schema Hibernate (ddl-auto: update) had produced for the JPA entities.
-- Existing databases are baselined at version 1 (FlywayConfig: baselineOnMigrate), so this
-- script only runs on a fresh, empty database. All later changes go in V2, V3, ...

create table accounts (
    id uuid not null,
    user_id uuid not null,
    provider varchar(255) not null,
    provider_account_id varchar(255) not null,
    primary key (id),
    unique (provider, provider_account_id)
);

create table daily_stats (
    date date,
    total_duration integer,
    id uuid not null,
    user_id uuid,
    primary key (id),
    unique (user_id, date)
);

create table group_members (
    joined_at timestamp(6),
    group_id uuid,
    id uuid not null,
    user_id uuid,
    primary key (id),
    unique (group_id, user_id)
);

create table group_rankings (
    total_duration integer,
    group_id uuid not null,
    id uuid not null,
    user_id uuid not null,
    primary key (id),
    unique (group_id, user_id)
);

create table refresh_tokens (
    revoked boolean not null,
    created_at timestamp(6),
    expires_at timestamp(6),
    version bigint,
    id uuid not null,
    replaced_by uuid,
    user_id uuid not null,
    token_hash varchar(255) not null,
    primary key (id)
);

create table session_subjects (
    id uuid not null,
    session_id uuid,
    subject_id uuid,
    primary key (id),
    unique (session_id, subject_id)
);

create table study_groups (
    max_members integer,
    created_at timestamp(6),
    id uuid not null,
    owner_id uuid,
    name varchar(255),
    primary key (id)
);

create table study_sessions (
    duration_seconds integer,
    end_time timestamp(6),
    start_time timestamp(6),
    id uuid not null,
    user_id uuid not null,
    status varchar(255) check ((status in ('RUNNING','COMPLETED'))),
    primary key (id)
);

create table subjects (
    id uuid not null,
    user_id uuid not null,
    name varchar(255),
    primary key (id)
);

create table users (
    created_at timestamp(6),
    id uuid not null,
    email varchar(255) not null unique,
    password varchar(255),
    username varchar(255) not null unique,
    primary key (id)
);

alter table if exists accounts
   add constraint FKnjuop33mo69pd79ctplkck40n
   foreign key (user_id)
   references users;

alter table if exists daily_stats
   add constraint FKkqh2a5n4dc3hregybpmrlfppe
   foreign key (user_id)
   references users;

alter table if exists group_members
   add constraint FK2hpxidj62j5p535elxscas5ig
   foreign key (group_id)
   references study_groups;

alter table if exists group_members
   add constraint FKnr9qg33qt2ovmv29g4vc3gtdx
   foreign key (user_id)
   references users;

alter table if exists group_rankings
   add constraint FK6aqf942548871y67qpij48von
   foreign key (group_id)
   references study_groups;

alter table if exists group_rankings
   add constraint FK3y4epns6rpyss92fo1e3cavml
   foreign key (user_id)
   references users;

alter table if exists refresh_tokens
   add constraint FK1lih5y2npsf8u5o3vhdb9y0os
   foreign key (user_id)
   references users;

alter table if exists session_subjects
   add constraint FK5fr5kh1shypbxkorp4t2gukwi
   foreign key (session_id)
   references study_sessions;

alter table if exists session_subjects
   add constraint FKecq5qgxjl84acrl1yes0wqqio
   foreign key (subject_id)
   references subjects;

alter table if exists study_groups
   add constraint FKkcukq4ecghm2o1f4wht1ntujn
   foreign key (owner_id)
   references users;

alter table if exists study_sessions
   add constraint FK1pw09mt08ohr4r1cuu33qwyw0
   foreign key (user_id)
   references users;

alter table if exists subjects
   add constraint FKm9k2q12kousqip4g5spqrm2am
   foreign key (user_id)
   references users;
