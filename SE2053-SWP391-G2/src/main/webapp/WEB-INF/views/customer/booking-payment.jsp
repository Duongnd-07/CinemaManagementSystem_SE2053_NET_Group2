<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" import="java.time.format.DateTimeFormatter" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="dateFormat" value="${DateTimeFormatter.ofPattern('dd/MM/yyyy')}"/>
<c:set var="timeFormat" value="${DateTimeFormatter.ofPattern('HH:mm')}"/>
<c:set var="booking" value="${summary.booking}"/>
<c:set var="movie" value="${summary.movie}"/>
<c:set var="showtime" value="${summary.showtime}"/>
<c:set var="room" value="${summary.room}"/>
<c:set var="pending" value="${booking.status == 'Pending'}"/>
<c:set var="paid" value="${booking.status == 'Paid'}"/>
<!DOCTYPE html>
<html lang="vi" class="dark">
<head>
    <title>${paid ? 'Đặt vé thành công' : 'Thanh toán'} - Đơn #${booking.bookingId} | CineLuxe</title>
    <jsp:include page="/WEB-INF/views/common/public-head.jsp"/>
    <link href="${ctx}/assets/css/seat-map.css" rel="stylesheet"/>
    <link href="${ctx}/assets/css/booking.css" rel="stylesheet"/>
</head>
<body class="booking-page min-h-screen flex flex-col">
    <jsp:include page="/WEB-INF/views/common/public-header.jsp"/>

    <c:if test="${not empty flashSuccess}">
        <div id="toast" class="booking-toast booking-toast--success" role="status">
            <span class="material-symbols-outlined">check_circle</span><c:out value="${flashSuccess}"/>
        </div>
    </c:if>

    <main class="flex-1 max-w-4xl w-full mx-auto px-4 sm:px-10 py-6 sm:py-8 space-y-5">
        <div class="flex flex-wrap items-center justify-between gap-3">
            <jsp:include page="booking-steps.jsp"><jsp:param name="step" value="3"/></jsp:include>
            <c:if test="${pending}">
                <div id="holdTimer" class="booking-timer" data-remaining="${summary.remainingSeconds}"
                     data-expired-url="${ctx}/customer/booking/payment?id=${booking.bookingId}">
                    <span class="material-symbols-outlined">timer</span>Giữ ghế còn <strong data-clock>--:--</strong>
                </div>
            </c:if>
        </div>

        <c:if test="${not empty flashError}">
            <div class="seat-map-alert" role="alert">
                <span class="material-symbols-outlined">error</span><span><c:out value="${flashError}"/></span>
            </div>
        </c:if>

        <c:choose>
            <c:when test="${paid}">
                <section class="cl-card p-6 text-center space-y-2">
                    <span class="material-symbols-outlined text-6xl text-[#34d399]">task_alt</span>
                    <h1 class="text-2xl font-extrabold text-white">Đặt vé thành công!</h1>
                    <p class="booking-hint">Mã đơn #${booking.bookingId}. Vui lòng đưa mã vé hoặc mã QR cho nhân viên soát vé khi vào phòng chiếu.</p>
                </section>
            </c:when>
            <c:when test="${pending}">
                <section class="cl-card p-5 flex items-start gap-3 border-[#3b82f6]/40">
                    <span class="material-symbols-outlined text-[#60a5fa]">info</span>
                    <p class="text-sm text-[#c7cad6]">
                        Đơn <strong class="text-white">#${booking.bookingId}</strong> đang chờ thanh toán. Cổng thanh toán online
                        (VNPay/SePay - UC-14) đang được phát triển; nút bên dưới chỉ <strong class="text-white">mô phỏng</strong>
                        giao dịch thành công để hoàn tất luồng đặt vé.
                    </p>
                </section>
            </c:when>
            <c:otherwise>
                <section class="cl-card p-6 text-center space-y-3">
                    <span class="material-symbols-outlined text-6xl text-[#e50914]">cancel</span>
                    <h1 class="text-2xl font-extrabold text-white">Đơn #${booking.bookingId} đã bị hủy</h1>
                    <p class="booking-hint">Đơn đã hết thời gian giữ ghế hoặc đã được hủy. Các ghế đã được trả lại.</p>
                    <a href="${ctx}/showtimes?movieId=${movie.movieId}" class="booking-btn booking-btn--primary">Đặt lại vé</a>
                </section>
            </c:otherwise>
        </c:choose>

        <section class="cl-card p-5 sm:p-6 space-y-4">
            <div class="flex flex-wrap items-center gap-2">
                <h2 class="text-xl font-extrabold text-white"><c:out value="${movie.title}"/></h2>
                <span class="booking-age ${movie.ageRating == 'P' ? 'booking-age--p' : ''}"><c:out value="${movie.ageRating}"/></span>
            </div>
            <p class="text-sm text-[#c7cad6]">
                ${showtime.startTime.format(timeFormat)} · ${showtime.startTime.format(dateFormat)} ·
                <c:out value="${room.roomName}"/> (<c:out value="${room.format}"/>)
            </p>

            <c:choose>
                <c:when test="${paid}">
                    <div class="space-y-3">
                        <c:forEach var="seat" items="${summary.seats}">
                            <div class="ticket-card">
                                <div>
                                    <div class="seat-map-meta-label">Ghế <c:out value="${seat.label}"/> ·
                                        ${seat.seatType == 'VIP' ? 'VIP' : seat.seatType == 'Sweetbox' ? 'Ghế đôi (2 người)' : 'Thường'}</div>
                                    <div class="ticket-code"><c:out value="${seat.ticketCode}"/></div>
                                </div>
                                <div class="text-right">
                                    <div class="seat-map-meta-label">Mã QR</div>
                                    <div class="text-xs text-[#8e919f] break-all"><c:out value="${seat.qrCode}"/></div>
                                </div>
                            </div>
                        </c:forEach>
                    </div>
                </c:when>
                <c:otherwise>
                    <div>
                        <c:forEach var="seat" items="${summary.seats}">
                            <div class="booking-line">
                                <span>Ghế <strong class="text-white"><c:out value="${seat.label}"/></strong> ·
                                    ${seat.seatType == 'VIP' ? 'VIP' : seat.seatType == 'Sweetbox' ? 'Ghế đôi (2 người)' : 'Thường'}</span>
                                <span><fmt:formatNumber value="${seat.price}" maxFractionDigits="0"/>đ</span>
                            </div>
                        </c:forEach>
                    </div>
                </c:otherwise>
            </c:choose>

            <div>
                <c:forEach var="item" items="${summary.concessions}">
                    <div class="booking-line">
                        <span><c:out value="${item.name}"/> × ${item.quantity}</span>
                        <span><fmt:formatNumber value="${item.subtotal}" maxFractionDigits="0"/>đ</span>
                    </div>
                </c:forEach>
                <div class="booking-line booking-line--total"><span>Tạm tính</span><span><fmt:formatNumber value="${booking.totalPrice}" maxFractionDigits="0"/>đ</span></div>
                <c:if test="${not empty summary.voucherCode}">
                    <div class="booking-line booking-line--discount"><span>Mã giảm giá <c:out value="${summary.voucherCode}"/></span><span>đã áp dụng</span></div>
                </c:if>
                <c:if test="${booking.pointsRedeemed > 0}">
                    <div class="booking-line booking-line--discount"><span>Đổi điểm tích lũy</span><span><fmt:formatNumber value="${booking.pointsRedeemed}"/> điểm</span></div>
                </c:if>
                <c:if test="${booking.discountAmount > 0}">
                    <div class="booking-line booking-line--discount"><span>Tổng giảm</span><span>−<fmt:formatNumber value="${booking.discountAmount}" maxFractionDigits="0"/>đ</span></div>
                </c:if>
                <div class="booking-line booking-line--total"><span>${paid ? 'Đã thanh toán' : 'Tổng thanh toán'}</span><span><fmt:formatNumber value="${booking.finalAmount}" maxFractionDigits="0"/>đ</span></div>
            </div>
        </section>

        <c:choose>
            <c:when test="${pending}">
                <div class="flex flex-wrap items-center justify-between gap-3">
                    <form method="post" action="${ctx}/customer/booking/payment" data-confirm="Hủy đơn và trả lại các ghế đang giữ?">
                        <input type="hidden" name="csrfToken" value="${csrfToken}"/>
                        <input type="hidden" name="id" value="${booking.bookingId}"/>
                        <input type="hidden" name="action" value="cancel"/>
                        <button type="submit" class="booking-btn booking-btn--danger">
                            <span class="material-symbols-outlined text-base">close</span>Hủy đơn
                        </button>
                    </form>
                    <form method="post" action="${ctx}/customer/booking/payment">
                        <input type="hidden" name="csrfToken" value="${csrfToken}"/>
                        <input type="hidden" name="id" value="${booking.bookingId}"/>
                        <button type="submit" name="action" value="pay" class="booking-btn booking-btn--primary" data-submit-once>
                            <span class="material-symbols-outlined text-base">payments</span>Thanh toán (demo)
                        </button>
                    </form>
                </div>
            </c:when>
            <c:when test="${paid}">
                <div class="text-center">
                    <a href="${ctx}/movies" class="booking-btn booking-btn--ghost">Tiếp tục xem phim khác</a>
                </div>
            </c:when>
        </c:choose>
    </main>

    <jsp:include page="/WEB-INF/views/common/public-footer.jsp"/>
    <script src="${ctx}/assets/js/booking-checkout.js"></script>
</body>
</html>
