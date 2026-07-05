import request from './request'

/**
 * 各模块 API。路径/字段对齐后端真实契约（见 docs/impl/00c_静态审查报告.md 第二节 FS1-FS12、C44-C50）。
 * 仅覆盖最小可视骨架用到的端点；字段以后端 DTO 为准，后端暂无的端点在此标注 TODO，不臆造。
 */

// M1 认证
export const authApi = {
  register: (data: any) => request.post('/auth/register', data),
  login: (data: any) => request.post('/auth/login', data),
  // FS2：真实端点 /users/me（非 /auth/me），返回 UserDTO
  me: () => request.get('/users/me'),
  // FS4：统一提交入口 POST /auth-applications，按 verifyMethod 分支携带不同字段
  // （STUDENT_SSO/STUDENT_MANUAL/ALUMNI_INVITE_CODE/ALUMNI_MANUAL_GUARANTEE），
  // 见 SubmitAuthApplicationRequest；当前无调用该接口的页面，先修正契约供后续接入。
  submitStudentAuth: (data: any) => request.post('/auth-applications', data),
}

// 标签只读查询（供各表单下拉 / 按 tagId 反查名称，tagType 为空返回全部）
export const tagApi = {
  list: (tagType?: string) => request.get('/tags', tagType ? { params: { tagType } } : undefined),
}

// M3 知识库
export const knowledgeApi = {
  list: (params: any) => request.get('/knowledge-entries', { params }),
  search: (params: any) => request.get('/knowledge-entries/search', { params }),
  detail: (id: number) => request.get(`/knowledge-entries/${id}`),
  // C46：真实路径 /feedbacks（复数），请求体字段为 feedbackType（USEFUL/OUTDATED/NEED_UPDATE）
  feedback: (id: number, feedbackType: string, comment?: string) =>
    request.post(`/knowledge-entries/${id}/feedbacks`, { feedbackType, comment }),
}

// M4 结构化求助
export const helpApi = {
  list: (params: any) => request.get('/help-tickets', { params }),
  detail: (id: number) => request.get(`/help-tickets/${id}`),
  // FS5：发布单不再提交 major/grade（后端从发布人档案只读快照），只需 title/content/questionTypeTagId/targetDirection
  create: (data: any) => request.post('/help-tickets', data),
  // FS8：三段式回答字段为 precondition/steps(数组)/cautions
  answer: (ticketId: number, data: { precondition?: string; steps: string[]; cautions?: string }) =>
    request.post(`/help-tickets/${ticketId}/answers`, data),
  followup: (ticketId: number, content: string) =>
    request.post(`/help-tickets/${ticketId}/followups`, { content }),
  // FS7：真实端点 PATCH /help-answers/{answerId}/adopt?ticketId=
  adopt: (ticketId: number, answerId: number) =>
    request.patch(`/help-answers/${answerId}/adopt`, null, { params: { ticketId } }),
}

// M5 机会与组队
export const opportunityApi = {
  list: (params: any) => request.get('/opportunities', { params }),
  detail: (id: number) => request.get(`/opportunities/${id}`),
  teams: (params: any) => request.get('/teams', { params }),
}

// M6 成长时间线
export const timelineApi = {
  // GET /api/v1/timeline/me 不接受查询参数（聚合视图，无 route 入参），调用方传参会被忽略，
  // 保留形参签名只为不破坏既有调用点，不代表后端消费它。
  mine: (params?: any) => request.get('/timeline/me', { params }),
  // FS9：真实端点 PATCH /timeline-nodes/{id}/progress，status∈{NOT_STARTED,DONE}
  markProgress: (nodeId: number, status: string) =>
    request.patch(`/timeline-nodes/${nodeId}/progress`, { status }),
}

// 通知
export const notificationApi = {
  list: (params: any) => request.get('/notifications', { params }),
  unreadCount: () => request.get('/notifications/unread-count'),
  markRead: (id: number) => request.patch(`/notifications/${id}/read`),
}

// M7 管理后台
export const adminApi = {
  // FS10：真实路径 /audit-tasks（无 admin 前缀），返回 {records,total,page,size,countByType}
  auditList: (params: any) => request.get('/audit-tasks', { params }),
  // FS11：approve/reject 均不存在，统一走 PATCH /audit-tasks/{id}/decide，decision=APPROVE/RETURN/REJECT
  decide: (
    id: number,
    decision: 'APPROVE' | 'RETURN' | 'REJECT',
    extra?: { reasonCode?: string; comment?: string; checklistResult?: any }
  ) => request.patch(`/audit-tasks/${id}/decide`, { decision, ...extra }),
  // FS12：真实端点 /admin/stats/overview，字段为 OperationOverviewDTO（authApprovedCount 等），
  // 与旧口径 pending/approvedToday/rejectedToday/avgAuditMinutes 完全不同，见该 DTO 注释。
  statsOverview: (params?: { dateFrom?: string; dateTo?: string }) =>
    request.get('/admin/stats/overview', { params }),
  // TODO(FS12)：后端暂无按 pending/approvedToday/rejectedToday/avgAuditMinutes 口径的专用端点；
  // /admin/stats/audit-throughput 只提供按日聚合趋势（AuditThroughputStatsDTO），非单一均值，
  // 如需该卡片需产品确认口径后由 M7 补端点，不在此臆造。
  auditThroughput: (params?: { dateFrom?: string; dateTo?: string }) =>
    request.get('/admin/stats/audit-throughput', { params }),
}
