<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<fmt:setLocale value="vi_VN"/>
<%-- Lịch sử bán tại quầy dùng chung cho Staff và Manager. Cần request attribute "result" (CounterHistoryService.Result),
     "baseUrl"; param showStaff=1 để hiện cột nhân viên / ca (Manager) --%>
<c:set var="paymentLabels" value="${{'Cash':'Tiền mặt','Transfer':'Chuyển khoản / QR','Card':'Thẻ (POS)'}}"/>
<c:set var="ticketLabels" value="${{'Watched':'Đã xem','Exchanged':'Đã đổi','Cancelled':'Đã hủy'}}"/>
<c:set var="showStaff" value="${param.showStaff == '1'}"/>

<div class="grid grid-cols-2 lg:grid-cols-4 gap-space-md mb-space-lg">
    <div class="bg-surface-container-low rounded-xl border border-white/10 p-space-md">
        <div class="text-body-sm text-on-surface-variant">Số đơn</div>
        <div class="text-headline-sm">${result.orderCount}</div>
    </div>
    <div class="bg-surface-container-low rounded-xl border border-white/10 p-space-md">
        <div class="text-body-sm text-on-surface-variant">Đã thu của khách</div>
        <div class="text-headline-sm"><fmt:formatNumber value="${result.collected}" maxFractionDigits="0"/>đ</div>
    </div>
    <div class="bg-surface-container-low rounded-xl border border-white/10 p-space-md">
        <div class="text-body-sm text-on-surface-variant">Đã hoàn trả khách</div>
        <div class="text-headline-sm text-amber-300"><fmt:formatNumber value="${result.refunded}" maxFractionDigits="0"/>đ</div>
    </div>
    <div class="bg-surface-container-low rounded-xl border border-white/10 p-space-md">
        <div class="text-body-sm text-on-surface-variant">Thực thu sau hoàn</div>
        <div class="text-headline-sm text-emerald-300"><fmt:formatNumber value="${result.net}" maxFractionDigits="0"/>đ</div>
    </div>
</div>

<c:choose>
    <c:when test="${empty result.page.items}">
        <div class="bg-surface-container-low rounded-xl border border-white/10 p-space-lg text-center text-on-surface-variant">
            Không có đơn bán tại quầy nào.
        </div>
    </c:when>
    <c:otherwise>
        <div class="bg-surface-container-low rounded-xl border border-white/10 overflow-x-auto">
            <table class="w-full text-left text-body-md align-top">
                <thead class="text-label-md text-on-surface-variant border-b border-white/10">
                <tr>
                    <th class="px-space-md py-2">Đơn</th>
                    <c:if test="${showStaff}"><th>Nhân viên</th></c:if>
                    <th>Khách hàng</th>
                    <th>Vé</th>
                    <th>Voucher / điểm</th>
                    <th>Thanh toán</th>
                    <th class="px-space-md text-right">Hoàn trả</th>
                </tr>
                </thead>
                <tbody class="divide-y divide-white/5">
                <c:forEach var="o" items="${result.page.items}">
                    <tr class="align-top">
                        <td class="px-space-md py-3">
                            <div class="font-label-lg">#${o.bookingId}</div>
                            <div class="text-body-sm text-on-surface-variant">${fn:substring(o.bookingTime, 11, 16)} ${fn:substring(o.bookingTime, 8, 10)}/${fn:substring(o.bookingTime, 5, 7)}</div>
                            <div class="text-body-sm mt-1"><c:out value="${o.movieTitle}"/></div>
                            <div class="text-body-sm text-on-surface-variant"><c:out value="${o.roomName}"/> · ${fn:substring(o.startTime, 11, 16)} ${fn:substring(o.startTime, 8, 10)}/${fn:substring(o.startTime, 5, 7)}</div>
                        </td>
                        <c:if test="${showStaff}">
                            <td class="py-3">
                                <div><c:out value="${o.staffName}"/></div>
                                <c:if test="${not empty o.shiftId}">
                                    <a class="text-body-sm text-primary hover:underline" href="${pageContext.request.contextPath}/manager/shifts?id=${o.shiftId}">Ca #${o.shiftId}</a>
                                    <span class="text-body-sm text-on-surface-variant">· Quầy ${fn:substring(o.counterNo, 1, 2)}</span>
                                </c:if>
                            </td>
                        </c:if>
                        <td class="py-3">
                            <c:choose>
                                <c:when test="${o.member}">
                                    <div><c:out value="${o.customerName}"/></div>
                                    <div class="text-body-sm text-on-surface-variant">Mã KH: #${o.customerId}</div>
                                    <div class="text-body-sm text-on-surface-variant"><c:out value="${not empty o.customerPhone ? o.customerPhone : o.customerEmail}"/></div>
                                </c:when>
                                <c:otherwise><span class="text-on-surface-variant">Khách vãng lai</span></c:otherwise>
                            </c:choose>
                        </td>
                        <td class="py-3">
                            <c:forEach var="t" items="${o.tickets}">
                                <div class="whitespace-nowrap"><b>${t.seat}</b>
                                    <span class="text-body-sm text-on-surface-variant"><c:out value="${t.ticketCode}"/></span>
                                    <c:if test="${not empty ticketLabels[t.status]}">
                                        <span class="cl-pill ${t.status == 'Watched' ? 'cl-pill--muted' : 'cl-pill--warning'}">${ticketLabels[t.status]}</span>
                                    </c:if>
                                </div>
                            </c:forEach>
                        </td>
                        <td class="py-3 text-body-sm">
                            <c:if test="${not empty o.voucherCode}">
                                <div>Voucher <b><c:out value="${o.voucherCode}"/></b></div>
                            </c:if>
                            <c:if test="${o.pointsRedeemed > 0}"><div>Đổi <b>${o.pointsRedeemed}</b> điểm</div></c:if>
                            <c:if test="${o.discountAmount > 0}">
                                <div class="text-emerald-300">Giảm <fmt:formatNumber value="${o.discountAmount}" maxFractionDigits="0"/>đ</div>
                            </c:if>
                            <c:if test="${o.pointsEarned > 0}"><div class="text-on-surface-variant">Cộng ${o.pointsEarned} điểm</div></c:if>
                            <c:if test="${empty o.voucherCode and o.pointsRedeemed == 0 and o.pointsEarned == 0}">
                                <span class="text-on-surface-variant">-</span>
                            </c:if>
                        </td>
                        <td class="py-3">
                            <div class="font-label-lg"><fmt:formatNumber value="${o.finalAmount}" maxFractionDigits="0"/>đ</div>
                            <c:if test="${o.discountAmount > 0}">
                                <div class="text-body-sm text-on-surface-variant line-through"><fmt:formatNumber value="${o.totalPrice}" maxFractionDigits="0"/>đ</div>
                            </c:if>
                            <div class="text-body-sm text-on-surface-variant">${paymentLabels[o.paymentMethod]}</div>
                            <c:if test="${not empty o.paymentReference}">
                                <div class="text-body-sm text-on-surface-variant">Mã GD: <c:out value="${o.paymentReference}"/></div>
                            </c:if>
                            <c:if test="${not empty o.cashReceived}">
                                <div class="text-body-sm text-on-surface-variant">Khách đưa <fmt:formatNumber value="${o.cashReceived}" maxFractionDigits="0"/>đ
                                    · thối <fmt:formatNumber value="${o.changeDue}" maxFractionDigits="0"/>đ</div>
                            </c:if>
                        </td>
                        <td class="px-space-md py-3 text-right">
                            <c:choose>
                                <c:when test="${o.refundTotal > 0}">
                                    <span class="text-amber-300 font-label-lg"><fmt:formatNumber value="${o.refundTotal}" maxFractionDigits="0"/>đ</span>
                                </c:when>
                                <c:otherwise><span class="text-on-surface-variant">-</span></c:otherwise>
                            </c:choose>
                        </td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
            <ui:pagination pageData="${result.page}" baseUrl="${baseUrl}" label="đơn"/>
        </div>
    </c:otherwise>
</c:choose>
