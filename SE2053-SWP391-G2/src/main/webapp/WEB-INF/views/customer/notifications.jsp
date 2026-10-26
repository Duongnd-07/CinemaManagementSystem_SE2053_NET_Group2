<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="vi" class="dark">
<head>
    <title>Thông báo - CineLuxe Cinema</title>
    <jsp:include page="/WEB-INF/views/common/public-head.jsp"/>
</head>
<body class="min-h-screen bg-[#0f131d] text-[#dfe2f1] flex flex-col">
    <jsp:include page="/WEB-INF/views/common/public-header.jsp"/>
    <main class="flex-1 max-w-5xl w-full mx-auto px-4 lg:px-8 py-10">
        <div class="flex flex-wrap items-end justify-between gap-4 mb-8">
            <div>
                <p class="text-sm text-[#e9bcb6]">Cập nhật tài khoản</p>
                <h1 class="text-3xl font-bold text-white">Thông báo</h1>
            </div>
            <c:if test="${not empty notifications.items}">
                <form action="${ctx}/customer/notifications" method="post">
                    <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}'/>"/>
                    <button class="rounded-lg border border-white/10 px-4 py-2 text-sm text-gray-200 hover:bg-white/10">
                        Đánh dấu tất cả đã đọc
                    </button>
                </form>
            </c:if>
        </div>
        <c:if test="${not empty flashSuccess}">
            <p role="status" class="mb-5 rounded-lg border border-emerald-500/30 bg-emerald-950/40 p-4 text-emerald-200"><c:out value="${flashSuccess}"/></p>
        </c:if>
        <c:choose>
            <c:when test="${empty notifications.items}">
                <div class="rounded-2xl border border-white/10 bg-[#171b26] p-12 text-center">
                    <span class="material-symbols-outlined text-5xl text-[#e50914]">notifications_none</span>
                    <p class="mt-4 text-lg font-semibold text-white"><c:out value="${emptyMessage}"/></p>
                </div>
            </c:when>
            <c:otherwise>
                <div class="space-y-3">
                    <c:forEach var="notification" items="${notifications.items}">
                        <article class="rounded-xl border ${notification.read ? 'border-white/5 bg-[#171b26]/60' : 'border-[#e50914]/30 bg-[#171b26]'} p-5">
                            <div class="flex items-start justify-between gap-4">
                                <div class="min-w-0">
                                    <h2 class="font-bold text-white"><c:out value="${notification.title}"/></h2>
                                    <p class="mt-2 whitespace-pre-line text-sm text-gray-300"><c:out value="${notification.message}"/></p>
                                    <p class="mt-3 text-xs text-gray-500"><c:out value="${notification.createdAt}"/></p>
                                </div>
                                <span class="shrink-0 rounded-full px-3 py-1 text-xs ${notification.read ? 'bg-white/10 text-gray-400' : 'bg-[#e50914]/20 text-[#ffb4ab]'}">
                                    <c:out value="${notification.read ? 'Đã đọc' : 'Chưa đọc'}"/>
                                </span>
                            </div>
                        </article>
                    </c:forEach>
                </div>
                <div class="mt-6 flex justify-end gap-5 text-sm">
                    <c:if test="${notifications.page > 1}"><a class="text-[#ffb4ab]" href="${ctx}/customer/notifications?page=${notifications.page - 1}">← Trang trước</a></c:if>
                    <span class="text-gray-400">Trang <c:out value="${notifications.page}"/> / <c:out value="${notifications.totalPages}"/></span>
                    <c:if test="${notifications.page < notifications.totalPages}"><a class="text-[#ffb4ab]" href="${ctx}/customer/notifications?page=${notifications.page + 1}">Trang sau →</a></c:if>
                </div>
            </c:otherwise>
        </c:choose>
    </main>
    <jsp:include page="/WEB-INF/views/common/public-footer.jsp"/>
</body>
</html>
