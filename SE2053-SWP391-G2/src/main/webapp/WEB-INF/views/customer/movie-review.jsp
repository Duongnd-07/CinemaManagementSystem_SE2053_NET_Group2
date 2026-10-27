<%-- UC-17: trang đánh giá phim đã Ended (không còn trang chi tiết công khai) cho khách đã xem --%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="posterSrc" value="${empty movie.posterUrl ? '' : (fn:startsWith(movie.posterUrl, 'http') ? movie.posterUrl : ctx.concat('/').concat(movie.posterUrl))}"/>
<!DOCTYPE html>
<html lang="vi" class="dark">
<head>
    <title>Đánh giá <c:out value="${movie.title}"/> - CineLuxe Cinema</title>
    <jsp:include page="/WEB-INF/views/common/public-head.jsp"/>
</head>
<body class="min-h-screen bg-[#0f131d] text-[#dfe2f1] flex flex-col">
    <jsp:include page="/WEB-INF/views/common/public-header.jsp"/>
    <main class="flex-1 max-w-4xl w-full mx-auto px-4 lg:px-8 py-10">
        <a href="${ctx}/customer/bookings" class="inline-flex items-center gap-1 text-sm font-semibold text-[#8e919f] hover:text-white transition mb-6">
            <span class="material-symbols-outlined text-base">arrow_back</span>Lịch sử đặt vé
        </a>

        <c:if test="${not empty flashSuccess or not empty flashError}">
            <div role="status" class="mb-5 rounded-xl border p-4 ${not empty flashSuccess ? 'border-emerald-500/30 bg-emerald-950/40 text-emerald-200' : 'border-red-500/30 bg-red-950/40 text-red-200'}">
                <c:out value="${not empty flashSuccess ? flashSuccess : flashError}"/>
            </div>
        </c:if>

        <div class="flex items-center gap-4 mb-8">
            <div class="w-16 h-24 rounded-lg overflow-hidden border border-white/10 bg-[#171b26] shrink-0 flex items-center justify-center">
                <c:choose>
                    <c:when test="${not empty posterSrc}">
                        <img alt="Poster <c:out value='${movie.title}'/>" onerror="this.remove()" referrerpolicy="no-referrer" src="<c:out value='${posterSrc}'/>" class="w-full h-full object-cover"/>
                    </c:when>
                    <c:otherwise><span class="material-symbols-outlined text-3xl text-[#8e919f]">image</span></c:otherwise>
                </c:choose>
            </div>
            <div>
                <p class="text-xs uppercase tracking-widest text-[#e9bcb6]">Phim đã kết thúc chiếu</p>
                <h1 class="text-2xl font-bold text-white"><c:out value="${movie.title}"/></h1>
            </div>
        </div>

        <ui:review-panel movieId="${movie.movieId}" baseUrl="${ctx}/customer/reviews?movieId=${movie.movieId}"
                          summary="${reviewSummary}" myReview="${myReview}" reviewPage="${reviewPage}"
                          canReview="${canReview}" isCustomer="${true}" csrfToken="${csrfToken}"
                          star="${reviewStar}" sort="${reviewSort}"/>
    </main>
    <jsp:include page="/WEB-INF/views/common/public-footer.jsp"/>
    <script src="${ctx}/assets/js/movie-review.js"></script>
</body>
</html>
