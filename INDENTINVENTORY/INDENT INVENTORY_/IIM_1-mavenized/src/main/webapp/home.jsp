<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.iim.models.User" %>
<%
  // Get logged-in user + role (align this with your header.jsp logic)
  User me = (User) session.getAttribute("user");
  String role = (me != null && me.getCategory() != null) ? me.getCategory().toLowerCase() : "";
  boolean isAdmin = "admin".equals(role) || "superadmin".equals(role);
%>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <title>Indent Inventory</title>
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <meta name="theme-color" content="#94765e"/>

  <link href="css/home.css" rel="stylesheet" />
  <link href="css/footer.css" rel="stylesheet" />
  <link href="css/header.css" rel="stylesheet" />

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
  <link rel="stylesheet"
        href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.2/css/all.min.css"/>
</head>
<body>
  <%@ include file="header.jsp" %>

  <main class="main" id="main">
    <h1 class="page-title" aria-label="Indent Inventory">Indent Inventory</h1>

    <section class="carousel" aria-roledescription="carousel">
      <button class="nav-btn left" aria-label="Previous">
        <i class="fa-solid fa-angle-left" aria-hidden="true"></i>
      </button>

      <div class="viewport" id="viewport">
        <div class="track" id="track">

          <a class="card" href="inventory.jsp">
            <i class="fa-solid fa-boxes-stacked"></i>
            <h3>Stock</h3>
            <p>View and manage item quantities.</p>
            <button class="card-btn" type="button">Open</button>
          </a>

          <a class="card" href="reports.jsp">
            <i class="fa-solid fa-chart-line"></i>
            <h3>Reports</h3>
            <p>Insights and performance analytics.</p>
            <button class="card-btn" type="button">View</button>
          </a>

          <a class="card" href="<%= request.getContextPath() %>/quickscan">
            <i class="fa-solid fa-barcode"></i>
            <h3>Quick Scan</h3>
            <p>Fast barcode entry and lookup.</p>
            <button class="card-btn" type="button">Scan</button>
          </a>

          <a class="card" href="alert.jsp">
            <i class="fa-solid fa-bell"></i>
            <h3>Alerts</h3>
            <p>Low stock & reminder notifications.</p>
            <button class="card-btn" type="button">Check</button>
          </a>

          <a class="card" href="department.jsp">
            <i class="fa-solid fa-building-user"></i>
            <h3>Departments</h3>
            <p>Organize items by department.</p>
            <button class="card-btn" type="button">Browse</button>
          </a>

          <a class="card" href="log.jsp">
            <i class="fa-solid fa-clipboard-list"></i>
            <h3>Log</h3>
            <p>System activity and usage history.</p>
            <button class="card-btn" type="button">See Log</button>
          </a>

          <% if (isAdmin) { %>
          <a class="card" href="user.jsp">
            <i class="fa-solid fa-users-gear"></i>
            <h3>Manage User</h3>
            <p>Administer system accounts.</p>
            <button class="card-btn" type="button">Manage</button>
          </a>
          <% } %>

        </div>
      </div>

      <button class="nav-btn right" aria-label="Next">
        <i class="fa-solid fa-angle-right" aria-hidden="true"></i>
      </button>
    </section>
  </main>

  <%@ include file="footer.jsp" %>

  <script>
  // ===== Fit-to-page (no scroll; header/footer fixed) =====
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
  // ===== Carousel logic (hover-scroll + buttons + active state) =====
  (function(){
    const viewport = document.getElementById('viewport');
    const track = document.getElementById('track');
    const btnLeft = document.querySelector('.nav-btn.left');
    const btnRight = document.querySelector('.nav-btn.right');

    const getGap = () => {
      const s = getComputedStyle(track);
      const g = s.columnGap || s.gap || "0";
      return parseFloat(g) || 0;
    };
    const centerToCard = (card, smooth=true) => {
      if(!card) return;
      const vp = viewport.getBoundingClientRect();
      const cr = card.getBoundingClientRect();
      const delta = (cr.left + cr.width/2) - (vp.left + vp.width/2);
      viewport.scrollBy({ left: delta, behavior: smooth ? 'smooth' : 'auto' });
    };
    const setActive = (card) => {
      [...track.querySelectorAll('.card')].forEach(c => c.classList.remove('active'));
      if(card) card.classList.add('active');
    };

    const originals = [...track.children];
    const clonesLeft  = originals.map(c => c.cloneNode(true));
    const clonesRight = originals.map(c => c.cloneNode(true));
    track.prepend(...clonesLeft);
    track.append(...clonesRight);

    const gap = getGap();
    const itemWidth = originals[0].getBoundingClientRect().width + gap;
    const setWidth = itemWidth * originals.length;

    viewport.scrollLeft = setWidth;

    const allCards = [...track.querySelectorAll('.card')];
    const qs = allCards.filter(c => /Quick Scan/i.test(c.textContent))
                       .sort((a,b)=>{
                         const mid = viewport.getBoundingClientRect().left + viewport.clientWidth/2;
                         return Math.abs(a.getBoundingClientRect().left + a.offsetWidth/2 - mid)
                              - Math.abs(b.getBoundingClientRect().left + b.offsetWidth/2 - mid);
                       })[0];
    centerToCard(qs, false);
    setActive(qs);

    let rafId = null, vx = 0;
    function loopScroll(){
      viewport.scrollLeft += vx;
      if (viewport.scrollLeft < setWidth * 0.5) viewport.scrollLeft += setWidth;
      else if (viewport.scrollLeft > setWidth * 1.5) viewport.scrollLeft -= setWidth;
      rafId = requestAnimationFrame(loopScroll);
    }
    function startLoop(){ if(!rafId) rafId = requestAnimationFrame(loopScroll); }
    function stopLoop(){ if(rafId){ cancelAnimationFrame(rafId); rafId=null; } vx = 0; }

    viewport.addEventListener('mousemove', (e)=>{
      const rect = viewport.getBoundingClientRect();
      const mid = rect.left + rect.width/2;
      const dead = rect.width * 0.20;
      const L = mid - dead, R = mid + dead;
      let newVx = 0;
      if (e.clientX < L){
        const dist = (L - e.clientX) / (L - rect.left);
        newVx = -(0.35 + 1.2*dist) * itemWidth;
      } else if (e.clientX > R){
        const dist = (e.clientX - R) / (rect.right - R);
        newVx = (0.35 + 1.2*dist) * itemWidth;
      }
      vx = newVx / 60;
      if (Math.abs(vx) > 0.01) startLoop(); else stopLoop();
    });
    viewport.addEventListener('mouseleave', stopLoop);

    track.addEventListener('mouseover', (e)=>{
      const card = e.target.closest('.card'); if(!card) return;
      setActive(card);
    });

    const step = () => itemWidth;
    btnRight.addEventListener('click', ()=> viewport.scrollBy({left: step(),  behavior:'smooth'}));
    btnLeft .addEventListener('click', ()=> viewport.scrollBy({left:-step(), behavior:'smooth'}));

    window.addEventListener('resize', ()=> { centerToCard(qs, false); setActive(qs); });
  })();
  </script>
</body>
</html>
