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
                                <c:when test="${room.status == 'Inactive'}">
                                    <span class="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-label-md bg-white/5 text-outline border border-white/15">
                                        <span class="material-symbols-outlined text-[14px]">block</span>Dừng hoạt động
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
                            <div class="inline-flex items-center gap-2">
                                <a class="inline-flex items-center gap-1 px-3 py-2 rounded-lg bg-surface-container-high hover:bg-surface-bright text-on-surface-variant hover:text-on-surface transition-all font-label-md"
                                   href="${ctx}/manager/rooms?info=${room.roomId}" title="Sửa tên, định dạng, trạng thái">
                                    <span class="material-symbols-outlined text-[18px]">edit</span>Sửa
                                </a>
                                <a class="inline-flex items-center gap-1 px-3 py-2 rounded-lg bg-surface-container-high hover:bg-surface-bright text-on-surface-variant hover:text-on-surface transition-all font-label-md"
                                   href="${ctx}/manager/rooms?edit=${room.roomId}" title="Sửa sơ đồ ghế">
                                    <span class="material-symbols-outlined text-[18px]">grid_on</span>Sơ đồ ghế
                                </a>
                            </div>
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
        Trạng thái Bảo trì chỉ được bật/tắt ở mục Sự cố thiết bị (khóa phòng để sửa chữa / xử lý xong sự cố).
        Hoạt động / Dừng hoạt động do quản lý chọn ở nút Sửa.
    </p>
</div>
</main>

<%-- Hộp thoại Sửa thông tin phòng (tên, định dạng, trạng thái) --%>
<c:if test="${not empty infoForm}">
    <c:set var="inputClass" value="form-input bg-surface-container-high text-on-surface px-4 rounded-lg border border-white/10 focus:border-primary outline-none w-full"/>
    <c:set var="maintenance" value="${infoRoom.status == 'Maintenance'}"/>
    <div class="fixed inset-0 bg-black/80 backdrop-blur-md z-50 flex items-center justify-center p-4 overflow-y-auto" data-modal>
        <div class="bg-surface-container-low border border-white/10 rounded-2xl w-full max-w-md p-space-xl shadow-2xl relative my-8">
            <a class="absolute right-4 top-4 text-on-surface-variant hover:text-on-surface" href="${ctx}/manager/rooms" title="Đóng">
                <span class="material-symbols-outlined">close</span>
            </a>
            <h3 class="text-headline-md text-on-surface mb-space-lg flex items-center gap-space-sm">
                <span class="material-symbols-outlined text-primary">meeting_room</span>Sửa phòng chiếu
            </h3>

            <c:if test="${not empty errors.general}">
                <div class="mb-space-md form-error-banner"><c:out value="${errors.general}"/></div>
            </c:if>

            <form method="post" action="${ctx}/manager/rooms" novalidate class="space-y-space-md">
                <input type="hidden" name="csrfToken" value="${csrfToken}"/>
                <input type="hidden" name="action" value="info"/>
                <input type="hidden" name="roomId" value="${infoForm.roomId}"/>
                <div>
                    <label class="block text-label-md text-on-surface-variant mb-1" for="infoName">Tên / mã phòng <span class="req">*</span></label>
                    <input class="${inputClass}" id="infoName" name="name" type="text" maxlength="${maxName}" autofocus
                           value="<c:out value='${infoForm.name}'/>"/>
                    <c:if test="${not empty errors.name}"><p class="form-error"><c:out value="${errors.name}"/></p></c:if>
                </div>
                <div>
                    <label class="block text-label-md text-on-surface-variant mb-1" for="infoFormat">Định dạng chiếu <span class="req">*</span></label>
                    <select class="${inputClass}" id="infoFormat" name="format">
                        <c:forEach var="format" items="${formats}">
                            <option value="${format}" ${infoForm.format == format ? 'selected' : ''}>${format}</option>
                        </c:forEach>
                    </select>
                    <c:if test="${not empty errors.format}"><p class="form-error"><c:out value="${errors.format}"/></p></c:if>
                </div>
                <div>
                    <label class="block text-label-md text-on-surface-variant mb-1" for="infoStatus">Trạng thái</label>
                    <c:choose>
                        <c:when test="${maintenance}">
                            <%-- Bảo trì do mục Sự cố thiết bị quản lý: chỉ hiển thị, không gửi lên --%>
                            <select class="${inputClass} opacity-60" id="infoStatus" disabled>
                                <option selected>Bảo trì</option>
                            </select>
                            <p class="mt-1 text-body-sm text-secondary">
                                Phòng đang bảo trì do sự cố thiết bị. Trạng thái tự trở lại khi sự cố được xử lý ở mục
                                <a class="underline" href="${ctx}/manager/equipment-reports">Sự cố thiết bị</a>.
                            </p>
                        </c:when>
                        <c:otherwise>
                            <select class="${inputClass}" id="infoStatus" name="status">
                                <option value="Active" ${infoForm.status == 'Active' ? 'selected' : ''}>Hoạt động</option>
                                <option value="Inactive" ${infoForm.status == 'Inactive' ? 'selected' : ''}>Dừng hoạt động</option>
                            </select>
                            <c:if test="${infoUpcoming > 0}">
                                <p class="mt-1 text-body-sm text-secondary">Phòng đang có ${infoUpcoming} suất chiếu sắp tới nên không thể đổi định dạng hoặc dừng hoạt động.</p>
                            </c:if>
                        </c:otherwise>
                    </c:choose>
                    <c:if test="${not empty errors.status}"><p class="form-error"><c:out value="${errors.status}"/></p></c:if>
                </div>
                <div class="flex items-center justify-end gap-space-md pt-space-md">
                    <a class="px-5 py-2.5 rounded-full bg-surface-container-high text-on-surface hover:bg-surface-bright font-label-lg transition-all"
                       href="${ctx}/manager/rooms">Hủy bỏ</a>
                    <button type="submit"
                            class="px-6 py-2.5 rounded-full bg-primary text-on-primary hover:bg-primary/90 font-label-lg shadow-[0_0_20px_-2px_rgba(229,9,20,0.45)] transition-all">
                        Lưu thay đổi
                    </button>
                </div>
            </form>
        </div>
    </div>
</c:if>

<script src="${ctx}/assets/js/manager-common.js"></script>
</body>
</html>
