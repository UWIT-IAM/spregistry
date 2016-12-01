drop table if exists proxy;

CREATE TABLE proxy (
    id integer NOT NULL AUTO_INCREMENT,
    uuid character(36) NOT NULL,
    entity_id text,
    status integer,
    end_time timestamp NULL DEFAULT now(),
    start_time timestamp DEFAULT now()
);

insert into proxy (uuid, entity_id, status, end_time, start_time)
  values ('58e115ef-36e7-4db6-bad6-cafd784f3faa', 'https://gettest.example.com', 1, null, now());
insert into proxy (uuid, entity_id, status, end_time, start_time)
  values ('298b2a35-a117-48d0-8d89-9957a6e4c91d', 'https://gettest.example.com', 1, null, now());
insert into proxy (uuid, entity_id, status, end_time, start_time)
  values ('80f79afb-0fa5-4d41-ae17-1a599299344a', 'https://notgettest.example.com', 1, null, now());




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

insert into filter_group (id, header_xml, footer_xml, status, update_time, edit_mode)
  values ('uwrp', '<head>', '</head>', 1, now(), 1);
  insert into filter_group (id, header_xml, footer_xml, status, update_time, edit_mode)
    values ('fakenoedit', '<head>', '</head>', 1, now(), 0);


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

drop table if exists metadata_group;
CREATE TABLE metadata_group (
    id character varying(48) NOT NULL,
    header_xml text,
    footer_xml text,
    status integer,
    update_time timestamp DEFAULT now(),
    edit_mode integer
);

ALTER TABLE metadata_group
    ADD PRIMARY KEY (id);

insert into metadata_group (id, header_xml, footer_xml, status, update_time, edit_mode)
  values ('uwrp', '<head>', '</head>', 1, now(), 1);
  insert into metadata_group (id, header_xml, footer_xml, status, update_time, edit_mode)
    values ('fakenoedit', '<head>', '</head>', 1, now(), 0);


drop table if exists metadata;
CREATE TABLE metadata (
    id integer NOT NULL AUTO_INCREMENT,
    uuid character(36) NOT NULL,
    entity_id character varying(128) NOT NULL,
    xml text,
    group_id character varying(48),
    end_time timestamp NULL DEFAULT now(),
    start_time timestamp DEFAULT now(),
    updated_by character varying(48)
);

ALTER TABLE metadata
    ADD PRIMARY KEY (id);

ALTER TABLE metadata
    ADD FOREIGN KEY (group_id) REFERENCES metadata_group(id);


