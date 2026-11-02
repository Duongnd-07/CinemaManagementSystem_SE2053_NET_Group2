<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<!DOCTYPE html>
<html lang="vi" class="dark">
<head>
    <title>Hộp thư - CineAdmin</title>
    <jsp:include page="/WEB-INF/views/common/manager-head.jsp"/>
    <style>
        @media (max-width: 767px) {
            .uc09-inbox > aside { display: none; }
            .uc09-inbox > header { left: 0; padding: 0 16px; }
            .uc09-inbox > header > span { display: none; }
            .uc09-inbox > header > div { width: 100%; justify-content: space-between; gap: 8px; }
        }
    </style>
</head>
<body class="uc09-inbox bg-surface font-body-md text-on-surface min-h-screen">
<jsp:include page="/WEB-INF/views/common/manager-sidebar.jsp"/>
<main class="md:ml-64 pt-24 px-4 md:px-space-xl pb-16">
    <h1 class="text-headline-md font-bold mb-2">Hộp thư của tôi</h1>
    <p class="text-on-surface-variant mb-6">Thông báo được gửi đến tài khoản của bạn.</p>
    <c:if test="${not empty flashSuccess}"><p role="status" class="mb-5 rounded-xl border border-emerald-400/40 bg-emerald-950 p-4 text-emerald-200"><c:out value="${flashSuccess}"/></p></c:if>
    <ui:notification-list pageData="${notifications}" baseUrl="${inboxUrl}" markReadUrl="${inboxUrl}" csrfToken="${csrfToken}" emptyMessage="${emptyMessage}"/>
</main>
</body>
</html>
