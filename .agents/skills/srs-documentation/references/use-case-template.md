# Fully-Dressed Use Case Specification Template

Use this standard template when drafting or updating any Use Case in `Group2_SRS`. It complies with IEEE 830 and FPT University SWP391 requirements.

---

## Use Case Specification: [UC-ID] [Use Case Name]

| Field | Description / Value |
| :--- | :--- |
| **Use Case ID** | `UC-xx` *(e.g., UC-14)* |
| **Use Case Name** | `[Action Verb] + [Noun Phrase]` *(e.g., Make Online Payment)* |
| **Actor(s)** | Primary: `Customer` / `Staff` / `Manager` / `Admin` / `Guest`<br>Secondary: `System / Scheduler`, `Payment Gateway (VNPay)` |
| **Brief Description** | 2-3 sentences explaining the goal and high-level interaction. |
| **Preconditions** | What must be true before this use case can begin (e.g., User is logged in, Booking status is `Pending`, Seats are held). |
| **Trigger** | The event that starts the use case (e.g., User clicks "Thanh toán ngay" button). |
| **Associated Business Rules** | Specific `GB-xx` rules enforced in this UC (e.g., `GB-01`, `GB-03`, `GB-05`). |
| **Postconditions (Success)** | State of the system after successful completion (e.g., Booking status becomes `Paid`, tickets generated, loyalty points added). |
| **Postconditions (Failure)** | State of the system if flow fails (e.g., Seat hold released or kept in `Pending` until expiry, transaction logged as failed). |

---

### Main Flow (Normal Scenario / Happy Path)

| Step | Actor Action | System Response |
| :---: | :--- | :--- |
| **1** | Actor triggers action on page `[URL/View]`. | System validates authorization and displays `[View/Form]`. |
| **2** | Actor enters required inputs `[Fields]` and submits. | System intercepts request via `[Servlet/Endpoint]` and validates input syntax. |
| **3** | ... | System invokes `[Service]`, checks business rules `[GB-xx]`, and queries `[DAO]`. |
| **4** | ... | System completes transaction, commits changes to database tables `[Tables]`. |
| **5** | ... | System sets flash notification message `[MSGxx]` and redirects user to `[Destination URL]` (PRG pattern). |

---

### Alternative Flows

* **[Step#]a. [Condition Title]** (e.g., *3a. User applies a discount voucher*):
  1. Actor inputs voucher code and clicks "Áp dụng".
  2. System verifies voucher constraints against `GB-05` (valid date, usage limit, minimum order amount).
  3. System recalculates `total_price` and updates summary view.
  4. Flow returns to Step 4 of Main Flow.

---

### Exception Flows

* **[Step#]b. [Error Condition]** (e.g., *2b. Input validation fails / Required field empty*):
  1. System detects invalid parameter format or missing mandatory field.
  2. System records warning log and sets error message `MSG02 ("The * field is required.")`.
  3. System forwards back to original form, preserving previously entered valid fields.
  4. Use case terminates or waits for user correction.

* **[Step#]c. [Payment/Transaction Error]** (e.g., *4b. Payment Gateway Checksum Mismatch / Timeout*):
  1. System detects invalid HMAC-SHA512 signature or transaction timeout.
  2. System logs severe security alert with event type `PAYMENT_FAILED`.
  3. System flags transaction as `ReviewRequired` or cancels booking.
  4. System redirects user to failure notification view with error details.

---

### Technical Traceability (For Internal Verification)

* **Controller / Servlet:** `vn.edu.fpt.controller.[actor].[ServletName]`
* **Service Layer:** `vn.edu.fpt.service.[ServiceName]`
* **DAO / Database:** `vn.edu.fpt.dao.[DaoName]` $\rightarrow$ Tables: `[Table1]`, `[Table2]`
* **JSP View:** `/WEB-INF/views/[actor]/[view-name].jsp`
* **Config Keys:** `[config.properties key or SystemConfig table key]`
