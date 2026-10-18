<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" import="java.time.format.DateTimeFormatter" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="dateFormat" value="${DateTimeFormatter.ofPattern('dd/MM/yyyy')}"/>
<c:set var="timeFormat" value="${DateTimeFormatter.ofPattern('HH:mm')}"/>
<c:set var="ageYears" value="${{'C13':'13','C16':'16','C18':'18'}}"/>
<!DOCTYPE html>
<html lang="vi" class="dark">
<head>
    <title>Chọn ghế<c:if test="${not empty seatMap}"> - <c:out value="${seatMap.movie.title}"/></c:if> | CineLuxe</title>
    <jsp:include page="/WEB-INF/views/common/public-head.jsp"/>
    <link href="${ctx}/assets/css/seat-map.css" rel="stylesheet"/>
    <link href="${ctx}/assets/css/booking.css" rel="stylesheet"/>
</head>
<body class="seat-map-page booking-page min-h-screen flex flex-col">
    <jsp:include page="/WEB-INF/views/common/public-header.jsp"/>

    <c:if test="${not empty flashSuccess}">
        <div id="toast" class="booking-toast booking-toast--success" role="status">
            <span class="material-symbols-outlined">check_circle</span><c:out value="${flashSuccess}"/>
        </div>
    </c:if>

    <main class="flex-1 max-w-6xl w-full mx-auto px-4 sm:px-10 pt-6 sm:pt-8 space-y-5">
        <c:choose>
            <c:when test="${not empty errorMessage}">
                <div class="cl-card p-10 text-center space-y-4">
                    <span class="material-symbols-outlined text-5xl text-[#e50914]">event_busy</span>
                    <p class="text-white font-semibold"><c:out value="${errorMessage}"/></p>
                    <a href="${ctx}/movies" class="booking-btn booking-btn--primary">Chọn phim khác</a>
                </div>
            </c:when>
            <c:otherwise>
                <c:set var="showtime" value="${seatMap.showtime}"/>
                <c:set var="movie" value="${seatMap.movie}"/>
                <c:set var="room" value="${seatMap.room}"/>

                <div class="flex flex-wrap items-center justify-between gap-3">
                    <a href="${ctx}/showtimes?movieId=${movie.movieId}&date=${showtime.startTime.toLocalDate()}"
                       class="inline-flex items-center gap-1 text-sm text-[#8e919f] hover:text-white transition">
                        <span class="material-symbols-outlined text-base">arrow_back</span>Chọn suất khác
                    </a>
                    <jsp:include page="booking-steps.jsp"><jsp:param name="step" value="1"/></jsp:include>
                </div>

                <c:if test="${not empty flashError}">
                    <div class="seat-map-alert" role="alert">
                        <span class="material-symbols-outlined">error</span><span><c:out value="${flashError}"/></span>
                    </div>
                </c:if>

                <section class="cl-card p-5 sm:p-6 flex flex-col sm:flex-row gap-5">
                    <div class="seat-map-poster">
                        <span class="material-symbols-outlined text-4xl">movie</span>
                        <c:if test="${not empty movie.posterUrl}">
                            <img alt="" onerror="this.remove()"
                                 src="${fn:startsWith(movie.posterUrl, 'http') ? '' : ctx.concat('/')}<c:out value='${movie.posterUrl}'/>"/>
                        </c:if>
                    </div>
                    <div class="flex-1 space-y-3">
                        <div class="flex flex-wrap items-center gap-2">
                            <h1 class="text-2xl sm:text-3xl font-extrabold text-white"><c:out value="${movie.title}"/></h1>
                            <span class="booking-age ${movie.ageRating == 'P' ? 'booking-age--p' : ''}"><c:out value="${movie.ageRating}"/></span>
                        </div>
                        <div class="grid grid-cols-2 md:grid-cols-4 gap-3 text-sm">
                            <div>
                                <div class="seat-map-meta-label">Ngày chiếu</div>
                                <div class="text-white font-semibold">${showtime.startTime.format(dateFormat)}</div>
                            </div>
                            <div>
                                <div class="seat-map-meta-label">Giờ chiếu</div>
                                <div class="text-white font-semibold">${showtime.startTime.format(timeFormat)} · ${movie.durationMinutes} phút</div>
                            </div>
                            <div>
                                <div class="seat-map-meta-label">Phòng chiếu</div>
                                <div class="text-white font-semibold"><c:out value="${room.roomName}"/> · <c:out value="${room.format}"/></div>
                            </div>
                            <div>
                                <div class="seat-map-meta-label">Giá vé</div>
                                <ul class="booking-price-list">
                                    <li>Thường <b><fmt:formatNumber value="${booking.seatPrices['Standard']}" maxFractionDigits="0"/>đ</b></li>
                                    <li>VIP <b><fmt:formatNumber value="${booking.seatPrices['VIP']}" maxFractionDigits="0"/>đ</b></li>
                                    <li>Đôi <b><fmt:formatNumber value="${booking.seatPrices['Sweetbox']}" maxFractionDigits="0"/>đ</b></li>
                                </ul>
                            </div>
                        </div>
                    </div>
                </section>

                <div id="fullyBookedBanner" class="seat-map-alert" role="alert" ${seatMap.fullyBooked ? '' : 'hidden'}>
                    <span class="material-symbols-outlined">event_seat</span><span><c:out value="${fullyBookedMessage}"/></span>
                </div>
                <div id="liveAlert" class="seat-map-alert" role="alert" hidden>
                    <span class="material-symbols-outlined">error</span><span data-message></span>
                </div>

                <section class="cl-card p-5 sm:p-8 space-y-6" id="seatPicker"
                         data-status-url="<c:out value='${statusUrl}'/>"
                         data-refresh-seconds="${refreshSeconds}"
                         data-max-seats="${booking.maxSeats}"
                         data-own-seats="${ownSeatIds}"
                         data-preselected="${preselectedSeatIds}"
                         data-taken-message="<c:out value='${seatTakenMessage}'/>">
                    <div class="flex flex-wrap items-center justify-between gap-3 text-xs text-[#8e919f]">
                        <span class="flex items-center gap-2">
                            <span class="live-dot"></span>Trạng thái ghế cập nhật trực tiếp
                        </span>
                        <span>Chọn tối đa ${booking.maxSeats} ghế. Ghế chỉ được giữ khi bạn bấm Tiếp tục.</span>
                    </div>

                    <div class="seat-map-scroll">
                        <div class="seat-map-stage" style="--seat-cols: ${seatMap.columnCount}">
                            <div class="seat-map-screen">MÀN HÌNH</div>
                            <c:forEach var="row" items="${seatMap.rows}">
                                <div class="seat-row">
                                    <span class="seat-row-label"><c:out value="${row.rowName}"/></span>
                                    <div class="seat-row-cells">
                                        <c:forEach var="seat" items="${row.cells}">
                                            <c:choose>
                                                <c:when test="${empty seat}">
                                                    <span class="seat-gap"></span>
                                                </c:when>
                                                <c:otherwise>
                                                    <c:set var="typeLabel" value="${seat.seatType == 'VIP' ? 'Ghế VIP' : seat.seatType == 'Sweetbox' ? 'Ghế đôi (2 người)' : 'Ghế thường'}"/>
                                                    <button type="button"
                                                            class="seat seat--${fn:toLowerCase(seat.seatType)} is-${fn:toLowerCase(seat.status)}"
                                                            data-seat-id="${seat.seatId}"
                                                            data-label="<c:out value='${seat.label}'/>"
                                                            data-price="${booking.seatPrices[seat.seatType]}"
                                                            title="<c:out value='${seat.label} · ${typeLabel}'/>"
                                                            aria-label="<c:out value='${seat.label} · ${typeLabel}'/>"
                                                            aria-pressed="false"
                                                            ${seat.status == 'AVAILABLE' ? '' : 'disabled'}>${seat.colNumber}</button>
                                                </c:otherwise>
                                            </c:choose>
                                        </c:forEach>
                                    </div>
                                    <span class="seat-row-label"><c:out value="${row.rowName}"/></span>
                                </div>
                            </c:forEach>
                        </div>
                    </div>

                    <div class="seat-map-legend">
                        <div>
                            <div class="seat-map-meta-label mb-2">Trạng thái ghế</div>
                            <ul class="legend-list">
                                <li><span class="seat seat--legend is-available"></span>Trống</li>
                                <li><span class="seat seat--legend is-selected"></span>Đang chọn</li>
                                <li><span class="seat seat--legend is-pending"></span>Đang giữ</li>
                                <li><span class="seat seat--legend is-sold"></span>Đã bán</li>
                                <li><span class="seat seat--legend is-maintenance"></span>Bảo trì</li>
                            </ul>
                        </div>
                        <div>
                            <div class="seat-map-meta-label mb-2">Loại ghế</div>
                            <ul class="legend-list">
                                <li><span class="seat seat--legend seat--standard is-available"></span>Ghế thường</li>
                                <li><span class="seat seat--legend seat--vip is-available"></span>Ghế VIP</li>
                                <li><span class="seat seat--legend seat--sweetbox is-available"></span>Ghế đôi (Sweetbox, 2 người)</li>
                            </ul>
                        </div>
                    </div>
                </section>

                <form id="holdForm" method="post" action="${ctx}/customer/booking/hold" class="booking-summary">
                    <input type="hidden" name="csrfToken" value="${csrfToken}"/>
                    <input type="hidden" name="showtimeId" value="${showtime.showtimeId}"/>
                    <input type="hidden" name="ageConfirmed" id="ageConfirmed" value="0"/>
                    <div class="booking-summary-seats">
                        <div class="seat-map-meta-label">Ghế đã chọn (<span id="selectedCount">0</span>/${booking.maxSeats})</div>
                        <div id="selectedSeats" data-empty="Chưa chọn ghế">Chưa chọn ghế</div>
                    </div>
                    <div class="text-right">
                        <div class="seat-map-meta-label">Tạm tính</div>
                        <div class="booking-total"><span id="selectedTotal">0</span>đ</div>
                    </div>
                    <c:choose>
                        <c:when test="${canBook}">
                            <button type="submit" id="continueBtn" class="booking-btn booking-btn--primary" disabled
                                    data-guest="${isGuest}">
                                Tiếp tục<span class="material-symbols-outlined text-base">arrow_forward</span>
                            </button>
                        </c:when>
                        <c:otherwise>
                            <span class="booking-hint"><c:out value="${customerOnlyMessage}"/></span>
                        </c:otherwise>
                    </c:choose>
                </form>

                <%-- Bước 2 / BR-AGE-01: phim C13, C16, C18 phải xác nhận độ tuổi trước khi chọn ghế; từ chối -> E5 --%>
                <c:if test="${booking.ageRestricted}">
                    <div id="ageModal" class="booking-modal" role="dialog" aria-modal="true" aria-labelledby="ageTitle">
                        <div class="booking-modal-box">
                            <span class="booking-age text-base"><c:out value="${movie.ageRating}"/></span>
                            <h2 id="ageTitle">Xác nhận độ tuổi</h2>
                            <p>
                                Phim <strong class="text-white"><c:out value="${movie.title}"/></strong> được phân loại
                                <strong class="text-white"><c:out value="${movie.ageRating}"/></strong>: chỉ dành cho khán giả từ
                                ${ageYears[movie.ageRating]} tuổi trở lên. Rạp có quyền kiểm tra giấy tờ tùy thân khi vào phòng chiếu
                                và từ chối phục vụ (không hoàn tiền) nếu không đủ tuổi.
                            </p>
                            <div class="booking-modal-actions">
                                <a href="${ctx}/movies" class="booking-btn booking-btn--ghost">Không, chọn phim khác</a>
                                <button type="button" id="ageConfirmBtn" class="booking-btn booking-btn--primary">
                                    Tôi đủ ${ageYears[movie.ageRating]} tuổi trở lên
                                </button>
                            </div>
                        </div>
                    </div>
                </c:if>

                <%-- AF1: Guest bấm Tiếp tục -> yêu cầu đăng nhập, sau đó quay lại đúng suất và ghế đã chọn --%>
                <c:if test="${isGuest}">
                    <div id="loginModal" class="booking-modal" role="dialog" aria-modal="true" aria-labelledby="loginTitle" hidden>
                        <div class="booking-modal-box">
                            <span class="material-symbols-outlined text-5xl text-[#e50914]">lock</span>
                            <h2 id="loginTitle">Đăng nhập để đặt vé</h2>
                            <p>Bạn cần đăng nhập tài khoản thành viên để giữ ghế và thanh toán. Ghế bạn đã chọn sẽ được giữ nguyên lựa chọn sau khi đăng nhập.</p>
                            <div class="booking-modal-actions">
                                <a href="${ctx}/register" class="booking-btn booking-btn--ghost">Đăng ký thành viên</a>
                                <a href="${ctx}/login" id="loginLink" class="booking-btn booking-btn--primary"
                                   data-login-url="${ctx}/login?returnUrl="
                                   data-return-url="${ctx}/booking?showtimeId=${showtime.showtimeId}">Đăng nhập</a>
                            </div>
                            <button type="button" class="booking-hint mt-4 underline" data-close-modal>Để sau</button>
                        </div>
                    </div>
                </c:if>
            </c:otherwise>
        </c:choose>
    </main>

    <jsp:include page="/WEB-INF/views/common/public-footer.jsp"/>
    <c:if test="${empty errorMessage}">
        <script src="${ctx}/assets/js/booking-seats.js"></script>
    </c:if>
</body>
</html>
