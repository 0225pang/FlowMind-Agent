<template>
  <div class="workspace">
    <div class="workspace-body">
      <SessionList
        :sessions="sessions"
        :active-id="currentSessionId"
        @select="switchSession"
        @new="createNewSession"
        @delete="deleteSession"
      />
      <AgentChat
        :key="currentSessionId || '_new_auto'"
        ref="chat"
        agent-type="auto"
        :session-id="currentSessionId"
        @session-created="onSessionCreated"
        @message-sent="onMessageSent"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import AgentChat from '@/components/AgentChat.vue'
import SessionList from '@/components/SessionList.vue'
import { createNewSession as createNewSessionApi, listSessions, deleteSession as apiDeleteSession } from '@/api/agent'

const currentSessionId = ref('')
const sessions = ref<Array<{ id: string; title: string; time: string; agentType: string }>>([])

onMounted(async () => {
  await loadSessions()
  // Auto-activate the most recent session, or create one so first message isn't swallowed
  if (sessions.value.length > 0) {
    currentSessionId.value = sessions.value[0].id
  } else {
    await createNewSession()
  }
})

async function loadSessions() {
  try {
    const list = await listSessions()
    sessions.value = list.map(s => ({
      id: s.id,
      title: s.title || '新对话',
      time: s.updatedAt || s.createdAt || new Date().toISOString(),
      agentType: s.agentType || 'auto'
    }))
  } catch {
    sessions.value = []
  }
}

async function createNewSession() {
  const sid = await createNewSessionApi()
  currentSessionId.value = sid
  await loadSessions()
}

function switchSession(id: string) {
  currentSessionId.value = id
}

async function deleteSession(id: string) {
  await apiDeleteSession('auto', id).catch(() => {})
  sessions.value = sessions.value.filter(s => s.id !== id)
  if (currentSessionId.value === id) {
    currentSessionId.value = sessions.value[0]?.id || ''
  }
  await loadSessions()
}

function onSessionCreated(id: string) {
  currentSessionId.value = id
  loadSessions()
}

function onMessageSent() {
  loadSessions()
}
</script>

<style scoped>
.workspace{display:flex;flex-direction:column;gap:0;height:calc(100vh - 144px)}
.workspace-body{flex:1;display:grid;grid-template-columns:200px minmax(0,1fr);gap:12px;min-height:0}
@media(max-width:1024px){.workspace-body{grid-template-columns:1fr;gap:10px}}
</style>
