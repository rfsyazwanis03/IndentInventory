<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isELIgnored="true" %>
<%
  com.iim.models.User me = (com.iim.models.User) session.getAttribute("user");
  if (me == null) { response.sendRedirect("login.jsp"); return; }
%>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8"/>
  <title>Indent Inventory</title>
  <meta name="viewport" content="width=device-width, initial-scale=1"/>
  <meta name="theme-color" content="#94765e"/>

  <link href="<%=request.getContextPath()%>/css/theme.css" rel="stylesheet"/>
  <link href="<%=request.getContextPath()%>/css/header.css" rel="stylesheet"/>
  <link href="<%=request.getContextPath()%>/css/footer.css" rel="stylesheet"/>
  <link rel="stylesheet" href="<%=request.getContextPath()%>/css/inventory.css?v=11"/>

  <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.2/css/all.min.css"/>
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
    /* Make upload icon match size and rhythm of other controls,
       but visually "invisible" (no filled background). */
    .icon.upload {
      /* match button height variable and visual rhythm */
      height: var(--h);
      width: var(--h);
      min-width: var(--h);
      padding: 0;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      border-radius: 999px;            /* matches pill feel */
      background: transparent;         /* invisible background */
      border: 1px solid transparent;   /* keep layout consistent with .icon */
      cursor: pointer;
      box-shadow: var(--shadow-sm);
    }

    /* subtle hover affordance: faint tinted ring using theme color */
    .icon.upload:hover {
      background: rgba(148,118,94,0.06);
      border-color: rgba(148,118,94,0.08);
      transform: translateY(-1px);
    }

    .icon.upload i {
      font-size: 16px;
      color: var(--btn); /* use primary color so icon reads as actionable */
      line-height: 1;
    }

    /* focus style for accessibility */
    .icon.upload:focus {
      outline: none;
      box-shadow: 0 0 0 4px rgba(148,118,94,0.12), var(--shadow-sm);
    }
  </style>
</head>
<body>
<%@ include file="header.jsp" %>

<script>
/* Fit height so only table scrolls (header/footer excluded) */
(function(){
  const root=document.documentElement;
  function apply(){
    const h=document.querySelector('header.hdr')?.getBoundingClientRect().height||0;
    const f=document.querySelector('footer')?.getBoundingClientRect().height||0;
    root.style.setProperty('--main-h',(window.innerHeight-h-f)+'px');
  }
  window.addEventListener('resize',apply,{passive:true});
  document.addEventListener('DOMContentLoaded',apply);
  window.addEventListener('load',apply);
  setTimeout(apply,0);setTimeout(apply,250);setTimeout(apply,600);
})();
</script>

<main class="main">
  <div class="content">
    <h1 class="page-title">Inventory</h1>

    <div class="topbar">
      <div class="search-pill">
        <i class="fa-solid fa-magnifying-glass"></i>
        <input id="q" class="search-input" placeholder="Search by barcode or name…" autocomplete="off"/>
      </div>

      <div class="rightbar">
        <select id="sort" class="select">
          <option value="name_asc">All (Name A–Z)</option>
          <option value="stock_asc">Stock: Low → High</option>
          <option value="stock_desc">Stock: High → Low</option>
          <option value="code_asc">Code (A–Z)</option>
        </select>
        <div class="actions">
          <button id="btnAdd" class="btn solid">Add</button>

          <!-- Export form: icon-only UPLOAD glyph, invisible background and same size as buttons -->
          <form id="frmExportInventory" method="post" action="<%= request.getContextPath() %>/reports" target="_blank" style="display:inline;">
            <input type="hidden" name="export" value="inventory"/>
            <input type="hidden" name="q" id="export_q" value=""/>
            <input type="hidden" name="sort" id="export_sort" value=""/>
            <!-- upload icon: visually subtle, same size and alignment as other controls -->
            <button id="btnExportPdf" type="button" class="icon upload" title="Download inventory PDF" aria-label="Download inventory PDF">
              <i class="fa-solid fa-upload" aria-hidden="true"></i>
            </button>
          </form>

          <button id="btnSave" class="btn solid hidden">Save</button>
          <button id="btnCancel" class="btn hidden">Cancel</button>
        </div>
      </div>
    </div>

    <div class="table-wrap">
      <table id="tblInventory" class="table">
        <!-- Widths fixed via colgroup -> header & content sync -->
        <colgroup>
          <col style="width:200px"><!-- Code -->
          <col style="width:auto"><!-- Item -->
          <col style="width:110px"><!-- Quantity -->
          <col style="width:120px"><!-- Status -->
          <col style="width:92px"><!-- Action -->
        </colgroup>
        <thead>
          <tr>
            <th>Code</th>
            <th>Item</th>
            <th>Quantity</th>
            <th>Status</th>
            <th>Action</th>
          </tr>
        </thead>
        <tbody id="list">
          <tr><td colspan="5" class="empty">Loading…</td></tr>
        </tbody>
      </table>
    </div>
  </div>
</main>

<%@ include file="footer.jsp" %>

<!-- Modal Add -->
<div id="modalAdd" class="modal" aria-hidden="true">
  <div class="modal-card">
    <h3>Add New Item</h3>
    <div class="form">
      <!-- Uppercase -->
      <label>Barcode
        <input type="text" id="addCode" maxlength="64"
               style="text-transform:uppercase"
               oninput="this.value=this.value.toUpperCase()"/>
      </label>
      <label>Item Name
        <input type="text" id="addName" maxlength="120"
               style="text-transform:uppercase"
               oninput="this.value=this.value.toUpperCase()"/>
      </label>
      <label>Quantity <input type="number" id="addQty" min="0" step="1" value="0"/></label>
      <label>Min Quantity <input type="number" id="addMinQty" min="0" step="1" value="0"/></label>
      <!-- NEW: critical threshold -->
      <label>Critical Min Quantity <input type="number" id="addCriticalMinQty" min="0" step="1" value="0"/></label>
    </div>
    <div class="modal-actions">
      <button id="btnModalCancel" class="btn">Cancel</button>
      <button id="btnModalSave" class="btn solid">Save</button>
    </div>
  </div>
</div>

<script>
(function(){
  const BASE='<%= request.getContextPath() %>';
  const API={
    list:(q,sort)=>{const p=new URLSearchParams();if(q)p.set('q',q);if(sort)p.set('sort',sort);const qs=p.toString();return BASE+'/items'+(qs?('?'+qs):'');},
    create:BASE+'/item-create',
    update:BASE+'/item-update' // status-only
  };
  const $=(s,p=document)=>p.querySelector(s);
  const listEl=$('#list'),qEl=$('#q'),sortEl=$('#sort');
  const btnAdd=$('#btnAdd'),btnSave=$('#btnSave'),btnCancel=$('#btnCancel');
  const modal=$('#modalAdd'),mCode=$('#addCode'),mName=$('#addName'),mQty=$('#addQty'),mMinQty=$('#addMinQty'),mCritMinQty=$('#addCriticalMinQty');
  const mCancel=$('#btnModalCancel'),mSave=$('#btnModalSave');
  const btnExportPdf = document.getElementById('btnExportPdf');
  const exportQ = document.getElementById('export_q');
  const exportSort = document.getElementById('export_sort');
  const exportForm = document.getElementById('frmExportInventory');
  let editingRow=null;

  const chip=s=>'<span class="badge '+(s==='active'?'ok':'off')+'">'+(s==='active'?'Active':'Inactive')+'</span>';

  const rowHTML=d=>(
    '<tr data-id="'+d.id+'">'+
      '<td class="mono">'+((d.code||'').toString().toUpperCase())+'</td>'+
      '<td>'+((d.name||'').toString().toUpperCase())+'</td>'+
      '<td>'+ (d.quantity||0) +'</td>'+
      '<td data-status="'+(d.status||'')+'">'+chip((d.status||'').toLowerCase())+'</td>'+
      '<td><button class="icon edit" title="Edit status"><i class="fa fa-pen"></i></button></td>'+
    '</tr>'
  );

  async function reload(){
    const r=await fetch(API.list(qEl.value.trim(),sortEl.value||'name_asc'),{headers:{'Accept':'application/json'}});
    if(!r.ok){listEl.innerHTML='<tr><td colspan="5" class="empty">Failed to load</td></tr>';return;}
    const arr=await r.json();
    if(!arr.length){listEl.innerHTML='<tr><td colspan="5" class="empty">No items found</td></tr>';return;}
    listEl.innerHTML=arr.map(rowHTML).join('');
    listEl.querySelectorAll('.edit').forEach(btn=>{
      btn.addEventListener('click',e=>{
        const tr=e.currentTarget.closest('tr');
        const d={id:tr.dataset.id,status:(tr.children[3].getAttribute('data-status')||'').toLowerCase()};
        startEdit(tr,d);
      });
    });
  }

  function startEdit(tr,d){
    if(editingRow)return; editingRow=tr;
    btnSave.classList.remove('hidden');btnCancel.classList.remove('hidden');

    // Only Status becomes editable
    const tds=tr.children;
    const currentStatus=(d.status==='active'?'active':'inactive');
    tds[3].innerHTML='<select class="input inv-status-select">'+
        '<option value="active" '+(currentStatus==='active'?'selected':'')+'>Active</option>'+
        '<option value="inactive" '+(currentStatus==='inactive'?'selected':'')+'>Inactive</option>'+
      '</select>';
    tds[4].innerHTML='<span>Editing…</span>';
  }

  btnSave.addEventListener('click',async()=>{
    if(!editingRow)return;
    const select=editingRow.querySelector('.inv-status-select');
    if(!select){editingRow=null;btnSave.classList.add('hidden');btnCancel.classList.add('hidden');reload();return;}
    const payload={ id:editingRow.dataset.id, status:select.value };
    try{
      const r=await fetch(API.update,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(payload)});
      const txt=await r.text();if(!r.ok)throw new Error(txt||('HTTP '+r.status));
      alert('Status updated');editingRow=null;btnSave.classList.add('hidden');btnCancel.classList.add('hidden');reload();
    }catch(e){alert('Save failed: '+e.message);}
  });

  btnCancel.addEventListener('click',()=>{editingRow=null;btnSave.classList.add('hidden');btnCancel.classList.add('hidden');reload();});

  let t=null;
  qEl.addEventListener('input',()=>{clearTimeout(t);t=setTimeout(reload,250);});
  qEl.addEventListener('keydown',e=>{if(e.key==='Enter'){e.preventDefault();reload();}});
  sortEl.addEventListener('change',reload);

  // Modal Add (uppercase for code/name)
  btnAdd.addEventListener('click',()=>{modal.classList.add('show');mCode.value='';mName.value='';mQty.value='0';mMinQty.value='0';mCritMinQty.value='0';mCode.focus();});
  mCancel.addEventListener('click',()=>modal.classList.remove('show'));
  mSave.addEventListener('click',async()=>{
    const payload={
      code:mCode.value.trim().toUpperCase(),
      name:mName.value.trim().toUpperCase(),
      quantity:Number(mQty.value||0),
      min_quantity:Number(mMinQty.value||0),
      critical_min_quantity:Number(mCritMinQty.value||0)
    };
    if(!payload.code||!payload.name){alert('Barcode and Item Name are required.');return;}
    try{
      const r=await fetch(API.create,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(payload)});
      const txt=await r.text();
      if(!r.ok){
        if(r.status===409){alert('Add failed: Barcode already exists.');}
        else{alert('Add failed: '+(txt||'HTTP '+r.status));}
        return;
      }
      modal.classList.remove('show');await reload();
      const tr=[...listEl.querySelectorAll('tr')].find(x=>x.children[0]?.textContent.trim()===payload.code);
      if(tr){tr.classList.add('row-flash');}
      alert('Item added');
    }catch(e){alert('Add failed: '+e.message);}
  });

  // Export icon click -> fill hidden inputs and submit form
  btnExportPdf.addEventListener('click', () => {
    exportQ.value = qEl.value.trim();
    exportSort.value = sortEl.value || 'name_asc';
    exportForm.submit();
  });

  reload();
})();
</script>
</body>
</html>
