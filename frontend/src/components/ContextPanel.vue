<template>
  <div class="context surface">
    <h3>自动路由</h3>
    <div class="route-list">
      <div v-for="item in routes" :key="item.name" class="route-item">
        <span class="dot" :style="{ background: item.color }" />
        <div>
          <strong>{{ item.name }}</strong>
          <p>{{ item.desc }}</p>
        </div>
      </div>
    </div>

    <h3>快捷动作</h3>
    <div class="actions">
      <el-button v-for="a in actions" :key="a.text" @click="$emit('quick', a.prompt)">
        {{ a.text }}
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  agentType?: string
}>()

defineEmits<{
  (e: 'quick', text: string): void
}>()

const routes = [
  { name: 'ContentAgent', desc: '小红书、朋友圈、公众号、选题、文案', color: '#5b6cff' },
  { name: 'KnowledgeAgent', desc: '资料整理、摘要、标签、知识库检索', color: '#19b37b' },
  { name: 'StudentAgent', desc: '学员画像、GPA、风险、申请进度', color: '#f59e0b' },
  { name: 'SchoolAgent', desc: '院校项目、夏令营、预推免、匹配推荐', color: '#ef4444' },
  { name: 'FeishuAgent', desc: '飞书文档、多维表格、机器人、同步', color: '#8b5cf6' }
]

const actions = [
  { text: '创建飞书文档', prompt: '创建一个飞书文档，里面写上我喜欢刘昌乐' },
  { text: '生成选题', prompt: '生成 10 个保研小红书选题，并给出爆款结构' },
  { text: '整理资料', prompt: '总结知识库里的夏令营资料，并提取标签' },
  { text: '分析学员', prompt: '分析学员01的申请风险并给出下一步动作' },
  { text: '推荐院校', prompt: '推荐适合经管学生的夏令营项目' }
]
</script>

<style scoped>
.context{padding:16px;display:flex;flex-direction:column;gap:14px;overflow:auto}
.context h3{font-size:15px;margin:0;color:#101828}
.route-list{display:flex;flex-direction:column;gap:10px}
.route-item{display:grid;grid-template-columns:auto 1fr;gap:8px;align-items:start;padding:10px;border:1px solid #e7ebf3;border-radius:8px;background:#fff}
.dot{width:9px;height:9px;border-radius:50%;margin-top:5px}
.route-item strong{display:block;font-size:13px;color:#101828}
.route-item p{margin:2px 0 0;color:#667085;font-size:12px;line-height:1.45}
.actions{display:flex;flex-direction:column;gap:8px}
.actions .el-button{margin:0;justify-content:flex-start}
</style>
