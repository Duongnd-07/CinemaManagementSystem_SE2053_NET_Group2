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
    <title>Sự cố thiết bị - CineAdmin</title>
    <jsp:include page="/WEB-INF/views/common/manager-head.jsp"/>
    <link href="${ctx}/assets/css/cineluxe.css" rel="stylesheet"/>
</head>
<body class="bg-surface font-body-md text-on-surface">
<jsp:include page="/WEB-INF/views/common/manager-sidebar.jsp"/>

<main class="pl-64 pt-16">
<div class="px-space-xl pt-space-lg pb-16">

    <c:if test="${not empty flashSuccess or not empty flashError}">
        <div id="toast" role="status"
             class="fixed top-20 right-8 z-[60] max-w-md flex items-start gap-space-sm px-space-lg py-space-md rounded-xl shadow-2xl border
                    ${not empty flashSuccess ? 'bg-emerald-950 border-emerald-500/40 text-emerald-300' : 'bg-error-container border-error/40 text-on-error-container'}">
            <span class="material-symbols-outlined">${not empty flashSuccess ? 'check_circle' : 'error'}</span>
            <span class="font-label-lg"><c:out value="${not empty flashSuccess ? flashSuccess : flashError}"/></span>
        </div>
    </c:if>

    <div class="mb-space-xl">
        <div class="flex items-center gap-space-sm mb-space-xs">
            <span class="text-label-sm px-2 py-0.5 rounded bg-primary/20 text-primary uppercase">Quản lý rạp</span>
            <span class="text-outline text-body-sm">/</span>
            <span class="text-on-surface-variant text-body-sm">Sự cố thiết bị</span>
        </div>
        <h1 class="text-headline-lg text-on-surface">Xử lý báo cáo sự cố</h1>
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
           href="${ctx}/manager/equipment-reports">Tất cả (${allCount})</a>
        <c:forEach var="st" items="${statuses}">
            <c:url var="tabUrl" value="/manager/equipment-reports">
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
                    <th class="py-4 px-5 whitespace-nowrap">Người gửi</th>
                    <th class="py-4 px-5 whitespace-nowrap">Mức khẩn cấp</th>
                    <th class="py-4 px-5 whitespace-nowrap">Trạng thái</th>
                    <th class="py-4 px-5 whitespace-nowrap">Thời gian gửi</th>
                    <th class="py-4 px-5 whitespace-nowrap"></th>
                </tr>
                </thead>
                <tbody class="divide-y divide-white/5 text-body-md text-on-surface">
                <c:forEach var="item" items="${reportPage.items}">
                    <tr class="hover:bg-surface-container transition-colors align-top">
                        <td class="py-4 px-5 text-outline font-label-sm whitespace-nowrap">#SC-${item.reportId}</td>
                        <td class="py-4 px-5 whitespace-nowrap">
                            <span class="material-symbols-outlined text-[18px] text-on-surface-variant mr-1">${categoryIcons[item.equipmentCategory]}</span>
                            <c:out value="${categoryLabels[item.equipmentCategory]}"/>
                        </td>
                        <td class="py-4 px-5 whitespace-nowrap">
                            <c:out value="${item.roomName}"/>
                            <c:if test="${not empty item.seatLabel}">
                                <span class="text-on-surface-variant"> • ${not empty item.seatId ? 'Ghế ' : ''}<c:out value="${item.seatLabel}"/></span>
                            </c:if>
                            <c:if test="${item.roomStatus == 'Maintenance'}">
                                <div class="mt-1"><span class="cl-pill cl-pill--danger">Phòng đang khóa</span></div>
                            </c:if>
                        </td>
                        <td class="py-4 px-5 whitespace-nowrap"><c:out value="${item.staffName}"/></td>
                        <td class="py-4 px-5"><span class="cl-pill ${urgencyPills[item.urgencyLevel]}"><c:out value="${urgencyLabels[item.urgencyLevel]}"/></span></td>
                        <td class="py-4 px-5"><span class="cl-pill ${statusPills[item.status]}"><c:out value="${statusLabels[item.status]}"/></span></td>
                        <td class="py-4 px-5 text-on-surface-variant whitespace-nowrap">
                            <c:set var="sentAt" value="${item.reportDate}"/>
                            <c:if test="${not empty sentAt}">${fn:substring(sentAt, 8, 10)}/${fn:substring(sentAt, 5, 7)}/${fn:substring(sentAt, 0, 4)} ${fn:substring(sentAt, 11, 16)}</c:if>
                        </td>
                        <td class="py-4 px-5 text-right whitespace-nowrap">
                            <a class="inline-flex items-center gap-1 px-3 py-1.5 rounded-lg bg-surface-container-high hover:bg-surface-bright text-on-surface font-label-lg transition-all"
                               href="${ctx}/manager/equipment-reports?id=${item.reportId}${fn:escapeXml(fn:replace(listQuery, '?', '&'))}">
                                <span class="material-symbols-outlined text-[18px]">${item.status == 'Resolved' ? 'visibility' : 'build'}</span>
                                ${item.status == 'Resolved' ? 'Xem' : 'Xử lý'}
                            </a>
                        </td>
                    </tr>
                </c:forEach>
                <c:if test="${empty reportPage.items}">
                    <tr><td colspan="8" class="py-12 px-6 text-center text-on-surface-variant"><c:out value="${msgNoResult}"/></td></tr>
                </c:if>
                </tbody>
            </table>
        </div>
        <c:url var="pageBase" value="/manager/equipment-reports">
            <c:if test="${not empty statusFilter}"><c:param name="status" value="${statusFilter}"/></c:if>
        </c:url>
        <ui:pagination pageData="${reportPage}" baseUrl="${pageBase}" label="báo cáo"/>
    </div>
</div>
</main>

<c:if test="${not empty report}">
    <div class="fixed inset-0 bg-black/80 backdrop-blur-md z-50 flex items-center justify-center p-4 overflow-y-auto" data-modal>
        <div class="bg-surface-container-low border border-white/10 rounded-2xl w-full max-w-2xl p-space-xl shadow-2xl relative my-8">
            <a class="absolute right-4 top-4 text-on-surface-variant hover:text-on-surface"
               href="${ctx}/manager/equipment-reports${fn:escapeXml(listQuery)}" title="Đóng">
                <span class="material-symbols-outlined">close</span>
            </a>
            <h3 class="text-headline-md text-on-surface mb-space-md flex items-center gap-space-sm">
                <span class="material-symbols-outlined text-primary">build</span>Báo cáo #SC-${report.reportId}
            </h3>

            <%-- Thông tin báo cáo --%>
            <div class="grid grid-cols-2 gap-x-space-lg gap-y-space-sm text-body-md mb-space-md">
                <div><span class="text-on-surface-variant">Thiết bị:</span> <c:out value="${categoryLabels[report.equipmentCategory]}"/></div>
                <div><span class="text-on-surface-variant">Mức khẩn cấp:</span>
                    <span class="cl-pill ${urgencyPills[report.urgencyLevel]}"><c:out value="${urgencyLabels[report.urgencyLevel]}"/></span></div>
                <div><span class="text-on-surface-variant">Vị trí:</span> <c:out value="${report.roomName}"/><c:if test="${not empty report.seatLabel}"> • ${not empty report.seatId ? 'Ghế ' : ''}<c:out value="${report.seatLabel}"/></c:if></div>
                <div><span class="text-on-surface-variant">Trạng thái:</span>
                    <span class="cl-pill ${statusPills[report.status]}"><c:out value="${statusLabels[report.status]}"/></span></div>
                <div><span class="text-on-surface-variant">Người gửi:</span> <c:out value="${report.staffName}"/></div>
                <div><span class="text-on-surface-variant">Gửi lúc:</span>
                    <c:set var="sentAt" value="${report.reportDate}"/>
                    <c:if test="${not empty sentAt}">${fn:substring(sentAt, 8, 10)}/${fn:substring(sentAt, 5, 7)}/${fn:substring(sentAt, 0, 4)} ${fn:substring(sentAt, 11, 16)}</c:if>
                </div>
            </div>
            <div class="bg-surface-container rounded-lg p-space-md text-body-md text-on-surface mb-space-lg whitespace-pre-line"><c:out value="${report.description}"/></div>

            <c:if test="${not empty errors.general}">
                <div class="mb-space-md form-error-banner"><c:out value="${errors.general}"/></div>
            </c:if>
            <%-- Báo cáo đã xử lý không có ô trạng thái nên lỗi trạng thái hiện ở đây --%>
            <c:if test="${resolved and not empty errors.newStatus}">
                <div class="mb-space-md form-error-banner"><c:out value="${errors.newStatus}"/></div>
            </c:if>

            <form method="post" action="${ctx}/manager/equipment-reports${fn:escapeXml(listQuery)}" novalidate class="space-y-space-md">
                <input type="hidden" name="csrfToken" value="${csrfToken}"/>
                <input type="hidden" name="reportId" value="${report.reportId}"/>
                <c:if test="${not resolved}">
                    <div>
                        <label class="block text-label-md text-on-surface-variant mb-1" for="newStatus">Cập nhật trạng thái</label>
                        <select class="${inputClass}" id="newStatus" name="newStatus">
                            <c:forEach var="st" items="${allowedStatuses}">
                                <option value="${st}" ${form.newStatus == st ? 'selected' : ''}>${statusLabels[st]}</option>
                            </c:forEach>
                        </select>
                        <c:if test="${not empty errors.newStatus}"><p class="form-error"><c:out value="${errors.newStatus}"/></p></c:if>
                    </div>
                </c:if>
                <div>
                    <label class="block text-label-md text-on-surface-variant mb-1" for="note">Ghi chú tiến độ</label>
                    <textarea class="${inputClass} py-2" id="note" name="note" rows="3" maxlength="500"
                              placeholder="VD: Đã gọi kỹ thuật, dự kiến sửa xong trong hôm nay"><c:out value="${form.note}"/></textarea>
                    <c:if test="${not empty errors.note}"><p class="form-error"><c:out value="${errors.note}"/></p></c:if>
                </div>
                <label class="flex items-start gap-space-sm cursor-pointer">
                    <input type="checkbox" name="lockRoom" value="1" class="mt-1" ${form.lockRoom ? 'checked' : ''}/>
                    <span>
                        <span class="text-on-surface font-label-lg">Khóa <c:out value="${report.roomName}"/> để sửa chữa</span>
                        <span class="block text-body-sm text-on-surface-variant">Phòng bị khóa sẽ không xếp được suất chiếu mới. Bỏ tích để mở khóa phòng (không mở được khi còn từ 2 thiết bị không phải ghế hỏng mức Cao/Khẩn cấp); phòng tự mở khóa khi mọi sự cố đã xử lý xong hoặc còn dưới 2 thiết bị hỏng mức Cao/Khẩn cấp.</span>
                    </span>
                </label>
                <div class="flex items-center justify-end gap-space-md pt-space-sm">
                    <a class="px-5 py-2.5 rounded-full bg-surface-container-high text-on-surface hover:bg-surface-bright font-label-lg transition-all"
                       href="${ctx}/manager/equipment-reports${fn:escapeXml(listQuery)}">Đóng</a>
                    <button type="submit"
                            class="px-6 py-2.5 rounded-full bg-primary text-on-primary hover:bg-primary/90 font-label-lg shadow-[0_0_20px_-2px_rgba(229,9,20,0.45)] transition-all">
                        Lưu cập nhật
                    </button>
                </div>
            </form>

            <%-- Lịch sử xử lý --%>
            <div class="mt-space-lg pt-space-md border-t border-white/10">
                <div class="text-label-md text-on-surface-variant uppercase tracking-wider mb-space-sm">Lịch sử xử lý</div>
                <c:if test="${empty logs}">
                    <p class="text-body-sm text-on-surface-variant">Chưa có thao tác xử lý nào.</p>
                </c:if>
                <ul class="space-y-space-sm">
                    <c:forEach var="log" items="${logs}">
                        <li class="text-body-sm">
                            <c:set var="logAt" value="${log.createdAt}"/>
                            <span class="text-on-surface-variant">${fn:substring(logAt, 8, 10)}/${fn:substring(logAt, 5, 7)}/${fn:substring(logAt, 0, 4)} ${fn:substring(logAt, 11, 16)}</span>
                            <span class="text-on-surface font-label-lg"> • <c:out value="${log.managerName}"/></span>
                            <c:if test="${log.oldStatus != log.newStatus}">
                                <span class="text-on-surface-variant"> • ${statusLabels[log.oldStatus]} → ${statusLabels[log.newStatus]}</span>
                            </c:if>
                            <c:if test="${not empty log.note}">
                                <div class="text-on-surface whitespace-pre-line"><c:out value="${log.note}"/></div>
                            </c:if>
                        </li>
                    </c:forEach>
                </ul>
            </div>
        </div>
    </div>
</c:if>

<script src="${ctx}/assets/js/manager-common.js"></script>
</body>
</html>
