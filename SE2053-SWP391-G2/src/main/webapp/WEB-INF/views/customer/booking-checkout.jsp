<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" import="java.time.format.DateTimeFormatter" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="dateFormat" value="${DateTimeFormatter.ofPattern('dd/MM/yyyy')}"/>
<c:set var="timeFormat" value="${DateTimeFormatter.ofPattern('HH:mm')}"/>
<c:set var="movie" value="${checkout.movie}"/>
<c:set var="showtime" value="${checkout.showtime}"/>
<c:set var="room" value="${checkout.room}"/>
<c:set var="quote" value="${checkout.quote}"/>
<!DOCTYPE html>
<html lang="vi" class="dark">
<head>
    <title>Xác nhận đặt vé - <c:out value="${movie.title}"/> | CineLuxe</title>
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

    <main class="flex-1 max-w-6xl w-full mx-auto px-4 sm:px-10 py-6 sm:py-8 space-y-5">
        <div class="flex flex-wrap items-center justify-between gap-3">
            <jsp:include page="booking-steps.jsp"><jsp:param name="step" value="2"/></jsp:include>
            <%-- BR-HOLD-01: đồng hồ đếm ngược thời gian giữ ghế; về 0 thì tải lại để server xử lý E1 --%>
            <div id="holdTimer" class="booking-timer" data-remaining="${checkout.remainingSeconds}"
                 data-expired-url="${ctx}/customer/booking/checkout">
                <span class="material-symbols-outlined">timer</span>Giữ ghế còn <strong data-clock>--:--</strong>
            </div>
        </div>

        <%-- Các vùng có id bên dưới được booking-checkout.js thay bằng bản mới mỗi lần tự tính lại tiền --%>
        <div id="generalAlert">
            <c:if test="${not empty flashError or not empty errors['general']}">
                <div class="seat-map-alert" role="alert">
                    <span class="material-symbols-outlined">error</span>
                    <span><c:out value="${not empty errors['general'] ? errors['general'] : flashError}"/></span>
                </div>
            </c:if>
        </div>

        <form id="checkoutForm" method="post" action="${ctx}/customer/booking/checkout"
              class="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
            <input type="hidden" name="csrfToken" value="${csrfToken}"/>

            <div class="lg:col-span-8 space-y-5">
                <section class="cl-card p-5 sm:p-6">
                    <div class="flex flex-col sm:flex-row gap-5">
                        <div class="seat-map-poster">
                            <span class="material-symbols-outlined text-4xl">movie</span>
                            <c:if test="${not empty movie.posterUrl}">
                                <img alt="" onerror="this.remove()"
                                     src="${fn:startsWith(movie.posterUrl, 'http') ? '' : ctx.concat('/')}<c:out value='${movie.posterUrl}'/>"/>
                            </c:if>
                        </div>
                        <div class="flex-1 space-y-3">
                            <div class="flex flex-wrap items-center gap-2">
                                <h1 class="text-2xl font-extrabold text-white"><c:out value="${movie.title}"/></h1>
                                <span class="booking-age ${movie.ageRating == 'P' ? 'booking-age--p' : ''}"><c:out value="${movie.ageRating}"/></span>
                            </div>
                            <p class="text-sm text-[#c7cad6] flex flex-wrap items-center gap-1.5">
                                <span>${showtime.startTime.format(timeFormat)} · ${showtime.startTime.format(dateFormat)} ·</span>
                                <span class="inline-block text-[11px] font-bold px-1.5 py-0.5 rounded leading-none ${showtime.translationType == 'Lồng tiếng' ? 'bg-amber-500/20 text-amber-300 border border-amber-500/40' : 'bg-sky-500/20 text-sky-300 border border-sky-500/40'}">
                                    <c:out value="${empty showtime.translationType ? 'Phụ đề' : showtime.translationType}"/>
                                </span>
                                <span>· <c:out value="${room.roomName}"/> (<c:out value="${room.format}"/>)</span>
                            </p>
                            <div>
                                <c:forEach var="seat" items="${checkout.seats}">
                                    <div class="booking-line">
                                        <span>Ghế <strong class="text-white"><c:out value="${seat.label}"/></strong> ·
                                            ${seat.seatType == 'VIP' ? 'VIP' : seat.seatType == 'Sweetbox' ? 'Ghế đôi (2 người)' : 'Thường'}</span>
                                        <span><fmt:formatNumber value="${seat.price}" maxFractionDigits="0"/>đ</span>
                                    </div>
                                </c:forEach>
                            </div>
                            <%-- Ghế đang giữ vẫn được chọn sẵn ở sơ đồ; giữ ghế mới thì lượt giữ cũ tự được nhả --%>
                            <a href="${seatPageUrl}" class="booking-hint underline hover:text-white">Đổi ghế</a>
                        </div>
                    </div>
                </section>

                <%-- Bước 6: bắp nước (tùy chọn) --%>
                <section class="cl-card p-5 sm:p-6">
                    <h2 class="booking-section-title"><span class="material-symbols-outlined">fastfood</span>Bắp nước <span class="booking-hint font-semibold">(không bắt buộc)</span></h2>
                    <c:choose>
                        <c:when test="${empty checkout.concessions}">
                            <p class="booking-hint">Hiện chưa có món bắp nước nào để đặt trước.</p>
                        </c:when>
                        <c:otherwise>
                            <div class="concession-grid">
                                <c:forEach var="item" items="${checkout.concessions}">
                                    <div class="concession-card ${item.quantity > 0 ? 'is-picked' : ''}" id="concession-${item.concessionId}">
                                        <div class="concession-thumb">
                                            <span class="material-symbols-outlined">local_cafe</span>
                                            <c:if test="${not empty item.imageUrl}">
                                                <img alt="" onerror="this.remove()" src="<c:out value='${item.imageUrl}'/>"/>
                                            </c:if>
                                        </div>
                                        <div class="flex-1 min-w-0 space-y-2">
                                            <div class="text-sm font-bold text-white"><c:out value="${item.name}"/></div>
                                            <div class="text-sm font-bold text-[#ffb95f]"><fmt:formatNumber value="${item.unitPrice}" maxFractionDigits="0"/>đ</div>
                                            <div class="qty-stepper">
                                                <button type="button" data-step="-1" aria-label="Giảm số lượng ${fn:escapeXml(item.name)}">−</button>
                                                <input type="number" name="qty_${item.concessionId}" value="${item.quantity}"
                                                       min="0" max="${item.maxQuantity}" inputmode="numeric"
                                                       aria-label="Số lượng ${fn:escapeXml(item.name)}"/>
                                                <button type="button" data-step="1" aria-label="Tăng số lượng ${fn:escapeXml(item.name)}">+</button>
                                            </div>
                                        </div>
                                    </div>
                                </c:forEach>
                            </div>
                        </c:otherwise>
                    </c:choose>
                    <div id="concessionFeedback">
                        <c:if test="${not empty errors['concession']}">
                            <p class="booking-field-error"><c:out value="${errors['concession']}"/></p>
                        </c:if>
                    </div>
                </section>

                <%-- Bước 7: voucher (tối đa 1/đơn - GB-05) hoặc quy đổi điểm --%>
                <section class="cl-card p-5 sm:p-6 grid grid-cols-1 md:grid-cols-2 gap-6">
                    <div>
                        <h2 class="booking-section-title"><span class="material-symbols-outlined">confirmation_number</span>Mã giảm giá</h2>
                        <label for="voucherCode" class="booking-hint">Mỗi đơn áp dụng tối đa 1 mã · nhập tay hoặc chọn từ danh sách</label>
                        <%-- Ô nhập kiêm danh sách thả xuống (combobox): gõ tay mã bất kỳ, hoặc mở danh sách voucher trong ví
                             để chọn. booking-checkout.js mở/đóng, lọc theo chữ đã gõ và tự tính lại tiền khi chọn. --%>
                        <div id="voucherCombo" class="voucher-combo mt-2">
                            <input id="voucherCode" name="voucherCode" maxlength="50" autocomplete="off"
                                   role="combobox" aria-autocomplete="list" aria-expanded="false" aria-controls="voucherListbox"
                                   class="booking-input voucher-combo-input uppercase ${not empty errors['voucher'] ? 'is-invalid' : ''}"
                                   value="<c:out value='${voucherInput}'/>" placeholder="Nhập hoặc chọn mã voucher"/>
                            <button type="button" class="voucher-combo-icon voucher-combo-clear" data-voucher-clear
                                    aria-label="Xóa mã voucher" tabindex="-1" ${empty voucherInput ? 'hidden' : ''}>
                                <span class="material-symbols-outlined">close</span>
                            </button>
                            <button type="button" class="voucher-combo-icon" data-voucher-toggle
                                    aria-label="Mở danh sách voucher của bạn" tabindex="-1">
                                <span class="material-symbols-outlined">expand_more</span>
                            </button>
                            <div class="voucher-dropdown" data-voucher-dropdown hidden>
                                <%-- Vùng này được thay mỗi lần tự tính lại tiền (thêm bắp nước có thể làm mã đủ đơn tối thiểu) --%>
                                <div id="voucherOptions">
                                    <c:choose>
                                        <c:when test="${empty checkout.vouchers}">
                                            <p class="voucher-empty">Bạn chưa có voucher nào còn hạn.</p>
                                        </c:when>
                                        <c:otherwise>
                                            <ul id="voucherListbox" class="voucher-list" role="listbox" aria-label="Voucher của bạn">
                                                <c:forEach var="v" items="${checkout.vouchers}">
                                                    <fmt:formatNumber var="minOrderText" value="${v.minOrderAmount}" maxFractionDigits="0"/>
                                                    <c:set var="reason" value="${v.inOtherOrder ? 'Đang dùng cho một đơn khác chưa thanh toán'
                                                            : (v.belowMinOrder and not v.applied
                                                                ? 'Đơn chưa đạt giá trị tối thiểu '.concat(minOrderText).concat('đ') : '')}"/>
                                                    <li role="option" class="voucher-option ${v.applied ? 'is-applied' : ''}"
                                                        data-voucher-apply="<c:out value='${v.voucherCode}'/>"
                                                        aria-selected="${v.applied}" aria-disabled="${not (v.usable or v.applied)}"
                                                        <c:if test="${not empty reason}">title="<c:out value='${reason}'/>"</c:if>>
                                                        <span class="voucher-code"><c:out value="${v.voucherCode}"/></span>
                                                        <span class="voucher-amount">
                                                            <c:choose>
                                                                <c:when test="${v.percent}">Giảm <fmt:formatNumber value="${v.discountValue}" maxFractionDigits="2"/>%</c:when>
                                                                <c:otherwise>Giảm <fmt:formatNumber value="${v.discountValue}" maxFractionDigits="0"/>đ</c:otherwise>
                                                            </c:choose>
                                                        </span>
                                                        <span class="voucher-expiry">HSD ${v.expiryDate.format(dateFormat)}</span>
                                                    </li>
                                                </c:forEach>
                                            </ul>
                                            <p class="voucher-empty" data-voucher-nomatch hidden>Không có voucher trong ví khớp với mã đã nhập.</p>
                                        </c:otherwise>
                                    </c:choose>
                                </div>
                            </div>
                        </div>
                        <div id="voucherFeedback">
                            <c:if test="${not empty errors['voucher']}">
                                <p class="booking-field-error"><c:out value="${errors['voucher']}"/></p>
                            </c:if>
                            <c:if test="${not empty quote.voucherCode and empty errors['voucher']}">
                                <p class="mt-2 text-sm text-[#34d399]">Đã áp dụng mã <strong><c:out value="${quote.voucherCode}"/></strong></p>
                            </c:if>
                        </div>
                    </div>
                    <div>
                        <h2 class="booking-section-title"><span class="material-symbols-outlined">stars</span>Điểm tích lũy</h2>
                        <label for="points" class="booking-hint" id="pointsInfo">
                            Bạn có <strong class="text-white"><fmt:formatNumber value="${checkout.availablePoints}"/></strong> điểm ·
                            1 điểm = <fmt:formatNumber value="${checkout.pointRate}"/>đ · đơn này dùng tối đa
                            <strong class="text-white"><fmt:formatNumber value="${quote.maxRedeemablePoints}"/></strong> điểm
                        </label>
                        <input id="points" name="points" type="number" min="0" inputmode="numeric"
                               class="booking-input mt-2 ${not empty errors['points'] ? 'is-invalid' : ''}"
                               value="${quote.pointsRedeemed > 0 ? quote.pointsRedeemed : ''}" placeholder="0"
                               ${checkout.availablePoints == 0 ? 'disabled' : ''}/>
                        <div id="pointsFeedback">
                            <c:if test="${not empty errors['points']}">
                                <p class="booking-field-error"><c:out value="${errors['points']}"/></p>
                            </c:if>
                        </div>
                        <p class="booking-hint mt-2">Điểm chỉ trừ vào tiền vé, không áp dụng cho bắp nước.</p>
                    </div>
                </section>
            </div>

            <%-- Bước 8: tổng tiền --%>
            <aside class="lg:col-span-4 cl-card p-5 sm:p-6 lg:sticky lg:top-28 space-y-4">
                <h2 class="booking-section-title"><span class="material-symbols-outlined">receipt_long</span>Tóm tắt đơn hàng</h2>
                <div id="orderTotals">
                    <div class="booking-line"><span>Tiền vé (${fn:length(checkout.seats)} ghế)</span><span><fmt:formatNumber value="${quote.ticketTotal}" maxFractionDigits="0"/>đ</span></div>
                    <div class="booking-line"><span>Bắp nước</span><span><fmt:formatNumber value="${quote.concessionTotal}" maxFractionDigits="0"/>đ</span></div>
                    <c:if test="${quote.voucherDiscount > 0}">
                        <div class="booking-line booking-line--discount"><span>Voucher <c:out value="${quote.voucherCode}"/></span><span>−<fmt:formatNumber value="${quote.voucherDiscount}" maxFractionDigits="0"/>đ</span></div>
                    </c:if>
                    <c:if test="${quote.pointsRedeemed > 0}">
                        <div class="booking-line booking-line--discount"><span>Đổi <fmt:formatNumber value="${quote.pointsRedeemed}"/> điểm</span><span>−<fmt:formatNumber value="${quote.pointsDiscount}" maxFractionDigits="0"/>đ</span></div>
                    </c:if>
                    <div class="booking-line booking-line--total"><span>Tổng thanh toán</span><span><fmt:formatNumber value="${quote.finalAmount}" maxFractionDigits="0"/>đ</span></div>
                </div>
                <p id="quoteStatus" class="booking-hint" aria-live="polite" hidden></p>
                <div class="flex flex-col gap-2">
                    <%-- Tổng tiền tự cập nhật bằng JS; nút này chỉ cho trình duyệt tắt JavaScript --%>
                    <noscript>
                        <button type="submit" name="action" value="update" class="booking-btn booking-btn--ghost w-full">
                            <span class="material-symbols-outlined text-base">refresh</span>Cập nhật
                        </button>
                    </noscript>
                    <button type="submit" name="action" value="confirm" class="booking-btn booking-btn--primary" data-submit-once>
                        Xác nhận &amp; thanh toán<span class="material-symbols-outlined text-base">arrow_forward</span>
                    </button>
                </div>
            </aside>
        </form>

        <%-- AF2: hủy đơn trong thời gian giữ ghế --%>
        <form id="cancelForm" method="post" action="${ctx}/customer/booking/checkout"
              data-confirm="Hủy đơn và trả lại các ghế đang giữ?">
            <input type="hidden" name="csrfToken" value="${csrfToken}"/>
            <input type="hidden" name="action" value="cancel"/>
            <button type="submit" class="booking-btn booking-btn--danger">
                <span class="material-symbols-outlined text-base">close</span>Hủy đơn
            </button>
        </form>
    </main>

    <jsp:include page="/WEB-INF/views/common/public-footer.jsp"/>
    <script src="${ctx}/assets/js/booking-checkout.js"></script>
</body>
</html>
