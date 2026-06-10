const { createApp, computed, reactive, ref } = window.Vue;

const USERS_KEY = "travel-ticket-users";
const SESSION_KEY = "travel-ticket-session";
const TICKETS_KEY = "travel-ticket-tickets";

const sampleTickets = [
  {
    id: "T20260610001",
    employeeId: "E10086",
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
    riskLevel: "低",
    createdAt: "2026-06-10 09:12",
  },
  {
    id: "T20260610002",
    employeeId: "E20018",
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
    riskLevel: "无",
    createdAt: "2026-06-10 08:30",
  },
  {
    id: "T20260610003",
    employeeId: "E30027",
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
    riskLevel: "高",
    createdAt: "2026-06-09 18:42",
  },
  {
    id: "T20260610004",
    employeeId: "E40032",
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
    riskLevel: "中",
    createdAt: "2026-06-09 16:10",
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

function defaultUsers() {
  return [
    {
      name: "系统管理员",
      company: "示例集团",
      email: "admin@travel.local",
      password: "admin123",
    },
  ];
}

function loadUsers() {
  const users = readJson(USERS_KEY, null);
  if (users) return users;
  const seeded = defaultUsers();
  writeJson(USERS_KEY, seeded);
  return seeded;
}

function loadTickets() {
  const tickets = readJson(TICKETS_KEY, null);
  if (tickets) return tickets;
  writeJson(TICKETS_KEY, sampleTickets);
  return sampleTickets.map((ticket) => ({ ...ticket }));
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

createApp({
  setup() {
    const authMode = ref("login");
    const currentUser = ref(readJson(SESSION_KEY, null));
    const toast = ref("");
    const editingId = ref("");
    const showTicketForm = ref(false);
    const tickets = ref(loadTickets());

    const loginForm = reactive({
      email: "",
      password: "",
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

    const ticketForm = reactive(emptyTicketForm());
    const statuses = ["全部", "待审批", "已通过", "已驳回", "待补票", "异常", "已核销"];
    const ticketTypes = ["全部", "高铁", "动车", "普铁", "城际", "飞机", "其他"];
    const editableStatuses = computed(() => statuses.filter((status) => status !== "全部"));
    const editableTicketTypes = computed(() => ticketTypes.filter((type) => type !== "全部"));

    const cities = computed(() => {
      const values = new Set(["全部"]);
      tickets.value.forEach((ticket) => {
        values.add(ticket.departureCity);
        values.add(ticket.arrivalCity);
      });
      return [...values].filter(Boolean);
    });

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

    const metrics = computed(() => {
      const rows = tickets.value;
      const pending = rows.filter((ticket) => ["待审批", "待补票", "异常"].includes(ticket.status));
      const risk = rows.filter((ticket) => ticket.riskLevel !== "无");
      const approved = rows.filter((ticket) => ["已通过", "已核销"].includes(ticket.status));
      return {
        total: rows.length,
        pendingAmount: pending.reduce((sum, ticket) => sum + Number(ticket.amount || 0), 0),
        riskRate: rows.length ? Math.round((risk.length / rows.length) * 100) : 0,
        approved: approved.length,
      };
    });

    const pendingTickets = computed(() => tickets.value.filter((ticket) => ticket.status === "待审批").slice(0, 5));
    const riskTickets = computed(() => tickets.value.filter((ticket) => ticket.riskLevel !== "无").slice(0, 5));

    function persistTickets() {
      writeJson(TICKETS_KEY, tickets.value);
    }

    function resetTicketForm() {
      Object.assign(ticketForm, emptyTicketForm());
      editingId.value = "";
    }

    function showMessage(message) {
      toast.value = message;
      window.clearTimeout(showMessage.timer);
      showMessage.timer = window.setTimeout(() => {
        toast.value = "";
      }, 2200);
    }

    function login() {
      const users = loadUsers();
      const email = loginForm.email.trim().toLowerCase();
      const found = users.find((user) => user.email === email && user.password === loginForm.password);
      if (!found) {
        showMessage("邮箱或密码不正确");
        return;
      }
      currentUser.value = { name: found.name, company: found.company, email: found.email };
      writeJson(SESSION_KEY, currentUser.value);
      showMessage("登录成功");
    }

    function register() {
      const name = registerForm.name.trim();
      const company = registerForm.company.trim();
      const email = registerForm.email.trim().toLowerCase();

      if (registerForm.password.length < 6) {
        showMessage("密码至少 6 位");
        return;
      }
      const users = loadUsers();
      if (users.some((user) => user.email === email)) {
        showMessage("该邮箱已注册");
        return;
      }
      const user = { name, company, email, password: registerForm.password };
      users.push(user);
      writeJson(USERS_KEY, users);
      currentUser.value = { name: user.name, company: user.company, email: user.email };
      writeJson(SESSION_KEY, currentUser.value);
      showMessage("注册成功");
    }

    function logout() {
      currentUser.value = null;
      localStorage.removeItem(SESSION_KEY);
    }

    function openCreateForm() {
      resetTicketForm();
      showTicketForm.value = true;
    }

    function editTicket(ticket) {
      Object.assign(ticketForm, {
        employeeId: ticket.employeeId,
        employeeName: ticket.employeeName,
        department: ticket.department,
        tripPurpose: ticket.tripPurpose,
        ticketType: ticket.ticketType,
        ticketNo: ticket.ticketNo,
        carrierNo: ticket.carrierNo,
        departureCity: ticket.departureCity,
        arrivalCity: ticket.arrivalCity,
        departureAt: ticket.departureAt,
        seatClass: ticket.seatClass,
        amount: ticket.amount,
        status: ticket.status,
        attachmentStatus: ticket.attachmentStatus,
      });
      editingId.value = ticket.id;
      showTicketForm.value = true;
    }

    function evaluateRisk(ticket) {
      if (ticket.attachmentStatus === "缺失") return "中";
      if (Number(ticket.amount) >= 1200) return "高";
      if (!ticket.departureAt) return "中";
      return "无";
    }

    function saveTicket() {
      const payload = {
        ...ticketForm,
        amount: Number(ticketForm.amount),
        riskLevel: evaluateRisk(ticketForm),
      };

      if (editingId.value) {
        tickets.value = tickets.value.map((ticket) => (ticket.id === editingId.value ? { ...ticket, ...payload } : ticket));
        showMessage("车票已更新");
      } else {
        tickets.value = [
          {
            ...payload,
            id: `T${Date.now()}`,
            createdAt: new Date().toLocaleString("zh-CN", { hour12: false }),
          },
          ...tickets.value,
        ];
        showMessage("车票已新增");
      }
      persistTickets();
      showTicketForm.value = false;
      resetTicketForm();
    }

    function updateStatus(ticket, status) {
      ticket.status = status;
      if (status === "已通过" || status === "已核销") {
        ticket.riskLevel = "无";
      }
      persistTickets();
      showMessage(`车票已更新为${status}`);
    }

    function removeTicket(ticketId) {
      tickets.value = tickets.value.filter((ticket) => ticket.id !== ticketId);
      persistTickets();
      showMessage("车票已删除");
    }

    function resetDemoData() {
      tickets.value = sampleTickets.map((ticket) => ({ ...ticket }));
      persistTickets();
      showMessage("演示数据已重置");
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

    return {
      authMode,
      currentUser,
      editableStatuses,
      editableTicketTypes,
      editingId,
      exportCsv,
      filters,
      filteredTickets,
      login,
      loginForm,
      logout,
      metrics,
      money,
      openCreateForm,
      pendingTickets,
      register,
      registerForm,
      removeTicket,
      resetDemoData,
      riskTickets,
      saveTicket,
      showTicketForm,
      statuses,
      ticketForm,
      ticketTypes,
      toast,
      updateStatus,
      cities,
      editTicket,
    };
  },
}).mount("#app");
