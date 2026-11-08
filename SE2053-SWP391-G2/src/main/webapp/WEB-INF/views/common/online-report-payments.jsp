<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<div class="or-table-wrap" tabindex="0" role="region" aria-label="Các lần thanh toán, cuộn ngang để xem đầy đủ">
<table class="or-table or-table-wide">
    <caption>Các lần thử thanh toán · Thời điểm tạo đã đổi sang giờ Việt Nam</caption>
    <thead><tr><th scope="col">Đơn / tham chiếu</th><th scope="col">Tạo / thanh toán</th><th scope="col">Trạng thái</th><th scope="col">Yêu cầu</th><th scope="col">Nhận</th><th scope="col">Kiểm tra</th></tr></thead>
    <tbody><c:forEach var="row" items="${reportRows}">
        <c:url var="orderUrl" value="/manager/online-reports"><c:param name="id" value="${row.bookingId}"/></c:url>
        <tr><td class="or-break"><a href="${orderUrl}">#${row.bookingId}</a><small><c:out value="${row.reference}"/></small><small>GD cổng: <c:out value="${row.providerTransactionId}" default="Chưa có"/></small></td>
            <td><c:out value="${fn:replace(row.createdAt, 'T', ' ')}"/><small>Paid: <c:out value="${fn:replace(row.paidAt, 'T', ' ')}" default="Chưa có"/></small></td>
            <td><c:out value="${paymentLabels[row.status]}" default="${row.status}"/></td>
            <td class="or-money"><fmt:formatNumber value="${row.amount}" maxFractionDigits="0"/>đ</td>
            <td class="or-money"><c:choose><c:when test="${row.receivedAmount != null}"><fmt:formatNumber value="${row.receivedAmount}" maxFractionDigits="0"/>đ</c:when><c:otherwise>—</c:otherwise></c:choose></td>
            <td class="or-break"><c:out value="${row.warning}" default=""/></td></tr>
    </c:forEach><c:if test="${empty reportRows}"><tr><td colspan="6">Không có lần thanh toán nào.</td></tr></c:if></tbody>
</table></div>
