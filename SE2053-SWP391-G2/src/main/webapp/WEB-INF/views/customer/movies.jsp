<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="comingSoonTab" value="${tab == 'coming-soon'}"/>
<c:set var="inputClass" value="w-full h-11 px-4 rounded-xl bg-[#0f131d] border border-white/10 text-sm text-white placeholder-[#8e919f] focus:border-[#e50914] focus:outline-none transition"/>
<%-- Link tab giữ nguyên bộ lọc đang áp dụng (UC-05); filterQuery do servlet URL-encode sẵn --%>
<c:set var="nowShowingUrl" value="${ctx}/movies?${filterQuery}tab=now-showing"/>
<c:set var="comingSoonUrl" value="${ctx}/movies?${filterQuery}tab=coming-soon"/>
<!DOCTYPE html>
<html lang="vi" class="dark">
<head>
    <title>${comingSoonTab ? 'Phim sắp chiếu' : 'Phim đang chiếu'} - CineLuxe Cinema</title>
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

    <main class="flex-1 max-w-7xl w-full mx-auto p-6 sm:p-10 space-y-6">
        <%-- UC-06 E1: phim được chọn không còn xem được, chuyển về đây kèm thông báo --%>
        <c:if test="${not empty sessionScope.flashError}">
            <div class="p-4 bg-red-950/80 border border-red-500/40 rounded-xl text-red-300 text-sm flex items-center gap-3" role="alert">
                <span class="material-symbols-outlined text-red-400 text-xl">error</span>
                <span><c:out value="${sessionScope.flashError}"/></span>
            </div>
            <c:remove var="flashError" scope="session"/>
        </c:if>

        <div>
            <h1 class="text-2xl sm:text-3xl font-extrabold text-white">Phim</h1>
            <p class="text-sm text-[#8e919f] mt-1">Danh sách phim đang chiếu và sắp chiếu tại CineLuxe.</p>
        </div>

        <!-- UC-05: Tìm kiếm & lọc phim -->
        <form method="get" action="${ctx}/movies" class="bg-[#171b26] border border-white/10 rounded-2xl p-4 sm:p-5 space-y-3" role="search">
            <div class="relative">
                <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-[#8e919f]">search</span>
                <input type="search" id="movie-search" name="q" maxlength="100" value="<c:out value='${search.keyword}'/>"
                       data-movie-suggest="${ctx}/movies/suggest" data-movie-detail="${ctx}/movies/detail?id="
                       placeholder="Nhập tên phim cần tìm..." aria-label="Tên phim"
                       class="${inputClass} pl-10"/>
            </div>
            <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
                <select name="genre" aria-label="Thể loại" class="${inputClass}">
                    <option value="">Tất cả thể loại</option>
                    <c:forEach var="genre" items="${genres}">
                        <option value="${genre.genreId}" ${search.genreId == genre.genreId ? 'selected' : ''}><c:out value="${genre.genreName}"/></option>
                    </c:forEach>
                </select>
                <input type="text" name="director" maxlength="100" value="<c:out value='${search.director}'/>"
                       placeholder="Đạo diễn" aria-label="Đạo diễn" class="${inputClass}"/>
                <input type="text" name="cast" maxlength="100" value="<c:out value='${search.cast}'/>"
                       placeholder="Diễn viên" aria-label="Diễn viên" class="${inputClass}"/>
                <%-- Cùng nhãn với ô Phân loại tuổi ở trang quản lý phim; color-scheme để danh sách xổ xuống cũng nền tối --%>
                <c:set var="ageLabels" value="${{'P':'Mọi lứa tuổi','C13':'Từ 13 tuổi','C16':'Từ 16 tuổi','C18':'Từ 18 tuổi'}}"/>
                <select name="age" aria-label="Phân loại tuổi" class="${inputClass} [color-scheme:dark]">
                    <option value="">-- Chọn --</option>
                    <c:forEach var="rating" items="${ageRatings}">
                        <option value="${rating}" ${search.ageRating == rating ? 'selected' : ''}>${rating} - ${ageLabels[rating]}</option>
                    </c:forEach>
                </select>
            </div>
            <div class="flex flex-wrap items-center justify-end gap-3">
                <c:if test="${filtered}">
                    <a href="${ctx}/movies" class="h-11 px-5 rounded-xl border border-white/10 text-sm font-semibold text-[#dfe2f1] hover:bg-white/5 flex items-center gap-1 transition">
                        <span class="material-symbols-outlined text-base">filter_alt_off</span>
                        Xóa bộ lọc
                    </a>
                </c:if>
                <button type="submit" class="h-11 px-6 rounded-xl bg-[#e50914] hover:bg-[#b80710] text-white text-sm font-bold flex items-center gap-1 transition">
                    <span class="material-symbols-outlined text-base">search</span>
                    Tìm kiếm
                </button>
            </div>
        </form>

        <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
            <!-- Hai danh mục theo trạng thái phim; số trong ngoặc là số phim khớp bộ lọc -->
            <nav class="flex gap-2" aria-label="Danh mục phim">
                <a href="<c:out value='${nowShowingUrl}'/>" class="movie-tab ${comingSoonTab ? '' : 'active'}">
                    Đang chiếu (${fn:length(nowShowing)})
                </a>
                <a href="<c:out value='${comingSoonUrl}'/>" class="movie-tab ${comingSoonTab ? 'active' : ''}">
                    Sắp chiếu (${fn:length(comingSoon)})
                </a>
            </nav>
            <c:if test="${filtered}">
                <p class="text-sm text-[#8e919f]">
                    Tìm thấy <strong class="text-white">${fn:length(nowShowing) + fn:length(comingSoon)}</strong> phim phù hợp
                </p>
            </c:if>
        </div>

        <ui:movie-grid movies="${moviePage.items}" emptyMessage="${emptyMessage}"/>

        <%-- Phân trang tab đang xem; link trang giữ nguyên bộ lọc và tab --%>
        <c:if test="${moviePage.totalPages > 1}">
            <c:set var="pageBase" value="${comingSoonTab ? comingSoonUrl : nowShowingUrl}"/>
            <nav class="movie-pager" aria-label="Phân trang phim">
                <p class="text-sm text-[#8e919f]">
                    Hiển thị ${moviePage.fromItem}-${moviePage.toItem} trong tổng số ${moviePage.totalItems} phim
                </p>
                <div class="flex flex-wrap items-center gap-2">
                    <c:choose>
                        <c:when test="${moviePage.page > 1}">
                            <a class="page-link" href="<c:out value='${pageBase}'/>&page=${moviePage.page - 1}" aria-label="Trang trước">
                                <span class="material-symbols-outlined text-base">chevron_left</span>
                            </a>
                        </c:when>
                        <c:otherwise>
                            <span class="page-link disabled" aria-hidden="true"><span class="material-symbols-outlined text-base">chevron_left</span></span>
                        </c:otherwise>
                    </c:choose>
                    <c:forEach var="p" begin="1" end="${moviePage.totalPages}">
                        <a class="page-link ${p == moviePage.page ? 'active' : ''}" href="<c:out value='${pageBase}'/>&page=${p}"
                           ${p == moviePage.page ? 'aria-current="page"' : ''}>${p}</a>
                    </c:forEach>
                    <c:choose>
                        <c:when test="${moviePage.page < moviePage.totalPages}">
                            <a class="page-link" href="<c:out value='${pageBase}'/>&page=${moviePage.page + 1}" aria-label="Trang sau">
                                <span class="material-symbols-outlined text-base">chevron_right</span>
                            </a>
                        </c:when>
                        <c:otherwise>
                            <span class="page-link disabled" aria-hidden="true"><span class="material-symbols-outlined text-base">chevron_right</span></span>
                        </c:otherwise>
                    </c:choose>
                </div>
            </nav>
        </c:if>
    </main>

    <jsp:include page="/WEB-INF/views/common/public-footer.jsp"/>
    <script src="${ctx}/assets/js/movie-suggest.js"></script>
</body>
</html>
