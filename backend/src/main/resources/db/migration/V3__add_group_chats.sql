-- Group chats. Until now every conversation was a direct one between two
-- people, so a conversation needed no name and every member was equal. Both
-- become variable here.

-- What kind of conversation this is. A direct one is found by its two members;
-- a group is opened by its id and keeps its identity even when people come and
-- go, so the two cannot be told apart by membership alone any more. The check
-- constraint is what keeps the column to the two values the code knows, since
-- Postgres has no enum here and the entity stores the name as text.
alter table conversations
    add column type varchar(20) not null default 'DIRECT';

alter table conversations
    add constraint chk_conversations_type check (type in ('DIRECT', 'GROUP'));

-- Only a group carries a name; a direct conversation is named after the other
-- person by whoever is reading it, which differs per reader and so cannot be
-- stored. The check keeps a stray name off a direct row and makes sure a group
-- has one.
alter table conversations
    add column name varchar(100);

alter table conversations
    add constraint chk_conversations_name check (
        (type = 'GROUP'  and name is not null)
     or (type = 'DIRECT' and name is null)
    );

-- Whether this member runs the group. The admin adds and removes people and
-- must hand the role on before leaving, so it lives on the membership row
-- rather than as an owner column on conversations: handing it over is then one
-- update of two rows and the admin is always someone who is actually a member.
-- Members of a direct conversation are all ordinary; the column is ignored
-- there.
alter table conversation_members
    add column role varchar(20) not null default 'MEMBER';

alter table conversation_members
    add constraint chk_conversation_members_role check (role in ('ADMIN', 'MEMBER'));
