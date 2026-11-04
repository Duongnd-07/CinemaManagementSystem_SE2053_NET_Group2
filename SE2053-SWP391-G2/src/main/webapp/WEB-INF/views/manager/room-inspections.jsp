<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%-- UC-35 A5: lịch sử kiểm tra phòng chiếu (chỉ xem) --%>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="inputClass" value="form-input bg-surface-container-high text-on-surface px-4 py-2 rounded-lg border border-white/10 focus:border-primary outline-none"/>
<!DOCTYPE html>
<html lang="vi" class="dark">
<head>
    <title>Lịch sử kiểm tra phòng - CineAdmin</title>
    <jsp:include page="/WEB-INF/views/common/manager-head.jsp"/>
</head>
<body class="bg-surface font-body-md text-on-surface">
<jsp:include page="/WEB-INF/views/common/manager-sidebar.jsp"/>

<main class="pl-64 pt-16">
<div class="px-space-xl pt-space-lg pb-16">
    <div class="mb-space-lg">
        <div class="flex items-center gap-space-sm mb-space-xs">
            <span class="text-label-sm px-2 py-0.5 rounded bg-primary/20 text-primary uppercase">Quản lý rạp</span>
            <span class="text-outline text-body-sm">/</span>
            <span class="text-on-surface-variant text-body-sm">Kiểm tra phòng chiếu</span>
        </div>
        <h1 class="text-headline-lg text-on-surface">Lịch sử kiểm tra phòng chiếu</h1>
        <p class="text-body-sm text-on-surface-variant mt-1">
            Mỗi suất chiếu cần được nhân viên kiểm tra ghế và dọn phòng trước giờ chiếu. Việc kiểm tra không chặn bán vé.
        </p>
    </div>

    <form method="get" action="${ctx}/manager/room-inspections" class="flex flex-wrap items-end gap-space-md mb-space-lg">
        <div>
            <label class="block text-label-md text-on-surface-variant mb-1" for="date">Ngày</label>
            <input class="${inputClass}" type="date" id="date" name="date" value="${date}"/>
        </div>
        <div>
            <label class="block text-label-md text-on-surface-variant mb-1" for="room">Phòng</label>
            <select class="${inputClass}" id="room" name="room">
                <option value="">Tất cả phòng</option>
                <c:forEach var="item" items="${rooms}">
                    <option value="${item.room.roomId}" ${roomFilter == item.room.roomId ? 'selected' : ''}><c:out value="${item.room.roomName}"/></option>
                </c:forEach>
            </select>
        </div>
        <div>
            <label class="block text-label-md text-on-surface-variant mb-1" for="status">Trạng thái</label>
            <select class="${inputClass}" id="status" name="status">
                <option value="">Tất cả</option>
                <option value="NotInspected" ${statusFilter == 'NotInspected' ? 'selected' : ''}>Chưa kiểm tra</option>
                <option value="OK" ${statusFilter == 'OK' ? 'selected' : ''}>Đã kiểm tra – Ổn</option>
                <option value="HasIssue" ${statusFilter == 'HasIssue' ? 'selected' : ''}>Đã kiểm tra – Có sự cố</option>
                <option value="Missed" ${statusFilter == 'Missed' ? 'selected' : ''}>Không được kiểm tra</option>
                <option value="Late" ${statusFilter == 'Late' ? 'selected' : ''}>Kiểm tra chậm</option>
            </select>
        </div>
        <button type="submit" class="px-5 py-2 rounded-full bg-primary text-on-primary hover:bg-primary/90 font-label-lg">Lọc</button>
    </form>

    <c:if test="${missedCount > 0 or lateCount > 0}">
        <p class="mb-space-md text-body-sm">
            <c:if test="${missedCount > 0}"><span class="text-error font-label-lg">${missedCount} suất không được kiểm tra</span></c:if>
            <c:if test="${missedCount > 0 and lateCount > 0}"> · </c:if>
            <c:if test="${lateCount > 0}"><span class="text-orange-300 font-label-lg">${lateCount} suất kiểm tra chậm</span></c:if>
        </p>
    </c:if>

    <div class="bg-surface-container-low rounded-xl overflow-hidden shadow-xl border border-white/5">
        <div class="overflow-x-auto">
            <table class="w-full text-left border-collapse">
                <thead>
                <tr class="bg-surface-container border-b border-white/10 text-on-surface-variant text-label-md uppercase tracking-wider">
                    <th class="py-4 px-6">Suất chiếu</th>
                    <th class="py-4 px-6">Phòng</th>
                    <th class="py-4 px-6">Phim</th>
                    <th class="py-4 px-6">Người kiểm tra</th>
                    <th class="py-4 px-6">Lúc</th>
                    <th class="py-4 px-6">Chi tiết</th>
                    <th class="py-4 px-6 text-right">Kết quả</th>
                </tr>
                </thead>
                <tbody class="divide-y divide-white/5 text-body-md text-on-surface">
                <c:forEach var="slot" items="${slots}">
                    <tr class="${slot.status == 'Missed' ? 'bg-error-container/10' : ''}">
                        <td class="py-3 px-6 whitespace-nowrap">${fn:substring(slot.startTime, 11, 16)} - ${fn:substring(slot.endTime, 11, 16)}</td>
                        <td class="py-3 px-6"><c:out value="${slot.roomName}"/></td>
                        <td class="py-3 px-6 text-on-surface-variant"><c:out value="${slot.movieTitle}"/></td>
                        <td class="py-3 px-6"><c:out value="${empty slot.staffName ? '—' : slot.staffName}"/></td>
                        <td class="py-3 px-6 whitespace-nowrap">${slot.inspected ? fn:substring(slot.inspectedAt, 11, 16) : '—'}</td>
                        <td class="py-3 px-6 text-body-sm text-on-surface-variant">
                            <c:if test="${slot.inspected}">
                                ${slot.equipmentOk ? 'Thiết bị khác bình thường' : 'Chưa xác nhận thiết bị khác'}
                                <c:if test="${slot.reportCount > 0}"> · ${slot.reportCount} báo cáo sự cố</c:if>
                                <c:if test="${not empty slot.note}"><span class="block text-outline">“<c:out value="${slot.note}"/>”</span></c:if>
                            </c:if>
                        </td>
                        <td class="py-3 px-6 text-right">
                            <jsp:include page="/WEB-INF/views/staff/inspection-status.jsp">
                                <jsp:param name="status" value="${slot.status}"/>
                                <jsp:param name="late" value="${slot.late}"/>
                            </jsp:include>
                        </td>
                    </tr>
                </c:forEach>
                <c:if test="${empty slots}">
                    <tr><td colspan="7" class="py-12 px-6 text-center text-on-surface-variant">Không có suất chiếu nào phù hợp.</td></tr>
                </c:if>
                </tbody>
            </table>
        </div>
    </div>
</div>
</main>
</body>
</html>
