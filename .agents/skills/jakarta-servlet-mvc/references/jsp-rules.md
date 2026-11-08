# JSP 3.1 & JSTL 3.0 Presentation Reference Guide

This guide defines the view layer standards for `.jsp` and `.tag` files in `SE2053-SWP391-G2`.

---

## 1. Required Taglib Declarations (Jakarta EE 10 / Tomcat 10)

Always declare taglibs at the top of JSP views using `jakarta.tags.*`:

```jsp
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
```

> **Warning:** Do NOT use the legacy Java EE URI `http://java.sun.com/jsp/jstl/core` or `http://xmlns.jcp.org/...`. Tomcat 10+ with JSTL 3.0 expects `jakarta.tags.core`.

---

## 2. No Scriptlet Policy

* ❌ **Forbidden:**
  ```jsp
  <%-- NEVER DO THIS --%>
  <%
      List<Movie> movies = (List<Movie>) request.getAttribute("movies");
      for (Movie m : movies) {
  %>
      <div><%= m.getTitle() %></div>
  <% } %>
  ```

* ✅ **Allowed (JSTL + EL):**
  ```jsp
  <c:forEach var="movie" items="${movies}">
      <div class="movie-card">
          <h3><c:out value="${movie.title}" /></h3>
          <p>Thời lượng: ${movie.duration} phút</p>
          <p>Khởi chiếu: <fmt:formatDate value="${movie.releaseDate}" pattern="dd/MM/yyyy" /></p>
      </div>
  </c:forEach>
  ```

---

## 3. Flash Messages & Alerts

Render notifications forwarded from Session / Request:

```jsp
<c:if test="${not empty flashSuccess}">
    <div class="alert alert-success alert-dismissible fade show" role="alert">
        <i class="bi bi-check-circle-fill me-2"></i>${flashSuccess}
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    </div>
</c:if>

<c:if test="${not empty flashError}">
    <div class="alert alert-danger alert-dismissible fade show" role="alert">
        <i class="bi bi-exclamation-triangle-fill me-2"></i>${flashError}
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    </div>
</c:if>
```

---

## 4. CSRF Token in Forms

Every `<form method="post">` must embed the CSRF token:

```jsp
<form action="${pageContext.request.contextPath}/manager/movies" method="post">
    <!-- CSRF Token Hidden Input -->
    <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}" />
    <input type="hidden" name="action" value="create" />

    <div class="mb-3">
        <label for="title" class="form-label">Tên phim</label>
        <input type="text" class="form-control" id="title" name="title" required value="${param.title}" />
    </div>

    <button type="submit" class="btn btn-primary">Lưu phim</button>
</form>
```

---

## 5. Currency & Date Formatting

Use `<fmt:formatNumber>` and `<fmt:formatDate>`:

```jsp
<!-- Format Currency VND: 100.000 đ -->
<span><fmt:formatNumber value="${ticket.price}" type="currency" currencySymbol="đ" maxFractionDigits="0" /></span>

<!-- Format Date: 28/10/2026 19:30 -->
<span><fmt:formatDate value="${showtime.startTime}" pattern="dd/MM/yyyy HH:mm" /></span>
```
