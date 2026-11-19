-- The profile: what a person wants to be called, and the language they read.

-- Optional. Someone who never sets one is shown by their username, so there is
-- nothing sensible to fill existing rows with, and null means "not set".
alter table users
    add column display_name varchar(50);

-- The language translations are written in for this reader. Stored as the
-- two-letter ISO 639-1 code in capitals ('EN', 'FR'), which is the name of the
-- matching constant in the Language enum. Every existing account gets English
-- through the default, so the column can be required from the start.
--
-- The check keeps the column to the codes the code knows. Offering another
-- language means adding it to the enum and to this list in a new migration.
alter table users
    add column language varchar(5) not null default 'EN';

alter table users
    add constraint chk_users_language check (
        language in ('EN', 'FR', 'ES', 'DE', 'IT', 'PT', 'AR', 'ZH')
    );
