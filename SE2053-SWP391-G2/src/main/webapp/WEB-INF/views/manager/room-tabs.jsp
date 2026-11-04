<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- UC-25: hai tab Phòng chiếu / Phòng mẫu. Tham số "active": rooms | templates --%>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="tabBase" value="inline-flex items-center gap-1.5 px-space-md py-2 -mb-px border-b-2 font-label-lg transition-colors"/>
<nav class="flex items-center gap-space-sm border-b border-white/10 mb-space-lg" aria-label="Phòng chiếu và phòng mẫu">
    <a class="${tabBase} ${param.active == 'rooms' ? 'border-primary text-on-surface' : 'border-transparent text-on-surface-variant hover:text-on-surface'}"
       href="${ctx}/manager/rooms" ${param.active == 'rooms' ? 'aria-current="page"' : ''}>
        <span class="material-symbols-outlined text-[18px]">meeting_room</span>Phòng chiếu
    </a>
    <c:if test="${allowedPermissions.contains('MANAGE_ROOM_TEMPLATES')}">
    <a class="${tabBase} ${param.active == 'templates' ? 'border-primary text-on-surface' : 'border-transparent text-on-surface-variant hover:text-on-surface'}"
       href="${ctx}/manager/room-templates" ${param.active == 'templates' ? 'aria-current="page"' : ''}>
        <span class="material-symbols-outlined text-[18px]">bookmarks</span>Phòng mẫu
    </a>
    </c:if>
</nav>
