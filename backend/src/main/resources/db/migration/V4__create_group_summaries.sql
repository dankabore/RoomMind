-- The saved summary of a group. One row per group at most: the conversation id
-- is the primary key, so a group can never end up with two competing summaries.
--
-- Each new summary is built from the previous one plus the messages after
-- last_message_id, so a request only sends the model what is new.
create table group_summaries (
    -- "on delete cascade" lets the database remove the summary itself when its
    -- group is deleted, which happens when the last member leaves. Without it
    -- that delete would be refused, and the code that ends a group would have
    -- to know summaries exist.
    conversation_id bigint      primary key references conversations (id) on delete cascade,

    -- text rather than varchar(n): the model decides how long a summary is,
    -- and a length limit here would turn a long one into a failed save.
    summary         text        not null,

    -- The newest message the summary covers. Deliberately not a foreign key:
    -- when a group ends its messages are deleted before the conversation, and a
    -- reference from here would block that. It is only a marker of how far the
    -- summary reaches, compared against message ids.
    last_message_id bigint      not null,

    updated_at      timestamptz not null default now()
);
