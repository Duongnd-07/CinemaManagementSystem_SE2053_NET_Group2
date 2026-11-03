<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- UC-35: nhân viên kiểm tra và báo cáo tình trạng phòng trước khi mở ca --%>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="inputClass" value="form-input w-full bg-surface-container-high text-on-surface px-4 rounded-lg border border-white/10 focus:border-primary outline-none"/>
<!DOCTYPE html>
<html lang="vi" class="dark">
<head>
    <title>Tình trạng phòng trước ca - CineLuxe Staff</title>
    <jsp:include page="/WEB-INF/views/common/manager-head.jsp"/>
    <link href="${ctx}/assets/css/cineluxe.css" rel="stylesheet"/>
</head>
<body class="bg-surface font-body-md text-on-surface min-h-screen">
<jsp:include page="/WEB-INF/views/common/staff-header.jsp"/>

<main class="max-w-6xl mx-auto px-space-lg pt-space-lg pb-16">
    <c:if test="${not empty flashSuccess or not empty flashError}">
        <div id="toast" role="status"
             class="mb-space-lg flex items-start gap-space-sm px-space-lg py-space-md rounded-xl border
                    ${not empty flashSuccess ? 'bg-emerald-950 border-emerald-500/40 text-emerald-300' : 'bg-error-container border-error/40 text-on-error-container'}">
            <span class="material-symbols-outlined">${not empty flashSuccess ? 'check_circle' : 'error'}</span>
            <span class="font-label-lg"><c:out value="${not empty flashSuccess ? flashSuccess : flashError}"/></span>
        </div>
    </c:if>

    <div class="mb-space-lg">
        <h1 class="text-headline-md text-on-surface flex items-center gap-space-sm">
            <span class="material-symbols-outlined text-primary-container">fact_check</span>Tình trạng phòng trước ca
        </h1>
        <p class="text-body-sm text-on-surface-variant mt-1">Kiểm tra từng phòng trước khi mở ca. Thiết bị cần sửa vẫn báo ở mục Sự cố thiết bị để quản lý xử lý.</p>
    </div>

    <c:if test="${inShift}">
        <div class="mb-space-lg rounded-xl border border-amber-500/40 bg-amber-500/10 px-space-lg py-space-md text-amber-300 font-label-lg">
            Bạn đang trong ca nên không lập báo cáo trước ca được. Nếu phát hiện hỏng hóc, hãy báo ở mục Sự cố thiết bị.
        </div>
    </c:if>

    <h2 class="text-title-lg mb-space-md">Hôm nay</h2>
    <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-space-md mb-space-xl">
        <c:forEach var="room" items="${rooms}">
            <c:set var="latest" value="${latestToday[room.roomId]}"/>
            <div class="cl-card p-space-md flex flex-col gap-space-sm">
                <div class="flex items-center justify-between gap-space-sm">
                    <span class="font-label-lg text-on-surface"><c:out value="${room.roomName}"/></span>
                    <c:choose>
                        <c:when test="${empty latest}"><span class="cl-pill cl-pill--muted">Chưa kiểm tra</span></c:when>
                        <c:when test="${latest.issue}"><span class="cl-pill cl-pill--danger">Có vấn đề</span></c:when>
                        <c:otherwise><span class="cl-pill cl-pill--success">Đạt</span></c:otherwise>
                    </c:choose>
                </div>
                <p class="text-body-sm text-on-surface-variant min-h-5">
                    <c:if test="${not empty latest}">${latest.createdTimeText} · <c:out value="${latest.staffName}"/></c:if>
                    <c:if test="${room.status == 'Maintenance'}"><span class="text-amber-300">Đang bảo trì</span></c:if>
                </p>
                <c:if test="${not inShift}">
                    <a class="text-center px-4 py-2 rounded-full bg-surface-container-high hover:bg-surface-bright font-label-lg transition-all"
                       href="${ctx}/staff/room-conditions?roomId=${room.roomId}">Kiểm tra phòng</a>
                </c:if>
            </div>
        </c:forEach>
    </div>

    <h2 class="text-title-lg mb-space-md">Báo cáo của tôi</h2>
    <c:url var="pageBase" value="/staff/room-conditions" scope="request"/>
    <jsp:include page="/WEB-INF/views/common/room-condition-table.jsp">
        <jsp:param name="showReportLink" value="1"/>
    </jsp:include>
</main>

<c:if test="${showForm}">
    <div class="fixed inset-0 bg-black/80 backdrop-blur-md z-50 flex items-center justify-center p-4 overflow-y-auto" data-modal>
        <div class="bg-surface-container-low border border-white/10 rounded-2xl w-full max-w-2xl p-space-xl shadow-2xl relative my-8">
            <a class="absolute right-4 top-4 text-on-surface-variant hover:text-on-surface" href="${ctx}/staff/room-conditions" title="Đóng">
                <span class="material-symbols-outlined">close</span>
            </a>
            <h3 class="text-headline-md text-on-surface mb-space-lg flex items-center gap-space-sm">
                <span class="material-symbols-outlined text-primary-container">fact_check</span>Kiểm tra phòng
            </h3>
            <c:if test="${not empty errors.general}">
                <p class="mb-space-md rounded-lg border border-error/40 bg-error-container px-space-md py-space-sm text-on-error-container" role="alert"><c:out value="${errors.general}"/></p>
            </c:if>
            <form method="post" action="${ctx}/staff/room-conditions" novalidate class="space-y-space-md">
                <input type="hidden" name="csrfToken" value="${csrfToken}"/>
                <div>
                    <label class="block text-label-md text-on-surface-variant mb-1" for="roomId">Phòng chiếu <span class="req">*</span></label>
                    <select class="${inputClass} h-11" id="roomId" name="roomId">
                        <option value="">-- Chọn phòng --</option>
                        <c:forEach var="room" items="${rooms}">
                            <option value="${room.roomId}" ${form.roomId eq ''.concat(room.roomId) ? 'selected' : ''}><c:out value="${room.roomName}"/></option>
                        </c:forEach>
                    </select>
                    <c:if test="${not empty errors.roomId}"><p class="form-error"><c:out value="${errors.roomId}"/></p></c:if>
                </div>

                <div class="divide-y divide-white/5 rounded-lg border border-white/10">
                    <c:forEach var="item" items="${checkItems}">
                        <c:set var="errorKey" value="item_${item.key}"/>
                        <fieldset class="p-space-md" data-check-item>
                            <div class="flex flex-wrap items-center justify-between gap-space-md">
                                <legend class="float-left">
                                    <span class="flex items-center gap-1 font-label-lg text-on-surface">
                                        <span class="material-symbols-outlined text-[20px] text-on-surface-variant">${item.icon}</span><c:out value="${item.label}"/> <span class="req">*</span>
                                    </span>
                                    <span class="block text-body-sm text-on-surface-variant"><c:out value="${item.description}"/></span>
                                </legend>
                                <div class="flex gap-space-md whitespace-nowrap">
                                    <label class="inline-flex items-center gap-1 cursor-pointer">
                                        <input type="radio" name="result_${item.key}" value="OK" ${form.results[item.key] == 'OK' ? 'checked' : ''}/>Đạt
                                    </label>
                                    <label class="inline-flex items-center gap-1 cursor-pointer">
                                        <input type="radio" name="result_${item.key}" value="FAIL" ${form.results[item.key] == 'FAIL' ? 'checked' : ''}/>Không đạt
                                    </label>
                                </div>
                            </div>
                            <div class="clear-both mt-space-sm" data-item-note>
                                <label class="block text-body-sm text-on-surface-variant mb-1" for="note_${item.key}">Mô tả vấn đề <span class="req">*</span></label>
                                <input class="${inputClass} h-10" type="text" id="note_${item.key}" name="note_${item.key}" maxlength="200"
                                       value="<c:out value='${form.itemNotes[item.key]}'/>"/>
                            </div>
                            <c:if test="${not empty errors[errorKey]}"><p class="form-error clear-both"><c:out value="${errors[errorKey]}"/></p></c:if>
                        </fieldset>
                    </c:forEach>
                </div>

                <div>
                    <label class="block text-label-md text-on-surface-variant mb-1" for="note">Ghi chú thêm</label>
                    <textarea class="${inputClass} py-2" id="note" name="note" rows="2" maxlength="500"><c:out value="${form.note}"/></textarea>
                    <c:if test="${not empty errors.note}"><p class="form-error"><c:out value="${errors.note}"/></p></c:if>
                </div>
                <div class="flex items-center justify-end gap-space-md pt-space-sm">
                    <a class="px-5 py-2.5 rounded-full bg-surface-container-high text-on-surface hover:bg-surface-bright font-label-lg transition-all" href="${ctx}/staff/room-conditions">Hủy bỏ</a>
                    <button type="submit" class="px-6 py-2.5 rounded-full bg-primary-container text-on-primary-container hover:bg-primary-container/90 font-label-lg">Gửi báo cáo</button>
                </div>
            </form>
        </div>
    </div>
    <script src="${ctx}/assets/js/room-condition.js"></script>
</c:if>
</body>
</html>
