<template>
  <div class="sop-panel surface">
    <div class="panel-head">
      <h3>内容智能体 SOP</h3>
      <el-tag effect="plain">ContentAgent</el-tag>
    </div>
    <el-tabs v-model="active">
      <el-tab-pane label="小红书" name="xiaohongshu">
        <div class="agent-note">
          <strong>爆款仿写型</strong>
          <span>主题 → 爆款结构 → 模板 → 三版笔记 → 标题 → 入库</span>
        </div>
        <ol>
          <li v-for="step in xiaohongshuSteps" :key="step">{{ step }}</li>
        </ol>
      </el-tab-pane>
      <el-tab-pane label="朋友圈" name="moments">
        <div class="agent-note">
          <strong>人设表达型</strong>
          <span>场景 → 人设 → 结构 → 三种风格 → 去营销化 → 入库</span>
        </div>
        <ol>
          <li v-for="step in momentsSteps" :key="step">{{ step }}</li>
        </ol>
      </el-tab-pane>
    </el-tabs>
    <div class="data-flow">
      <strong>接口预留</strong>
      <span>飞书机器人 / 前端界面触发</span>
      <span>LLM API 生成内容</span>
      <span>向量数据库检索历史结构</span>
      <span>飞书文档 + 多维表格 + 本地库沉淀</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const active = ref('xiaohongshu')
const xiaohongshuSteps = [
  '检索同主题 Top 20 高赞笔记结构',
  '抽取标题类型、开头钩子、正文结构、结尾转化',
  '压缩成可复用结构模板',
  '生成干货版、情绪增强版、转化引导版',
  '生成 10 条标题并自动打标签',
  '写入飞书文档、多维表格和本地库'
]
const momentsSteps = [
  '识别成果型、过程型、思考型场景',
  '映射导师/学姐/规划师身份标签',
  '生成场景、动作、反馈、方法、收尾结构',
  '输出专业理性版、学姐温和版、稍带传播版',
  '删除营销感词汇并增强真实细节',
  '记录使用日期、转化效果和复用标签'
]
</script>

<style scoped>
.sop-panel{padding:16px}.panel-head{display:flex;align-items:center;justify-content:space-between;gap:10px;margin-bottom:10px}.panel-head h3{margin:0;font-size:15px}.agent-note{padding:10px;border:1px solid #e7ebf3;border-radius:8px;background:#fbfcff;margin-bottom:12px}.agent-note strong,.agent-note span{display:block}.agent-note span{color:#667085;font-size:12px;margin-top:4px}ol{margin:0;padding-left:18px;color:#344054;line-height:1.7}li{margin-bottom:6px}.data-flow{margin-top:14px;padding-top:12px;border-top:1px solid #e7ebf3;display:flex;flex-direction:column;gap:6px}.data-flow strong{font-size:13px}.data-flow span{font-size:12px;color:#667085}
</style>
