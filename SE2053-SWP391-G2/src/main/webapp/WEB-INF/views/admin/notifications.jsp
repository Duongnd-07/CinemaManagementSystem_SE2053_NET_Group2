<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="vi" class="dark">
<head>
    <title>Thông báo - CineLuxe Admin</title>
    <jsp:include page="/WEB-INF/views/common/public-head.jsp"/>
    <style>
        body { font-family: 'Be Vietnam Pro', sans-serif; }
        .uc09-admin :is(a, button):focus-visible { outline: 2px solid white; outline-offset: 3px; }
    </style>
</head>
<body class="uc09-admin min-h-screen bg-[#0c0f17] text-[#dfe2f1]">
<header class="sticky top-0 z-40 flex flex-wrap items-center justify-between gap-3 border-b border-white/10 bg-[#131722] px-4 sm:px-8 py-3">
    <a href="${ctx}/admin/users" class="inline-flex min-h-11 items-center gap-2 font-bold text-white">
        <span class="material-symbols-outlined text-[#ffb4ab]" aria-hidden="true">admin_panel_settings</span>CINEADMIN
    </a>
    <div class="flex items-center gap-3">
        <jsp:include page="/WEB-INF/views/common/notification-bell.jsp"><jsp:param name="href" value="${ctx}/admin/notifications"/></jsp:include>
        <span class="hidden sm:inline text-white"><c:out value="${sessionScope.user.fullName}"/></span>
        <a href="${ctx}/logout" class="inline-flex min-h-11 items-center rounded-xl border border-white/10 px-4 text-white hover:bg-white/10">Đăng xuất</a>
    </div>
</header>
<main class="max-w-5xl mx-auto px-4 sm:px-6 py-8">
    <nav class="mb-6 flex flex-wrap gap-3" aria-label="Điều hướng quản trị">
        <a href="${ctx}/admin/users" class="inline-flex min-h-11 items-center rounded-xl border border-white/10 bg-white/5 px-4 text-white hover:bg-white/10">Tài khoản</a>
        <a href="${ctx}/admin/configurations" class="inline-flex min-h-11 items-center rounded-xl border border-white/10 bg-white/5 px-4 text-white hover:bg-white/10">Cấu hình</a>
        <a href="${ctx}/admin/logs" class="inline-flex min-h-11 items-center rounded-xl border border-white/10 bg-white/5 px-4 text-white hover:bg-white/10">Nhật ký hệ thống</a>
    </nav>
    <h1 class="text-2xl font-bold text-white mb-2">Thông báo</h1>
    <p class="mb-6 text-gray-300">Thông báo được gửi đến tài khoản quản trị của bạn.</p>
    <c:if test="${not empty flashSuccess}"><p role="status" class="mb-5 rounded-xl border border-emerald-400/40 bg-emerald-950 p-4 text-emerald-200"><c:out value="${flashSuccess}"/></p></c:if>
    <ui:notification-list pageData="${notifications}" baseUrl="${inboxUrl}" markReadUrl="${inboxUrl}" csrfToken="${csrfToken}" emptyMessage="${emptyMessage}"/>
</main>
</body>
</html>
