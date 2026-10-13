<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="categoryLabels" value="${{'Projector':'Máy chiếu','Sound System':'Âm thanh','Air Conditioning':'Điều hòa','Seat':'Ghế','Other':'Khác'}}"/>
<c:set var="categoryIcons" value="${{'Projector':'videocam','Sound System':'volume_up','Air Conditioning':'ac_unit','Seat':'event_seat','Other':'more_horiz'}}"/>
<c:set var="urgencyLabels" value="${{'Low':'Thấp','Medium':'Trung bình','High':'Cao','Critical':'Khẩn cấp'}}"/>
<c:set var="urgencyPills" value="${{'Low':'cl-pill--muted','Medium':'cl-pill--success','High':'cl-pill--warning','Critical':'cl-pill--danger'}}"/>
<c:set var="statusLabels" value="${{'Pending':'Chờ xử lý','In Progress':'Đang xử lý','Resolved':'Đã xử lý'}}"/>
<c:set var="statusPills" value="${{'Pending':'cl-pill--warning','In Progress':'cl-pill--info','Resolved':'cl-pill--success'}}"/>
<c:set var="inputClass" value="form-input bg-surface-container-high text-on-surface px-4 rounded-lg border border-white/10 focus:border-primary outline-none"/>
<!DOCTYPE html>
<html lang="vi" class="dark">
<head>
    <title>Báo cáo sự cố thiết bị - CineLuxe Staff</title>
    <jsp:include page="/WEB-INF/views/common/manager-head.jsp"/>
    <link href="${ctx}/assets/css/cineluxe.css" rel="stylesheet"/>
</head>
<body class="bg-surface font-body-md text-on-surface min-h-screen">
<jsp:include page="/WEB-INF/views/common/staff-header.jsp"/>

<main class="max-w-6xl mx-auto px-space-lg pt-space-lg pb-16">

    <c:if test="${not empty flashSuccess or not empty flashError}">
        <div id="toast" role="status"
             class="fixed top-20 right-8 z-[60] max-w-md flex items-start gap-space-sm px-space-lg py-space-md rounded-xl shadow-2xl border
                    ${not empty flashSuccess ? 'bg-emerald-950 border-emerald-500/40 text-emerald-300' : 'bg-error-container border-error/40 text-on-error-container'}">
            <span class="material-symbols-outlined">${not empty flashSuccess ? 'check_circle' : 'error'}</span>
            <span class="font-label-lg"><c:out value="${not empty flashSuccess ? flashSuccess : flashError}"/></span>
        </div>
    </c:if>

    <div class="flex flex-col md:flex-row md:items-center justify-between gap-space-md mb-space-lg">
        <div>
            <h1 class="text-headline-md text-on-surface flex items-center gap-space-sm">
                <span class="material-symbols-outlined text-primary-container">build</span>Sự cố thiết bị
            </h1>
            <p class="text-body-sm text-on-surface-variant mt-1">Các báo cáo bạn đã gửi cho quản lý rạp.</p>
        </div>
        <a href="${ctx}/staff/equipment-reports?add=1${fn:escapeXml(fn:replace(listQuery, '?', '&'))}"
           class="flex items-center gap-space-sm bg-primary-container hover:bg-primary-container/90 text-on-primary-container px-space-lg py-2 rounded-full font-label-lg shadow-[0_0_20px_-2px_rgba(229,9,20,0.45)] transition-all whitespace-nowrap">
            <span class="material-symbols-outlined text-[20px]">add</span>
            <span>Báo cáo sự cố</span>
        </a>
    </div>

    <%-- Bộ lọc trạng thái --%>
    <c:set var="allCount" value="0"/>
    <c:forEach var="st" items="${statuses}">
        <c:set var="allCount" value="${allCount + (empty statusCounts[st] ? 0 : statusCounts[st])}"/>
    </c:forEach>
    <div class="flex flex-wrap items-center gap-space-sm bg-surface-container-low p-space-md rounded-xl mb-space-lg">
        <c:set var="tabOn" value="bg-primary-container text-on-primary-container shadow-[0_0_15px_-2px_rgba(229,9,20,0.4)]"/>
        <c:set var="tabOff" value="bg-surface-container-high text-on-surface-variant hover:text-on-surface"/>
        <a class="px-4 py-2 rounded-lg font-label-lg text-sm transition-all ${empty statusFilter ? tabOn : tabOff}"
           href="${ctx}/staff/equipment-reports">Tất cả (${allCount})</a>
        <c:forEach var="st" items="${statuses}">
            <c:url var="tabUrl" value="/staff/equipment-reports">
                <c:param name="status" value="${st}"/>
            </c:url>
            <a class="px-4 py-2 rounded-lg font-label-lg text-sm transition-all ${statusFilter == st ? tabOn : tabOff}"
               href="${fn:escapeXml(tabUrl)}">
                ${statusLabels[st]} (${empty statusCounts[st] ? 0 : statusCounts[st]})
            </a>
        </c:forEach>
    </div>

    <div class="cl-card overflow-hidden">
        <div class="overflow-x-auto">
            <table class="w-full text-left border-collapse">
                <thead>
                <tr class="bg-surface-container border-b border-white/10 text-on-surface-variant text-label-md uppercase tracking-wider">
                    <th class="py-4 px-5 whitespace-nowrap">Mã</th>
                    <th class="py-4 px-5 whitespace-nowrap">Thiết bị</th>
                    <th class="py-4 px-5 whitespace-nowrap">Vị trí</th>
                    <th class="py-4 px-5 whitespace-nowrap">Mô tả</th>
                    <th class="py-4 px-5 whitespace-nowrap">Mức khẩn cấp</th>
                    <th class="py-4 px-5 whitespace-nowrap">Trạng thái</th>
                    <th class="py-4 px-5 whitespace-nowrap">Thời gian gửi</th>
                </tr>
                </thead>
                <tbody class="divide-y divide-white/5 text-body-md text-on-surface">
                <c:forEach var="report" items="${reportPage.items}">
                    <tr class="hover:bg-surface-container transition-colors align-top">
                        <td class="py-4 px-5 text-outline font-label-sm whitespace-nowrap">#SC-${report.reportId}</td>
                        <td class="py-4 px-5 whitespace-nowrap">
                            <span class="material-symbols-outlined text-[18px] text-on-surface-variant mr-1">${categoryIcons[report.equipmentCategory]}</span>
                            <c:out value="${categoryLabels[report.equipmentCategory]}"/>
                        </td>
                        <td class="py-4 px-5 whitespace-nowrap">
                            <c:out value="${report.roomName}"/>
                            <c:if test="${not empty report.seatLabel}">
                                <span class="text-on-surface-variant"> • Ghế <c:out value="${report.seatLabel}"/></span>
                            </c:if>
                        </td>
                        <td class="py-4 px-5 text-on-surface-variant max-w-xs"><p class="line-clamp-2"><c:out value="${report.description}"/></p></td>
                        <td class="py-4 px-5"><span class="cl-pill ${urgencyPills[report.urgencyLevel]}"><c:out value="${urgencyLabels[report.urgencyLevel]}"/></span></td>
                        <td class="py-4 px-5"><span class="cl-pill ${statusPills[report.status]}"><c:out value="${statusLabels[report.status]}"/></span></td>
                        <td class="py-4 px-5 text-on-surface-variant whitespace-nowrap">
                            <c:set var="sentAt" value="${report.reportDate}"/>
                            <c:if test="${not empty sentAt}">${fn:substring(sentAt, 8, 10)}/${fn:substring(sentAt, 5, 7)}/${fn:substring(sentAt, 0, 4)} ${fn:substring(sentAt, 11, 16)}</c:if>
                        </td>
                    </tr>
                </c:forEach>
                <c:if test="${empty reportPage.items}">
                    <tr><td colspan="7" class="py-12 px-6 text-center text-on-surface-variant"><c:out value="${msgNoResult}"/></td></tr>
                </c:if>
                </tbody>
            </table>
        </div>
        <c:url var="pageBase" value="/staff/equipment-reports">
            <c:if test="${not empty statusFilter}"><c:param name="status" value="${statusFilter}"/></c:if>
        </c:url>
        <ui:pagination pageData="${reportPage}" baseUrl="${pageBase}" label="báo cáo"/>
    </div>
</main>

<c:if test="${showForm}">
    <div class="fixed inset-0 bg-black/80 backdrop-blur-md z-50 flex items-center justify-center p-4 overflow-y-auto" data-modal>
        <div class="bg-surface-container-low border border-white/10 rounded-2xl w-full max-w-xl p-space-xl shadow-2xl relative my-8">
            <a class="absolute right-4 top-4 text-on-surface-variant hover:text-on-surface"
               href="${ctx}/staff/equipment-reports${fn:escapeXml(listQuery)}" title="Đóng">
                <span class="material-symbols-outlined">close</span>
            </a>
            <h3 class="text-headline-md text-on-surface mb-space-lg flex items-center gap-space-sm">
                <span class="material-symbols-outlined text-primary-container">report</span>Báo cáo sự cố thiết bị
            </h3>

            <form method="post" action="${ctx}/staff/equipment-reports" novalidate class="space-y-space-md" id="reportForm">
                <input type="hidden" name="csrfToken" value="${csrfToken}"/>
                <div class="grid grid-cols-2 gap-space-md">
                    <div>
                        <label class="block text-label-md text-on-surface-variant mb-1" for="category">Loại thiết bị <span class="req">*</span></label>
                        <select class="${inputClass}" id="category" name="category">
                            <option value="">-- Chọn loại --</option>
                            <c:forEach var="cat" items="${categories}">
                                <option value="${cat}" ${form.category == cat ? 'selected' : ''}>${categoryLabels[cat]}</option>
                            </c:forEach>
                        </select>
                        <c:if test="${not empty errors.category}"><p class="form-error"><c:out value="${errors.category}"/></p></c:if>
                    </div>
                    <div>
                        <label class="block text-label-md text-on-surface-variant mb-1" for="urgency">Mức khẩn cấp <span class="req">*</span></label>
                        <select class="${inputClass}" id="urgency" name="urgency">
                            <option value="">-- Chọn mức --</option>
                            <c:forEach var="level" items="${urgencyLevels}">
                                <option value="${level}" ${form.urgency == level ? 'selected' : ''}>${urgencyLabels[level]}</option>
                            </c:forEach>
                        </select>
                        <c:if test="${not empty errors.urgency}"><p class="form-error"><c:out value="${errors.urgency}"/></p></c:if>
                    </div>
                </div>
                <div class="grid grid-cols-2 gap-space-md">
                    <div>
                        <label class="block text-label-md text-on-surface-variant mb-1" for="roomId">Phòng chiếu <span class="req">*</span></label>
                        <select class="${inputClass}" id="roomId" name="roomId">
                            <option value="">-- Chọn phòng --</option>
                            <c:forEach var="room" items="${rooms}">
                                <option value="${room.roomId}" ${form.roomId eq ''.concat(room.roomId) ? 'selected' : ''}>
                                    <c:out value="${room.roomName}"/>${room.status == 'Maintenance' ? ' (đang bảo trì)' : ''}
                                </option>
                            </c:forEach>
                        </select>
                        <c:if test="${not empty errors.roomId}"><p class="form-error"><c:out value="${errors.roomId}"/></p></c:if>
                    </div>
                    <%-- Chỉ dùng khi loại thiết bị là Ghế; equipment-report.js ẩn/hiện và lọc ghế theo phòng --%>
                    <div id="seatField">
                        <label class="block text-label-md text-on-surface-variant mb-1" for="seatId">Ghế <span class="req">*</span></label>
                        <select class="${inputClass}" id="seatId" name="seatId">
                            <option value="">-- Chọn ghế --</option>
                            <c:forEach var="seat" items="${seats}">
                                <option value="${seat.seatId}" data-room="${seat.roomId}" ${form.seatId eq ''.concat(seat.seatId) ? 'selected' : ''}>
                                    <c:out value="${seat.rowName}"/>${seat.colNumber}${seat.status == 'Maintenance' ? ' (đang bảo trì)' : ''}
                                </option>
                            </c:forEach>
                        </select>
                        <c:if test="${not empty errors.seatId}"><p class="form-error"><c:out value="${errors.seatId}"/></p></c:if>
                    </div>
                </div>
                <%-- Sự cố đang mở của phòng đã chọn (equipment-report.js lọc theo phòng) để nhân viên không báo trùng --%>
                <div id="openReports" class="rounded-lg border border-amber-500/40 bg-amber-500/10 p-space-md" hidden>
                    <div class="text-amber-300 font-label-lg mb-1">Phòng này đang có sự cố chưa xử lý. Nếu trùng thì không cần gửi lại.</div>
                    <ul class="text-body-sm text-on-surface space-y-1">
                        <c:forEach var="open" items="${openReports}">
                            <li data-room="${open.roomId}">
                                <span class="text-on-surface-variant">#SC-${open.reportId}</span>
                                • ${categoryLabels[open.equipmentCategory]}<c:if test="${not empty open.seatLabel}"> <c:out value="${open.seatLabel}"/></c:if>
                                • ${urgencyLabels[open.urgencyLevel]}
                                • ${statusLabels[open.status]}:
                                <span class="text-on-surface-variant"><c:out value="${fn:length(open.description) > 80 ? fn:substring(open.description, 0, 80).concat('…') : open.description}"/></span>
                            </li>
                        </c:forEach>
                    </ul>
                </div>
                <div>
                    <label class="block text-label-md text-on-surface-variant mb-1" for="description">Mô tả sự cố <span class="req">*</span></label>
                    <textarea class="${inputClass} py-2" id="description" name="description" rows="4" maxlength="1000"
                              placeholder="VD: Máy chiếu bị nhòe hình ở góc phải màn hình"><c:out value="${form.description}"/></textarea>
                    <c:if test="${not empty errors.description}"><p class="form-error"><c:out value="${errors.description}"/></p></c:if>
                </div>
                <p class="text-body-sm text-on-surface-variant" id="seatNotice">
                    Khi báo ghế hỏng, ghế sẽ được khóa bảo trì ngay và không bán được nữa.
                </p>
                <div class="flex items-center justify-end gap-space-md pt-space-sm">
                    <a class="px-5 py-2.5 rounded-full bg-surface-container-high text-on-surface hover:bg-surface-bright font-label-lg transition-all"
                       href="${ctx}/staff/equipment-reports${fn:escapeXml(listQuery)}">Hủy bỏ</a>
                    <button type="submit"
                            class="px-6 py-2.5 rounded-full bg-primary-container text-on-primary-container hover:bg-primary-container/90 font-label-lg shadow-[0_0_20px_-2px_rgba(229,9,20,0.45)] transition-all">
                        Gửi báo cáo
                    </button>
                </div>
            </form>
        </div>
    </div>
    <script src="${ctx}/assets/js/equipment-report.js"></script>
</c:if>

<script src="${ctx}/assets/js/manager-common.js"></script>
</body>
</html>
