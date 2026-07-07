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
}

// 身份认证申请（用户侧提交 + 生命周期）。SubmitAuthApplicationRequest{verifyMethod,realName,studentNo,college,majorText,evidenceUrl,inviteCode,guarantor1Id,guarantor2Id}
// AuthApplicationDTO{id,applyRole,verifyMethod,realName,studentNo,majorText,college,status,autoApproved,rejectReason,statusHint,createdAt}
export const authApplicationApi = {
  submit: (data: any) => request.post('/auth-applications', data),
  mine: (params?: { status?: string; page?: number; size?: number }) => request.get('/auth-applications/me', { params }),
  detail: (id: number) => request.get(`/auth-applications/${id}`),
  withdraw: (id: number) => request.patch(`/auth-applications/${id}/withdraw`),
  resubmit: (id: number, data: any) => request.patch(`/auth-applications/${id}/resubmit`, data),
}

// 贡献者认证申请（校友）：POST /contributor-cert-applications，ApplyContributorCertRequest{honorCertUrl,note}，落审核队列
export const contributorCertApi = {
  apply: (data: { honorCertUrl?: string; note?: string }) => request.post('/contributor-cert-applications', data),
}

// 编辑个人资料（真实端点已存在）：PUT /users/me，UpdateProfileRequest
export const userApi = {
  updateMe: (data: any) => request.put('/users/me', data),
  // 他人公开主页：GET /users/{id}/public → PublicUserDTO{userId,username,role,authStatus,bio,major,grade,avatarUrl,tags[],followerCount,followingCount,following,postCount,badges[]}
  publicProfile: (id: number) => request.get(`/users/${id}/public`),
}

// 关注：POST/DELETE /users/{id}/follow；GET /users/{id}/follow-status → {following,followerCount,followingCount}
export const followApi = {
  follow: (id: number) => request.post(`/users/${id}/follow`),
  unfollow: (id: number) => request.delete(`/users/${id}/follow`),
  status: (id: number) => request.get(`/users/${id}/follow-status`),
}

// 私信消息中心：会话列表/历史/发送/标记已读/未读数
export const messageApi = {
  conversations: () => request.get('/messages/conversations'),
  history: (peerId: number) => request.get(`/messages/conversations/${peerId}`),
  send: (receiverId: number, content: string) => request.post('/messages', { receiverId, content }),
  markRead: (peerId: number) => request.patch(`/messages/conversations/${peerId}/read`),
  unreadCount: () => request.get('/messages/unread-count'),
}

// 徽章：GET /users/{id}/badges（公开）/ /users/me/badges（全部）；PATCH /badges/{id} 置顶/隐藏
export const badgeApi = {
  userBadges: (id: number) => request.get(`/users/${id}/badges`),
  myBadges: () => request.get('/users/me/badges'),
  setFlags: (id: number, data: { pinned?: boolean; hidden?: boolean }) => request.patch(`/badges/${id}`, data),
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
  // FR-M5-08：简单报名信令（STUDENT/ALUMNI），路径参数无请求体
  apply: (id: number) => request.patch(`/opportunities/${id}/apply`),
}

// M6 成长时间线
export const timelineApi = {
  // GET /api/v1/timeline/me 不接受查询参数（聚合视图，无 route 入参），调用方传参会被忽略，
  // 保留形参签名只为不破坏既有调用点，不代表后端消费它。返回 MyTimelineDTO：
  // {routeType,currentStage,needsRouteDecision,graduated,stages:[{stage,stageLabel,nodes:[{node,progressStatus,overdue,monthsOverdue,...}]}],overallProgress}
  mine: (params?: any) => request.get('/timeline/me', { params }),
  // FS9：真实端点 PATCH /timeline-nodes/{id}/progress，status∈{NOT_STARTED,DONE}
  markProgress: (nodeId: number, status: string) =>
    request.patch(`/timeline-nodes/${nodeId}/progress`, { status }),
  // FR-M6-07：选择/切换发展路线，routeType∈{POSTGRAD,EMPLOY,COMPETITION,CIVIL}（UNDECIDED 不可提交）
  confirmRoute: (routeType: string) => request.patch('/timeline/me/route', null, { params: { routeType } }),
}

// 通知
export const notificationApi = {
  list: (params: any) => request.get('/notifications', { params }),
  unreadCount: () => request.get('/notifications/unread-count'),
  markRead: (id: number) => request.patch(`/notifications/${id}/read`),
  markAllRead: () => request.patch('/notifications/read-all'),
}

// M7 管理后台
export const adminApi = {
  // FS10：真实路径 /audit-tasks（无 admin 前缀），返回 {records,total,page,size,countByType}
  auditList: (params: any) => request.get('/audit-tasks', { params }),
  auditDetail: (id: number) => request.get(`/audit-tasks/${id}`),
  // FS11：approve/reject 均不存在，统一走 PATCH /audit-tasks/{id}/decide，decision=APPROVE/RETURN/REJECT
  decide: (
    id: number,
    decision: 'APPROVE' | 'RETURN' | 'REJECT',
    extra?: { reasonCode?: string; comment?: string; checklistResult?: any }
  ) => request.patch(`/audit-tasks/${id}/decide`, { decision, ...extra }),
  // 批量审核：PATCH /audit-tasks/batch-decide，BatchDecideRequest{targetType,ids,decision,reasonCode?,comment?}
  batchDecide: (data: { targetType?: string; ids: number[]; decision: 'APPROVE' | 'RETURN' | 'REJECT'; reasonCode?: string; comment?: string }) =>
    request.patch('/audit-tasks/batch-decide', data),
  // 举报处理：queue/detail/handle。ReportDTO{id,targetType,targetId,targetSummary,reporterName,reasonType,description,status,handleAction,handleComment,createdAt,handledAt}
  reportQueue: (params: { status?: string; targetType?: string; page?: number; size?: number }) => request.get('/reports', { params }),
  reportDetail: (id: number) => request.get(`/reports/${id}`),
  // HandleReportRequest{decision,handleAction(NONE/CONTENT_HIDDEN/CONTENT_OFFLINE/USER_DISABLED),handleComment}
  reportHandle: (id: number, data: { decision?: string; handleAction: string; handleComment?: string }) =>
    request.patch(`/reports/${id}/handle`, data),
  // 标签管理：分页(TagUsageDTO 含 usageCount) + 增改停用
  tagPage: (params: { tagType?: string; keyword?: string; page?: number; size?: number }) => request.get('/admin/tags', { params }),
  tagCreate: (data: { tagType: string; tagName: string; parentId?: number; sortOrder?: number }) => request.post('/admin/tags', data),
  tagUpdate: (id: number, data: { tagName?: string; parentId?: number; sortOrder?: number }) => request.put(`/admin/tags/${id}`, data),
  tagDisable: (id: number) => request.delete(`/admin/tags/${id}`),
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
