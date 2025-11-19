<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isELIgnored="true" %>
<%@ page import="java.util.*, com.iim.models.*, com.iim.dao.*" %>
<%
  com.iim.models.User me = (com.iim.models.User) session.getAttribute("user");
  if (me == null) { response.sendRedirect("login.jsp"); return; }

  // If JSP opened directly, redirect to /reports so servlet prepares data
  if (request.getAttribute("items") == null) {
    response.sendRedirect(request.getContextPath() + "/reports");
    return;
  }

  List<Item> items = (List<Item>) request.getAttribute("items");
  List<ReportRow> rows = (List<ReportRow>) request.getAttribute("rows");
  String chosenItemName = (String) request.getAttribute("chosenItemName");
  Integer chosenYear = (Integer) request.getAttribute("chosenYear");

  // Provided by servlet
  int[] colTotals = (int[]) request.getAttribute("colTotals");
  Integer grandTotal = (Integer) request.getAttribute("grandTotal");

  String preChosenName = (chosenItemName==null? "" : chosenItemName
      .replace("&","&amp;").replace("\"","&quot;").replace("<","&lt;").replace(">","&gt;"));
%>
<!DOCTYPE html>
<html lang="en">
    <head>
        <meta charset="UTF-8"/>
        <title>Indent Inventory</title>
        <meta name="viewport" content="width=device-width, initial-scale=1"/>
        <meta name="theme-color" content="#94765e"/>

        <!-- Shared CSS -->
        <link href="<%=request.getContextPath()%>/css/theme.css" rel="stylesheet"/>
        <link href="<%=request.getContextPath()%>/css/header.css" rel="stylesheet"/>
        <link href="<%=request.getContextPath()%>/css/footer.css" rel="stylesheet"/>
        <!-- Page CSS -->
        <link rel="stylesheet" href="<%=request.getContextPath()%>/css/reports.css?v=12"/>
        <!-- Icons -->
        <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.2/css/all.min.css"/>

        <!-- Favicon -->
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

        <script>
            (function fitToPage() {
                const root = document.documentElement;
                function applySize() {
                    const hdr = document.querySelector('header.hdr');
                    const ftr = document.querySelector('footer');
                    const hHdr = hdr ? hdr.getBoundingClientRect().height : 0;
                    const hFtr = ftr ? ftr.getBoundingClientRect().height : 0;
                    root.style.setProperty('--main-h', (window.innerHeight - hHdr - hFtr) + 'px');
                }
                applySize();
                window.addEventListener('resize', applySize, {passive: true});
            })();
        </script>

        <main class="main" id="main">
            <div class="content">
                <h1 class="page-title">Reports &amp; Analytics</h1>

                <section class="frame" aria-label="Reports Frame">
                    <!-- Left controls -->
                    <aside class="controls">
                        <form id="filterForm" action="reports" method="get" class="ctrl-form" onsubmit="return ensureSelections();">
                            <label class="ctrl-label">Item</label>
                            <div class="ctrl-field">
                                <div class="search-pill">
                                    <i class="fa-solid fa-magnifying-glass"></i>
                                    <input id="itemSearch" class="search-input" placeholder="Search by name…" autocomplete="off"/>
                                    <button id="btnItemSearch" class="search-btn" type="button" title="Search"></button>
                                </div>
                                <div id="itemResults" class="dropdown" hidden role="listbox" aria-label="Items"></div>

                                <input type="hidden" name="item" id="itemId"
                                       value="<%= (request.getParameter("item")==null) ? "" : request.getParameter("item") %>" />
                                <input type="hidden" id="preItemName" value="<%= preChosenName %>"/>
                                <!-- hidden export type for POST (pdf / excel) -->
                                <input type="hidden" name="export" id="exportType" value=""/>
                            </div>

                            <label class="ctrl-label">Year</label>
                            <div class="ctrl-field">
                                <select name="year" id="yearSel" class="native-select" aria-hidden="true" tabindex="-1">
                                    <option value="">— Select Year —</option>
                                </select>
                                <div class="select-like" id="yearSelect">
                                    <button type="button" class="select-display" id="yearBtn" aria-haspopup="listbox" aria-expanded="false">
                                        — Select Year —
                                        <i class="fa-solid fa-chevron-down" aria-hidden="true"></i>
                                    </button>
                                    <div class="year-menu" id="yearMenu" role="listbox" hidden></div>
                                </div>
                            </div>

                            <div class="ctrl-note">
                                <i class="fa-regular fa-circle-question"></i>
                                <span>Pick an <b>Item</b>, then choose an available <b>Year</b> from the transaction history.</span>
                            </div>

                            <button type="submit" class="btn gen" title="Generate">
                                <i class="fa-solid fa-chart-column"></i> Generate
                            </button>

                            <!-- Export button opens modal, only enabled when rows exist -->
                            <button type="button"
                                    class="btn export"
                                    id="openExportModal"
                                    <%= (rows==null || rows.isEmpty()) ? "disabled" : "" %>>
                                <i class="fa-solid fa-file-export"></i> Export…
                            </button>
                        </form>
                    </aside>

                    <!-- Right report display -->
                    <section class="report-wrap">
                        <% if (rows == null) { %>
                        <div class="empty">
                            <p>Please choose an <b>Item</b> and a <b>Year</b> to generate your report.</p>
                            <small>Only departments/months with actual data will be displayed.</small>
                        </div>
                        <% } else if (rows.isEmpty()) { %>
                        <div class="empty">
                            <p>No data found for the chosen selection.</p>
                            <small>Try a different year or item.</small>
                        </div>
                        <% } else { %>
                        <header class="report-head">
                            <div class="title">
                                <h2><%= chosenYear %> — <%= chosenItemName %></h2>
                                <p class="sub">Usage by Department (ISSUE transactions)</p>
                            </div>

                            <div class="head-actions">
                                <button type="button" class="zoom-btn" id="zoomOut" title="Zoom out (−)">−</button>
                                <input type="range" id="zoomRange" min="60" max="200" value="100" step="5" aria-label="Zoom percent"/>
                                <button type="button" class="zoom-btn" id="zoomIn" title="Zoom in (+)">+</button>
                                <button type="button" class="zoom-reset" id="zoomReset" title="Reset zoom to 100%">100%</button>
                            </div>
                        </header>

                        <div class="report-frame" id="reportFrame">
                            <div class="report-canvas" id="reportCanvas">
                                <table class="rpt">
                                    <thead>
                                        <tr>
                                            <th class="dept">DEPARTMENT</th>
                                            <th>JAN</th><th>FEB</th><th>MAR</th><th>APR</th><th>MAY</th><th>JUN</th>
                                            <th>JUL</th><th>AUG</th><th>SEP</th><th>OCT</th><th>NOV</th><th>DEC</th>
                                            <th class="total">TOTAL</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <% for (ReportRow r : rows) {
                     int[] m = r.getMonths(); %>
                                        <tr>
                                            <td class="dept"><%= r.getDepartmentName() %></td>
                                            <% for (int i=0;i<12;i++) { %>
                                            <td><%= m[i]==0 ? "" : String.valueOf(m[i]) %></td>
                                            <% } %>
                                            <td class="total"><%= r.getTotal() %></td>
                                        </tr>
                                        <% } %>
                                    </tbody>

                                    <tfoot class="rpt-sum">
                                        <tr>
                                            <td class="dept">TOTAL</td>
                                            <% for (int i=0; i<12; i++) { %>
                                            <td><%= (colTotals!=null && colTotals[i] > 0) ? String.valueOf(colTotals[i]) : "" %></td>
                                            <% } %>
                                            <td class="total"><%= (grandTotal==null? 0 : grandTotal) %></td>
                                        </tr>
                                    </tfoot>
                                </table>
                            </div>
                        </div>
                        <% } %>
                    </section>
                </section>
            </div>
        </main>

        <!-- EXPORT MODAL -->
        <div class="export-overlay" id="exportOverlay" hidden>
            <div class="export-modal" role="dialog" aria-modal="true" aria-labelledby="exportTitle">
                <button type="button" class="export-close" id="closeExportModal" aria-label="Close">
                    <i class="fa-solid fa-xmark"></i>
                </button>
                <h2 class="export-title" id="exportTitle">Export Report</h2>
                <p class="export-sub">
                    How would you like to export this report?
                </p>
                <p class="export-desc">
                    You selected an item and year. Choose whether to download the report as a PDF document or an Excel-ready CSV file.
                </p>

                <div class="export-options">
                    <button type="button" class="export-option primary" id="btnExportPdf">
                        <span class="icon"><i class="fa-regular fa-file-pdf"></i></span>
                        <span class="text">
                            <span class="label">Download PDF</span>
                            <span class="hint">Best for printing or sharing as a document.</span>
                        </span>
                    </button>

                    <button type="button" class="export-option" id="btnExportExcel">
                        <span class="icon"><i class="fa-regular fa-file-excel"></i></span>
                        <span class="text">
                            <span class="label">Download Excel (.csv)</span>
                            <span class="hint">Opens in Excel or other spreadsheet apps.</span>
                        </span>
                    </button>
                </div>

                <p class="export-note">
                    <strong>Note:</strong> Excel download uses CSV text format, which opens normally in Excel.
                </p>
            </div>
        </div>

        <%@ include file="footer.jsp" %>

        <script>
            (function () {
                const form = document.getElementById('filterForm');
                const exportTypeEl = document.getElementById('exportType');

                /* ===== Year (DOB-like) ===== */
                const yearSel = document.getElementById('yearSel');     // hidden select
                const yearBtn = document.getElementById('yearBtn');
                const yearMenu = document.getElementById('yearMenu');
                const yearWrap = document.getElementById('yearSelect');

                function setYearDisplay(txt) {
                    yearBtn.firstChild.nodeValue = (txt + ' ');
                }
                function openYearMenu() {
                    if (yearSel.disabled)
                        return;
                    yearMenu.hidden = false;
                    yearBtn.setAttribute('aria-expanded', 'true');
                    const cur = yearSel.value;
                    if (cur) {
                        const el = yearMenu.querySelector('[data-year="' + cur + '"]');
                        if (el)
                            el.scrollIntoView({block: 'center'});
                    }
                }
                function closeYearMenu() {
                    yearMenu.hidden = true;
                    yearBtn.setAttribute('aria-expanded', 'false');
                }
                function toggleYearMenu() {
                    yearMenu.hidden ? openYearMenu() : closeYearMenu();
                }

                function buildYearMenu(arr) {
                    arr.sort((a, b) => b - a); // newest -> oldest
                    // native select
                    yearSel.innerHTML = '<option value="">— Select Year —</option>';
                    const frag = document.createDocumentFragment();
                    arr.forEach(y => {
                        const o = document.createElement('option');
                        o.value = y;
                        o.textContent = y;
                        frag.appendChild(o);
                    });
                    yearSel.appendChild(frag);
                    yearSel.disabled = !arr.length;

                    // faux menu
                    yearMenu.innerHTML = '';
                    arr.forEach(y => {
                        const btn = document.createElement('button');
                        btn.type = 'button';
                        btn.className = 'year-item';
                        btn.dataset.year = y;
                        btn.setAttribute('role', 'option');
                        btn.textContent = y;
                        btn.addEventListener('click', () => {
                            yearSel.value = String(y);
                            setYearDisplay(String(y));
                            closeYearMenu();
                        });
                        yearMenu.appendChild(btn);
                    });
                }

                // Type-ahead (digits)
                let typeBuf = '';
                let typeTimer = null;
                function handleTypeAhead(ch) {
                    if (!/[0-9]/.test(ch))
                        return;
                    clearTimeout(typeTimer);
                    typeBuf += ch;
                    typeTimer = setTimeout(() => typeBuf = '', 600);
                    const t = yearMenu.querySelector('[data-year^="' + typeBuf + '"]');
                    if (t)
                        t.scrollIntoView({block: 'center'});
                }
                yearBtn.addEventListener('click', toggleYearMenu);
                document.addEventListener('click', (e) => {
                    if (!yearWrap.contains(e.target))
                        closeYearMenu();
                });
                yearBtn.addEventListener('keydown', (e) => {
                    if (e.key === 'Enter' || e.key === ' ') {
                        e.preventDefault();
                        toggleYearMenu();
                    }
                    if (e.key === 'Escape') {
                        closeYearMenu();
                    }
                    if (e.key === 'ArrowDown') {
                        e.preventDefault();
                        openYearMenu();
                        yearMenu.scrollTop += 28;
                    }
                    if (e.key === 'ArrowUp') {
                        e.preventDefault();
                        openYearMenu();
                        yearMenu.scrollTop -= 28;
                    }
                    if (e.key === 'PageDown') {
                        e.preventDefault();
                        openYearMenu();
                        yearMenu.scrollTop += yearMenu.clientHeight;
                    }
                    if (e.key === 'PageUp') {
                        e.preventDefault();
                        openYearMenu();
                        yearMenu.scrollTop -= yearMenu.clientHeight;
                    }
                    if (e.key === 'Home') {
                        e.preventDefault();
                        openYearMenu();
                        yearMenu.scrollTop = 0;
                    }
                    if (e.key === 'End') {
                        e.preventDefault();
                        openYearMenu();
                        yearMenu.scrollTop = yearMenu.scrollHeight;
                    }
                    if (e.key.length === 1) {
                        openYearMenu();
                        handleTypeAhead(e.key);
                    }
                });

                /* ===== Item search ===== */
                const itemId = document.getElementById('itemId');
                const itemSearch = document.getElementById('itemSearch');
                const itemDrop = document.getElementById('itemResults');
                const btnSearch = document.getElementById('btnItemSearch');
                const preItemName = document.getElementById('preItemName');

                form.addEventListener('keydown', e => {
                    if (e.key === 'Enter')
                        e.preventDefault();
                });

                let searchTimer = null;
                let inflight = null;
                function showDropdown(show) {
                    show ? itemDrop.removeAttribute('hidden') : itemDrop.setAttribute('hidden', '');
                }
                function renderResults(list) {
                    if (!list || !list.length) {
                        itemDrop.innerHTML = '<div class="dd-empty">No items found</div>';
                        return;
                    }
                    itemDrop.innerHTML = '';
                    list.forEach(it => {
                        const btn = document.createElement('button');
                        btn.type = 'button';
                        btn.className = 'dd-row';
                        btn.setAttribute('role', 'option');
                        btn.dataset.id = it.id;
                        btn.innerHTML = `<span class="dd-name"></span>`;
                        btn.children[0].textContent = it.name || '';
                        btn.addEventListener('click', () => {
                            itemId.value = String(it.id);
                            itemSearch.value = it.name || ('#' + it.id);
                            showDropdown(false);
                            loadYears(itemId.value); // auto-set current year if available
                        });
                        itemDrop.appendChild(btn);
                    });
                }
                async function searchItems(q) {
                    try {
                        if (inflight)
                            inflight.abort();
                        inflight = new AbortSignal ? new AbortController() : null;
                    } catch (e) {
                        inflight = null;
                    }
                    try {
                        const opts = {headers: {'Accept': 'application/json'}};
                        if (inflight && inflight.signal)
                            opts.signal = inflight.signal;
                        const r = await fetch('report-items?q=' + encodeURIComponent(q || ''), opts);
                        if (!r.ok) {
                            itemDrop.innerHTML = '<div class="dd-empty">Search failed</div>';
                            showDropdown(true);
                            return;
                        }
                        const list = await r.json();
                        renderResults(list);
                        showDropdown(true);
                    } catch (e) {
                        if (e.name !== 'AbortError') {
                            itemDrop.innerHTML = '<div class="dd-empty">Search error</div>';
                            showDropdown(true);
                        }
                    }
                }
                function debouncedSearch() {
                    clearTimeout(searchTimer);
                    searchTimer = setTimeout(() => searchItems(itemSearch.value.trim()), 250);
                }
                itemSearch.addEventListener('input', debouncedSearch);
                itemSearch.addEventListener('focus', () => {
                    debouncedSearch();
                });
                btnSearch.addEventListener('click', () => searchItems(itemSearch.value.trim()));
                document.addEventListener('click', (e) => {
                    if (!itemDrop.contains(e.target) && e.target !== itemSearch && e.target !== btnSearch)
                        showDropdown(false);
                });
                itemSearch.addEventListener('keydown', (e) => {
                    if (e.key === 'Escape')
                        showDropdown(false);
                    if (e.key === 'Enter') {
                        e.preventDefault();
                        if (!itemId.value)
                            debouncedSearch();
                    }
                });

                /* ===== Years loader with auto-default current year ===== */
                function clearYears(disable = true) {
                    yearSel.innerHTML = '<option value="">— Select Year —</option>';
                    yearSel.disabled = !!disable;
                    yearMenu.innerHTML = '';
                    setYearDisplay('— Select Year —');
                }
                async function loadYears(item) {
                    clearYears(true);
                    if (!item)
                        return;
                    try {
                        const res = await fetch('reports-years?item=' + encodeURIComponent(item), {
                            cache: 'no-store', headers: {'Accept': 'application/json'}
                        });
                        const text = await res.text();
                        let arr;
                        try {
                            arr = JSON.parse(text);
                        } catch (_) {
                            console.error('Non-JSON from /reports-years:', text);
                            return;
                        }
                        if (Array.isArray(arr) && arr.length) {
                            buildYearMenu(arr);

                            const chosenYearVal = '<%= (chosenYear==null) ? "" : chosenYear.toString() %>';
                            if (chosenYearVal && arr.includes(parseInt(chosenYearVal, 10))) {
                                yearSel.value = chosenYearVal;
                                setYearDisplay(chosenYearVal);
                            } else {
                                const nowY = new Date().getFullYear();
                                if (arr.includes(nowY)) {
                                    yearSel.value = String(nowY);
                                    setYearDisplay(String(nowY));
                                }
                            }
                        } else {
                            yearSel.disabled = true;
                        }
                    } catch (err) {
                        console.error('Failed to load years:', err);
                        yearSel.disabled = true;
                    }
                }

                // Prefill after SSR
                (function prefill() {
                    const name = (preItemName && preItemName.value) || '';
                    if (name)
                        itemSearch.value = name;
                    const chosen = '<%= (request.getParameter("item")==null) ? "" : request.getParameter("item") %>';
                    if (chosen) {
                        loadYears(chosen);
                    }
                })();

                // Guards
                window.ensureSelections = function () {
                    if (!itemId.value) {
                        alert('Please select an Item.');
                        return false;
                    }
                    if (yearSel.disabled || !yearSel.value) {
                        alert('Please select a Year.');
                        return false;
                    }

                    // For normal Generate (GET) – clear export and reset method
                    if (exportTypeEl) {
                        exportTypeEl.value = "";
                    }
                    form.method = "get";

                    return true;
                };

                /* ===== Export modal logic ===== */
                const overlay = document.getElementById('exportOverlay');
                const openExportBtn = document.getElementById('openExportModal');
                const closeExportBtn = document.getElementById('closeExportModal');
                const btnExportPdf = document.getElementById('btnExportPdf');
                const btnExportExcel = document.getElementById('btnExportExcel');

                function openExport() {
                    if (!openExportBtn || openExportBtn.disabled)
                        return;
                    overlay.hidden = false;
                    document.body.classList.add('modal-open');
                }
                function closeExport() {
                    overlay.hidden = true;
                    document.body.classList.remove('modal-open');
                }

                function doExport(type) {
                    if (!itemId.value || yearSel.disabled || !yearSel.value) {
                        alert('Please generate a report first (Item + Year).');
                        return;
                    }
                    if (!exportTypeEl)
                        return;
                    exportTypeEl.value = type;
                    form.method = "post";
                    form.submit();
                    closeExport();
                }

                if (openExportBtn) {
                    openExportBtn.addEventListener('click', openExport);
                }
                if (closeExportBtn) {
                    closeExportBtn.addEventListener('click', closeExport);
                }
                overlay.addEventListener('click', function (e) {
                    if (e.target === overlay) {
                        closeExport();
                    }
                });
                document.addEventListener('keydown', function (e) {
                    if (e.key === 'Escape' && !overlay.hidden) {
                        closeExport();
                    }
                });
                if (btnExportPdf) {
                    btnExportPdf.addEventListener('click', function () {
                        doExport('pdf');
                    });
                }
                if (btnExportExcel) {
                    btnExportExcel.addEventListener('click', function () {
                        doExport('excel');
                    });
                }

                /* ===== Zoom (minimal) ===== */
                const frame = document.getElementById('reportFrame');
                const canvas = document.getElementById('reportCanvas');
                const range = document.getElementById('zoomRange');
                const btnIn = document.getElementById('zoomIn');
                const btnOut = document.getElementById('zoomOut');
                const btnReset = document.getElementById('zoomReset');

                const BASE_SCALE = 0.70;
                function updateOverflow(pct) {
                    if (!frame || !canvas)
                        return;
                    frame.style.overflowX = (pct > 100) ? 'auto' : 'hidden';
                    const h = canvas.getBoundingClientRect().height;
                    frame.style.overflowY = (h > frame.clientHeight + 1) ? 'auto' : 'hidden';
                }
                function applyZoom(pct) {
                    if (!canvas || !frame)
                        return;
                    pct = Math.max(60, Math.min(200, parseInt(pct, 10) || 100));
                    const scale = BASE_SCALE * (pct / 100);
                    canvas.style.transform = 'scale(' + scale + ')';
                    canvas.style.width = (100 / scale) + '%';
                    canvas.style.transformOrigin = 'top left';
                    range.value = String(pct);
                    btnReset.textContent = pct + '%';
                    updateOverflow(pct);
                }
                if (!canvas) {
                    [range, btnIn, btnOut, btnReset].forEach(el => {
                        if (el)
                            el.disabled = true;
                    });
                } else {
                    [range, btnIn, btnOut, btnReset].forEach(el => {
                        if (el)
                            el.disabled = false;
                    });
                    applyZoom(100);
                    btnIn.addEventListener('click', () => applyZoom(parseInt(range.value, 10) + 10));
                    btnOut.addEventListener('click', () => applyZoom(parseInt(range.value, 10) - 10));
                    btnReset.addEventListener('click', () => applyZoom(100));
                    range.addEventListener('input', () => applyZoom(parseInt(range.value, 10)));
                    window.addEventListener('resize', () => updateOverflow(parseInt(range.value, 10) || 100));
                }
            })();
        </script>
    </body>
</html>

