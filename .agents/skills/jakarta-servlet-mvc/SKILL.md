---
name: jakarta-servlet-mvc
description: Specialized development skill for Java 21, Jakarta Servlet 6.0, JSP 3.1, JSTL 3.0, HikariCP, and MSSQL under MVC architecture. Use whenever writing, refactoring, debugging, or reviewing Servlets, Controllers, DAOs, Services, JSP pages, or database interactions in the Cinema Management System project.
---

# Jakarta Servlet 6.0 & MVC Architecture Skill

This skill enforces strict architecture, security, and coding conventions for the Cinema Management System (`SE2053-SWP391-G2`), targeting **Java 21**, **Tomcat 10+ (Jakarta EE 10)**, **MSSQL**, and **HikariCP**.

---

## 1. Core Platform Constraints (Non-Negotiable)

* **Namespace:** MUST ALWAYS use `jakarta.servlet.*` and `jakarta.servlet.http.*`. NEVER import `javax.servlet.*`.
* **Container:** Apache Tomcat 10.1.x / 11.x (Servlet 6.0, JSP 3.1, JSTL 3.0).
* **Connection Pooling:** MUST acquire connections via `vn.edu.fpt.util.DBContext.getConnection()`. NEVER create standalone `DriverManager.getConnection()`.
* **Resources Management:** MUST ALWAYS use `try-with-resources` for `Connection`, `PreparedStatement`, and `ResultSet`.

---

## 2. Layered Architecture (MVC)

Adhere strictly to the 4-tier MVC structure:

```
vn.edu.fpt
├── controller       # @WebServlet: HTTP Request/Response, Parsing, Session, Flash Messages, Dispatch
├── service          # Business logic, Multi-DAO orchestration, Transaction, ValidationException
├── dao              # SQL Queries, DBContext, PreparedStatement mapping, POJO hydration
├── model            # POJOs / Entities / DTOs / Enums (No SQL or Servlet imports)
└── util             # DBContext, CsrfToken, Messages, Page, Security, Helpers
```

### Flow of Execution
1. **HTTP Request** arrives at `Controller` (`@WebServlet`).
2. Controller validates inputs, checks session/CSRF, and delegates to `Service`.
3. Service enforces business rules and calls one or more `DAO`s.
4. DAO queries MSSQL via `DBContext` and returns `Model` objects.
5. Controller attaches data to `request.setAttribute(...)` and forwards to `/WEB-INF/views/...` (or redirects via PRG pattern).

---

## 3. Controller (Servlet) Best Practices

Refer to [references/controller-guide.md](references/controller-guide.md) for detailed code templates.

1. **Annotations:** Register servlets via `@WebServlet("/url/pattern")`. Avoid `web.xml` servlet mappings unless globally required.
2. **View Location:** JSPs MUST reside under `/WEB-INF/views/` to prevent direct client access.
3. **Encoding & Content Type:**
   - Handle UTF-8 encoding in `doGet` and `doPost` (or ensure `EncodingFilter` applies):
     ```java
     request.setCharacterEncoding("UTF-8");
     response.setContentType("text/html;charset=UTF-8");
     ```
4. **Post-Redirect-Get (PRG) Pattern:**
   - On successful `doPost` state change (Insert/Update/Delete), ALWAYS use `response.sendRedirect(request.getContextPath() + "...")`.
   - Use `HttpSession` flash messages (`session.setAttribute("message", ...)`) to display notifications after redirect.
   - Use `request.getRequestDispatcher(VIEW).forward(request, response)` ONLY for rendering views or returning form validation errors.
5. **CSRF Protection:**
   - Validate CSRF token on destructive state-changing POST requests using `CsrfToken.validate(request)`.

---

## 4. DAO & Database Access Rules

Refer to [references/dao-guide.md](references/dao-guide.md) for full DAO templates.

1. **SQL Injection Defense:** NEVER concatenate user input into SQL strings. ALWAYS use `?` placeholders with `PreparedStatement`.
2. **Connection Leak Prevention:**
   ```java
   String sql = "SELECT id, title, duration FROM Movies WHERE status = ?";
   try (Connection conn = DBContext.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
       ps.setString(1, status);
       try (ResultSet rs = ps.executeQuery()) {
           while (rs.next()) {
               // Map ResultSet to Model
           }
       }
   } catch (SQLException e) {
       LOGGER.log(Level.SEVERE, "Error querying movies", e);
       throw new RuntimeException("Database error occurred", e);
   }
   ```
3. **Transaction Management:** When executing multiple dependent queries (e.g. Booking + Ticket + Payment), manage transactions explicitly:
   - `conn.setAutoCommit(false);`
   - Execute queries, then `conn.commit();`
   - In `catch`, `conn.rollback();`
   - In `finally`, `conn.setAutoCommit(true);`

---

## 5. JSP & JSTL Presentation Rules

Refer to [references/jsp-rules.md](references/jsp-rules.md) for detailed JSP guidelines.

1. **NO Java Scriptlets:** Absolutely DO NOT write `<% ... %>`, `<%= ... %>`, or `<%! ... %>` in `.jsp` files.
2. **Expression Language (EL):** Use `${movie.title}` with null-safe operators.
3. **JSTL 3.0 Standard URIs:**
   ```jsp
   <%@ taglib prefix="c" uri="jakarta.tags.core" %>
   <%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
   <%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
   ```
   *(Notice `jakarta.tags.*` instead of legacy `http://java.sun.com/jsp/jstl/*`).*
4. **XSS Protection:**
   - Always output untrusted user input using `<c:out value="${userInput}" />` or ensure proper escaping.
5. **Asset Linking:** Always prefix static resources and links with context path:
   ```jsp
   <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/cineluxe.css">
   ```

---

## 6. Pre-Delivery Checklist for Any Java Web Task

Before finalizing any changes to Servlets, DAOs, or JSPs:
- [ ] No `javax.servlet` imports; all packages use `jakarta.servlet`.
- [ ] All `Connection`, `PreparedStatement`, `ResultSet` are wrapped in `try-with-resources`.
- [ ] No string concatenation in SQL statements (`?` parameters used everywhere).
- [ ] Servlets do not contain raw SQL statements.
- [ ] Forms submit with CSRF token; POST handlers validate CSRF token where required.
- [ ] JSP files contain zero `<% scriptlets %>` and use `jakarta.tags.*` JSTL tags.
- [ ] PRG pattern applied on POST actions (redirects after successful modifications).
