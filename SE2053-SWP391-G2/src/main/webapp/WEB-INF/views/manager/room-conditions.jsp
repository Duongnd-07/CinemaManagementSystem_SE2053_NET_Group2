<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- UC-35: Manager xem báo cáo tình trạng phòng trước ca theo ngày --%>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="inputClass" value="form-input bg-surface-container-high text-on-surface px-4 rounded-lg border border-white/10 focus:border-primary outline-none"/>
<!DOCTYPE html>
<html lang="vi" class="dark">
<head>
    <title>Tình trạng phòng trước ca - CineAdmin</title>
    <jsp:include page="/WEB-INF/views/common/manager-head.jsp"/>
    <link href="${ctx}/assets/css/cineluxe.css" rel="stylesheet"/>
</head>
<body class="bg-surface font-body-md text-on-surface">
<jsp:include page="/WEB-INF/views/common/manager-sidebar.jsp"/>

<main class="pl-64 pt-16">
<div class="px-space-xl pt-space-lg pb-16">
    <div class="mb-space-xl">
        <div class="flex items-center gap-space-sm mb-space-xs">
            <span class="text-label-sm px-2 py-0.5 rounded bg-primary/20 text-primary uppercase">Quản lý rạp</span>
            <span class="text-outline text-body-sm">/</span>
            <span class="text-on-surface-variant text-body-sm">Tình trạng phòng</span>
        </div>
        <h1 class="text-headline-lg text-on-surface">Tình trạng phòng trước ca</h1>
        <p class="text-body-md text-on-surface-variant mt-1">Kết quả nhân viên kiểm tra phòng chiếu trước khi mở ca.</p>
    </div>

    <c:if test="${uncheckedToday > 0}">
        <div class="mb-space-lg rounded-xl border border-amber-500/40 bg-amber-500/10 px-space-lg py-space-md text-amber-300 font-label-lg">
            Hôm nay còn ${uncheckedToday} phòng chưa được kiểm tra.
        </div>
    </c:if>

    <form method="get" action="${ctx}/manager/room-conditions" class="flex flex-wrap items-end gap-space-md mb-space-lg">
        <div>
            <label class="block text-label-md text-on-surface-variant mb-1" for="date">Ngày</label>
            <input class="${inputClass} h-11" type="date" id="date" name="date" value="${date}" onchange="this.form.submit()"/>
        </div>
        <div>
            <label class="block text-label-md text-on-surface-variant mb-1" for="result">Kết quả</label>
            <select class="${inputClass} h-11" id="result" name="result" onchange="this.form.submit()">
                <option value="">Tất cả</option>
                <option value="Issue" ${resultFilter == 'Issue' ? 'selected' : ''}>Có vấn đề</option>
                <option value="Good" ${resultFilter == 'Good' ? 'selected' : ''}>Đạt</option>
            </select>
        </div>
    </form>

    <c:url var="pageBase" value="/manager/room-conditions" scope="request">
        <c:param name="date" value="${date}"/>
        <c:if test="${not empty resultFilter}"><c:param name="result" value="${resultFilter}"/></c:if>
    </c:url>
    <jsp:include page="/WEB-INF/views/common/room-condition-table.jsp">
        <jsp:param name="showStaff" value="1"/>
    </jsp:include>
</div>
</main>
</body>
</html>
