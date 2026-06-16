<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2 class="page-title">知识库</h2>
        <p class="subtle">从飞书「{{ folderName }}」同步文档，在此查看和添加标签。</p>
      </div>
      <div class="head-actions">
        <el-input
          v-model="keyword"
          class="search-input"
          placeholder="搜索标题或摘要..."
          clearable
          :prefix-icon="Search"
          @keyup.enter="loadDocs"
          @clear="loadDocs"
        />
        <el-button :icon="Refresh" :loading="syncing" @click="triggerSync">
          {{ syncing ? '同步中...' : '同步飞书' }}
        </el-button>
      </div>
    </div>

    <div class="grid four">
      <StatCard label="文档" :value="docs.length" :trend="`${docTypes} 种类型`" icon="Collection" color="#5b6cff" />
      <StatCard label="标签" :value="allTags.length" trend="可点击筛选" icon="PriceTag" color="#19b37b" />
      <StatCard label="已同步" :value="docs.length" :trend="lastSyncText" icon="DocumentChecked" color="#8b5cf6" />
      <StatCard label="来源" :value="'飞书'" :trend="folderName" icon="Aim" color="#f59e0b" />
    </div>

    <!-- Tag filter chips -->
    <div v-if="allTags.length" class="tag-filter">
      <el-tag
        v-for="tag in allTags"
        :key="tag"
        :type="filterTag === tag ? 'primary' : 'info'"
        size="small"
        class="tag-chip"
        @click="filterTag = filterTag === tag ? '' : tag"
      >{{ tag }}</el-tag>
      <el-button v-if="filterTag" text size="small" @click="filterTag = ''">清除筛选</el-button>
    </div>

    <div v-if="filteredDocs.length === 0 && !loading" class="empty-state">
      <el-empty :description="docs.length === 0 ? '尚未同步飞书文档，点击上方按钮同步' : '没有匹配的文档'">
        <el-button v-if="docs.length === 0" type="primary" :loading="syncing" @click="triggerSync">立即同步</el-button>
      </el-empty>
    </div>

    <div v-else class="grid three">
      <div v-for="doc in filteredDocs" :key="doc.id" class="doc-card surface hoverable" @click="openDetail(doc)">
        <div class="doc-card-type">
          <el-tag :type="typeTag(doc.feishuType)" size="small" effect="plain">
            {{ typeLabel(doc.feishuType) }}
          </el-tag>
        </div>
        <h3>{{ doc.title }}</h3>
        <p class="doc-summary">{{ doc.summary || '点击同步飞书后会自动生成 AI 摘要。' }}</p>
        <div class="doc-tags" v-if="doc.tags?.length">
          <el-tag v-for="tag in doc.tags" :key="tag" size="small" type="info">{{ tag }}</el-tag>
        </div>
        <div class="doc-meta">
          <span>{{ formatDate(doc.updatedAt) }}</span>
        </div>
      </div>
    </div>

    <!-- Detail Dialog — read-only content + editable tags -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingDoc?.title || '文档详情'"
      width="860px"
      :close-on-click-modal="false"
      destroy-on-close
      top="4vh"
    >
      <div v-if="editingDoc" class="detail">
        <div class="detail-meta">
          <span class="detail-type">
            <el-tag :type="typeTag(editingDoc.feishuType)" effect="plain" size="small">
              {{ typeLabel(editingDoc.feishuType) }}
            </el-tag>
          </span>
          <span class="detail-date">同步于 {{ formatFullDate(editingDoc.updatedAt) }}</span>
          <a v-if="editingDoc.feishuUrl" :href="editingDoc.feishuUrl" target="_blank" class="feishu-link">
            在飞书中打开 →
          </a>
        </div>
        <div class="detail-summary" v-if="editingDoc.summary">
          <strong>AI 摘要：</strong>{{ editingDoc.summary }}
        </div>
        <div class="detail-content" v-html="renderedContent" />
        <div class="detail-tags">
          <span class="tags-label">标签：</span>
          <el-tag
            v-for="(tag, idx) in editTags"
            :key="idx"
            closable
            size="small"
            @close="removeTag(idx)"
          >{{ tag }}</el-tag>
          <el-input
            v-if="showTagInput"
            ref="tagInputRef"
            v-model="newTag"
            size="small"
            style="width: 100px"
            @keyup.enter="addTag"
            @blur="addTag"
          />
          <el-button v-else size="small" @click="startAddTag">+ 添加</el-button>
        </div>
      </div>
      <template #footer>
        <el-button @click="dialogVisible = false">关闭</el-button>
        <el-button type="primary" :loading="saving" @click="saveTags">保存标签</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { Search, Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import StatCard from '@/components/StatCard.vue'
import { renderHtml } from '@/utils/markdown'
import { knowledgeApi, type KnowledgeDoc } from '@/api/knowledge'
import { fmtBeijing, relativeTime } from '@/utils/datetime'

const docs = ref<KnowledgeDoc[]>([])
const loading = ref(false)
const syncing = ref(false)
const saving = ref(false)
const keyword = ref('')
const filterTag = ref('')
const folderName = '保研知识库'
const lastSyncAt = ref('')

// Dialog
const dialogVisible = ref(false)
const editingDoc = ref<KnowledgeDoc | null>(null)
const editTags = ref<string[]>([])
const showTagInput = ref(false)
const newTag = ref('')
const tagInputRef = ref<InstanceType<typeof import('element-plus').ElInput>>()

const allTags = computed(() => {
  const set = new Set<string>()
  docs.value.forEach(d => d.tags?.forEach(t => set.add(t)))
  return [...set]
})

const docTypes = computed(() => {
  const set = new Set(docs.value.map(d => d.feishuType).filter(Boolean))
  return set.size
})

const filteredDocs = computed(() => {
  if (!filterTag.value) return docs.value
  return docs.value.filter(d => d.tags?.includes(filterTag.value))
})

const lastSyncText = computed(() => {
  if (lastSyncAt.value) return formatDate(lastSyncAt.value)
  return '待同步'
})

const renderedContent = computed(() => {
  if (!editingDoc.value?.content) return '<p class="empty-content">该文档暂无可预览内容（可能为 PDF 或图片），请在飞书中查看。</p>'
  return renderHtml(editingDoc.value.content)
})

onMounted(() => loadDocs())

async function loadDocs() {
  loading.value = true
  try {
    docs.value = await knowledgeApi.searchDocs(keyword.value || undefined)
  } catch {
    docs.value = []
  } finally {
    loading.value = false
  }
}

async function triggerSync() {
  syncing.value = true
  try {
    const result = await knowledgeApi.syncFromFeishu()
    lastSyncAt.value = new Date().toISOString()
    ElMessage.success(result.message || '同步完成')
    await loadDocs()
  } catch (err: any) {
    ElMessage.error('同步失败: ' + (err.message || 'unknown'))
  } finally {
    syncing.value = false
  }
}

function openDetail(doc: KnowledgeDoc) {
  editingDoc.value = doc
  editTags.value = doc.tags ? [...doc.tags] : []
  dialogVisible.value = true
}

async function saveTags() {
  if (!editingDoc.value) return
  saving.value = true
  try {
    const updated = await knowledgeApi.updateTags(editingDoc.value.id, editTags.value)
    const idx = docs.value.findIndex(d => d.id === updated.id)
    if (idx >= 0) docs.value[idx] = { ...docs.value[idx], tags: editTags.value }
    ElMessage.success('标签已保存')
    dialogVisible.value = false
  } catch (err: any) {
    ElMessage.error('保存失败: ' + (err.message || 'unknown'))
  } finally {
    saving.value = false
  }
}

function startAddTag() {
  showTagInput.value = true
  nextTick(() => tagInputRef.value?.focus?.())
}

function addTag() {
  const tag = newTag.value.trim()
  if (tag && !editTags.value.includes(tag)) editTags.value.push(tag)
  newTag.value = ''
  showTagInput.value = false
}

function removeTag(idx: number) {
  editTags.value.splice(idx, 1)
}

function typeLabel(type: string) {
  const map: Record<string, string> = { docx: '飞书文档', doc: '文档', sheet: '电子表格', bitable: '多维表格', pdf: 'PDF', file: '文件', folder: '文件夹' }
  return map[type] || type || 'file'
}

function typeTag(type: string): 'primary' | 'success' | 'warning' | 'info' | 'danger' {
  const map: Record<string, string> = { docx: 'primary', doc: 'primary', sheet: 'success', bitable: 'success', pdf: 'danger', folder: 'warning' }
  return (map[type] || 'info') as 'primary' | 'success' | 'warning' | 'info' | 'danger'
}

function formatDate(ts: string) {
  if (!ts) return ''
  return relativeTime(ts)
}

function formatFullDate(ts: string) {
  if (!ts) return ''
  return fmtBeijing(ts)
}
</script>

<style scoped>
.head-actions { display: flex; gap: 10px; align-items: center; }
.search-input { width: 260px; }

.tag-filter { display: flex; flex-wrap: wrap; gap: 6px; align-items: center; margin-bottom: 8px; }
.tag-chip { cursor: pointer; }

.empty-state { padding: 60px 0; text-align: center; }

.doc-card { padding: 16px; cursor: pointer; display: flex; flex-direction: column; gap: 8px; }
.doc-card-type { display: flex; justify-content: space-between; align-items: center; }
.doc-card h3 { font-size: 15px; margin: 0; color: #101828; line-height: 1.4; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.doc-summary { margin: 0; color: #667085; font-size: 13px; line-height: 1.6; min-height: 38px; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.doc-tags { display: flex; flex-wrap: wrap; gap: 6px; }
.doc-meta { font-size: 12px; color: #9ca3af; }

/* Detail dialog */
.detail { display: flex; flex-direction: column; gap: 14px; max-height: 70vh; overflow-y: auto; }
.detail-meta { display: flex; gap: 14px; align-items: center; color: #667085; font-size: 13px; }
.detail-date { color: #9ca3af; }
.feishu-link { color: #5b6cff; text-decoration: none; margin-left: auto; }
.feishu-link:hover { text-decoration: underline; }

.detail-summary { padding: 10px 14px; background: #f4f6ff; border-radius: 8px; color: #344054; font-size: 13px; line-height: 1.6; border-left: 4px solid #5b6cff; }
.detail-summary strong { color: #101828; }

.detail-content { line-height: 1.8; font-size: 14px; color: #344054; padding: 4px 0; }
.detail-content :deep(p) { margin: 0 0 10px; }
.detail-content :deep(h2), .detail-content :deep(h3), .detail-content :deep(h4) { margin: 14px 0 8px; color: #101828; font-size: 15px; }
.detail-content :deep(ul), .detail-content :deep(ol) { margin: 6px 0 10px; padding-left: 22px; }
.detail-content :deep(li) { margin: 4px 0; }
.detail-content :deep(table) { width: 100%; border-collapse: collapse; margin: 10px 0; font-size: 13px; }
.detail-content :deep(th), .detail-content :deep(td) { border: 1px solid #dfe5f2; padding: 8px 12px; text-align: left; }
.detail-content :deep(th) { background: #f4f6ff; color: #101828; font-weight: 700; }
.detail-content :deep(blockquote) { margin: 10px 0; border-left: 3px solid #5b6cff; padding: 6px 14px; background: #f4f6ff; color: #475467; }
.detail-content :deep(pre) { margin: 10px 0; border-radius: 8px; padding: 14px 16px; background: #1e293b; color: #e2e8f0; overflow-x: auto; font-size: 12px; }
.detail-content :deep(hr) { margin: 16px 0; border: 0; border-top: 1px solid #e7ebf3; }
.detail-content :deep(strong) { color: #101828; font-weight: 700; }
.detail-content :deep(a) { color: #5b6cff; }
.empty-content { color: #9ca3af; font-style: italic; }

.detail-tags { display: flex; flex-wrap: wrap; gap: 6px; align-items: center; padding-top: 6px; border-top: 1px solid #eef2f7; }
.tags-label { font-size: 13px; color: #667085; margin-right: 4px; }
</style>
