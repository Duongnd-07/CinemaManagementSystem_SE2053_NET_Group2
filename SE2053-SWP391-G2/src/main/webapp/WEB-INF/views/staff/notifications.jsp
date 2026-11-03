<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<!DOCTYPE html>
<html lang="vi" class="dark">
<head>
    <title>Thông báo - CineLuxe Staff</title>
    <jsp:include page="/WEB-INF/views/common/manager-head.jsp"/>
    <style>
        @media (max-width: 1279px) { .uc09-staff > header nav { display: none; } }
        @media (max-width: 639px) {
            .uc09-staff > header { height: auto; min-height: 64px; padding: 8px 16px; flex-wrap: wrap; gap: 8px; }
            .uc09-staff > header > div:last-child { gap: 8px; }
        }
    </style>
</head>
<body class="uc09-staff bg-surface font-body-md text-on-surface min-h-screen">
<jsp:include page="/WEB-INF/views/common/staff-header.jsp"/>
<main class="max-w-5xl mx-auto px-4 md:px-space-lg py-8">
    <a class="min-h-11 inline-flex items-center mb-4 text-primary underline" href="${pageContext.request.contextPath}/staff/dashboard">Bàn làm việc</a>
    <h1 class="text-headline-md font-bold mb-2">Thông báo</h1>
    <p class="text-on-surface-variant mb-6">Cập nhật từ quản lý và hệ thống.</p>
    <c:if test="${not empty flashSuccess}"><p role="status" class="mb-5 rounded-xl border border-emerald-400/40 bg-emerald-950 p-4 text-emerald-200"><c:out value="${flashSuccess}"/></p></c:if>
    <ui:notification-list pageData="${notifications}" baseUrl="${inboxUrl}" markReadUrl="${inboxUrl}" csrfToken="${csrfToken}" emptyMessage="${emptyMessage}"/>
</main>
</body>
</html>
