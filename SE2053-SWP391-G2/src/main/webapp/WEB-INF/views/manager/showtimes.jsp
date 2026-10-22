<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="inputClass" value="w-full bg-surface-container-high text-on-surface px-4 py-2.5 rounded-lg border border-white/10 focus:border-primary outline-none text-body-md transition-colors"/>
<!DOCTYPE html>
<html lang="vi" class="dark">
<head>
    <title>Quản lý suất chiếu - CineAdmin</title>
    <jsp:include page="/WEB-INF/views/common/manager-head.jsp"/>
</head>
<body class="bg-surface font-body-md text-on-surface min-h-screen">
<jsp:include page="/WEB-INF/views/common/manager-sidebar.jsp"/>

<main class="pl-64 pt-16">
<div class="px-space-xl pt-space-lg pb-16">

    <%-- Toast thông báo tự động ẩn sau 5s --%>
    <c:if test="${not empty flashSuccess or not empty flashError}">
        <div id="toast" role="status"
             class="fixed top-20 right-8 z-[60] flex items-center gap-space-sm px-space-lg py-space-md rounded-xl shadow-2xl border transition-all duration-300
                    ${not empty flashSuccess ? 'bg-emerald-500/15 border-emerald-500/40 text-emerald-300' : 'bg-error-container/80 border-error/40 text-on-error-container'}">
            <span class="material-symbols-outlined">${not empty flashSuccess ? 'check_circle' : 'error'}</span>
            <span class="font-label-lg"><c:out value="${not empty flashSuccess ? flashSuccess : flashError}"/></span>
        </div>
    </c:if>

    <%-- Tiêu đề trang & Nút thêm mới --%>
    <div class="flex flex-col md:flex-row md:items-center justify-between gap-space-md mb-space-xl">
        <div>
            <div class="flex items-center gap-space-sm mb-space-xs">
                <span class="text-label-sm px-2 py-0.5 rounded bg-primary/20 text-primary uppercase">Quản lý rạp</span>
                <span class="text-outline text-body-sm">/</span>
                <span class="text-on-surface-variant text-body-sm">Suất chiếu (UC-24)</span>
            </div>
            <h1 class="text-headline-lg text-on-surface font-bold">Lên lịch &amp; Quản lý suất chiếu</h1>
            <p class="text-on-surface-variant text-body-sm mt-1">
                Tự động kiểm tra chống trùng phòng (<span class="text-primary font-medium">+15 phút dọn rạp</span>) và định giá thông minh theo ngày/khung giờ.
            </p>
        </div>
        <a href="${ctx}/manager/showtimes?add=1${fn:escapeXml(fn:replace(listQuery, '?', '&'))}"
           class="flex items-center gap-space-sm bg-primary hover:bg-primary/90 text-on-primary px-space-lg py-2.5 rounded-full font-label-lg shadow-[0_0_20px_-2px_rgba(229,9,20,0.45)] transition-all whitespace-nowrap self-start md:self-auto">
            <span class="material-symbols-outlined text-[20px]">add_circle</span>
            <span>Lên lịch suất chiếu mới</span>
        </a>
    </div>

    <%-- Thẻ đếm trạng thái (Status Filter Tabs) --%>
    <section class="grid grid-cols-2 lg:grid-cols-4 gap-space-md mb-space-xl" aria-label="Thống kê suất chiếu">
        <c:url var="tabAllUrl" value="/manager/showtimes">
            <c:if test="${not empty selectedDate}"><c:param name="date" value="${selectedDate}"/></c:if>
            <c:if test="${not empty selectedMovieId}"><c:param name="movieId" value="${selectedMovieId}"/></c:if>
            <c:if test="${not empty selectedRoomId}"><c:param name="roomId" value="${selectedRoomId}"/></c:if>
        </c:url>
        <a href="${tabAllUrl}"
           class="p-space-lg rounded-xl border transition-all ${empty selectedStatus ? 'bg-surface-container border-primary shadow-lg shadow-primary/10' : 'bg-surface-container-low border-white/5 hover:border-white/20'}">
            <div class="flex items-center justify-between">
                <span class="text-on-surface-variant text-body-sm font-medium">Tất cả suất chiếu</span>
                <span class="material-symbols-outlined text-outline text-[20px]">calendar_month</span>
            </div>
            <p class="text-headline-lg text-on-surface font-bold mt-2">
                <c:set var="totalCount" value="0"/>
                <c:forEach var="entry" items="${statusCounts}">
                    <c:set var="totalCount" value="${totalCount + entry.value}"/>
                </c:forEach>
                ${totalCount}
            </p>
        </a>

        <c:url var="tabActiveUrl" value="/manager/showtimes">
            <c:param name="status" value="Active"/>
            <c:if test="${not empty selectedDate}"><c:param name="date" value="${selectedDate}"/></c:if>
            <c:if test="${not empty selectedMovieId}"><c:param name="movieId" value="${selectedMovieId}"/></c:if>
            <c:if test="${not empty selectedRoomId}"><c:param name="roomId" value="${selectedRoomId}"/></c:if>
        </c:url>
        <a href="${tabActiveUrl}"
           class="p-space-lg rounded-xl border transition-all ${selectedStatus == 'Active' ? 'bg-surface-container border-emerald-500 shadow-lg shadow-emerald-500/10' : 'bg-surface-container-low border-white/5 hover:border-white/20'}">
            <div class="flex items-center justify-between">
                <span class="text-emerald-400 text-body-sm font-medium">Đang mở bán</span>
                <span class="material-symbols-outlined text-emerald-400 text-[20px]">play_circle</span>
            </div>
            <p class="text-headline-lg text-emerald-400 font-bold mt-2">${statusCounts['Active'] != null ? statusCounts['Active'] : 0}</p>
        </a>

        <c:url var="tabEndedUrl" value="/manager/showtimes">
            <c:param name="status" value="Ended"/>
            <c:if test="${not empty selectedDate}"><c:param name="date" value="${selectedDate}"/></c:if>
            <c:if test="${not empty selectedMovieId}"><c:param name="movieId" value="${selectedMovieId}"/></c:if>
            <c:if test="${not empty selectedRoomId}"><c:param name="roomId" value="${selectedRoomId}"/></c:if>
        </c:url>
        <a href="${tabEndedUrl}"
           class="p-space-lg rounded-xl border transition-all ${selectedStatus == 'Ended' ? 'bg-surface-container border-slate-400 shadow-lg' : 'bg-surface-container-low border-white/5 hover:border-white/20'}">
            <div class="flex items-center justify-between">
                <span class="text-on-surface-variant text-body-sm font-medium">Đã kết thúc</span>
                <span class="material-symbols-outlined text-outline text-[20px]">check_circle</span>
            </div>
            <p class="text-headline-lg text-on-surface-variant font-bold mt-2">${statusCounts['Ended'] != null ? statusCounts['Ended'] : 0}</p>
        </a>

        <c:url var="tabCancelledUrl" value="/manager/showtimes">
            <c:param name="status" value="Cancelled"/>
            <c:if test="${not empty selectedDate}"><c:param name="date" value="${selectedDate}"/></c:if>
            <c:if test="${not empty selectedMovieId}"><c:param name="movieId" value="${selectedMovieId}"/></c:if>
            <c:if test="${not empty selectedRoomId}"><c:param name="roomId" value="${selectedRoomId}"/></c:if>
        </c:url>
        <a href="${tabCancelledUrl}"
           class="p-space-lg rounded-xl border transition-all ${selectedStatus == 'Cancelled' ? 'bg-surface-container border-rose-500 shadow-lg shadow-rose-500/10' : 'bg-surface-container-low border-white/5 hover:border-white/20'}">
            <div class="flex items-center justify-between">
                <span class="text-rose-400 text-body-sm font-medium">Đã hủy</span>
                <span class="material-symbols-outlined text-rose-400 text-[20px]">cancel</span>
            </div>
            <p class="text-headline-lg text-rose-400 font-bold mt-2">${statusCounts['Cancelled'] != null ? statusCounts['Cancelled'] : 0}</p>
        </a>
    </section>

    <%-- Thanh tìm kiếm & Bộ lọc --%>
    <form method="get" action="${ctx}/manager/showtimes"
          class="bg-surface-container-low p-space-md rounded-xl border border-white/5 mb-space-lg flex flex-wrap items-center gap-space-md">
        <input type="hidden" name="status" value="<c:out value='${selectedStatus}'/>"/>

        <div class="flex-1 min-w-[180px]">
            <label class="block text-label-sm text-on-surface-variant mb-1 font-medium">Ngày chiếu</label>
            <input type="date" name="date" value="<c:out value='${selectedDate}'/>"
                   class="w-full bg-surface-container-high text-on-surface px-4 py-2 rounded-lg border border-white/10 focus:border-primary outline-none text-body-md"/>
        </div>

        <div class="flex-1 min-w-[220px]">
            <label class="block text-label-sm text-on-surface-variant mb-1 font-medium">Phim chiếu</label>
            <select name="movieId" class="w-full bg-surface-container-high text-on-surface px-4 py-2 rounded-lg border border-white/10 focus:border-primary outline-none text-body-md">
                <option value="">-- Tất cả phim --</option>
                <c:forEach var="movie" items="${movies}">
                    <option value="${movie.movieId}" ${selectedMovieId == movie.movieId ? 'selected' : ''}>
                        <c:out value="${movie.title}"/> (${movie.durationMinutes}p)
                    </option>
                </c:forEach>
            </select>
        </div>

        <div class="flex-1 min-w-[180px]">
            <label class="block text-label-sm text-on-surface-variant mb-1 font-medium">Phòng chiếu</label>
            <select name="roomId" class="w-full bg-surface-container-high text-on-surface px-4 py-2 rounded-lg border border-white/10 focus:border-primary outline-none text-body-md">
                <option value="">-- Tất cả phòng --</option>
                <c:forEach var="room" items="${rooms}">
                    <option value="${room.roomId}" ${selectedRoomId == room.roomId ? 'selected' : ''}>
                        <c:out value="${room.roomName}"/> (${empty room.format ? '2D' : room.format})
                    </option>
                </c:forEach>
            </select>
        </div>

        <div class="flex items-end gap-2 pt-5">
            <button type="submit" class="inline-flex items-center gap-1.5 px-5 py-2.5 rounded-lg bg-primary hover:bg-primary/90 text-on-primary font-label-md transition-colors">
                <span class="material-symbols-outlined text-[18px]">filter_list</span>Lọc
            </button>
            <a href="${ctx}/manager/showtimes" class="inline-flex items-center gap-1.5 px-4 py-2.5 rounded-lg bg-surface-container-high hover:bg-surface-bright text-on-surface-variant font-label-md transition-colors" title="Đặt lại bộ lọc">
                <span class="material-symbols-outlined text-[18px]">restart_alt</span>Đặt lại
            </a>
        </div>
    </form>

    <%-- Bảng danh sách suất chiếu --%>
    <div class="bg-surface-container-low rounded-xl overflow-hidden shadow-xl border border-white/5">
        <div class="overflow-x-auto">
            <table class="w-full text-left border-collapse">
                <thead>
                <tr class="bg-surface-container border-b border-white/10 text-on-surface-variant text-label-md uppercase tracking-wider">
                    <th class="py-4 px-5">Mã suất</th>
                    <th class="py-4 px-5">Phim</th>
                    <th class="py-4 px-5">Phòng chiếu</th>
                    <th class="py-4 px-5">Khung giờ chiếu</th>
                    <th class="py-4 px-5 text-right">Giá gốc</th>
                    <th class="py-4 px-5 text-center">Đã đặt</th>
                    <th class="py-4 px-5 text-center">Trạng thái</th>
                    <th class="py-4 px-5 text-right">Thao tác</th>
                </tr>
                </thead>
                <tbody class="divide-y divide-white/5 text-body-md text-on-surface">
                <c:choose>
                    <c:when test="${empty showtimePage.items}">
                        <tr>
                            <td colspan="8" class="py-12 text-center text-on-surface-variant">
                                <span class="material-symbols-outlined text-[48px] text-outline mb-2">event_busy</span>
                                <p class="text-body-lg">Không tìm thấy suất chiếu nào phù hợp với bộ lọc.</p>
                            </td>
                        </tr>
                    </c:when>
                    <c:otherwise>
                        <c:forEach var="showtime" items="${showtimePage.items}">
                            <tr class="hover:bg-surface-container transition-colors">
                                <td class="py-4 px-5 font-mono text-outline text-label-sm whitespace-nowrap">
                                    #ST-${showtime.showtimeId}
                                </td>
                                <td class="py-4 px-5">
                                    <div class="flex items-center gap-3">
                                        <c:choose>
                                            <c:when test="${not empty showtime.movie.posterUrl}">
                                                <img src="${showtime.movie.posterUrl}" alt="Poster" class="w-10 h-14 object-cover rounded shadow-md border border-white/10 flex-shrink-0"/>
                                            </c:when>
                                            <c:otherwise>
                                                <div class="w-10 h-14 bg-surface-container-high rounded flex items-center justify-center text-outline flex-shrink-0">
                                                    <span class="material-symbols-outlined text-sm">movie</span>
                                                </div>
                                            </c:otherwise>
                                        </c:choose>
                                        <div>
                                            <div class="font-bold text-on-surface hover:text-primary transition-colors">
                                                <c:out value="${showtime.movie.title}"/>
                                            </div>
                                            <div class="flex items-center gap-2 text-label-sm text-outline mt-0.5">
                                                <span>${showtime.movie.durationMinutes} phút</span>
                                                <c:if test="${not empty showtime.movie.ageRating}">
                                                    <span class="px-1.5 py-0.2 rounded bg-surface-container-highest text-on-surface-variant border border-white/10 text-[11px]">
                                                        ${showtime.movie.ageRating}
                                                    </span>
                                                </c:if>
                                            </div>
                                        </div>
                                    </div>
                                </td>
                                <td class="py-4 px-5">
                                    <div class="font-medium text-on-surface"><c:out value="${showtime.room.roomName}"/></div>
                                    <div class="flex items-center gap-2 text-label-sm mt-0.5">
                                        <span class="px-2 py-0.5 rounded text-[11px] font-bold bg-primary/15 text-primary border border-primary/30">
                                            <c:out value="${empty showtime.room.format ? '2D' : showtime.room.format}"/>
                                        </span>
                                        <span class="text-outline">${showtime.room.totalSeats} ghế</span>
                                    </div>
                                </td>
                                <td class="py-4 px-5">
                                    <div class="font-bold text-primary text-[15px]">
                                        ${showtime.startTime.format(timeFormat)} → ${showtime.endTime.format(timeFormat)}
                                    </div>
                                    <div class="flex items-center gap-2 text-label-sm text-on-surface-variant mt-0.5">
                                        <span>${showtime.startTime.format(dateFormat)}</span>
                                        <span class="text-[11px] px-1.5 py-0.2 rounded bg-amber-500/10 text-amber-400 border border-amber-500/20" title="Thời gian bao gồm 15 phút dọn rạp sau khi phim hết">
                                            +15p dọn rạp
                                        </span>
                                    </div>
                                </td>
                                <td class="py-4 px-5 text-right font-bold text-on-surface whitespace-nowrap">
                                    <fmt:formatNumber value="${showtime.basePrice}" pattern="#,##0"/>đ
                                </td>
                                <td class="py-4 px-5 text-center">
                                    <c:choose>
                                        <c:when test="${showtime.bookedTickets > 0}">
                                            <span class="inline-flex items-center px-2 py-0.5 rounded-full text-label-sm bg-blue-500/15 text-blue-400 border border-blue-500/30 font-medium">
                                                ${showtime.bookedTickets} vé
                                            </span>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="text-outline text-label-sm">0 vé</span>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                                <td class="py-4 px-5 text-center">
                                    <c:choose>
                                        <c:when test="${showtime.status == 'Active'}">
                                            <span class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-label-sm bg-emerald-500/15 text-emerald-400 border border-emerald-500/30">
                                                <span class="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse"></span>Đang mở bán
                                            </span>
                                        </c:when>
                                        <c:when test="${showtime.status == 'Ended'}">
                                            <span class="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-label-sm bg-white/5 text-outline border border-white/10">
                                                <span class="material-symbols-outlined text-[13px]">check</span>Đã kết thúc
                                            </span>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-label-sm bg-rose-500/15 text-rose-400 border border-rose-500/30">
                                                <span class="material-symbols-outlined text-[13px]">cancel</span>Đã hủy
                                            </span>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                                <td class="py-4 px-5 text-right whitespace-nowrap">
                                    <div class="inline-flex items-center gap-1.5">
                                        <a href="${ctx}/manager/showtimes?edit=${showtime.showtimeId}${fn:escapeXml(fn:replace(listQuery, '?', '&'))}"
                                           class="inline-flex items-center gap-1 px-3 py-1.5 rounded-lg bg-surface-container-high hover:bg-surface-bright text-on-surface-variant hover:text-on-surface transition-all font-label-md"
                                           title="Chỉnh sửa thông tin">
                                            <span class="material-symbols-outlined text-[16px]">edit</span>Sửa
                                        </a>

                                        <c:if test="${showtime.status != 'Cancelled'}">
                                            <form method="post" action="${ctx}/manager/showtimes${listQuery}" class="inline"
                                                  onsubmit="return confirm('Bạn có chắc chắn muốn HỦY suất chiếu này không? Khách hàng sẽ không thể đặt vé thêm.');">
                                                <input type="hidden" name="csrfToken" value="${csrfToken}"/>
                                                <input type="hidden" name="action" value="cancel"/>
                                                <input type="hidden" name="showtimeId" value="${showtime.showtimeId}"/>
                                                <button type="submit" class="inline-flex items-center gap-1 px-2.5 py-1.5 rounded-lg bg-amber-500/10 hover:bg-amber-500/20 text-amber-400 transition-all font-label-md" title="Hủy suất chiếu">
                                                    <span class="material-symbols-outlined text-[16px]">block</span>
                                                </button>
                                            </form>
                                        </c:if>

                                        <c:if test="${showtime.bookedTickets == 0}">
                                            <form method="post" action="${ctx}/manager/showtimes${listQuery}" class="inline"
                                                  onsubmit="return confirm('Bạn có chắc muốn XÓA vĩnh viễn suất chiếu này khỏi hệ thống?');">
                                                <input type="hidden" name="csrfToken" value="${csrfToken}"/>
                                                <input type="hidden" name="action" value="delete"/>
                                                <input type="hidden" name="showtimeId" value="${showtime.showtimeId}"/>
                                                <button type="submit" class="inline-flex items-center gap-1 px-2.5 py-1.5 rounded-lg bg-rose-500/10 hover:bg-rose-500/20 text-rose-400 transition-all font-label-md" title="Xóa suất chiếu">
                                                    <span class="material-symbols-outlined text-[16px]">delete</span>
                                                </button>
                                            </form>
                                        </c:if>
                                    </div>
                                </td>
                            </tr>
                        </c:forEach>
                    </c:otherwise>
                </c:choose>
                </tbody>
            </table>
        </div>

        <%-- Phân trang --%>
        <c:url var="pageBase" value="/manager/showtimes">
            <c:if test="${not empty selectedDate}"><c:param name="date" value="${selectedDate}"/></c:if>
            <c:if test="${not empty selectedMovieId}"><c:param name="movieId" value="${selectedMovieId}"/></c:if>
            <c:if test="${not empty selectedRoomId}"><c:param name="roomId" value="${selectedRoomId}"/></c:if>
            <c:if test="${not empty selectedStatus}"><c:param name="status" value="${selectedStatus}"/></c:if>
        </c:url>
        <ui:pagination pageData="${showtimePage}" baseUrl="${pageBase}" label="suất chiếu"/>
    </div>
</div>
</main>

<%-- MODAL THÊM / SỬA SUẤT CHIẾU VỚI THUẬT TOÁN TÍNH GIỜ & ĐỊNH GIÁ TRỰC TIẾP --%>
<c:if test="${showForm}">
    <div class="fixed inset-0 bg-black/80 backdrop-blur-md z-50 flex items-center justify-center p-4 overflow-y-auto">
        <div class="bg-surface-container-low border border-white/10 rounded-2xl w-full max-w-2xl p-space-xl shadow-2xl relative my-8">
            <a class="absolute right-5 top-5 text-on-surface-variant hover:text-on-surface transition-colors"
               href="${ctx}/manager/showtimes${listQuery}" title="Đóng">
                <span class="material-symbols-outlined text-[24px]">close</span>
            </a>

            <div class="flex items-center gap-2 mb-space-md">
                <span class="material-symbols-outlined text-primary text-[28px]">schedule</span>
                <h2 class="text-headline-md text-on-surface font-bold">
                    ${empty form.showtimeId or form.showtimeId == 0 ? 'Lên lịch suất chiếu mới' : 'Chỉnh sửa suất chiếu #ST-'.concat(form.showtimeId)}
                </h2>
            </div>

            <%-- Cảnh báo nếu suất chiếu đã bán vé --%>
            <c:if test="${not empty editingShowtime and editingShowtime.bookedTickets > 0}">
                <div class="mb-space-md p-space-md rounded-xl bg-amber-500/15 border border-amber-500/30 text-amber-300 flex items-start gap-2.5 text-body-sm">
                    <span class="material-symbols-outlined text-[20px] flex-shrink-0">warning</span>
                    <span>
                        Suất chiếu này đã phát sinh <strong>${editingShowtime.bookedTickets} vé đặt</strong>. Theo quy định nghiệp vụ, thông tin phim, phòng chiếu và giờ chiếu không thể thay đổi nhằm bảo vệ vé đã xuất.
                    </span>
                </div>
            </c:if>

            <%-- Banner lỗi tổng quan hoặc lỗi trùng phòng chiếu --%>
            <c:if test="${not empty errors.general}">
                <div class="mb-space-md p-space-md rounded-xl bg-rose-500/15 border border-rose-500/30 text-rose-300 flex items-start gap-2.5 text-body-sm">
                    <span class="material-symbols-outlined text-[20px] flex-shrink-0">error</span>
                    <span><c:out value="${errors.general}"/></span>
                </div>
            </c:if>
            <c:if test="${not empty errors.collision}">
                <div class="mb-space-md p-space-md rounded-xl bg-rose-500/20 border-2 border-rose-500/60 text-rose-200 flex items-start gap-2.5 text-body-md shadow-lg shadow-rose-950/40">
                    <span class="material-symbols-outlined text-[24px] text-rose-400 flex-shrink-0">event_busy</span>
                    <div>
                        <strong class="font-bold block text-rose-100">Xung đột trùng phòng chiếu (GB-08):</strong>
                        <span><c:out value="${errors.collision}"/></span>
                    </div>
                </div>
            </c:if>

            <form method="post" action="${ctx}/manager/showtimes${listQuery}" class="space-y-space-md">
                <input type="hidden" name="csrfToken" value="${csrfToken}"/>
                <input type="hidden" name="showtimeId" value="${form.showtimeId}"/>

                <div class="grid grid-cols-1 md:grid-cols-2 gap-space-md">
                    <%-- Chọn Phim --%>
                    <div>
                        <label class="block text-label-md text-on-surface-variant mb-1 font-medium" for="modalMovieId">
                            Phim chiếu <span class="text-primary">*</span>
                        </label>
                        <select id="modalMovieId" name="movieId" class="${inputClass}" required
                                ${not empty editingShowtime and editingShowtime.bookedTickets > 0 ? 'disabled' : ''}>
                            <option value="">-- Chọn phim --</option>
                            <c:forEach var="movie" items="${movies}">
                                <option value="${movie.movieId}"
                                        data-duration="${movie.durationMinutes}"
                                        data-title="<c:out value='${movie.title}'/>"
                                        ${form.movieId == movie.movieId ? 'selected' : ''}>
                                    <c:out value="${movie.title}"/> (${movie.durationMinutes} phút)
                                </option>
                            </c:forEach>
                        </select>
                        <c:if test="${not empty editingShowtime and editingShowtime.bookedTickets > 0}">
                            <input type="hidden" name="movieId" value="${form.movieId}"/>
                        </c:if>
                        <c:if test="${not empty errors.movieId}">
                            <p class="text-rose-400 text-label-sm mt-1"><c:out value="${errors.movieId}"/></p>
                        </c:if>
                    </div>

                    <%-- Chọn Phòng Chiếu --%>
                    <div>
                        <label class="block text-label-md text-on-surface-variant mb-1 font-medium" for="modalRoomId">
                            Phòng chiếu <span class="text-primary">*</span>
                        </label>
                        <select id="modalRoomId" name="roomId" class="${inputClass}" required
                                ${not empty editingShowtime and editingShowtime.bookedTickets > 0 ? 'disabled' : ''}>
                            <option value="">-- Chọn phòng chiếu --</option>
                            <c:forEach var="room" items="${rooms}">
                                <c:choose>
                                    <c:when test="${room.status == 'Active'}">
                                        <option value="${room.roomId}"
                                                data-format="${empty room.format ? '2D' : room.format}"
                                                data-name="<c:out value='${room.roomName}'/>"
                                                ${form.roomId == room.roomId ? 'selected' : ''}>
                                            <c:out value="${room.roomName}"/> (${empty room.format ? '2D' : room.format} - ${room.totalSeats} ghế)
                                        </option>
                                    </c:when>
                                    <c:when test="${not empty editingShowtime and form.roomId == room.roomId}">
                                        <option value="${room.roomId}" disabled
                                                data-format="${empty room.format ? '2D' : room.format}"
                                                data-name="<c:out value='${room.roomName}'/>"
                                                selected>
                                            <c:out value="${room.roomName}"/> (${room.status == 'Maintenance' ? 'Đang bảo trì' : 'Đã dừng hoạt động'})
                                        </option>
                                    </c:when>
                                </c:choose>
                            </c:forEach>
                        </select>
                        <c:if test="${not empty editingShowtime and editingShowtime.bookedTickets > 0}">
                            <input type="hidden" name="roomId" value="${form.roomId}"/>
                        </c:if>
                        <c:if test="${not empty errors.roomId}">
                            <p class="text-rose-400 text-label-sm mt-1"><c:out value="${errors.roomId}"/></p>
                        </c:if>
                    </div>
                </div>

                <%-- Chọn Giờ Chiếu --%>
                <div>
                    <label class="block text-label-md text-on-surface-variant mb-1 font-medium" for="modalStartTime">
                        Thời gian bắt đầu chiếu <span class="text-primary">*</span>
                    </label>
                    <input id="modalStartTime" name="startTime" class="${inputClass}"
                           type="datetime-local" required value="<c:out value='${form.startTime}'/>"
                           ${not empty editingShowtime and editingShowtime.bookedTickets > 0 ? 'readonly' : ''}/>
                    <c:if test="${not empty errors.startTime}">
                        <p class="text-rose-400 text-label-sm mt-1"><c:out value="${errors.startTime}"/></p>
                    </c:if>
                </div>

                <%-- KHUNG TÍNH TOÁN THUẬT TOÁN THỜI GIAN KẾT THÚC VÀ ĐỊNH GIÁ (PREVIEW TỨC THÌ) --%>
                <div class="p-4 rounded-xl bg-surface-container border border-white/10 space-y-3">
                    <div class="flex items-center justify-between text-body-sm pb-2 border-b border-white/5">
                        <div class="flex items-center gap-1.5 text-on-surface-variant">
                            <span class="material-symbols-outlined text-[18px] text-primary">timer</span>
                            <span>Dự kiến kết thúc (<strong class="text-primary">+15p dọn rạp</strong>):</span>
                        </div>
                        <span id="previewEndTime" class="font-bold text-on-surface font-mono text-[14px]">
                            Vui lòng chọn phim &amp; giờ bắt đầu
                        </span>
                    </div>

                    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-2 text-body-sm">
                        <div>
                            <div class="flex items-center gap-1.5 text-on-surface-variant">
                                <span class="material-symbols-outlined text-[18px] text-amber-400">payments</span>
                                <span>Giá vé gợi ý thuật toán:</span>
                            </div>
                            <p id="previewPricingDetail" class="text-outline text-label-sm mt-0.5">
                                Chưa đủ dữ liệu tính giá
                            </p>
                        </div>
                        <div class="flex items-center gap-2 self-start sm:self-auto">
                            <span id="previewPriceValue" class="text-headline-sm font-bold text-emerald-400 font-mono">--</span>
                            <button type="button" id="btnApplyPrice"
                                    class="px-2.5 py-1 rounded bg-emerald-500/20 hover:bg-emerald-500/30 text-emerald-300 text-label-sm border border-emerald-500/30 transition-colors">
                                Áp dụng
                            </button>
                        </div>
                    </div>
                </div>

                <div class="grid grid-cols-1 md:grid-cols-2 gap-space-md">
                    <%-- Giá vé gốc --%>
                    <div>
                        <label class="block text-label-md text-on-surface-variant mb-1 font-medium" for="modalBasePrice">
                            Giá vé gốc (VND) <span class="text-primary">*</span>
                        </label>
                        <input id="modalBasePrice" name="basePrice" class="${inputClass} font-mono"
                               type="number" min="10000" step="1000" required
                               placeholder="VD: 85000"
                               value="<c:out value='${form.basePrice}'/>"/>
                        <p class="text-outline text-label-sm mt-1">Tối thiểu 10.000đ và là bội số của 1.000đ.</p>
                        <c:if test="${not empty errors.basePrice}">
                            <p class="text-rose-400 text-label-sm mt-1"><c:out value="${errors.basePrice}"/></p>
                        </c:if>
                    </div>

                    <%-- Trạng thái --%>
                    <div>
                        <label class="block text-label-md text-on-surface-variant mb-1 font-medium" for="modalStatus">
                            Trạng thái suất chiếu
                        </label>
                        <select id="modalStatus" name="status" class="${inputClass}">
                            <option value="Active" ${form.status == 'Active' ? 'selected' : ''}>Đang mở bán (Active)</option>
                            <option value="Ended" ${form.status == 'Ended' ? 'selected' : ''}>Đã kết thúc (Ended)</option>
                            <option value="Cancelled" ${form.status == 'Cancelled' ? 'selected' : ''}>Đã hủy (Cancelled)</option>
                        </select>
                    </div>
                </div>

                <div class="flex justify-end gap-space-sm pt-space-md border-t border-white/5">
                    <a class="px-space-lg py-2 rounded-full bg-surface-container-high hover:bg-surface-bright text-on-surface font-label-md transition-colors"
                       href="${ctx}/manager/showtimes${listQuery}">
                        Hủy
                    </a>
                    <button class="px-space-xl py-2 rounded-full bg-primary hover:bg-primary/90 text-on-primary font-label-md shadow-lg shadow-primary/25 transition-all"
                            type="submit">
                        ${empty form.showtimeId or form.showtimeId == 0 ? 'Tạo suất chiếu' : 'Lưu thay đổi'}
                    </button>
                </div>
            </form>
        </div>
    </div>
</c:if>

<%-- SCRIPT TÍNH TOÁN GIỜ KẾT THÚC & ĐỊNH GIÁ TRỰC TIẾP TRÊN FORM --%>
<script>
document.addEventListener('DOMContentLoaded', function () {
    // Tự động ẩn toast sau 5 giây
    var toast = document.getElementById('toast');
    if (toast) {
        setTimeout(function () {
            toast.style.opacity = '0';
            toast.style.transform = 'translateY(-10px)';
            setTimeout(function () {
                if (toast.parentNode) {
                    toast.parentNode.removeChild(toast);
                }
            }, 300);
        }, 5000);
    }

    var movieSelect = document.getElementById('modalMovieId');
    var roomSelect = document.getElementById('modalRoomId');
    var startTimeInput = document.getElementById('modalStartTime');
    var basePriceInput = document.getElementById('modalBasePrice');
    var previewEndTime = document.getElementById('previewEndTime');
    var previewPricingDetail = document.getElementById('previewPricingDetail');
    var previewPriceValue = document.getElementById('previewPriceValue');
    var btnApplyPrice = document.getElementById('btnApplyPrice');

    if (!movieSelect || !roomSelect || !startTimeInput) {
        return;
    }

    var currentSuggestedPrice = 0;

    function recalculate() {
        var startVal = startTimeInput.value;
        var movieOpt = movieSelect.options[movieSelect.selectedIndex];
        var roomOpt = roomSelect.options[roomSelect.selectedIndex];

        // 1. Tính giờ kết thúc (GB-08: Thời lượng + 15p dọn rạp)
        if (startVal && movieOpt && movieOpt.dataset.duration) {
            var duration = parseInt(movieOpt.dataset.duration, 10);
            var startDate = new Date(startVal);
            if (!isNaN(startDate.getTime()) && duration > 0) {
                var totalMinutes = duration + 15;
                var endDate = new Date(startDate.getTime() + totalMinutes * 60000);
                var pad = function(n) { return n < 10 ? '0' + n : n; };
                var hours = pad(endDate.getHours());
                var mins = pad(endDate.getMinutes());
                var day = pad(endDate.getDate());
                var month = pad(endDate.getMonth() + 1);
                var year = endDate.getFullYear();
                previewEndTime.textContent = hours + ':' + mins + ' (' + day + '/' + month + '/' + year + ') [' + duration + 'p + 15p dọn rạp]';
                previewEndTime.className = 'font-bold text-emerald-400 font-mono text-[14px]';
            } else {
                previewEndTime.textContent = 'Vui lòng chọn phim & giờ bắt đầu hợp lệ';
                previewEndTime.className = 'font-bold text-on-surface-variant font-mono text-[14px]';
            }
        } else {
            previewEndTime.textContent = 'Vui lòng chọn phim & giờ bắt đầu';
            previewEndTime.className = 'font-bold text-on-surface-variant font-mono text-[14px]';
        }

        // 2. Định giá vé theo thuật toán (ShowtimePricing)
        if (startVal && roomOpt) {
            var sDate = new Date(startVal);
            if (!isNaN(sDate.getTime())) {
                var dayOfWeek = sDate.getDay(); // 0 = Sunday, 5 = Friday, 6 = Saturday
                var isWeekend = (dayOfWeek === 0 || dayOfWeek === 5 || dayOfWeek === 6);
                var hour = sDate.getHours();

                var slotName = '';
                var base = 0;
                if (hour < 12) {
                    slotName = 'Suất sáng (< 12h)';
                    base = isWeekend ? 80000 : 65000;
                } else if (hour < 17) {
                    slotName = 'Suất chiều (12h - 17h)';
                    base = isWeekend ? 95000 : 75000;
                } else if (hour < 22) {
                    slotName = 'Giờ vàng tối (17h - 22h)';
                    base = isWeekend ? 110000 : 85000;
                } else {
                    slotName = 'Suất khuya (≥ 22h)';
                    base = isWeekend ? 90000 : 75000;
                }

                var format = roomOpt.dataset.format || '2D';
                var surcharge = 0;
                if (format === '3D') {
                    surcharge = 20000;
                } else if (format === 'IMAX') {
                    surcharge = 50000;
                }

                currentSuggestedPrice = base + surcharge;
                var formatVnd = function(num) {
                    return num.toLocaleString('vi-VN') + 'đ';
                };

                var detailText = (isWeekend ? 'Cuối tuần' : 'Ngày thường') + ' · ' + slotName + ' (' + formatVnd(base) + ')';
                if (surcharge > 0) {
                    detailText += ' · Phụ thu ' + format + ' (+' + formatVnd(surcharge) + ')';
                }
                previewPricingDetail.textContent = detailText;
                previewPriceValue.textContent = formatVnd(currentSuggestedPrice);

                // Nếu ô giá vé gốc đang trống, tự động điền luôn giá gợi ý
                if (!basePriceInput.value) {
                    basePriceInput.value = currentSuggestedPrice;
                }
            } else {
                resetPricing();
            }
        } else {
            resetPricing();
        }
    }

    function resetPricing() {
        currentSuggestedPrice = 0;
        previewPricingDetail.textContent = 'Chọn giờ bắt đầu & phòng để tính giá';
        previewPriceValue.textContent = '--';
    }

    movieSelect.addEventListener('change', recalculate);
    roomSelect.addEventListener('change', recalculate);
    startTimeInput.addEventListener('input', recalculate);
    startTimeInput.addEventListener('change', recalculate);

    if (btnApplyPrice) {
        btnApplyPrice.addEventListener('click', function () {
            if (currentSuggestedPrice > 0) {
                basePriceInput.value = currentSuggestedPrice;
            }
        });
    }

    // Chạy tính toán ban đầu nếu form đã có sẵn dữ liệu (khi sửa hoặc sau validate lỗi)
    recalculate();
});
</script>
</body>
</html>
