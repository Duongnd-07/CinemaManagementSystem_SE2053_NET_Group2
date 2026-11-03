<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<%-- UC-22: ca làm việc của nhân viên. Không có ?id: ca đang mở (nếu có) + lịch sử ca; có ?id: báo cáo một ca --%>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="vi" class="dark">
<head>
    <title>Ca làm việc - CineLuxe Staff</title>
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
                <span class="material-symbols-outlined text-primary-container">schedule</span>Ca làm việc
            </h1>
            <p class="text-body-sm text-on-surface-variant mt-1">Báo cáo doanh thu và đối chiếu két tiền của từng ca bán tại quầy.</p>
        </div>
        <c:if test="${empty history}">
            <a class="inline-flex items-center gap-1 text-on-surface-variant hover:text-primary" href="${ctx}/staff/shifts">
                <span class="material-symbols-outlined text-[18px]">arrow_back</span>Tất cả ca của tôi
            </a>
        </c:if>
    </div>

    <c:choose>
        <c:when test="${not empty report}">
            <jsp:include page="/WEB-INF/views/common/shift-report-body.jsp"/>
        </c:when>
        <c:otherwise>
            <c:if test="${not empty errors.general}">
                <div class="form-error-banner mb-space-md"><c:out value="${errors.general}"/></div>
            </c:if>
            <div class="bg-surface-container-low rounded-xl border border-white/10 p-space-lg flex flex-wrap items-center justify-between gap-space-md">
                <span class="text-on-surface-variant">Bạn chưa mở ca nào.</span>
                <a href="${ctx}/staff/counter"
                   class="flex items-center gap-space-sm bg-primary-container text-on-primary-container px-space-lg py-2 rounded-full font-label-lg">
                    <span class="material-symbols-outlined text-[20px]">play_arrow</span>Mở ca để bán vé
                </a>
            </div>
        </c:otherwise>
    </c:choose>

    <c:if test="${not empty history}">
        <section class="mt-space-xl">
            <h2 class="text-title-md mb-space-md">Các ca gần đây</h2>
            <div class="bg-surface-container-low rounded-xl border border-white/10 overflow-x-auto">
                <table class="w-full text-left text-body-md">
                    <thead class="text-label-md text-on-surface-variant border-b border-white/10">
                    <tr><th class="px-space-md py-2">Ca</th><th>Quầy</th><th>Bắt đầu</th><th>Kết thúc</th>
                        <th class="text-right">Tiền mặt</th><th class="text-right">Chuyển khoản</th><th class="text-right">Thẻ</th><th class="px-space-md">Két</th></tr>
                    </thead>
                    <tbody class="divide-y divide-white/5">
                    <c:forEach var="s" items="${history}">
                        <tr class="hover:bg-surface-container">
                            <td class="px-space-md py-2"><a class="text-primary hover:underline" href="${ctx}/staff/shifts?id=${s.shiftId}">#${s.shiftId}</a></td>
                            <td>Quầy ${fn:substring(s.counterNo, 1, 2)}</td>
                            <td>${fn:substring(s.startedAt, 11, 16)} ${fn:substring(s.startedAt, 8, 10)}/${fn:substring(s.startedAt, 5, 7)}</td>
                            <td><c:choose><c:when test="${empty s.endedAt}"><span class="cl-pill cl-pill--success">Đang mở</span></c:when>
                                <c:otherwise>${fn:substring(s.endedAt, 11, 16)} ${fn:substring(s.endedAt, 8, 10)}/${fn:substring(s.endedAt, 5, 7)}${empty s.closedBy ? ' (tự kết)' : ''}</c:otherwise></c:choose></td>
                            <td class="text-right"><c:if test="${not empty s.cashTotal}"><fmt:formatNumber value="${s.cashTotal}" maxFractionDigits="0"/>đ</c:if></td>
                            <td class="text-right"><c:if test="${not empty s.transferTotal}"><fmt:formatNumber value="${s.transferTotal}" maxFractionDigits="0"/>đ</c:if></td>
                            <td class="text-right"><c:if test="${not empty s.cardTotal}"><fmt:formatNumber value="${s.cardTotal}" maxFractionDigits="0"/>đ</c:if></td>
                            <td class="px-space-md"><c:choose>
                                <c:when test="${empty s.endedAt}">-</c:when>
                                <c:when test="${empty s.countedCash}"><span class="cl-pill cl-pill--warning">Chưa đếm</span></c:when>
                                <c:otherwise><span class="cl-pill cl-pill--muted">Đã đếm</span></c:otherwise>
                            </c:choose></td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
        </section>
    </c:if>
</main>
<script>
    (function () {
        var toast = document.getElementById('toast');
        if (toast) {
            setTimeout(function () { toast.remove(); }, 5000);
        }
    })();
</script>
</body>
</html>
