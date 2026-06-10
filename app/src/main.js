const { createApp, computed, onMounted, reactive, ref } = window.Vue;

const SESSION_KEY = "travel-ticket-session";
const API_BASE_KEY = "travel-ticket-api-base";
const apiBase = localStorage.getItem(API_BASE_KEY) || "http://127.0.0.1:8080";

const statusEnum = {
  全部: "",
  待审批: "PENDING_REVIEW",
  已通过: "APPROVED",
  已驳回: "REJECTED",
  待补票: "MISSING_ATTACHMENT",
  异常: "EXCEPTION",
  已核销: "REIMBURSED",
};

const statusLabel = Object.fromEntries(Object.entries(statusEnum).map(([label, value]) => [value, label]));

const riskLabel = {
  NONE: "无",
  LOW: "低",
  MEDIUM: "中",
  HIGH: "高",
  CRITICAL: "严重",
};

const riskEnum = Object.fromEntries(Object.entries(riskLabel).map(([value, label]) => [label, value]));

const ticketTypeEnum = {
  高铁: "HIGH_SPEED_RAIL",
  动车: "EMU",
  普铁: "TRAIN",
  城际: "INTERCITY",
  飞机: "FLIGHT",
  其他: "OTHER",
};

const ticketTypeLabel = Object.fromEntries(Object.entries(ticketTypeEnum).map(([label, value]) => [value, label]));

const cityOptions = [
  "全部",
  "北京",
  "上海",
  "广州",
  "深圳",
  "杭州",
  "成都",
  "武汉",
  "南京",
  "西安",
  "重庆",
  "天津",
  "苏州",
  "厦门",
  "青岛",
  "郑州",
  "长沙",
  "合肥",
  "宁波",
  "福州",
  "昆明",
];

const attachmentEnum = {
  已上传: "UPLOADED",
  缺失: "MISSING",
};

const attachmentLabel = {
  UPLOADED: "已上传",
  MISSING: "缺失",
};

const sampleTickets = [
  {
    employeeId: 10086,
    employeeName: "沈韵",
    department: "华东销售部",
    tripPurpose: "上海重点客户拜访",
    ticketType: "高铁",
    ticketNo: "G12-20260610-001",
    carrierNo: "G12",
    departureCity: "上海",
    arrivalCity: "北京",
    departureAt: "2026-06-10T09:30",
    seatClass: "二等座",
    amount: 553,
    status: "待审批",
    attachmentStatus: "已上传",
  },
  {
    employeeId: 20018,
    employeeName: "罗启",
    department: "交付中心",
    tripPurpose: "项目验收",
    ticketType: "动车",
    ticketNo: "D2282-20260610-002",
    carrierNo: "D2282",
    departureCity: "深圳",
    arrivalCity: "杭州",
    departureAt: "2026-06-10T10:15",
    seatClass: "一等座",
    amount: 468,
    status: "已核销",
    attachmentStatus: "已上传",
  },
  {
    employeeId: 30027,
    employeeName: "陈伊",
    department: "财务共享",
    tripPurpose: "总部会议",
    ticketType: "高铁",
    ticketNo: "G17-20260610-003",
    carrierNo: "G17",
    departureCity: "北京",
    arrivalCity: "上海",
    departureAt: "2026-06-10T13:20",
    seatClass: "商务座",
    amount: 1667,
    status: "异常",
    attachmentStatus: "已上传",
  },
  {
    employeeId: 40032,
    employeeName: "唐硕",
    department: "北区运营",
    tripPurpose: "门店巡检",
    ticketType: "城际",
    ticketNo: "C812-20260610-004",
    carrierNo: "C812",
    departureCity: "天津",
    arrivalCity: "北京",
    departureAt: "2026-06-11T07:50",
    seatClass: "二等座",
    amount: 68,
    status: "待补票",
    attachmentStatus: "缺失",
  },
];

function readJson(key, fallback) {
  try {
    const stored = localStorage.getItem(key);
    return stored ? JSON.parse(stored) : fallback;
  } catch {
    return fallback;
  }
}

function writeJson(key, value) {
  localStorage.setItem(key, JSON.stringify(value));
}

function emptyTicketForm() {
  return {
    employeeId: "",
    employeeName: "",
    department: "",
    tripPurpose: "",
    ticketType: "高铁",
    ticketNo: "",
    carrierNo: "",
    departureCity: "",
    arrivalCity: "",
    departureAt: "",
    seatClass: "二等座",
    amount: "",
    status: "待审批",
    attachmentStatus: "已上传",
  };
}

function toInstant(value) {
  if (!value) {
    return null;
  }
  return new Date(value).toISOString();
}

function toLocalDateTime(value) {
  if (!value) {
    return "";
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "";
  }
  const offset = date.getTimezoneOffset() * 60000;
  return new Date(date.getTime() - offset).toISOString().slice(0, 16);
}

function toEmployeeId(value) {
  const digits = String(value).replace(/\D/g, "");
  return Number(digits || value);
}

function mapTicket(item) {
  return {
    id: item.id || item.ticketId,
    tenantId: item.tenantId,
    employeeId: item.employeeId,
    employeeName: item.employeeName || `员工${item.employeeId}`,
    department: item.department || "未分配部门",
    tripPurpose: item.tripPurpose || "未填写事由",
    ticketType: ticketTypeLabel[item.travelType] || item.travelType || "其他",
    ticketNo: item.ticketNo,
    carrierNo: item.carrierNo,
    departureCity: item.departureCity,
    arrivalCity: item.arrivalCity,
    departureAt: toLocalDateTime(item.departAt),
    arriveAt: toLocalDateTime(item.arriveAt),
    seatClass: item.seatClass || "",
    amount: Number(item.amount || 0),
    currency: item.currency || "CNY",
    status: statusLabel[item.status] || item.status || "待审批",
    attachmentStatus: attachmentLabel[item.attachmentStatus] || item.attachmentStatus || "已上传",
    riskLevel: riskLabel[item.riskLevel] || item.riskLevel || "无",
    createdAt: item.createdAt,
    updatedAt: item.updatedAt,
  };
}

function toPayload(form) {
  return {
    employeeId: toEmployeeId(form.employeeId),
    employeeName: form.employeeName.trim(),
    department: form.department.trim(),
    ticketNo: form.ticketNo.trim(),
    externalSource: "WEB",
    externalTicketId: form.ticketNo.trim(),
    travelType: ticketTypeEnum[form.ticketType] || "OTHER",
    departureCity: form.departureCity.trim(),
    arrivalCity: form.arrivalCity.trim(),
    carrierNo: form.carrierNo.trim(),
    seatClass: form.seatClass.trim(),
    tripPurpose: form.tripPurpose.trim(),
    attachmentStatus: attachmentEnum[form.attachmentStatus] || "UPLOADED",
    departAt: toInstant(form.departureAt),
    arriveAt: null,
    amount: Number(form.amount),
    currency: "CNY",
    status: statusEnum[form.status] || "PENDING_REVIEW",
  };
}

function pageCount(pager) {
  return Math.max(1, Math.ceil(Number(pager.total || 0) / Number(pager.size || 1)));
}

function pageRange(pager, visibleCount) {
  if (!pager.total || !visibleCount) {
    return "0-0";
  }
  const start = pager.page * pager.size + 1;
  const end = Math.min(pager.total, start + visibleCount - 1);
  return `${start}-${end}`;
}

createApp({
  setup() {
    const authMode = ref("login");
    const currentUser = ref(readJson(SESSION_KEY, null));
    const activeSection = ref("dashboard");
    const toast = ref("");
    const editingId = ref("");
    const showTicketForm = ref(false);
    const deleteTarget = ref(null);
    const loading = ref(false);
    const tickets = ref([]);
    const pendingTickets = ref([]);
    const riskEvents = ref([]);

    const metrics = reactive({
      total: 0,
      pendingAmount: 0,
      riskRate: 0,
      approved: 0,
    });

    const loginForm = reactive({
      email: "admin@travel.local",
      password: "admin123",
    });

    const registerForm = reactive({
      name: "",
      company: "",
      email: "",
      password: "",
    });

    const filters = reactive({
      query: "",
      status: "全部",
      city: "全部",
      ticketType: "全部",
    });

    const ticketPager = reactive({
      page: 0,
      size: 20,
      total: 0,
    });

    const pendingPager = reactive({
      page: 0,
      size: 20,
      total: 0,
    });

    const riskPager = reactive({
      page: 0,
      size: 20,
      total: 0,
    });

    const ticketForm = reactive(emptyTicketForm());
    const statuses = ["全部", "待审批", "已通过", "已驳回", "待补票", "异常", "已核销"];
    const ticketTypes = ["全部", "高铁", "动车", "普铁", "城际", "飞机", "其他"];
    const editableStatuses = computed(() => statuses.filter((status) => status !== "全部"));
    const editableTicketTypes = computed(() => ticketTypes.filter((type) => type !== "全部"));
    const isDashboard = computed(() => activeSection.value === "dashboard");
    const showTicketModule = computed(() => isDashboard.value || activeSection.value === "tickets");
    const showApprovalModule = computed(() => isDashboard.value || activeSection.value === "approvals");
    const showRiskModule = computed(() => isDashboard.value || activeSection.value === "risk");
    const sectionTitle = computed(() => ({
      dashboard: "出差车票归集、审批与核销",
      tickets: "车票记录管理",
      approvals: "待审批车票处理",
      risk: "风险预警复核",
    }[activeSection.value]));
    const sectionHint = computed(() => ({
      dashboard: `租户：${currentUser.value?.tenantId} · 数据来自 PostgreSQL，热点快照写入 Redis，搜索索引写入 Elasticsearch`,
      tickets: "分页查看、筛选、编辑和删除车票记录",
      approvals: "按待审批状态分页处理车票审批动作",
      risk: "按风险等级分页查看需要复核的车票",
    }[activeSection.value]));

    const cities = cityOptions;

    const filteredTickets = computed(() => {
      const keyword = filters.query.trim().toLowerCase();
      return tickets.value.filter((ticket) => {
        const text = [
          ticket.employeeName,
          ticket.employeeId,
          ticket.department,
          ticket.ticketNo,
          ticket.carrierNo,
          ticket.departureCity,
          ticket.arrivalCity,
          ticket.tripPurpose,
        ]
          .join(" ")
          .toLowerCase();
        const queryMatched = !keyword || text.includes(keyword);
        const statusMatched = filters.status === "全部" || ticket.status === filters.status;
        const cityMatched =
          filters.city === "全部" || ticket.departureCity === filters.city || ticket.arrivalCity === filters.city;
        const typeMatched = filters.ticketType === "全部" || ticket.ticketType === filters.ticketType;
        return queryMatched && statusMatched && cityMatched && typeMatched;
      });
    });

    const ticketPageCount = computed(() => pageCount(ticketPager));
    const pendingPageCount = computed(() => pageCount(pendingPager));
    const riskPageCount = computed(() => pageCount(riskPager));
    const ticketRangeText = computed(() => pageRange(ticketPager, tickets.value.length));
    const pendingRangeText = computed(() => pageRange(pendingPager, pendingTickets.value.length));
    const riskRangeText = computed(() => pageRange(riskPager, riskEvents.value.length));

    const riskTickets = computed(() => {
      return riskEvents.value.map((event) => ({
        id: event.ticketId,
        employeeName: event.employeeName,
        department: event.department,
        ticketNo: event.ticketNo,
        carrierNo: event.carrierNo,
        departureCity: event.route?.split(" -> ")[0] || "",
        arrivalCity: event.route?.split(" -> ")[1] || "",
        attachmentStatus: attachmentLabel[event.attachmentStatus] || event.attachmentStatus,
        riskLevel: riskLabel[event.riskLevel] || event.riskLevel,
        message: event.message,
      }));
    });

    async function api(path, options = {}) {
      const headers = {
        Accept: "application/json",
        ...(options.body ? { "Content-Type": "application/json" } : {}),
      };
      if (options.tenant !== false && currentUser.value?.tenantId) {
        headers["X-Tenant-Id"] = currentUser.value.tenantId;
      }

      const response = await fetch(`${apiBase}${path}`, {
        method: options.method || "GET",
        headers,
        body: options.body ? JSON.stringify(options.body) : undefined,
      });
      const payload = await response.json().catch(() => null);
      if (!response.ok || payload?.success === false) {
        throw new Error(payload?.message || `请求失败：${response.status}`);
      }
      return payload?.data;
    }

    function showMessage(message) {
      toast.value = message;
      window.clearTimeout(showMessage.timer);
      showMessage.timer = window.setTimeout(() => {
        toast.value = "";
      }, 2600);
    }

    async function runWithLoading(task) {
      loading.value = true;
      try {
        return await task();
      } finally {
        loading.value = false;
      }
    }

    function applyPage(result, pager, target) {
      target.value = (result?.items || []).map(mapTicket);
      pager.page = Number(result?.page ?? pager.page);
      pager.size = Number(result?.size ?? pager.size);
      pager.total = Number(result?.total ?? 0);
    }

    function applyRiskPage(result) {
      riskEvents.value = result?.items || [];
      riskPager.page = Number(result?.page ?? riskPager.page);
      riskPager.size = Number(result?.size ?? riskPager.size);
      riskPager.total = Number(result?.total ?? 0);
    }

    function appendSharedFilters(query, options = {}) {
      const includeStatus = options.includeStatus !== false;
      const keyword = filters.query.trim();
      const status = statusEnum[filters.status];
      const travelType = ticketTypeEnum[filters.ticketType];
      if (keyword) {
        query.set("q", keyword);
      }
      if (includeStatus && status) {
        query.set("status", status);
      }
      if (filters.city !== "全部") {
        query.set("city", filters.city);
      }
      if (travelType) {
        query.set("travelType", travelType);
      }
      return query;
    }

    async function fetchTicketRecords() {
      const query = new URLSearchParams({
        page: String(ticketPager.page),
        size: String(ticketPager.size),
      });
      return api(`/api/v1/tickets?${appendSharedFilters(query)}`);
    }

    async function fetchPendingApprovals() {
      const query = new URLSearchParams({
        status: "PENDING_REVIEW",
        page: String(pendingPager.page),
        size: String(pendingPager.size),
      });
      return api(`/api/v1/tickets?${appendSharedFilters(query, { includeStatus: false })}`);
    }

    async function fetchRiskEvents() {
      const query = new URLSearchParams({
        page: String(riskPager.page),
        size: String(riskPager.size),
      });
      return api(`/api/v1/risk/events?${appendSharedFilters(query)}`);
    }

    function resetAllPagesAndReload() {
      ticketPager.page = 0;
      pendingPager.page = 0;
      riskPager.page = 0;
      reloadData();
    }

    function resetTicketPageAndReload() {
      resetAllPagesAndReload();
    }

    function clearFilters() {
      filters.query = "";
      filters.status = "全部";
      filters.city = "全部";
      filters.ticketType = "全部";
      resetAllPagesAndReload();
    }

    function resetPendingPageAndReload() {
      pendingPager.page = 0;
      reloadPendingApprovals();
    }

    function resetRiskPageAndReload() {
      riskPager.page = 0;
      reloadRiskEvents();
    }

    function goTicketPage(page) {
      const nextPage = Math.min(Math.max(page, 0), ticketPageCount.value - 1);
      if (nextPage === ticketPager.page) {
        return;
      }
      ticketPager.page = nextPage;
      reloadData();
    }

    function goPendingPage(page) {
      const nextPage = Math.min(Math.max(page, 0), pendingPageCount.value - 1);
      if (nextPage === pendingPager.page) {
        return;
      }
      pendingPager.page = nextPage;
      reloadPendingApprovals();
    }

    function goRiskPage(page) {
      const nextPage = Math.min(Math.max(page, 0), riskPageCount.value - 1);
      if (nextPage === riskPager.page) {
        return;
      }
      riskPager.page = nextPage;
      reloadRiskEvents();
    }

    async function reloadPendingApprovals() {
      await runWithLoading(async () => {
        applyPage(await fetchPendingApprovals(), pendingPager, pendingTickets);
      }).catch((error) => showMessage(error.message));
    }

    async function reloadRiskEvents() {
      await runWithLoading(async () => {
        applyRiskPage(await fetchRiskEvents());
      }).catch((error) => showMessage(error.message));
    }

    async function login() {
      await runWithLoading(async () => {
        const user = await api("/api/v1/auth/login", {
          method: "POST",
          tenant: false,
          body: loginForm,
        });
        currentUser.value = user;
        writeJson(SESSION_KEY, user);
        showMessage("登录成功");
        await reloadData();
      }).catch((error) => showMessage(error.message));
    }

    async function register() {
      await runWithLoading(async () => {
        const user = await api("/api/v1/auth/register", {
          method: "POST",
          tenant: false,
          body: registerForm,
        });
        currentUser.value = user;
        writeJson(SESSION_KEY, user);
        showMessage("注册成功");
        await reloadData();
      }).catch((error) => showMessage(error.message));
    }

    function logout() {
      currentUser.value = null;
      localStorage.removeItem(SESSION_KEY);
      activeSection.value = "dashboard";
      tickets.value = [];
      pendingTickets.value = [];
      riskEvents.value = [];
    }

    function switchSection(section) {
      activeSection.value = section;
    }

    async function reloadData() {
      if (!currentUser.value) {
        return;
      }

      await runWithLoading(async () => {
        const [listResult, pendingResult, riskResult, summary] = await Promise.all([
          fetchTicketRecords(),
          fetchPendingApprovals(),
          fetchRiskEvents(),
          api("/api/v1/reports/summary"),
        ]);

        applyPage(listResult, ticketPager, tickets);
        applyPage(pendingResult, pendingPager, pendingTickets);
        applyRiskPage(riskResult);
        metrics.total = summary?.ticketCount || 0;
        metrics.pendingAmount = Number(summary?.pendingAmount || 0);
        metrics.riskRate = Math.round(Number(summary?.riskRate || 0) * 100);
        metrics.approved = Number(summary?.approvedCount || 0);
      }).catch((error) => showMessage(error.message));
    }

    function resetTicketForm() {
      Object.assign(ticketForm, emptyTicketForm());
      editingId.value = "";
    }

    function openCreateForm() {
      resetTicketForm();
      showTicketForm.value = true;
    }

    async function editTicket(ticket) {
      await runWithLoading(async () => {
        const detail = await api(`/api/v1/tickets/${ticket.id}`);
        Object.assign(ticketForm, mapTicket(detail));
        editingId.value = ticket.id;
        showTicketForm.value = true;
      }).catch((error) => showMessage(error.message));
    }

    async function saveTicket() {
      await runWithLoading(async () => {
        const payload = toPayload(ticketForm);
        if (editingId.value) {
          await api(`/api/v1/tickets/${editingId.value}`, {
            method: "PUT",
            body: payload,
          });
          showMessage("车票已更新，并同步写入 Redis 与 ES");
        } else {
          await api("/api/v1/tickets", {
            method: "POST",
            body: payload,
          });
          showMessage("车票已新增，并同步写入 Redis 与 ES");
        }
        showTicketForm.value = false;
        resetTicketForm();
        await reloadData();
      }).catch((error) => showMessage(error.message));
    }

    async function updateStatus(ticket, status) {
      const action = {
        已通过: "approve",
        已驳回: "reject",
        待补票: "return",
        已核销: "reimburse",
        异常: "exception",
      }[status];
      if (!action) {
        return;
      }
      await runWithLoading(async () => {
        await api(`/api/v1/approvals/tickets/${ticket.id}/actions`, {
          method: "POST",
          body: { action, comment: `前端操作：${status}` },
        });
        showMessage(`车票已更新为${status}`);
        await reloadData();
      }).catch((error) => showMessage(error.message));
    }

    function removeTicket(ticket) {
      deleteTarget.value = ticket;
    }

    function cancelDelete() {
      deleteTarget.value = null;
    }

    async function confirmRemoveTicket() {
      const ticket = deleteTarget.value;
      if (!ticket) {
        return;
      }
      await runWithLoading(async () => {
        await api(`/api/v1/tickets/${ticket.id}`, { method: "DELETE" });
        showMessage("车票已删除，并清理 Redis 与 ES");
        deleteTarget.value = null;
        await reloadData();
      }).catch((error) => showMessage(error.message));
    }

    async function seedDemoData() {
      await runWithLoading(async () => {
        let created = 0;
        for (const ticket of sampleTickets) {
          try {
            await api("/api/v1/tickets", {
              method: "POST",
              body: toPayload(ticket),
            });
            created++;
          } catch (error) {
            if (!error.message.includes("ticketNo already exists")) {
              throw error;
            }
          }
        }
        showMessage(created ? `已导入 ${created} 条演示数据` : "演示数据已存在");
        await reloadData();
      }).catch((error) => showMessage(error.message));
    }

    async function reindexSearch() {
      await runWithLoading(async () => {
        const count = await api("/api/v1/search/tickets/reindex", { method: "POST" });
        showMessage(`已重建 ${count} 条 ES 索引`);
      }).catch((error) => showMessage(error.message));
    }

    function exportCsv() {
      const rows = [
        ["票号", "员工", "部门", "路线", "车次", "金额", "状态", "风险"],
        ...filteredTickets.value.map((ticket) => [
          ticket.ticketNo,
          ticket.employeeName,
          ticket.department,
          `${ticket.departureCity}-${ticket.arrivalCity}`,
          ticket.carrierNo,
          ticket.amount,
          ticket.status,
          ticket.riskLevel,
        ]),
      ];
      const csv = rows.map((row) => row.join(",")).join("\n");
      const blob = new Blob([`\uFEFF${csv}`], { type: "text/csv;charset=utf-8" });
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = "travel-tickets.csv";
      link.click();
      URL.revokeObjectURL(url);
      showMessage("CSV 已导出");
    }

    function money(value) {
      return new Intl.NumberFormat("zh-CN", {
        style: "currency",
        currency: "CNY",
        maximumFractionDigits: 0,
      }).format(value || 0);
    }

    onMounted(() => {
      if (currentUser.value) {
        reloadData();
      }
    });

    return {
      apiBase,
      activeSection,
      authMode,
      currentUser,
      cancelDelete,
      clearFilters,
      confirmRemoveTicket,
      deleteTarget,
      editableStatuses,
      editableTicketTypes,
      editingId,
      exportCsv,
      filters,
      filteredTickets,
      goPendingPage,
      goRiskPage,
      goTicketPage,
      isDashboard,
      login,
      loginForm,
      loading,
      logout,
      metrics,
      money,
      openCreateForm,
      pendingPageCount,
      pendingPager,
      pendingRangeText,
      pendingTickets,
      register,
      registerForm,
      reindexSearch,
      reloadData,
      reloadRiskEvents,
      resetPendingPageAndReload,
      resetRiskPageAndReload,
      resetAllPagesAndReload,
      resetTicketPageAndReload,
      removeTicket,
      riskPageCount,
      riskPager,
      riskRangeText,
      riskTickets,
      saveTicket,
      seedDemoData,
      sectionHint,
      sectionTitle,
      showApprovalModule,
      showRiskModule,
      showTicketForm,
      showTicketModule,
      statuses,
      switchSection,
      ticketForm,
      ticketPageCount,
      ticketPager,
      ticketRangeText,
      ticketTypes,
      toast,
      updateStatus,
      cities,
      editTicket,
    };
  },
}).mount("#app");
