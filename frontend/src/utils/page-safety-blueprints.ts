export type PageKey =
  | 'agent'
  | 'knowledge'
  | 'content'
  | 'school'
  | 'student'
  | 'analytics'
  | 'feishu'
  | 'settings'

export type RoleKey =
  | 'CONTENT_OPERATOR'
  | 'EDU_CONSULTANT'
  | 'IP_OPERATOR'
  | 'TEAM_ADMIN'
  | 'STUDENT_USER'

export type GuardSeverity = 'info' | 'warning' | 'danger'

export type GuardStep = {
  key: string
  label: string
  description: string
  severity: GuardSeverity
  blocking: boolean
}

export type EmptyStateBlueprint = {
  title: string
  description: string
  actions: string[]
}

export type OperationBlueprint = {
  key: string
  label: string
  endpoint?: string
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  requiresNetwork: boolean
  requiresConfirm: boolean
  requiresFeishuAuth: boolean
  requiresVectorReady: boolean
  allowedRoles: RoleKey[]
  preflight: GuardStep[]
  successHints: string[]
  failureHints: string[]
}

export type PageSafetyBlueprint = {
  page: PageKey
  label: string
  route: string
  allowedRoles: RoleKey[]
  mainEndpoints: string[]
  mobileParity: string[]
  responsiveRules: string[]
  emptyState: EmptyStateBlueprint
  operations: OperationBlueprint[]
  diagnostics: GuardStep[]
}

const allRoles: RoleKey[] = ['CONTENT_OPERATOR', 'EDU_CONSULTANT', 'IP_OPERATOR', 'TEAM_ADMIN', 'STUDENT_USER']
const staffRoles: RoleKey[] = ['CONTENT_OPERATOR', 'EDU_CONSULTANT', 'IP_OPERATOR', 'TEAM_ADMIN']
const teacherRoles: RoleKey[] = ['EDU_CONSULTANT', 'TEAM_ADMIN']
const adminRoles: RoleKey[] = ['TEAM_ADMIN']

function step(key: string, label: string, description: string, severity: GuardSeverity = 'info', blocking = false): GuardStep {
  return { key, label, description, severity, blocking }
}

function op(config: OperationBlueprint): OperationBlueprint {
  return config
}

export const pageSafetyBlueprints: PageSafetyBlueprint[] = [
  {
    page: 'agent',
    label: 'AI Workspace',
    route: '/workspace',
    allowedRoles: allRoles,
    mainEndpoints: ['/api/agents/chat', '/api/agents/chat/stream', '/api/agents/sessions', '/api/knowledge/vector/search'],
    mobileParity: [
      'The chat composer supports the same prompt validation as the web client.',
      'Only tools actually invoked by backend should be shown in the tool panel.',
      'Thinking text and tool calls should remain collapsible after streaming ends.',
      'Vector search evidence should be displayed before general model knowledge.',
      'Markdown should be rendered as rich text instead of raw symbols.'
    ],
    responsiveRules: [
      'Chat area must use the full main viewport and avoid fixed card height.',
      'Tool panels collapse into bottom sheets on narrow screens.',
      'Streaming content should keep the latest token visible without locking scroll.',
      'Long tool output is clipped with an expand affordance.',
      'Composer keeps a stable height and scrolls internally for long prompts.'
    ],
    emptyState: {
      title: 'Start a conversation',
      description: 'Ask about content creation, Feishu documents, school projects, student analysis or knowledge search.',
      actions: ['Use a sample prompt', 'Check backend health', 'Open knowledge sync status']
    },
    diagnostics: [
      step('backend-health', 'Backend health', 'The backend tunnel or local service must be reachable.', 'danger', true),
      step('auth-token', 'Login token', 'A missing token should redirect to login or show a clear expired-session message.', 'danger', true),
      step('vector-ready', 'Vector readiness', 'When vector database is offline, show that the answer may not include knowledge evidence.', 'warning'),
      step('llm-mode', 'LLM mode', 'Show whether the system is using DeepSeek-compatible API or MockLLM.', 'info')
    ],
    operations: [
      op({
        key: 'agent.send',
        label: 'Send message',
        endpoint: '/api/agents/chat/stream',
        method: 'POST',
        requiresNetwork: true,
        requiresConfirm: false,
        requiresFeishuAuth: false,
        requiresVectorReady: false,
        allowedRoles: allRoles,
        preflight: [
          step('prompt-not-empty', 'Prompt is not empty', 'Block empty or whitespace-only prompts.', 'danger', true),
          step('prompt-length', 'Prompt length', 'Warn above 4000 characters and suggest splitting into multiple turns.', 'warning'),
          step('secret-check', 'Secret check', 'Warn when input looks like an API key, password or token.', 'warning')
        ],
        successHints: ['Append assistant message progressively', 'Persist final answer and metadata', 'Keep tool details expandable'],
        failureHints: ['Offer retry', 'Show backend error text', 'Keep the user prompt in composer for editing']
      }),
      op({
        key: 'agent.stop',
        label: 'Stop streaming',
        requiresNetwork: false,
        requiresConfirm: false,
        requiresFeishuAuth: false,
        requiresVectorReady: false,
        allowedRoles: allRoles,
        preflight: [step('stream-active', 'Stream is active', 'Only enable stop button while a response is streaming.')],
        successHints: ['Mark message as stopped', 'Allow resend', 'Do not clear partial text'],
        failureHints: ['Disable duplicate stop clicks', 'Explain if stream already ended']
      }),
      op({
        key: 'agent.retry',
        label: 'Retry last message',
        endpoint: '/api/agents/chat/stream',
        method: 'POST',
        requiresNetwork: true,
        requiresConfirm: false,
        requiresFeishuAuth: false,
        requiresVectorReady: false,
        allowedRoles: allRoles,
        preflight: [step('last-user-message', 'Last user message exists', 'Retry must use the previous user message.')],
        successHints: ['Create a new assistant bubble', 'Keep old failed bubble for comparison'],
        failureHints: ['Show retry count', 'Suggest switching to MockLLM when API is unavailable']
      })
    ]
  },
  {
    page: 'knowledge',
    label: 'Knowledge Base',
    route: '/knowledge',
    allowedRoles: allRoles,
    mainEndpoints: ['/api/knowledge/docs', '/api/knowledge/tags', '/api/knowledge/vector/search', '/api/knowledge/vector/index'],
    mobileParity: [
      'Document cards show title, tags, source and summary.',
      'Vector results show matched fields and source document.',
      'Empty search returns recovery suggestions instead of a blank area.',
      'Upload and sync actions use the same confirmation copy as web.',
      'Mobile list supports pull-to-refresh and compact filters.'
    ],
    responsiveRules: [
      'Tag filters wrap instead of overflowing horizontally.',
      'Summary text is clamped with expand action.',
      'Document details open in a drawer on web and a sheet on mobile.',
      'Search box remains visible above results.',
      'Source links are tap targets at least 44px high.'
    ],
    emptyState: {
      title: 'No knowledge documents',
      description: 'Sync Feishu documents, import mock data or add local records before using document-grounded answers.',
      actions: ['Sync Feishu folder', 'Create demo document', 'Open vector diagnostics']
    },
    diagnostics: [
      step('mysql-docs', 'Document table', 'knowledge_doc records should exist before indexing.', 'warning'),
      step('weaviate-schema', 'Vector schema', 'Schema must include title, content, source, tags and document id.', 'danger', true),
      step('chunk-count', 'Chunk count', 'A zero chunk count means semantic search cannot answer from local knowledge.', 'warning'),
      step('feishu-source', 'Feishu source', 'Feishu tokens should be stored for traceability when synced.', 'info')
    ],
    operations: [
      op({
        key: 'knowledge.search',
        label: 'Search vector knowledge',
        endpoint: '/api/knowledge/vector/search',
        method: 'GET',
        requiresNetwork: true,
        requiresConfirm: false,
        requiresFeishuAuth: false,
        requiresVectorReady: true,
        allowedRoles: allRoles,
        preflight: [
          step('keyword-length', 'Keyword length', 'Warn when keyword is shorter than two characters.', 'warning'),
          step('vector-health', 'Vector health', 'Check vector service before search.', 'warning')
        ],
        successHints: ['Show matched fields', 'Show score if returned', 'Keep source document link visible'],
        failureHints: ['Fallback to document list', 'Suggest re-index', 'Ask user to simplify the query']
      }),
      op({
        key: 'knowledge.index',
        label: 'Build vector index',
        endpoint: '/api/knowledge/vector/index',
        method: 'POST',
        requiresNetwork: true,
        requiresConfirm: true,
        requiresFeishuAuth: false,
        requiresVectorReady: false,
        allowedRoles: staffRoles,
        preflight: [
          step('confirm-reindex', 'Confirm re-index', 'Explain whether this will clear or append vector chunks.', 'warning'),
          step('doc-count', 'Document count', 'Warn if there are no MySQL documents to index.', 'warning')
        ],
        successHints: ['Show indexed count', 'Show skipped duplicate count', 'Refresh vector diagnostics'],
        failureHints: ['Show Weaviate error', 'Offer schema reset guidance', 'Keep previous index status visible']
      }),
      op({
        key: 'knowledge.summarize',
        label: 'Summarize document',
        endpoint: '/api/knowledge/docs/{id}/summarize',
        method: 'POST',
        requiresNetwork: true,
        requiresConfirm: false,
        requiresFeishuAuth: false,
        requiresVectorReady: false,
        allowedRoles: staffRoles,
        preflight: [step('doc-selected', 'Document selected', 'A document id is required.')],
        successHints: ['Update summary card', 'Store tags if returned'],
        failureHints: ['Keep old summary', 'Suggest asking AI Workspace with the title']
      })
    ]
  },
  {
    page: 'content',
    label: 'Content Creation',
    route: '/content',
    allowedRoles: staffRoles,
    mainEndpoints: ['/api/content/topics', '/api/content/copies', '/api/content/calendar', '/api/content/topics/generate'],
    mobileParity: [
      'Topic library keeps the 3 by 2 card rhythm on large screens and paged cards on mobile.',
      'Copy library is paginated and shows all historical copywriting assets.',
      'Calendar markers open the list of content scheduled for that date.',
      'Copy history supports image upload, image URL and image suggestion.',
      'Topic and copy cards support five-star rating and usage status.'
    ],
    responsiveRules: [
      'Topic cards become a single-column carousel or paged grid on mobile.',
      'Calendar day markers must remain visible at small widths.',
      'Copy body is clamped with detail modal for long text.',
      'Edit and delete buttons require clear touch spacing.',
      'Image suggestions should not push core text below the fold.'
    ],
    emptyState: {
      title: 'No content assets',
      description: 'Generate topics in AI Workspace or add topics and copywriting assets manually.',
      actions: ['Add topic', 'Add copywriting asset', 'Open content calendar']
    },
    diagnostics: [
      step('topic-table', 'Topic table', 'content_topic should store title, theme, style, rating and status.', 'warning'),
      step('copy-table', 'Copy table', 'copy assets should store body, image info, usage date and rating.', 'warning'),
      step('calendar-table', 'Calendar table', 'calendar records must link to copy or topic ids.', 'warning'),
      step('permissions', 'Content permission', 'Student users should not see content management tabs.', 'danger', true)
    ],
    operations: [
      op({
        key: 'content.topic.create',
        label: 'Add topic',
        endpoint: '/api/content/topics',
        method: 'POST',
        requiresNetwork: true,
        requiresConfirm: false,
        requiresFeishuAuth: false,
        requiresVectorReady: false,
        allowedRoles: staffRoles,
        preflight: [
          step('title-required', 'Title required', 'Topic title cannot be empty.', 'danger', true),
          step('rating-range', 'Rating range', 'Rating must be one to five stars.', 'danger', true)
        ],
        successHints: ['Refresh current page', 'Keep page number stable', 'Show star rating immediately'],
        failureHints: ['Keep draft input', 'Show duplicate title warning', 'Offer retry']
      }),
      op({
        key: 'content.topic.delete',
        label: 'Delete topic',
        endpoint: '/api/content/topics/{id}',
        method: 'DELETE',
        requiresNetwork: true,
        requiresConfirm: true,
        requiresFeishuAuth: false,
        requiresVectorReady: false,
        allowedRoles: ['CONTENT_OPERATOR', 'TEAM_ADMIN'],
        preflight: [
          step('topic-selected', 'Topic selected', 'A topic id is required.', 'danger', true),
          step('history-warning', 'History warning', 'Warn if the topic already has generated copy history.', 'warning')
        ],
        successHints: ['Remove from list', 'Recalculate pagination', 'Show undo-like archived message when backend supports soft delete'],
        failureHints: ['Explain permission issue', 'Keep card visible', 'Suggest archive instead']
      }),
      op({
        key: 'content.copy.create',
        label: 'Add copywriting asset',
        endpoint: '/api/content/copies',
        method: 'POST',
        requiresNetwork: true,
        requiresConfirm: false,
        requiresFeishuAuth: false,
        requiresVectorReady: false,
        allowedRoles: staffRoles,
        preflight: [
          step('body-required', 'Body required', 'Copy body cannot be empty.', 'danger', true),
          step('image-or-suggestion', 'Image or suggestion', 'If no image is attached, keep an image suggestion field.', 'info'),
          step('usage-date', 'Usage date', 'Used copy should have a concrete publication date.', 'warning')
        ],
        successHints: ['Refresh copy page', 'Update calendar marker if date exists', 'Show image preview or suggestion block'],
        failureHints: ['Keep draft content', 'Validate image URL', 'Retry without image first']
      }),
      op({
        key: 'content.calendar.open',
        label: 'Open calendar day',
        requiresNetwork: false,
        requiresConfirm: false,
        requiresFeishuAuth: false,
        requiresVectorReady: false,
        allowedRoles: staffRoles,
        preflight: [step('date-has-marker', 'Date has marker', 'Only dates with content should expand a publication list.')],
        successHints: ['Scroll list into view', 'Keep selected date highlighted'],
        failureHints: ['Show no content message', 'Offer to schedule content on this day']
      })
    ]
  },
  {
    page: 'school',
    label: 'School Intelligence',
    route: '/schools',
    allowedRoles: allRoles,
    mainEndpoints: ['/api/schools', '/api/school-projects', '/api/schools/recommend'],
    mobileParity: [
      'School and project cards show deadline, conditions and material requirements.',
      'Recommendation results explain match reason and risk.',
      'Student users can browse but editing controls are hidden.',
      'Deadline urgency uses consistent tags across web and mobile.',
      'Project detail opens as modal or bottom sheet with full material list.'
    ],
    responsiveRules: [
      'Project list supports compact filters for school, deadline and project type.',
      'Deadline countdown should not overflow cards.',
      'Long material requirements are grouped into bullet lists.',
      'Recommendation CTA stays near selected profile or condition form.',
      'Tabs should be scrollable on small screens.'
    ],
    emptyState: {
      title: 'No school projects',
      description: 'Import project information, add mock data or ask AI Workspace to organize school intelligence.',
      actions: ['Add project', 'Run recommendation', 'Open deadline trend']
    },
    diagnostics: [
      step('school-table', 'School table', 'school_info stores base school metadata.', 'warning'),
      step('project-table', 'Project table', 'school_project stores deadline and material requirements.', 'warning'),
      step('recommendation-input', 'Recommendation input', 'Recommendation requires profile or criteria.', 'warning'),
      step('student-permission', 'Student permission', 'Student users should not see edit/delete actions.', 'danger', true)
    ],
    operations: [
      op({
        key: 'school.recommend',
        label: 'Recommend school projects',
        endpoint: '/api/schools/recommend',
        method: 'POST',
        requiresNetwork: true,
        requiresConfirm: false,
        requiresFeishuAuth: false,
        requiresVectorReady: false,
        allowedRoles: ['EDU_CONSULTANT', 'TEAM_ADMIN', 'STUDENT_USER'],
        preflight: [
          step('profile-or-condition', 'Profile or condition', 'Need a student profile or manual criteria.', 'danger', true),
          step('deadline-awareness', 'Deadline awareness', 'Warn when projects may already be closed.', 'warning')
        ],
        successHints: ['Show match score', 'Show risk reason', 'Offer export to Feishu'],
        failureHints: ['Simplify conditions', 'Refresh project list', 'Use mock recommendation fallback']
      }),
      op({
        key: 'school.project.save',
        label: 'Save school project',
        endpoint: '/api/school-projects',
        method: 'POST',
        requiresNetwork: true,
        requiresConfirm: false,
        requiresFeishuAuth: false,
        requiresVectorReady: false,
        allowedRoles: teacherRoles,
        preflight: [
          step('school-required', 'School required', 'School name cannot be empty.', 'danger', true),
          step('deadline-format', 'Deadline format', 'Deadline should be a valid date.', 'warning'),
          step('materials-required', 'Materials', 'Material requirements should be clear enough for students.', 'warning')
        ],
        successHints: ['Refresh project list', 'Update deadline chart'],
        failureHints: ['Keep edit draft', 'Show validation messages']
      })
    ]
  },
  {
    page: 'student',
    label: 'Student Management',
    route: '/students',
    allowedRoles: teacherRoles,
    mainEndpoints: ['/api/students', '/api/students/{id}', '/api/students/{id}/analyze'],
    mobileParity: [
      'Student list uses compact cards on mobile and table on web.',
      'Detail drawer maps to a full-screen mobile sheet.',
      'Risk level tags use identical colors.',
      'AI analysis exposes evidence and recommendations.',
      'Student privacy fields are hidden from unauthorized roles.'
    ],
    responsiveRules: [
      'Long target school lists wrap inside detail panels.',
      'GPA and ranking are grouped as scan-friendly metrics.',
      'Edit form uses step sections on mobile.',
      'Danger actions stay at the bottom of detail panel.',
      'Risk explanation is visible next to the tag.'
    ],
    emptyState: {
      title: 'No student profiles',
      description: 'Add student profiles before running student analysis or school recommendation.',
      actions: ['Add student', 'Import mock students', 'Open role settings']
    },
    diagnostics: [
      step('student-role', 'Role check', 'Only teacher and admin roles can enter this page.', 'danger', true),
      step('gpa-validity', 'GPA validity', 'GPA should be normalized before AI analysis.', 'warning'),
      step('privacy', 'Privacy', 'Avoid showing student personal data in exported logs.', 'danger', true)
    ],
    operations: [
      op({
        key: 'student.profile.save',
        label: 'Save student profile',
        endpoint: '/api/students',
        method: 'POST',
        requiresNetwork: true,
        requiresConfirm: false,
        requiresFeishuAuth: false,
        requiresVectorReady: false,
        allowedRoles: teacherRoles,
        preflight: [
          step('name-required', 'Name required', 'Student name cannot be empty.', 'danger', true),
          step('gpa-range', 'GPA range', 'GPA must be in a reasonable range.', 'danger', true),
          step('risk-selected', 'Risk selected', 'Risk level should be explicit.', 'warning')
        ],
        successHints: ['Refresh list', 'Close drawer after save', 'Offer AI analysis'],
        failureHints: ['Keep form data', 'Highlight invalid fields']
      }),
      op({
        key: 'student.analyze',
        label: 'Analyze student',
        endpoint: '/api/students/{id}/analyze',
        method: 'POST',
        requiresNetwork: true,
        requiresConfirm: false,
        requiresFeishuAuth: false,
        requiresVectorReady: false,
        allowedRoles: teacherRoles,
        preflight: [step('student-selected', 'Student selected', 'A student id is required.', 'danger', true)],
        successHints: ['Show strengths', 'Show risk factors', 'Offer school matching'],
        failureHints: ['Use profile summary fallback', 'Retry after backend recovers']
      })
    ]
  },
  {
    page: 'analytics',
    label: 'Analytics',
    route: '/analytics',
    allowedRoles: staffRoles,
    mainEndpoints: ['/api/analytics/overview', '/api/analytics/student-distribution', '/api/analytics/content-stats', '/api/analytics/school-deadlines'],
    mobileParity: [
      'Metric cards are visible before charts.',
      'Charts degrade to list summaries on small screens.',
      'Loading and empty states explain which dataset failed.',
      'Content and school statistics use consistent labels.',
      'Mobile chart cards keep a fixed aspect ratio.'
    ],
    responsiveRules: [
      'Charts use resize observers.',
      'Legend wraps under the chart on mobile.',
      'Overview cards use two columns on tablets and one column on phones.',
      'Large charts are scrollable only inside their card.',
      'No chart should block the rest of dashboard when it fails.'
    ],
    emptyState: {
      title: 'No analytics data',
      description: 'Analytics uses content, student, school and task data. Import mock data or refresh backend aggregates.',
      actions: ['Refresh overview', 'Open content assets', 'Open school projects']
    },
    diagnostics: [
      step('overview-api', 'Overview API', 'Overview endpoint should return stable card metrics.', 'warning'),
      step('chart-data', 'Chart data', 'Chart arrays should not be null.', 'warning'),
      step('resize', 'Resize handling', 'Charts must resize after route transition.', 'info')
    ],
    operations: [
      op({
        key: 'analytics.refresh',
        label: 'Refresh analytics',
        endpoint: '/api/analytics/overview',
        method: 'GET',
        requiresNetwork: true,
        requiresConfirm: false,
        requiresFeishuAuth: false,
        requiresVectorReady: false,
        allowedRoles: staffRoles,
        preflight: [step('network', 'Network', 'Analytics requires backend access.', 'danger', true)],
        successHints: ['Update cards first', 'Then update charts', 'Show last updated time'],
        failureHints: ['Keep previous chart data', 'Show dataset-specific error']
      })
    ]
  },
  {
    page: 'feishu',
    label: 'Feishu Sync',
    route: '/feishu',
    allowedRoles: staffRoles,
    mainEndpoints: ['/api/feishu/sync/status', '/api/feishu/sync/docs', '/api/feishu/sync/bitable', '/api/feishu/logs', '/api/feishu/doc/create'],
    mobileParity: [
      'Sync status cards show document, bitable, task and bot status.',
      'Creation actions require clear folder token display.',
      'Permission failures include a direct authorization hint.',
      'Logs can be filtered by status and action type.',
      'AI Workspace tool calls and Feishu page logs use the same wording.'
    ],
    responsiveRules: [
      'Status cards use vertical layout on mobile.',
      'Long log messages are collapsible.',
      'Sync buttons are disabled during active sync.',
      'Folder token input is copyable.',
      'Permission links are readable and not truncated without tooltip.'
    ],
    emptyState: {
      title: 'No Feishu logs',
      description: 'Authorize lark-cli and run a sync or document creation action.',
      actions: ['Check auth guide', 'Sync docs', 'Create test document']
    },
    diagnostics: [
      step('lark-auth', 'Lark authorization', 'Backend lark-cli must be authenticated with the correct scopes.', 'danger', true),
      step('folder-token', 'Folder token', 'Shared folders require a valid parent token.', 'warning'),
      step('scope-doc-create', 'Doc creation scope', 'Creating docs needs document creation permission.', 'danger', true),
      step('audit-log', 'Audit log', 'Every sync or creation should write a log entry.', 'info')
    ],
    operations: [
      op({
        key: 'feishu.sync.docs',
        label: 'Sync Feishu docs',
        endpoint: '/api/feishu/sync/docs',
        method: 'POST',
        requiresNetwork: true,
        requiresConfirm: true,
        requiresFeishuAuth: true,
        requiresVectorReady: false,
        allowedRoles: staffRoles,
        preflight: [
          step('folder-token', 'Folder token', 'A shared folder token is required for folder sync.', 'danger', true),
          step('auth-ready', 'Authorization', 'lark-cli authorization should be valid.', 'danger', true)
        ],
        successHints: ['Show synced count', 'Offer vector index refresh', 'Append sync log'],
        failureHints: ['Show missing scope', 'Ask user to re-authorize', 'Keep previous logs']
      }),
      op({
        key: 'feishu.doc.create',
        label: 'Create Feishu doc',
        endpoint: '/api/feishu/doc/create',
        method: 'POST',
        requiresNetwork: true,
        requiresConfirm: false,
        requiresFeishuAuth: true,
        requiresVectorReady: false,
        allowedRoles: staffRoles,
        preflight: [
          step('title', 'Title', 'Document title is required.', 'danger', true),
          step('content', 'Content', 'Document content is required.', 'danger', true),
          step('parent-token', 'Parent token', 'Folder token is optional but should be shown when used.', 'info')
        ],
        successHints: ['Show document URL', 'Copy token button', 'Write Feishu sync record'],
        failureHints: ['Show permission error', 'Fallback to local content draft']
      })
    ]
  },
  {
    page: 'settings',
    label: 'Settings',
    route: '/settings',
    allowedRoles: allRoles,
    mainEndpoints: ['/api/users/me', '/api/auth/login'],
    mobileParity: [
      'Backend URL can be changed on both web and mobile.',
      'Role and visible menu list should be shown clearly.',
      'Unsafe local API keys should be warned before saving.',
      'Logout keeps backend URL but clears token.',
      'Health check result should be visible before returning to workspace.'
    ],
    responsiveRules: [
      'Settings sections collapse on mobile.',
      'Danger zone actions are separated from normal settings.',
      'Long backend URLs wrap safely.',
      'Role permission matrix is horizontally scrollable when needed.',
      'Save buttons stay near the edited section.'
    ],
    emptyState: {
      title: 'Settings are local',
      description: 'Configure backend address, login state and permission diagnostics.',
      actions: ['Run health check', 'View role permissions', 'Logout']
    },
    diagnostics: [
      step('base-url', 'Backend URL', 'Base URL must include protocol and should not end with extra slashes.', 'danger', true),
      step('token', 'Token', 'Expired token should be cleared with a friendly message.', 'warning'),
      step('role-menu', 'Role menu', 'Hidden menus should match backend role permissions.', 'warning')
    ],
    operations: [
      op({
        key: 'settings.backend.save',
        label: 'Save backend URL',
        requiresNetwork: false,
        requiresConfirm: false,
        requiresFeishuAuth: false,
        requiresVectorReady: false,
        allowedRoles: allRoles,
        preflight: [
          step('format', 'URL format', 'URL must start with http:// or https://.', 'danger', true),
          step('trim', 'Trim slash', 'Remove trailing slash before storing.', 'info')
        ],
        successHints: ['Run health check', 'Persist locally', 'Refresh API client base URL'],
        failureHints: ['Keep previous working URL', 'Show example ngrok URL']
      }),
      op({
        key: 'settings.logout',
        label: 'Logout',
        requiresNetwork: false,
        requiresConfirm: true,
        requiresFeishuAuth: false,
        requiresVectorReady: false,
        allowedRoles: allRoles,
        preflight: [step('confirm', 'Confirm logout', 'User should confirm before clearing token.', 'warning')],
        successHints: ['Clear token', 'Return to login', 'Keep backend URL'],
        failureHints: ['Allow force clear local session']
      })
    ]
  }
]

export function blueprintForPage(page: PageKey | string) {
  return pageSafetyBlueprints.find((item) => item.page === page)
}

export function operationsForRole(role: RoleKey | string) {
  const result: OperationBlueprint[] = []
  pageSafetyBlueprints.forEach((page) => {
    page.operations.forEach((operation) => {
      if (operation.allowedRoles.includes(role as RoleKey)) result.push(operation)
    })
  })
  return result
}

export function visiblePagesForRole(role: RoleKey | string) {
  return pageSafetyBlueprints.filter((page) => page.allowedRoles.includes(role as RoleKey))
}

export function disabledPagesForRole(role: RoleKey | string) {
  return pageSafetyBlueprints.filter((page) => !page.allowedRoles.includes(role as RoleKey))
}

export function blockingDiagnostics(page: PageKey | string) {
  return blueprintForPage(page)?.diagnostics.filter((item) => item.blocking) || []
}

export function operationNeedsConfirmation(operationKey: string) {
  return pageSafetyBlueprints.some((page) =>
    page.operations.some((operation) => operation.key === operationKey && operation.requiresConfirm)
  )
}

export function operationFailureHints(operationKey: string) {
  for (const page of pageSafetyBlueprints) {
    const operation = page.operations.find((item) => item.key === operationKey)
    if (operation) return operation.failureHints
  }
  return ['Retry later', 'Check backend logs', 'Confirm current role permission']
}

export function renderSafetyMarkdown(role: RoleKey | string) {
  const lines: string[] = []
  lines.push(`# FlowMind Frontend Safety Blueprint`)
  lines.push(``)
  lines.push(`Current role: ${role}`)
  lines.push(``)
  lines.push(`## Visible Pages`)
  visiblePagesForRole(role).forEach((page) => {
    lines.push(`- ${page.label}: ${page.route}`)
  })
  lines.push(``)
  lines.push(`## Disabled Pages`)
  disabledPagesForRole(role).forEach((page) => {
    lines.push(`- ${page.label}: ${page.route}`)
  })
  lines.push(``)
  lines.push(`## Blocking Diagnostics`)
  pageSafetyBlueprints.forEach((page) => {
    const blocking = page.diagnostics.filter((item) => item.blocking)
    if (blocking.length) {
      lines.push(`### ${page.label}`)
      blocking.forEach((item) => lines.push(`- ${item.label}: ${item.description}`))
    }
  })
  return lines.join('\n')
}
