<%@ tag body-content="empty" pageEncoding="UTF-8" import="vn.edu.fpt.model.NotificationCategory,java.time.format.DateTimeFormatter" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%@ attribute name="pageData" required="true" type="vn.edu.fpt.util.Page" %>
<%@ attribute name="baseUrl" required="true" %>
<%@ attribute name="markReadUrl" required="true" %>
<%@ attribute name="csrfToken" required="true" %>
<%@ attribute name="emptyMessage" required="true" %>
<style>
    .uc09-list { min-width: 0; overflow-wrap: anywhere; }
    .uc09-list :is(a, button) { min-height: 44px; min-width: 44px; }
    .uc09-list :is(a, button):focus-visible { outline: 2px solid white; outline-offset: 3px; }
    .uc09-list .uc09-pagination > div { flex-wrap: wrap; gap: 16px; background: transparent; color: #d1d5db; }
    .uc09-list .uc09-pagination > div > div:last-child { flex-wrap: wrap; }
    .uc09-list .uc09-pagination a, .uc09-list .uc09-pagination span { display: inline-flex; align-items: center; justify-content: center; color: white; background: rgb(255 255 255 / .05); }
    .uc09-list .uc09-pagination .font-bold { background: #b80710; color: white; }
</style>
<section class="uc09-list space-y-4" aria-label="Danh sách thông báo">
    <c:choose>
        <c:when test="${empty pageData.items}">
            <div class="rounded-xl border border-white/10 bg-white/5 px-5 py-12 text-center">
                <span class="material-symbols-outlined text-4xl text-gray-300" aria-hidden="true">notifications_none</span>
                <p class="mt-4 text-white"><c:out value="${emptyMessage}"/></p>
            </div>
        </c:when>
        <c:otherwise>
            <form action="<c:out value='${markReadUrl}'/>" method="post" class="flex justify-end">
                <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}'/>"/>
                <input type="hidden" name="action" value="markAllRead"/>
                <button type="submit" class="rounded-lg border border-white/10 bg-white/5 px-4 py-2 text-sm text-white hover:bg-white/10">
                    Đánh dấu tất cả đã đọc
                </button>
            </form>
            <c:forEach var="notification" items="${pageData.items}">
                <article class="rounded-xl border border-white/10 bg-white/5 p-5">
                    <div class="flex flex-wrap items-center justify-between gap-3 text-sm text-gray-300">
                        <span><c:out value="${NotificationCategory.labelOf(notification.notificationType)}"/></span>
                        <span class="rounded-full border border-white/10 px-3 py-1 text-white">
                            <c:out value="${notification.read ? 'Đã đọc' : '● Mới'}"/>
                        </span>
                    </div>
                    <h2 class="mt-3 font-bold text-white"><c:out value="${notification.title}"/></h2>
                    <p class="mt-2 whitespace-pre-line text-gray-300"><c:out value="${notification.message}"/></p>
                    <time class="mt-4 block text-sm text-gray-300" datetime="<c:out value='${notification.createdAt}'/>">
                        <c:out value="${notification.createdAt.format(DateTimeFormatter.ofPattern('dd/MM/yyyy HH:mm'))}"/>
                    </time>
                </article>
            </c:forEach>
            <div class="uc09-pagination rounded-xl border border-white/10 bg-white/5">
                <ui:pagination pageData="${pageData}" baseUrl="${baseUrl}" label="thông báo"/>
            </div>
        </c:otherwise>
    </c:choose>
</section>
