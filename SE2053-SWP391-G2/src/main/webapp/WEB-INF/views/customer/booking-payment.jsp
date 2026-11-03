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
            <c:when test="${pending and summary.free}">
                <section class="cl-card p-5 flex items-start gap-3 border-[#34d399]/40">
                    <span class="material-symbols-outlined text-[#34d399]">redeem</span>
                    <p class="text-sm text-[#c7cad6]">
                        Đơn <strong class="text-white">#${booking.bookingId}</strong> đã được trừ hết bằng voucher/điểm tích lũy,
                        không cần thanh toán thêm. Bấm <strong class="text-white">Xác nhận đặt vé</strong> để nhận vé điện tử.
                    </p>
                </section>
            </c:when>
            <c:when test="${pending and summary.awaitingGateway}">
                <%-- Đã chuyển sang VNPay, chờ IPN: trang tự tải lại để hiện kết quả, không mời thanh toán thêm lần nữa --%>
                <section id="awaitingPayment" class="cl-card p-5 flex items-start gap-3 border-[#fbbf24]/40"
                         data-auto-refresh="5">
                    <span class="material-symbols-outlined text-[#fbbf24]">hourglass_top</span>
                    <p class="text-sm text-[#c7cad6]">
                        Đang chờ VNPay xác nhận thanh toán cho đơn <strong class="text-white">#${booking.bookingId}</strong>.
                        Trang sẽ tự cập nhật khi có kết quả, vui lòng không thanh toán lại nếu bạn đã trả tiền.
                    </p>
                </section>
            </c:when>
            <c:when test="${pending}">
                <section class="cl-card p-5 flex items-start gap-3 border-[#3b82f6]/40">
                    <span class="material-symbols-outlined text-[#60a5fa]">${summary.lastPaymentFailed ? 'error' : 'info'}</span>
                    <p class="text-sm text-[#c7cad6]">
                        <c:choose>
                            <c:when test="${summary.lastPaymentFailed}">
                                Lần thanh toán trước của đơn <strong class="text-white">#${booking.bookingId}</strong> chưa thành công
                                (bị hủy hoặc bị từ chối). Bạn có thể thanh toán lại trong thời gian giữ ghế.
                            </c:when>
                            <c:otherwise>
                                Đơn <strong class="text-white">#${booking.bookingId}</strong> đang chờ thanh toán qua VNPay.
                                Đơn chỉ được xác nhận sau khi hệ thống nhận và xác minh thông báo thanh toán từ VNPay.
                            </c:otherwise>
                        </c:choose>
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
            <p class="text-sm text-[#c7cad6] flex flex-wrap items-center gap-1.5">
                <span>${showtime.startTime.format(timeFormat)} · ${showtime.startTime.format(dateFormat)} ·</span>
                <span class="inline-block text-[11px] font-bold px-1.5 py-0.5 rounded leading-none ${showtime.translationType == 'Lồng tiếng' ? 'bg-amber-500/20 text-amber-300 border border-amber-500/40' : 'bg-sky-500/20 text-sky-300 border border-sky-500/40'}">
                    <c:out value="${empty showtime.translationType ? 'Phụ đề' : showtime.translationType}"/>
                </span>
                <span>· <c:out value="${room.roomName}"/> (<c:out value="${room.format}"/>)</span>
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
                    <form method="post" action="${ctx}/customer/booking/payment"
                          data-confirm="${summary.awaitingGateway
                                ? 'Nếu bạn đã trả tiền trên VNPay, đừng hủy đơn - hãy chờ hệ thống xác nhận. Vẫn hủy đơn và trả lại ghế?'
                                : 'Hủy đơn và trả lại các ghế đang giữ?'}">
                        <input type="hidden" name="csrfToken" value="${csrfToken}"/>
                        <input type="hidden" name="id" value="${booking.bookingId}"/>
                        <input type="hidden" name="action" value="cancel"/>
                        <button type="submit" class="booking-btn booking-btn--danger">
                            <span class="material-symbols-outlined text-base">close</span>Hủy đơn
                        </button>
                    </form>
                    <c:choose>
                        <c:when test="${summary.free}">
                            <form method="post" action="${ctx}/customer/booking/payment">
                                <input type="hidden" name="csrfToken" value="${csrfToken}"/>
                                <input type="hidden" name="id" value="${booking.bookingId}"/>
                                <button type="submit" name="action" value="confirmFree" class="booking-btn booking-btn--primary" data-submit-once>
                                    <span class="material-symbols-outlined text-base">check_circle</span>Xác nhận đặt vé (0đ)
                                </button>
                            </form>
                        </c:when>
                        <c:when test="${summary.awaitingGateway}">
                            <%-- Khách đóng trang VNPay mà chưa trả: vẫn cho thanh toán lại nhưng phải xác nhận rõ --%>
                            <form method="post" action="${ctx}/customer/booking/payment"
                                  data-confirm="Chỉ thanh toán lại nếu bạn CHƯA trả tiền ở lần trước (ví dụ đã đóng trang VNPay). Nếu đã trả, hãy chờ để tránh bị trừ tiền hai lần. Tiếp tục?">
                                <input type="hidden" name="csrfToken" value="${csrfToken}"/>
                                <input type="hidden" name="id" value="${booking.bookingId}"/>
                                <button type="submit" name="action" value="pay" class="booking-btn booking-btn--ghost" data-submit-once>
                                    <span class="material-symbols-outlined text-base">refresh</span>Tôi chưa trả tiền, thanh toán lại
                                </button>
                            </form>
                        </c:when>
                        <c:otherwise>
                            <form method="post" action="${ctx}/customer/booking/payment">
                                <input type="hidden" name="csrfToken" value="${csrfToken}"/>
                                <input type="hidden" name="id" value="${booking.bookingId}"/>
                                <button type="submit" name="action" value="pay" class="booking-btn booking-btn--primary" data-submit-once>
                                    <span class="material-symbols-outlined text-base">payments</span>${summary.lastPaymentFailed ? 'Thanh toán lại qua VNPay' : 'Thanh toán qua VNPay'}
                                </button>
                            </form>
                        </c:otherwise>
                    </c:choose>
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
