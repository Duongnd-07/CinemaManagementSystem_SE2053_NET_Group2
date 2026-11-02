<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="bellCount" value="${empty unreadNotificationCount ? 0 : unreadNotificationCount}"/>
<a href="<c:out value='${param.href}'/>" aria-label="Thông báo, ${bellCount} chưa đọc"
   class="relative inline-flex h-11 w-11 shrink-0 items-center justify-center rounded-xl text-gray-200 hover:bg-white/10 hover:text-white focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-white">
    <span class="material-symbols-outlined" aria-hidden="true">notifications</span>
    <c:if test="${bellCount > 0}">
        <span aria-hidden="true" class="absolute right-0 top-0 min-w-5 h-5 px-1 rounded-full bg-red-700 text-white text-xs font-bold flex items-center justify-center whitespace-nowrap">
            <c:out value="${bellCount > 9 ? '9+' : bellCount}"/>
        </span>
    </c:if>
</a>
