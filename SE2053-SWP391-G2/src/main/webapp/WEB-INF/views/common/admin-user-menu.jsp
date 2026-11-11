<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- Menu tài khoản Admin: bấm vào tên Admin trên thanh đầu trang để mở danh sách chức năng.
     Truyền mục đang mở qua <jsp:param name="active" value="users|permissions|configurations|logs"/>.
     Thông báo không nằm trong menu vì đã có chuông thông báo trên thanh đầu trang. --%>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="active" value="${param.active}"/>
<c:set var="itemBase" value="flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm transition focus-visible:outline focus-visible:outline-2 focus-visible:outline-white"/>
<c:set var="itemIdle" value="text-[#c4c7d4] hover:text-white hover:bg-white/5"/>
<c:set var="itemActive" value="text-white bg-white/10"/>

<div class="relative" data-admin-menu>
    <button type="button" data-admin-menu-toggle aria-haspopup="true" aria-expanded="false" aria-controls="adminUserMenuPanel"
            class="flex items-center gap-2.5 px-3 py-1.5 rounded-xl bg-white/5 hover:bg-white/10 border border-white/10 transition focus-visible:outline focus-visible:outline-2 focus-visible:outline-white">
        <span class="material-symbols-outlined text-[#ffb95f] text-lg" aria-hidden="true">shield_person</span>
        <span class="hidden sm:inline text-xs font-semibold text-white">
            <c:out value="${sessionScope.user.fullName != null ? sessionScope.user.fullName : 'Quản trị viên'}"/>
        </span>
        <span class="text-[10px] bg-red-950/80 border border-red-500/40 text-red-300 px-1.5 py-0.5 rounded font-bold">Admin</span>
        <span class="material-symbols-outlined text-base text-[#8e919f] transition-transform" data-admin-menu-caret aria-hidden="true">expand_more</span>
    </button>

    <div id="adminUserMenuPanel" data-admin-menu-panel hidden
         class="absolute right-0 mt-2 w-64 max-w-[calc(100vw-2rem)] rounded-2xl border border-white/10 bg-[#171b26] p-2 shadow-2xl z-50">
        <div class="px-3 pt-2 pb-3 mb-1 border-b border-white/10">
            <div class="text-sm font-bold text-white truncate">
                <c:out value="${sessionScope.user.fullName != null ? sessionScope.user.fullName : 'Quản trị viên'}"/>
            </div>
            <div class="text-xs text-[#8e919f] truncate"><c:out value="${sessionScope.user.email}"/></div>
        </div>

        <nav aria-label="Chức năng quản trị" class="flex flex-col gap-0.5">
            <a href="${ctx}/admin/users" class="${itemBase} ${active == 'users' ? itemActive : itemIdle}" ${active == 'users' ? 'aria-current="page"' : ''}>
                <span class="material-symbols-outlined text-lg" aria-hidden="true">group</span>Quản lý tài khoản
            </a>
            <a href="${ctx}/admin/permissions" class="${itemBase} ${active == 'permissions' ? itemActive : itemIdle}" ${active == 'permissions' ? 'aria-current="page"' : ''}>
                <span class="material-symbols-outlined text-lg" aria-hidden="true">key</span>Phân quyền
            </a>
            <a href="${ctx}/admin/configurations" class="${itemBase} ${active == 'configurations' ? itemActive : itemIdle}" ${active == 'configurations' ? 'aria-current="page"' : ''}>
                <span class="material-symbols-outlined text-lg" aria-hidden="true">tune</span>Cấu hình hệ thống
            </a>
            <a href="${ctx}/admin/logs" class="${itemBase} ${active == 'logs' ? itemActive : itemIdle}" ${active == 'logs' ? 'aria-current="page"' : ''}>
                <span class="material-symbols-outlined text-lg" aria-hidden="true">receipt_long</span>Nhật ký hệ thống
            </a>
        </nav>

        <div class="mt-1 pt-1 border-t border-white/10">
            <a href="${ctx}/logout" class="${itemBase} text-red-300 hover:text-white hover:bg-red-500/15">
                <span class="material-symbols-outlined text-lg" aria-hidden="true">logout</span>Đăng xuất
            </a>
        </div>
    </div>
</div>

<script>
    (function () {
        var root = document.currentScript.previousElementSibling;
        var toggle = root.querySelector('[data-admin-menu-toggle]');
        var panel = root.querySelector('[data-admin-menu-panel]');
        var caret = root.querySelector('[data-admin-menu-caret]');

        function setOpen(open) {
            panel.hidden = !open;
            toggle.setAttribute('aria-expanded', open ? 'true' : 'false');
            caret.style.transform = open ? 'rotate(180deg)' : '';
        }

        toggle.addEventListener('click', function (e) {
            e.stopPropagation();
            setOpen(panel.hidden);
        });
        document.addEventListener('click', function (e) {
            if (!panel.hidden && !root.contains(e.target)) setOpen(false);
        });
        document.addEventListener('keydown', function (e) {
            if (e.key === 'Escape' && !panel.hidden) {
                setOpen(false);
                toggle.focus();
            }
        });
    })();
</script>
