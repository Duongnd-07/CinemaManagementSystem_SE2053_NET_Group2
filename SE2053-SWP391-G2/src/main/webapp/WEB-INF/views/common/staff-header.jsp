<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- Thanh đầu trang dùng chung cho các chức năng của nhân viên. Truyền mục đang chọn qua attribute "activeMenu" --%>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="navBase" value="flex items-center gap-space-xs px-space-md py-space-sm rounded-lg font-label-lg transition-all"/>
<c:set var="navIdle" value="text-on-surface-variant hover:bg-surface-container-high hover:text-on-surface"/>
<c:set var="navActive" value="bg-primary-container text-on-primary-container"/>

<header class="sticky top-0 z-40 h-16 bg-surface-container-low border-b border-white/10 px-space-lg flex items-center justify-between">
    <div class="flex items-center gap-space-lg">
        <a class="flex items-center gap-space-sm" href="${ctx}/staff/dashboard">
            <span class="material-symbols-outlined text-[28px] text-primary-container">badge</span>
            <span class="font-headline-sm text-on-surface tracking-wide">CINELUXE</span>
            <span class="cl-pill">Staff</span>
        </a>
        <nav class="flex items-center gap-space-xs">
            <a class="${navBase} ${activeMenu == 'dashboard' ? navActive : navIdle}" href="${ctx}/staff/dashboard">
                <span class="material-symbols-outlined text-[20px]">home</span>Bàn làm việc
            </a>
            <a class="${navBase} ${activeMenu == 'counter' ? navActive : navIdle}" href="${ctx}/staff/counter">
                <span class="material-symbols-outlined text-[20px]">point_of_sale</span>Bán vé tại quầy
            </a>
            <a class="${navBase} ${activeMenu == 'shifts' ? navActive : navIdle}" href="${ctx}/staff/shifts">
                <span class="material-symbols-outlined text-[20px]">schedule</span>Ca làm việc
            </a>
            <a class="${navBase} ${activeMenu == 'equipment-reports' ? navActive : navIdle}" href="${ctx}/staff/equipment-reports">
                <span class="material-symbols-outlined text-[20px]">build</span>Sự cố thiết bị
            </a>
        </nav>
    </div>
    <div class="flex items-center gap-space-lg">
        <div class="flex items-center gap-space-sm">
            <div class="w-8 h-8 rounded-full bg-primary flex items-center justify-center">
                <span class="material-symbols-outlined text-on-primary text-[18px]">person</span>
            </div>
            <span class="text-on-surface font-label-lg"><c:out value="${sessionScope.user.fullName}"/></span>
        </div>
        <a class="flex items-center gap-space-xs text-on-surface-variant hover:text-primary transition-colors font-label-lg"
           href="${ctx}/logout">
            <span class="material-symbols-outlined text-[20px]">logout</span>Đăng xuất
        </a>
    </div>
</header>
