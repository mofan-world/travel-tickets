const tickets = [
  {
    id: "T20260605001",
    employee: "沈韵",
    department: "华东销售",
    from: "上海",
    to: "北京",
    train: "G12",
    amount: 553,
    status: "待审核",
    risk: "金额接近部门单次上限",
    hours: 8,
  },
  {
    id: "T20260605002",
    employee: "罗启",
    department: "交付中心",
    from: "深圳",
    to: "杭州",
    train: "D2282",
    amount: 468,
    status: "已核销",
    risk: "",
    hours: 18,
  },
  {
    id: "T20260605003",
    employee: "陈伊",
    department: "财务共享",
    from: "北京",
    to: "上海",
    train: "G17",
    amount: 667,
    status: "异常",
    risk: "疑似重复票号",
    hours: 26,
  },
  {
    id: "T20260605004",
    employee: "唐硕",
    department: "北区运营",
    from: "杭州",
    to: "深圳",
    train: "G99",
    amount: 615,
    status: "待补票",
    risk: "缺少电子凭证",
    hours: 31,
  },
  {
    id: "T20260605005",
    employee: "许澈",
    department: "研发平台",
    from: "上海",
    to: "杭州",
    train: "G7355",
    amount: 73,
    status: "已核销",
    risk: "",
    hours: 11,
  },
  {
    id: "T20260605006",
    employee: "袁知",
    department: "大客户部",
    from: "北京",
    to: "深圳",
    train: "G81",
    amount: 944,
    status: "待审核",
    risk: "跨区域高额票",
    hours: 14,
  },
];

const trend = [
  { label: "周一", value: 38 },
  { label: "周二", value: 52 },
  { label: "周三", value: 71 },
  { label: "周四", value: 64 },
  { label: "周五", value: 88 },
  { label: "周六", value: 31 },
  { label: "周日", value: 26 },
];

const state = {
  city: "全部",
  status: "全部",
  query: "",
  range: 7,
};

const nodes = {
  table: document.querySelector("#ticketTable"),
  approvalQueue: document.querySelector("#approvalQueue"),
  riskList: document.querySelector("#riskList"),
  trendChart: document.querySelector("#trendChart"),
  metricTickets: document.querySelector("#metricTickets"),
  metricPending: document.querySelector("#metricPending"),
  metricRisk: document.querySelector("#metricRisk"),
  metricSla: document.querySelector("#metricSla"),
  toast: document.querySelector("#toast"),
  dialog: document.querySelector("#ticketDialog"),
  form: document.querySelector("#ticketForm"),
};

function formatCurrency(value) {
  return new Intl.NumberFormat("zh-CN", {
    style: "currency",
    currency: "CNY",
    maximumFractionDigits: 0,
  }).format(value);
}

function getFilteredTickets() {
  return tickets.filter((ticket) => {
    const cityMatch = state.city === "全部" || ticket.from === state.city || ticket.to === state.city;
    const statusMatch = state.status === "全部" || ticket.status === state.status;
    const text = `${ticket.employee}${ticket.department}${ticket.from}${ticket.to}${ticket.train}${ticket.id}`;
    const queryMatch = text.toLowerCase().includes(state.query.toLowerCase().trim());
    return cityMatch && statusMatch && queryMatch;
  });
}

function statusClass(status) {
  const map = {
    待审核: "pending",
    已核销: "done",
    异常: "risk",
    待补票: "missing",
  };
  return map[status] || "pending";
}

function renderMetrics(rows) {
  const pendingAmount = rows
    .filter((ticket) => ticket.status === "待审核")
    .reduce((sum, ticket) => sum + ticket.amount, 0);
  const riskCount = rows.filter((ticket) => ticket.risk).length;
  const avgHours = rows.length ? Math.round(rows.reduce((sum, ticket) => sum + ticket.hours, 0) / rows.length) : 0;

  nodes.metricTickets.textContent = rows.length.toLocaleString("zh-CN");
  nodes.metricPending.textContent = formatCurrency(pendingAmount);
  nodes.metricRisk.textContent = rows.length ? `${Math.round((riskCount / rows.length) * 100)}%` : "0%";
  nodes.metricSla.textContent = `${avgHours}h`;
}

function renderTable(rows) {
  nodes.table.innerHTML = rows
    .map(
      (ticket) => `
        <tr>
          <td class="employee-cell">
            <strong>${ticket.employee}</strong>
            <small>${ticket.department}</small>
          </td>
          <td class="route-cell">
            <strong>${ticket.from} -> ${ticket.to}</strong>
            <small>${ticket.id}</small>
          </td>
          <td>${ticket.train}</td>
          <td>${formatCurrency(ticket.amount)}</td>
          <td><span class="status ${statusClass(ticket.status)}">${ticket.status}</span></td>
          <td><button class="row-action" data-action="inspect" data-id="${ticket.id}" title="查看详情" aria-label="查看详情">...</button></td>
        </tr>
      `,
    )
    .join("");
}

function renderApprovals(rows) {
  const approvals = rows.filter((ticket) => ticket.status === "待审核").slice(0, 4);
  nodes.approvalQueue.innerHTML =
    approvals
      .map(
        (ticket) => `
          <article class="queue-item">
            <header>
              <strong>${ticket.employee} / ${ticket.department}</strong>
              <span>${formatCurrency(ticket.amount)}</span>
            </header>
            <p>${ticket.from} 到 ${ticket.to}，车次 ${ticket.train}，已等待 ${ticket.hours} 小时。</p>
            <button data-action="approve" data-id="${ticket.id}">通过</button>
          </article>
        `,
      )
      .join("") || `<p class="empty">当前筛选下没有待审核记录。</p>`;
}

function renderRisks(rows) {
  const risks = rows.filter((ticket) => ticket.risk);
  nodes.riskList.innerHTML =
    risks
      .map(
        (ticket) => `
          <article class="risk-item">
            <header>
              <strong>${ticket.employee} / ${ticket.train}</strong>
              <span class="priority">P${ticket.status === "异常" ? 1 : 2}</span>
            </header>
            <p>${ticket.risk}，路线 ${ticket.from} 到 ${ticket.to}。</p>
          </article>
        `,
      )
      .join("") || `<p class="empty">暂无风险项。</p>`;
}

function renderTrend() {
  const scale = Math.max(...trend.map((item) => item.value));
  nodes.trendChart.innerHTML = trend
    .map(
      (item) => `
        <div class="bar" title="${item.label} ${item.value} 万元">
          <span style="height:${Math.max(12, (item.value / scale) * 100)}%"></span>
          <small>${item.label}</small>
        </div>
      `,
    )
    .join("");
}

function render() {
  const rows = getFilteredTickets();
  renderMetrics(rows);
  renderTable(rows);
  renderApprovals(rows);
  renderRisks(rows);
  renderTrend();
}

function showToast(message) {
  nodes.toast.textContent = message;
  nodes.toast.classList.add("is-visible");
  window.setTimeout(() => nodes.toast.classList.remove("is-visible"), 2200);
}

document.querySelector("#cityFilter").addEventListener("change", (event) => {
  state.city = event.target.value;
  render();
});

document.querySelector("#statusFilter").addEventListener("change", (event) => {
  state.status = event.target.value;
  render();
});

document.querySelector("#searchInput").addEventListener("input", (event) => {
  state.query = event.target.value;
  render();
});

document.querySelector(".segmented").addEventListener("click", (event) => {
  const button = event.target.closest("button");
  if (!button) return;
  state.range = Number(button.dataset.range);
  document.querySelectorAll(".segmented button").forEach((item) => item.classList.toggle("is-selected", item === button));
  showToast(`已切换到最近 ${state.range} 天数据`);
});

document.querySelector(".nav").addEventListener("click", (event) => {
  const button = event.target.closest(".nav-item");
  if (!button) return;
  document.querySelectorAll(".nav-item").forEach((item) => item.classList.toggle("is-active", item === button));
  const target = {
    overview: ".metric-grid",
    tickets: ".ticket-panel",
    approvals: ".side-panel",
    risk: ".risk-panel",
  }[button.dataset.view];
  document.querySelector(target)?.scrollIntoView({ behavior: "smooth", block: "start" });
});

document.body.addEventListener("click", (event) => {
  const actionTarget = event.target.closest("[data-action]");
  if (!actionTarget) return;
  const ticket = tickets.find((item) => item.id === actionTarget.dataset.id);
  if (!ticket) return;

  if (actionTarget.dataset.action === "approve") {
    ticket.status = "已核销";
    ticket.risk = "";
    render();
    showToast(`${ticket.employee} 的车票已通过审批`);
  }

  if (actionTarget.dataset.action === "inspect") {
    showToast(`${ticket.employee}，${ticket.from} 到 ${ticket.to}，${ticket.train}，${formatCurrency(ticket.amount)}`);
  }
});

document.querySelector("#openCreateTicket").addEventListener("click", () => {
  nodes.dialog.showModal();
});

document.querySelector("#closeDialog").addEventListener("click", () => {
  nodes.dialog.close();
});

document.querySelector("#cancelTicket").addEventListener("click", () => {
  nodes.dialog.close();
});

document.querySelector("#refreshBtn").addEventListener("click", () => {
  render();
  showToast("数据已刷新");
});

document.querySelector("#exportBtn").addEventListener("click", () => {
  const rows = getFilteredTickets();
  const csv = [
    ["票号", "员工", "部门", "出发", "到达", "车次", "金额", "状态"].join(","),
    ...rows.map((ticket) =>
      [ticket.id, ticket.employee, ticket.department, ticket.from, ticket.to, ticket.train, ticket.amount, ticket.status].join(","),
    ),
  ].join("\n");
  const blob = new Blob([`\uFEFF${csv}`], { type: "text/csv;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = "ticket-export.csv";
  link.click();
  URL.revokeObjectURL(url);
  showToast("已生成当前筛选结果 CSV");
});

nodes.form.addEventListener("submit", (event) => {
  event.preventDefault();
  const data = new FormData(nodes.form);
  const ticket = {
    id: `T${Date.now()}`,
    employee: data.get("employee"),
    department: data.get("department"),
    from: data.get("from"),
    to: data.get("to"),
    train: data.get("train"),
    amount: Number(data.get("amount")),
    status: "待审核",
    risk: "",
    hours: 0,
  };
  tickets.unshift(ticket);
  nodes.form.reset();
  nodes.dialog.close();
  render();
  showToast("新车票已进入审批队列");
});

render();
