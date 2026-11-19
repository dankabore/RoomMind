-- The @ai assistant's answers are ordinary messages, and every message has a
-- sender, so the assistant gets an account of its own to send them from.

-- Marks that account. Everything that must treat it differently (leaving it
-- off the people page, drawing its messages differently) reads this column
-- rather than recognising a particular username.
alter table users
    add column assistant boolean not null default false;

-- At most one assistant. The index only covers rows where the column is true,
-- so ordinary accounts are unaffected and a second assistant row is refused.
create unique index uq_users_one_assistant on users (assistant) where assistant;

-- The account itself. Nobody can sign in as it: '!' is not a BCrypt hash, so
-- no password ever matches. The username cannot already be taken, because
-- registration requires at least three characters.
insert into users (email, username, password_hash, display_name, assistant)
values ('assistant@roommind.invalid', 'ai', '!', 'AI assistant', true);
