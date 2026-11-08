<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%-- Sidebar + thanh header dùng chung cho mọi chức năng quản lý. Truyền menu đang chọn qua param/attribute "activeMenu" --%>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="active" value="${not empty param.activeMenu ? param.activeMenu : activeMenu}"/>
<c:set var="itemBase" value="flex items-center px-space-md py-space-sm rounded-xl transition-all"/>
<c:set var="itemIdle" value="text-on-surface-variant hover:bg-surface-container-high hover:text-on-surface font-body-md"/>
<c:set var="itemActive" value="bg-primary-container text-on-primary-container font-label-lg shadow-[0_0_20px_-2px_rgba(229,9,20,0.45)]"/>

<aside class="manager-sidebar fixed left-0 top-0 h-full w-64 bg-surface-container-low z-50 flex flex-col pt-space-lg pb-space-lg">
    <div class="manager-sidebar-brand px-gutter mb-space-xl flex items-center gap-space-sm">
        <div class="w-9 h-9 rounded-lg bg-primary flex items-center justify-center shadow-[0_0_20px_-2px_rgba(229,9,20,0.45)]">
            <span class="material-symbols-outlined text-on-primary text-[20px]">movie</span>
        </div>
        <span class="font-headline-sm text-on-surface tracking-tight">CINEADMIN</span>
    </div>
    <nav class="manager-nav flex-1 px-space-md overflow-y-auto" aria-label="Điều hướng quản lý">
        <c:if test="${sessionScope.user.role == 'Staff'}">
            <a class="manager-nav-link ${itemBase} ${itemIdle}" href="${ctx}/staff/dashboard">
                <span class="material-symbols-outlined mr-space-md">arrow_back</span>Trang nhân viên
            </a>
        </c:if>
        <c:if test="${sessionScope.user.role == 'Manager'}">
            <a class="manager-nav-link ${itemBase} ${active == 'dashboard' ? itemActive : itemIdle}" href="${ctx}/manager/movies">
                <span class="material-symbols-outlined mr-space-md">dashboard</span>Trang chủ
            </a>
        </c:if>
        <c:if test="${allowedPermissions.contains('MANAGE_MOVIES') or allowedPermissions.contains('MANAGE_GENRES') or allowedPermissions.contains('MANAGE_SHOWTIMES') or allowedPermissions.contains('MANAGE_REVIEWS')}">
            <details class="manager-nav-group" name="manager-navigation" ${active == 'movies' or active == 'genres' or active == 'showtimes' or active == 'reviews' ? 'open' : ''}>
                <summary class="manager-nav-summary text-on-surface-variant hover:bg-surface-container-high hover:text-on-surface font-label-lg">
                    <span class="material-symbols-outlined" aria-hidden="true">movie_filter</span>
                    <span class="manager-nav-label">Phim &amp; lịch chiếu</span>
                </summary>
                <div class="manager-nav-children border-l border-outline-variant">
                    <c:if test="${allowedPermissions.contains('MANAGE_MOVIES')}">
                        <a class="manager-nav-link ${itemBase} ${active == 'movies' ? itemActive : itemIdle}" href="${ctx}/manager/movies" aria-current="${active == 'movies' ? 'page' : 'false'}">
                            <span class="material-symbols-outlined mr-space-md" aria-hidden="true">movie</span>Phim
                        </a>
                    </c:if>
                    <c:if test="${allowedPermissions.contains('MANAGE_GENRES')}">
                        <a class="manager-nav-link ${itemBase} ${active == 'genres' ? itemActive : itemIdle}" href="${ctx}/manager/genres" aria-current="${active == 'genres' ? 'page' : 'false'}">
                            <span class="material-symbols-outlined mr-space-md" aria-hidden="true">category</span>Thể loại phim
                        </a>
                    </c:if>
                    <c:if test="${allowedPermissions.contains('MANAGE_SHOWTIMES')}">
                        <a class="manager-nav-link ${itemBase} ${active == 'showtimes' ? itemActive : itemIdle}" href="${ctx}/manager/showtimes" aria-current="${active == 'showtimes' ? 'page' : 'false'}">
                            <span class="material-symbols-outlined mr-space-md" aria-hidden="true">schedule</span>Suất chiếu
                        </a>
                    </c:if>
                    <c:if test="${allowedPermissions.contains('MANAGE_REVIEWS')}">
                        <a class="manager-nav-link ${itemBase} ${active == 'reviews' ? itemActive : itemIdle}" href="${ctx}/manager/reviews" aria-current="${active == 'reviews' ? 'page' : 'false'}">
                            <span class="material-symbols-outlined mr-space-md" aria-hidden="true">reviews</span>Đánh giá phim
                        </a>
                    </c:if>
                </div>
            </details>
        </c:if>
        <c:if test="${allowedPermissions.contains('MANAGE_ROOMS') or allowedPermissions.contains('VIEW_ROOM_INSPECTIONS') or allowedPermissions.contains('VIEW_ROOM_CONDITIONS') or allowedPermissions.contains('MANAGE_EQUIPMENT_REPORTS') or allowedPermissions.contains('VIEW_SHIFT_REPORTS')}">
            <details class="manager-nav-group" name="manager-navigation" ${active == 'rooms' or active == 'room-inspections' or active == 'room-conditions' or active == 'equipment-reports' or active == 'shifts' ? 'open' : ''}>
                <summary class="manager-nav-summary text-on-surface-variant hover:bg-surface-container-high hover:text-on-surface font-label-lg">
                    <span class="material-symbols-outlined" aria-hidden="true">settings</span>
                    <span class="manager-nav-label">Vận hành rạp</span>
                </summary>
                <div class="manager-nav-children border-l border-outline-variant">
                    <c:if test="${allowedPermissions.contains('MANAGE_ROOMS')}">
                        <a class="manager-nav-link ${itemBase} ${active == 'rooms' ? itemActive : itemIdle}" href="${ctx}/manager/rooms" aria-current="${active == 'rooms' ? 'page' : 'false'}">
                            <span class="material-symbols-outlined mr-space-md" aria-hidden="true">meeting_room</span>Phòng chiếu
                        </a>
                    </c:if>
                    <c:if test="${allowedPermissions.contains('VIEW_ROOM_INSPECTIONS')}">
                        <a class="manager-nav-link ${itemBase} ${active == 'room-inspections' ? itemActive : itemIdle}" href="${ctx}/manager/room-inspections" aria-current="${active == 'room-inspections' ? 'page' : 'false'}">
                            <span class="material-symbols-outlined mr-space-md" aria-hidden="true">cleaning_services</span>Kiểm tra phòng chiếu
                        </a>
                    </c:if>
                    <c:if test="${allowedPermissions.contains('VIEW_ROOM_CONDITIONS')}">
                        <a class="manager-nav-link ${itemBase} ${active == 'room-conditions' ? itemActive : itemIdle}" href="${ctx}/manager/room-conditions" aria-current="${active == 'room-conditions' ? 'page' : 'false'}">
                            <span class="material-symbols-outlined mr-space-md" aria-hidden="true">fact_check</span>Tình trạng phòng
                        </a>
                    </c:if>
                    <c:if test="${allowedPermissions.contains('MANAGE_EQUIPMENT_REPORTS')}">
                        <a class="manager-nav-link ${itemBase} ${active == 'equipment-reports' ? itemActive : itemIdle}" href="${ctx}/manager/equipment-reports" aria-current="${active == 'equipment-reports' ? 'page' : 'false'}">
                            <span class="material-symbols-outlined mr-space-md" aria-hidden="true">build</span>Sự cố thiết bị
                        </a>
                    </c:if>
                    <c:if test="${allowedPermissions.contains('VIEW_SHIFT_REPORTS')}">
                        <a class="manager-nav-link ${itemBase} ${active == 'shifts' ? itemActive : itemIdle}" href="${ctx}/manager/shifts" aria-current="${active == 'shifts' ? 'page' : 'false'}">
                            <span class="material-symbols-outlined mr-space-md" aria-hidden="true">schedule</span>Báo cáo ca
                        </a>
                    </c:if>
                </div>
            </details>
        </c:if>
        <c:if test="${allowedPermissions.contains('MANAGE_CONCESSIONS') or allowedPermissions.contains('MANAGE_VOUCHERS')}">
            <details class="manager-nav-group" name="manager-navigation" ${active == 'concessions' or active == 'vouchers' ? 'open' : ''}>
                <summary class="manager-nav-summary text-on-surface-variant hover:bg-surface-container-high hover:text-on-surface font-label-lg">
                    <span class="material-symbols-outlined" aria-hidden="true">local_offer</span>
                    <span class="manager-nav-label">Dịch vụ &amp; khuyến mãi</span>
                </summary>
                <div class="manager-nav-children border-l border-outline-variant">
                    <c:if test="${allowedPermissions.contains('MANAGE_CONCESSIONS')}">
                        <a class="manager-nav-link ${itemBase} ${active == 'concessions' ? itemActive : itemIdle}" href="${ctx}/manager/concessions" aria-current="${active == 'concessions' ? 'page' : 'false'}">
                            <span class="material-symbols-outlined mr-space-md" aria-hidden="true">local_convenience_store</span>Đồ ăn &amp; Thức uống
                        </a>
                    </c:if>
                    <c:if test="${allowedPermissions.contains('MANAGE_VOUCHERS')}">
                        <a class="manager-nav-link ${itemBase} ${active == 'vouchers' ? itemActive : itemIdle}" href="${ctx}/manager/vouchers" aria-current="${active == 'vouchers' ? 'page' : 'false'}">
                            <span class="material-symbols-outlined mr-space-md" aria-hidden="true">confirmation_number</span>Voucher &amp; khuyến mãi
                        </a>
                    </c:if>
                </div>
            </details>
        </c:if>
        <c:if test="${allowedPermissions.contains('VIEW_COUNTER_HISTORY') or allowedPermissions.contains('VIEW_ONLINE_REPORTS') or allowedPermissions.contains('VIEW_REPORTS')}">
            <details class="manager-nav-group" name="manager-navigation" ${active == 'counter-history' or active == 'online-reports' or active == 'reports' ? 'open' : ''}>
                <summary class="manager-nav-summary text-on-surface-variant hover:bg-surface-container-high hover:text-on-surface font-label-lg">
                    <span class="material-symbols-outlined" aria-hidden="true">monitoring</span>
                    <span class="manager-nav-label">Báo cáo &amp; doanh thu</span>
                </summary>
                <div class="manager-nav-children border-l border-outline-variant">
                    <c:if test="${allowedPermissions.contains('VIEW_COUNTER_HISTORY')}">
                        <a class="manager-nav-link ${itemBase} ${active == 'counter-history' ? itemActive : itemIdle}" href="${ctx}/manager/counter-history" aria-current="${active == 'counter-history' ? 'page' : 'false'}">
                            <span class="material-symbols-outlined mr-space-md" aria-hidden="true">receipt_long</span>Lịch sử bán vé tại quầy
                        </a>
                    </c:if>
                    <c:if test="${allowedPermissions.contains('VIEW_ONLINE_REPORTS')}">
                        <a class="manager-nav-link ${itemBase} min-h-11 ${active == 'online-reports' ? itemActive : itemIdle}" href="${ctx}/manager/online-reports" aria-current="${active == 'online-reports' ? 'page' : 'false'}">
                            <span class="material-symbols-outlined mr-space-md" aria-hidden="true">payments</span>Lịch sử bán vé online
                        </a>
                    </c:if>
                    <c:if test="${allowedPermissions.contains('VIEW_REPORTS')}">
                        <a class="manager-nav-link ${itemBase} ${active == 'reports' ? itemActive : itemIdle}" href="${ctx}/manager/reports" aria-current="${active == 'reports' ? 'page' : 'false'}">
                            <span class="material-symbols-outlined mr-space-md" aria-hidden="true">bar_chart</span>Báo cáo tổng hợp
                        </a>
                    </c:if>
                </div>
            </details>
        </c:if>
        <c:if test="${allowedPermissions.contains('SEND_ANNOUNCEMENTS')}">
            <a class="manager-nav-link ${itemBase} min-h-11 ${active == 'announcements' ? itemActive : itemIdle}" href="${ctx}/manager/announcements" aria-current="${active == 'announcements' ? 'page' : 'false'}">
                <span class="material-symbols-outlined mr-space-md" aria-hidden="true">campaign</span>Gửi thông báo
            </a>
        </c:if>
    </nav>
</aside>

<header class="fixed top-0 left-64 right-0 h-16 bg-surface/75 backdrop-blur-xl z-40 flex items-center justify-between px-space-xl">
    <span class="text-on-surface-variant font-label-lg">Hệ thống Quản lý Rạp</span>
    <div class="flex items-center gap-space-lg">
        <jsp:include page="/WEB-INF/views/common/notification-bell.jsp">
            <jsp:param name="href" value="${ctx}${sessionScope.user.role == 'Staff' ? '/staff/notifications' : '/manager/notifications'}"/>
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
