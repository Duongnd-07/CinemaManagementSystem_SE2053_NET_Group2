<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- UC-19: Manager xem lịch sử bán tại quầy của mọi nhân viên theo ngày --%>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="inputClass" value="form-input bg-surface-container-high text-on-surface px-4 rounded-lg border border-white/10 focus:border-primary outline-none"/>
<!DOCTYPE html>
<html lang="vi" class="dark">
<head>
    <title>Lịch sử bán tại quầy - CineAdmin</title>
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
            <span class="text-on-surface-variant text-body-sm">Lịch sử bán tại quầy</span>
        </div>
        <h1 class="text-headline-lg text-on-surface">Lịch sử bán vé tại quầy</h1>
        <p class="text-body-md text-on-surface-variant mt-1">Đơn của mọi nhân viên: vé, khách, voucher, điểm, tiền thu và tiền trả lại khách.</p>
    </div>

    <form method="get" action="${ctx}/manager/counter-history" class="flex flex-wrap items-end gap-space-md mb-space-lg">
        <div>
            <label class="block text-label-md text-on-surface-variant mb-1" for="date">Ngày bán</label>
            <input class="${inputClass} h-11" type="date" id="date" name="date" value="${date}" onchange="this.form.submit()"/>
        </div>
        <div class="flex-1 min-w-[240px] max-w-xl">
            <label class="block text-label-md text-on-surface-variant mb-1" for="q">Tìm đơn</label>
            <input class="${inputClass} h-11 w-full" type="search" id="q" name="q" value="<c:out value='${q}'/>"
                   placeholder="Mã đơn, mã vé, SĐT / email hoặc tên khách"/>
        </div>
        <button type="submit" class="h-11 px-space-lg rounded-full bg-primary-container text-on-primary-container font-label-lg">Tìm</button>
    </form>

    <jsp:include page="/WEB-INF/views/common/counter-orders-table.jsp">
        <jsp:param name="showStaff" value="1"/>
    </jsp:include>
</div>
</main>
</body>
</html>
