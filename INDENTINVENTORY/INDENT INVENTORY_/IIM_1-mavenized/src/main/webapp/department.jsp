<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.*, com.iim.models.*, com.iim.dao.*" %>
<%@ page isELIgnored="true" %>
<%
    com.iim.models.User me = (com.iim.models.User) session.getAttribute("user");
    if (me == null) {
        response.sendRedirect("login.jsp");
        return;
    }

    List<Department> deps = (List<Department>) request.getAttribute("departments");
%>
<!DOCTYPE html>
<html lang="en">
    <head>
        <meta charset="UTF-8" />
        <title>Indent Inventory</title>
        <meta name="viewport" content="width=device-width, initial-scale=1" />
        <meta name="theme-color" content="#94765e"/>

        <!-- Theme tokens (must be first) -->
        <link href="<%=request.getContextPath()%>/css/theme.css" rel="stylesheet"/>
        <!-- Site chrome -->
        <link href="<%=request.getContextPath()%>/css/header.css" rel="stylesheet"/>
        <link href="<%=request.getContextPath()%>/css/footer.css" rel="stylesheet"/>
        <!-- Page CSS -->
        <link href="<%=request.getContextPath()%>/css/departments.css?v=1" rel="stylesheet"/>

        <link rel="stylesheet"
              href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.2/css/all.min.css"/>

        <!-- Favicon (barcode) -->
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
    <body class="layout">
        <%@ include file="header.jsp" %>

        <main class="page" id="page">
            <h1 class="page-title">Departments</h1>

            <section class="dept-wrap">
                <!-- Left: list -->
                <aside class="dept-list">
                    <div class="search-pill">
                        <i class="fa-solid fa-magnifying-glass"></i>
                        <input id="q" class="search-input" placeholder="Search by code or name…" autocomplete="off"/>
                        <button id="btnSearch" class="search-btn" type="button" title="Search"></button>
                    </div>

                    <div class="tbl-head">
                        <span class="col-code">DEPT CODE</span>
                        <span class="col-name">NAME</span>
                        <span class="col-status">STATUS</span>
                    </div>

                    <div id="list" class="tbl-body" role="listbox" aria-label="Departments">
                        <% if (deps != null) {
                                for (Department d : deps) {
                                    String st = (d.getStatus() == null ? "" : d.getStatus());
                                    String stLower = st.toLowerCase();
                                    String stPretty = stLower.isEmpty() ? "" : (Character.toUpperCase(stLower.charAt(0)) + stLower.substring(1));
                                    String codeUp = (d.getCode() == null) ? "" : d.getCode().toUpperCase();
                                    String nameUp = (d.getName() == null) ? "" : d.getName().toUpperCase();
                        %>
                        <button class="row" type="button" data-id="<%= d.getId()%>">
                            <span class="col-code"><%= codeUp%></span>
                            <span class="col-name"><%= nameUp%></span>
                            <span class="col-status <%= "active".equals(stLower) ? "ok" : "off"%>"><%= stPretty%></span>
                        </button>
                        <%   }
                            }%>
                    </div>
                </aside>

                <!-- Right: detail -->
                <section class="dept-detail">
                    <div class="detail-topbar">
                        <div class="toolbar">
                            <button id="btnAdd" class="btn btn-outline" type="button">Add</button>
                            <button id="btnUpdate" class="btn btn-outline" type="button" disabled>Update</button>
                            <button id="btnSave" class="btn btn-primary" type="button" disabled>Save</button>
                            <button id="btnCancel" class="btn btn-ghost" type="button" disabled>Cancel</button>
                        </div>
                    </div>

                    <div class="detail-body">
                        <div class="card">
                            <div class="form-row">
                                <label class="lbl" for="code">Dept code</label>
                                <!-- Force uppercase visually and in value -->
                                <input id="code" class="input" type="text" maxlength="12"
                                       style="text-transform:uppercase"
                                       oninput="this.value=this.value.toUpperCase()" disabled/>
                            </div>

                            <div class="form-row">
                                <label class="lbl" for="name">Name</label>
                                <!-- Force uppercase visually and in value -->
                                <input id="name" class="input" type="text" maxlength="80"
                                       style="text-transform:uppercase"
                                       oninput="this.value=this.value.toUpperCase()" disabled/>
                            </div>

                            <!-- Status (blank by default; text + icon; editable only in Update/Add) -->
                            <div class="form-row">
                                <label class="lbl">Status</label>
                                <div class="status-toggle" id="statusWrap">
                                    <input type="hidden" id="status" value=""/>
                                    <span id="statusText" class="status-text"></span>
                                    <button id="statusToggle" type="button" class="status-btn" title="Click to toggle" disabled></button>
                                </div>
                            </div>

                            <p id="emptyHint" class="hint">
                                Select a department on the left, or click <b>Add</b> to create a new one.
                            </p>
                        </div>
                    </div>
                </section>
            </section>
        </main>

        <%@ include file="footer.jsp" %>

        <script>
            /* ===== Fit page height to viewport (no vertical scroll at 100% zoom) ===== */
            (function fitToPage() {
                var root = document.documentElement;
                function applySize() {
                    var hdr = document.querySelector('header.hdr');
                    var ftr = document.querySelector('footer');
                    var hHdr = hdr ? hdr.getBoundingClientRect().height : 0;
                    var hFtr = ftr ? ftr.getBoundingClientRect().height : 0;
                    root.style.setProperty('--main-h', (window.innerHeight - hHdr - hFtr) + 'px');
                }
                applySize();
                window.addEventListener('resize', applySize, {passive: true});
            })();
        </script>

        <script>
            (function () {
                var BASE = '<%= request.getContextPath()%>';
                var API = {
                    list: function(q) {
                        return BASE + '/departments' + (q ? ('?q=' + encodeURIComponent(q)) : '');
                    },
                    detail: function(id) {
                        return BASE + '/department?id=' + encodeURIComponent(id);
                    },
                    create: BASE + '/department-create',
                    update: BASE + '/department-update'
                };

                var $ = function (s, p) {
                    return (p || document).querySelector(s);
                };
                var listEl = $('#list');
                var qEl = $('#q');
                var btnSearch = $('#btnSearch');

                var codeEl = $('#code'), nameEl = $('#name');
                var btnAdd = $('#btnAdd'), btnUpdate = $('#btnUpdate'), btnSave = $('#btnSave'), btnCancel = $('#btnCancel');
                var emptyHint = $('#emptyHint');

                // Status elements
                var statusEl = document.getElementById('status');
                var statusText = document.getElementById('statusText');
                var statusBtn = document.getElementById('statusToggle');

                var current = null;
                var mode = 'view';

                // ---- Live search (debounced + cancellable) ----
                var searchTimer = null;
                var currentSearch = null; // AbortController

                function debouncedSearch() {
                    clearTimeout(searchTimer);
                    searchTimer = setTimeout(function () {
                        reloadList(qEl.value);
                    }, 250);
                }

                qEl.addEventListener('input', debouncedSearch);
                qEl.addEventListener('keydown', function (e) {
                    if (e.key === 'Enter') {
                        e.preventDefault();
                        reloadList(qEl.value);
                    }
                });
                btnSearch.addEventListener('click', function () {
                    reloadList(qEl.value);
                });

                // ---- Status rendering/toggle ----
                function setStatusUI(v) {
                    if (!v) {
                        statusEl.value = '';
                        statusText.textContent = '';
                        statusBtn.className = 'status-btn';
                        statusBtn.innerHTML = '';
                        return;
                    }
                    var isActive = String(v).toLowerCase() === 'active';
                    statusEl.value = isActive ? 'active' : 'inactive';
                    statusText.textContent = isActive ? 'Active' : 'Inactive';
                    statusBtn.className = 'status-btn ' + (isActive ? 'active' : 'inactive');
                    statusBtn.innerHTML = isActive ? '<i class="fa-solid fa-check"></i>' : '<i class="fa-solid fa-xmark"></i>';
                }

                statusBtn.addEventListener('click', function () {
                    if (statusBtn.disabled) return;
                    var next = statusEl.value === 'active' ? 'inactive' : 'active';
                    setStatusUI(next);
                });

                var setDisabled = function (flag) {
                    [codeEl, nameEl, statusEl].forEach(function (e) { e.disabled = flag; });
                    statusBtn.disabled = flag;
                };

                function setMode(next) {
                    mode = next;
                    var editing = (mode === 'add' || mode === 'update');
                    btnAdd.disabled = editing;
                    btnUpdate.disabled = !current || editing;
                    btnSave.disabled = !editing;
                    btnCancel.disabled = !editing;
                    setDisabled(!editing);
                    emptyHint.style.display = (current || editing) ? 'none' : 'block';
                }

                function fillForm(d) {
                    d = d || {};
                    // Render uppercase even for legacy data
                    codeEl.value = ((d.code || '')).toUpperCase();
                    nameEl.value = ((d.name || '')).toUpperCase();
                    setStatusUI(d.status || '');
                }

                function clearSel() {
                    var activeRows = listEl.querySelectorAll('.row.active');
                    Array.prototype.forEach.call(activeRows, function (x) {
                        x.classList.remove('active');
                    });
                }

                function toast(x) {
                    alert(x);
                }

                // ---- List reload (supports cancel) ----
                async function reloadList(q) {
                    try {
                        if (currentSearch && currentSearch.abort) {
                            currentSearch.abort();
                        }
                        if (window.AbortController) {
                            currentSearch = new AbortController();
                        } else {
                            currentSearch = null;
                        }

                        var fetchOptions = {
                            headers: {'Accept': 'application/json'}
                        };
                        if (currentSearch && currentSearch.signal) {
                            fetchOptions.signal = currentSearch.signal;
                        }

                        var r = await fetch(API.list(q), fetchOptions);
                        if (!r.ok) throw new Error(await r.text());
                        var arr = await r.json();
                        listEl.innerHTML = '';
                        if (!arr.length) {
                            var e = document.createElement('div');
                            e.className = 'empty';
                            e.textContent = 'No departments found';
                            listEl.appendChild(e);
                            return;
                        }
                        arr.forEach(function (d) {
                            var row = document.createElement('button');
                            row.type = 'button';
                            row.className = 'row';
                            row.dataset.id = d.id;
                            row.innerHTML =
                                '<span class="col-code"></span>' +
                                '<span class="col-name"></span>' +
                                '<span class="col-status ' +
                                (String(d.status).toLowerCase() === 'active' ? 'ok' : 'off') +
                                '"></span>';
                            // Uppercase in list too
                            row.children[0].textContent = (d.code || '').toUpperCase();
                            row.children[1].textContent = (d.name || '').toUpperCase();
                            row.children[2].textContent =
                                d.status
                                    ? (String(d.status).charAt(0).toUpperCase() +
                                       String(d.status).slice(1).toLowerCase())
                                    : '';
                            row.addEventListener('click', function () {
                                selectRow(row);
                            });
                            listEl.appendChild(row);
                        });
                    } catch (e) {
                        if (e.name !== 'AbortError') {
                            // optional: console.error(e);
                        }
                    }
                }

                async function selectRow(row) {
                    clearSel();
                    row.classList.add('active');
                    try {
                        var r = await fetch(API.detail(row.dataset.id), {headers: {'Accept': 'application/json'}});
                        if (!r.ok) throw new Error(await r.text());
                        current = await r.json();
                        fillForm(current);
                        setMode('view');
                    } catch (e) {
                        toast('Failed to load department.');
                    }
                }

                // Toolbar
                btnAdd.addEventListener('click', function () {
                    current = null;
                    fillForm({code: '', name: '', status: ''});
                    clearSel();
                    setMode('add');
                    codeEl.focus();
                });

                btnUpdate.addEventListener('click', function () {
                    if (!current) {
                        toast('Select a department first.');
                        return;
                    }
                    setMode('update');
                    nameEl.focus();
                });

                btnCancel.addEventListener('click', function () {
                    if (mode === 'add') {
                        current = null;
                        fillForm({});
                        clearSel();
                    }
                    if (mode === 'update') {
                        fillForm(current);
                    }
                    setMode('view');
                });

                btnSave.addEventListener('click', async function () {
                    // Ensure payload is uppercase (defensive)
                    var payload = {
                        id: (current && current.id != null) ? String(current.id) : "",
                        code: codeEl.value.trim().toUpperCase(),
                        name: nameEl.value.trim().toUpperCase(),
                        status: (statusEl.value || '')
                    };
                    if (!payload.code || !payload.name) {
                        toast('Code and Name are required.');
                        return;
                    }
                    try {
                        btnSave.disabled = true;
                        var url = (mode === 'add') ? API.create : API.update;
                        var r = await fetch(url, {
                            method: 'POST',
                            headers: {'Content-Type': 'application/json'},
                            body: JSON.stringify(payload)
                        });
                        var txt = await r.text();
                        if (!r.ok) throw new Error(txt);

                        toast('Saved successfully.');
                        await reloadList(qEl.value);
                        setMode('view');

                        // Find by uppercase code
                        var rows = listEl.querySelectorAll('.row');
                        var match = null;
                        Array.prototype.forEach.call(rows, function (rw) {
                            if (match) return;
                            var span = rw.querySelector('.col-code');
                            var codeTxt = span ? span.textContent : '';
                            if (codeTxt === payload.code) {
                                match = rw;
                            }
                        });

                        if (match) {
                            match.click();
                        } else {
                            current = null;
                            fillForm({});
                            clearSel();
                        }
                    } catch (e) {
                        toast('Save failed: ' + e.message);
                    } finally {
                        btnSave.disabled = false;
                    }
                });

                // boot
                setMode('view');
                if (!(listEl.children && listEl.children.length)) {
                    reloadList('');
                }
            })();
        </script>
    </body>
</html>
