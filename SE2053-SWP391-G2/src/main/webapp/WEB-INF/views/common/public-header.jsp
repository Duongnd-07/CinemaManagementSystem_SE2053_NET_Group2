<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- Thanh điều hướng dùng chung cho Guest/Customer; trang gọi đặt request attribute activeNav để tô sáng mục hiện tại --%>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="navActive" value="text-white"/>
<c:set var="navIdle" value="hover:text-white"/>
<header class="h-20 bg-[#171b26]/90 backdrop-blur-md border-b border-white/10 px-6 sm:px-10 flex items-center justify-between sticky top-0 z-50">
    <a href="${ctx}/" class="flex items-center gap-2 group">
        <span class="material-symbols-outlined text-4xl text-[#e50914] transition-transform group-hover:scale-110">movie</span>
        <span class="text-2xl font-extrabold tracking-widest text-white">CINE<span class="text-[#e50914]">LUXE</span></span>
    </a>

    <nav class="hidden md:flex items-center gap-8 text-sm font-semibold text-[#8e919f]">
        <a href="${ctx}/" class="${activeNav == 'home' ? navActive : navIdle} hover:text-[#e50914] transition">Trang chủ</a>
        <a href="${ctx}/movies?tab=now-showing" class="${activeNav == 'now-showing' ? navActive : navIdle} hover:text-[#e50914] transition">Phim đang chiếu</a>
        <a href="${ctx}/movies?tab=coming-soon" class="${activeNav == 'coming-soon' ? navActive : navIdle} hover:text-[#e50914] transition">Phim sắp chiếu</a>
        <a href="${ctx}/" class="${navIdle} transition">Lịch chiếu</a>
        <a href="${ctx}/" class="${navIdle} transition">Bắp nước</a>
        <a href="${ctx}/" class="${navIdle} transition">Khuyến mãi</a>
    </nav>

    <!-- Trạng thái xác thực Người dùng -->
    <div class="flex items-center gap-3">
        <c:choose>
            <c:when test="${not empty sessionScope.user}">
                <!-- Đã đăng nhập -->
                <div class="flex items-center gap-3 bg-[#0f131d] border border-white/10 rounded-full py-1.5 pl-3 pr-2">
                    <div class="flex flex-col text-right">
                        <span class="text-xs font-bold text-white"><c:out value="${sessionScope.user.fullName}"/></span>
                        <span class="text-[10px] text-[#ffb95f] font-semibold flex items-center justify-end gap-1">
                            <span class="material-symbols-outlined text-[12px]">stars</span>
                            <c:out value="${sessionScope.user.pointBalance}"/> điểm (${sessionScope.user.role})
                        </span>
                    </div>
                    <div class="w-8 h-8 rounded-full bg-[#e50914]/20 border border-[#e50914]/40 flex items-center justify-center text-[#e50914] font-bold text-xs">
                        <span class="material-symbols-outlined text-base">person</span>
                    </div>
                </div>

                <!-- Nút tắt theo quyền -->
                <c:if test="${sessionScope.user.role == 'Admin' || sessionScope.user.role == 'Manager'}">
                    <a href="${ctx}/manager/movies"
                       class="px-3 py-1.5 bg-amber-500/20 hover:bg-amber-500/30 text-amber-300 border border-amber-500/30 rounded-lg text-xs font-semibold flex items-center gap-1 transition">
                        <span class="material-symbols-outlined text-sm">settings</span>
                        Quản lý
                    </a>
                </c:if>
                <c:if test="${sessionScope.user.role == 'Staff'}">
                    <a href="${ctx}/staff/dashboard"
                       class="px-3 py-1.5 bg-blue-500/20 hover:bg-blue-500/30 text-blue-300 border border-blue-500/30 rounded-lg text-xs font-semibold flex items-center gap-1 transition">
                        <span class="material-symbols-outlined text-sm">badge</span>
                        Nghiệp vụ
                    </a>
                </c:if>

                <!-- Đăng xuất -->
                <a href="${ctx}/logout"
                   class="px-3 py-1.5 bg-white/5 hover:bg-red-500/20 hover:text-red-300 text-gray-300 border border-white/10 rounded-lg text-xs font-semibold flex items-center gap-1 transition">
                    <span class="material-symbols-outlined text-sm">logout</span>
                    Đăng xuất
                </a>
            </c:when>
            <c:otherwise>
                <!-- Chưa đăng nhập -->
                <a href="${ctx}/login"
                   class="px-4 py-2 text-xs font-bold text-white hover:text-[#e50914] transition">
                    Đăng nhập
                </a>
                <a href="${ctx}/register"
                   class="px-4 py-2 bg-[#e50914] hover:bg-[#b80710] text-white font-bold rounded-xl text-xs transition shadow-[0_0_15px_-3px_rgba(229,9,20,0.5)]">
                    Đăng ký thành viên
                </a>
            </c:otherwise>
        </c:choose>
    </div>
</header>
