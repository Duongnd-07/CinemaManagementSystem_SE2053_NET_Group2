<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi" class="dark">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Bàn làm việc Nhân viên - CineLuxe Cinema</title>
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
    <!-- Header -->
    <header class="h-16 bg-[#171b26] border-b border-white/10 px-6 flex items-center justify-between sticky top-0 z-30">
        <div class="flex items-center gap-3">
            <span class="material-symbols-outlined text-3xl text-[#e50914]">badge</span>
            <div>
                <span class="font-extrabold text-lg tracking-wider text-white">CINELUXE <span class="text-blue-400 text-xs px-2 py-0.5 bg-blue-500/10 rounded-full border border-blue-500/20">STAFF PORTAL</span></span>
            </div>
        </div>
        <div class="flex items-center gap-4">
            <div class="flex items-center gap-2">
                <div class="w-8 h-8 rounded-full bg-blue-600/30 border border-blue-500/40 flex items-center justify-center text-blue-400 font-bold text-sm">
                    NV
                </div>
                <div class="text-left hidden sm:block">
                    <div class="text-sm font-semibold text-white"><c:out value="${currentUser.fullName}"/></div>
                    <div class="text-[11px] text-[#8e919f]">Nhân viên quầy / soát vé</div>
                </div>
            </div>
            <a href="${pageContext.request.contextPath}/logout"
               class="px-3.5 py-1.5 bg-white/5 hover:bg-red-500/20 hover:text-red-300 text-gray-300 border border-white/10 hover:border-red-500/30 rounded-lg text-xs font-semibold flex items-center gap-1.5 transition">
                <span class="material-symbols-outlined text-sm">logout</span>
                Đăng xuất
            </a>
        </div>
    </header>

    <!-- Main Content -->
    <main class="flex-1 max-w-6xl w-full mx-auto p-6 space-y-6">
        <!-- Flash Alert (Tự động ẩn sau 5s) -->
        <c:if test="${not empty flashSuccess}">
            <div class="flash-alert p-4 bg-emerald-950/80 border border-emerald-500/40 rounded-xl text-emerald-300 text-sm flex items-center justify-between gap-3 shadow-lg transition-all duration-500 overflow-hidden">
                <div class="flex items-center gap-3 min-w-0">
                    <span class="material-symbols-outlined text-emerald-400 text-xl flex-shrink-0">check_circle</span>
                    <span class="break-words"><c:out value="${flashSuccess}"/></span>
                </div>
                <button type="button" onclick="dismissFlashAlert(this.closest('.flash-alert'))" class="text-emerald-400/70 hover:text-emerald-200 transition p-1 rounded-lg hover:bg-emerald-500/10 flex-shrink-0" title="Đóng">
                    <span class="material-symbols-outlined text-lg">close</span>
                </button>
            </div>
        </c:if>

        <!-- Welcome Banner -->
        <div class="bg-gradient-to-r from-[#171b26] to-[#1e2333] border border-white/10 rounded-2xl p-6 sm:p-8 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 shadow-xl">
            <div>
                <div class="text-xs uppercase font-bold tracking-wider text-blue-400 mb-1">Cổng tác nghiệp nhân viên</div>
                <h1 class="text-2xl sm:text-3xl font-bold text-white">Xin chào, <c:out value="${currentUser.fullName}"/>!</h1>
                <p class="text-sm text-[#8e919f] mt-1">Chúc bạn một ca làm việc tràn đầy năng lượng và hiệu quả tại CineLuxe Cinema.</p>
            </div>
            <div class="px-4 py-2 bg-emerald-500/10 border border-emerald-500/30 rounded-xl text-emerald-400 text-xs font-semibold flex items-center gap-2">
                <span class="w-2.5 h-2.5 rounded-full bg-emerald-400 animate-pulse"></span>
                Trạng thái: Đang trong ca trực
            </div>
        </div>

        <!-- Chức năng nghiệp vụ Staff -->
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
            <!-- 1. Bán vé tại quầy -->
            <div class="bg-[#171b26] border border-white/10 hover:border-blue-500/40 rounded-xl p-5 transition group hover:-translate-y-1 shadow-lg">
                <div class="w-12 h-12 rounded-xl bg-blue-500/20 text-blue-400 flex items-center justify-center mb-4 group-hover:scale-110 transition">
                    <span class="material-symbols-outlined text-2xl">confirmation_number</span>
                </div>
                <h3 class="font-bold text-white text-base mb-1">Bán vé tại quầy</h3>
                <p class="text-xs text-[#8e919f] mb-4">Mở sơ đồ ghế, chọn suất chiếu và in vé xem phim cho khách hàng.</p>
                <span class="text-xs font-semibold text-blue-400 flex items-center gap-1 group-hover:underline">
                    Vào quầy POS <span class="material-symbols-outlined text-xs">arrow_forward</span>
                </span>
            </div>

            <!-- 2. Soát vé vào phòng chiếu -->
            <div class="bg-[#171b26] border border-white/10 hover:border-emerald-500/40 rounded-xl p-5 transition group hover:-translate-y-1 shadow-lg">
                <div class="w-12 h-12 rounded-xl bg-emerald-500/20 text-emerald-400 flex items-center justify-center mb-4 group-hover:scale-110 transition">
                    <span class="material-symbols-outlined text-2xl">qr_code_scanner</span>
                </div>
                <h3 class="font-bold text-white text-base mb-1">Soát vé vào rạp</h3>
                <p class="text-xs text-[#8e919f] mb-4">Quét mã QR trên vé giấy hoặc ứng dụng của khách trước giờ chiếu.</p>
                <span class="text-xs font-semibold text-emerald-400 flex items-center gap-1 group-hover:underline">
                    Quét mã vé <span class="material-symbols-outlined text-xs">arrow_forward</span>
                </span>
            </div>

            <!-- 3. Bán bắp nước -->
            <div class="bg-[#171b26] border border-white/10 hover:border-amber-500/40 rounded-xl p-5 transition group hover:-translate-y-1 shadow-lg">
                <div class="w-12 h-12 rounded-xl bg-amber-500/20 text-amber-400 flex items-center justify-center mb-4 group-hover:scale-110 transition">
                    <span class="material-symbols-outlined text-2xl">fastfood</span>
                </div>
                <h3 class="font-bold text-white text-base mb-1">Bắp nước &amp; Combo</h3>
                <p class="text-xs text-[#8e919f] mb-4">Order bắp rang bơ, nước ngọt và các gói combo ưu đãi tại quầy.</p>
                <span class="text-xs font-semibold text-amber-400 flex items-center gap-1 group-hover:underline">
                    Bán combo <span class="material-symbols-outlined text-xs">arrow_forward</span>
                </span>
            </div>

            <!-- 4. Báo cáo sự cố thiết bị -->
            <a href="${pageContext.request.contextPath}/staff/equipment-reports"
               class="block bg-[#171b26] border border-white/10 hover:border-purple-500/40 rounded-xl p-5 transition group hover:-translate-y-1 shadow-lg">
                <div class="w-12 h-12 rounded-xl bg-purple-500/20 text-purple-400 flex items-center justify-center mb-4 group-hover:scale-110 transition">
                    <span class="material-symbols-outlined text-2xl">build</span>
                </div>
                <h3 class="font-bold text-white text-base mb-1">Sự cố thiết bị</h3>
                <p class="text-xs text-[#8e919f] mb-4">Ghi nhận ghế hỏng, đèn chiếu, âm thanh để kỹ thuật viên xử lý.</p>
                <span class="text-xs font-semibold text-purple-400 flex items-center gap-1 group-hover:underline">
                    Báo sự cố <span class="material-symbols-outlined text-xs">arrow_forward</span>
                </span>
            </a>
        </div>
    </main>

    <!-- Footer -->
    <footer class="h-12 border-t border-white/10 px-6 flex items-center justify-between text-xs text-[#8e919f]">
        <span>&copy; 2026 CineLuxe Cinema - Nhân viên tác nghiệp</span>
        <span>Hệ thống phân quyền Role-Based Access Control (RBAC)</span>
    </footer>
    <script>
        function dismissFlashAlert(el) {
            if (!el) return;
            el.style.transition = 'opacity 0.5s ease, transform 0.5s ease, max-height 0.5s ease, margin 0.5s ease, padding 0.5s ease';
            el.style.opacity = '0';
            el.style.transform = 'translateY(-8px)';
            el.style.maxHeight = el.scrollHeight + 'px';
            setTimeout(function () {
                el.style.maxHeight = '0px';
                el.style.marginTop = '0px';
                el.style.marginBottom = '0px';
                el.style.paddingTop = '0px';
                el.style.paddingBottom = '0px';
            }, 50);
            setTimeout(function () {
                el.remove();
            }, 550);
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
