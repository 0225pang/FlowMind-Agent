<template>
  <div class="agent-tabs">
    <div class="master">
      <span class="master-dot" />
      <div>
        <strong>FlowMind 总智能体</strong>
        <p>已启用 5 个专业 Agent，输入后自动判断调用内容、知识库、学员、院校或飞书能力。</p>
      </div>
    </div>
    <div class="chips">
      <span v-for="ag in agents" :key="ag.type" class="chip">
        <span class="dot" :style="{ background: ag.color }" />
        {{ ag.name }}
      </span>
    </div>
  </div>
</template>

<script setup lang="ts">
const agents = [
  { type: 'content', name: 'ContentAgent', color: '#5b6cff' },
  { type: 'knowledge', name: 'KnowledgeAgent', color: '#19b37b' },
  { type: 'student', name: 'StudentAgent', color: '#f59e0b' },
  { type: 'school', name: 'SchoolAgent', color: '#ef4444' },
  { type: 'feishu', name: 'FeishuAgent', color: '#8b5cf6' }
]
</script>

<style scoped>
.agent-tabs {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 14px;
  border: 1px solid rgba(91, 108, 255, 0.14);
  border-radius: 8px;
  background: linear-gradient(135deg, rgba(91, 108, 255, 0.08), rgba(25, 179, 123, 0.06)), #fff;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.04);
}
.master{display:flex;align-items:center;gap:12px;min-width:0}
.master-dot{width:12px;height:12px;border-radius:50%;background:linear-gradient(135deg,#5b6cff,#19b37b);box-shadow:0 0 0 6px rgba(91,108,255,.10);flex-shrink:0}
.master strong{display:block;color:#101828;font-size:15px}
.master p{margin:2px 0 0;color:#667085;font-size:12px}
.chips{display:flex;gap:8px;flex-wrap:wrap;justify-content:flex-end}
.chip{height:30px;display:inline-flex;align-items:center;gap:6px;border:1px solid #dfe5f2;border-radius:999px;background:#fff;padding:0 10px;color:#475467;font-size:12px;white-space:nowrap}
.dot{width:8px;height:8px;border-radius:50%;flex-shrink:0}
@media(max-width:1024px){.agent-tabs{align-items:flex-start;flex-direction:column}.chips{justify-content:flex-start}}
</style>
