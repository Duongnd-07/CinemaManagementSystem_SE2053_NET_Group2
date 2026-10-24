<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" import="java.time.format.DateTimeFormatter, java.time.LocalDate" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="dayMonth" value="${DateTimeFormatter.ofPattern('dd/MM')}"/>
<c:set var="fullDate" value="${DateTimeFormatter.ofPattern('dd/MM/yyyy')}"/>
<c:set var="timeFormat" value="${DateTimeFormatter.ofPattern('HH:mm')}"/>
<c:set var="today" value="${LocalDate.now()}"/>
<c:set var="weekdays" value="${{'MONDAY':'Thứ Hai','TUESDAY':'Thứ Ba','WEDNESDAY':'Thứ Tư','THURSDAY':'Thứ Năm','FRIDAY':'Thứ Sáu','SATURDAY':'Thứ Bảy','SUNDAY':'Chủ Nhật'}}"/>
<c:set var="selDate" value="${schedule.selectedDate}"/>
<c:set var="selFormat" value="${schedule.selectedFormat}"/>
<c:set var="selRoom" value="${schedule.selectedRoomId}"/>
<c:set var="selTrans" value="${schedule.selectedTranslationType}"/>
<%-- URL gốc giữ phim; các link bộ lọc nối thêm tham số còn lại --%>
<c:set var="baseUrl" value="${ctx}/showtimes?movieId=${movie.movieId}"/>
<c:set var="formatParam" value="${empty selFormat ? '' : '&format='.concat(selFormat)}"/>
<c:set var="roomParam" value="${selRoom > 0 ? '&room='.concat(selRoom) : ''}"/>
<c:set var="dateParam" value="${empty selDate ? '' : '&date='.concat(selDate)}"/>
<c:set var="transParam" value="${empty selTrans ? '' : '&translationType='.concat(selTrans)}"/>
<c:set var="posterSrc" value="${empty movie.posterUrl ? '' : (fn:startsWith(movie.posterUrl, 'http') ? movie.posterUrl : ctx.concat('/').concat(movie.posterUrl))}"/>
<!DOCTYPE html>
<html lang="vi" class="dark">
<head>
    <title>Lịch chiếu <c:out value="${movie.title}"/> - CineLuxe Cinema</title>
    <jsp:include page="/WEB-INF/views/common/public-head.jsp"/>
    <style>
        body {
            font-family: 'Be Vietnam Pro', sans-serif;
            background-color: #0f131d;
            color: #dfe2f1;
        }
    </style>
</head>
<body class="min-h-screen bg-[#0f131d] flex flex-col">
    <jsp:include page="/WEB-INF/views/common/public-header.jsp"/>

    <main class="flex-1 max-w-7xl w-full mx-auto p-6 sm:p-10 space-y-6">
        <!-- Phim đang xem lịch chiếu -->
        <div class="flex items-center gap-4 bg-[#171b26] border border-white/10 rounded-2xl p-4">
            <div class="movie-poster w-16 sm:w-20 shrink-0 rounded-lg overflow-hidden">
                <span class="material-symbols-outlined text-3xl text-[#8e919f]">image</span>
                <c:if test="${not empty posterSrc}">
                    <img alt="" onerror="this.remove()" referrerpolicy="no-referrer" src="<c:out value='${posterSrc}'/>"/>
                </c:if>
            </div>
            <div class="min-w-0 flex-1 space-y-1">
                <p class="text-xs font-semibold uppercase tracking-widest text-[#e50914]">Lịch chiếu</p>
                <h1 class="text-xl sm:text-2xl font-extrabold text-white truncate"><c:out value="${movie.title}"/></h1>
                <div class="flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-[#8e919f]">
                    <span class="age-badge age-badge-inline age-${fn:toLowerCase(movie.ageRating)}"><c:out value="${movie.ageRating}"/></span>
                    <span class="flex items-center gap-1"><span class="material-symbols-outlined text-sm">schedule</span>${movie.durationMinutes} phút</span>
                    <a href="${ctx}/movies/detail?id=${movie.movieId}" class="flex items-center gap-1 text-[#dfe2f1] hover:text-[#e50914] transition">
                        <span class="material-symbols-outlined text-sm">info</span>Chi tiết phim
                    </a>
                </div>
            </div>
        </div>

        <!-- Chọn ngày: hôm nay + 7 ngày tới; "Tất cả" là chế độ mặc định -->
        <nav class="date-strip" aria-label="Chọn ngày chiếu">
            <a href="<c:out value='${baseUrl}${formatParam}${roomParam}${transParam}'/>" class="date-chip ${empty selDate ? 'active' : ''}">
                <span class="date-chip-top">Tất cả</span>
                <span class="date-chip-main">8 ngày</span>
            </a>
            <c:forEach var="day" items="${schedule.days}">
                <c:set var="dayCount" value="${schedule.countByDay[day]}"/>
                <a href="<c:out value='${baseUrl}&date=${day}${formatParam}${roomParam}${transParam}'/>"
                   class="date-chip ${day == selDate ? 'active' : ''} ${dayCount == 0 ? 'empty' : ''}">
                    <span class="date-chip-top">${day == today ? 'Hôm nay' : (day == today.plusDays(1) ? 'Ngày mai' : weekdays[day.dayOfWeek.name()])}</span>
                    <span class="date-chip-main">${day.format(dayMonth)}</span>
                    <span class="date-chip-count">${dayCount} suất</span>
                </a>
            </c:forEach>
        </nav>

        <!-- Lọc định dạng, bản chiếu và phòng -->
        <div class="flex flex-col gap-3">
            <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                <div class="flex flex-wrap items-center gap-2" role="group" aria-label="Định dạng chiếu">
                    <a href="<c:out value='${baseUrl}${dateParam}${roomParam}${transParam}'/>" class="movie-tab ${empty selFormat ? 'active' : ''}">Tất cả định dạng</a>
                    <c:forEach var="format" items="${schedule.formats}">
                        <a href="<c:out value='${baseUrl}${dateParam}&format=${format}${roomParam}${transParam}'/>" class="movie-tab ${format == selFormat ? 'active' : ''}"><c:out value="${format}"/></a>
                    </c:forEach>
                </div>
                <form method="get" action="${ctx}/showtimes" class="flex items-center gap-2">
                    <input type="hidden" name="movieId" value="${movie.movieId}"/>
                    <c:if test="${not empty selDate}"><input type="hidden" name="date" value="${selDate}"/></c:if>
                    <c:if test="${not empty selFormat}"><input type="hidden" name="format" value="<c:out value='${selFormat}'/>"/></c:if>
                    <c:if test="${not empty selTrans}"><input type="hidden" name="translationType" value="<c:out value='${selTrans}'/>"/></c:if>
                    <label for="room" class="text-sm text-[#8e919f] whitespace-nowrap">Phòng</label>
                    <select id="room" name="room" onchange="this.form.submit()"
                            class="h-10 px-3 rounded-xl bg-[#0f131d] border border-white/10 text-sm text-white focus:border-[#e50914] focus:outline-none [color-scheme:dark]">
                        <option value="">Tất cả phòng</option>
                        <c:forEach var="room" items="${schedule.rooms}">
                            <option value="${room.roomId}" ${room.roomId == selRoom ? 'selected' : ''}><c:out value="${room.roomName}"/><c:if test="${not empty room.format}"> (<c:out value="${room.format}"/>)</c:if></option>
                        </c:forEach>
                    </select>
                    <noscript><button type="submit" class="movie-tab">Lọc</button></noscript>
                </form>
            </div>

            <!-- Bộ lọc Bản chiếu: Tất cả / Phụ đề / Lồng tiếng -->
            <div class="flex flex-wrap items-center gap-2 pt-1 border-t border-white/5" role="group" aria-label="Bản chiếu">
                <span class="text-xs text-[#8e919f] font-semibold uppercase tracking-wider mr-1 flex items-center gap-1">
                    <span class="material-symbols-outlined text-sm text-[#e50914]">translate</span> Bản chiếu:
                </span>
                <a href="<c:out value='${baseUrl}${dateParam}${formatParam}${roomParam}'/>"
                   class="px-3 py-1 rounded-lg text-xs font-semibold transition ${empty selTrans ? 'bg-white/15 text-white border border-white/30' : 'bg-[#171b26] text-[#8e919f] hover:text-white border border-white/10'}">
                    Tất cả
                </a>
                <a href="<c:out value='${baseUrl}${dateParam}${formatParam}${roomParam}&translationType=Phụ đề'/>"
                   class="px-3 py-1 rounded-lg text-xs font-semibold transition ${selTrans == 'Phụ đề' ? 'bg-sky-500/20 text-sky-300 border border-sky-500/40' : 'bg-[#171b26] text-[#8e919f] hover:text-white border border-white/10'}">
                    Phụ đề
                </a>
                <a href="<c:out value='${baseUrl}${dateParam}${formatParam}${roomParam}&translationType=Lồng tiếng'/>"
                   class="px-3 py-1 rounded-lg text-xs font-semibold transition ${selTrans == 'Lồng tiếng' ? 'bg-amber-500/20 text-amber-300 border border-amber-500/40' : 'bg-[#171b26] text-[#8e919f] hover:text-white border border-white/10'}">
                    Lồng tiếng
                </a>
            </div>
        </div>

        <!-- Danh sách suất chiếu: Ngày -> Định dạng -> Phòng -->
        <c:choose>
            <c:when test="${empty schedule.groups}">
                <div class="movie-empty">
                    <span class="material-symbols-outlined text-4xl text-[#8e919f]">event_busy</span>
                    <p><c:out value="${emptyMessage}"/></p>
                </div>
            </c:when>
            <c:otherwise>
                <div class="space-y-6">
                    <c:forEach var="dayEntry" items="${schedule.groups}">
                        <c:set var="day" value="${dayEntry.key}"/>
                        <section class="bg-[#171b26] border border-white/10 rounded-2xl overflow-hidden">
                            <h2 class="px-5 py-3 bg-white/[0.03] border-b border-white/10 text-base font-bold text-white flex items-center gap-2">
                                <span class="material-symbols-outlined text-[#e50914]">calendar_month</span>
                                ${weekdays[day.dayOfWeek.name()]}, ${day.format(fullDate)}
                                <c:if test="${day == today}"><span class="text-xs font-semibold text-[#ffb95f]">(Hôm nay)</span></c:if>
                            </h2>
                            <div class="divide-y divide-white/5">
                                <c:forEach var="formatEntry" items="${dayEntry.value}">
                                    <div class="p-5 space-y-4">
                                        <span class="inline-block px-3 py-1 rounded-md text-xs font-extrabold tracking-wider bg-[#e50914]/15 text-[#ff8a85] border border-[#e50914]/30"><c:out value="${formatEntry.key}"/></span>
                                        <c:forEach var="roomEntry" items="${formatEntry.value}">
                                            <div class="flex flex-col sm:flex-row sm:items-start gap-2 sm:gap-4">
                                                <p class="text-sm font-semibold text-[#dfe2f1] sm:w-36 shrink-0 sm:pt-2 flex items-center gap-1">
                                                    <span class="material-symbols-outlined text-base text-[#8e919f]">meeting_room</span><c:out value="${roomEntry.key}"/>
                                                </p>
                                                <div class="flex flex-wrap gap-2">
                                                    <c:forEach var="showtime" items="${roomEntry.value}">
                                                        <%-- Chọn giờ chiếu -> chọn ghế của suất (UC-13) để bắt đầu đặt vé --%>
                                                        <a href="${ctx}/booking?showtimeId=${showtime.showtimeId}" class="time-slot"
                                                           title="Chọn ghế suất ${showtime.startTime.format(timeFormat)} (${showtime.translationType}) - <c:out value='${roomEntry.key}'/>">
                                                            <span class="time-slot-start flex items-center justify-between gap-1.5">
                                                                <span>${showtime.startTime.format(timeFormat)}</span>
                                                                <span class="inline-block text-[10px] font-bold px-1.5 py-0.5 rounded leading-none ${showtime.translationType == 'Lồng tiếng' ? 'bg-amber-500/20 text-amber-300 border border-amber-500/40' : 'bg-sky-500/20 text-sky-300 border border-sky-500/40'}">
                                                                    <c:out value="${empty showtime.translationType ? 'Phụ đề' : showtime.translationType}"/>
                                                                </span>
                                                            </span>
                                                            <span class="time-slot-sub">
                                                                <c:if test="${not empty showtime.endTime}">~${showtime.endTime.format(timeFormat)} · </c:if><fmt:formatNumber value="${showtime.basePrice}" type="number" maxFractionDigits="0"/>đ
                                                            </span>
                                                        </a>
                                                    </c:forEach>
                                                </div>
                                            </div>
                                        </c:forEach>
                                    </div>
                                </c:forEach>
                            </div>
                        </section>
                    </c:forEach>
                </div>
            </c:otherwise>
        </c:choose>
    </main>

    <jsp:include page="/WEB-INF/views/common/public-footer.jsp"/>
</body>
</html>
