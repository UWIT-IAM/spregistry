drop table if exists proxy;

CREATE TABLE proxy (
    entity_id text,
    social_provider text,
    social_key text,
    social_secret text,
    status integer,
    update_time timestamp DEFAULT now()
);

insert into proxy (entity_id, social_provider, social_key, social_secret, status)
  values ('https://gettest.example.com', 'Google', 'thisisthegoog','shhh', 1);
insert into proxy (entity_id, social_provider, social_key, social_secret, status)
  values ('https://gettest.example.com', 'Twitter', 'thisisthetwit','lkdsjf', 1);
insert into proxy (entity_id, social_provider, social_key, social_secret, status)
  values ('https://notgettest.example.com', 'Twitter', 'thisisthetwit','lkdsjf', 1);


drop table if exists filter_group;
CREATE TABLE filter_group (
    id character varying(48) NOT NULL,
    header_xml text,
    footer_xml text,
    status integer,
    update_time timestamp DEFAULT now(),
    edit_mode integer
);

ALTER TABLE filter_group
    ADD PRIMARY KEY (id);

drop table if exists filter;
CREATE TABLE filter (
    entity_id character varying(128) NOT NULL,
    xml text,
    group_id character varying(48),
    status integer,
    update_time timestamp DEFAULT now()
);

ALTER TABLE filter
    ADD PRIMARY KEY (entity_id);

ALTER TABLE filter
    ADD FOREIGN KEY (group_id) REFERENCES filter_group(id);




