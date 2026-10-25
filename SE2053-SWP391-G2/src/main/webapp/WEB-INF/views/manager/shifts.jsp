<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<%-- UC-22: Manager xem báo cáo ca khi bàn giao. Không có ?id: danh sách ca theo ngày; có ?id: báo cáo một ca --%>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="inputClass" value="form-input bg-surface-container-high text-on-surface px-4 rounded-lg border border-white/10 focus:border-primary outline-none"/>
<!DOCTYPE html>
<html lang="vi" class="dark">
<head>
    <title>Báo cáo ca - CineAdmin</title>
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
            <span class="text-on-surface-variant text-body-sm">Báo cáo ca</span>
        </div>
        <h1 class="text-headline-lg text-on-surface">Báo cáo ca bán tại quầy</h1>
        <p class="text-body-md text-on-surface-variant mt-1">Doanh thu và đối chiếu két tiền của từng ca, dùng khi bàn giao ca.</p>
    </div>

    <c:choose>
        <c:when test="${not empty report}">
            <a class="inline-flex items-center gap-1 text-on-surface-variant hover:text-primary mb-space-md"
               href="${ctx}/manager/shifts?date=${fn:substring(report.shift.startedAt, 0, 10)}">
                <span class="material-symbols-outlined text-[18px]">arrow_back</span>Danh sách ca
            </a>
            <jsp:include page="/WEB-INF/views/common/shift-report-body.jsp"/>
        </c:when>
        <c:otherwise>
            <%-- Thời lượng ca do Manager cấu hình; hết giờ hệ thống tự kết ca và chốt số liệu --%>
            <form method="post" action="${ctx}/manager/shifts"
                  class="flex flex-wrap items-end gap-space-md mb-space-lg bg-surface-container-low rounded-xl border border-white/10 p-space-md"
                  onsubmit="return confirm('Đổi thời lượng ca? Ca đang mở sẽ tính lại giờ tự kết; ca đã quá thời lượng mới sẽ bị kết ngay.');">
                <input type="hidden" name="csrfToken" value="${csrfToken}"/>
                <input type="hidden" name="action" value="setHours"/>
                <div>
                    <label class="block text-label-md text-on-surface-variant mb-1" for="shiftHours">Thời lượng một ca (giờ)</label>
                    <input class="${inputClass} h-11 w-28" type="number" id="shiftHours" name="shiftHours"
                           min="${minShiftHours}" max="${maxShiftHours}" step="1" value="${shiftHours}" required/>
                </div>
                <button type="submit" class="h-11 px-space-lg rounded-full bg-primary-container text-on-primary-container font-label-lg">Lưu</button>
                <p class="text-body-sm text-on-surface-variant pb-3">Từ ${minShiftHours} đến ${maxShiftHours} giờ. Hết giờ, ca tự kết thúc và chốt báo cáo; nhân viên được báo trước 15 phút.</p>
            </form>

            <form method="get" action="${ctx}/manager/shifts" class="flex flex-wrap items-end gap-space-md mb-space-lg">
                <div>
                    <label class="block text-label-md text-on-surface-variant mb-1" for="date">Ngày</label>
                    <input class="${inputClass} h-11" type="date" id="date" name="date" value="${date}" onchange="this.form.submit()"/>
                </div>
                <p class="text-body-sm text-on-surface-variant pb-3">Gồm các ca bắt đầu trong ngày và các ca vẫn đang mở.</p>
            </form>
            <c:choose>
                <c:when test="${empty shifts}">
                    <div class="bg-surface-container-low rounded-xl border border-white/10 p-space-lg text-center text-on-surface-variant">
                        Không có ca nào trong ngày này.
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="bg-surface-container-low rounded-xl border border-white/10 overflow-x-auto">
                        <table class="w-full text-left text-body-md">
                            <thead class="text-label-md text-on-surface-variant border-b border-white/10">
                            <tr><th class="px-space-md py-2">Ca</th><th>Nhân viên</th><th>Quầy</th><th>Bắt đầu</th><th>Kết thúc</th>
                                <th class="text-right" title="Tiền lẻ có trong két khi mở ca">Tiền đầu ca</th>
                                <th class="text-right">Thực thu</th>
                                <th class="text-right" title="Tiền mặt đếm được trong két khi kết ca">Tiền cuối ca</th>
                                <th class="px-space-md">Két tiền mặt</th></tr>
                            </thead>
                            <tbody class="divide-y divide-white/5">
                            <c:forEach var="s" items="${shifts}">
                                <tr class="hover:bg-surface-container">
                                    <td class="px-space-md py-2"><a class="text-primary hover:underline" href="${ctx}/manager/shifts?id=${s.shiftId}">#${s.shiftId}</a></td>
                                    <td><c:out value="${s.staffName}"/></td>
                                    <td>Quầy ${fn:substring(s.counterNo, 1, 2)}</td>
                                    <td>${fn:substring(s.startedAt, 11, 16)} ${fn:substring(s.startedAt, 8, 10)}/${fn:substring(s.startedAt, 5, 7)}</td>
                                    <td><c:choose>
                                        <c:when test="${empty s.endedAt}"><span class="cl-pill cl-pill--success">Đang mở</span></c:when>
                                        <c:otherwise>${fn:substring(s.endedAt, 11, 16)}
                                            <span class="text-body-sm text-on-surface-variant">${empty s.closedBy ? '(tự kết)' : ''}</span></c:otherwise>
                                    </c:choose></td>
                                    <td class="text-right"><fmt:formatNumber value="${s.openingCash}" maxFractionDigits="0"/>đ</td>
                                    <td class="text-right"><c:if test="${not empty s.cashTotal}">
                                        <fmt:formatNumber value="${s.cashTotal + s.transferTotal + s.cardTotal}" maxFractionDigits="0"/>đ</c:if></td>
                                    <%-- Số đếm thực tế khi kết ca; ca đang mở hoặc chưa đếm thì chưa có --%>
                                    <td class="text-right"><c:choose>
                                        <c:when test="${not empty s.countedCash}"><fmt:formatNumber value="${s.countedCash}" maxFractionDigits="0"/>đ</c:when>
                                        <c:otherwise><span class="text-on-surface-variant">-</span></c:otherwise>
                                    </c:choose></td>
                                    <td class="px-space-md"><c:choose>
                                        <c:when test="${empty s.endedAt}">-</c:when>
                                        <c:when test="${empty s.countedCash}"><span class="cl-pill cl-pill--warning">Chưa đếm</span></c:when>
                                        <c:otherwise>
                                            <c:set var="diff" value="${s.countedCash - (s.openingCash + s.cashTotal - s.refundTotal)}"/>
                                            <c:choose>
                                                <c:when test="${diff.signum() == 0}"><span class="cl-pill cl-pill--success">Khớp</span></c:when>
                                                <c:otherwise><span class="cl-pill cl-pill--danger">${diff > 0 ? 'Thừa' : 'Thiếu'}
                                                    <fmt:formatNumber value="${diff > 0 ? diff : -diff}" maxFractionDigits="0"/>đ</span></c:otherwise>
                                            </c:choose>
                                        </c:otherwise>
                                    </c:choose></td>
                                </tr>
                            </c:forEach>
                            </tbody>
                        </table>
                    </div>
                </c:otherwise>
            </c:choose>
        </c:otherwise>
    </c:choose>
</div>
</main>
<script src="${ctx}/assets/js/manager-common.js"></script>
</body>
</html>
