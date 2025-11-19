<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isELIgnored="true" %>
<%@ page import="java.util.*, java.text.SimpleDateFormat, java.util.regex.*, com.iim.models.Alert, com.iim.models.User" %>
<%
  User me = (User) session.getAttribute("user");
  if (me == null) { response.sendRedirect("login.jsp"); return; }

  if (request.getAttribute("alerts") == null &&
      request.getAttribute("selected") == null &&
      request.getAttribute("page") == null) {
    response.sendRedirect(request.getContextPath() + "/alerts");
    return;
  }

  List<Alert> alerts = (List<Alert>) request.getAttribute("alerts");
  if (alerts == null) alerts = new ArrayList<>();
  Alert selected = (Alert) request.getAttribute("selected");
  Integer pageObj  = (Integer) request.getAttribute("page");
  Integer pagesObj = (Integer) request.getAttribute("pages");
  String sort   = (String) request.getAttribute("sort");
  String filter = (String) request.getAttribute("filter");

  int pageNo = (pageObj == null) ? 1 : pageObj;
  int pages  = (pagesObj == null) ? 1 : Math.max(1, pagesObj);
  if (sort == null) sort = "latest";
  if (filter == null) filter = "all";
  if (selected == null && !alerts.isEmpty()) selected = alerts.get(0);

  Boolean hasPrevObj = (Boolean) request.getAttribute("hasPrev");
  Boolean hasNextObj = (Boolean) request.getAttribute("hasNext");
  boolean hasPrev = (hasPrevObj != null) ? hasPrevObj : (pageNo > 1);
  boolean hasNext = (hasNextObj != null) ? hasNextObj : (pageNo < pages);

  SimpleDateFormat dfList   = new SimpleDateFormat("dd MMM yyyy, HH:mm");
  SimpleDateFormat dfDetail = new SimpleDateFormat("dd MMM yyyy (EEE) HH:mm");
  TimeZone tz = TimeZone.getTimeZone("Asia/Kuala_Lumpur");
  dfList.setTimeZone(tz);
  dfDetail.setTimeZone(tz);

  String dItemName = "", dBarcode = "", dCurrQty = "", dMinQty = "", dNote = "";
  if (selected != null && selected.getDetail() != null) {
    String det = selected.getDetail();
    Matcher m1 = Pattern.compile("Item\\s+\"([^\"]+)\"").matcher(det);
    if (m1.find()) dItemName = m1.group(1);
    Matcher m2 = Pattern.compile("\\(Barcode:\\s*([\\w-]+)\\)").matcher(det);
    if (m2.find()) dBarcode = m2.group(1);
    Matcher m3 = Pattern.compile("Current qty:\\s*(\\d+)").matcher(det);
    if (m3.find()) dCurrQty = m3.group(1);
    Matcher m4 = Pattern.compile("(Min qty|Critical min):\\s*(\\d+)").matcher(det);
    if (m4.find()) dMinQty = m4.group(2);
    Matcher m5 = Pattern.compile("(Immediate.*|Please.*)$").matcher(det);
    if (m5.find()) dNote = m5.group(1);
  }

  String CTX = request.getContextPath();
%>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8"/>
  <title>Alerts — Indent Inventory</title>
  <meta name="viewport" content="width=device-width, initial-scale=1"/>
  <meta name="theme-color" content="#5E7658"/>
  <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.2/css/all.min.css"/>

  <link rel="stylesheet" href="<%= CTX %>/css/theme.css"/>
  <link rel="stylesheet" href="<%= CTX %>/css/header.css"/>
  <link rel="stylesheet" href="<%= CTX %>/css/footer.css"/>
  <link rel="stylesheet" href="<%= CTX %>/css/alerts.css?v=9"/>

  <link rel="icon" type="image/svg+xml"
        href="data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'%3E%3Crect width='8' height='100' x='5' fill='black'/%3E%3Crect width='5' height='100' x='20' fill='black'/%3E%3Crect width='10' height='100' x='30' fill='black'/%3E%3Crect width='6' height='100' x='50' fill='black'/%3E%3Crect width='8' height='100' x='65' fill='black'/%3E%3Crect width='5' height='100' x='80' fill='black'/%3E%3C/svg%3E">
</head>
<body>
  <%@ include file="header.jsp" %>

  <main class="page" id="pageRoot">
    <div class="content">
      <h1 class="page-title">Alerts</h1>

      <div class="alerts-wrap">
        <!-- LEFT: LIST -->
        <section class="list-pane">

          <form class="toolbar" method="get" action="<%= CTX %>/alerts">
            <div class="left-tools">
              <label class="chk" title="Select all">
                <input type="checkbox" id="checkAll"><span></span>
              </label>
              <button type="button" data-action="delete" class="icon-btn danger" title="Delete selected">
                <i class="fa-solid fa-trash"></i>
              </button>
              <button type="button" data-action="markRead" class="icon-btn" title="Mark as read">
                <i class="fa-solid fa-envelope-open-text"></i>
              </button>
              <button type="button" data-action="markUnread" class="icon-btn" title="Mark as unread">
                <i class="fa-regular fa-envelope"></i>
              </button>
            </div>

            <div class="right-tools">
              <input type="hidden" name="page" value="<%= pageNo %>"/>
              <label class="fld"><span>Filter</span>
                <select name="filter" onchange="this.form.submit()">
                  <option value="all"    <%= "all".equals(filter)    ? "selected":"" %>>All</option>
                  <option value="unread" <%= "unread".equals(filter) ? "selected":"" %>>Unread</option>
                  <option value="read"   <%= "read".equals(filter)   ? "selected":"" %>>Read</option>
                </select>
              </label>
              <label class="fld"><span>Sort</span>
                <select name="sort" onchange="this.form.submit()">
                  <option value="latest" <%= "latest".equals(sort) ? "selected":"" %>>Latest</option>
                  <option value="oldest" <%= "oldest".equals(sort) ? "selected":"" %>>Oldest</option>
                  <option value="title"  <%= "title".equals(sort)  ? "selected":"" %>>Title</option>
                </select>
              </label>
            </div>
          </form>

          <form id="listForm" method="post" action="<%= CTX %>/alerts">
            <input type="hidden" name="action" value="">
            <input type="hidden" name="page"   value="<%= pageNo %>">
            <input type="hidden" name="sort"   value="<%= sort %>">
            <input type="hidden" name="filter" value="<%= filter %>">

            <ul class="items">
              <% if (alerts.isEmpty()) { %>
                <li class="empty">No alerts.</li>
              <% } else {
                   for (Alert a : alerts) {
                     String base = a.isRead() ? "row read" : "row unread";
                     String sev  = ("critical".equalsIgnoreCase(a.getSeverity()) ? "critical" : "normal");
                     String cls  = base + " " + sev;

                     String rawTitle = a.getTitle() == null ? "" : a.getTitle();
                     // If DAO has already prefixed REMINDER!/CRITICAL/LOW, just show raw title safely
              %>
                <li class="<%= cls %>">
                  <label class="chk"><input type="checkbox" name="ids" value="<%= a.getId() %>"><span></span></label>
                  <a class="title" href="<%= CTX %>/alerts?id=<%= a.getId() %>&sort=<%= sort %>&filter=<%= filter %>&page=<%= pageNo %>">
                    <%= rawTitle %>
                  </a>
                  <time><%= dfList.format(a.getCreatedAt()) %></time>
                </li>
              <%   }
                 } %>
            </ul>
          </form>

          <nav class="pager">
            <% if (hasPrev) { %>
              <a class="nav prev" href="<%= CTX %>/alerts?page=<%= pageNo-1 %>&sort=<%= sort %>&filter=<%= filter %>">&laquo; Prev</a>
            <% } else { %>
              <span class="disabled">&laquo; Prev</span>
            <% } %>

            <span class="pageno">Page <%= pageNo %> / <%= pages %></span>

            <% if (hasNext) { %>
              <a class="nav next" href="<%= CTX %>/alerts?page=<%= pageNo+1 %>&sort=<%= sort %>&filter=<%= filter %>">Next &raquo;</a>
            <% } else { %>
              <span class="disabled">Next &raquo;</span>
            <% } %>
          </nav>
        </section>

        <!-- RIGHT: DETAIL -->
        <section class="detail-pane">
          <% if (selected == null) { %>
            <div class="no-select">Select an alert from the list.</div>
          <% } else {
               String sev = (selected.getSeverity()==null) ? "normal" : selected.getSeverity();
               boolean selReminder = selected.getTitle() != null && selected.getTitle().toUpperCase().startsWith("REMINDER!");
               String head = ("critical".equalsIgnoreCase(sev) ? "CRITICAL STOCK – " : "LOW STOCK – ");
          %>
            <header class="detail-head">
              <div>
                <h2 class="detail-title <%= sev %>"><%= (selReminder? "REMINDER! " : "") + head %><%= dItemName.isEmpty()? selected.getTitle() : dItemName %></h2>
              </div>
              <div class="menu">
                <form method="post" action="<%= CTX %>/alerts">
                  <input type="hidden" name="ids" value="<%= selected.getId() %>"/>
                  <input type="hidden" name="page"   value="<%= pageNo %>"/>
                  <input type="hidden" name="sort"   value="<%= sort %>"/>
                  <input type="hidden" name="filter" value="<%= filter %>"/>
                  <button class="icon-btn" name="action" value="markUnread" title="Mark as unread">
                    <i class="fa-regular fa-envelope"></i>
                  </button>
                  <button class="icon-btn danger" name="action" value="delete" title="Delete">
                    <i class="fa-solid fa-trash"></i>
                  </button>
                </form>
              </div>
            </header>

            <div class="meta meta-compact">
              <div class="meta-row">
                <span>Alert Type</span>
                <b class="sev-badge <%= sev %>"><%= "critical".equalsIgnoreCase(sev) ? "Critical alert" : "Normal alert" %></b>
              </div>
              <div class="meta-row">
                <span>Created</span>
                <time><%= dfDetail.format(selected.getCreatedAt()) %></time>
              </div>
            </div>

            <section class="summary">
              <div class="kv">
                <div><label>Item</label><b><%= dItemName.isEmpty()? "-" : dItemName %></b></div>
                <div><label>Barcode</label><span><%= dBarcode.isEmpty()? "-" : dBarcode %></span></div>
                <div><label>Current Qty</label><span class="qty now"><%= dCurrQty.isEmpty()? "-" : dCurrQty %></span></div>
                <div><label><%= "critical".equalsIgnoreCase(sev) ? "Critical Min" : "Minimum Qty" %></label><span class="qty min"><%= dMinQty.isEmpty()? "-" : dMinQty %></span></div>
              </div>
            </section>

            <article class="detail-body">
              <% if (dNote != null && !dNote.isEmpty()) { %>
                <p class="note"><i class="fa-solid fa-circle-exclamation"></i> <%= dNote %></p>
              <% } %>
            </article>

            <div class="snooze cta-row">
              <button type="button" class="btn" id="btnSnoozeOpen">Remind Later</button>
            </div>
          <% } %>
        </section>
      </div>
    </div>
  </main>

  <%@ include file="footer.jsp" %>

  <!-- Snooze Modal -->
  <div class="modal hidden" id="snoozeModal" aria-hidden="true">
    <div class="modal-card" role="dialog" aria-modal="true" aria-labelledby="snoozeTitle">
      <div class="modal-head">
        <h3 id="snoozeTitle">Remind Later</h3>
        <button class="icon-btn" id="snoozeClose" title="Close"><i class="fa-solid fa-xmark"></i></button>
      </div>
      <form method="post" action="<%= CTX %>/alerts" id="snoozeForm">
        <input type="hidden" name="action" value="snooze">
        <input type="hidden" name="id" value="<%= (selected!=null)?selected.getId():0 %>">
        <input type="hidden" name="page"   value="<%= pageNo %>"/>
        <input type="hidden" name="sort"   value="<%= sort %>"/>
        <input type="hidden" name="filter" value="<%= filter %>"/>

        <div class="modal-body grid2">
          <label>Date
            <input type="date" name="remind_at_date" id="snoozeDate" required>
          </label>
          <label>Time
            <input type="time" name="remind_at_time" id="snoozeTime" required>
          </label>
        </div>

        <div class="modal-actions">
          <button type="button" class="btn ghost" id="snoozeCancel">Cancel</button>
          <button class="btn">Save</button>
        </div>
      </form>
    </div>
  </div>

  <script>
    (function(){
      const header = document.getElementById('siteHeader') || document.querySelector('header');
      const footer = document.getElementById('siteFooter') || document.querySelector('footer');
      function setMainHeight(){
        const hh = header ? header.offsetHeight : 0;
        const fh = footer ? footer.offsetHeight : 0;
        const h  = Math.max(320, window.innerHeight - hh - fh);
        document.documentElement.style.setProperty('--main-h', h + 'px');
      }
      setMainHeight(); window.addEventListener('resize', setMainHeight);
    })();

    const checkAll = document.getElementById('checkAll');
    if (checkAll) checkAll.addEventListener('change', () => {
      document.querySelectorAll('.items input[type="checkbox"]').forEach(c => c.checked = checkAll.checked);
    });

    document.querySelectorAll('.toolbar [data-action]').forEach(btn=>{
      btn.addEventListener('click', (e)=>{
        e.preventDefault();
        const form = document.getElementById('listForm');
        const any = form.querySelector('input[name="ids"]:checked');
        if(!any){ alert('Please select at least one alert.'); return; }
        form.querySelector('input[name="action"]').value = btn.dataset.action;
        form.submit();
      });
    });

    const modal = document.getElementById('snoozeModal');
    const btnOpen = document.getElementById('btnSnoozeOpen');
    const btnClose = document.getElementById('snoozeClose');
    const btnCancel = document.getElementById('snoozeCancel');
    const inputDate = document.getElementById('snoozeDate');
    const inputTime = document.getElementById('snoozeTime');

    function openModal() {
      const now = new Date();
      now.setMinutes(now.getMinutes() + 5);
      let m = Math.ceil(now.getMinutes()/5)*5;
      if (m === 60) { now.setHours(now.getHours()+1); m = 0; }
      const y  = now.getFullYear();
      const mo = String(now.getMonth()+1).padStart(2,'0');
      const d  = String(now.getDate()).padStart(2,'0');
      const hh = String(now.getHours()).padStart(2,'0');
      const mm = String(m).padStart(2,'0');
      inputDate.value = `${y}-${mo}-${d}`;
      inputDate.min   = `${y}-${mo}-${d}`;
      inputTime.value = `${hh}:${mm}`;
      modal.classList.remove('hidden'); modal.setAttribute('aria-hidden','false');
    }
    function closeModal(){ modal.classList.add('hidden'); modal.setAttribute('aria-hidden','true'); }
    if (btnOpen)   btnOpen.addEventListener('click', openModal);
    if (btnClose)  btnClose.addEventListener('click', closeModal);
    if (btnCancel) btnCancel.addEventListener('click', closeModal);
    modal.addEventListener('click', (e)=>{ if(e.target === modal) closeModal(); });
  </script>
</body>
</html>
