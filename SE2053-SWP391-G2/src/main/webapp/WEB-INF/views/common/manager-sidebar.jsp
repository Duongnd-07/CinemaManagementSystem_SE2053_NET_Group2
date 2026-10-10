<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%-- Sidebar + thanh header dùng chung cho mọi chức năng quản lý. Truyền menu đang chọn qua param/attribute "activeMenu" --%>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="active" value="${not empty param.activeMenu ? param.activeMenu : activeMenu}"/>
<c:set var="itemBase" value="flex items-center px-space-md py-space-sm rounded-xl transition-all"/>
<c:set var="itemIdle" value="text-on-surface-variant hover:bg-surface-container-high hover:text-on-surface font-body-md"/>
<c:set var="itemActive" value="bg-primary-container text-on-primary-container font-label-lg shadow-[0_0_20px_-2px_rgba(229,9,20,0.45)]"/>

<aside class="fixed left-0 top-0 h-full w-64 bg-surface-container-low z-50 flex flex-col pt-space-lg pb-space-lg">
    <div class="px-gutter mb-space-xl flex items-center gap-space-sm">
        <div class="w-9 h-9 rounded-lg bg-primary flex items-center justify-center shadow-[0_0_20px_-2px_rgba(229,9,20,0.45)]">
            <span class="material-symbols-outlined text-on-primary text-[20px]">movie</span>
        </div>
        <span class="font-headline-sm text-on-surface tracking-tight">CINEADMIN</span>
    </div>
    <nav class="flex-1 px-space-md space-y-space-xs overflow-y-auto">
        <a class="${itemBase} ${active == 'dashboard' ? itemActive : itemIdle}" href="${ctx}/manager/dashboard">
            <span class="material-symbols-outlined mr-space-md">dashboard</span>Trang chủ
        </a>
        <a class="${itemBase} ${active == 'movies' ? itemActive : itemIdle}" href="${ctx}/manager/movies">
            <span class="material-symbols-outlined mr-space-md">movie</span>Phim
        </a>
        <a class="${itemBase} ${active == 'genres' ? itemActive : itemIdle}" href="${ctx}/manager/genres">
            <span class="material-symbols-outlined mr-space-md">category</span>Thể loại phim
        </a>
        <a class="${itemBase} ${active == 'showtimes' ? itemActive : itemIdle}" href="${ctx}/manager/showtimes">
            <span class="material-symbols-outlined mr-space-md">schedule</span>Suất chiếu
        </a>
        <a class="${itemBase} ${active == 'rooms' ? itemActive : itemIdle}" href="${ctx}/manager/rooms">
            <span class="material-symbols-outlined mr-space-md">meeting_room</span>Phòng chiếu
        </a>
        <a class="${itemBase} ${active == 'concessions' ? itemActive : itemIdle}" href="${ctx}/manager/concessions">
            <span class="material-symbols-outlined mr-space-md">local_convenience_store</span>Đồ ăn &amp; Thức uống
        </a>
        <a class="${itemBase} ${active == 'equipment-reports' ? itemActive : itemIdle}" href="${ctx}/manager/equipment-reports">
            <span class="material-symbols-outlined mr-space-md">build</span>Sự cố thiết bị
        </a>
        <a class="${itemBase} ${active == 'users' ? itemActive : itemIdle}" href="${ctx}/manager/users">
            <span class="material-symbols-outlined mr-space-md">group</span>Người dùng
        </a>
        <a class="${itemBase} ${active == 'reports' ? itemActive : itemIdle}" href="${ctx}/manager/reports">
            <span class="material-symbols-outlined mr-space-md">bar_chart</span>Báo cáo
        </a>
    </nav>
</aside>

<header class="fixed top-0 left-64 right-0 h-16 bg-surface/75 backdrop-blur-xl z-40 flex items-center justify-between px-space-xl">
    <span class="text-on-surface-variant font-label-lg">Hệ thống Quản lý Rạp</span>
    <div class="flex items-center gap-space-lg">
        <div class="flex items-center gap-space-sm">
            <div class="w-8 h-8 rounded-full bg-primary flex items-center justify-center">
                <span class="material-symbols-outlined text-on-primary text-[18px]">person</span>
            </div>
            <span class="text-on-surface font-label-lg">
                <c:out value="${not empty sessionScope.user ? sessionScope.user.fullName : 'Quản lý'}"/>
            </span>
        </div>
        <a class="flex items-center gap-space-xs text-on-surface-variant hover:text-primary transition-colors font-label-lg"
           href="${ctx}/logout">
            <span class="material-symbols-outlined text-[20px]">logout</span>Đăng xuất
        </a>
    </div>
</header>
