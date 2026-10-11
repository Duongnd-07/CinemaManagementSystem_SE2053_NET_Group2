<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi" class="dark">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>CineLuxe Cinema - Trải nghiệm điện ảnh đỉnh cao</title>
    <script src="https://cdn.tailwindcss.com"></script>
    <link href="https://fonts.googleapis.com/css2?family=Be+Vietnam+Pro:wght@400;500;600;700;800&display=swap" rel="stylesheet"/>
    <link href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@24,400,0,0" rel="stylesheet"/>
    <link href="${pageContext.request.contextPath}/assets/css/cineluxe.css" rel="stylesheet"/>
    <style>
        body {
            font-family: 'Be Vietnam Pro', sans-serif;
            background-color: #0f131d;
            color: #dfe2f1;
        }
    </style>
</head>
<body class="min-h-screen bg-[#0f131d] flex flex-col">
    <!-- Navbar Header -->
    <header class="h-20 bg-[#171b26]/90 backdrop-blur-md border-b border-white/10 px-6 sm:px-10 flex items-center justify-between sticky top-0 z-50">
        <a href="${pageContext.request.contextPath}/" class="flex items-center gap-2 group">
            <span class="material-symbols-outlined text-4xl text-[#e50914] transition-transform group-hover:scale-110">movie</span>
            <span class="text-2xl font-extrabold tracking-widest text-white">CINE<span class="text-[#e50914]">LUXE</span></span>
        </a>

        <nav class="hidden md:flex items-center gap-8 text-sm font-semibold text-[#8e919f]">
            <a href="${pageContext.request.contextPath}/" class="text-white hover:text-[#e50914] transition">Trang chủ</a>
            <a href="${pageContext.request.contextPath}/" class="hover:text-white transition">Lịch chiếu</a>
            <a href="${pageContext.request.contextPath}/" class="hover:text-white transition">Phim đang chiếu</a>
            <a href="${pageContext.request.contextPath}/" class="hover:text-white transition">Bắp nước</a>
            <a href="${pageContext.request.contextPath}/" class="hover:text-white transition">Khuyến mãi</a>
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
                        <a href="${pageContext.request.contextPath}/manager/movies"
                           class="px-3 py-1.5 bg-amber-500/20 hover:bg-amber-500/30 text-amber-300 border border-amber-500/30 rounded-lg text-xs font-semibold flex items-center gap-1 transition">
                            <span class="material-symbols-outlined text-sm">settings</span>
                            Quản lý
                        </a>
                    </c:if>
                    <c:if test="${sessionScope.user.role == 'Staff'}">
                        <a href="${pageContext.request.contextPath}/staff/dashboard"
                           class="px-3 py-1.5 bg-blue-500/20 hover:bg-blue-500/30 text-blue-300 border border-blue-500/30 rounded-lg text-xs font-semibold flex items-center gap-1 transition">
                            <span class="material-symbols-outlined text-sm">badge</span>
                            Nghiệp vụ
                        </a>
                    </c:if>

                    <!-- Đăng xuất -->
                    <a href="${pageContext.request.contextPath}/logout"
                       class="px-3 py-1.5 bg-white/5 hover:bg-red-500/20 hover:text-red-300 text-gray-300 border border-white/10 rounded-lg text-xs font-semibold flex items-center gap-1 transition">
                        <span class="material-symbols-outlined text-sm">logout</span>
                        Đăng xuất
                    </a>
                </c:when>
                <c:otherwise>
                    <!-- Chưa đăng nhập -->
                    <a href="${pageContext.request.contextPath}/login"
                       class="px-4 py-2 text-xs font-bold text-white hover:text-[#e50914] transition">
                        Đăng nhập
                    </a>
                    <a href="${pageContext.request.contextPath}/register"
                       class="px-4 py-2 bg-[#e50914] hover:bg-[#b80710] text-white font-bold rounded-xl text-xs transition shadow-[0_0_15px_-3px_rgba(229,9,20,0.5)]">
                        Đăng ký thành viên
                    </a>
                </c:otherwise>
            </c:choose>
        </div>
    </header>

    <!-- Main Content -->
    <main class="flex-1 max-w-7xl w-full mx-auto p-6 sm:p-10 space-y-8">
        <!-- Flash Alert -->
        <c:if test="${not empty sessionScope.flashSuccess}">
            <div class="p-4 bg-emerald-950/80 border border-emerald-500/40 rounded-xl text-emerald-300 text-sm flex items-center gap-3">
                <span class="material-symbols-outlined text-emerald-400 text-xl">check_circle</span>
                <span><c:out value="${sessionScope.flashSuccess}"/></span>
            </div>
            <c:remove var="flashSuccess" scope="session"/>
        </c:if>

        <!-- Hero Banner -->
        <div class="relative rounded-3xl overflow-hidden bg-gradient-to-r from-red-950/60 via-[#171b26] to-[#0f131d] border border-white/10 p-8 sm:p-14 shadow-2xl">
            <div class="max-w-2xl relative z-10 space-y-4">
                <span class="px-3 py-1 bg-[#e50914]/20 border border-[#e50914]/40 rounded-full text-[#e50914] text-xs font-bold tracking-widest uppercase">
                    CineLuxe Experience 2026
                </span>
                <h1 class="text-3xl sm:text-5xl font-extrabold text-white leading-tight">
                    Đỉnh Cao Điện Ảnh <br/><span class="text-[#e50914]">Âm Thanh Dolby Atmos</span>
                </h1>
                <p class="text-sm sm:text-base text-[#e9bcb6] leading-relaxed">
                    Đắm chìm vào thế giới phim đỉnh cao với màn hình IMAX Laser, ghế bọc da cao cấp và ưu đãi độc quyền dành cho thành viên CineLuxe.
                </p>
                <div class="flex flex-wrap items-center gap-4 pt-2">
                    <c:choose>
                        <c:when test="${not empty sessionScope.user}">
                            <div class="px-5 py-3 bg-[#171b26] border border-white/20 rounded-xl text-white text-sm font-semibold flex items-center gap-2">
                                <span class="material-symbols-outlined text-[#ffb95f]">verified</span>
                                Xin chào, <strong><c:out value="${sessionScope.user.fullName}"/></strong>!
                            </div>
                        </c:when>
                        <c:otherwise>
                            <a href="${pageContext.request.contextPath}/register"
                               class="px-6 py-3 bg-[#e50914] hover:bg-[#b80710] text-white font-bold rounded-xl text-sm transition shadow-[0_0_20px_-2px_rgba(229,9,20,0.6)] flex items-center gap-2">
                                <span class="material-symbols-outlined">card_membership</span>
                                Đăng ký nhận 100 điểm thưởng
                            </a>
                            <a href="${pageContext.request.contextPath}/login"
                               class="px-6 py-3 bg-white/5 hover:bg-white/10 border border-white/10 rounded-xl text-white text-sm font-semibold transition">
                                Đăng nhập tài khoản
                            </a>
                        </c:otherwise>
                    </c:choose>
                </div>
            </div>
        </div>

        <!-- Banner Ưu đãi thành viên -->
        <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
            <div class="bg-[#171b26] border border-white/10 rounded-2xl p-6 space-y-2">
                <div class="w-10 h-10 rounded-xl bg-[#e50914]/20 text-[#e50914] flex items-center justify-center font-bold">
                    <span class="material-symbols-outlined">confirmation_number</span>
                </div>
                <h3 class="font-bold text-white text-lg">Đặt vé trực tuyến nhanh</h3>
                <p class="text-xs text-[#8e919f]">Chọn chỗ ngồi đẹp nhất trước giờ chiếu mà không cần xếp hàng tại rạp.</p>
            </div>
            <div class="bg-[#171b26] border border-white/10 rounded-2xl p-6 space-y-2">
                <div class="w-10 h-10 rounded-xl bg-amber-500/20 text-amber-400 flex items-center justify-center font-bold">
                    <span class="material-symbols-outlined">redeem</span>
                </div>
                <h3 class="font-bold text-white text-lg">Tích điểm đổi quà</h3>
                <p class="text-xs text-[#8e919f]">Tích lũy 5% giá trị giao dịch để quy đổi bắp nước và vé xem phim miễn phí.</p>
            </div>
            <div class="bg-[#171b26] border border-white/10 rounded-2xl p-6 space-y-2">
                <div class="w-10 h-10 rounded-xl bg-purple-500/20 text-purple-400 flex items-center justify-center font-bold">
                    <span class="material-symbols-outlined">cake</span>
                </div>
                <h3 class="font-bold text-white text-lg">Ưu đãi sinh nhật VIP</h3>
                <p class="text-xs text-[#8e919f]">Tặng combo bắp nước và vé xem phim vào tuần lễ sinh nhật của bạn.</p>
            </div>
        </div>
    </main>

    <!-- Footer -->
    <footer class="bg-[#171b26] border-t border-white/10 py-8 px-6 text-center text-xs text-[#8e919f]">
        <div class="max-w-7xl mx-auto flex flex-col sm:flex-row items-center justify-between gap-4">
            <div class="flex items-center gap-2">
                <span class="material-symbols-outlined text-[#e50914]">movie</span>
                <span class="font-bold text-white">CINELUXE CINEMA</span>
            </div>
            <div>&copy; 2026 CineLuxe Cinema - SWP391 Group 2. Mọi quyền được bảo lưu.</div>
            <div class="flex gap-4">
                <a href="#" class="hover:text-white transition">Điều khoản sử dụng</a>
                <a href="#" class="hover:text-white transition">Chính sách bảo mật</a>
            </div>
        </div>
    </footer>
</body>
</html>