<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.*, com.iim.models.*, com.iim.dao.*" %>
<%@ page isELIgnored="true" %>
<%
  com.iim.models.User me = (com.iim.models.User) session.getAttribute("user");
  if (me == null) { response.sendRedirect("login.jsp"); return; }

  List<User> users = new UserDAO().listAll();
  String errMsg = (String) request.getAttribute("error"); // may be null
%>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <title>Indent Inventory</title>
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <meta name="theme-color" content="#94765e"/>

  <!-- Theme + chrome -->
  <link href="<%=request.getContextPath()%>/css/theme.css" rel="stylesheet"/>
  <link href="<%=request.getContextPath()%>/css/header.css" rel="stylesheet"/>
  <link href="<%=request.getContextPath()%>/css/footer.css" rel="stylesheet"/>

  <!-- Page skin (synced to Department) -->
  <link href="<%=request.getContextPath()%>/css/users_deptstyle.css?v=3" rel="stylesheet"/>

  <link rel="stylesheet"
        href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.2/css/all.min.css"/>
  <link rel="icon" type="image/svg+xml"
        href="data:image/svg+xml,
        <svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'>
          <rect width='8' height='100' x='5' fill='black'/>
          <rect width='5' height='100' x='20' fill='black'/>
          <rect width='10' height='100' x='30' fill='black'/>
          <rect width='6' height='100' x='50' fill='black'/>
          <rect width='8' height='100' x='65' fill='black'/>
          <rect width='5' height='100' x='80' fill='black'/>
        </svg>">

  <style>
    :root{
      --ink:#3C3A37; --ink-dim:#7B746D; --ink-strong:#4D453F;
      --divider:#E7E4DD; --btn:#94765E; --bg:#ffffff; --r-lg:22px;
      --shadow:0 12px 36px rgba(50,29,11,.10), 0 6px 18px rgba(50,29,11,.06);
      --shadow-lg:0 18px 48px rgba(50,29,11,.16), 0 8px 24px rgba(50,29,11,.09);
      --easing:cubic-bezier(.2,.6,.2,1);
    }
    /* Backdrop */
    .modal-backdrop{
      position:fixed; inset:0; z-index:1000;
      display:none; align-items:center; justify-content:center;
      background:rgba(20,14,7,.30);
      backdrop-filter: blur(2px);
      animation: fadeIn .15s var(--easing);
    }
    .modal-backdrop.show{ display:flex; }

    /* Card */
    .modal-card{
      width:min(520px, 92vw);
      background:#fff;
      border:1px solid var(--divider);
      border-radius:24px;
      box-shadow:var(--shadow-lg);
      padding:20px 20px 16px;
      transform: translateY(6px) scale(.98);
      opacity:0;
      animation: popIn .18s var(--easing) forwards;
    }
    .modal-head{
      display:flex; align-items:center; gap:12px; margin-bottom:10px;
    }
    .modal-title{
      margin:0; font-family:"Fraunces",serif; font-weight:900;
      font-variation-settings:"opsz" 96;
      font-size:clamp(18px,2.2vw,22px); color:var(--ink-strong);
      letter-spacing:.01em;
    }
    .modal-icon{
      width:36px; height:36px; border-radius:12px;
      display:grid; place-items:center; font-size:18px; flex:0 0 36px;
      color:#b84242; background:#f8eaea; border:1px solid #efdede;
    }
    .modal-body{
      color:var(--ink); line-height:1.5; font-size:15px;
      padding:6px 2px 2px;
      word-wrap:break-word; white-space:pre-wrap;
    }
    .modal-actions{
      display:flex; gap:10px; justify-content:flex-end; margin-top:16px;
    }
    .btn{ height:42px; border-radius:999px; padding:0 16px; font-weight:800; border:none; cursor:pointer; }
    .btn-primary{ color:#fff; background:#94765e; box-shadow:0 10px 24px rgba(148,118,94,.25); }
    .btn-primary:hover{ filter:brightness(1.03); transform:translateY(-1px); box-shadow:0 12px 28px rgba(148,118,94,.32); }
    .btn-ghost{ background:#cebfac; color:#4d453f; box-shadow:0 6px 16px rgba(0,0,0,.06); }
    .btn-ghost:hover{ filter:brightness(1.04); }

    @keyframes popIn{ to{ transform:translateY(0) scale(1); opacity:1 } }
    @keyframes fadeIn{ from{ opacity:0 } to{ opacity:1 } }

    /* Focus outline for accessibility */
    .btn:focus-visible{ outline:3px solid rgba(148,118,94,.4); outline-offset:2px }
  </style>
</head>
<body class="layout">
  <%@ include file="header.jsp" %>

  <main class="page" id="page">
    <h1 class="page-title">Manage User</h1>

    <section class="user-wrap">
      <!-- Left: list -->
      <aside class="user-list">
        <div class="search-pill">
          <i class="fa-solid fa-magnifying-glass"></i>
          <input id="q" class="search-input" placeholder="Search by username or category…" autocomplete="off"/>
          <button id="btnSearch" class="search-btn" type="button" title="Search"></button>
        </div>

        <div class="tbl-head">
          <span class="col-cat">CATEGORY</span>
          <span class="col-usr">USERNAME</span>
          <span class="col-sta">STATUS</span>
        </div>

        <div id="list" class="tbl-body" role="listbox" aria-label="Users">
          <% if (users != null) {
               for (User u : users) {
                 String st = (u.getStatus()==null?"":u.getStatus());
                 String stLower = st.toLowerCase();
                 String stPretty = stLower.isEmpty()? "" : (Character.toUpperCase(stLower.charAt(0)) + stLower.substring(1));
          %>
            <button class="row" type="button"
                    data-id="<%= u.getId() %>"
                    data-category="<%= u.getCategory()==null? "" : u.getCategory() %>"
                    data-username="<%= u.getUsername()==null? "" : u.getUsername() %>"
                    data-status="<%= stLower %>"
                    data-description="<%= u.getDescription()==null? "" : u.getDescription().replace("\"","&quot;") %>">
              <span class="col-cat" title="<%= u.getCategory() %>"><%= u.getCategory() %></span>
              <span class="col-usr" title="<%= u.getUsername() %>"><%= u.getUsername() %></span>
              <span class="col-sta <%= "active".equals(stLower) ? "ok" : "off" %>"><%= stPretty %></span>
            </button>
          <% } } %>
        </div>
      </aside>

      <!-- Right: detail -->
      <section class="user-detail">
        <div class="detail-topbar">
          <div class="toolbar">
            <button id="btnAdd" class="btn btn-outline" type="button">Add</button>
            <button id="btnUpdate" class="btn btn-outline" type="button" disabled>Update</button>
            <button id="btnSave" class="btn btn-primary" type="submit" form="userForm" disabled>Save</button>
            <button id="btnCancel" class="btn btn-ghost" type="button" disabled>Cancel</button>
          </div>
        </div>

        <div class="detail-body">
          <div class="card">
            <form id="userForm" method="post" action="users" autocomplete="off" novalidate>
              <input type="hidden" name="action" id="formAction" value="update"/>
              <input type="hidden" name="id" id="id" value=""/>

              <div class="form-row">
                <label class="lbl" for="category">Category <span class="req" aria-hidden="true">*</span></label>
                <input id="category" name="category" class="input" type="text" maxlength="50" required disabled/>
              </div>

              <div class="form-row">
                <label class="lbl" for="username">Username <span class="req" aria-hidden="true">*</span></label>
                <input id="username" name="username" class="input" type="text" maxlength="50" required disabled/>
              </div>

              <div class="form-row">
                <label class="lbl" for="password">Password <span id="pwdReq" class="req" aria-hidden="true" style="display:none">*</span></label>
                <div class="stack">
                  <input id="password" name="password" class="input" type="password" placeholder="" disabled/>
                  <div id="pwdHint" class="hint" style="display:none">Leave blank to keep current password</div>
                </div>
              </div>

              <div class="form-row">
                <label class="lbl">Status <span class="req" aria-hidden="true">*</span></label>
                <div class="status-toggle" id="statusWrap">
                  <select id="status" name="status" class="input" required disabled>
                    <option value="">—</option>
                    <option value="active">Active</option>
                    <option value="inactive">Inactive</option>
                  </select>
                  <button id="statusToggle" type="button" class="status-btn" title="Toggle" disabled></button>
                </div>
              </div>

              <div class="form-row">
                <label class="lbl" for="description">Description</label>
                <input id="description" name="description" class="input" type="text" maxlength="255" disabled/>
              </div>
            </form>

            <p id="emptyHint" class="hint">
              Select a user on the left, or click <b>Add</b> to create a new one.
            </p>
          </div>
        </div>
      </section>
    </section>
  </main>

  <%@ include file="footer.jsp" %>

  <!-- Nice modal (replaces default alert) -->
  <div id="niceModal" class="modal-backdrop" role="dialog" aria-modal="true" aria-labelledby="niceModalTitle" aria-hidden="true">
    <div class="modal-card" role="document">
      <div class="modal-head">
        <div class="modal-icon"><i class="fa-solid fa-triangle-exclamation"></i></div>
        <h2 id="niceModalTitle" class="modal-title">Notice</h2>
      </div>
      <div id="niceModalMsg" class="modal-body">Message here</div>
      <div class="modal-actions">
        <button id="niceModalOk" type="button" class="btn btn-primary">OK</button>
      </div>
    </div>
  </div>

<script>
(function fitToPage(){
  const root = document.documentElement;
  function applySize(){
    const hdr = document.querySelector('header.hdr');
    const ftr = document.querySelector('footer');
    const hHdr = hdr ? hdr.getBoundingClientRect().height : 0;
    const hFtr = ftr ? ftr.getBoundingClientRect().height : 0;
    root.style.setProperty('--main-h', (window.innerHeight - hHdr - hFtr) + 'px');
  }
  applySize();
  window.addEventListener('resize', applySize, {passive:true});
})();
</script>

<script>
/* ========= Nice alert utility ========= */
(function(){
  const modal = document.getElementById('niceModal');
  const msgEl = document.getElementById('niceModalMsg');
  const okBtn = document.getElementById('niceModalOk');
  const titleEl = document.getElementById('niceModalTitle');
  let onClose = null;
  let lastFocus = null;

  function openModal(text, title){
    if(title) titleEl.textContent = title; else titleEl.textContent = 'Notice';
    msgEl.textContent = text;
    lastFocus = document.activeElement;
    modal.classList.add('show');
    modal.removeAttribute('aria-hidden');
    // focus trap to OK
    okBtn.focus();
    document.addEventListener('keydown', escClose, true);
  }
  function closeModal(){
    modal.classList.remove('show');
    modal.setAttribute('aria-hidden','true');
    document.removeEventListener('keydown', escClose, true);
    if (lastFocus && lastFocus.focus) { try{ lastFocus.focus(); }catch(e){} }
    if(typeof onClose === 'function'){ const cb = onClose; onClose=null; cb(); }
  }
  function escClose(e){ if(e.key === 'Escape'){ e.preventDefault(); closeModal(); } }
  okBtn.addEventListener('click', closeModal);
  modal.addEventListener('click', (e) => {
    if (e.target === modal) closeModal();
  });

  // public API
  window.niceAlert = function(text, title, cb){
    onClose = (typeof cb === 'function') ? cb : null;
    openModal(String(text || ''), title);
  };

  // simple confirm scaffold (not used now, but handy):
  window.niceConfirm = function(opts){
    // opts: {title, message, okText, cancelText, onOk, onCancel}
    // can be extended if you need later
  };
})();
</script>

<script>
(function(){
  const $ = (s, p=document) => p.querySelector(s);
  const listEl = $('#list');
  const qEl = $('#q');
  const btnSearch = $('#btnSearch');

  // form fields
  const form = $('#userForm');
  const idEl = $('#id');
  const actionEl = $('#formAction');
  const catEl = $('#category');
  const usrEl = $('#username');
  const pwdEl = $('#password');
  const staEl = $('#status');
  const staBtn = $('#statusToggle');
  const descEl = $('#description');
  const pwdHint = $('#pwdHint');
  const pwdReq  = $('#pwdReq');

  const btnAdd = $('#btnAdd'), btnUpdate = $('#btnUpdate'), btnSave = $('#btnSave'), btnCancel = $('#btnCancel');
  const emptyHint = $('#emptyHint');

  let current = null; // selected row
  let mode = 'view';

  const setDisabled = (flag) => {
    [catEl, usrEl, pwdEl, staEl, descEl].forEach(e => e.disabled = flag);
    staBtn.disabled = flag;
  };

  function setMode(next){
    mode = next;
    const editing = (mode==='add' || mode==='update');
    btnAdd.disabled = editing;
    btnUpdate.disabled = !current || editing;
    btnSave.disabled = !editing;
    btnCancel.disabled = !editing;
    setDisabled(!editing);
    emptyHint.style.display = (current || editing) ? 'none' : 'block';
    actionEl.value = (mode==='add') ? 'add' : 'update';

    // password UX
    if (mode === 'add') {
      pwdHint.style.display = 'none';
      pwdReq.style.display  = '';
      pwdEl.placeholder = 'Enter password';
      pwdEl.required = true;
      staEl.required = true;
      catEl.focus();
    } else if (mode === 'update') {
      pwdHint.style.display = '';
      pwdReq.style.display  = 'none';
      pwdEl.placeholder = '';
      pwdEl.required = false;
      staEl.required = true;
      usrEl.focus();
    } else {
      pwdHint.style.display = 'none';
      pwdReq.style.display  = 'none';
      pwdEl.required = false;
    }
  }

  function clearSelection(){ [...listEl.querySelectorAll('.row.active')].forEach(x=>x.classList.remove('active')); }

  function fillForm(obj){
    idEl.value = obj?.id || '';
    catEl.value = obj?.category || '';
    usrEl.value = obj?.username || '';
    pwdEl.value = ''; // always blank
    staEl.value = obj?.status || '';
    descEl.value = obj?.description || '';
    renderStatusBtn();
  }

  function renderStatusBtn(){
    const v = String(staEl.value || '');
    if(!v){ staBtn.className='status-btn'; staBtn.innerHTML=''; return; }
    const on = v.toLowerCase()==='active';
    staBtn.className = 'status-btn ' + (on?'active':'inactive');
    staBtn.innerHTML = on ? '<i class="fa-solid fa-check"></i>' : '<i class="fa-solid fa-xmark"></i>';
  }
  staBtn.addEventListener('click', () => {
    if (staBtn.disabled) return;
    staEl.value = (staEl.value==='active') ? 'inactive' : 'active';
    renderStatusBtn();
  });
  staEl.addEventListener('change', renderStatusBtn);

  function selectRow(row){
    clearSelection(); row.classList.add('active');
    current = {
      id: row.dataset.id,
      category: row.dataset.category,
      username: row.dataset.username,
      status: row.dataset.status,
      description: row.dataset.description
    };
    fillForm(current);
    setMode('view');
  }
  [...listEl.querySelectorAll('.row')].forEach(r => r.addEventListener('click', () => selectRow(r)));

  // Toolbar
  btnAdd.addEventListener('click', () => {
    current = null; clearSelection();
    fillForm({id:'', category:'', username:'', status:'', description:''});
    setMode('add');
  });
  btnUpdate.addEventListener('click', () => {
    if(!current) return;
    setMode('update');
  });
  btnCancel.addEventListener('click', () => {
    if(mode==='add'){ current=null; fillForm({}); clearSelection(); }
    else if(mode==='update'){ fillForm(current); }
    setMode('view');
  });

  // Client validation before submit
  form.addEventListener('submit', (e) => {
    const m = actionEl.value;
    const missing = [];
    const v = s => (s==null? '' : String(s).trim());

    if (m === 'add') {
      if (!v(catEl.value)) missing.push('Category');
      if (!v(usrEl.value)) missing.push('Username');
      if (!v(pwdEl.value)) missing.push('Password');
      if (!v(staEl.value)) missing.push('Status');
    } else if (m === 'update') {
      if (!v(catEl.value)) missing.push('Category');
      if (!v(usrEl.value)) missing.push('Username');
      if (!v(staEl.value)) missing.push('Status');
    }
    if (missing.length) {
      e.preventDefault();
      niceAlert('Please fill in: ' + missing.join(', '), 'Incomplete');
      return;
    }

    // ===== Admin soft-guard on ADD (popup + block) =====
    if (m === 'add') {
      if ((catEl.value || '').trim().toLowerCase() === 'admin') {
        e.preventDefault();
        niceAlert('There must be only 1 admin. Please choose another category/role.', 'Admin limit');
        return;
      }
    }
  });

  // Search (client-side filter)
  function matches(row, q){
    if(!q) return true;
    q = q.toLowerCase();
    const cat = (row.dataset.category || '').toLowerCase();
    const usr = (row.dataset.username || '').toLowerCase();
    return cat.includes(q) || usr.includes(q);
  }
  function filterList(){
    const q = qEl.value.trim().toLowerCase();
    const rows = [...listEl.querySelectorAll('.row')];
    let visible = 0;
    rows.forEach(r => {
      const ok = matches(r, q);
      r.style.display = ok ? '' : 'none';
      if(ok) visible++;
    });
    if(visible===0){
      if(!listEl.querySelector('.empty')){
        const e=document.createElement('div'); e.className='empty'; e.textContent='No users found';
        listEl.appendChild(e);
      }
    } else {
      const emp = listEl.querySelector('.empty'); if(emp) emp.remove();
    }
  }
  let t=null;
  qEl.addEventListener('input', ()=>{ clearTimeout(t); t=setTimeout(filterList, 200); });
  btnSearch.addEventListener('click', filterList);
  qEl.addEventListener('keydown', e=>{ if(e.key==='Enter'){ e.preventDefault(); filterList(); } });

  // boot
  setMode('view');

  // If server passed an error (e.g., second admin attempt), show it nicely
  <% if (errMsg != null && !errMsg.trim().isEmpty()) {
       String safe = errMsg.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n");
  %>
    niceAlert("<%= safe %>", "Admin limit");
  <% } %>
})();
</script>

</body>
</html>
