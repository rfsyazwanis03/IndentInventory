<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isELIgnored="true" %>
<%
    com.iim.models.User me = (com.iim.models.User) session.getAttribute("user");
    if (me == null) {
        response.sendRedirect("login.jsp");
        return;
    }
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
        <link href="<%=request.getContextPath()%>/css/inventory.css" rel="stylesheet"/>
        <link href="<%=request.getContextPath()%>/css/log.css?v=11" rel="stylesheet"/>

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
    </head>
    <body>
        <%@ include file="header.jsp" %>

        <main class="main">
            <div class="content content--tight" id="contentRef">
                <h1 class="page-title" id="titleRef">Activity Log</h1>

                <div class="log-toolbar" id="toolbarRef">
                    <button id="btnFilter" class="btn btn-ghost" aria-expanded="false" aria-controls="filterPanel">
                        <i class="fa fa-filter"></i><span class="hide-sm">&nbsp;Filter</span>
                    </button>
                    <div class="spacer"></div>

                    <div class="minipager">
                        <button id="first" class="btn icon" title="First page" aria-label="First page"><i class="fa fa-angles-left"></i></button>
                        <button id="prev"  class="btn icon" title="Previous"   aria-label="Previous page"><span>&laquo;</span></button>
                        <span class="page-info">Page <span id="pg">1</span> / <span id="pgMax">—</span></span>
                        <button id="next"  class="btn icon" title="Next"       aria-label="Next page"><span>&raquo;</span></button>
                        <button id="last"  class="btn icon" title="Last page"  aria-label="Last page"><i class="fa fa-angles-right"></i></button>
                    </div>
                </div>

                <!-- Filter -->
                <div id="filterPanel" class="filter-panel" hidden>
                    <div class="panel-arrow"></div>

                    <div class="row">
                        <label for="fAction">Action</label>
                        <select id="fAction" class="input">
                            <option value="ALL">All actions</option>
                            <option value="LOGIN">Login</option>
                            <option value="LOGOUT">Logout</option>
                            <option value="ADD_ITEM">Add Item</option>
                            <option value="UPDATE_ITEM">Update Item</option>
                            <option value="QUICK_SCAN_SAVE">Quick Scan Issue</option>

                            <option value="SCAN_ITEM">Scan Item</option>
                            <option value="REPORT_GENERATE">Report Generate</option>
                            <option value="REPORT_EXPORT">Report Export</option>
                            <option value="QUICK_SCAN_RESTOCK_SAVE">Restock</option>
                            <option value="DEPT_CREATE">Create Department</option>
                            <option value="DEPT_UPDATE">Update Department</option>
                        </select>
                    </div>


                    <div class="row two">
                        <div>
                            <label for="fFrom">From</label>
                            <input id="fFrom" class="input" type="date"/>
                        </div>
                        <div>
                            <label for="fTo">To</label>
                            <input id="fTo" class="input" type="date"/>
                        </div>
                    </div>

                    <div class="row">
                        <label for="fQ">Search</label>
                        <input id="fQ" class="input" placeholder="Search details…"/>
                    </div>

                    <div class="actions">
                        <button id="btnApply" class="btn btn-primary"><i class="fa fa-check"></i>&nbsp;Apply</button>
                        <button id="btnReset" class="btn"><i class="fa fa-eraser"></i>&nbsp;Reset</button>
                    </div>
                </div>

                <!-- Table -->
                <div class="table-wrap" id="tblFrame">
                    <table class="table" id="tableRef">
                        <thead id="theadRef">
                            <tr>
                                <th class="nowrap">Date / time</th>
                                <th>Action</th>
                                <th>Detail</th>
                            </tr>
                        </thead>
                        <tbody id="rows">
                            <tr><td colspan="3" class="empty">Loading…</td></tr>
                        </tbody>
                    </table>
                </div>
            </div>
        </main>

        <%@ include file="footer.jsp" %>

        <script>
            (() => {
                const BASE = '<%= request.getContextPath()%>';

                // Refs
                const content = document.getElementById('contentRef');
                const titleRef = document.getElementById('titleRef');
                const toolbarRef = document.getElementById('toolbarRef');
                const theadRef = document.getElementById('theadRef');
                const rowsEl = document.getElementById('rows');

                // Filter
                const btnFilter = document.getElementById('btnFilter');
                const panel = document.getElementById('filterPanel');
                const fAction = document.getElementById('fAction');
                const fFrom = document.getElementById('fFrom');
                const fTo = document.getElementById('fTo');
                const fQ = document.getElementById('fQ');
                const btnApply = document.getElementById('btnApply');
                const btnReset = document.getElementById('btnReset');

                // Pager
                const first = document.getElementById('first');
                const prev = document.getElementById('prev');
                const next = document.getElementById('next');
                const last = document.getElementById('last');
                const pg = document.getElementById('pg');
                const pgMax = document.getElementById('pgMax');

                const SIZE = 8;       // 8 rows tetap
                const MAX_PAGES = 10; // maksimum 10 page
                let page = 1;
                let pageMax = MAX_PAGES;

                /* ========== FILTER PANEL: position + toggle (FIXED) ========== */
                function positionPanel() {
                    const r = btnFilter.getBoundingClientRect();
                    const panelW = 320, margin = 12, vw = window.innerWidth;
                    let left = window.scrollX + r.left;
                    const maxLeft = window.scrollX + vw - panelW - margin;
                    if (left > maxLeft)
                        left = Math.max(window.scrollX + margin, maxLeft);
                    panel.style.minWidth = panelW + 'px';
                    panel.style.maxWidth = panelW + 'px';
                    panel.style.top = (window.scrollY + r.bottom + 10) + 'px';
                    panel.style.left = left + 'px';
                }
                function togglePanel(show) {
                    const want = (typeof show === 'boolean') ? show : panel.hasAttribute('hidden');
                    if (want) {
                        panel.removeAttribute('hidden');
                        btnFilter.setAttribute('aria-expanded', 'true');
                        positionPanel();
                    } else {
                        panel.setAttribute('hidden', '');
                        btnFilter.setAttribute('aria-expanded', 'false');
                    }
                }
                btnFilter.addEventListener('click', () => togglePanel());
                document.addEventListener('click', e => {
                    if (!panel.contains(e.target) && !btnFilter.contains(e.target))
                        togglePanel(false);
                });
                document.addEventListener('keydown', e => {
                    if (e.key === 'Escape')
                        togglePanel(false);
                });
                window.addEventListener('resize', () => {
                    if (!panel.hasAttribute('hidden'))
                        positionPanel();
                });

                /* ========== FIT ROW HEIGHT ========== */
                function fitRows() {
                    const H = content.getBoundingClientRect().height;
                    const avail = H - (titleRef.offsetHeight + toolbarRef.offsetHeight + theadRef.offsetHeight + 20);
                    const rowH = Math.max(42, Math.floor((avail - 2) / SIZE));
                    document.documentElement.style.setProperty('--row-h', rowH + 'px');
                    document.documentElement.style.setProperty('--thead-h', theadRef.offsetHeight + 'px');
                }
                new ResizeObserver(fitRows).observe(content);
                new ResizeObserver(fitRows).observe(theadRef);
                window.addEventListener('load', fitRows);
                window.addEventListener('resize', fitRows);

                /* ========== DATA LOAD & RENDER (sentiasa 8 baris) ========== */
                const escapeHtml = s => (s || '').replace(/[&<>"']/g, m => ({'&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'}[m]));

                async function load() {
                    const p = new URLSearchParams();
                    if (fAction.value && fAction.value !== 'ALL')
                        p.set('action', fAction.value);
                    if (fFrom.value)
                        p.set('from', fFrom.value);
                    if (fTo.value)
                        p.set('to', fTo.value);
                    if (fQ.value.trim())
                        p.set('q', fQ.value.trim());
                    p.set('page', page);
                    p.set('size', SIZE);

                    rowsEl.innerHTML = '<tr><td colspan="3" class="empty">Loading…</td></tr>';

                    try {
                        const r = await fetch(BASE + '/logs?' + p.toString(), {headers: {'Accept': 'application/json'}});
                        if (!r.ok) {
                            rowsEl.innerHTML = '<tr><td colspan="3" class="empty">Failed</td></tr>';
                            updatePager();
                            fitRows();
                            return;
                        }

                        const data = await r.json();
                        let items = Array.isArray(data) ? data : (data.items || []);

                        // optional filter out
                        items = items.filter(o => o && o.action && o.action !== 'SCAN_ITEM');

                        // pad hingga cukup 8 baris
                        const pad = Math.max(0, SIZE - items.length);
                        const htmlRows = items.slice(0, SIZE).map(o => `
                <tr>
                  <td class="td--mono">${escapeHtml(o.time || '')}</td>
                  <td>${escapeHtml(o.action || '')}</td>
                  <td>${escapeHtml(o.detail || '')}</td>
                </tr>`).join('');
                        const blanks = Array.from({length: pad}).map(() => '<tr><td></td><td></td><td></td></tr>').join('');
                        rowsEl.innerHTML = (items.length ? htmlRows : '') + blanks;

                    } catch (e) {
                        rowsEl.innerHTML = '<tr><td colspan="3" class="empty">Error</td></tr>';
                    }
                    updatePager();
                    fitRows();
                }

                /* ========== PAGER (max 10 page) ========== */
                function updatePager() {
                    pg.textContent = page;
                    pgMax.textContent = pageMax;
                    first.disabled = (page <= 1);
                    prev.disabled = (page <= 1);
                    next.disabled = (page >= pageMax);
                    last.disabled = (page >= pageMax);
                }
                first.addEventListener('click', () => {
                    if (page > 1) {
                        page = 1;
                        load();
                    }
                });
                prev.addEventListener('click', () => {
                    if (page > 1) {
                        page--;
                        load();
                    }
                });
                next.addEventListener('click', () => {
                    if (page < pageMax) {
                        page++;
                        load();
                    }
                });
                last.addEventListener('click', () => {
                    if (page < pageMax) {
                        page = pageMax;
                        load();
                    }
                });

                /* ========== FILTER actions ========== */
                btnApply.addEventListener('click', () => {
                    page = 1;
                    togglePanel(false);
                    load();
                });
                btnReset.addEventListener('click', () => {
                    fAction.value = 'ALL';
                    fFrom.value = '';
                    fTo.value = '';
                    fQ.value = '';
                    page = 1;
                    togglePanel(false);
                    load();
                });
                fQ.addEventListener('keydown', e => {
                    if (e.key === 'Enter') {
                        page = 1;
                        togglePanel(false);
                        load();
                    }
                });

                // initial
                load();
            })();
        </script>

    </body>
</html>
