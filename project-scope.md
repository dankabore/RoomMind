## Problem

I want to get more project on my resume and i wanted something that uses live technologies such as websockets and also ai.

## Solution

Build a real-time chat application that uses websockets for live communication and integrates ai for summarizing the conversation, translating messages and helping users not having to read tons of messages in group chats to know what's going on.

## Features

- Real time messaging users, on a one to one.
- Group chat functionality with multiple users.
- AI-powered message summarization to provide quick overviews of long conversations.
- AI-powered translation of messages to support multilingual communication.
- @Ai assistant to help users with questions they might have about the chat.
- profile management for users to customize their experience.
- Dashboard for each user to view all their contacts, groups, and recent conversations.

## Decisions

### Finding people and starting a conversation

All registered users are listed on a single "people" page, searchable by
username. From that page you can message anyone directly.

There is no friend request or accept step. A one-to-one conversation is created
implicitly by the first message sent.

### Message history

Every message is stored, including messages that arrive while you are offline.
When you open a conversation you see the recent messages and can scroll up to
load older ones, so past conversations are always available.

### Profile

Each user has a profile they can edit. At minimum it holds their username and
their preferred language, since the language setting is what the translation
feature reads to know what to translate into.

### Translation

Each message can be translated on demand with a translate button. The target
language comes from the reader's own profile.

Translation is never automatic and the original message text is always
preserved.

### Summarization

When a conversation has piled up and you want to remember what was being
discussed, you press a summarize button and the app describes what the
conversation has been about. Like translation, this is on demand only.

### @Ai assistant

The @Ai assistant is the next step up from the two features above: instead of
summarizing everything, you ask a specific question about the conversation and
get an answer.

The answer is posted publicly into the chat, so everyone in the conversation
sees both the question and the response.

### Group chats

- Whoever creates the group is its admin.
- The admin can add people and remove people.
- Users cannot join a group on their own. The only way in is to be added by the
  admin.
- Any ordinary member can leave on their own.
- The admin cannot simply leave. If there is anyone else in the group, the
  admin must hand the admin role to another member first, and only then can
  they leave. Because of this, admin transfer is required, not optional.
- An admin who is the last person in the group can leave, and the group ends
  with them.

### Not the first priority

These are wanted eventually, but are explicitly not part of the first push:

- Online / offline presence
- Typing indicators
- Unread counts
- Read receipts

## Proposed first version

The suggested cut for the first working version is accounts and login, the
people page, one-to-one and group messaging over websockets, stored history
with scrollback, basic profile editing, and summarization as the single AI
feature.

Translation and the @Ai assistant come second. All three AI features run on the
same underlying plumbing, so once summarization works the other two are much
cheaper to add.

This proposal is not yet confirmed.

## Still open

- Tech stack: backend, frontend and database, and which parts are meant to be a
  learning exercise.
- Which AI provider to use, and whether there is an API key and budget already.
- Whether the proposed first version above is the right cut.
- Whether this gets deployed to a public URL or stays a local demo with a
  recorded walkthrough.
- What happens to a group if its admin's account is deleted, since the rules
  above assume an admin is always present to hand off the role.
