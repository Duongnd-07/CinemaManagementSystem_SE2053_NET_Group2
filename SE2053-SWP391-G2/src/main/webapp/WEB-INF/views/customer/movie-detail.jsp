<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" import="java.time.format.DateTimeFormatter" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="dateFormat" value="${DateTimeFormatter.ofPattern('dd/MM/yyyy')}"/>
<%-- Mô tả nhãn phân loại theo Thông tư 12/2015/TT-BVHTTDL (hệ nhãn P/C13/C16/C18) --%>
<c:set var="ageNotes" value="${{'P':'Phim được phép phổ biến đến người xem ở mọi độ tuổi.','C13':'Phim cấm phổ biến đến người xem dưới 13 tuổi.','C16':'Phim cấm phổ biến đến người xem dưới 16 tuổi.','C18':'Phim cấm phổ biến đến người xem dưới 18 tuổi.'}}"/>
<c:set var="comingSoon" value="${movie.status == 'Coming Soon'}"/>
<c:set var="posterSrc" value="${empty movie.posterUrl ? '' : (fn:startsWith(movie.posterUrl, 'http') ? movie.posterUrl : ctx.concat('/').concat(movie.posterUrl))}"/>
<!DOCTYPE html>
<html lang="vi" class="dark">
<head>
    <title><c:out value="${movie.title}"/> - CineLuxe Cinema</title>
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

    <main class="flex-1">
        <!-- Phần đầu: poster + thông tin chính, nền là poster làm mờ -->
        <section class="relative overflow-hidden border-b border-white/10">
            <c:if test="${not empty posterSrc}">
                <img class="detail-backdrop" alt="" aria-hidden="true" onerror="this.remove()" referrerpolicy="no-referrer" src="<c:out value='${posterSrc}'/>"/>
            </c:if>
            <div class="relative max-w-7xl mx-auto px-6 sm:px-10 py-8 sm:py-12">
                <a href="${ctx}/movies?tab=${comingSoon ? 'coming-soon' : 'now-showing'}"
                   class="inline-flex items-center gap-1 text-sm font-semibold text-[#8e919f] hover:text-white transition mb-6">
                    <span class="material-symbols-outlined text-base">arrow_back</span>
                    Danh sách phim
                </a>

                <div class="flex flex-col md:flex-row gap-6 md:gap-10">
                    <div class="movie-poster detail-poster shrink-0 mx-auto md:mx-0 rounded-2xl overflow-hidden border border-white/10 shadow-2xl">
                        <span class="material-symbols-outlined text-6xl text-[#8e919f]">image</span>
                        <c:if test="${not empty posterSrc}">
                            <img alt="Poster <c:out value='${movie.title}'/>" onerror="this.remove()" referrerpolicy="no-referrer" src="<c:out value='${posterSrc}'/>"/>
                        </c:if>
                    </div>

                    <div class="flex-1 min-w-0 space-y-5">
                        <div class="flex flex-wrap items-center gap-2">
                            <span class="px-3 py-1 rounded-full text-xs font-bold border ${comingSoon ? 'bg-amber-500/15 text-amber-400 border-amber-500/30' : 'bg-emerald-500/15 text-emerald-400 border-emerald-500/30'}">
                                ${comingSoon ? 'Sắp chiếu' : 'Đang chiếu'}
                            </span>
                            <c:if test="${not empty movie.formats}">
                                <c:forEach var="format" items="${fn:split(movie.formats, ',')}">
                                    <span class="px-3 py-1 rounded-full text-xs font-bold bg-white/5 text-[#dfe2f1] border border-white/10"><c:out value="${fn:trim(format)}"/></span>
                                </c:forEach>
                            </c:if>
                        </div>

                        <h1 class="text-3xl sm:text-4xl font-extrabold text-white leading-tight"><c:out value="${movie.title}"/></h1>

                        <!-- Nhãn tuổi nổi bật kèm mô tả theo quy định -->
                        <div class="flex items-center gap-3 p-3 rounded-xl bg-black/30 border border-white/10 w-fit max-w-full">
                            <span class="age-badge age-badge-lg age-${fn:toLowerCase(movie.ageRating)}"><c:out value="${movie.ageRating}"/></span>
                            <span class="text-sm text-[#e9bcb6]"><c:out value="${ageNotes[movie.ageRating]}"/></span>
                        </div>

                        <dl class="grid grid-cols-1 sm:grid-cols-2 gap-x-8 gap-y-3 text-sm">
                            <div class="flex gap-2">
                                <dt class="detail-label"><span class="material-symbols-outlined text-base">category</span>Thể loại</dt>
                                <dd class="text-white">
                                    <c:choose>
                                        <c:when test="${not empty movie.genres}">
                                            <c:forEach var="genre" items="${movie.genres}" varStatus="loop"><c:out value="${genre.genreName}"/><c:if test="${not loop.last}">, </c:if></c:forEach>
                                        </c:when>
                                        <c:otherwise>Chưa phân loại</c:otherwise>
                                    </c:choose>
                                </dd>
                            </div>
                            <div class="flex gap-2">
                                <dt class="detail-label"><span class="material-symbols-outlined text-base">schedule</span>Thời lượng</dt>
                                <dd class="text-white">${movie.durationMinutes} phút</dd>
                            </div>
                            <div class="flex gap-2">
                                <dt class="detail-label"><span class="material-symbols-outlined text-base">calendar_month</span>Khởi chiếu</dt>
                                <dd class="text-white">${empty movie.releaseDate ? 'Đang cập nhật' : movie.releaseDate.format(dateFormat)}</dd>
                            </div>
                            <div class="flex gap-2">
                                <dt class="detail-label"><span class="material-symbols-outlined text-base">movie_edit</span>Đạo diễn</dt>
                                <dd class="text-white"><c:out value="${empty movie.director ? 'Đang cập nhật' : movie.director}"/></dd>
                            </div>
                            <div class="flex gap-2">
                                <dt class="detail-label"><span class="material-symbols-outlined text-base">public</span>Quốc gia</dt>
                                <dd class="text-white"><c:out value="${empty movie.country ? 'Đang cập nhật' : movie.country}"/></dd>
                            </div>
                            <div class="flex gap-2">
                                <dt class="detail-label"><span class="material-symbols-outlined text-base">translate</span>Ngôn ngữ</dt>
                                <dd class="text-white"><c:out value="${empty movie.language ? 'Đang cập nhật' : movie.language}"/></dd>
                            </div>
                            <div class="flex gap-2 sm:col-span-2">
                                <dt class="detail-label"><span class="material-symbols-outlined text-base">business</span>Nhà phát hành</dt>
                                <dd class="text-white"><c:out value="${empty movie.distributor ? 'Đang cập nhật' : movie.distributor}"/></dd>
                            </div>
                            <div class="flex gap-2 sm:col-span-2">
                                <dt class="detail-label"><span class="material-symbols-outlined text-base">groups</span>Diễn viên</dt>
                                <dd class="text-white"><c:out value="${empty movie.cast ? 'Đang cập nhật' : movie.cast}"/></dd>
                            </div>
                        </dl>

                        <!-- Hành động: Xem lịch chiếu luôn có; Đặt vé chỉ khi còn suất chiếu sắp tới -->
                        <div class="flex flex-wrap items-center gap-3 pt-2">
                            <c:if test="${canBook}">
                                <a href="${ctx}/showtimes?movieId=${movie.movieId}"
                                   class="h-12 px-7 rounded-xl bg-[#e50914] hover:bg-[#b80710] text-white text-sm font-bold flex items-center gap-2 transition shadow-[0_0_20px_-2px_rgba(229,9,20,0.6)]">
                                    <span class="material-symbols-outlined">confirmation_number</span>
                                    Đặt vé ngay
                                </a>
                            </c:if>
                            <a href="${ctx}/showtimes?movieId=${movie.movieId}"
                               class="h-12 px-6 rounded-xl bg-white/5 hover:bg-white/10 border border-white/15 text-white text-sm font-semibold flex items-center gap-2 transition">
                                <span class="material-symbols-outlined">event</span>
                                Xem lịch chiếu
                            </a>
                            <c:if test="${not canBook}">
                                <span class="text-sm text-[#8e919f] flex items-center gap-1">
                                    <span class="material-symbols-outlined text-base">info</span>
                                    ${comingSoon ? 'Phim chưa mở bán vé.' : 'Hiện chưa có suất chiếu sắp tới.'}
                                </span>
                            </c:if>
                        </div>
                    </div>
                </div>
            </div>
        </section>

        <div class="max-w-7xl mx-auto px-6 sm:px-10 py-8 sm:py-10 grid grid-cols-1 lg:grid-cols-5 gap-8">
            <!-- Nội dung phim -->
            <section class="lg:col-span-2 space-y-3">
                <h2 class="text-xl font-extrabold text-white flex items-center gap-2">
                    <span class="w-1.5 h-6 rounded-full bg-[#e50914]"></span>Nội dung phim
                </h2>
                <p class="text-sm leading-7 text-[#dfe2f1] whitespace-pre-line"><c:out value="${empty movie.synopsis ? 'Nội dung phim đang được cập nhật.' : movie.synopsis}"/></p>
            </section>

            <!-- Trailer: chỉ nhúng trình phát khi có link YouTube hợp lệ -->
            <section class="lg:col-span-3 space-y-3">
                <h2 class="text-xl font-extrabold text-white flex items-center gap-2">
                    <span class="w-1.5 h-6 rounded-full bg-[#ffb95f]"></span>Trailer
                </h2>
                <c:choose>
                    <c:when test="${not empty trailerId}">
                        <div class="detail-trailer">
                            <iframe src="https://www.youtube-nocookie.com/embed/${trailerId}"
                                    title="Trailer <c:out value='${movie.title}'/>" loading="lazy"
                                    allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
                                    referrerpolicy="strict-origin-when-cross-origin" allowfullscreen></iframe>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div class="movie-empty">
                            <span class="material-symbols-outlined text-4xl text-[#8e919f]">videocam_off</span>
                            <p>Phim chưa có trailer.</p>
                        </div>
                    </c:otherwise>
                </c:choose>
            </section>
        </div>
    </main>

    <jsp:include page="/WEB-INF/views/common/public-footer.jsp"/>
</body>
</html>
