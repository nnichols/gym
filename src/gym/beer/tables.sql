CREATE TABLE IF NOT EXISTS recipes(
    id uuid PRIMARY KEY,
    name text NOT NULL,
    style text NOT NULL,
    fermentables jsonb NOT NULL,
    hops jsonb NOT NULL,
    yeast jsonb NOT NULL,
);

CREATE INDEX CONCURRENTLY IF NOT EXISTS recipes_style ON recipes USING btree (style);

CREATE TABLE IF NOT EXISTS fermentables(
    id uuid PRIMARY KEY,
    name text NOT NULL,
    type text NOT NULL,
    body jsonb NOT NULL,
);

CREATE INDEX CONCURRENTLY IF NOT EXISTS fermentables_type ON fermentables (type);
CREATE INDEX CONCURRENTLY IF NOT EXISTS fermentables_name ON fermentables (name);

CREATE TABLE IF NOT EXISTS hops(
    id uuid PRIMARY KEY,
    name text NOT NULL,
    type text NOT NULL,
    body jsonb NOT NULL,
);

CREATE INDEX CONCURRENTLY IF NOT EXISTS hops_type ON hops (type);
CREATE INDEX CONCURRENTLY IF NOT EXISTS hops_name ON hops (name);

CREATE TABLE IF NOT EXISTS yeasts(
    id uuid PRIMARY KEY,
    name text NOT NULL,
    type text NOT NULL,
    body jsonb NOT NULL,
);

CREATE INDEX CONCURRENTLY IF NOT EXISTS yeasts_type ON yeasts (type);
CREATE INDEX CONCURRENTLY IF NOT EXISTS yeasts_name ON yeasts (name);

SELECT style, COUNT(style) FROM RECIPES GROUP BY style ORDER BY count DESC;

CREATE TABLE IF NOT EXISTS relationship(
    id uuid PRIMARY KEY,
    entity_one_name text NOT NULL,
    entity_one_type text NOT NULL,
    entity_two_name text NOT NULL,
    entity_two_type text NOT NULL,
    occurences int NOT NULL
);

CREATE INDEX CONCURRENTLY IF NOT EXISTS e_o_name ON relationship (entity_one_name);
CREATE INDEX CONCURRENTLY IF NOT EXISTS e_o_type ON relationship (entity_one_type);
CREATE INDEX CONCURRENTLY IF NOT EXISTS e_t_name ON relationship (entity_two_name);
CREATE INDEX CONCURRENTLY IF NOT EXISTS e_t_type ON relationship (entity_two_weight);

ALTER TABLE relationship ADD CONSTRAINT no_duplicates UNIQUE (entity_one_name, entity_one_type, entity_two_name, entity_two_type);