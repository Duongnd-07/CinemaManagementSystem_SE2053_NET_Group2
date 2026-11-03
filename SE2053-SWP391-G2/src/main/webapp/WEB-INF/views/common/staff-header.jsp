<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- Thanh đầu trang dùng chung cho các chức năng của nhân viên. Truyền mục đang chọn qua attribute "activeMenu" --%>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="navBase" value="flex items-center gap-1 px-space-sm py-space-sm rounded-lg font-label-lg text-sm whitespace-nowrap transition-all"/>
<c:set var="navIdle" value="text-on-surface-variant hover:bg-surface-container-high hover:text-on-surface"/>
<c:set var="navActive" value="bg-primary-container text-on-primary-container"/>

<header class="sticky top-0 z-40 h-16 bg-surface-container-low border-b border-white/10 px-space-lg flex items-center justify-between gap-space-sm">
    <div class="flex items-center gap-space-sm min-w-0">
        <a class="flex items-center gap-space-sm shrink-0" href="${ctx}/staff/dashboard">
            <span class="material-symbols-outlined text-[28px] text-primary-container">badge</span>
            <span class="hidden 2xl:inline font-headline-sm text-on-surface tracking-wide">CINELUXE</span>
            <span class="cl-pill">Staff</span>
        </a>
        <nav class="flex items-center gap-1 min-w-0 overflow-x-auto">
            <a class="${navBase} ${activeMenu == 'dashboard' ? navActive : navIdle}" href="${ctx}/staff/dashboard">
                <span class="material-symbols-outlined text-[20px]">home</span>Bàn làm việc
            </a>
            <c:if test="${allowedPermissions.contains('COUNTER_SALE')}">
            <a class="${navBase} ${activeMenu == 'counter' ? navActive : navIdle}" href="${ctx}/staff/counter">
                <span class="material-symbols-outlined text-[20px]">point_of_sale</span>Bán vé tại quầy
            </a>
            </c:if>
            <c:if test="${allowedPermissions.contains('OWN_COUNTER_HISTORY')}">
            <a class="${navBase} ${activeMenu == 'counter-history' ? navActive : navIdle}" href="${ctx}/staff/counter-history">
                <span class="material-symbols-outlined text-[20px]">receipt_long</span>Lịch sử bán vé
            </a>
            </c:if>
            <c:if test="${allowedPermissions.contains('OWN_SHIFTS')}">
            <a class="${navBase} ${activeMenu == 'shifts' ? navActive : navIdle}" href="${ctx}/staff/shifts">
                <span class="material-symbols-outlined text-[20px]">schedule</span>Ca làm việc
            </a>
            </c:if>
            <c:if test="${allowedPermissions.contains('SEAT_EXCHANGE')}">
            <a class="${navBase} ${activeMenu == 'seat-exchange' ? navActive : navIdle}" href="${ctx}/staff/seat-exchange">
                <span class="material-symbols-outlined text-[20px]">event_seat</span>Đổi ghế hỏng
            </a>
            </c:if>
            <c:if test="${allowedPermissions.contains('REPORT_EQUIPMENT')}">
            <a class="${navBase} ${activeMenu == 'equipment-reports' ? navActive : navIdle}" href="${ctx}/staff/equipment-reports">
                <span class="material-symbols-outlined text-[20px]">build</span>Sự cố thiết bị
            </a>
            </c:if>
            <c:if test="${not empty managementHome}">
                <a class="${navBase} ${navIdle}" href="${ctx}${managementHome}">
                    <span class="material-symbols-outlined text-[20px]">manage_accounts</span>Quản lý
                </a>
            </c:if>
        </nav>
    </div>
    <div class="flex items-center gap-space-sm shrink-0">
        <div class="flex items-center gap-space-sm">
            <div class="hidden 2xl:flex w-8 h-8 rounded-full bg-primary items-center justify-center">
                <span class="material-symbols-outlined text-on-primary text-[18px]">person</span>
            </div>
            <span class="hidden 2xl:inline text-on-surface font-label-lg whitespace-nowrap"><c:out value="${sessionScope.user.fullName}"/></span>
        </div>
        <a class="flex items-center gap-space-xs text-on-surface-variant hover:text-primary transition-colors font-label-lg whitespace-nowrap"
           href="${ctx}/logout" title="Đăng xuất" aria-label="Đăng xuất">
            <span class="material-symbols-outlined text-[20px]">logout</span><span class="hidden 2xl:inline">Đăng xuất</span>
        </a>
    </div>
</header>
