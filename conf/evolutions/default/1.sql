# --- !Ups
CREATE TABLE Event (
    id varchar NOT NULL ,
    title varchar(255) NOT NULL,
    owner_id bigint(20) NOT NULL,
    PRIMARY KEY(id)
);

CREATE TABLE Member (
    id bigint(20) NOT NULL,
    event varchar NOT NULL,
    name varchar(255) NOT NULL,
    PRIMARY KEY(id)
);

# --- !Downs
DROP TABLE Member;
DROP TABLE Event;