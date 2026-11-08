<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<div class="or-table-wrap" tabindex="0" role="region" aria-label="Điều chỉnh đổi hoàn, cuộn ngang để xem đầy đủ">
<table class="or-table or-table-wide">
    <caption>Điều chỉnh của đơn online · Voucher hủy vé và voucher đền bù được phân biệt theo loại xử lý</caption>
    <thead><tr><th scope="col">Đơn / thời gian</th><th scope="col">Phim / xử lý</th><th scope="col">Tiền mặt hoàn</th><th scope="col">Voucher</th><th scope="col">Người xử lý / ca</th><th scope="col">Lý do</th></tr></thead>
    <tbody><c:forEach var="row" items="${reportRows}">
        <c:url var="orderUrl" value="/manager/online-reports"><c:param name="id" value="${row.bookingId}"/></c:url>
        <tr><td><a href="${orderUrl}">#${row.bookingId}</a><small><c:out value="${fn:replace(row.occurredAt, 'T', ' ')}"/></small><small>Ngày bán: <c:out value="${fn:replace(row.paidAt, 'T', ' ')}" default="Chưa rõ"/></small></td>
            <td><c:out value="${row.title}"/><small><c:out value="${adjustmentLabels[row.status]}" default="${row.status}"/></small></td>
            <td class="or-money"><fmt:formatNumber value="${row.cashRefund}" maxFractionDigits="0"/>đ</td>
            <td class="or-money"><fmt:formatNumber value="${row.voucherAmount}" maxFractionDigits="0"/>đ<small><c:out value="${row.reference}"/></small></td>
            <td><c:out value="${row.staffName}"/><small><c:if test="${row.shiftId != null}">Ca #${row.shiftId}</c:if><c:if test="${row.status == 'Cancellation'}">Khách tự hủy</c:if></small></td>
            <td class="or-break"><c:out value="${row.description}"/></td></tr>
    </c:forEach><c:if test="${empty reportRows}"><tr><td colspan="6">Không có điều chỉnh nào.</td></tr></c:if></tbody>
</table></div>
