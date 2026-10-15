<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="vi" class="dark">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Sơ đồ ghế<c:if test="${not empty seatMap}"> - <c:out value="${seatMap.movie.title}"/></c:if> | CineLuxe</title>
    <script src="https://cdn.tailwindcss.com"></script>
    <link href="https://fonts.googleapis.com/css2?family=Be+Vietnam+Pro:wght@400;500;600;700;800&display=swap" rel="stylesheet"/>
    <link href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@24,400,0,0" rel="stylesheet"/>
    <link href="${ctx}/assets/css/cineluxe.css" rel="stylesheet"/>
    <link href="${ctx}/assets/css/seat-map.css" rel="stylesheet"/>
</head>
<body class="seat-map-page min-h-screen flex flex-col">
    <header class="h-20 bg-[#171b26]/90 backdrop-blur-md border-b border-white/10 px-6 sm:px-10 flex items-center justify-between sticky top-0 z-50">
        <a href="${ctx}/" class="flex items-center gap-2 group">
            <span class="material-symbols-outlined text-4xl text-[#e50914] transition-transform group-hover:scale-110">movie</span>
            <span class="text-2xl font-extrabold tracking-widest text-white">CINE<span class="text-[#e50914]">LUXE</span></span>
        </a>
        <div class="flex items-center gap-3 text-xs font-semibold">
            <c:choose>
                <c:when test="${not empty sessionScope.user}">
                    <span class="flex items-center gap-1 text-white">
                        <span class="material-symbols-outlined text-base text-[#e50914]">person</span>
                        <c:out value="${sessionScope.user.fullName}"/>
                    </span>
                </c:when>
                <c:otherwise>
                    <a href="${ctx}/login" class="px-4 py-2 text-white hover:text-[#e50914] transition">Đăng nhập</a>
                </c:otherwise>
            </c:choose>
        </div>
    </header>

    <main class="flex-1 max-w-6xl w-full mx-auto p-6 sm:p-10 space-y-6">
        <a href="${ctx}/" class="inline-flex items-center gap-1 text-sm text-[#8e919f] hover:text-white transition">
            <span class="material-symbols-outlined text-base">arrow_back</span>
            Quay lại
        </a>

        <c:choose>
            <c:when test="${not empty errorMessage}">
                <div class="cl-card p-10 text-center space-y-4">
                    <span class="material-symbols-outlined text-5xl text-[#e50914]">event_busy</span>
                    <p class="text-white font-semibold"><c:out value="${errorMessage}"/></p>
                    <a href="${ctx}/" class="inline-flex px-5 py-2.5 bg-[#e50914] hover:bg-[#b80710] text-white font-bold rounded-xl text-sm transition">
                        Về trang chủ
                    </a>
                </div>
            </c:when>
            <c:otherwise>
                <c:set var="showtime" value="${seatMap.showtime}"/>
                <c:set var="movie" value="${seatMap.movie}"/>
                <c:set var="room" value="${seatMap.room}"/>

                <section class="cl-card p-5 sm:p-6 flex flex-col sm:flex-row gap-5">
                    <div class="seat-map-poster">
                        <c:choose>
                            <c:when test="${not empty movie.posterUrl}">
                                <img alt="Poster ${fn:escapeXml(movie.title)}"
                                     src="${fn:startsWith(movie.posterUrl, 'http') ? '' : ctx.concat('/')}<c:out value='${movie.posterUrl}'/>"/>
                            </c:when>
                            <c:otherwise>
                                <span class="material-symbols-outlined text-4xl">movie</span>
                            </c:otherwise>
                        </c:choose>
                    </div>
                    <div class="flex-1 space-y-3">
                        <div class="flex flex-wrap items-center gap-2">
                            <h1 class="text-2xl sm:text-3xl font-extrabold text-white"><c:out value="${movie.title}"/></h1>
                            <span class="age-badge"><c:out value="${movie.ageRating}"/></span>
                        </div>
                        <div class="grid grid-cols-2 md:grid-cols-4 gap-3 text-sm">
                            <div>
                                <div class="seat-map-meta-label">Ngày chiếu</div>
                                <div class="text-white font-semibold">${showDate}</div>
                            </div>
                            <div>
                                <div class="seat-map-meta-label">Giờ chiếu</div>
                                <div class="text-white font-semibold">${showTime} · ${movie.durationMinutes} phút</div>
                            </div>
                            <div>
                                <div class="seat-map-meta-label">Phòng chiếu</div>
                                <div class="text-white font-semibold"><c:out value="${room.roomName}"/> · <c:out value="${room.format}"/></div>
                            </div>
                            <div>
                                <div class="seat-map-meta-label">Giá vé từ</div>
                                <div class="cl-price"><fmt:formatNumber value="${showtime.basePrice}" maxFractionDigits="0"/>đ</div>
                            </div>
                        </div>
                    </div>
                </section>

                <%-- E1: luôn render banner để script bật/tắt khi trạng thái ghế thay đổi --%>
                <div id="fullyBookedBanner" class="seat-map-alert" role="alert" ${seatMap.fullyBooked ? '' : 'hidden'}>
                    <span class="material-symbols-outlined">event_seat</span>
                    <span><c:out value="${fullyBookedMessage}"/></span>
                </div>
                <div id="refreshError" class="seat-map-alert" role="alert" hidden>
                    <span class="material-symbols-outlined">error</span>
                    <span data-message></span>
                </div>

                <section class="cl-card p-5 sm:p-8 space-y-6" id="seatMap"
                         data-status-url="<c:out value='${statusUrl}'/>" data-refresh-seconds="${refreshSeconds}">
                    <div class="flex flex-wrap items-center justify-between gap-3 text-xs text-[#8e919f]">
                        <span class="flex items-center gap-2">
                            <span class="live-dot"></span>
                            Cập nhật trực tiếp lúc <strong class="text-white" id="updatedAt">${updatedAt}</strong>
                        </span>
                        <span>Sơ đồ chỉ để xem, ghế chỉ được giữ khi bạn tiến hành đặt vé.</span>
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
                                                    <c:set var="statusLabel" value="${seat.status == 'PENDING' ? 'Đang giữ' : seat.status == 'SOLD' ? 'Đã bán' : seat.status == 'MAINTENANCE' ? 'Bảo trì' : 'Trống'}"/>
                                                    <span class="seat seat--${fn:toLowerCase(seat.seatType)} is-${fn:toLowerCase(seat.status)}"
                                                          data-seat-id="${seat.seatId}"
                                                          data-name="<c:out value='${seat.label} · ${typeLabel}'/>"
                                                          title="<c:out value='${seat.label} · ${typeLabel} · ${statusLabel}'/>"
                                                          aria-label="<c:out value='${seat.label} · ${typeLabel} · ${statusLabel}'/>">${seat.colNumber}</span>
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
                                <li data-status="AVAILABLE" data-label="Trống"><span class="seat seat--legend is-available"></span>Trống <b id="count-AVAILABLE">${seatMap.statusCounts['AVAILABLE']}</b></li>
                                <li data-status="PENDING" data-label="Đang giữ"><span class="seat seat--legend is-pending"></span>Đang giữ <b id="count-PENDING">${seatMap.statusCounts['PENDING']}</b></li>
                                <li data-status="SOLD" data-label="Đã bán"><span class="seat seat--legend is-sold"></span>Đã bán <b id="count-SOLD">${seatMap.statusCounts['SOLD']}</b></li>
                                <li data-status="MAINTENANCE" data-label="Bảo trì"><span class="seat seat--legend is-maintenance"></span>Bảo trì <b id="count-MAINTENANCE">${seatMap.statusCounts['MAINTENANCE']}</b></li>
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
            </c:otherwise>
        </c:choose>
    </main>

    <footer class="bg-[#171b26] border-t border-white/10 py-6 px-6 text-center text-xs text-[#8e919f]">
        &copy; 2026 CineLuxe Cinema - SWP391 Group 2. Mọi quyền được bảo lưu.
    </footer>

    <c:if test="${empty errorMessage}">
        <script src="${ctx}/assets/js/seat-map.js"></script>
    </c:if>
</body>
</html>
