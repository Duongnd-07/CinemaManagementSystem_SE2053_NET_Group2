<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%-- UC-19: lịch sử bán vé tại quầy của nhân viên, xem theo từng ca của mình --%>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="inputClass" value="form-input bg-surface-container-high text-on-surface px-4 rounded-lg border border-white/10 focus:border-primary outline-none"/>
<!DOCTYPE html>
<html lang="vi" class="dark">
<head>
    <title>Lịch sử bán vé - CineLuxe Staff</title>
    <jsp:include page="/WEB-INF/views/common/manager-head.jsp"/>
    <link href="${ctx}/assets/css/cineluxe.css" rel="stylesheet"/>
</head>
<body class="bg-surface font-body-md text-on-surface min-h-screen">
<jsp:include page="/WEB-INF/views/common/staff-header.jsp"/>

<main class="max-w-7xl mx-auto px-space-lg pt-space-lg pb-16">
    <div class="mb-space-lg">
        <h1 class="text-headline-md text-on-surface flex items-center gap-space-sm">
            <span class="material-symbols-outlined text-primary-container">receipt_long</span>Lịch sử bán vé
        </h1>
        <p class="text-body-sm text-on-surface-variant mt-1">Các đơn bạn đã bán tại quầy trong từng ca: vé, khách, voucher, điểm, tiền thu và tiền trả lại khách.</p>
    </div>

    <c:choose>
        <c:when test="${empty shift}">
            <div class="bg-surface-container-low rounded-xl border border-white/10 p-space-lg flex flex-wrap items-center justify-between gap-space-md">
                <span class="text-on-surface-variant">Bạn chưa có ca bán vé nào.</span>
                <a href="${ctx}/staff/counter"
                   class="flex items-center gap-space-sm bg-primary-container text-on-primary-container px-space-lg py-2 rounded-full font-label-lg">
                    <span class="material-symbols-outlined text-[20px]">play_arrow</span>Mở ca để bán vé
                </a>
            </div>
        </c:when>
        <c:otherwise>
            <form method="get" action="${ctx}/staff/counter-history" class="flex flex-wrap items-end gap-space-md mb-space-lg">
                <div>
                    <label class="block text-label-md text-on-surface-variant mb-1" for="shiftId">Ca làm việc</label>
                    <select class="${inputClass} h-11" id="shiftId" name="shiftId" onchange="this.form.submit()">
                        <c:forEach var="s" items="${shifts}">
                            <option value="${s.shiftId}" ${s.shiftId == shift.shiftId ? 'selected' : ''}>
                                Ca #${s.shiftId} · Quầy ${fn:substring(s.counterNo, 1, 2)} · ${fn:substring(s.startedAt, 11, 16)} ${fn:substring(s.startedAt, 8, 10)}/${fn:substring(s.startedAt, 5, 7)}${empty s.endedAt ? ' (đang mở)' : ''}
                            </option>
                        </c:forEach>
                    </select>
                </div>
                <div class="flex-1 min-w-[240px]">
                    <label class="block text-label-md text-on-surface-variant mb-1" for="q">Tìm đơn</label>
                    <input class="${inputClass} h-11 w-full" type="search" id="q" name="q" value="<c:out value='${q}'/>"
                           placeholder="Mã đơn, mã vé, SĐT / email hoặc tên khách"/>
                </div>
                <button type="submit" class="h-11 px-space-lg rounded-full bg-primary-container text-on-primary-container font-label-lg">Tìm</button>
            </form>
            <jsp:include page="/WEB-INF/views/common/counter-orders-table.jsp"/>
        </c:otherwise>
    </c:choose>
</main>
</body>
</html>
