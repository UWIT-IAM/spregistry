drop table if exists proxy;

CREATE TABLE proxy (
    entity_id text,
    social_provider text,
    social_key text,
    social_secret text,
    status integer,
    -- update_time timestamp with time zone DEFAULT now()
);


-- ALTER TABLE public.proxy OWNER TO spreg;

--
-- Name: entity_id_idx; Type: INDEX; Schema: public; Owner: spreg; Tablespace:
--

--CREATE INDEX entity_id_idx ON proxy USING btree (entity_id);


insert into proxy (entity_id, social_provider, social_key, social_secret)
    values ('https://www.example.com', 'Google', 'thisisthegoog','shhh');

