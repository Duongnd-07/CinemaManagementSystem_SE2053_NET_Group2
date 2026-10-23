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
                Hỗ trợ <strong class="text-primary font-medium">lên lịch hàng loạt theo tuần</strong>, chống trùng phòng (<span class="text-primary font-medium">+15 phút dọn rạp</span>) và định giá thông minh theo ngày/khung giờ.
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
                <span class="text-on-surface-variant text-body-sm font-medium">Đã kết thúc (Tự động)</span>
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

        <div class="flex-1 min-w-[150px]">
            <label class="block text-label-sm text-on-surface-variant mb-1 font-medium">Bản chiếu</label>
            <select name="translationType" class="w-full bg-surface-container-high text-on-surface px-4 py-2 rounded-lg border border-white/10 focus:border-primary outline-none text-body-md">
                <option value="">-- Tất cả bản chiếu --</option>
                <option value="Phụ đề" ${selectedTranslationType == 'Phụ đề' ? 'selected' : ''}>Phụ đề</option>
                <option value="Lồng tiếng" ${selectedTranslationType == 'Lồng tiếng' ? 'selected' : ''}>Lồng tiếng</option>
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
                                                <c:if test="${not empty showtime.movie.formats}">
                                                    <span class="text-outline">· <c:out value="${showtime.movie.formats}"/></span>
                                                </c:if>
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
                                        <span class="px-2 py-0.5 rounded text-[11px] font-bold ${showtime.translationType == 'Lồng tiếng' ? 'bg-amber-500/20 text-amber-300 border border-amber-500/30' : 'bg-sky-500/20 text-sky-300 border border-sky-500/30'}">
                                            <c:out value="${empty showtime.translationType ? 'Phụ đề' : showtime.translationType}"/>
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

                                        <%-- Nút Hủy: Chặn hủy nếu suất đã bán vé để bảo vệ quyền lợi khách hàng (Góp ý 2) --%>
                                        <c:if test="${showtime.status == 'Active'}">
                                            <c:choose>
                                                <c:when test="${showtime.bookedTickets > 0}">
                                                    <button type="button"
                                                            onclick="alert('Suất chiếu này đã bán ${showtime.bookedTickets} vé cho khách hàng. Theo quy định bảo vệ khách hàng, bạn không thể hủy suất chiếu này.');"
                                                            class="inline-flex items-center gap-1 px-2.5 py-1.5 rounded-lg bg-white/5 text-outline cursor-not-allowed transition-all font-label-md opacity-60"
                                                            title="Đã có vé bán, không thể hủy">
                                                        <span class="material-symbols-outlined text-[16px]">block</span>
                                                    </button>
                                                </c:when>
                                                <c:otherwise>
                                                    <form method="post" action="${ctx}/manager/showtimes${listQuery}" class="inline"
                                                          onsubmit="return confirm('Bạn có chắc chắn muốn HỦY suất chiếu này không? Khách hàng sẽ không thể đặt vé thêm.');">
                                                        <input type="hidden" name="csrfToken" value="${csrfToken}"/>
                                                        <input type="hidden" name="action" value="cancel"/>
                                                        <input type="hidden" name="showtimeId" value="${showtime.showtimeId}"/>
                                                        <button type="submit" class="inline-flex items-center gap-1 px-2.5 py-1.5 rounded-lg bg-amber-500/10 hover:bg-amber-500/20 text-amber-400 transition-all font-label-md" title="Hủy suất chiếu">
                                                            <span class="material-symbols-outlined text-[16px]">block</span>
                                                        </button>
                                                    </form>
                                                </c:otherwise>
                                            </c:choose>
                                        </c:if>

                                        <%-- Nút Xóa: Chỉ hiện khi chưa có vé bán và chưa có giữ chỗ (Góp ý 3) --%>
                                        <c:if test="${showtime.bookedTickets == 0}">
                                            <form method="post" action="${ctx}/manager/showtimes${listQuery}" class="inline"
                                                  onsubmit="return confirm('Bạn có chắc muốn XÓA vĩnh viễn suất chiếu này khỏi hệ thống? (Lưu ý: Chỉ xóa được khi chưa phát sinh bất kỳ vé hoặc giao dịch nào)');">
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

<%-- MODAL LÊN LỊCH SUẤT CHIẾU (HỖ TRỢ HÀNG LOẠT VÀ TẠO LẺ) --%>
<c:if test="${showForm}">
    <div class="fixed inset-0 bg-black/80 backdrop-blur-md z-50 flex items-center justify-center p-4 overflow-y-auto">
        <div class="bg-surface-container-low border border-white/10 rounded-2xl w-full max-w-3xl p-space-xl shadow-2xl relative my-8">
            <a class="absolute right-5 top-5 text-on-surface-variant hover:text-on-surface transition-colors"
               href="${ctx}/manager/showtimes${listQuery}" title="Đóng">
                <span class="material-symbols-outlined text-[24px]">close</span>
            </a>

            <div class="flex items-center gap-2 mb-space-md">
                <span class="material-symbols-outlined text-primary text-[28px]">schedule</span>
                <h2 class="text-headline-md text-on-surface font-bold">
                    ${empty form.showtimeId or form.showtimeId == 0 ? 'Lên lịch suất chiếu' : 'Chỉnh sửa suất chiếu #ST-'.concat(form.showtimeId)}
                </h2>
            </div>

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

            <c:choose>
                <%-- ======================================================= --%>
                <%-- FORM CHỈNH SỬA SUẤT CHIẾU HIỆN CÓ                      --%>
                <%-- ======================================================= --%>
                <c:when test="${not empty form.showtimeId and form.showtimeId > 0}">
                    <c:if test="${not empty editingShowtime and editingShowtime.bookedTickets > 0}">
                        <div class="mb-space-md p-space-md rounded-xl bg-amber-500/15 border border-amber-500/30 text-amber-300 flex items-start gap-2.5 text-body-sm">
                            <span class="material-symbols-outlined text-[20px] flex-shrink-0">warning</span>
                            <span>
                                Suất chiếu này đã phát sinh <strong>${editingShowtime.bookedTickets} vé đặt</strong>. Theo quy định bảo vệ khách hàng, thông tin phim, phòng chiếu và giờ bắt đầu không thể thay đổi.
                            </span>
                        </div>
                    </c:if>

                    <form method="post" action="${ctx}/manager/showtimes${listQuery}" class="space-y-space-md">
                        <input type="hidden" name="csrfToken" value="${csrfToken}"/>
                        <input type="hidden" name="showtimeId" value="${form.showtimeId}"/>

                        <div class="grid grid-cols-1 md:grid-cols-2 gap-space-md">
                            <div>
                                <label class="block text-label-md text-on-surface-variant mb-1 font-medium" for="modalMovieId">Phim chiếu</label>
                                <select id="modalMovieId" name="movieId" class="${inputClass}" required
                                        ${not empty editingShowtime and editingShowtime.bookedTickets > 0 ? 'disabled' : ''}>
                                    <c:forEach var="movie" items="${movies}">
                                        <option value="${movie.movieId}"
                                                data-duration="${movie.durationMinutes}"
                                                data-formats="<c:out value='${movie.formats}'/>"
                                                ${form.movieId == movie.movieId ? 'selected' : ''}>
                                            <c:out value="${movie.title}"/> (${movie.durationMinutes}p)
                                        </option>
                                    </c:forEach>
                                </select>
                                <c:if test="${not empty editingShowtime and editingShowtime.bookedTickets > 0}">
                                    <input type="hidden" name="movieId" value="${form.movieId}"/>
                                </c:if>
                            </div>

                            <div>
                                <label class="block text-label-md text-on-surface-variant mb-1 font-medium" for="modalRoomId">Phòng chiếu</label>
                                <select id="modalRoomId" name="roomId" class="${inputClass}" required
                                        ${not empty editingShowtime and editingShowtime.bookedTickets > 0 ? 'disabled' : ''}>
                                    <c:forEach var="room" items="${rooms}">
                                        <c:choose>
                                            <c:when test="${room.status == 'Active'}">
                                                <option value="${room.roomId}"
                                                        data-format="${empty room.format ? '2D' : room.format}"
                                                        ${form.roomId == room.roomId ? 'selected' : ''}>
                                                    <c:out value="${room.roomName}"/> (${empty room.format ? '2D' : room.format} - ${room.totalSeats} ghế)
                                                </option>
                                            </c:when>
                                            <c:when test="${form.roomId == room.roomId}">
                                                <option value="${room.roomId}" disabled selected>
                                                    <c:out value="${room.roomName}"/> (${room.status == 'Maintenance' ? 'Đang bảo trì' : 'Đã dừng hoạt động'})
                                                </option>
                                            </c:when>
                                        </c:choose>
                                    </c:forEach>
                                </select>
                                <c:if test="${not empty editingShowtime and editingShowtime.bookedTickets > 0}">
                                    <input type="hidden" name="roomId" value="${form.roomId}"/>
                                </c:if>
                            </div>
                        </div>

                        <div>
                            <label class="block text-label-md text-on-surface-variant mb-1.5 font-medium">Hình thức bản chiếu <span class="text-primary">*</span></label>
                            <div class="grid grid-cols-2 gap-3">
                                <label class="flex items-center gap-2.5 p-3 rounded-xl border border-white/10 bg-surface-container-high cursor-pointer hover:border-primary/50 transition">
                                    <input type="radio" name="translationType" value="Phụ đề" class="accent-primary" ${empty form.translationType or form.translationType == 'Phụ đề' ? 'checked' : ''}/>
                                    <div>
                                        <div class="text-body-sm font-bold text-on-surface">Phụ đề</div>
                                        <div class="text-[11px] text-outline">Ngôn ngữ gốc + Phụ đề</div>
                                    </div>
                                </label>
                                <label class="flex items-center gap-2.5 p-3 rounded-xl border border-white/10 bg-surface-container-high cursor-pointer hover:border-primary/50 transition">
                                    <input type="radio" name="translationType" value="Lồng tiếng" class="accent-primary" ${form.translationType == 'Lồng tiếng' ? 'checked' : ''}/>
                                    <div>
                                        <div class="text-body-sm font-bold text-on-surface">Lồng tiếng</div>
                                        <div class="text-[11px] text-outline">Lồng tiếng Việt</div>
                                    </div>
                                </label>
                            </div>
                        </div>

                        <div>
                            <label class="block text-label-md text-on-surface-variant mb-1 font-medium" for="modalStartTime">Thời gian bắt đầu</label>
                            <input id="modalStartTime" name="startTime" class="${inputClass}"
                                   type="datetime-local" required value="<c:out value='${form.startTime}'/>"
                                   ${not empty editingShowtime and editingShowtime.bookedTickets > 0 ? 'readonly' : ''}/>
                            <c:if test="${not empty errors.startTime}">
                                <p class="text-rose-400 text-label-sm mt-1"><c:out value="${errors.startTime}"/></p>
                            </c:if>
                        </div>

                        <%-- Khung preview tính toán thời gian và giá vé --%>
                        <div class="p-4 rounded-xl bg-surface-container border border-white/10 space-y-2">
                            <div class="flex items-center justify-between text-body-sm">
                                <span class="text-on-surface-variant">Dự kiến kết thúc (+15p dọn rạp):</span>
                                <span id="previewEndTime" class="font-bold text-on-surface font-mono">--</span>
                            </div>
                            <div class="flex items-center justify-between text-body-sm pt-2 border-t border-white/5">
                                <span class="text-on-surface-variant">Giá vé gợi ý:</span>
                                <div class="flex items-center gap-2">
                                    <span id="previewPriceValue" class="font-bold text-emerald-400 font-mono">--</span>
                                    <button type="button" id="btnApplyPrice" class="px-2 py-0.5 rounded bg-emerald-500/20 text-emerald-300 text-label-sm">Áp dụng</button>
                                </div>
                            </div>
                        </div>

                        <div>
                            <label class="block text-label-md text-on-surface-variant mb-1 font-medium" for="modalBasePrice">Giá vé gốc (VND) <span class="text-primary">*</span></label>
                            <input id="modalBasePrice" name="basePrice" class="${inputClass} font-mono"
                                   type="number" min="10000" step="1000" required value="<c:out value='${form.basePrice}'/>"/>
                            <c:if test="${not empty errors.basePrice}">
                                <p class="text-rose-400 text-label-sm mt-1"><c:out value="${errors.basePrice}"/></p>
                            </c:if>
                        </div>

                        <div class="flex justify-end gap-space-sm pt-space-md border-t border-white/5">
                            <a class="px-space-lg py-2 rounded-full bg-surface-container-high hover:bg-surface-bright text-on-surface font-label-md"
                               href="${ctx}/manager/showtimes${listQuery}">Hủy</a>
                            <button class="px-space-xl py-2 rounded-full bg-primary hover:bg-primary/90 text-on-primary font-label-md shadow-lg"
                                    type="submit">Lưu thay đổi</button>
                        </div>
                    </form>
                </c:when>

                <%-- ======================================================= --%>
                <%-- TẠO MỚI SUẤT CHIẾU (HỖ TRỢ 2 TAB: HÀNG LOẠT VÀ TẠO LẺ)  --%>
                <%-- ======================================================= --%>
                <c:otherwise>
                    <%-- Tab Switcher --%>
                    <div class="flex items-center gap-2 p-1 bg-surface-container rounded-xl mb-space-lg border border-white/5">
                        <button type="button" id="tabBtnBatch" onclick="switchCreateTab('batch')"
                                class="flex-1 py-2 px-4 rounded-lg font-label-md transition-all flex items-center justify-center gap-2 bg-primary text-on-primary shadow-md">
                            <span class="material-symbols-outlined text-[18px]">bolt</span>
                            <span>Lên lịch hàng loạt (Khuyên dùng)</span>
                        </button>
                        <button type="button" id="tabBtnSingle" onclick="switchCreateTab('single')"
                                class="flex-1 py-2 px-4 rounded-lg font-label-md transition-all flex items-center justify-center gap-2 text-on-surface-variant hover:text-on-surface">
                            <span class="material-symbols-outlined text-[18px]">add</span>
                            <span>Tạo 1 suất đơn lẻ</span>
                        </button>
                    </div>

                    <%-- ---------------------------------------------------- --%>
                    <%-- TAB 1: FORM LÊN LỊCH HÀNG LOẠT                        --%>
                    <%-- ---------------------------------------------------- --%>
                    <div id="tabContentBatch">
                        <form method="post" action="${ctx}/manager/showtimes${listQuery}" class="space-y-space-md" id="batchForm">
                            <input type="hidden" name="csrfToken" value="${csrfToken}"/>
                            <input type="hidden" name="action" value="batch_save"/>

                            <div class="grid grid-cols-1 md:grid-cols-2 gap-space-md">
                                <div>
                                    <label class="block text-label-md text-on-surface-variant mb-1 font-medium" for="batchMovieId">
                                        Phim chiếu <span class="text-primary">*</span>
                                    </label>
                                    <select id="batchMovieId" name="batchMovieId" class="${inputClass}" required onchange="onBatchParamsChange()">
                                        <option value="">-- Chọn phim --</option>
                                        <c:forEach var="movie" items="${movies}">
                                            <option value="${movie.movieId}"
                                                    data-duration="${movie.durationMinutes}"
                                                    data-formats="<c:out value='${movie.formats}'/>"
                                                    data-release="${movie.releaseDate != null ? movie.releaseDate : ''}">
                                                <c:out value="${movie.title}"/> (${movie.durationMinutes}p)
                                            </option>
                                        </c:forEach>
                                    </select>
                                </div>

                                <div>
                                    <label class="block text-label-md text-on-surface-variant mb-1 font-medium" for="batchRoomId">
                                        Phòng chiếu <span class="text-primary">*</span>
                                    </label>
                                    <select id="batchRoomId" name="batchRoomId" class="${inputClass}" required onchange="onBatchParamsChange()">
                                        <option value="">-- Chọn phòng chiếu --</option>
                                        <c:forEach var="room" items="${rooms}">
                                            <c:if test="${room.status == 'Active'}">
                                                <option value="${room.roomId}"
                                                        data-format="${empty room.format ? '2D' : room.format}">
                                                    <c:out value="${room.roomName}"/> (${empty room.format ? '2D' : room.format} - ${room.totalSeats} ghế)
                                                </option>
                                            </c:if>
                                        </c:forEach>
                                    </select>
                                </div>
                            </div>

                            <%-- Cảnh báo format không khớp --%>
                            <div id="batchFormatWarning" class="hidden p-3 rounded-xl bg-rose-500/15 border border-rose-500/30 text-rose-300 text-body-sm flex items-center gap-2">
                                <span class="material-symbols-outlined text-[18px]">warning</span>
                                <span id="batchFormatWarningText"></span>
                            </div>

                            <%-- Hình thức bản chiếu --%>
                            <div>
                                <label class="block text-label-md text-on-surface-variant mb-1.5 font-medium">Hình thức bản chiếu <span class="text-primary">*</span></label>
                                <div class="grid grid-cols-2 gap-3">
                                    <label class="flex items-center gap-2.5 p-3 rounded-xl border border-white/10 bg-surface-container-high cursor-pointer hover:border-primary/50 transition">
                                        <input type="radio" name="batchTranslationType" value="Phụ đề" checked class="accent-primary"/>
                                        <div>
                                            <div class="text-body-sm font-bold text-on-surface">Phụ đề</div>
                                            <div class="text-[11px] text-outline">Ngôn ngữ gốc + Phụ đề</div>
                                        </div>
                                    </label>
                                    <label class="flex items-center gap-2.5 p-3 rounded-xl border border-white/10 bg-surface-container-high cursor-pointer hover:border-primary/50 transition">
                                        <input type="radio" name="batchTranslationType" value="Lồng tiếng" class="accent-primary"/>
                                        <div>
                                            <div class="text-body-sm font-bold text-on-surface">Lồng tiếng</div>
                                            <div class="text-[11px] text-outline">Lồng tiếng Việt</div>
                                        </div>
                                    </label>
                                </div>
                            </div>

                            <%-- Khoảng ngày chiếu --%>
                            <div class="grid grid-cols-1 md:grid-cols-2 gap-space-md">
                                <div>
                                    <label class="block text-label-md text-on-surface-variant mb-1 font-medium" for="startDate">
                                        Từ ngày <span class="text-primary">*</span>
                                    </label>
                                    <input type="date" id="startDate" name="startDate" class="${inputClass}" required onchange="onBatchParamsChange()"/>
                                </div>
                                <div>
                                    <label class="block text-label-md text-on-surface-variant mb-1 font-medium" for="endDate">
                                        Đến ngày <span class="text-primary">*</span>
                                    </label>
                                    <input type="date" id="endDate" name="endDate" class="${inputClass}" required onchange="onBatchParamsChange()"/>
                                </div>
                            </div>

                            <%-- Chọn thứ trong tuần --%>
                            <div>
                                <div class="flex items-center justify-between mb-2">
                                    <label class="text-label-md text-on-surface-variant font-medium">Áp dụng vào các thứ trong tuần <span class="text-primary">*</span></label>
                                    <div class="flex items-center gap-2 text-label-sm">
                                        <button type="button" onclick="selectDays('all')" class="text-primary hover:underline">Cả tuần</button>
                                        <span class="text-outline">·</span>
                                        <button type="button" onclick="selectDays('weekday')" class="text-primary hover:underline">Ngày thường (T2-T5)</button>
                                        <span class="text-outline">·</span>
                                        <button type="button" onclick="selectDays('weekend')" class="text-primary hover:underline">Cuối tuần (T6-CN)</button>
                                    </div>
                                </div>
                                <div class="grid grid-cols-7 gap-2">
                                    <label class="flex flex-col items-center justify-center p-2 rounded-lg bg-surface-container-high border border-white/5 cursor-pointer hover:border-primary/40 has-[:checked]:border-primary has-[:checked]:bg-primary/10">
                                        <input type="checkbox" name="daysOfWeek" value="1" checked class="accent-primary" onchange="onBatchParamsChange()"/>
                                        <span class="text-label-sm font-medium mt-1">Thứ 2</span>
                                    </label>
                                    <label class="flex flex-col items-center justify-center p-2 rounded-lg bg-surface-container-high border border-white/5 cursor-pointer hover:border-primary/40 has-[:checked]:border-primary has-[:checked]:bg-primary/10">
                                        <input type="checkbox" name="daysOfWeek" value="2" checked class="accent-primary" onchange="onBatchParamsChange()"/>
                                        <span class="text-label-sm font-medium mt-1">Thứ 3</span>
                                    </label>
                                    <label class="flex flex-col items-center justify-center p-2 rounded-lg bg-surface-container-high border border-white/5 cursor-pointer hover:border-primary/40 has-[:checked]:border-primary has-[:checked]:bg-primary/10">
                                        <input type="checkbox" name="daysOfWeek" value="3" checked class="accent-primary" onchange="onBatchParamsChange()"/>
                                        <span class="text-label-sm font-medium mt-1">Thứ 4</span>
                                    </label>
                                    <label class="flex flex-col items-center justify-center p-2 rounded-lg bg-surface-container-high border border-white/5 cursor-pointer hover:border-primary/40 has-[:checked]:border-primary has-[:checked]:bg-primary/10">
                                        <input type="checkbox" name="daysOfWeek" value="4" checked class="accent-primary" onchange="onBatchParamsChange()"/>
                                        <span class="text-label-sm font-medium mt-1">Thứ 5</span>
                                    </label>
                                    <label class="flex flex-col items-center justify-center p-2 rounded-lg bg-surface-container-high border border-white/5 cursor-pointer hover:border-primary/40 has-[:checked]:border-primary has-[:checked]:bg-primary/10">
                                        <input type="checkbox" name="daysOfWeek" value="5" checked class="accent-primary" onchange="onBatchParamsChange()"/>
                                        <span class="text-label-sm font-medium mt-1 text-emerald-400">Thứ 6</span>
                                    </label>
                                    <label class="flex flex-col items-center justify-center p-2 rounded-lg bg-surface-container-high border border-white/5 cursor-pointer hover:border-primary/40 has-[:checked]:border-primary has-[:checked]:bg-primary/10">
                                        <input type="checkbox" name="daysOfWeek" value="6" checked class="accent-primary" onchange="onBatchParamsChange()"/>
                                        <span class="text-label-sm font-medium mt-1 text-emerald-400">Thứ 7</span>
                                    </label>
                                    <label class="flex flex-col items-center justify-center p-2 rounded-lg bg-surface-container-high border border-white/5 cursor-pointer hover:border-primary/40 has-[:checked]:border-primary has-[:checked]:bg-primary/10">
                                        <input type="checkbox" name="daysOfWeek" value="7" checked class="accent-primary" onchange="onBatchParamsChange()"/>
                                        <span class="text-label-sm font-medium mt-1 text-emerald-400">Chủ Nhật</span>
                                    </label>
                                </div>
                            </div>

                            <%-- Khung giờ chiếu trong ngày --%>
                            <div>
                                <label class="block text-label-md text-on-surface-variant mb-1 font-medium">Khung giờ chiếu trong ngày <span class="text-primary">*</span></label>
                                <div class="flex flex-wrap items-center gap-2 mb-2">
                                    <button type="button" onclick="addTimeSlot('09:30')" class="px-2.5 py-1 rounded bg-surface-container-high hover:bg-surface-bright text-label-sm">+ 09:30 (Sáng)</button>
                                    <button type="button" onclick="addTimeSlot('13:30')" class="px-2.5 py-1 rounded bg-surface-container-high hover:bg-surface-bright text-label-sm">+ 13:30 (Chiều)</button>
                                    <button type="button" onclick="addTimeSlot('16:30')" class="px-2.5 py-1 rounded bg-surface-container-high hover:bg-surface-bright text-label-sm">+ 16:30 (Chiều)</button>
                                    <button type="button" onclick="addTimeSlot('19:30')" class="px-2.5 py-1 rounded bg-surface-container-high hover:bg-surface-bright text-label-sm">+ 19:30 (Giờ vàng)</button>
                                    <button type="button" onclick="addTimeSlot('22:00')" class="px-2.5 py-1 rounded bg-surface-container-high hover:bg-surface-bright text-label-sm">+ 22:00 (Khuya)</button>
                                </div>
                                <div class="flex items-center gap-2">
                                    <input type="time" id="customTimeInput" class="${inputClass} max-w-[160px]"/>
                                    <button type="button" onclick="addCustomTimeSlot()" class="px-4 py-2.5 rounded-lg bg-surface-container-high hover:bg-surface-bright text-label-md font-medium">Thêm giờ</button>
                                </div>
                                <%-- Container chứa các tag giờ được chọn --%>
                                <div id="selectedSlotsContainer" class="flex flex-wrap gap-2 mt-2 min-h-[38px] p-2 rounded-lg bg-surface-container border border-white/5">
                                    <span class="text-outline text-label-sm self-center">Chưa chọn giờ chiếu nào...</span>
                                </div>
                            </div>

                            <%-- Giá vé cố định (tùy chọn) & Bỏ qua trùng lặp --%>
                            <div class="grid grid-cols-1 md:grid-cols-2 gap-space-md pt-2 border-t border-white/5">
                                <div>
                                    <label class="block text-label-md text-on-surface-variant mb-1 font-medium" for="batchBasePrice">
                                        Giá vé áp dụng (Tùy chọn)
                                    </label>
                                    <input type="number" id="batchBasePrice" name="batchBasePrice" class="${inputClass} font-mono" min="10000" step="1000"
                                           placeholder="Để trống = Tự tính theo giá gợi ý từng suất" oninput="onBatchParamsChange()"/>
                                    <p class="text-outline text-label-sm mt-1">Để trống để hệ thống tự định giá theo Ngày thường / Cuối tuần / Khung giờ.</p>
                                </div>
                                <div class="flex flex-col justify-center">
                                    <label class="flex items-start gap-2.5 cursor-pointer mt-2">
                                        <input type="checkbox" name="skipCollisions" value="1" checked class="accent-primary mt-1"/>
                                        <span class="text-body-sm text-on-surface">
                                            <strong>Tự động bỏ qua suất bị trùng phòng:</strong> Nếu có suất bị trùng lịch đã có trước đó, chỉ tạo các suất hợp lệ và không báo lỗi hủy cả đợt.
                                        </span>
                                    </label>
                                </div>
                            </div>

                            <%-- Bảng xem trước thống kê số suất chiếu --%>
                            <div id="batchPreviewBox" class="p-4 rounded-xl bg-surface-container border border-white/10 space-y-2">
                                <div class="flex items-center justify-between">
                                    <span class="text-body-sm font-medium text-on-surface-variant flex items-center gap-1.5">
                                        <span class="material-symbols-outlined text-[18px] text-primary">calendar_month</span>
                                        <span>Dự kiến sinh ra:</span>
                                    </span>
                                    <span id="batchCountBadge" class="font-bold text-headline-sm text-primary font-mono">0 suất chiếu</span>
                                </div>
                                <p id="batchDetailText" class="text-outline text-body-sm">Vui lòng chọn phim, phòng, khoảng ngày và ít nhất một khung giờ.</p>
                            </div>

                            <div class="flex justify-end gap-space-sm pt-space-md border-t border-white/5">
                                <a class="px-space-lg py-2 rounded-full bg-surface-container-high hover:bg-surface-bright text-on-surface font-label-md"
                                   href="${ctx}/manager/showtimes${listQuery}">Hủy</a>
                                <button class="px-space-xl py-2.5 rounded-full bg-primary hover:bg-primary/90 text-on-primary font-label-md shadow-lg shadow-primary/25 transition-all flex items-center gap-1.5"
                                        type="submit" id="btnSubmitBatch">
                                    <span class="material-symbols-outlined text-[18px]">done_all</span>
                                    <span>Lưu toàn bộ suất chiếu</span>
                                </button>
                            </div>
                        </form>
                    </div>

                    <%-- ---------------------------------------------------- --%>
                    <%-- TAB 2: FORM TẠO 1 SUẤT ĐƠN LẺ                        --%>
                    <%-- ---------------------------------------------------- --%>
                    <div id="tabContentSingle" class="hidden">
                        <form method="post" action="${ctx}/manager/showtimes${listQuery}" class="space-y-space-md">
                            <input type="hidden" name="csrfToken" value="${csrfToken}"/>

                            <div class="grid grid-cols-1 md:grid-cols-2 gap-space-md">
                                <div>
                                    <label class="block text-label-md text-on-surface-variant mb-1 font-medium" for="modalMovieId">Phim chiếu <span class="text-primary">*</span></label>
                                    <select id="modalMovieId" name="movieId" class="${inputClass}" required>
                                        <option value="">-- Chọn phim --</option>
                                        <c:forEach var="movie" items="${movies}">
                                            <option value="${movie.movieId}"
                                                    data-duration="${movie.durationMinutes}"
                                                    data-formats="<c:out value='${movie.formats}'/>">
                                                <c:out value="${movie.title}"/> (${movie.durationMinutes} phút)
                                            </option>
                                        </c:forEach>
                                    </select>
                                </div>

                                <div>
                                    <label class="block text-label-md text-on-surface-variant mb-1 font-medium" for="modalRoomId">Phòng chiếu <span class="text-primary">*</span></label>
                                    <select id="modalRoomId" name="roomId" class="${inputClass}" required>
                                        <option value="">-- Chọn phòng chiếu --</option>
                                        <c:forEach var="room" items="${rooms}">
                                            <c:if test="${room.status == 'Active'}">
                                                <option value="${room.roomId}" data-format="${empty room.format ? '2D' : room.format}">
                                                    <c:out value="${room.roomName}"/> (${empty room.format ? '2D' : room.format} - ${room.totalSeats} ghế)
                                                </option>
                                            </c:if>
                                        </c:forEach>
                                    </select>
                                </div>
                            </div>

                            <div>
                                <label class="block text-label-md text-on-surface-variant mb-1.5 font-medium">Hình thức bản chiếu <span class="text-primary">*</span></label>
                                <div class="grid grid-cols-2 gap-3">
                                    <label class="flex items-center gap-2.5 p-3 rounded-xl border border-white/10 bg-surface-container-high cursor-pointer hover:border-primary/50 transition">
                                        <input type="radio" name="translationType" value="Phụ đề" checked class="accent-primary"/>
                                        <div>
                                            <div class="text-body-sm font-bold text-on-surface">Phụ đề</div>
                                            <div class="text-[11px] text-outline">Ngôn ngữ gốc + Phụ đề</div>
                                        </div>
                                    </label>
                                    <label class="flex items-center gap-2.5 p-3 rounded-xl border border-white/10 bg-surface-container-high cursor-pointer hover:border-primary/50 transition">
                                        <input type="radio" name="translationType" value="Lồng tiếng" class="accent-primary"/>
                                        <div>
                                            <div class="text-body-sm font-bold text-on-surface">Lồng tiếng</div>
                                            <div class="text-[11px] text-outline">Lồng tiếng Việt</div>
                                        </div>
                                    </label>
                                </div>
                            </div>

                            <div>
                                <label class="block text-label-md text-on-surface-variant mb-1 font-medium" for="modalStartTime">Thời gian bắt đầu <span class="text-primary">*</span></label>
                                <input id="modalStartTime" name="startTime" class="${inputClass}" type="datetime-local" required/>
                            </div>

                            <%-- Khung preview tính toán thời gian và giá vé --%>
                            <div class="p-4 rounded-xl bg-surface-container border border-white/10 space-y-2">
                                <div class="flex items-center justify-between text-body-sm">
                                    <span class="text-on-surface-variant">Dự kiến kết thúc (+15p dọn rạp):</span>
                                    <span id="previewEndTime" class="font-bold text-on-surface font-mono">--</span>
                                </div>
                                <div class="flex items-center justify-between text-body-sm pt-2 border-t border-white/5">
                                    <span class="text-on-surface-variant">Giá vé gợi ý:</span>
                                    <div class="flex items-center gap-2">
                                        <span id="previewPriceValue" class="font-bold text-emerald-400 font-mono">--</span>
                                        <button type="button" id="btnApplyPrice" class="px-2 py-0.5 rounded bg-emerald-500/20 text-emerald-300 text-label-sm">Áp dụng</button>
                                    </div>
                                </div>
                            </div>

                            <div>
                                <label class="block text-label-md text-on-surface-variant mb-1 font-medium" for="modalBasePrice">Giá vé gốc (VND) <span class="text-primary">*</span></label>
                                <input id="modalBasePrice" name="basePrice" class="${inputClass} font-mono"
                                       type="number" min="10000" step="1000" required placeholder="VD: 85000"/>
                            </div>

                            <div class="flex justify-end gap-space-sm pt-space-md border-t border-white/5">
                                <a class="px-space-lg py-2 rounded-full bg-surface-container-high hover:bg-surface-bright text-on-surface font-label-md"
                                   href="${ctx}/manager/showtimes${listQuery}">Hủy</a>
                                <button class="px-space-xl py-2 rounded-full bg-primary hover:bg-primary/90 text-on-primary font-label-md shadow-lg"
                                        type="submit">Tạo suất chiếu</button>
                            </div>
                        </form>
                    </div>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
</c:if>

<%-- JAVASCRIPT HỖ TRỢ TÍNH TOÁN BATCH & SINGLE --%>
<script>
document.addEventListener('DOMContentLoaded', function () {
    // 1. Tự động ẩn toast sau 5 giây
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

    // Thiết lập ngày mặc định cho Batch Form (Hôm nay -> 7 ngày tới)
    var todayStr = new Date().toISOString().split('T')[0];
    var nextWeek = new Date();
    nextWeek.setDate(nextWeek.getDate() + 7);
    var nextWeekStr = nextWeek.toISOString().split('T')[0];

    var startDateInput = document.getElementById('startDate');
    var endDateInput = document.getElementById('endDate');
    if (startDateInput && !startDateInput.value) {
        startDateInput.value = todayStr;
        startDateInput.min = todayStr;
    }
    if (endDateInput && !endDateInput.value) {
        endDateInput.value = nextWeekStr;
        endDateInput.min = todayStr;
    }

    // Khởi tạo các mốc giờ mẫu cho Batch
    if (document.getElementById('selectedSlotsContainer')) {
        addTimeSlot('09:30');
        addTimeSlot('13:30');
        addTimeSlot('19:30');
    }

    // Khởi tạo logic tính toán cho form đơn
    initSingleCalculations();
});

// Chuyển đổi giữa 2 tab: Batch và Single
function switchCreateTab(tab) {
    var tabBtnBatch = document.getElementById('tabBtnBatch');
    var tabBtnSingle = document.getElementById('tabBtnSingle');
    var tabContentBatch = document.getElementById('tabContentBatch');
    var tabContentSingle = document.getElementById('tabContentSingle');

    if (tab === 'batch') {
        tabBtnBatch.className = 'flex-1 py-2 px-4 rounded-lg font-label-md transition-all flex items-center justify-center gap-2 bg-primary text-on-primary shadow-md';
        tabBtnSingle.className = 'flex-1 py-2 px-4 rounded-lg font-label-md transition-all flex items-center justify-center gap-2 text-on-surface-variant hover:text-on-surface';
        tabContentBatch.classList.remove('hidden');
        tabContentSingle.classList.add('hidden');
    } else {
        tabBtnSingle.className = 'flex-1 py-2 px-4 rounded-lg font-label-md transition-all flex items-center justify-center gap-2 bg-primary text-on-primary shadow-md';
        tabBtnBatch.className = 'flex-1 py-2 px-4 rounded-lg font-label-md transition-all flex items-center justify-center gap-2 text-on-surface-variant hover:text-on-surface';
        tabContentSingle.classList.remove('hidden');
        tabContentBatch.classList.add('hidden');
    }
}

// Danh sách các mốc giờ chiếu trong batch
var currentBatchSlots = [];

function addTimeSlot(timeVal) {
    if (!timeVal || currentBatchSlots.indexOf(timeVal) !== -1) {
        return;
    }
    currentBatchSlots.push(timeVal);
    currentBatchSlots.sort();
    renderBatchSlotTags();
    onBatchParamsChange();
}

function addCustomTimeSlot() {
    var customInput = document.getElementById('customTimeInput');
    if (customInput && customInput.value) {
        addTimeSlot(customInput.value);
        customInput.value = '';
    }
}

function removeTimeSlot(timeVal) {
    var idx = currentBatchSlots.indexOf(timeVal);
    if (idx !== -1) {
        currentBatchSlots.splice(idx, 1);
        renderBatchSlotTags();
        onBatchParamsChange();
    }
}

function renderBatchSlotTags() {
    var container = document.getElementById('selectedSlotsContainer');
    if (!container) return;

    if (currentBatchSlots.length === 0) {
        container.innerHTML = '<span class="text-outline text-label-sm self-center">Chưa chọn giờ chiếu nào. Hãy bấm các nút bên trên hoặc tự nhập giờ.</span>';
        return;
    }

    var html = '';
    for (var i = 0; i < currentBatchSlots.length; i++) {
        var t = currentBatchSlots[i];
        html += '<span class="inline-flex items-center gap-1.5 px-3 py-1 rounded-md bg-primary/15 text-primary border border-primary/30 font-mono text-label-md">';
        html += '<input type="hidden" name="timeSlots" value="' + t + '"/>';
        html += '<span>' + t + '</span>';
        html += '<button type="button" onclick="removeTimeSlot(\'' + t + '\')" class="text-primary hover:text-white transition-colors" title="Xóa mốc giờ">&times;</button>';
        html += '</span>';
    }
    container.innerHTML = html;
}

function selectDays(type) {
    var checkboxes = document.querySelectorAll('input[name="daysOfWeek"]');
    checkboxes.forEach(function (cb) {
        var val = parseInt(cb.value, 10);
        if (type === 'all') {
            cb.checked = true;
        } else if (type === 'weekday') {
            cb.checked = (val >= 1 && val <= 4);
        } else if (type === 'weekend') {
            cb.checked = (val >= 5 && val <= 7);
        }
    });
    onBatchParamsChange();
}

function onBatchParamsChange() {
    var movieSelect = document.getElementById('batchMovieId');
    var roomSelect = document.getElementById('batchRoomId');
    var startDateInput = document.getElementById('startDate');
    var endDateInput = document.getElementById('endDate');
    var batchCountBadge = document.getElementById('batchCountBadge');
    var batchDetailText = document.getElementById('batchDetailText');
    var formatWarning = document.getElementById('batchFormatWarning');
    var formatWarningText = document.getElementById('batchFormatWarningText');

    if (!movieSelect || !roomSelect || !startDateInput || !endDateInput) {
        return;
    }

    var movieOpt = movieSelect.options[movieSelect.selectedIndex];
    var roomOpt = roomSelect.options[roomSelect.selectedIndex];

    // Kiểm tra định dạng phim vs phòng (Góp ý 5)
    if (movieOpt && movieOpt.value && roomOpt && roomOpt.value) {
        var movieFormats = (movieOpt.dataset.formats || '').toUpperCase();
        var roomFormat = (roomOpt.dataset.format || '2D').toUpperCase();
        if (movieFormats && movieFormats.indexOf(roomFormat) === -1) {
            formatWarningText.textContent = 'Phim chỉ hỗ trợ bản [' + (movieOpt.dataset.formats || '2D') + '], không tương thích với phòng [' + roomFormat + ']. Hãy chọn phòng khác.';
            formatWarning.classList.remove('hidden');
        } else {
            formatWarning.classList.add('hidden');
        }
    } else {
        formatWarning.classList.add('hidden');
    }

    var startVal = startDateInput.value;
    var endVal = endDateInput.value;
    if (!startVal || !endVal || currentBatchSlots.length === 0) {
        batchCountBadge.textContent = '0 suất chiếu';
        batchDetailText.textContent = 'Vui lòng chọn phim, phòng, khoảng ngày và ít nhất một khung giờ.';
        return;
    }

    var sDate = new Date(startVal);
    var eDate = new Date(endVal);
    if (sDate > eDate) {
        batchCountBadge.textContent = '0 suất chiếu';
        batchDetailText.textContent = 'Ngày kết thúc phải từ ngày bắt đầu trở đi.';
        return;
    }

    // Đếm các ngày thỏa mãn
    var selectedDays = [];
    document.querySelectorAll('input[name="daysOfWeek"]:checked').forEach(function (cb) {
        selectedDays.push(parseInt(cb.value, 10));
    });

    var matchingDays = 0;
    var cur = new Date(sDate);
    while (cur <= eDate) {
        var jsDay = cur.getDay(); // 0=Sun, 1=Mon...
        var isoDay = jsDay === 0 ? 7 : jsDay;
        if (selectedDays.indexOf(isoDay) !== -1) {
            matchingDays++;
        }
        cur.setDate(cur.getDate() + 1);
    }

    var totalEstimated = matchingDays * currentBatchSlots.length;
    batchCountBadge.textContent = totalEstimated + ' suất chiếu dự kiến';
    batchDetailText.textContent = matchingDays + ' ngày phù hợp × ' + currentBatchSlots.length + ' khung giờ/ngày. Hệ thống sẽ tự động tính toán giờ kết thúc (+15p dọn rạp) và bảng giá.';
}

// Logic tính toán cho form đơn
function initSingleCalculations() {
    var movieSelect = document.getElementById('modalMovieId');
    var roomSelect = document.getElementById('modalRoomId');
    var startTimeInput = document.getElementById('modalStartTime');
    var basePriceInput = document.getElementById('modalBasePrice');
    var previewEndTime = document.getElementById('previewEndTime');
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

        if (startVal && movieOpt && movieOpt.dataset.duration) {
            var duration = parseInt(movieOpt.dataset.duration, 10);
            var startDate = new Date(startVal);
            if (!isNaN(startDate.getTime()) && duration > 0) {
                var totalMinutes = duration + 15;
                var endDate = new Date(startDate.getTime() + totalMinutes * 60000);
                var pad = function(n) { return n < 10 ? '0' + n : n; };
                previewEndTime.textContent = pad(endDate.getHours()) + ':' + pad(endDate.getMinutes()) + ' (' + pad(endDate.getDate()) + '/' + pad(endDate.getMonth() + 1) + ') [' + duration + 'p + 15p dọn]';
            }
        }

        if (startVal && roomOpt) {
            var sDate = new Date(startVal);
            if (!isNaN(sDate.getTime())) {
                var day = sDate.getDay();
                var isWeekend = (day === 0 || day === 5 || day === 6);
                var hour = sDate.getHours();
                var base = 0;
                if (hour < 12) base = isWeekend ? 80000 : 65000;
                else if (hour < 17) base = isWeekend ? 95000 : 75000;
                else if (hour < 22) base = isWeekend ? 110000 : 85000;
                else base = isWeekend ? 90000 : 75000;

                var format = roomOpt.dataset.format || '2D';
                var surcharge = 0;
                if (format === '3D') surcharge = 20000;
                else if (format === 'IMAX') surcharge = 50000;

                currentSuggestedPrice = base + surcharge;
                if (previewPriceValue) {
                    previewPriceValue.textContent = currentSuggestedPrice.toLocaleString('vi-VN') + 'đ';
                }
                if (basePriceInput && !basePriceInput.value) {
                    basePriceInput.value = currentSuggestedPrice;
                }
            }
        }
    }

    movieSelect.addEventListener('change', recalculate);
    roomSelect.addEventListener('change', recalculate);
    startTimeInput.addEventListener('change', recalculate);
    startTimeInput.addEventListener('input', recalculate);

    if (btnApplyPrice) {
        btnApplyPrice.addEventListener('click', function () {
            if (currentSuggestedPrice > 0 && basePriceInput) {
                basePriceInput.value = currentSuggestedPrice;
            }
        });
    }

    recalculate();
}
</script>
</body>
</html>
