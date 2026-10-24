<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="vi" class="dark">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Quản lý tài khoản & Phân quyền - CineLuxe Admin</title>
    <script src="https://cdn.tailwindcss.com"></script>
    <link href="https://fonts.googleapis.com/css2?family=Be+Vietnam+Pro:wght@400;500;600;700;800&display=swap" rel="stylesheet"/>
    <link href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@24,400,0,0" rel="stylesheet"/>
    <link href="${pageContext.request.contextPath}/assets/css/cineluxe.css" rel="stylesheet"/>
    <style>
        body {
            font-family: 'Be Vietnam Pro', sans-serif;
            background-color: #0c0f17;
            color: #dfe2f1;
        }
    </style>
</head>
<body class="min-h-screen bg-[#0c0f17] text-[#dfe2f1]">
    <!-- Top Navbar -->
    <header class="h-16 bg-[#131722]/90 backdrop-blur-md border-b border-white/10 sticky top-0 z-40 flex items-center justify-between px-4 sm:px-8 shadow-lg">
        <div class="flex items-center gap-3">
            <div class="w-9 h-9 rounded-xl bg-gradient-to-tr from-[#e50914] to-[#ff4d58] flex items-center justify-center shadow-[0_0_15px_rgba(229,9,20,0.5)]">
                <span class="material-symbols-outlined text-white text-xl">admin_panel_settings</span>
            </div>
            <div>
                <span class="text-lg font-extrabold tracking-wider text-white">CINE<span class="text-[#e50914]">ADMIN</span></span>
                <span class="text-[11px] text-[#8e919f] ml-2 px-2 py-0.5 rounded bg-white/5 border border-white/10 uppercase tracking-widest font-semibold">Quản trị tối cao</span>
            </div>
        </div>

        <div class="flex items-center gap-4">
            <a href="${pageContext.request.contextPath}/admin/configurations"
               class="flex items-center gap-1.5 text-xs text-[#8e919f] hover:text-white bg-white/5 hover:bg-white/10 border border-white/10 px-3 py-1.5 rounded-xl transition">
                <span class="material-symbols-outlined text-base">tune</span>
                <span>Cấu hình hệ thống</span>
            </a>
            <a href="${pageContext.request.contextPath}/admin/logs"
               class="flex items-center gap-1.5 text-xs text-[#8e919f] hover:text-white bg-white/5 hover:bg-white/10 border border-white/10 px-3 py-1.5 rounded-xl transition">
                <span class="material-symbols-outlined text-base">receipt_long</span>
                <span>Nhật ký hệ thống</span>
            </a>
            <div class="flex items-center gap-2.5 px-3 py-1.5 rounded-xl bg-white/5 border border-white/10">
                <span class="material-symbols-outlined text-[#ffb95f] text-lg">shield_person</span>
                <span class="text-xs font-semibold text-white">
                    <c:out value="${sessionScope.user.fullName != null ? sessionScope.user.fullName : 'Quản trị viên'}"/>
                </span>
                <span class="text-[10px] text-gray-400 bg-red-950/80 border border-red-500/40 text-red-300 px-1.5 py-0.5 rounded font-bold">Admin</span>
            </div>
            <a href="${pageContext.request.contextPath}/logout"
               class="flex items-center gap-1.5 text-xs text-[#8e919f] hover:text-[#e50914] bg-white/5 hover:bg-red-500/10 border border-white/10 hover:border-red-500/30 px-3 py-1.5 rounded-xl transition">
                <span class="material-symbols-outlined text-base">logout</span>
                <span>Đăng xuất</span>
            </a>
        </div>
    </header>

    <!-- Main Content Container -->
    <main class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">

        <!-- Page Title & Header Actions -->
        <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
            <div>
                <h1 class="text-2xl sm:text-3xl font-extrabold text-white tracking-tight flex items-center gap-3">
                    <span class="material-symbols-outlined text-[#e50914] text-3xl">manage_accounts</span>
                    Quản lý tài khoản &amp; Phân quyền
                </h1>
                <p class="text-xs sm:text-sm text-[#8e919f] mt-1">
                    Trang quản trị toàn quyền phân chia vai trò (Admin, Manager, Staff, Customer), cập nhật thông tin và kiểm soát trạng thái tài khoản.
                </p>
            </div>
            <div>
                <button type="button" onclick="openCreateUserModal()"
                        class="px-4 py-2.5 bg-gradient-to-r from-[#e50914] to-[#b80710] hover:from-[#f40d1a] hover:to-[#cb0812] text-white font-bold rounded-xl text-xs sm:text-sm shadow-[0_0_20px_-2px_rgba(229,9,20,0.5)] transition flex items-center gap-2">
                    <span class="material-symbols-outlined text-lg">person_add</span>
                    Thêm tài khoản mới
                </button>
            </div>
        </div>

        <!-- Flash Messages (Auto-dismiss in 5s) -->
        <c:if test="${not empty flashSuccess}">
            <div class="flash-alert p-4 bg-emerald-950/80 border border-emerald-500/40 rounded-xl text-emerald-300 text-sm flex items-center justify-between gap-3 shadow-lg transition-all duration-500 overflow-hidden">
                <div class="flex items-center gap-3">
                    <span class="material-symbols-outlined text-emerald-400 text-2xl">check_circle</span>
                    <span class="font-medium"><c:out value="${flashSuccess}"/></span>
                </div>
                <button type="button" onclick="dismissFlashAlert(this.closest('.flash-alert'))" class="text-emerald-400/70 hover:text-emerald-200 transition p-1 rounded-lg hover:bg-emerald-500/10" title="Đóng">
                    <span class="material-symbols-outlined text-lg">close</span>
                </button>
            </div>
        </c:if>
        <c:if test="${not empty flashError}">
            <div class="flash-alert p-4 bg-red-950/80 border border-red-500/40 rounded-xl text-red-300 text-sm flex items-center justify-between gap-3 shadow-lg transition-all duration-500 overflow-hidden">
                <div class="flex items-center gap-3">
                    <span class="material-symbols-outlined text-red-400 text-2xl">error</span>
                    <span class="font-medium"><c:out value="${flashError}"/></span>
                </div>
                <button type="button" onclick="dismissFlashAlert(this.closest('.flash-alert'))" class="text-red-400/70 hover:text-red-200 transition p-1 rounded-lg hover:bg-red-500/10" title="Đóng">
                    <span class="material-symbols-outlined text-lg">close</span>
                </button>
            </div>
        </c:if>

        <!-- Stats Overview Cards -->
        <div class="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-3.5">
            <!-- 1. Total -->
            <div class="bg-[#171b26] border border-white/10 rounded-2xl p-4 flex items-center gap-3.5">
                <div class="w-11 h-11 rounded-xl bg-white/5 border border-white/10 flex items-center justify-center text-white">
                    <span class="material-symbols-outlined text-2xl">group</span>
                </div>
                <div>
                    <div class="text-[11px] font-semibold uppercase tracking-wider text-gray-400">Tổng tài khoản</div>
                    <div class="text-xl font-extrabold text-white">${stats.Total != null ? stats.Total : 0}</div>
                </div>
            </div>

            <!-- 2. Admin -->
            <div class="bg-[#171b26] border border-red-500/20 rounded-2xl p-4 flex items-center gap-3.5">
                <div class="w-11 h-11 rounded-xl bg-red-500/10 border border-red-500/30 flex items-center justify-center text-red-400">
                    <span class="material-symbols-outlined text-2xl">shield_person</span>
                </div>
                <div>
                    <div class="text-[11px] font-semibold uppercase tracking-wider text-red-400">Admin</div>
                    <div class="text-xl font-extrabold text-white">${stats.Admin != null ? stats.Admin : 0}</div>
                </div>
            </div>

            <!-- 3. Manager -->
            <div class="bg-[#171b26] border border-amber-500/20 rounded-2xl p-4 flex items-center gap-3.5">
                <div class="w-11 h-11 rounded-xl bg-amber-500/10 border border-amber-500/30 flex items-center justify-center text-amber-400">
                    <span class="material-symbols-outlined text-2xl">manage_accounts</span>
                </div>
                <div>
                    <div class="text-[11px] font-semibold uppercase tracking-wider text-amber-400">Quản lý</div>
                    <div class="text-xl font-extrabold text-white">${stats.Manager != null ? stats.Manager : 0}</div>
                </div>
            </div>

            <!-- 4. Staff -->
            <div class="bg-[#171b26] border border-blue-500/20 rounded-2xl p-4 flex items-center gap-3.5">
                <div class="w-11 h-11 rounded-xl bg-blue-500/10 border border-blue-500/30 flex items-center justify-center text-blue-400">
                    <span class="material-symbols-outlined text-2xl">badge</span>
                </div>
                <div>
                    <div class="text-[11px] font-semibold uppercase tracking-wider text-blue-400">Nhân viên</div>
                    <div class="text-xl font-extrabold text-white">${stats.Staff != null ? stats.Staff : 0}</div>
                </div>
            </div>

            <!-- 5. Customer -->
            <div class="bg-[#171b26] border border-emerald-500/20 rounded-2xl p-4 flex items-center gap-3.5 col-span-2 sm:col-span-1">
                <div class="w-11 h-11 rounded-xl bg-emerald-500/10 border border-emerald-500/30 flex items-center justify-center text-emerald-400">
                    <span class="material-symbols-outlined text-2xl">person</span>
                </div>
                <div>
                    <div class="text-[11px] font-semibold uppercase tracking-wider text-emerald-400">Khách hàng</div>
                    <div class="text-xl font-extrabold text-white">${stats.Customer != null ? stats.Customer : 0}</div>
                </div>
            </div>
        </div>

        <!-- Search & Filter Toolbar -->
        <div class="bg-[#171b26] border border-white/10 rounded-2xl p-4 sm:p-5 shadow-xl">
            <form action="${pageContext.request.contextPath}/admin/users" method="GET" class="grid grid-cols-1 sm:grid-cols-12 gap-3.5 items-end">
                <!-- Search Keyword -->
                <div class="sm:col-span-5">
                    <label class="block text-xs font-semibold text-gray-300 uppercase tracking-wider mb-1.5">
                        Tìm kiếm người dùng
                    </label>
                    <div class="relative">
                        <span class="material-symbols-outlined absolute left-3.5 top-1/2 -translate-y-1/2 text-gray-400 text-lg">search</span>
                        <input type="text" name="keyword" value="<c:out value='${keyword}'/>"
                               placeholder="Tìm theo Họ tên, Email, Số điện thoại..."
                               class="w-full pl-10 pr-4 py-2 bg-[#0f131d] border border-white/10 rounded-xl text-white placeholder-gray-500 text-xs sm:text-sm focus:outline-none focus:border-[#e50914] transition"/>
                    </div>
                </div>

                <!-- Filter Role -->
                <div class="sm:col-span-3">
                    <label class="block text-xs font-semibold text-gray-300 uppercase tracking-wider mb-1.5">
                        Lọc theo vai trò
                    </label>
                    <select name="role"
                            class="w-full px-3 py-2 bg-[#0f131d] border border-white/10 rounded-xl text-white text-xs sm:text-sm focus:outline-none focus:border-[#e50914] transition">
                        <option value="ALL" ${selectedRole == 'ALL' ? 'selected' : ''}>-- Tất cả vai trò --</option>
                        <option value="Admin" ${selectedRole == 'Admin' ? 'selected' : ''}>Quản trị viên (Admin)</option>
                        <option value="Manager" ${selectedRole == 'Manager' ? 'selected' : ''}>Quản lý (Manager)</option>
                        <option value="Staff" ${selectedRole == 'Staff' ? 'selected' : ''}>Nhân viên (Staff)</option>
                        <option value="Customer" ${selectedRole == 'Customer' ? 'selected' : ''}>Khách hàng (Customer)</option>
                    </select>
                </div>

                <!-- Filter Status -->
                <div class="sm:col-span-2">
                    <label class="block text-xs font-semibold text-gray-300 uppercase tracking-wider mb-1.5">
                        Trạng thái
                    </label>
                    <select name="status"
                            class="w-full px-3 py-2 bg-[#0f131d] border border-white/10 rounded-xl text-white text-xs sm:text-sm focus:outline-none focus:border-[#e50914] transition">
                        <option value="ALL" ${selectedStatus == 'ALL' ? 'selected' : ''}>-- Tất cả --</option>
                        <option value="Active" ${selectedStatus == 'Active' ? 'selected' : ''}>Hoạt động</option>
                        <option value="Suspended" ${selectedStatus == 'Suspended' ? 'selected' : ''}>Tạm khóa</option>
                        <option value="Deactivated" ${selectedStatus == 'Deactivated' ? 'selected' : ''}>Đã vô hiệu hóa</option>
                    </select>
                </div>

                <!-- Submit & Reset Buttons -->
                <div class="sm:col-span-2 flex gap-2">
                    <button type="submit"
                            class="flex-1 py-2 bg-[#e50914] hover:bg-[#b80710] text-white font-bold rounded-xl text-xs sm:text-sm transition flex items-center justify-center gap-1.5 shadow-[0_0_15px_-2px_rgba(229,9,20,0.4)]">
                        <span class="material-symbols-outlined text-base">filter_list</span>
                        Lọc
                    </button>
                    <a href="${pageContext.request.contextPath}/admin/users"
                       title="Đặt lại bộ lọc"
                       class="px-3 py-2 bg-white/5 hover:bg-white/10 border border-white/10 rounded-xl text-gray-300 hover:text-white transition flex items-center justify-center">
                        <span class="material-symbols-outlined text-base">restart_alt</span>
                    </a>
                </div>
            </form>
        </div>

        <!-- Users Data Table -->
        <div class="bg-[#171b26] border border-white/10 rounded-2xl shadow-xl overflow-hidden">
            <div class="overflow-x-auto">
                <table class="w-full text-left border-collapse">
                    <thead>
                        <tr class="border-b border-white/10 bg-white/[0.02] text-[11px] font-bold text-gray-400 uppercase tracking-wider">
                            <th class="py-3.5 px-4 w-14 text-center">ID</th>
                            <th class="py-3.5 px-4">Tài khoản &amp; Email</th>
                            <th class="py-3.5 px-4">Số điện thoại</th>
                            <th class="py-3.5 px-4 text-center">Vai trò hiện tại</th>
                            <th class="py-3.5 px-4 text-center">Trạng thái</th>
                            <th class="py-3.5 px-4 text-center">Điểm</th>
                            <th class="py-3.5 px-4 text-right pr-6">Hành động quản trị</th>
                        </tr>
                    </thead>
                    <tbody class="divide-y divide-white/5 text-xs sm:text-sm">
                        <c:choose>
                            <c:when test="${not empty users}">
                                <c:forEach var="u" items="${users}">
                                    <c:set var="isSelf" value="${sessionScope.user.userId == u.userId}"/>
                                    <tr class="hover:bg-white/[0.02] transition">
                                        <!-- ID -->
                                        <td class="py-3.5 px-4 text-center text-gray-400 font-mono text-xs">
                                            #${u.userId}
                                        </td>

                                        <!-- Name & Email -->
                                        <td class="py-3.5 px-4">
                                            <div class="flex items-center gap-3">
                                                <div class="w-8 h-8 rounded-full bg-gradient-to-tr from-white/10 to-white/5 border border-white/10 flex items-center justify-center font-bold text-xs text-white">
                                                    ${u.fullName != null && u.fullName.length() > 0 ? u.fullName.substring(0, 1).toUpperCase() : 'U'}
                                                </div>
                                                <div>
                                                    <div class="font-bold text-white flex items-center gap-2">
                                                        <c:out value="${u.fullName}"/>
                                                        <c:if test="${isSelf}">
                                                            <span class="text-[10px] text-emerald-400 bg-emerald-950/80 border border-emerald-500/40 px-1.5 py-0.2 rounded font-semibold">Bạn</span>
                                                        </c:if>
                                                    </div>
                                                    <div class="text-xs text-gray-400">
                                                        <c:out value="${u.email}"/>
                                                    </div>
                                                </div>
                                            </div>
                                        </td>

                                        <!-- Phone -->
                                        <td class="py-3.5 px-4 text-gray-300 font-mono">
                                            <c:out value="${u.phone}"/>
                                        </td>

                                        <!-- Role Badge -->
                                        <td class="py-3.5 px-4 text-center">
                                            <c:choose>
                                                <c:when test="${u.role == 'Admin'}">
                                                    <span class="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-[11px] font-bold bg-red-950/80 border border-red-500/40 text-red-300 shadow-[0_0_10px_rgba(239,68,68,0.2)]">
                                                        <span class="material-symbols-outlined text-xs">shield_person</span>
                                                        Admin
                                                    </span>
                                                </c:when>
                                                <c:when test="${u.role == 'Manager'}">
                                                    <span class="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-[11px] font-bold bg-amber-950/80 border border-amber-500/40 text-amber-300 shadow-[0_0_10px_rgba(245,158,11,0.2)]">
                                                        <span class="material-symbols-outlined text-xs">manage_accounts</span>
                                                        Manager
                                                    </span>
                                                </c:when>
                                                <c:when test="${u.role == 'Staff'}">
                                                    <span class="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-[11px] font-bold bg-blue-950/80 border border-blue-500/40 text-blue-300 shadow-[0_0_10px_rgba(59,130,246,0.2)]">
                                                        <span class="material-symbols-outlined text-xs">badge</span>
                                                        Staff
                                                    </span>
                                                </c:when>
                                                <c:otherwise>
                                                    <span class="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-[11px] font-bold bg-emerald-950/80 border border-emerald-500/40 text-emerald-300">
                                                        <span class="material-symbols-outlined text-xs">person</span>
                                                        Customer
                                                    </span>
                                                </c:otherwise>
                                            </c:choose>
                                        </td>

                                        <!-- Status Badge -->
                                        <td class="py-3.5 px-4 text-center">
                                            <c:choose>
                                                <c:when test="${u.status == 'Active'}">
                                                    <span class="inline-flex items-center gap-1 px-2 py-0.5 rounded text-[11px] font-semibold text-emerald-400 bg-emerald-950/60 border border-emerald-500/30">
                                                        <span class="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse"></span>
                                                        Hoạt động
                                                    </span>
                                                </c:when>
                                                <c:when test="${u.status == 'Suspended'}">
                                                    <span class="inline-flex items-center gap-1 px-2 py-0.5 rounded text-[11px] font-semibold text-amber-400 bg-amber-950/60 border border-amber-500/30">
                                                        <span class="w-1.5 h-1.5 rounded-full bg-amber-400"></span>
                                                        Tạm khóa
                                                    </span>
                                                </c:when>
                                                <c:otherwise>
                                                    <span class="inline-flex items-center gap-1 px-2 py-0.5 rounded text-[11px] font-semibold text-red-400 bg-red-950/60 border border-red-500/30">
                                                        <span class="w-1.5 h-1.5 rounded-full bg-red-400"></span>
                                                        Vô hiệu hóa
                                                    </span>
                                                </c:otherwise>
                                            </c:choose>
                                        </td>

                                        <!-- Points -->
                                        <td class="py-3.5 px-4 text-center font-mono text-xs text-[#ffb95f]">
                                            ${u.pointBalance}
                                        </td>

                                        <!-- Actions -->
                                        <td class="py-3.5 px-4 text-right pr-6">
                                            <div class="inline-flex items-center gap-1.5">
                                                <!-- Nút Sửa thông tin (Edit Profile) -->
                                                <button type="button"
                                                        onclick="openEditModal('${u.userId}', '<c:out value="${u.fullName}"/>', '<c:out value="${u.email}"/>', '<c:out value="${u.phone}"/>')"
                                                        class="px-2.5 py-1 text-xs font-semibold rounded-lg bg-white/5 hover:bg-emerald-500/20 text-emerald-300 border border-white/10 hover:border-emerald-500/40 transition flex items-center gap-1"
                                                        title="Sửa thông tin tài khoản">
                                                    <span class="material-symbols-outlined text-sm">edit</span>
                                                    <span>Sửa</span>
                                                </button>

                                                <!-- Nút Phân quyền (Role) -->
                                                <button type="button"
                                                        ${isSelf ? 'disabled title="Không thể tự đổi quyền của chính mình"' : ''}
                                                        onclick="openRoleModal('${u.userId}', '<c:out value="${u.fullName}"/>', '<c:out value="${u.email}"/>', '${u.role}')"
                                                        class="px-2.5 py-1 text-xs font-semibold rounded-lg transition flex items-center gap-1 ${isSelf ? 'opacity-30 cursor-not-allowed bg-white/5 text-gray-500' : 'bg-white/5 hover:bg-amber-500/20 text-amber-300 border border-white/10 hover:border-amber-500/40'}">
                                                    <span class="material-symbols-outlined text-sm">swap_horiz</span>
                                                    <span>Phân quyền</span>
                                                </button>

                                                <!-- Nút Đổi trạng thái (Status) -->
                                                <button type="button"
                                                        ${isSelf ? 'disabled title="Không thể tự khóa tài khoản của chính mình"' : ''}
                                                        onclick="openStatusModal('${u.userId}', '<c:out value="${u.fullName}"/>', '<c:out value="${u.email}"/>', '${u.status}')"
                                                        class="px-2.5 py-1 text-xs font-semibold rounded-lg transition flex items-center gap-1 ${isSelf ? 'opacity-30 cursor-not-allowed bg-white/5 text-gray-500' : 'bg-white/5 hover:bg-blue-500/20 text-blue-300 border border-white/10 hover:border-blue-500/40'}">
                                                    <span class="material-symbols-outlined text-sm">lock_reset</span>
                                                    <span>Trạng thái</span>
                                                </button>

                                                <!-- Nút Đặt lại mật khẩu -->
                                                <button type="button"
                                                        onclick="openResetPasswordModal('${u.userId}', '<c:out value="${u.fullName}"/>', '<c:out value="${u.email}"/>')"
                                                        class="p-1.5 text-gray-400 hover:text-[#ffb95f] hover:bg-white/5 rounded-lg border border-transparent hover:border-white/10 transition"
                                                        title="Đặt lại mật khẩu">
                                                    <span class="material-symbols-outlined text-base">key</span>
                                                </button>
                                            </div>
                                        </td>
                                    </tr>
                                </c:forEach>
                            </c:when>
                            <c:otherwise>
                                <tr>
                                    <td colspan="7" class="py-12 text-center text-gray-400">
                                        <span class="material-symbols-outlined text-4xl text-gray-600 mb-2 block">person_search</span>
                                        Không tìm thấy tài khoản nào phù hợp với bộ lọc.
                                    </td>
                                </tr>
                            </c:otherwise>
                        </c:choose>
                    </tbody>
                </table>
            </div>

            <!-- Pagination Bar -->
            <c:if test="${totalPages > 1}">
                <div class="px-6 py-4 border-t border-white/10 bg-white/[0.01] flex flex-col sm:flex-row items-center justify-between gap-3 text-xs text-gray-400">
                    <div>
                        Hiển thị trang <strong class="text-white">${currentPage}</strong> / <strong class="text-white">${totalPages}</strong> (Tổng cộng <strong>${totalRecords}</strong> người dùng)
                    </div>
                    <div class="flex items-center gap-1">
                        <c:if test="${currentPage > 1}">
                            <a href="${pageContext.request.contextPath}/admin/users?keyword=<c:out value='${keyword}'/>&role=${selectedRole}&status=${selectedStatus}&page=${currentPage - 1}"
                               class="px-3 py-1.5 bg-white/5 hover:bg-white/10 border border-white/10 rounded-lg text-white transition flex items-center gap-1">
                                <span class="material-symbols-outlined text-sm">chevron_left</span> Trước
                            </a>
                        </c:if>

                        <c:forEach begin="1" end="${totalPages}" var="p">
                            <c:if test="${p == 1 || p == totalPages || (p >= currentPage - 2 && p <= currentPage + 2)}">
                                <a href="${pageContext.request.contextPath}/admin/users?keyword=<c:out value='${keyword}'/>&role=${selectedRole}&status=${selectedStatus}&page=${p}"
                                   class="px-3 py-1.5 rounded-lg border transition ${p == currentPage ? 'bg-[#e50914] border-[#e50914] text-white font-bold' : 'bg-white/5 border-white/10 text-gray-300 hover:bg-white/10'}">
                                    ${p}
                                </a>
                            </c:if>
                        </c:forEach>

                        <c:if test="${currentPage < totalPages}">
                            <a href="${pageContext.request.contextPath}/admin/users?keyword=<c:out value='${keyword}'/>&role=${selectedRole}&status=${selectedStatus}&page=${currentPage + 1}"
                               class="px-3 py-1.5 bg-white/5 hover:bg-white/10 border border-white/10 rounded-lg text-white transition flex items-center gap-1">
                                Sau <span class="material-symbols-outlined text-sm">chevron_right</span>
                            </a>
                        </c:if>
                    </div>
                </div>
            </c:if>
        </div>
    </main>

    <!-- ==================== MODALS ==================== -->

    <!-- Modal 1: Thêm tài khoản mới -->
    <div id="createModal" class="fixed inset-0 bg-black/70 backdrop-blur-sm z-50 hidden flex items-center justify-center p-4">
        <div class="bg-[#171b26] border border-white/10 rounded-2xl w-full max-w-lg p-6 shadow-2xl space-y-4">
            <div class="flex items-center justify-between pb-3 border-b border-white/10">
                <div class="flex items-center gap-2 font-bold text-white text-lg">
                    <span class="material-symbols-outlined text-[#e50914]">person_add</span>
                    Thêm tài khoản mới
                </div>
                <button type="button" onclick="closeCreateUserModal()" class="text-gray-400 hover:text-white">
                    <span class="material-symbols-outlined">close</span>
                </button>
            </div>

            <form action="${pageContext.request.contextPath}/admin/users" method="POST" class="space-y-3.5">
                <input type="hidden" name="csrfToken" value="${csrfToken}"/>
                <input type="hidden" name="action" value="create-user"/>

                <div>
                    <label class="block text-xs font-semibold text-gray-300 uppercase tracking-wider mb-1">
                        Họ và tên <span class="text-[#e50914]">*</span>
                    </label>
                    <input type="text" name="fullName" required placeholder="Ví dụ: Nguyễn Văn An" maxlength="50"
                           class="w-full px-3.5 py-2 bg-[#0f131d] border border-white/10 rounded-xl text-white placeholder-gray-500 text-sm focus:outline-none focus:border-[#e50914]"/>
                </div>

                <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
                    <div>
                        <label class="block text-xs font-semibold text-gray-300 uppercase tracking-wider mb-1">
                            Email <span class="text-[#e50914]">*</span>
                        </label>
                        <input type="email" name="email" required placeholder="nhanvien@cinema.vn" maxlength="100"
                               class="w-full px-3.5 py-2 bg-[#0f131d] border border-white/10 rounded-xl text-white placeholder-gray-500 text-sm focus:outline-none focus:border-[#e50914]"/>
                    </div>
                    <div>
                        <label class="block text-xs font-semibold text-gray-300 uppercase tracking-wider mb-1">
                            Số điện thoại <span class="text-[#e50914]">*</span>
                        </label>
                        <input type="tel" name="phone" required placeholder="0912345678" pattern="0[0-9]{9}" maxlength="10"
                               class="w-full px-3.5 py-2 bg-[#0f131d] border border-white/10 rounded-xl text-white placeholder-gray-500 text-sm focus:outline-none focus:border-[#e50914]"/>
                    </div>
                </div>

                <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
                    <div>
                        <label class="block text-xs font-semibold text-gray-300 uppercase tracking-wider mb-1">
                            Vai trò <span class="text-[#e50914]">*</span>
                        </label>
                        <select name="role" required
                                class="w-full px-3 py-2 bg-[#0f131d] border border-white/10 rounded-xl text-white text-sm focus:outline-none focus:border-[#e50914]">
                            <option value="Staff">Nhân viên (Staff)</option>
                            <option value="Manager">Quản lý (Manager)</option>
                            <option value="Admin">Quản trị viên (Admin)</option>
                            <option value="Customer">Khách hàng (Customer)</option>
                        </select>
                    </div>
                    <div>
                        <label class="block text-xs font-semibold text-gray-300 uppercase tracking-wider mb-1">
                            Mật khẩu khởi tạo <span class="text-[#e50914]">*</span>
                        </label>
                        <div class="relative">
                            <input type="password" id="createPasswordInput" name="password" required placeholder="Tối thiểu 8 ký tự (hoa, thường, số)" minlength="8" maxlength="32"
                                   class="w-full pl-3.5 pr-10 py-2 bg-[#0f131d] border border-white/10 rounded-xl text-white placeholder-gray-500 text-sm focus:outline-none focus:border-[#e50914]"/>
                            <button type="button" onclick="togglePasswordVisibility('createPasswordInput', this)"
                                    class="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-white">
                                <span class="material-symbols-outlined text-lg">visibility</span>
                            </button>
                        </div>
                    </div>
                </div>

                <div class="p-2.5 bg-[#0f131d] rounded-xl border border-white/5 text-[11px] text-gray-400">
                    <span class="text-amber-400 font-bold">Lưu ý:</span> Mật khẩu phải có từ 8 - 32 ký tự, bao gồm ít nhất 1 chữ hoa, 1 chữ thường và 1 chữ số.
                </div>

                <div class="flex justify-end gap-2.5 pt-2">
                    <button type="button" onclick="closeCreateUserModal()"
                            class="px-4 py-2 bg-white/5 hover:bg-white/10 border border-white/10 rounded-xl text-xs font-bold text-gray-300 transition">
                        Hủy
                    </button>
                    <button type="submit"
                            class="px-4 py-2 bg-[#e50914] hover:bg-[#b80710] text-white font-bold rounded-xl text-xs transition shadow-[0_0_15px_rgba(229,9,20,0.5)] flex items-center gap-1.5">
                        <span class="material-symbols-outlined text-sm">save</span>
                        Tạo tài khoản
                    </button>
                </div>
            </form>
        </div>
    </div>

    <!-- Modal 2: Chỉnh sửa thông tin tài khoản -->
    <div id="editProfileModal" class="fixed inset-0 bg-black/70 backdrop-blur-sm z-50 hidden flex items-center justify-center p-4">
        <div class="bg-[#171b26] border border-white/10 rounded-2xl w-full max-w-md p-6 shadow-2xl space-y-4">
            <div class="flex items-center justify-between pb-3 border-b border-white/10">
                <div class="flex items-center gap-2 font-bold text-white text-lg">
                    <span class="material-symbols-outlined text-emerald-400">edit</span>
                    Cập nhật thông tin tài khoản
                </div>
                <button type="button" onclick="closeEditModal()" class="text-gray-400 hover:text-white">
                    <span class="material-symbols-outlined">close</span>
                </button>
            </div>

            <form action="${pageContext.request.contextPath}/admin/users" method="POST" class="space-y-4">
                <input type="hidden" name="csrfToken" value="${csrfToken}"/>
                <input type="hidden" name="action" value="update-profile"/>
                <input type="hidden" id="editModalUserId" name="userId" value=""/>

                <div>
                    <label class="block text-xs font-semibold text-gray-400 uppercase tracking-wider mb-1">
                        Email đăng nhập (Không thể thay đổi)
                    </label>
                    <input type="email" id="editModalEmail" readonly
                           class="w-full px-3.5 py-2.5 bg-[#0f131d]/60 border border-white/5 rounded-xl text-gray-400 text-sm cursor-not-allowed"/>
                </div>

                <div>
                    <label class="block text-xs font-semibold text-gray-300 uppercase tracking-wider mb-1">
                        Họ và tên <span class="text-[#e50914]">*</span>
                    </label>
                    <input type="text" id="editModalFullName" name="fullName" required maxlength="50"
                           class="w-full px-3.5 py-2.5 bg-[#0f131d] border border-white/10 rounded-xl text-white text-sm focus:outline-none focus:border-[#e50914]"/>
                </div>

                <div>
                    <label class="block text-xs font-semibold text-gray-300 uppercase tracking-wider mb-1">
                        Số điện thoại <span class="text-[#e50914]">*</span>
                    </label>
                    <input type="tel" id="editModalPhone" name="phone" required pattern="0[0-9]{9}" maxlength="10"
                           class="w-full px-3.5 py-2.5 bg-[#0f131d] border border-white/10 rounded-xl text-white text-sm focus:outline-none focus:border-[#e50914]"/>
                </div>

                <div class="flex justify-end gap-2.5 pt-2">
                    <button type="button" onclick="closeEditModal()"
                            class="px-4 py-2 bg-white/5 hover:bg-white/10 border border-white/10 rounded-xl text-xs font-bold text-gray-300 transition">
                        Hủy
                    </button>
                    <button type="submit"
                            class="px-4 py-2 bg-[#e50914] hover:bg-[#b80710] text-white font-bold rounded-xl text-xs transition shadow-[0_0_15px_rgba(229,9,20,0.5)]">
                        Lưu thông tin
                    </button>
                </div>
            </form>
        </div>
    </div>

    <!-- Modal 3: Phân quyền vai trò (Role) -->
    <div id="roleModal" class="fixed inset-0 bg-black/70 backdrop-blur-sm z-50 hidden flex items-center justify-center p-4">
        <div class="bg-[#171b26] border border-white/10 rounded-2xl w-full max-w-md p-6 shadow-2xl space-y-4">
            <div class="flex items-center justify-between pb-3 border-b border-white/10">
                <div class="flex items-center gap-2 font-bold text-white text-lg">
                    <span class="material-symbols-outlined text-amber-400">swap_horiz</span>
                    Phân quyền vai trò
                </div>
                <button type="button" onclick="closeRoleModal()" class="text-gray-400 hover:text-white">
                    <span class="material-symbols-outlined">close</span>
                </button>
            </div>

            <form action="${pageContext.request.contextPath}/admin/users" method="POST" class="space-y-4">
                <input type="hidden" name="csrfToken" value="${csrfToken}"/>
                <input type="hidden" name="action" value="change-role"/>
                <input type="hidden" id="roleModalUserId" name="userId" value=""/>

                <div class="bg-[#0f131d] p-3 rounded-xl border border-white/5 text-xs text-gray-300 space-y-1">
                    <div>Tài khoản: <strong id="roleModalUserName" class="text-white"></strong></div>
                    <div>Email: <span id="roleModalUserEmail" class="text-gray-400"></span></div>
                </div>

                <div>
                    <label class="block text-xs font-semibold text-gray-300 uppercase tracking-wider mb-2">
                        Chọn vai trò mới <span class="text-[#e50914]">*</span>
                    </label>
                    <select id="roleModalSelect" name="newRole" required
                            class="w-full px-3 py-2.5 bg-[#0f131d] border border-white/10 rounded-xl text-white text-sm focus:outline-none focus:border-[#e50914]">
                        <option value="Customer">Khách hàng (Customer - Đặt vé online & Tích điểm)</option>
                        <option value="Staff">Nhân viên (Staff - Bán vé POS & Báo cáo sự cố)</option>
                        <option value="Manager">Quản lý (Manager - Quản lý phim, suất chiếu, phòng, giá vé)</option>
                    </select>
                </div>

                <div class="p-2.5 bg-[#0f131d] rounded-xl border border-white/5 text-[11px] text-gray-400 space-y-1">
                    <div><span class="text-amber-400 font-bold">Lưu ý phân quyền:</span> Chỉ có thể phân quyền thành Customer, Staff hoặc Manager (không thể phân quyền thành Admin).</div>
                    <div>Không thể hạ quyền tài khoản Admin hoạt động duy nhất trong hệ thống.</div>
                </div>

                <div class="flex justify-end gap-2.5 pt-2">
                    <button type="button" onclick="closeRoleModal()"
                            class="px-4 py-2 bg-white/5 hover:bg-white/10 border border-white/10 rounded-xl text-xs font-bold text-gray-300 transition">
                        Hủy
                    </button>
                    <button type="submit"
                            class="px-4 py-2 bg-[#e50914] hover:bg-[#b80710] text-white font-bold rounded-xl text-xs transition shadow-[0_0_15px_rgba(229,9,20,0.5)]">
                        Lưu phân quyền
                    </button>
                </div>
            </form>
        </div>
    </div>

    <!-- Modal 4: Đổi trạng thái (Status) -->
    <div id="statusModal" class="fixed inset-0 bg-black/70 backdrop-blur-sm z-50 hidden flex items-center justify-center p-4">
        <div class="bg-[#171b26] border border-white/10 rounded-2xl w-full max-w-md p-6 shadow-2xl space-y-4">
            <div class="flex items-center justify-between pb-3 border-b border-white/10">
                <div class="flex items-center gap-2 font-bold text-white text-lg">
                    <span class="material-symbols-outlined text-blue-400">lock_reset</span>
                    Cập nhật trạng thái tài khoản
                </div>
                <button type="button" onclick="closeStatusModal()" class="text-gray-400 hover:text-white">
                    <span class="material-symbols-outlined">close</span>
                </button>
            </div>

            <form action="${pageContext.request.contextPath}/admin/users" method="POST" class="space-y-4">
                <input type="hidden" name="csrfToken" value="${csrfToken}"/>
                <input type="hidden" name="action" value="change-status"/>
                <input type="hidden" id="statusModalUserId" name="userId" value=""/>

                <div class="bg-[#0f131d] p-3 rounded-xl border border-white/5 text-xs text-gray-300 space-y-1">
                    <div>Tài khoản: <strong id="statusModalUserName" class="text-white"></strong></div>
                    <div>Email: <span id="statusModalUserEmail" class="text-gray-400"></span></div>
                </div>

                <div>
                    <label class="block text-xs font-semibold text-gray-300 uppercase tracking-wider mb-2">
                        Chọn trạng thái <span class="text-[#e50914]">*</span>
                    </label>
                    <select id="statusModalSelect" name="newStatus" required
                            class="w-full px-3 py-2.5 bg-[#0f131d] border border-white/10 rounded-xl text-white text-sm focus:outline-none focus:border-[#e50914]">
                        <option value="Active">Hoạt động (Active - Được phép đăng nhập)</option>
                        <option value="Suspended">Tạm khóa (Suspended - Chặn đăng nhập tạm thời)</option>
                        <option value="Deactivated">Vô hiệu hóa (Deactivated - Đóng tài khoản vĩnh viễn)</option>
                    </select>
                </div>

                <div class="p-2.5 bg-[#0f131d] rounded-xl border border-white/5 text-[11px] text-gray-400">
                    <span class="text-amber-400 font-bold">GB-12:</span> Hệ thống không xóa cứng tài khoản để bảo toàn toàn vẹn lịch sử giao dịch và đối soát tài chính.
                </div>

                <div class="flex justify-end gap-2.5 pt-2">
                    <button type="button" onclick="closeStatusModal()"
                            class="px-4 py-2 bg-white/5 hover:bg-white/10 border border-white/10 rounded-xl text-xs font-bold text-gray-300 transition">
                        Hủy
                    </button>
                    <button type="submit"
                            class="px-4 py-2 bg-[#e50914] hover:bg-[#b80710] text-white font-bold rounded-xl text-xs transition shadow-[0_0_15px_rgba(229,9,20,0.5)]">
                        Lưu trạng thái
                    </button>
                </div>
            </form>
        </div>
    </div>

    <!-- Modal 5: Đặt lại mật khẩu -->
    <div id="resetPasswordModal" class="fixed inset-0 bg-black/70 backdrop-blur-sm z-50 hidden flex items-center justify-center p-4">
        <div class="bg-[#171b26] border border-white/10 rounded-2xl w-full max-w-md p-6 shadow-2xl space-y-4">
            <div class="flex items-center justify-between pb-3 border-b border-white/10">
                <div class="flex items-center gap-2 font-bold text-white text-lg">
                    <span class="material-symbols-outlined text-amber-400">key</span>
                    Đặt lại mật khẩu cho tài khoản
                </div>
                <button type="button" onclick="closeResetPasswordModal()" class="text-gray-400 hover:text-white">
                    <span class="material-symbols-outlined">close</span>
                </button>
            </div>

            <form action="${pageContext.request.contextPath}/admin/users" method="POST" class="space-y-4">
                <input type="hidden" name="csrfToken" value="${csrfToken}"/>
                <input type="hidden" name="action" value="reset-password"/>
                <input type="hidden" id="resetPasswordModalUserId" name="userId" value=""/>

                <div class="bg-[#0f131d] p-3 rounded-xl border border-white/5 text-xs text-gray-300 space-y-1">
                    <div>Tài khoản: <strong id="resetPasswordModalUserName" class="text-white"></strong></div>
                    <div>Email: <span id="resetPasswordModalUserEmail" class="text-gray-400"></span></div>
                </div>

                <div>
                    <label class="block text-xs font-semibold text-gray-300 uppercase tracking-wider mb-2">
                        Mật khẩu mới <span class="text-[#e50914]">*</span>
                    </label>
                    <div class="relative">
                        <input type="password" id="resetPasswordInput" name="newPassword" required placeholder="Nhập mật khẩu mới (8-32 ký tự)" minlength="8" maxlength="32"
                               class="w-full pl-3.5 pr-10 py-2.5 bg-[#0f131d] border border-white/10 rounded-xl text-white placeholder-gray-500 text-sm focus:outline-none focus:border-[#e50914]"/>
                        <button type="button" onclick="togglePasswordVisibility('resetPasswordInput', this)"
                                class="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-white">
                            <span class="material-symbols-outlined text-lg">visibility</span>
                        </button>
                    </div>
                </div>

                <div class="p-2.5 bg-[#0f131d] rounded-xl border border-white/5 text-[11px] text-gray-400">
                    Mật khẩu phải có từ 8 - 32 ký tự, bao gồm ít nhất 1 chữ hoa, 1 chữ thường và 1 chữ số.
                </div>

                <div class="flex justify-end gap-2.5 pt-2">
                    <button type="button" onclick="closeResetPasswordModal()"
                            class="px-4 py-2 bg-white/5 hover:bg-white/10 border border-white/10 rounded-xl text-xs font-bold text-gray-300 transition">
                        Hủy
                    </button>
                    <button type="submit"
                            class="px-4 py-2 bg-[#e50914] hover:bg-[#b80710] text-white font-bold rounded-xl text-xs transition shadow-[0_0_15px_rgba(229,9,20,0.5)]">
                        Xác nhận đặt lại MK
                    </button>
                </div>
            </form>
        </div>
    </div>

    <!-- JavaScript Handlers -->
    <script>
        // Modal Tạo tài khoản
        function openCreateUserModal() {
            document.getElementById('createModal').classList.remove('hidden');
        }
        function closeCreateUserModal() {
            document.getElementById('createModal').classList.add('hidden');
        }

        // Modal Sửa thông tin tài khoản
        function openEditModal(userId, fullName, email, phone) {
            document.getElementById('editModalUserId').value = userId;
            document.getElementById('editModalEmail').value = email;
            document.getElementById('editModalFullName').value = fullName;
            document.getElementById('editModalPhone').value = phone;
            document.getElementById('editProfileModal').classList.remove('hidden');
        }
        function closeEditModal() {
            document.getElementById('editProfileModal').classList.add('hidden');
        }

        // Modal Phân quyền vai trò
        function openRoleModal(userId, fullName, email, currentRole) {
            document.getElementById('roleModalUserId').value = userId;
            document.getElementById('roleModalUserName').innerText = fullName;
            document.getElementById('roleModalUserEmail').innerText = email;
            const roleSelect = document.getElementById('roleModalSelect');
            if (currentRole === 'Admin') {
                roleSelect.value = 'Customer';
            } else {
                roleSelect.value = currentRole;
            }
            document.getElementById('roleModal').classList.remove('hidden');
        }
        function closeRoleModal() {
            document.getElementById('roleModal').classList.add('hidden');
        }

        // Modal Trạng thái
        function openStatusModal(userId, fullName, email, currentStatus) {
            document.getElementById('statusModalUserId').value = userId;
            document.getElementById('statusModalUserName').innerText = fullName;
            document.getElementById('statusModalUserEmail').innerText = email;
            document.getElementById('statusModalSelect').value = currentStatus;
            document.getElementById('statusModal').classList.remove('hidden');
        }
        function closeStatusModal() {
            document.getElementById('statusModal').classList.add('hidden');
        }

        // Modal Đặt lại mật khẩu
        function openResetPasswordModal(userId, fullName, email) {
            document.getElementById('resetPasswordModalUserId').value = userId;
            document.getElementById('resetPasswordModalUserName').innerText = fullName;
            document.getElementById('resetPasswordModalUserEmail').innerText = email;
            document.getElementById('resetPasswordInput').value = '';
            document.getElementById('resetPasswordModal').classList.remove('hidden');
        }
        function closeResetPasswordModal() {
            document.getElementById('resetPasswordModal').classList.add('hidden');
        }

        // Hiện/ẩn mật khẩu
        function togglePasswordVisibility(inputId, btn) {
            const input = document.getElementById(inputId);
            if (!input) return;
            const icon = btn.querySelector('.material-symbols-outlined');
            if (input.type === 'password') {
                input.type = 'text';
                if (icon) icon.innerText = 'visibility_off';
            } else {
                input.type = 'password';
                if (icon) icon.innerText = 'visibility';
            }
        }

        // Đóng modal khi bấm ESC hoặc click ra ngoài vùng backdrop
        document.addEventListener('keydown', function (e) {
            if (e.key === 'Escape') {
                closeCreateUserModal();
                closeEditModal();
                closeRoleModal();
                closeStatusModal();
                closeResetPasswordModal();
            }
        });

        ['createModal', 'editProfileModal', 'roleModal', 'statusModal', 'resetPasswordModal'].forEach(id => {
            const el = document.getElementById(id);
            if (el) {
                el.addEventListener('click', function (e) {
                    if (e.target === el) {
                        el.classList.add('hidden');
                    }
                });
            }
        });

        // Tự động đóng thông báo flash sau 5 giây (5000ms)
        function dismissFlashAlert(el) {
            if (!el) return;
            el.style.transition = 'opacity 0.6s ease, transform 0.6s ease, max-height 0.6s ease, margin 0.6s ease, padding 0.6s ease';
            el.style.opacity = '0';
            el.style.transform = 'translateY(-10px)';
            el.style.maxHeight = el.scrollHeight + 'px';
            setTimeout(() => {
                el.style.maxHeight = '0px';
                el.style.marginTop = '0px';
                el.style.marginBottom = '0px';
                el.style.paddingTop = '0px';
                el.style.paddingBottom = '0px';
            }, 50);
            setTimeout(() => {
                el.remove();
            }, 650);
        }

        document.addEventListener('DOMContentLoaded', function () {
            const alerts = document.querySelectorAll('.flash-alert');
            if (alerts.length > 0) {
                setTimeout(function () {
                    alerts.forEach(function (alert) {
                        dismissFlashAlert(alert);
                    });
                }, 5000);
            }
        });
    </script>
</body>
</html>
