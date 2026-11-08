# UC-37 — Kết quả triển khai, kiểm tra và review

Ngày thực hiện: 06/11/2026. Chức năng: **Xem báo cáo doanh thu bán vé online**.

## 1. Kết quả

- Đã thêm đặc tả tiếng Anh theo bảng 15 mục vào phần **8. UC 37 View Online Ticket Sales Report** của Google Docs SRS và thêm dòng UC-37 vào bảng danh mục phần 7. Đã đọc lại để xác minh bảng, nội dung và các phần trước đó được giữ nguyên.
- Link SRS: https://docs.google.com/document/d/17T0gNOSK2dXy32OZps6nHkysTtHXuXEUJdG0dGS2ap0/edit
- Bản đặc tả lưu trong repo: `docs/UC37_USE_CASE.md`.
- Đã triển khai `/manager/online-reports`, thêm menu quản lý và quyền `VIEW_ONLINE_REPORTS` trong UC-31.
- Đã áp dụng migration bổ sung cho SQL Server local của dự án và xác minh chạy lại thành công.
- Maven package thành công; WAR tại `SE2053-SWP391-G2/target/SE2053-SWP391-G2.war`.

## 2. Chức năng đã có

1. Chọn nhanh Hôm nay / 7 ngày gần nhất / Tháng này hoặc khoảng ngày tùy chọn, tối đa 366 ngày.
2. Lọc phim, mã suất, mã đơn; tổng hợp toàn bộ kết quả, độc lập với phân trang 15 dòng.
3. Xem số đơn/vé gốc, giá trị vé và bắp nước trước giảm, giảm giá, tiền thu, tiền mặt hoàn, voucher hủy vé và thu trừ tiền mặt hoàn.
4. Biểu đồ tiền thu theo ngày và bảng số liệu cho tất cả ngày, gồm cả ngày không bán.
5. Ba phần: **Đơn đã thanh toán**, **Đổi / hoàn**, **Kiểm tra thanh toán**.
6. Chi tiết đơn online: vé gốc/vé đổi, bắp nước, mọi lần thử thanh toán và lịch sử điều chỉnh trên toàn thời gian.
7. CSV UTF-8 có BOM, đúng bộ lọc/cơ sở ngày, tối đa 10.000 dòng; vượt giới hạn bị từ chối trước khi trả file, không cắt âm thầm.
8. Manager được quyền mặc định; Staff cần được Admin cấp quyền. Trang, chi tiết và CSV dùng cùng endpoint được AuthFilter kiểm tra.
9. Giao diện có nhãn bộ lọc, lỗi validation, focus bàn phím, trạng thái rỗng và bảng cuộn trong vùng riêng trên mobile.

## 3. Quy tắc số liệu

| Nội dung | Cách xử lý |
|---|---|
| Đơn online | Theo `Booking.Channel = Online`, dù in/đổi/hoàn tại quầy |
| Ngày bán | `Booking.PaidAt` theo giờ Việt Nam; không dùng ngày tạo đơn hoặc ngày chiếu thay thế |
| Thành công | Booking Paid; đơn có tiền cần bằng chứng thanh toán Paid khớp Amount/ReceivedAmount và có PaidAt; đơn 0đ không cần giao dịch VNPay |
| Nhiều lần thử | Chỉ tính doanh số một lần theo Booking; xem riêng từng lần thử ở phần thanh toán |
| Vé đổi | Loại các Ticket được tham chiếu bằng NewTicketID/NewTicketID2 khỏi số vé bán gốc và giá trị vé bán ban đầu |
| Hoàn tiền | Theo ngày sự kiện `TicketExchange.CashRefund`, gồm cả xuống hạng hoàn chênh lệch và đơn bán kỳ trước |
| Hoàn voucher | Theo sự kiện `TicketCancellation`, tách khỏi tiền đã chi; voucher đền bù ghế hỏng được phân biệt ở loại xử lý |
| Thu trừ hoàn | Thu từ đơn online − tiền mặt hoàn trong kỳ; có thể âm, chưa trừ phí cổng |
| Giảm giá | Dùng DiscountAmount đã lưu, không trừ điểm lần hai và không tính lại theo giá hiện tại |
| Thử thanh toán | Theo ngày tạo OnlinePayment; CreatedAt UTC được đổi thành giờ Việt Nam khi lọc/hiển thị |
| Dữ liệu cũ | Thiếu timestamp/liên kết được cảnh báo; không gán ngày hoặc ghép voucher bằng phỏng đoán |

UC-29 vẫn dùng cơ sở ngày tạo đơn và hoàn trả gắn với đơn. Giao diện đã ghi rõ và có link sang báo cáo mới; số UC-29/UC-37 không mặc định bằng nhau khi dùng hai cơ sở ngày khác nhau.

## 4. Các file/hạng mục thay đổi

| Hạng mục | File |
|---|---|
| Schema/backfill/index | `sql/UC37_online_sales_report.sql` |
| Truy vấn báo cáo | `src/main/java/vn/edu/fpt/dao/OnlineSalesReportDAO.java` |
| Nghiệp vụ, validation, CSV | `src/main/java/vn/edu/fpt/service/OnlineSalesReportService.java` |
| Endpoint | `src/main/java/vn/edu/fpt/controller/manager/OnlineSalesReportServlet.java` |
| Dữ liệu trả về | `model/OnlineSalesFilter`, `OnlineSalesSummary`, `OnlineSalesRow`, `OnlineSalesResult`, `OnlineSalesDetail` |
| Ghi thời điểm Paid | `BookingDAO`, `BookingService`: cả VNPay và xác nhận đơn 0đ |
| Lịch sử hủy/voucher | `CustomerBookingDAO`, `TicketCancellationService`: ghi cùng transaction hủy vé |
| Mốc thời gian đổi/hoàn | `SeatExchangeDAO`: ghi CreatedAt rõ theo giờ Việt Nam |
| Quyền và thông điệp | `Permission`, `Messages` |
| Trang/partial/CSS | `manager/online-reports.jsp`, `common/online-report-payments.jsp`, `common/online-report-adjustments.jsp`, `assets/css/online-reports.css` |
| Điều hướng | `common/manager-sidebar.jsp`, `manager/analytics.jsp` |
| Hướng dẫn/spec | `README.md`, `docs/UC37_USE_CASE.md`, tài liệu này |
| Bộ kiểm tra | `src/test/java/vn/edu/fpt/service/OnlineSalesReportChecks.java`, `scripts/run-uc37-tests.ps1`, các script HTTP/browser |

Đường dẫn Java/JSP/SQL trong bảng tính từ `SE2053-SWP391-G2/`, trừ các file docs/README ở root repo.

## 5. Kiểm thử đã thực hiện

**106 kiểm tra đạt**, gồm:

| Nhóm | Số kiểm tra | Kết quả |
|---|---:|---|
| Unit và SQL tích hợp | 63 | PASS |
| HTTP/JSP/auth/CSV và trang hồi quy | 36 | PASS |
| Chrome desktop/mobile | 7 | PASS |

Những tình huống quan trọng được kiểm tra:

- Hôm nay theo Việt Nam khác ngày UTC; mốc tạo thanh toán UTC rơi sang ngày mới tại Việt Nam.
- Khoảng 366 ngày hợp lệ; 367 ngày, ngày không tồn tại, khoảng ngược và ID/status/tab sai bị từ chối.
- Hai lần Failed + một Paid không nhân tiền/vé/bắp nước; đơn Pending, Counter không vào doanh số online.
- Đơn 0đ có PaidAt, có vé và giảm giá; gọi chốt Paid lần hai bị từ chối.
- Vé cấp lại khi đổi ghế không tăng số vé bán/giá trị vé gốc; chi tiết vẫn giữ lịch sử.
- Đơn bán kỳ trước hoàn tiền hôm nay; voucher hoàn vé tách khỏi tiền mặt, không cộng trùng RefundAmount.
- Số thu trừ hoàn có thể âm; chuỗi ngày được điền 0; số trang rất lớn được giới hạn về trang có dữ liệu.
- Xuất trên 10.000 dòng bị chặn; CSV escape dấu phẩy, dấu nháy, xuống dòng và tiền tố công thức; số âm dạng số vẫn được giữ.
- Chi tiết đơn Counter trả 404; người chưa đăng nhập chuyển về login; Staff/Customer không có quyền bị 403 với cả trang, chi tiết và CSV.
- Admin nhìn thấy quyền mới trong UC-31; Manager mở được các trang UC-29, báo cáo ca và lịch sử quầy hiện có.
- Chrome 1440px và 375px: điều hướng tab, biểu mẫu, không tràn ngang toàn trang, bảng giữ độ rộng dễ đọc trong vùng cuộn, không có lỗi JavaScript chưa xử lý.
- Migration chạy lại thành công; sau kiểm tra không còn movie/booking/voucher fixture do bộ SQL tạo. Các tài khoản HTTP test riêng được vô hiệu hóa, lịch sử đăng nhập được giữ lại.

Evidence trong workspace: `D:/Ky9/SWP/outputs/uc37_20261106/` chứa `http-results.json`, `browser-results.json`, ảnh `report-desktop.png`, `report-mobile.png`, HTML/CSV đã đọc qua HTTP. Bộ SQL dùng transaction và rollback trong finally; không commit fixture tài chính.

Lệnh chạy lại:

```powershell
# Tại SE2053-SWP391-G2
mvn -o -q -DskipTests package
./scripts/run-uc37-tests.ps1 -Database
```

HTTP/browser được kiểm tra trên Tomcat copy riêng tại cổng 18087, đã bỏ scheduled listener trong bản copy để tránh gửi mail/chạy job nền. WAR phát hành không bỏ listener. Script HTTP dùng thư viện chuẩn Python; browser QA tùy chọn dùng playwright-core trong output và Chrome có sẵn.

## 6. Review và các điểm đã chỉnh

- Tách tính cảnh báo nhất quán thanh toán về Service; DAO chỉ truy vấn/map dữ liệu. Thông điệp validation/cảnh báo được tập trung tại Messages.
- Mọi action dùng một URL thuộc danh mục Permission, tránh bỏ sót quyền ở endpoint chi tiết hoặc CSV. Không có thao tác sửa Paid/hoàn tiền trong báo cáo.
- Tổng hợp Ticket/Concession ở cấp Booking trước khi kết hợp; không JOIN trực tiếp toàn bộ lần thử thanh toán vào tổng tiền.
- Tiền thu và tiền hoàn dùng cơ sở thời gian riêng; không trừ voucher như tiền mặt.
- Lưu mốc PaidAt/cancellation rõ ràng, ghi trong cùng transaction nghiệp vụ; timestamp mới được cắt phần nano để tránh SQL làm tròn qua ngày ở giây cuối.
- Truy vấn tổng/chi tiết trong một transaction SERIALIZABLE ngắn để có kết quả nhất quán; xuất file sau khi transaction đọc đã hoàn tất.
- Bảng mobile được giữ độ rộng cột và cuộn trong vùng riêng; các KPI có thứ tự cố định, timestamp không hiển thị chuỗi nano dài.
- Rà soát SQL tham số hóa, XSS qua output escaping, CSV formula injection và ID overflow. `git diff --check` đạt.

Không còn phát hiện lỗi chặn sử dụng trong phạm vi đã kiểm tra. Đây là self-review trên thay đổi hiện tại, không phải một lượt review độc lập bởi thành viên khác.

## 7. Phạm vi kiểm chứng và vận hành

- Đã kiểm tra ứng dụng với SQL Server/Tomcat local và dữ liệu giao dịch nội bộ; chưa thực hiện giao dịch VNPay thật hoặc đối soát sao kê ngân hàng. Phí cổng và tiền chuyển về ngân hàng không nằm trong chỉ số bản này.
- Báo cáo hiển thị cảnh báo cho dữ liệu lịch sử thiếu mốc; không thể khôi phục chính xác ngày xác nhận đơn 0đ cũ hoặc liên kết voucher cũ chỉ từ dữ liệu hiện còn.
- Các bản ghi đổi/hoàn cũ dùng GETDATE theo timezone của máy SQL lúc tạo. Quy ước mới ghi rõ giờ Việt Nam; cần chuyển dữ liệu có kiểm chứng nếu trước đây DB chạy ở timezone khác.
- Phải áp dụng migration trước khi chạy WAR mới vì các đường thanh toán/hủy vé đã bắt đầu ghi các trường/bảng mới. Migration đã được áp dụng cho DB local hiện tại.
- Sau khi chạy WAR mới trong môi trường làm việc, đăng nhập Manager → **Bán vé online**. Staff được cấp quyền qua UC-31 vào khu quản lý rồi chọn menu tương ứng.

## 8. Luồng tổng quát

```mermaid
sequenceDiagram
    actor Actor as Manager / Staff được cấp quyền
    participant Filter as AuthFilter
    participant Controller as OnlineSalesReportServlet
    participant Service as OnlineSalesReportService
    participant DAO as OnlineSalesReportDAO
    participant DB as SQL Server
    Actor->>Filter: GET /manager/online-reports
    Filter->>Filter: Kiểm tra phiên và VIEW_ONLINE_REPORTS
    Filter->>Controller: Request được phép
    Controller->>Service: Validate bộ lọc / yêu cầu chi tiết
    Service->>DAO: Đọc tổng, dòng, lịch sử trong transaction
    DAO->>DB: Truy vấn tham số hóa
    DB-->>DAO: Booking / payment / adjustment data
    DAO-->>Service: Dữ liệu đã tổng hợp đúng cấp
    Service->>Service: Tính thu trừ hoàn, cảnh báo, kiểm tra giới hạn CSV
    Service-->>Controller: Kết quả báo cáo
    Controller-->>Actor: JSP hoặc CSV
```
