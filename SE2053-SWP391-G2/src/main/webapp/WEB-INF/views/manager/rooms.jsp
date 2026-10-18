<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="vi" class="dark">
<head>
    <title>Phòng chiếu &amp; sơ đồ ghế - CineAdmin</title>
    <jsp:include page="/WEB-INF/views/common/manager-head.jsp"/>
</head>
<body class="bg-surface font-body-md text-on-surface">
<jsp:include page="/WEB-INF/views/common/manager-sidebar.jsp"/>

<main class="pl-64 pt-16">
<div class="px-space-xl pt-space-lg pb-16">

    <c:if test="${not empty flashSuccess or not empty flashError}">
        <div id="toast" role="status"
             class="fixed top-20 right-8 z-[60] flex items-center gap-space-sm px-space-lg py-space-md rounded-xl shadow-2xl border
                    ${not empty flashSuccess ? 'bg-emerald-500/15 border-emerald-500/40 text-emerald-300' : 'bg-error-container/80 border-error/40 text-on-error-container'}">
            <span class="material-symbols-outlined">${not empty flashSuccess ? 'check_circle' : 'error'}</span>
            <span class="font-label-lg"><c:out value="${not empty flashSuccess ? flashSuccess : flashError}"/></span>
        </div>
    </c:if>

    <div class="flex flex-col md:flex-row md:items-center justify-between gap-space-md mb-space-xl">
        <div>
            <div class="flex items-center gap-space-sm mb-space-xs">
                <span class="text-label-sm px-2 py-0.5 rounded bg-primary/20 text-primary uppercase">Quản lý rạp</span>
                <span class="text-outline text-body-sm">/</span>
                <span class="text-on-surface-variant text-body-sm">Phòng chiếu</span>
            </div>
            <h1 class="text-headline-lg text-on-surface">Phòng chiếu &amp; Sơ đồ ghế</h1>
        </div>
        <a href="${ctx}/manager/rooms?add=1"
           class="flex items-center gap-space-sm bg-primary hover:bg-primary/90 text-on-primary px-space-lg py-2 rounded-full font-label-lg shadow-[0_0_20px_-2px_rgba(229,9,20,0.45)] transition-all whitespace-nowrap self-start md:self-auto">
            <span class="material-symbols-outlined text-[20px]">add</span>
            <span>Thêm phòng mới</span>
        </a>
    </div>

    <div class="bg-surface-container-low rounded-xl overflow-hidden shadow-xl border border-white/5">
        <div class="overflow-x-auto">
            <table class="w-full text-left border-collapse">
                <thead>
                <tr class="bg-surface-container border-b border-white/10 text-on-surface-variant text-label-md uppercase tracking-wider">
                    <th class="py-4 px-6">Mã</th>
                    <th class="py-4 px-6">Tên phòng</th>
                    <th class="py-4 px-6">Định dạng</th>
                    <th class="py-4 px-6">Số ghế / sức chứa</th>
                    <th class="py-4 px-6">Suất chiếu sắp tới</th>
                    <th class="py-4 px-6">Trạng thái</th>
                    <th class="py-4 px-6 text-right">Hành động</th>
                </tr>
                </thead>
                <tbody class="divide-y divide-white/5 text-body-md text-on-surface">
                <c:forEach var="item" items="${rooms}">
                    <c:set var="room" value="${item.room}"/>
                    <tr class="hover:bg-surface-container transition-colors">
                        <td class="py-4 px-6 text-outline font-label-sm">#RM-${room.roomId}</td>
                        <td class="py-4 px-6 font-headline-sm"><c:out value="${room.roomName}"/></td>
                        <td class="py-4 px-6">
                            <span class="px-2 py-0.5 rounded-md text-label-md bg-primary/15 text-primary border border-primary/30"><c:out value="${empty room.format ? '—' : room.format}"/></span>
                        </td>
                        <td class="py-4 px-6 text-on-surface-variant">
                            <c:choose>
                                <c:when test="${item.activeSeats + item.disabledSeats == 0}"><span class="text-secondary">Chưa có sơ đồ ghế</span></c:when>
                                <c:otherwise>
                                    ${item.activeSeats + item.disabledSeats} ghế
                                    <c:if test="${item.disabledSeats > 0}"><span class="text-outline">(${item.disabledSeats} đang khóa)</span></c:if>
                                    <span class="block text-body-sm text-outline">Sức chứa ${room.totalSeats} người</span>
                                </c:otherwise>
                            </c:choose>
                        </td>
                        <td class="py-4 px-6 text-on-surface-variant">${item.upcomingShowtimes}</td>
                        <td class="py-4 px-6">
                            <c:choose>
                                <c:when test="${room.status == 'Maintenance'}">
                                    <span class="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-label-md bg-amber-500/15 text-amber-400 border border-amber-500/30">
                                        <span class="material-symbols-outlined text-[14px]">build</span>Bảo trì
                                    </span>
                                </c:when>
                                <c:otherwise>
                                    <span class="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-label-md bg-emerald-500/15 text-emerald-400 border border-emerald-500/30">
                                        <span class="w-1.5 h-1.5 rounded-full bg-emerald-400"></span>Hoạt động
                                    </span>
                                </c:otherwise>
                            </c:choose>
                        </td>
                        <td class="py-4 px-6 text-right">
                            <a class="inline-flex items-center gap-1 px-3 py-2 rounded-lg bg-surface-container-high hover:bg-surface-bright text-on-surface-variant hover:text-on-surface transition-all font-label-md"
                               href="${ctx}/manager/rooms?edit=${room.roomId}" title="Sửa phòng và sơ đồ ghế">
                                <span class="material-symbols-outlined text-[18px]">grid_on</span>Sơ đồ ghế
                            </a>
                        </td>
                    </tr>
                </c:forEach>
                <c:if test="${empty rooms}">
                    <tr><td colspan="7" class="py-12 px-6 text-center text-on-surface-variant">Chưa có phòng chiếu nào. Hãy bấm "Thêm phòng mới".</td></tr>
                </c:if>
                </tbody>
            </table>
        </div>
    </div>
    <p class="mt-space-md text-body-sm text-outline">
        Trạng thái Bảo trì của phòng được cập nhật từ chức năng xử lý sự cố thiết bị.
    </p>
</div>
</main>

<script src="${ctx}/assets/js/manager-common.js"></script>
</body>
</html>
