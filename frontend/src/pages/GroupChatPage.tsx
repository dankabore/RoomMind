import { useState } from 'react'
import type { ReactNode } from 'react'
import { useQuery } from '@tanstack/react-query'
import { useNavigate, useParams } from 'react-router-dom'
import GroupChatHeader from '../components/GroupChatHeader'
import GroupMembers from '../components/GroupMembers'
import MessageComposer from '../components/MessageComposer'
import MessageList from '../components/MessageList'
import { api, errorMessage } from '../lib/api'
import type { Person } from '../lib/chat'
import { useLiveMessages, useMessages, useSendMessage } from '../lib/chat'
import type { Group } from '../lib/groups'
import { useAddMember, useGroup, useLeaveGroup, useRemoveMember, useTransferAdmin } from '../lib/groups'

/**
 * One group conversation.
 *
 * The address is /group/<conversation id>, not a person's id the way a direct
 * chat is. A group exists in its own right from the moment it is created, so
 * there is nothing to find or create on arrival — and no person to name it
 * after.
 *
 * This part only fetches the group. The screen is GroupView below, drawn once
 * the answer is in, so nothing inside it has to cope with a group that is not
 * loaded yet.
 */
function GroupChatPage() {
  const { conversationId } = useParams()
  const groupId = Number(conversationId)
  const { data: group, isPending, error } = useGroup(groupId)

  // First, as on the chat page: for an address like /group/abc the fetch never
  // runs, and a fetch that never runs counts as pending forever.
  if (!Number.isInteger(groupId)) {
    return <Centered>That is not a group.</Centered>
  }

  if (isPending) {
    return <Centered>Opening the group…</Centered>
  }

  if (!group) {
    return <Centered>{errorMessage(error, 'Could not open that group.')}</Centered>
  }

  // A fresh GroupView per group, so moving between two never carries over a
  // half-typed message or a scroll position.
  return <GroupView key={group.id} group={group} />
}

/**
 * The group screen: header, messages, input box, and the member list when it is
 * open. It connects the data from lib/chat and lib/groups to the components
 * that draw it, and does nothing else.
 */
function GroupView({ group }: { group: Group }) {
  // Cached by RequireAuth before this page rendered, so this reads the answer
  // rather than asking again. It says which messages are your own, and whether
  // you are the admin.
  const { data: me } = useQuery({
    queryKey: ['me'],
    queryFn: async () => {
      const response = await api.get<Person>('/api/auth/me')
      return response.data
    },
    retry: false,
  })

  const navigate = useNavigate()
  const [membersOpen, setMembersOpen] = useState(false)

  const history = useMessages(group.id)
  useLiveMessages(group.id)
  const sendMessage = useSendMessage(group.id)
  const addMember = useAddMember(group.id)
  const removeMember = useRemoveMember(group.id)
  const transferAdmin = useTransferAdmin(group.id)
  const leaveGroup = useLeaveGroup(group.id)

  // Leaving is the one change that makes this screen unreadable to the person
  // who made it, so it is also the one that navigates away. replace, because
  // going Back to a group you have left would only show an error.
  async function handleLeave() {
    await leaveGroup.mutateAsync()
    navigate('/', { replace: true })
  }

  // The backend decides this too, and refuses either way; here it only decides
  // whether the buttons are worth drawing.
  const iAmAdmin = group.members.some((member) => member.id === me?.id && member.role === 'ADMIN')

  return (
    // Exactly one screen tall: the header and the input box keep their size and
    // the middle takes what is left, so only the messages and the member list
    // scroll.
    <div className="flex h-screen flex-col bg-slate-100">
      <GroupChatHeader
        group={group}
        me={me}
        membersOpen={membersOpen}
        onToggleMembers={() => setMembersOpen((open) => !open)}
      />

      <div className="flex min-h-0 flex-1">
        <div className="flex min-w-0 flex-1 flex-col">
          <MessageList
            messages={history.messages}
            myId={me?.id}
            otherUsername={group.name}
            // In a group the side a bubble sits on only says whether it is
            // yours, so everyone else's needs a name above it.
            showSenders
            loading={history.loading}
            error={history.error ? errorMessage(history.error, 'Could not load these messages.') : undefined}
            hasOlder={history.hasOlder}
            loadingOlder={history.loadingOlder}
            onLoadOlder={history.loadOlder}
          />

          <MessageComposer
            recipient={group.name}
            sending={sendMessage.isPending}
            error={sendMessage.error ? errorMessage(sendMessage.error, 'That message did not send.') : undefined}
            onSend={sendMessage.mutateAsync}
          />
        </div>

        {membersOpen && (
          <GroupMembers
            members={group.members}
            myId={me?.id}
            canManage={iAmAdmin}
            onAdd={addMember.mutateAsync}
            addingId={addMember.isPending ? addMember.variables : undefined}
            addError={addMember.error ? errorMessage(addMember.error, 'Could not add them.') : undefined}
            onRemove={removeMember.mutateAsync}
            removingId={removeMember.isPending ? removeMember.variables : undefined}
            removeError={
              removeMember.error ? errorMessage(removeMember.error, 'Could not remove them.') : undefined
            }
            onPromote={transferAdmin.mutateAsync}
            promotingId={transferAdmin.isPending ? transferAdmin.variables : undefined}
            promoteError={
              transferAdmin.error
                ? errorMessage(transferAdmin.error, 'Could not hand over the admin role.')
                : undefined
            }
            onLeave={handleLeave}
            leaving={leaveGroup.isPending}
            leaveError={leaveGroup.error ? errorMessage(leaveGroup.error, 'Could not leave the group.') : undefined}
          />
        )}
      </div>
    </div>
  )
}

function Centered({ children }: { children: ReactNode }) {
  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-100 p-6">
      <p className="text-sm text-slate-500">{children}</p>
    </div>
  )
}

export default GroupChatPage
