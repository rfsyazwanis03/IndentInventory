<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.*, com.iim.models.*, com.iim.dao.*" %>
<%@ page isELIgnored="true" %>
<%
  com.iim.models.User me = (com.iim.models.User) session.getAttribute("user");
  if (me == null) { response.sendRedirect("login.jsp"); return; }

  List<Department> deps = (List<Department>) request.getAttribute("departments");
  String today = new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date());
%>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <title>Indent Inventory</title>
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <meta name="theme-color" content="#94765e"/>

  <link href="<%=request.getContextPath()%>/css/theme.css" rel="stylesheet"/>
  <link href="<%=request.getContextPath()%>/css/header.css" rel="stylesheet"/>
  <link href="<%=request.getContextPath()%>/css/footer.css" rel="stylesheet"/>
  <link href="<%=request.getContextPath()%>/css/quickscan.css?v=3" rel="stylesheet"/>

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
</head>
<body>
  <%@ include file="header.jsp" %>

  <main class="main" id="main">
    <h1 class="page-title quickscan-title" id="qsTitle">Quick Scan</h1>

    <section class="qs-panel" aria-label="Quick Scan Panel">
      <div class="qs-form">
        <div class="qs-field">
          <label class="qs-label" for="date">Date</label>
          <input id="date" class="qs-input" type="date" value="<%= today %>" />
        </div>

        <!-- Department block (hidden in RESTOCK mode) -->
        <div class="qs-field" id="deptBlock">
          <label class="qs-label" for="department">Department</label>
          <select id="department" class="qs-select" required>
            <option value="" disabled selected>Select department…</option>
            <% if (deps != null) for (Department d : deps) { %>
              <option value="<%= d.getId() %>"><%= d.getCode() %> — <%= d.getName() %></option>
            <% } %>
          </select>
          <!-- typeahead UI -->
          <input id="departmentSearch" class="qs-input" placeholder="Type to search department…" style="display:none" autocomplete="off" />
          <div id="deptDropdown" class="dropdown" style="display:none"></div>
        </div>

        <div class="scan-wrap">
          <span class="scan-hint">
            <i class="fa-solid fa-barcode"></i> Scanner typed input is supported.
          </span>
          <input id="scan" class="scan-sink" type="text" autocomplete="off" aria-hidden="true" />
        </div>
      </div>

      <div class="items-card">
        <div class="items-head">
          <div id="itemsHeadTitle" style="font-size:16px">Item</div>
          <button id="btnAddManual" type="button" class="add-manual">+ Add manually</button>
        </div>

        <div class="list-head">
          <div>Code</div>
          <div>Item</div>
          <div class="right">Quantity</div>
          <div></div>
        </div>

        <div id="list" class="list-body"></div>

        <div class="items-footer">
          <div class="total">Total: <span id="totalQty">0</span></div>
          <div class="actions">
            <button id="btnCancel" class="btn btn-alt" type="button">Cancel</button>
            <button id="btnSave" class="btn btn-primary" type="button">Save</button>
          </div>
        </div>
      </div>
    </section>
  </main>

  <%@ include file="footer.jsp" %>

  <!-- ===== Mode Picker Modal (single, cleaned) ===== -->
  <div id="modeModal" class="modal mode-modal open" role="dialog" aria-modal="true" aria-labelledby="modeTitle">
    <div class="modal-card">

      <!-- Back top-left -->
      <div class="mode-header">
        <button id="modeBack" type="button" class="back-link">
          <i class="fa-solid fa-arrow-left"></i> Back to Home
        </button>
      </div>

      <!-- Title center -->
      <h3 id="modeTitle" class="modal-title mode-center">Choose action</h3>

      <!-- Options -->
      <div class="mode-grid">
        <button id="modeIssue" type="button" class="mode-btn">
          <div class="icon"><i class="fa-solid fa-right-from-bracket"></i></div>
          Issue to Department
        </button>
        <button id="modeRestock" type="button" class="mode-btn">
          <div class="icon"><i class="fa-solid fa-rotate-left"></i></div>
          Restock
        </button>
      </div>

      <!-- Friendly footer note -->
      <div class="mode-note">
        Select whether to issue items to departments or restock your inventory.
      </div>
    </div>
  </div>

  <!-- Manual Add Modal -->
  <div id="modal" class="modal" role="dialog" aria-modal="true">
    <div class="modal-card">
      <h3 class="modal-title">Add item manually</h3>
      <div class="modal-row">
        <label style="width:120px">Barcode</label>
        <input id="m_barcode" class="qs-input" style="flex:1" placeholder="Enter item barcode"/>
      </div>
      <div class="modal-row">
        <label style="width:120px">Search name</label>
        <input id="m_name" class="qs-input" style="flex:1" placeholder="Type to search…"/>
      </div>
      <div id="m_suggestions" class="modal-row" style="flex-wrap:wrap; gap:8px"></div>
      <div class="modal-row" style="justify-content:flex-end">
        <button id="m_close" class="btn btn-alt" type="button">Close</button>
        <button id="m_add" class="btn btn-primary" type="button">Add</button>
      </div>
    </div>
  </div>

<script>
/* ===== Fit height to viewport (no page scroll) ===== */
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
/* ===== Quick Scan with Mode (ISSUE / RESTOCK) ===== */
(function(){
  const BASE = '<%= request.getContextPath() %>';

  // DOM
  const scan = document.getElementById('scan');
  const list = document.getElementById('list');
  const totalQty = document.getElementById('totalQty');
  const btnSave = document.getElementById('btnSave');
  const btnCancel = document.getElementById('btnCancel');
  const date = document.getElementById('date');
  const qsTitle = document.getElementById('qsTitle');
  const itemsHeadTitle = document.getElementById('itemsHeadTitle');

  // Department/typeahead
  const deptBlock = document.getElementById('deptBlock');
  const deptSelect = document.getElementById('department');
  const deptSearch = document.getElementById('departmentSearch');
  const deptDropdown = document.getElementById('deptDropdown');

  // Manual modal
  const modal = document.getElementById('modal');
  const mBarcode = document.getElementById('m_barcode');
  const mName = document.getElementById('m_name');
  const mSugs = document.getElementById('m_suggestions');
  const btnAddManual = document.getElementById('btnAddManual');
  const mClose = document.getElementById('m_close');
  const mAdd = document.getElementById('m_add');

  // Mode picker
  const modeModal = document.getElementById('modeModal');
  const modeIssue = document.getElementById('modeIssue');
  const modeRestock = document.getElementById('modeRestock');
  const modeBack = document.getElementById('modeBack');

  // Cart
  const cart = new Map();

  // Back to Home
  modeBack.addEventListener('click', ()=>{ window.location.href = BASE + '/home.jsp'; });

  // Mode state: "ISSUE" | "RESTOCK"
  let MODE = null;

  function applyModeUI(){
    if (MODE === 'ISSUE'){
      deptBlock.style.display = '';
      qsTitle.textContent = 'Quick Scan — Issue';
      itemsHeadTitle.textContent = 'Item';
    } else if (MODE === 'RESTOCK'){
      deptBlock.style.display = 'none';
      deptSelect.value = '';
      deptSearch.value = '';
      qsTitle.textContent = 'Quick Scan — Restock';
      itemsHeadTitle.textContent = 'Item (Restock)';
    }
  }

  // ====== RENDER LIST ======
  const render = () => {
    list.innerHTML = '';
    let total = 0;
    cart.forEach((it) => {
      total += it.qty;
      const row = document.createElement('div');
      row.className = 'item-row';
      row.innerHTML = `
        <div class="code">${it.code || '-'}</div>
        <div class="name">${it.name}</div>
        <div class="qty-box">
          <button class="qty-btn minus">–</button>
          <input class="qty-input" type="number" min="1" value="${it.qty}">
          <button class="qty-btn plus">+</button>
        </div>
        <button class="remove-row" title="Remove">×</button>
      `;
      row.querySelector('.minus').onclick = () => { it.qty = Math.max(1, it.qty-1); render(); };
      row.querySelector('.plus').onclick  = () => { it.qty += 1; render(); };
      row.querySelector('.qty-input').onchange = (e)=>{ it.qty = Math.max(1, parseInt(e.target.value||1)); render(); };
      row.querySelector('.remove-row').onclick = () => { cart.delete(it.id); render(); };
      list.appendChild(row);
    });
    totalQty.textContent = total;
  };

  async function fetchItemBy(codeOrId, by='barcode'){
    const url = by==='barcode'
      ? BASE + '/scan-item?code=' + encodeURIComponent(codeOrId)
      : BASE + '/scan-item?id='   + encodeURIComponent(codeOrId);
    const r = await fetch(url, {headers:{'Accept':'application/json'}});
    if(!r.ok) throw new Error('Not found');
    return await r.json();
  }
  scan.classList.remove('typing');
  function addToCart(data){
    if(cart.has(data.id)){
      cart.get(data.id).qty += 1;
    } else {
      cart.set(data.id, {id:data.id, code:data.code, name:data.name, qty:1});
    }
    render();
  }

  // Focus + HID capture
  window.addEventListener('load', ()=> scan.focus());
  document.addEventListener('keydown', (e) => {
    if (modal.classList.contains('open') || modeModal.classList.contains('open')) return;
    const tag = (e.target && e.target.tagName) || '';
    if (['INPUT','TEXTAREA','SELECT'].includes(tag)) return;
    if (e.ctrlKey || e.altKey || e.metaKey) return;
    if (e.key.length === 1 || e.key === 'Backspace') scan.focus();
  });
  document.addEventListener('click',(e)=>{
    if(modal.classList.contains('open') || modeModal.classList.contains('open')) return;
    const t = e.target.tagName;
    if (['INPUT','TEXTAREA','SELECT','BUTTON'].includes(t)) return;
    if(!e.target.closest('.qs-panel')) return;
    scan.focus();
  });

  // Scan logic
  let scanTimer=null;
  async function processScan(val){
    const code=(val||'').trim();
    if(!code) return;
    try{
      const item=await fetchItemBy(code,'barcode');
      addToCart(item);
      scan.value='';
      scan.classList.remove('typing');
    }catch(e){ alert('Item not found: '+code); scan.select(); }
  }
  scan.addEventListener('keydown',(e)=>{
    if(e.key==='Enter'){ e.preventDefault(); processScan(scan.value); }
  });
  scan.addEventListener('input',()=>{
    scan.classList.add('typing');
    clearTimeout(scanTimer);
    scanTimer=setTimeout(()=>processScan(scan.value),220);
  });
  scan.addEventListener('paste',()=> setTimeout(()=>processScan(scan.value),50));
  scan.addEventListener('change',()=>processScan(scan.value));

  // Department Typeahead
  (function initDeptTypeahead(){
    const options = [...deptSelect.querySelectorAll('option')]
      .filter(o => o.value)
      .map(o => ({ value:o.value, label:o.textContent.trim() }));

    deptSelect.style.display = 'none';
    deptSearch.style.display = 'block';
    deptDropdown.style.display = 'block';

    const menu = document.createElement('div');
    menu.className = 'dept-menu';
    deptDropdown.appendChild(menu);

    let activeIndex = -1;
    let filtered = options;
    let isOpen = false;
    let blurTimer = null;

    function openMenu(){ if(!isOpen){ menu.style.display='block'; isOpen=true; } }
    function closeMenu(){ if(isOpen){ menu.style.display='none'; isOpen=false; activeIndex=-1; } }

    function renderMenu(){
      menu.innerHTML = '';
      if (filtered.length === 0) {
        const empty = document.createElement('div');
        empty.className = 'dept-item dept-muted';
        empty.textContent = 'No match';
        menu.appendChild(empty);
        return;
      }
      filtered.forEach((opt, i) => {
        const item = document.createElement('div');
        item.className = 'dept-item' + (i===activeIndex ? ' active' : '');
        item.textContent = opt.label;
        item.dataset.value = opt.value;
        item.onclick = () => selectValue(opt);
        menu.appendChild(item);
      });
    }

    function selectValue(opt){
      const last = deptSelect.value;
      deptSelect.value = opt.value;
      deptSearch.value = opt.label;
      renderMenu();
      closeMenu();
      deptSelect.dispatchEvent(new Event('change', {bubbles:true}));
      scan.focus();
    }

    function filter(q){
      const s = q.trim().toLowerCase();
      filtered = !s ? options : options.filter(o => o.label.toLowerCase().includes(s));
      activeIndex = filtered.length ? 0 : -1;
      renderMenu();
      if (filtered.length) openMenu(); else closeMenu();
    }

    deptSearch.addEventListener('focus', () => { filter(deptSearch.value); openMenu(); });
    deptSearch.addEventListener('input', (e)=> filter(e.target.value));
    deptSearch.addEventListener('keydown', (e)=>{
      if (!isOpen && (e.key === 'ArrowDown' || e.key === 'ArrowUp')) openMenu();
      if (e.key === 'ArrowDown'){ e.preventDefault(); activeIndex = Math.min(activeIndex+1, filtered.length-1); renderMenu(); }
      else if (e.key === 'ArrowUp'){ e.preventDefault(); activeIndex = Math.max(activeIndex-1, 0); renderMenu(); }
      else if (e.key === 'Enter'){
        e.preventDefault();
        if (filtered.length) selectValue(filtered[Math.max(0, activeIndex)]);
      } else if (e.key === 'Escape'){ closeMenu(); deptSearch.blur(); }
    });

    document.addEventListener('click', (e)=>{
      if (e.target === deptSearch || e.target.closest('.dept-menu')) return;
      closeMenu();
    });

    deptSearch.addEventListener('blur', ()=> { blurTimer = setTimeout(closeMenu, 120); });
    menu.addEventListener('mousedown', ()=> { if (blurTimer){ clearTimeout(blurTimer); blurTimer=null; } });

    renderMenu();
    closeMenu();
  })();

  // Save
  btnSave.addEventListener('click', async ()=>{
    if(cart.size===0){ alert('No items.'); return; }

    const items = Array.from(cart.values()).map(x=>({id:x.id, qty:x.qty, code:x.code}));
    const payload = {date:date.value, items};

    let url = BASE + '/save-quickscan';
    if (MODE === 'ISSUE'){
      if(!deptSelect.value){
        alert('Please select department.'); return;
      }
      payload.departmentId = deptSelect.value;
      url = BASE + '/save-quickscan';
    } else if (MODE === 'RESTOCK'){
      url = BASE + '/save-quickscan-restock';
    } else {
      alert('Please choose Issue or Restock first.'); return;
    }

    btnSave.disabled=true; btnSave.textContent='Saving…';
    try{
      const r=await fetch(url,{ method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify(payload) });
      const txt=await r.text();
      if(!r.ok) throw new Error(txt);
      alert('Saved successfully');
      cart.clear();
      render();
      window.location.href = BASE + '/home.jsp';
    }catch(err){
      alert('Save failed: '+err.message);
    }finally{
      btnSave.disabled=false; btnSave.textContent='Save';
    }
  });

  // Cancel
  btnCancel.addEventListener('click', ()=>{
    if (cart.size===0) return;
    if (confirm('Clear current list?')){ cart.clear(); render(); }
  });

  // Department change guard (Issue mode only)
  let lastDept = '';
  deptSelect.addEventListener('change', ()=>{
    if (MODE!=='ISSUE') return;
    if (cart.size > 0 && lastDept && lastDept !== deptSelect.value){
      const ok = confirm('Change department? Current scanned list will be cleared.');
      if (!ok){ deptSelect.value = lastDept; return; }
      cart.clear(); render();
    }
    lastDept = deptSelect.value;
  });

  // Modal manual add
  const openModal=()=>{ modal.classList.add('open'); mBarcode.value=''; mName.value=''; mName.dataset.pickId=''; mSugs.innerHTML=''; mBarcode.focus(); };
  const closeModal=()=>{ modal.classList.remove('open'); scan.focus(); };
  btnAddManual.onclick=openModal; mClose.onclick=closeModal;
  modal.addEventListener('click',(e)=>{ if(!e.target.closest('.modal-card')) closeModal(); });
  document.addEventListener('keydown',(e)=>{ if(modal.classList.contains('open')&&e.key==='Escape') closeModal(); });
  [mBarcode,mName].forEach(inp=>inp.addEventListener('keydown',(e)=>{ if(e.key==='Enter'){ mAdd.click(); }}));
  mAdd.onclick=async()=>{
    const b=mBarcode.value.trim();
    const n=mName.dataset.pickId;
    try{
      const item=b?await fetchItemBy(b,'barcode'):await fetchItemBy(n,'id');
      addToCart(item); closeModal();
    }catch(_){ alert('Item not found.'); }
  };

  // Suggestions (modal)
  let sugsTimer=null;
  mName.addEventListener('input', () => {
    clearTimeout(sugsTimer);
    const q = mName.value.trim();
    if (!q) { mSugs.innerHTML = ''; mName.dataset.pickId=''; return; }
    sugsTimer = setTimeout(async () => {
      try{
        const r = await fetch(BASE + '/search-items?q=' + encodeURIComponent(q), {headers:{'Accept':'application/json'}});
        if (!r.ok) { mSugs.innerHTML=''; return; }
        const arr = await r.json();
        mSugs.innerHTML='';
        arr.slice(0, 12).forEach(it => {
          const chip = document.createElement('button');
          chip.type='button';
          chip.className='add-manual m-chip';
          chip.setAttribute('data-id', it.id);
          chip.setAttribute('data-name', it.name);
          chip.textContent = `${it.name} (${it.code || '-'})`;
          mSugs.appendChild(chip);
        });
      }catch(_){}
    }, 180);
  });

  mSugs.addEventListener('click', async (e) => {
    const btn = e.target.closest('button[data-id]');
    if (!btn) return;
    mName.value = btn.getAttribute('data-name');
    mName.dataset.pickId = btn.getAttribute('data-id');
    try{
      const item = await fetchItemBy(mName.dataset.pickId, 'id');
      addToCart(item);
      closeModal();
    }catch(_){ alert('Item not found.'); }
  });

  // ===== Mode picker wiring =====
  function setMode(m){
    MODE = m;
    modeModal.classList.remove('open');
    applyModeUI();
    scan.focus();
  }
  modeIssue.addEventListener('click', ()=> setMode('ISSUE'));
  modeRestock.addEventListener('click', ()=> setMode('RESTOCK'));
})();
</script>
</body>
</html>
