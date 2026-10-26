<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<%-- Số phim tối đa mỗi danh mục trên trang chủ; xem đầy đủ ở /movies --%>
<c:set var="homeLimit" value="${8}"/>
<!DOCTYPE html>
<html lang="vi" class="dark">
<head>
    <title>CineLuxe Cinema - Trải nghiệm điện ảnh đỉnh cao</title>
    <jsp:include page="/WEB-INF/views/common/public-head.jsp"/>
    <style>
        body {
            font-family: 'Be Vietnam Pro', sans-serif;
            background-color: #0f131d;
            color: #dfe2f1;
        }
    </style>
</head>
<body class="min-h-screen bg-[#0f131d] flex flex-col">
    <jsp:include page="/WEB-INF/views/common/public-header.jsp"/>

    <!-- Main Content -->
    <main class="flex-1 max-w-7xl w-full mx-auto p-6 sm:p-10 space-y-10">
        <!-- Flash Alert (Tự động ẩn sau 5s) -->
        <c:if test="${not empty sessionScope.flashSuccess}">
            <div class="flash-alert p-4 bg-emerald-950/80 border border-emerald-500/40 rounded-xl text-emerald-300 text-sm flex items-center justify-between gap-3 shadow-lg transition-all duration-500 overflow-hidden">
                <div class="flex items-center gap-3 min-w-0">
                    <span class="material-symbols-outlined text-emerald-400 text-xl flex-shrink-0">check_circle</span>
                    <span class="break-words"><c:out value="${sessionScope.flashSuccess}"/></span>
                </div>
                <button type="button" onclick="dismissFlashAlert(this.closest('.flash-alert'))" class="text-emerald-400/70 hover:text-emerald-200 transition p-1 rounded-lg hover:bg-emerald-500/10 flex-shrink-0" title="Đóng">
                    <span class="material-symbols-outlined text-lg">close</span>
                </button>
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
                            <a href="${ctx}/register"
                               class="px-6 py-3 bg-[#e50914] hover:bg-[#b80710] text-white font-bold rounded-xl text-sm transition shadow-[0_0_20px_-2px_rgba(229,9,20,0.6)] flex items-center gap-2">
                                <span class="material-symbols-outlined">card_membership</span>
                                Đăng ký nhận 100 điểm thưởng
                            </a>
                            <a href="${ctx}/login"
                               class="px-6 py-3 bg-white/5 hover:bg-white/10 border border-white/10 rounded-xl text-white text-sm font-semibold transition">
                                Đăng nhập tài khoản
                            </a>
                        </c:otherwise>
                    </c:choose>
                </div>
            </div>
        </div>

        <!-- UC-05: Tìm nhanh theo tên phim, kết quả hiển thị ở mục Phim -->
        <form method="get" action="${ctx}/movies" role="search"
              class="flex flex-col sm:flex-row gap-3 bg-[#171b26] border border-white/10 rounded-2xl p-3">
            <div class="relative flex-1">
                <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-[#8e919f]">search</span>
                <input type="search" id="home-search" name="q" maxlength="100" placeholder="Tìm phim theo tên..." aria-label="Tên phim"
                       data-movie-suggest="${ctx}/movies/suggest" data-movie-detail="${ctx}/movies/detail?id="
                       class="w-full h-11 pl-10 pr-4 rounded-xl bg-[#0f131d] border border-white/10 text-sm text-white placeholder-[#8e919f] focus:border-[#e50914] focus:outline-none transition"/>
            </div>
            <button type="submit" class="h-11 px-6 rounded-xl bg-[#e50914] hover:bg-[#b80710] text-white text-sm font-bold transition">
                Tìm kiếm
            </button>
            <a href="${ctx}/movies" class="h-11 px-5 rounded-xl border border-white/10 text-sm font-semibold text-[#dfe2f1] hover:bg-white/5 flex items-center justify-center gap-1 transition">
                <span class="material-symbols-outlined text-base">tune</span>
                Lọc nâng cao
            </a>
        </form>

        <!-- UC-04: Phim đang chiếu -->
        <section id="now-showing" class="space-y-5">
            <div class="flex items-end justify-between gap-4">
                <h2 class="text-xl sm:text-2xl font-extrabold text-white flex items-center gap-2">
                    <span class="w-1.5 h-7 rounded-full bg-[#e50914]"></span>
                    Phim đang chiếu
                </h2>
                <c:if test="${fn:length(nowShowing) > homeLimit}">
                    <a href="${ctx}/movies?tab=now-showing" class="text-sm font-semibold text-[#e50914] hover:underline flex items-center gap-1">
                        Xem tất cả <span class="material-symbols-outlined text-base">arrow_forward</span>
                    </a>
                </c:if>
            </div>
            <ui:movie-grid movies="${nowShowing}" emptyMessage="${emptyMessage}" limit="${homeLimit}"/>
        </section>

        <!-- UC-04: Phim sắp chiếu -->
        <section id="coming-soon" class="space-y-5">
            <div class="flex items-end justify-between gap-4">
                <h2 class="text-xl sm:text-2xl font-extrabold text-white flex items-center gap-2">
                    <span class="w-1.5 h-7 rounded-full bg-[#ffb95f]"></span>
                    Phim sắp chiếu
                </h2>
                <c:if test="${fn:length(comingSoon) > homeLimit}">
                    <a href="${ctx}/movies?tab=coming-soon" class="text-sm font-semibold text-[#e50914] hover:underline flex items-center gap-1">
                        Xem tất cả <span class="material-symbols-outlined text-base">arrow_forward</span>
                    </a>
                </c:if>
            </div>
            <ui:movie-grid movies="${comingSoon}" emptyMessage="${emptyMessage}" limit="${homeLimit}"/>
        </section>

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

    <jsp:include page="/WEB-INF/views/common/public-footer.jsp"/>
    <script src="${ctx}/assets/js/movie-suggest.js"></script>
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
