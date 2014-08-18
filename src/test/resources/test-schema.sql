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


