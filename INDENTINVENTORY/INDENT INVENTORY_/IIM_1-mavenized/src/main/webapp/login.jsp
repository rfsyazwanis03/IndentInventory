<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8"/>
<title>Indent Inventory</title>
<meta name="viewport" content="width=device-width, initial-scale=1"/>
<link href="<%=request.getContextPath()%>/css/theme.css?v=6" rel="stylesheet"/>
<link href="<%=request.getContextPath()%>/css/login.css?v=6" rel="stylesheet"/>
<link href="<%=request.getContextPath()%>/css/footer.css?v=4" rel="stylesheet"/>
<link rel="icon" type="image/svg+xml" href="data:image/svg+xml, <svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'><rect width='8' height='100' x='5' fill='black'/><rect width='5' height='100' x='20' fill='black'/><rect width='10' height='100' x='30' fill='black'/><rect width='6' height='100' x='50' fill='black'/><rect width='8' height='100' x='65' fill='black'/><rect width='5' height='100' x='80' fill='black'/></svg>">
</head>
<body>
<header class="header">
  <img class="logo" src="<%=request.getContextPath()%>/assets/img/logo.png" alt="Hospital Logo">
</header>

<div class="wrapper">
  <h1 class="page-title">Indent Inventory</h1>
  <main class="login-card">
    <p class="page-subtitle">Login</p>
    <form method="post" action="login" autocomplete="on">
      <label class="field" for="username">
        <span class="icon" aria-hidden="true">
          <svg viewBox="0 0 24 24"><path d="M12 12c2.97 0 5.4-2.43 5.4-5.4S14.97 1.2 12 1.2 6.6 3.63 6.6 6.6 9.03 12 12 12zm0 2.4c-4.02 0-9.6 2.01-9.6 6v1.8h19.2V20.4c0-3.99-5.58-6-9.6-6z"/></svg>
        </span>
        <input class="input" id="username" type="text" name="username" placeholder="Username" required autofocus>
      </label>
      <label class="field" for="password">
        <span class="icon" aria-hidden="true">
          <svg viewBox="0 0 24 24"><path d="M17 9h-1V7a4 4 0 10-8 0v2H7a2 2 0 00-2 2v8a2 2 0 002 2h10a2 2 0 002-2v-8a2 2 0 00-2-2zm-6 0V7a2 2 0 114 0v2h-4z"/></svg>
        </span>
        <input class="input" id="password" type="password" name="password" placeholder="Password" required>
      </label>
      <div class="helper-row">
        <label><input type="checkbox" name="remember"> Remember me</label>
      </div>
      <button class="btn" type="submit">Login</button>
      <%
        String err = (String) request.getAttribute("error");
        if (err != null) {
      %>
        <p class="error-msg"><%= err %></p>
      <% } %>
    </form>
  </main>
</div>

<%@ include file="footer.jsp" %>
</body>
</html>
