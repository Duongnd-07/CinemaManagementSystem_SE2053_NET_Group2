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
        <c:if test="${sessionScope.user.role == 'Staff'}">
            <a class="${itemBase} ${itemIdle}" href="${ctx}/staff/dashboard">
                <span class="material-symbols-outlined mr-space-md">arrow_back</span>Trang nhân viên
            </a>
        </c:if>
        <c:if test="${sessionScope.user.role == 'Manager'}">
            <a class="${itemBase} ${active == 'dashboard' ? itemActive : itemIdle}" href="${ctx}/manager/movies">
                <span class="material-symbols-outlined mr-space-md">dashboard</span>Trang chủ
            </a>
        </c:if>
        <c:if test="${allowedPermissions.contains('MANAGE_MOVIES')}">
        <a class="${itemBase} ${active == 'movies' ? itemActive : itemIdle}" href="${ctx}/manager/movies">
            <span class="material-symbols-outlined mr-space-md">movie</span>Phim
        </a>
        </c:if>
        <c:if test="${allowedPermissions.contains('MANAGE_GENRES')}">
        <a class="${itemBase} ${active == 'genres' ? itemActive : itemIdle}" href="${ctx}/manager/genres">
            <span class="material-symbols-outlined mr-space-md">category</span>Thể loại phim
        </a>
        </c:if>
        <c:if test="${allowedPermissions.contains('MANAGE_SHOWTIMES')}">
        <a class="${itemBase} ${active == 'showtimes' ? itemActive : itemIdle}" href="${ctx}/manager/showtimes">
            <span class="material-symbols-outlined mr-space-md">schedule</span>Suất chiếu
        </a>
        </c:if>
        <c:if test="${allowedPermissions.contains('MANAGE_ROOMS')}">
        <a class="${itemBase} ${active == 'rooms' ? itemActive : itemIdle}" href="${ctx}/manager/rooms">
            <span class="material-symbols-outlined mr-space-md">meeting_room</span>Phòng chiếu
        </a>
        </c:if>
        <c:if test="${allowedPermissions.contains('MANAGE_CONCESSIONS')}">
        <a class="${itemBase} ${active == 'concessions' ? itemActive : itemIdle}" href="${ctx}/manager/concessions">
            <span class="material-symbols-outlined mr-space-md">local_convenience_store</span>Đồ ăn &amp; Thức uống
        </a>
        </c:if>
        <c:if test="${allowedPermissions.contains('MANAGE_VOUCHERS')}">
        <a class="${itemBase} ${active == 'vouchers' ? itemActive : itemIdle}" href="${ctx}/manager/vouchers">
            <span class="material-symbols-outlined mr-space-md">confirmation_number</span>Voucher &amp; khuyến mãi
        </a>
        </c:if>
        <c:if test="${allowedPermissions.contains('MANAGE_EQUIPMENT_REPORTS')}">
        <a class="${itemBase} ${active == 'equipment-reports' ? itemActive : itemIdle}" href="${ctx}/manager/equipment-reports">
            <span class="material-symbols-outlined mr-space-md">build</span>Sự cố thiết bị
        </a>
        </c:if>
        <a class="${itemBase} ${active == 'room-inspections' ? itemActive : itemIdle}" href="${ctx}/manager/room-inspections">
            <span class="material-symbols-outlined mr-space-md">cleaning_services</span>Kiểm tra phòng chiếu
        </a>
        <c:if test="${allowedPermissions.contains('MANAGE_REVIEWS')}">
        <a class="${itemBase} ${active == 'reviews' ? itemActive : itemIdle}" href="${ctx}/manager/reviews">
            <span class="material-symbols-outlined mr-space-md">reviews</span>Đánh giá phim
        </a>
        </c:if>
        <a class="${itemBase} min-h-11 ${active == 'announcements' ? itemActive : itemIdle}" href="${ctx}/manager/announcements">
            <span class="material-symbols-outlined mr-space-md" aria-hidden="true">campaign</span>Gửi thông báo
        </a>
        <c:if test="${allowedPermissions.contains('VIEW_SHIFT_REPORTS')}">
        <a class="${itemBase} ${active == 'shifts' ? itemActive : itemIdle}" href="${ctx}/manager/shifts">
            <span class="material-symbols-outlined mr-space-md">schedule</span>Báo cáo ca
        </a>
        </c:if>
        <c:if test="${allowedPermissions.contains('VIEW_COUNTER_HISTORY')}">
        <a class="${itemBase} ${active == 'counter-history' ? itemActive : itemIdle}" href="${ctx}/manager/counter-history">
            <span class="material-symbols-outlined mr-space-md">receipt_long</span>Lịch sử bán tại quầy
        </a>
        </c:if>
        <c:if test="${allowedPermissions.contains('VIEW_REPORTS')}">
        <a class="${itemBase} ${active == 'reports' ? itemActive : itemIdle}" href="${ctx}/manager/reports">
            <span class="material-symbols-outlined mr-space-md">bar_chart</span>Báo cáo
        </a>
        </c:if>
    </nav>
</aside>

<header class="fixed top-0 left-64 right-0 h-16 bg-surface/75 backdrop-blur-xl z-40 flex items-center justify-between px-space-xl">
    <span class="text-on-surface-variant font-label-lg">Hệ thống Quản lý Rạp</span>
    <div class="flex items-center gap-space-lg">
        <jsp:include page="/WEB-INF/views/common/notification-bell.jsp">
            <jsp:param name="href" value="${ctx}/manager/notifications"/>
        </jsp:include>
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
