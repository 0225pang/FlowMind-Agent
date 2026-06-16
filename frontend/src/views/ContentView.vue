<template>
  <div class="page content-page">
    <div class="page-head">
      <div>
        <h2 class="page-title">内容运营</h2>
        <p class="subtle">管理主题库、文案库、历史使用记录和内容日历；小红书和朋友圈生成 SOP 已统一放在 AI 工作台。</p>
      </div>
      <div class="toolbar">
        <el-input v-model="keyword" clearable placeholder="搜索主题 / 文案 / 标签" :prefix-icon="Search" />
        <el-select v-model="statusFilter" clearable placeholder="主题状态" style="width: 132px">
          <el-option v-for="item in statusOptions" :key="item" :label="item" :value="item" />
        </el-select>
        <el-select v-model="channelFilter" clearable placeholder="渠道" style="width: 116px">
          <el-option v-for="item in channelOptions" :key="item" :label="item" :value="item" />
        </el-select>
      </div>
    </div>

    <div class="grid four">
      <StatCard label="主题总数" :value="themes.length" trend="3x2 分页展示" icon="Collection" color="#5b6cff" />
      <StatCard label="文案总数" :value="drafts.length" trend="支持图片和建议" icon="Document" color="#8b5cf6" />
      <StatCard label="已评分" :value="ratedCount" trend="星级越高越重要" icon="Star" color="#f59e0b" />
      <StatCard label="日历排期" :value="calendarItems.length" trend="点击日期展开" icon="Calendar" color="#19b37b" />
    </div>

    <section class="surface block">
      <div class="block-head">
        <div>
          <h3>主题库</h3>
          <span>点击主题卡片查看该主题下生成过的历史文案、使用日期、图片和编辑入口。</span>
        </div>
        <div class="block-head-actions">
          <el-button type="primary" :icon="Plus" @click="openCreateTheme">新增主题</el-button>
          <el-button :icon="Plus" @click="openAgentHint">去 AI 工作台生成</el-button>
        </div>
      </div>

      <div class="theme-grid">
        <article v-for="theme in pagedThemes" :key="theme.id" class="theme-card hoverable" @click="openTheme(theme)">
          <div class="theme-top">
            <el-tag size="small">{{ theme.platform }}</el-tag>
            <el-tag size="small" :type="statusType(theme.status)">{{ theme.status }}</el-tag>
            <el-button class="delete-btn" :icon="Delete" size="small" type="danger" plain @click.stop="removeTheme(theme)">
              删除
            </el-button>
          </div>
          <strong>{{ theme.title }}</strong>
          <StarRating :model-value="theme.rating ?? 0" show-label @update:model-value="value => handleRateTheme(theme, value)" @click.stop />
          <p>{{ theme.summary }}</p>
          <div class="theme-meta">
            <span>热度 {{ theme.heat }}</span>
            <span>计划 {{ theme.plannedDate }}</span>
            <span>{{ theme.drafts.length }} 条文案</span>
          </div>
          <div class="tag-row">
            <el-tag v-for="tag in theme.tags" :key="tag" size="small" type="info">{{ tag }}</el-tag>
          </div>
        </article>
      </div>

      <div class="pager-row">
        <span>共 {{ filteredThemes.length }} 个主题</span>
        <el-pagination
          v-model:current-page="themePage"
          background
          layout="prev, pager, next"
          :page-size="themePageSize"
          :total="filteredThemes.length"
        />
      </div>
    </section>

    <section class="grid two content-main">
      <div class="surface block copy-block">
        <div class="block-head">
          <div>
            <h3>文案库</h3>
            <span>展示所有历史文案，支持分页、编辑、删除、评分、查看使用日期和上传图片。</span>
          </div>
          <div class="block-head-actions">
            <el-button :icon="Plus" type="primary" @click="openCreateCopy">新增文案</el-button>
            <el-segmented v-model="usageFilter" :options="usageSegmentOptions" />
          </div>
        </div>

        <div class="copy-list">
          <article v-for="draft in pagedDrafts" :key="draft.id" class="copy-card">
            <div class="copy-main">
              <div class="copy-title-row">
                <strong>{{ draft.title }}</strong>
                <el-tag :type="usageType(draft.usageStatus)">{{ draft.usageStatus }}</el-tag>
              </div>
              <StarRating :model-value="draft.rating ?? 0" show-label @update:model-value="value => handleRateCopy(draft, value)" />
              <p>{{ draft.content }}</p>
              <div class="copy-meta">
                <span>{{ draft.channel }} / {{ draft.version }}</span>
                <span>{{ draft.usedDate ? `使用于 ${draft.usedDate}` : `生成于 ${draft.generatedAt}` }}</span>
                <span>{{ draft.owner }}</span>
              </div>
              <div class="asset-row">
                <template v-if="draft.images.length">
                  <img v-for="image in draft.images.slice(0, 3)" :key="image.id" :src="image.url" :alt="image.name" />
                  <span>已配图 {{ draft.images.length }} 张</span>
                </template>
                <template v-else>
                  <el-icon><Picture /></el-icon>
                  <span>配图建议：{{ draft.imageSuggestion }}</span>
                </template>
              </div>
            </div>
            <div class="copy-actions">
              <el-button :icon="View" plain @click="openDraftTheme(draft)">历史</el-button>
              <el-button :icon="EditPen" type="primary" plain @click="editDraft(draft)">编辑</el-button>
              <el-button :icon="Delete" type="danger" plain @click="removeCopy(draft)">删除</el-button>
            </div>
          </article>
        </div>

        <div class="pager-row">
          <span>共 {{ filteredDrafts.length }} 条文案</span>
          <el-pagination
            v-model:current-page="copyPage"
            background
            layout="prev, pager, next"
            :page-size="copyPageSize"
            :total="filteredDrafts.length"
          />
        </div>
      </div>

      <div class="surface block calendar-block">
        <div class="block-head">
          <div>
            <h3>内容日历</h3>
            <span>有内容发布或排期的日期会出现标记，点击日期查看当天内容列表。</span>
          </div>
        </div>

        <el-calendar v-model="calendarDate" class="content-calendar">
          <template #date-cell="{ data }">
            <button class="calendar-cell" :class="{ active: data.day === selectedDate }" type="button" @click.stop="selectDate(data.day)">
              <span>{{ Number(data.day.slice(-2)) }}</span>
              <i v-if="itemsByDate(data.day).length" />
              <small v-if="itemsByDate(data.day).length">{{ itemsByDate(data.day).length }}</small>
            </button>
          </template>
        </el-calendar>

        <div class="selected-day">
          <div class="selected-head">
            <strong>{{ selectedDate }} 内容列表</strong>
            <el-tag v-if="selectedDayItems.length" type="primary">{{ selectedDayItems.length }} 条</el-tag>
            <el-tag v-else type="info">无排期</el-tag>
          </div>
          <div v-if="selectedDayItems.length" class="day-list">
            <button v-for="item in selectedDayItems" :key="item.id" class="day-item" type="button" @click="openThemeById(item.themeId)">
              <span>{{ item.channel }}</span>
              <strong>{{ item.title }}</strong>
              <el-tag size="small" :type="statusType(item.status)">{{ item.status }}</el-tag>
            </button>
          </div>
          <el-empty v-else description="当天暂无发布内容" :image-size="76" />
        </div>
      </div>
    </section>

    <el-drawer v-model="drawerVisible" :title="selectedTheme?.title" size="54%">
      <template v-if="selectedTheme">
        <div class="drawer-summary">
          <el-tag>{{ selectedTheme.platform }}</el-tag>
          <el-tag :type="statusType(selectedTheme.status)">{{ selectedTheme.status }}</el-tag>
          <span>计划日期：{{ selectedTheme.plannedDate }}</span>
          <span>主题：{{ selectedTheme.topic }}</span>
        </div>
        <p class="drawer-desc">{{ selectedTheme.summary }}</p>

        <div v-for="draft in selectedTheme.drafts" :key="draft.id" class="history-card">
          <div class="history-head">
            <div>
              <strong>{{ draft.title }}</strong>
              <span>{{ draft.channel }} / {{ draft.version }} / {{ draft.style }}</span>
            </div>
            <div class="history-actions">
              <StarRating :model-value="draft.rating ?? 0" @update:model-value="value => handleRateCopy(draft, value)" />
              <el-button type="primary" plain :icon="EditPen" @click="editDraft(draft)">编辑</el-button>
            </div>
          </div>
          <p>{{ draft.content }}</p>

          <div class="image-panel">
            <div v-if="draft.images.length" class="image-grid">
              <img v-for="image in draft.images" :key="image.id" :src="image.url" :alt="image.name" />
            </div>
            <div v-else class="suggestion">
              <el-icon><Picture /></el-icon>
              <span>配图建议：{{ draft.imageSuggestion }}</span>
            </div>
            <el-upload
              class="upload-inline"
              :auto-upload="false"
              :show-file-list="false"
              accept="image/*"
              :on-change="uploadHandler(draft)"
            >
              <el-button :icon="Upload">上传图片</el-button>
            </el-upload>
          </div>

          <div class="history-meta">
            <el-tag :type="usageType(draft.usageStatus)">{{ draft.usageStatus }}</el-tag>
            <span v-if="draft.usedDate">使用日期：{{ draft.usedDate }}</span>
            <span>生成：{{ draft.generatedAt }}</span>
            <span>负责人：{{ draft.owner }}</span>
          </div>
          <div class="feedback">{{ draft.feedback || '暂无效果反馈' }}</div>
        </div>
      </template>
    </el-drawer>

    <el-dialog v-model="editVisible" title="编辑历史文案" width="720px">
      <el-form v-if="editingDraft" label-position="top">
        <el-form-item label="标题">
          <el-input v-model="editingDraft.title" />
        </el-form-item>
        <el-form-item label="正文">
          <el-input v-model="editingDraft.content" type="textarea" :rows="7" />
        </el-form-item>
        <div class="dialog-grid">
          <el-form-item label="使用状态">
            <el-select v-model="editingDraft.usageStatus">
              <el-option v-for="item in usageOptions" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
          <el-form-item label="使用日期">
            <el-date-picker v-model="editingDraft.usedDate" value-format="YYYY-MM-DD" type="date" placeholder="选择日期" />
          </el-form-item>
        </div>
        <el-form-item label="配图建议">
          <el-input v-model="editingDraft.imageSuggestion" />
        </el-form-item>
        <el-form-item label="效果反馈">
          <el-input v-model="editingDraft.feedback" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="saveDraft">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="createThemeVisible" title="新增主题" width="620px">
      <el-form label-position="top">
        <el-form-item label="主题标题" required>
          <el-input v-model="newTheme.title" placeholder="如：保研面试高频问题汇总" />
        </el-form-item>
        <el-form-item label="主题关键词">
          <el-input v-model="newTheme.topic" placeholder="如：保研面试" />
        </el-form-item>
        <div class="dialog-grid">
          <el-form-item label="平台" required>
            <el-select v-model="newTheme.platform">
              <el-option v-for="item in channelOptions" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
          <el-form-item label="类型" required>
            <el-select v-model="newTheme.type">
              <el-option v-for="item in themeTypeOptions" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
        </div>
        <div class="dialog-grid">
          <el-form-item label="状态">
            <el-select v-model="newTheme.status">
              <el-option v-for="item in statusOptions" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
          <el-form-item label="热度">
            <el-input-number v-model="newTheme.heat" :min="0" :max="100" />
          </el-form-item>
        </div>
        <div class="dialog-grid">
          <el-form-item label="计划日期">
            <el-date-picker v-model="newTheme.plannedDate" value-format="YYYY-MM-DD" type="date" placeholder="选择日期" />
          </el-form-item>
          <el-form-item label="标签（逗号分隔）">
            <el-input v-model="newTheme.tagsStr" placeholder="如：面试, 自我介绍, 模板" />
          </el-form-item>
        </div>
        <el-form-item label="主题摘要">
          <el-input v-model="newTheme.summary" type="textarea" :rows="3" placeholder="简要描述这个主题的内容方向和目标" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createThemeVisible = false">取消</el-button>
        <el-button type="primary" @click="saveNewTheme">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="createCopyVisible" title="新增文案" width="620px">
      <el-form label-position="top">
        <el-form-item label="所属主题" required>
          <el-select v-model="newCopy.themeId" filterable>
            <el-option v-for="theme in themes" :key="theme.id" :label="theme.title" :value="theme.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="文案标题" required>
          <el-input v-model="newCopy.title" placeholder="如：夏令营面试这样准备就对了" />
        </el-form-item>
        <div class="dialog-grid">
          <el-form-item label="渠道" required>
            <el-select v-model="newCopy.channel">
              <el-option v-for="item in channelOptions" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
          <el-form-item label="版本">
            <el-select v-model="newCopy.version">
              <el-option v-for="item in versionOptions" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item label="风格">
          <el-input v-model="newCopy.style" placeholder="如：干货、温柔、专业" />
        </el-form-item>
        <el-form-item label="正文" required>
          <el-input v-model="newCopy.content" type="textarea" :rows="6" placeholder="输入文案正文..." />
        </el-form-item>
        <el-form-item label="负责人">
          <el-input v-model="newCopy.owner" placeholder="如：内容运营" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createCopyVisible = false">取消</el-button>
        <el-button type="primary" @click="saveNewCopy">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox, type UploadFile } from 'element-plus'
import { Delete, EditPen, Picture, Plus, Search, Upload, View } from '@element-plus/icons-vue'
import StatCard from '@/components/StatCard.vue'
import StarRating from '@/components/StarRating.vue'
import {
  contentApi,
  contentCalendar,
  contentThemes,
  copyLibrary,
  type ContentCalendarItem,
  type ContentChannel,
  type ContentStatus,
  type ContentTheme,
  type CopyDraft,
  type CopyUsageStatus
} from '@/api/content'

const keyword = ref('')
const statusFilter = ref<ContentStatus | ''>('')
const channelFilter = ref<ContentChannel | ''>('')
const usageFilter = ref<'全部' | CopyUsageStatus>('全部')
const drawerVisible = ref(false)
const editVisible = ref(false)
const selectedTheme = ref<ContentTheme>()
const editingDraft = ref<CopyDraft>()
const themes = ref<ContentTheme[]>([...contentThemes])
const drafts = ref<CopyDraft[]>([...copyLibrary])
const calendarItems = ref<ContentCalendarItem[]>([...contentCalendar])

const themePage = ref(1)
const themePageSize = 6
const copyPage = ref(1)
const copyPageSize = 5
const calendarDate = ref(new Date('2026-06-15'))
const selectedDate = ref(formatDate(calendarDate.value))
const createThemeVisible = ref(false)
const createCopyVisible = ref(false)

const statusOptions: ContentStatus[] = ['待创作', '已生成', '待发布', '已发布']
const usageOptions: CopyUsageStatus[] = ['未使用', '已使用', '已归档']
const usageSegmentOptions = ['全部', ...usageOptions]
const channelOptions: ContentChannel[] = ['小红书', '朋友圈', '公众号']
const themeTypeOptions = ['爆款仿写', '经验干货', '人设表达', '模板', '成果型人设', '清单型', '标题库']
const versionOptions = ['干货版', '情绪增强版', '转化引导版', '专业理性版', '学姐温和版', '稍带传播版']

const newTheme = reactive({
  title: '',
  topic: '',
  platform: '小红书' as ContentChannel,
  type: '经验干货',
  status: '待创作' as ContentStatus,
  heat: 80,
  plannedDate: '',
  summary: '',
  tagsStr: ''
})

const newCopy = reactive({
  themeId: 0,
  title: '',
  channel: '小红书' as ContentChannel,
  version: '干货版',
  style: '干货',
  content: '',
  owner: '内容运营'
})

const filteredThemes = computed(() => {
  const key = keyword.value.trim()
  return themes.value.filter(theme => {
    const matchKeyword = !key || theme.title.includes(key) || theme.summary.includes(key) || theme.tags.some(tag => tag.includes(key))
    const matchStatus = !statusFilter.value || theme.status === statusFilter.value
    const matchChannel = !channelFilter.value || theme.platform === channelFilter.value
    return matchKeyword && matchStatus && matchChannel
  })
})

const pagedThemes = computed(() => {
  const start = (themePage.value - 1) * themePageSize
  return filteredThemes.value.slice(start, start + themePageSize)
})

const filteredDrafts = computed(() => {
  const key = keyword.value.trim()
  return drafts.value.filter(draft => {
    const matchKeyword = !key || draft.title.includes(key) || draft.content.includes(key) || draft.imageSuggestion.includes(key)
    const matchChannel = !channelFilter.value || draft.channel === channelFilter.value
    const matchUsage = usageFilter.value === '全部' || draft.usageStatus === usageFilter.value
    return matchKeyword && matchChannel && matchUsage
  })
})

const pagedDrafts = computed(() => {
  const start = (copyPage.value - 1) * copyPageSize
  return filteredDrafts.value.slice(start, start + copyPageSize)
})

const ratedCount = computed(() => themes.value.filter(item => (item.rating ?? 0) > 0).length + drafts.value.filter(item => (item.rating ?? 0) > 0).length)
const selectedDayItems = computed(() => itemsByDate(selectedDate.value))

watch([keyword, statusFilter, channelFilter], () => {
  themePage.value = 1
  copyPage.value = 1
})

watch(usageFilter, () => {
  copyPage.value = 1
})

watch(calendarDate, value => {
  selectedDate.value = formatDate(value)
})

onMounted(loadContentData)

async function loadContentData() {
  const [nextThemes, nextDrafts, nextCalendar] = await Promise.all([
    contentApi.getThemes(),
    contentApi.getDrafts(),
    contentApi.getCalendar()
  ])
  themes.value = [...nextThemes]
  drafts.value = [...nextDrafts]
  calendarItems.value = [...nextCalendar]
  if (selectedTheme.value) selectedTheme.value = themes.value.find(item => item.id === selectedTheme.value?.id)
}

function statusType(status: ContentStatus) {
  if (status === '已发布') return 'success'
  if (status === '待发布') return 'warning'
  if (status === '已生成') return 'primary'
  return 'info'
}

function usageType(status: CopyUsageStatus) {
  if (status === '已使用') return 'success'
  if (status === '已归档') return 'info'
  return 'warning'
}

function formatDate(date: Date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function itemsByDate(date: string) {
  return calendarItems.value.filter(item => item.date === date)
}

function selectDate(date: string) {
  selectedDate.value = date
  calendarDate.value = new Date(`${date}T00:00:00`)
}

function openTheme(theme: ContentTheme) {
  selectedTheme.value = theme
  drawerVisible.value = true
}

function openThemeById(themeId: number) {
  const theme = themes.value.find(item => item.id === themeId)
  if (theme) openTheme(theme)
}

function openDraftTheme(draft: CopyDraft) {
  openThemeById(draft.themeId)
}

function editDraft(draft: CopyDraft) {
  editingDraft.value = { ...draft, images: [...draft.images] }
  editVisible.value = true
}

async function saveDraft() {
  if (!editingDraft.value) return
  const savedDraft = await contentApi.updateDraft({ ...editingDraft.value, images: [...editingDraft.value.images] })
  replaceDraft(savedDraft)
  editVisible.value = false
  ElMessage.success('文案已更新')
}

function replaceDraft(nextDraft: CopyDraft) {
  const draftIndex = drafts.value.findIndex(item => item.id === nextDraft.id)
  if (draftIndex >= 0) drafts.value[draftIndex] = nextDraft
  else drafts.value.unshift(nextDraft)

  const theme = themes.value.find(item => item.id === nextDraft.themeId)
  if (theme) {
    const themeDraftIndex = theme.drafts.findIndex(item => item.id === nextDraft.id)
    if (themeDraftIndex >= 0) theme.drafts[themeDraftIndex] = nextDraft
    else theme.drafts.unshift(nextDraft)
    if (selectedTheme.value?.id === theme.id) selectedTheme.value = { ...theme }
  }
}

async function handleImageChange(file: UploadFile, draft: CopyDraft) {
  if (!file.raw) return
  const savedImage = await contentApi.uploadDraftImage(draft.id, {
    id: Date.now(),
    name: file.name,
    url: URL.createObjectURL(file.raw)
  })
  const nextDraft = { ...draft, images: [...draft.images, savedImage] }
  replaceDraft(nextDraft)
  ElMessage.success('图片已添加到历史文案')
}

function openCreateTheme() {
  Object.assign(newTheme, {
    title: '',
    topic: '',
    platform: '小红书',
    type: '经验干货',
    status: '待创作',
    heat: 80,
    plannedDate: '',
    summary: '',
    tagsStr: ''
  })
  createThemeVisible.value = true
}

async function saveNewTheme() {
  if (!newTheme.title.trim()) {
    ElMessage.warning('请输入主题标题')
    return
  }
  const created = await contentApi.createTheme({
    title: newTheme.title,
    topic: newTheme.topic || newTheme.title,
    platform: newTheme.platform,
    type: newTheme.type,
    status: newTheme.status,
    heat: newTheme.heat,
    plannedDate: newTheme.plannedDate || new Date().toISOString().slice(0, 10),
    summary: newTheme.summary || '手动创建的主题，等待补充文案和排期。',
    tags: newTheme.tagsStr.split(/[,，]/).map(item => item.trim()).filter(Boolean)
  })
  themes.value.unshift(created)
  createThemeVisible.value = false
  ElMessage.success('主题已创建')
}

async function removeTheme(theme: ContentTheme) {
  try {
    await ElMessageBox.confirm(`确定删除主题“${theme.title}”吗？该主题下的文案和日历排期也会一起删除。`, '确认删除', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  await contentApi.deleteTheme(theme.id)
  themes.value = themes.value.filter(item => item.id !== theme.id)
  drafts.value = drafts.value.filter(item => item.themeId !== theme.id)
  calendarItems.value = calendarItems.value.filter(item => item.themeId !== theme.id)
  if (selectedTheme.value?.id === theme.id) {
    selectedTheme.value = undefined
    drawerVisible.value = false
  }
  ElMessage.success('主题已删除')
}

function openCreateCopy() {
  Object.assign(newCopy, {
    themeId: selectedTheme.value?.id ?? themes.value[0]?.id ?? 0,
    title: '',
    channel: '小红书',
    version: '干货版',
    style: '干货',
    content: '',
    owner: '内容运营'
  })
  createCopyVisible.value = true
}

async function saveNewCopy() {
  if (!newCopy.themeId) {
    ElMessage.warning('请选择所属主题')
    return
  }
  if (!newCopy.title.trim()) {
    ElMessage.warning('请输入文案标题')
    return
  }
  if (!newCopy.content.trim()) {
    ElMessage.warning('请输入文案正文')
    return
  }
  const created = await contentApi.createCopy(newCopy.themeId, {
    title: newCopy.title,
    channel: newCopy.channel,
    version: newCopy.version,
    style: newCopy.style,
    content: newCopy.content,
    owner: newCopy.owner || '内容运营'
  })
  replaceDraft(created)
  createCopyVisible.value = false
  ElMessage.success('文案已创建')
}

async function removeCopy(draft: CopyDraft) {
  try {
    await ElMessageBox.confirm(`确定删除文案“${draft.title}”吗？`, '确认删除', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  await contentApi.deleteCopy(draft.id)
  drafts.value = drafts.value.filter(item => item.id !== draft.id)
  calendarItems.value = calendarItems.value.filter(item => item.draftId !== draft.id)
  const theme = themes.value.find(item => item.id === draft.themeId)
  if (theme) {
    theme.drafts = theme.drafts.filter(item => item.id !== draft.id)
    if (selectedTheme.value?.id === theme.id) selectedTheme.value = { ...theme }
  }
  ElMessage.success('文案已删除')
}

async function handleRateTheme(theme: ContentTheme, rating: number) {
  const updated = await contentApi.rateTheme(theme.id, rating)
  const index = themes.value.findIndex(item => item.id === theme.id)
  if (index >= 0) themes.value[index] = updated
  if (selectedTheme.value?.id === theme.id) selectedTheme.value = updated
  ElMessage.success(`主题已评为 ${rating} 星`)
}

async function handleRateCopy(draft: CopyDraft, rating: number) {
  const updated = await contentApi.rateCopy(draft.id, rating)
  replaceDraft(updated)
  ElMessage.success(`文案已评为 ${rating} 星`)
}

function uploadHandler(draft: CopyDraft) {
  return (file: UploadFile) => handleImageChange(file, draft)
}

function openAgentHint() {
  ElMessage.info('小红书和朋友圈生成入口已放在 AI 工作台的 ContentAgent 中')
}
</script>

<style scoped>
.content-page {
  --content-line: #e7ebf3;
  --content-muted: #667085;
}

.toolbar,
.block-head,
.block-head-actions,
.theme-top,
.tag-row,
.theme-meta,
.copy-meta,
.asset-row,
.drawer-summary,
.history-meta,
.history-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.toolbar {
  flex-wrap: wrap;
  justify-content: flex-end;
}

.toolbar .el-input {
  width: 260px;
}

.block {
  padding: 16px;
}

.block-head {
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 14px;
}

.block-head-actions,
.theme-meta,
.tag-row,
.copy-meta,
.asset-row,
.drawer-summary,
.history-meta {
  flex-wrap: wrap;
}

.block-head h3 {
  margin: 0 0 4px;
  font-size: 17px;
}

.block-head span,
.pager-row span,
.copy-meta,
.drawer-summary,
.drawer-desc,
.history-head span,
.history-meta {
  color: var(--content-muted);
  font-size: 13px;
}

.theme-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.theme-card {
  min-height: 224px;
  border: 1px solid var(--content-line);
  border-radius: 8px;
  padding: 15px;
  cursor: pointer;
  background: linear-gradient(180deg, #ffffff 0%, #fbfcff 100%);
}

.theme-card strong {
  display: block;
  min-height: 46px;
  margin-bottom: 6px;
  font-size: 16px;
  line-height: 1.45;
}

.theme-card p {
  min-height: 48px;
  margin: 10px 0;
  color: var(--content-muted);
  line-height: 1.65;
}

.theme-meta {
  margin-bottom: 10px;
  font-size: 12px;
  color: var(--content-muted);
}

.delete-btn {
  margin-left: auto !important;
}

.pager-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 14px;
}

.content-main {
  align-items: start;
}

.copy-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.copy-card {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  min-height: 152px;
  border: 1px solid var(--content-line);
  border-radius: 8px;
  padding: 12px;
  background: #fbfcff;
}

.copy-main {
  min-width: 0;
  flex: 1;
}

.copy-title-row,
.selected-head,
.day-item,
.history-head,
.image-panel {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.copy-title-row strong {
  line-height: 1.5;
}

.copy-card p {
  display: -webkit-box;
  overflow: hidden;
  margin: 8px 0;
  color: #344054;
  line-height: 1.65;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.asset-row {
  min-height: 36px;
  margin-top: 8px;
  color: var(--content-muted);
  font-size: 12px;
}

.asset-row img {
  width: 42px;
  height: 28px;
  border: 1px solid var(--content-line);
  border-radius: 6px;
  object-fit: cover;
}

.copy-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 78px;
}

.content-calendar {
  --el-calendar-cell-width: 62px;
  border: 1px solid var(--content-line);
  border-radius: 8px;
  overflow: hidden;
}

:deep(.content-calendar .el-calendar__header) {
  padding: 12px;
}

:deep(.content-calendar .el-calendar-table td) {
  border-color: #eef2f7;
}

:deep(.content-calendar .el-calendar-day) {
  height: 62px;
  padding: 0;
}

.calendar-cell {
  position: relative;
  width: 100%;
  height: 100%;
  border: 0;
  background: transparent;
  color: #344054;
  cursor: pointer;
}

.calendar-cell:hover,
.calendar-cell.active {
  background: #f3f6ff;
}

.calendar-cell span {
  position: absolute;
  top: 8px;
  left: 9px;
  font-weight: 650;
}

.calendar-cell i {
  position: absolute;
  bottom: 10px;
  left: 10px;
  width: 7px;
  height: 7px;
  border-radius: 999px;
  background: #5b6cff;
  box-shadow: 10px 0 0 #19b37b;
}

.calendar-cell small {
  position: absolute;
  right: 8px;
  bottom: 7px;
  min-width: 18px;
  height: 18px;
  border-radius: 999px;
  background: #5b6cff;
  color: #fff;
  font-size: 11px;
  line-height: 18px;
}

.selected-day {
  margin-top: 14px;
  border: 1px solid var(--content-line);
  border-radius: 8px;
  padding: 12px;
  background: #fbfcff;
}

.day-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 10px;
}

.day-item {
  width: 100%;
  min-height: 48px;
  border: 1px solid var(--content-line);
  border-radius: 8px;
  padding: 9px 10px;
  background: #fff;
  cursor: pointer;
  text-align: left;
}

.day-item span {
  min-width: 48px;
  color: #5b6cff;
  font-size: 12px;
}

.day-item strong {
  flex: 1;
  min-width: 0;
  line-height: 1.4;
}

.drawer-desc {
  line-height: 1.7;
}

.history-card {
  margin-top: 14px;
  border: 1px solid var(--content-line);
  border-radius: 8px;
  padding: 14px;
  background: #fff;
}

.history-head strong,
.history-head span {
  display: block;
}

.history-card p {
  color: #344054;
  line-height: 1.8;
}

.image-panel {
  border: 1px dashed #d8def0;
  border-radius: 8px;
  padding: 10px;
  background: #f8faff;
}

.image-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.image-grid img {
  width: 96px;
  height: 62px;
  border: 1px solid var(--content-line);
  border-radius: 8px;
  object-fit: cover;
}

.suggestion {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--content-muted);
  line-height: 1.5;
}

.upload-inline {
  flex: none;
}

.feedback {
  margin-top: 10px;
  border-radius: 8px;
  padding: 10px;
  background: #f6f8fc;
  color: var(--content-muted);
}

.dialog-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

@media (max-width: 1180px) {
  .theme-grid,
  .content-main {
    grid-template-columns: 1fr;
  }

  .toolbar {
    width: 100%;
    align-items: stretch;
    flex-direction: column;
  }

  .toolbar .el-input {
    width: 100%;
  }
}

@media (max-width: 720px) {
  .copy-card,
  .history-head,
  .image-panel,
  .pager-row,
  .block-head {
    flex-direction: column;
    align-items: stretch;
  }

  .copy-actions {
    flex-direction: row;
  }

  .dialog-grid {
    grid-template-columns: 1fr;
  }
}
</style>
