<%@ tag body-content="empty" pageEncoding="UTF-8" import="java.time.format.DateTimeFormatter" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ attribute name="movies" required="true" type="java.util.List" description="Danh sách Movie đã sắp xếp" %>
<%@ attribute name="emptyMessage" required="true" description="Thông báo khi danh mục không có phim (UC-04 E1)" %>
<%@ attribute name="limit" type="java.lang.Integer" description="Số phim tối đa hiển thị; bỏ trống để hiển thị tất cả" %>
<%-- UC-04: lưới thẻ phim gồm Poster, Tên, Thể loại, Nhãn tuổi, Thời lượng, Ngày khởi chiếu --%>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="dateFormat" value="${DateTimeFormatter.ofPattern('dd/MM/yyyy')}"/>
<c:set var="ageLabels" value="${{'P':'Mọi lứa tuổi','C13':'Từ 13 tuổi','C16':'Từ 16 tuổi','C18':'Từ 18 tuổi'}}"/>
<c:choose>
    <c:when test="${empty movies}">
        <div class="movie-empty">
            <span class="material-symbols-outlined text-4xl text-[#8e919f]">movie_off</span>
            <p><c:out value="${emptyMessage}"/></p>
        </div>
    </c:when>
    <c:otherwise>
        <div class="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4 sm:gap-6">
            <c:forEach var="movie" items="${movies}" end="${empty limit ? fn:length(movies) - 1 : limit - 1}">
                <article class="movie-card">
                    <div class="movie-poster">
                        <%-- Biểu tượng nằm dưới ảnh: hiện ra khi phim chưa có poster hoặc ảnh tải lỗi --%>
                        <span class="material-symbols-outlined text-5xl text-[#8e919f]">image</span>
                        <c:if test="${not empty movie.posterUrl}">
                            <img alt="Poster <c:out value='${movie.title}'/>" loading="lazy" onerror="this.remove()"
                                 src="${fn:startsWith(movie.posterUrl, 'http') ? '' : ctx.concat('/')}<c:out value='${movie.posterUrl}'/>"/>
                        </c:if>
                        <span class="age-badge age-${fn:toLowerCase(movie.ageRating)}"
                              title="${ageLabels[movie.ageRating]}"><c:out value="${movie.ageRating}"/></span>
                    </div>
                    <div class="p-3 sm:p-4 flex flex-col gap-1.5 flex-1">
                        <h3 class="font-bold text-white text-sm sm:text-base leading-snug line-clamp-2" title="<c:out value='${movie.title}'/>">
                            <c:out value="${movie.title}"/>
                        </h3>
                        <p class="text-xs text-[#e9bcb6] line-clamp-1">
                            <c:choose>
                                <c:when test="${not empty movie.genres}">
                                    <c:forEach var="genre" items="${movie.genres}" varStatus="loop"><c:out value="${genre.genreName}"/><c:if test="${not loop.last}">, </c:if></c:forEach>
                                </c:when>
                                <c:otherwise>Chưa phân loại</c:otherwise>
                            </c:choose>
                        </p>
                        <div class="mt-auto pt-2 flex items-center justify-between gap-2 text-[11px] sm:text-xs text-[#8e919f]">
                            <span class="flex items-center gap-1">
                                <span class="material-symbols-outlined text-sm">schedule</span>${movie.durationMinutes} phút
                            </span>
                            <span class="flex items-center gap-1">
                                <span class="material-symbols-outlined text-sm">calendar_month</span>
                                <c:if test="${not empty movie.releaseDate}">${movie.releaseDate.format(dateFormat)}</c:if>
                            </span>
                        </div>
                    </div>
                </article>
            </c:forEach>
        </div>
    </c:otherwise>
</c:choose>
