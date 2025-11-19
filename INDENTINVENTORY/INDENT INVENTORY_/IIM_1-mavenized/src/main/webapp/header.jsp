<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isELIgnored="true" %>
<%@ page import="java.util.*, java.util.regex.*, com.iim.models.User, com.iim.models.Alert" %>

<%
    // Session user
    User hdrUser = (User) session.getAttribute("user");
    String hdrUsername = (hdrUser != null) ? hdrUser.getUsername() : "Guest";

    // Data dari filter/servlet (fallback selamat)
    List<Alert> notifList = (List<Alert>) request.getAttribute("notifList");
    if (notifList == null) notifList = Collections.emptyList();
    Integer unreadCountObj = (Integer) request.getAttribute("unreadCount");
    int unreadCount = (unreadCountObj == null) ? 0 : unreadCountObj;

    // Base path
    final String HDR_CTX = request.getContextPath();
%>

<link rel="stylesheet" href="<%= HDR_CTX %>/css/header.css"/>
<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.2/css/all.min.css"/>

<header class="hdr" id="siteHeader">
  <!-- Left: logo -->
  <a class="hdr__logo" href="<%= HDR_CTX %>/home.jsp" aria-label="Go to Home">
    <img src="<%= HDR_CTX %>/assets/img/logo.png" alt="Logo">
  </a>

  <!-- Center: main nav -->
  <nav class="hdr__nav" id="mainNav">
    <a href="<%= HDR_CTX %>/quickscan">Quick Scan</a>
    <a href="<%= HDR_CTX %>/department.jsp">Department</a>
    <a href="<%= HDR_CTX %>/reports">Reports</a>
    <a href="<%= HDR_CTX %>/inventory.jsp">Inventory</a>
    <a href="<%= HDR_CTX %>/log.jsp">Log</a>
    <% if (hdrUser != null && "admin".equalsIgnoreCase(hdrUser.getCategory())) { %>
      <a href="<%= HDR_CTX %>/user.jsp">Manage User</a>
    <% } %>
  </nav>

  <!-- Right: notification + user -->
  <div class="hdr__right">

    <!-- Notifications -->
    <div class="dd-root">
      <button class="iconbtn" aria-label="Notifications" id="notifBtn">
        <i class="fa-solid fa-bell"></i>
        <% if (unreadCount > 0) { %><span class="dot" aria-hidden="true"></span><% } %>
      </button>

      <div class="dd-card dd-notif" role="menu" aria-label="Notifications">
        <div class="dd-title">Notifications</div>

        <div class="dd-list" id="notifList">
          <%
            if (!notifList.isEmpty()) {
              Pattern p = Pattern.compile("Item\\s+\"([^\"]+)\"\\s+is\\s+LOW\\s+STOCK", Pattern.CASE_INSENSITIVE);
              for (Alert a : notifList) {
                String display = a.getTitle();
                Matcher m = p.matcher(display);
                if (m.find()) display = "LOW STOCK \u2013 " + m.group(1);
          %>
            <a class="dd-item" href="<%= HDR_CTX %>/alerts?id=<%= a.getId() %>">
              <i class="fa-solid fa-circle-exclamation"></i>
              <div class="dd-text"><%= display %></div>
            </a>
          <%
              }
            } else {
          %>
            <div class="dd-empty">No alerts</div>
          <%
            }
          %>
        </div>

        <div class="dd-footer">
          <a class="see-more" href="<%= HDR_CTX %>/alerts">See more</a>
        </div>
      </div>
    </div>

    <!-- User -->
    <div class="dd-root">
      <button class="iconbtn" aria-label="User menu">
        <i class="fa-solid fa-user"></i>
      </button>

      <div class="dd-card dd-user" role="menu" aria-label="User menu">
        <div class="dd-title"><%= hdrUsername %></div>
        <a class="dd-item" href="<%= HDR_CTX %>/LogoutServlet">
          <i class="fa-solid fa-right-from-bracket"></i>
          <div class="dd-text">Sign out</div>
        </a>
      </div>
    </div>

  </div>
</header>

<!-- 🔊 preload sounds -->
<audio id="sndNormal" preload="auto">
  <source src="<%= HDR_CTX %>/assets/sound/alert.mp3" type="audio/mpeg">
</audio>
<audio id="sndCritical" preload="auto">
  <source src="<%= HDR_CTX %>/assets/sound/critical.mp3" type="audio/mpeg">
</audio>

<script>
  const CTX = "<%= HDR_CTX %>";

  // Highlight active nav link
  (function () {
    const here = location.pathname.replace(/\/+$/, '');
    document.querySelectorAll('#mainNav a').forEach(a => {
      const href = a.getAttribute('href').replace(location.origin, '').replace(/\/+$/, '');
      if (here.endsWith(href)) a.classList.add('active');
    });
  })();

  // Dropdown open/close
  document.addEventListener('click', (e) => {
    document.querySelectorAll('.dd-root').forEach(r => {
      if (!r.contains(e.target)) r.classList.remove('open');
    });
  });
  document.querySelectorAll('.dd-root > .iconbtn').forEach(btn=>{
    btn.addEventListener('click', (e)=>{
      e.currentTarget.parentElement.classList.toggle('open');
    });
  });

  // Normalize title -> "LOW STOCK – Item"
  function normalizeTitle(t){
    const m = /Item\s+"([^"]+)"\s+is\s+LOW\s+STOCK/i.exec(t);
    return m ? `LOW STOCK \u2013 ${m[1]}` : t;
  }

  // ====== SOUND LOGIC ======
  const NS = CTX + ':alerts';
  function getStore(key, def){
    try { const v = localStorage.getItem(key); return v === null ? def : JSON.parse(v); } catch(e){ return def; }
  }
  function setStore(key, val){ try { localStorage.setItem(key, JSON.stringify(val)); } catch(e){} }

  const sndNormal   = document.getElementById('sndNormal');
  const sndCritical = document.getElementById('sndCritical');

  let audioPrimed = !!getStore(NS + '.primed', false);
  function primeAudio(){
    if (audioPrimed) return;
    try {
      sndNormal.volume = 0;
      const p = sndNormal.play();
      if (p && p.then) {
        p.then(()=>{
          sndNormal.pause();
          sndNormal.currentTime = 0;
          sndNormal.volume = 1;
          audioPrimed = true;
          setStore(NS + '.primed', true);
        }).catch(()=>{});
      }
    } catch(_) {}
  }
  window.addEventListener('click', primeAudio, {once:true});
  window.addEventListener('keydown', primeAudio, {once:true});
  window.addEventListener('touchstart', primeAudio, {once:true});

  // 🔊 play once (normal) or 3× (critical)
  function playChime(sev){
    const isCritical = String(sev||'').toLowerCase() === 'critical';
    const el = isCritical ? sndCritical : sndNormal;
    if (!el) return;

    try {
      el.currentTime = 0;
      el.volume = 1;
      el.play().catch(()=>{});

      if (isCritical) {
        // play 2 more times after 2s and 4s
        setTimeout(() => {
          try { el.currentTime = 0; el.play().catch(()=>{}); } catch(e){}
        }, 2000);
    }
    } catch(e){}
  }

  // ====== POLLING (no cache, 10s interval) ======
  (function poll(){
    fetch(CTX + '/api/alerts-feed', {
      headers: {'Accept':'application/json', 'Cache-Control':'no-cache'},
      cache: 'no-store'
    })
      .then(r=>r.ok ? r.json() : null)
      .then(data=>{
        if(!data) return;

        // Dot indicator
        const btn = document.getElementById('notifBtn');
        if (btn) {
          let dot = btn.querySelector('.dot');
          if (data.unread > 0) {
            if (!dot) { dot = document.createElement('span'); dot.className = 'dot'; btn.appendChild(dot); }
          } else if (dot) { dot.remove(); }
        }

        // List
        const list = document.getElementById('notifList');
        if (list) {
          list.innerHTML = '';
          if (Array.isArray(data.items) && data.items.length) {
            data.items.forEach(it=>{
              const a = document.createElement('a');
              a.className = 'dd-item';
              a.href = CTX + '/alerts?id=' + it.id;
              a.innerHTML = '<i class="fa-solid fa-circle-exclamation"></i><div class="dd-text"></div>';
              a.querySelector('.dd-text').textContent = normalizeTitle(it.title || '');
              list.appendChild(a);
            });
          } else {
            const div = document.createElement('div');
            div.className = 'dd-empty';
            div.textContent = 'No alerts';
            list.appendChild(div);
          }
        }

        // ===== Trigger sound when NEW alert appears =====
        const storedLatest = getStore(NS + '.latest', null);
        const storedUnread = getStore(NS + '.unread', null);
        const nowLatest    = Number(data.latest || 0);
        const nowUnread    = Number(data.unread || 0);

        if (storedLatest === null && storedUnread === null) {
          setStore(NS + '.latest', nowLatest);
          setStore(NS + '.unread', nowUnread);
          return;
        }

        const isNewId = nowLatest > Number(storedLatest || 0);
        const unreadIncreased = nowUnread > Number(storedUnread || 0);

        if (isNewId || unreadIncreased) {
          let sev = 'normal';
          if (Array.isArray(data.items)) {
            const newest = data.items.find(x => Number(x.id) === nowLatest);
            if (newest && newest.severity) sev = String(newest.severity).toLowerCase();
            else if (data.items.some(x => String(x.severity||'').toLowerCase()==='critical')) sev = 'critical';
          }
          playChime(sev);
        }

        setStore(NS + '.latest', nowLatest);
        setStore(NS + '.unread', nowUnread);
      })
      .catch(()=>{})
      .finally(()=> setTimeout(poll, 10000));
  })();
</script>
