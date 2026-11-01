---
name: srs-documentation
description: Specialized software requirement engineering skill for the Cinema Management System (SE2053-SWP391-G2). Use whenever creating, updating, auditing, or reverse-engineering Software Requirement Specification (SRS) documents, Use Case specifications, Business Rules (GB-xx), or Requirements Traceability Matrices (RTM) from the existing Java/SQL codebase.
---

# Cinema Management System — SRS Documentation Skill

This skill provides automated standards, templates, and reverse-engineering workflows to author and maintain the **Software Requirement Specification (SRS)** for the Cinema Management System (`SE2053-SWP391-G2`) according to **IEEE 830 / ISO/IEC/IEEE 29148** and FPT SWP391 academic standards.

---

## 1. Core Principles

1. **Code-to-Spec Fidelity (Reverse Sync):** Every specification statement must reflect the actual Java code (`vn.edu.fpt.*`) and SQL Server schema (`sql/*.sql`). Never invent hypothetical features.
2. **Canonical Identifiers:** Always strictly reuse established project IDs:
   - Use Cases: `UC-01` to `UC-32`
   - Business Rules: `GB-01` to `GB-12`
   - Background Jobs: `JOB-01` to `JOB-03`
   - System Messages: `MSG01`, `MSG02`, `MSG08`, `MSG09`, etc.
   *(Refer to [references/cinema-spec-catalog.md](references/cinema-spec-catalog.md) for the master index).*
3. **Actor Precision:** Use only the 5 authenticated system roles + 2 system/guest actors:
   - `Guest` (Unauthenticated visitor)
   - `Customer` (Registered cinema user)
   - `Staff` (Counter / Ticket checker / POS operator)
   - `Manager` (Schedule, movie, room, voucher & price manager)
   - `Admin` (User account, permission, system configuration & log manager)
   - `System / Scheduler` (Automated background executor)
   - `Payment Gateway` (External third-party: VNPay / SePay)

---

## 2. Specification Workflow: How to Update a Use Case

When the user asks to write or update a Use Case (e.g., *"Cập nhật Use Case UC-14 theo code hiện tại"*):

```
1. Code Inspection ──> 2. Business Rule Check ──> 3. Draft Fully-Dressed Spec ──> 4. Update RTM Entry
(Read Controller,      (Cross-check GB-xx,         (Generate standardized         (Map to endpoints
 DAO, Models, SQL)      config keys, Jobs)          Markdown table)                & DB tables)
```

### Step 1: Read the implementation files
- Controller/Servlet: Inspect `@WebServlet`, `doGet`, `doPost`, parameters, validation, and session handling.
- Service & DAO: Check business logic, exception handling, calculations, and SQL queries.
- SQL Migrations: Check table schema, constraints, data types, and default values.

### Step 2: Cross-check Business Rules (`GB-xx`)
Identify which `GB-xx` rules apply (e.g., `GB-01` seat hold 10 mins, `GB-06` cancellation voucher 80%, `GB-08` showtime buffer +15 mins).

### Step 3: Format using the Fully Dressed Template
Output the specification using the standard table format defined in [references/use-case-template.md](references/use-case-template.md).

---

## 3. Standard SRS Document Outline (IEEE 830 Format)

Ensure generated sections fit seamlessly into the group's final SRS document:

```text
1. INTRODUCTION
   1.1 Purpose
   1.2 Document Conventions (UC-xx, GB-xx, MSGxx)
   1.3 Intended Audience & Reading Suggestions
   1.4 Product Scope
   1.5 References
2. OVERALL DESCRIPTION
   2.1 Product Perspective & Context
   2.2 User Classes and Characteristics (RBAC Roles)
   2.3 Operating Environment (Java 21, Tomcat 10, SQL Server, VNPay)
   2.4 Design and Implementation Constraints (MVC, HikariCP, no ORM)
   2.5 Assumptions and Dependencies
3. SYSTEM FEATURES & USE CASE SPECIFICATIONS
   3.1 Actor-Specific Use Case Diagrams
   3.2 Detailed Use Case Specifications (UC-01 through UC-32)
   3.3 Global Business Rules Catalog (GB-01 through GB-12)
4. EXTERNAL INTERFACE REQUIREMENTS
   4.1 User Interfaces (JSP View Mapping)
   4.2 Hardware Interfaces (POS barcode/QR scanner, thermal ticket printer)
   4.3 Software & Database Interfaces (SQL Server JDBC, HikariCP)
   4.4 Communications Interfaces (VNPay HMAC-SHA512 Webhook, Jakarta Mail SMTP)
5. NON-FUNCTIONAL REQUIREMENTS (NFR)
   5.1 Performance (Seat lock concurrency, response time < 2s)
   5.2 Security (BCrypt password hash, CSRF tokens, XSS escaping, SQL parameterization)
   5.3 Reliability & Availability (JOB recovery, idempotency)
6. REQUIREMENTS TRACEABILITY MATRIX (RTM)
   (UC <-> Controller <-> Service <-> DAO <-> SQL Table)
```

---

## 4. Reverse-Engineering Verification Checklist

Before presenting any drafted SRS section, verify:
- [ ] Actor matches the controller URL prefix (e.g. `/customer/*` = Customer, `/admin/*` = Admin).
- [ ] Normal Flow steps describe user action $\rightarrow$ system response sequentially.
- [ ] Alternative flows cover optional paths (e.g., applying voucher, paying via counter vs online).
- [ ] Exception flows cover real validation errors and error messages (`MSGxx`) in code.
- [ ] Database updates match actual `INSERT`/`UPDATE` operations in the DAO.
- [ ] Any referenced background jobs (`JOB-xx`) or configurations match code properties.
