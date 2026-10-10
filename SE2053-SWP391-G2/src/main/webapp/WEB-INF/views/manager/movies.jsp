<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="statusLabels" value="${{'Now Showing':'Đang chiếu','Coming Soon':'Sắp chiếu','Ended':'Đã kết thúc','Hidden':'Đã ẩn'}}"/>
<c:set var="statusStyles" value="${{'Now Showing':'bg-emerald-500/15 text-emerald-400 border-emerald-500/30','Coming Soon':'bg-amber-500/15 text-amber-400 border-amber-500/30','Ended':'bg-surface-bright text-outline border-white/10','Hidden':'bg-error/10 text-error border-error/30'}}"/>
<c:set var="statusDots" value="${{'Now Showing':'bg-emerald-400 animate-pulse','Coming Soon':'bg-amber-400','Ended':'bg-outline','Hidden':'bg-error'}}"/>
<c:set var="inputClass" value="form-input bg-surface-container-high text-on-surface px-4 rounded-lg border border-white/10 focus:border-primary outline-none"/>
<!DOCTYPE html>
<html lang="vi" class="dark">
<head>
    <title>Quản lý phim - CineAdmin</title>
    <jsp:include page="/WEB-INF/views/common/manager-head.jsp"/>
</head>
<body class="bg-surface font-body-md text-on-surface">
<jsp:include page="/WEB-INF/views/common/manager-sidebar.jsp"/>

<main class="pl-64 pt-16">
<div class="px-space-xl pt-space-lg pb-16">

    <%-- Toast thông báo --%>
    <c:if test="${not empty flashSuccess or not empty flashError}">
        <div id="toast" role="status"
             class="fixed top-20 right-8 z-[60] flex items-center gap-space-sm px-space-lg py-space-md rounded-xl shadow-2xl border
                    ${not empty flashSuccess ? 'bg-emerald-500/15 border-emerald-500/40 text-emerald-300' : 'bg-error-container/80 border-error/40 text-on-error-container'}">
            <span class="material-symbols-outlined">${not empty flashSuccess ? 'check_circle' : 'error'}</span>
            <span class="font-label-lg"><c:out value="${not empty flashSuccess ? flashSuccess : flashError}"/></span>
        </div>
    </c:if>

    <div class="flex flex-col md:flex-row md:items-center justify-between gap-space-md mb-space-xl">
        <div>
            <div class="flex items-center gap-space-sm mb-space-xs">
                <span class="text-label-sm px-2 py-0.5 rounded bg-primary/20 text-primary uppercase">Quản lý rạp</span>
                <span class="text-outline text-body-sm">/</span>
                <span class="text-on-surface-variant text-body-sm">Phim</span>
            </div>
            <h1 class="text-headline-lg text-on-surface">Quản lý Phim</h1>
        </div>
        <div class="flex items-center gap-space-md">
            <form method="get" action="${ctx}/manager/movies" class="relative w-72">
                <c:if test="${not empty statusFilter}"><input type="hidden" name="status" value="${fn:escapeXml(statusFilter)}"/></c:if>
                <c:if test="${not empty sort}"><input type="hidden" name="sort" value="${fn:escapeXml(sort)}"/></c:if>
                <span class="material-symbols-outlined absolute left-3 top-2.5 text-outline text-[20px]">search</span>
                <input class="w-full bg-surface-container-high text-on-surface text-body-md pl-10 pr-4 py-2 rounded-md outline-none focus:ring-2 focus:ring-primary/40 border border-white/10"
                       type="text" name="q" value="<c:out value='${keyword}'/>" maxlength="100"
                       placeholder="Tìm theo tên, đạo diễn, diễn viên..."/>
            </form>
            <a href="${ctx}/manager/movies?add=1"
               class="flex items-center gap-space-sm bg-primary hover:bg-primary/90 text-on-primary px-space-lg py-2 rounded-full font-label-lg shadow-[0_0_20px_-2px_rgba(229,9,20,0.45)] transition-all whitespace-nowrap">
                <span class="material-symbols-outlined text-[20px]">add</span>
                <span>Thêm phim mới</span>
            </a>
        </div>
    </div>

    <%-- Bộ lọc trạng thái + sắp xếp --%>
    <div class="flex flex-wrap items-center justify-between gap-space-md bg-surface-container-low p-space-md rounded-xl mb-space-lg">
        <div class="flex flex-wrap items-center gap-space-sm">
            <c:set var="tabOn" value="bg-primary-container text-on-primary-container shadow-[0_0_15px_-2px_rgba(229,9,20,0.4)]"/>
            <c:set var="tabOff" value="bg-surface-container-high text-on-surface-variant hover:text-on-surface"/>
            <c:url var="allTabUrl" value="/manager/movies">
                <c:if test="${not empty keyword}"><c:param name="q" value="${keyword}"/></c:if>
                <c:if test="${not empty sort}"><c:param name="sort" value="${sort}"/></c:if>
            </c:url>
            <a class="px-4 py-2 rounded-lg font-label-lg text-sm transition-all ${empty statusFilter ? tabOn : tabOff}"
               href="${fn:escapeXml(allTabUrl)}">Tất cả (${allCount})</a>
            <c:forEach var="st" items="${statuses}">
                <c:set var="cnt" value="${statusCounts[st]}"/>
                <c:url var="tabUrl" value="/manager/movies">
                    <c:param name="status" value="${st}"/>
                    <c:if test="${not empty keyword}"><c:param name="q" value="${keyword}"/></c:if>
                    <c:if test="${not empty sort}"><c:param name="sort" value="${sort}"/></c:if>
                </c:url>
                <a class="px-4 py-2 rounded-lg font-label-lg text-sm transition-all ${statusFilter == st ? tabOn : tabOff}"
                   href="${fn:escapeXml(tabUrl)}">
                    ${statusLabels[st]} (${not empty cnt ? cnt : 0})
                </a>
            </c:forEach>
        </div>
        <form method="get" action="${ctx}/manager/movies" class="flex items-center gap-space-sm text-on-surface-variant text-body-sm">
            <c:if test="${not empty keyword}"><input type="hidden" name="q" value="${fn:escapeXml(keyword)}"/></c:if>
            <c:if test="${not empty statusFilter}"><input type="hidden" name="status" value="${fn:escapeXml(statusFilter)}"/></c:if>
            <label for="sort">Sắp xếp:</label>
            <select id="sort" name="sort" data-auto-submit
                    class="bg-surface-container-high text-on-surface px-3 py-1.5 rounded-md outline-none border border-white/10">
                <option value="" ${empty sort ? 'selected' : ''}>Mới nhất</option>
                <option value="name" ${sort == 'name' ? 'selected' : ''}>Tên A-Z</option>
                <option value="release" ${sort == 'release' ? 'selected' : ''}>Ngày khởi chiếu</option>
            </select>
        </form>
    </div>

    <%-- Bảng danh sách phim --%>
    <div class="bg-surface-container-low rounded-xl overflow-hidden shadow-xl border border-white/5">
        <div class="overflow-x-auto">
            <table class="w-full text-left border-collapse">
                <thead>
                <tr class="bg-surface-container border-b border-white/10 text-on-surface-variant text-label-md uppercase tracking-wider">
                    <th class="py-4 px-6">Mã &amp; Poster</th>
                    <th class="py-4 px-6">Tên phim &amp; Thể loại</th>
                    <th class="py-4 px-6">Định dạng</th>
                    <th class="py-4 px-6">Thời lượng</th>
                    <th class="py-4 px-6">Trạng thái</th>
                    <th class="py-4 px-6 text-right">Hành động</th>
                </tr>
                </thead>
                <tbody class="divide-y divide-white/5 text-body-md text-on-surface">
                <c:forEach var="movie" items="${moviePage.items}">
                    <tr class="hover:bg-surface-container transition-colors">
                        <td class="py-4 px-6">
                            <div class="flex items-center gap-space-md">
                                <span class="text-outline font-label-sm">#MV-<fmt:formatNumber value="${movie.movieId}" minIntegerDigits="3" groupingUsed="false"/></span>
                                <div class="w-10 h-14 rounded overflow-hidden bg-surface-container-high shrink-0 flex items-center justify-center">
                                    <c:choose>
                                        <c:when test="${not empty movie.posterUrl}">
                                            <img class="w-full h-full object-cover" alt="" loading="lazy"
                                                 src="${fn:startsWith(movie.posterUrl, 'http') ? '' : ctx.concat('/')}<c:out value='${movie.posterUrl}'/>"/>
                                        </c:when>
                                        <c:otherwise><span class="material-symbols-outlined text-outline">image</span></c:otherwise>
                                    </c:choose>
                                </div>
                            </div>
                        </td>
                        <td class="py-4 px-6">
                            <div class="font-headline-sm text-on-surface"><c:out value="${movie.title}"/></div>
                            <div class="text-body-sm text-outline">
                                <c:forEach var="genre" items="${movie.genres}" varStatus="loop"><c:out value="${genre.genreName}"/><c:if test="${not loop.last}">, </c:if></c:forEach>
                                <c:if test="${not empty movie.genres}"> • </c:if><c:out value="${movie.ageRating}"/>
                            </div>
                        </td>
                        <td class="py-4 px-6">
                            <div class="flex items-center gap-1.5 flex-wrap">
                                <c:forEach var="format" items="${fn:split(movie.formats, ',')}">
                                    <span class="px-2 py-0.5 rounded text-[11px] font-bold ${format == 'IMAX' ? 'bg-primary/20 text-primary border border-primary/40' : 'bg-surface-container-high text-on-surface-variant'}"><c:out value="${format}"/></span>
                                </c:forEach>
                            </div>
                        </td>
                        <td class="py-4 px-6 text-on-surface-variant">${movie.durationMinutes} phút</td>
                        <td class="py-4 px-6">
                            <span class="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-label-sm border ${statusStyles[movie.status]}">
                                <span class="w-1.5 h-1.5 rounded-full ${statusDots[movie.status]}"></span>
                                <c:out value="${statusLabels[movie.status]}"/>
                            </span>
                        </td>
                        <td class="py-4 px-6 text-right">
                            <div class="flex items-center justify-end gap-2">
                                <a class="p-2 rounded-lg bg-surface-container-high hover:bg-surface-bright text-on-surface-variant hover:text-on-surface transition-all"
                                   title="Sửa" href="${ctx}/manager/movies?edit=${movie.movieId}${fn:escapeXml(fn:replace(listQuery, '?', '&'))}">
                                    <span class="material-symbols-outlined text-[18px]">edit</span>
                                </a>
                                <form method="post" action="${ctx}/manager/movies${listQuery}" class="inline"
                                      data-confirm="Ẩn phim &quot;${fn:escapeXml(movie.title)}&quot; khỏi danh mục công khai?">
                                    <input type="hidden" name="csrfToken" value="${csrfToken}"/>
                                    <input type="hidden" name="action" value="archive"/>
                                    <input type="hidden" name="movieId" value="${movie.movieId}"/>
                                    <button type="submit" title="Ẩn phim" ${movie.status == 'Hidden' ? 'disabled' : ''}
                                            class="p-2 rounded-lg bg-error/10 hover:bg-error/20 text-error transition-all disabled:opacity-30 disabled:cursor-not-allowed">
                                        <span class="material-symbols-outlined text-[18px]">delete</span>
                                    </button>
                                </form>
                            </div>
                        </td>
                    </tr>
                </c:forEach>
                <c:if test="${empty moviePage.items}">
                    <tr><td colspan="6" class="py-12 px-6 text-center text-on-surface-variant"><c:out value="${msgNoResult}"/></td></tr>
                </c:if>
                </tbody>
            </table>
        </div>

        <c:url var="pageBase" value="/manager/movies">
            <c:if test="${not empty keyword}"><c:param name="q" value="${keyword}"/></c:if>
            <c:if test="${not empty statusFilter}"><c:param name="status" value="${statusFilter}"/></c:if>
            <c:if test="${not empty sort}"><c:param name="sort" value="${sort}"/></c:if>
        </c:url>
        <ui:pagination pageData="${moviePage}" baseUrl="${pageBase}" label="phim"/>
    </div>
</div>
</main>

<%-- Form thêm/sửa phim --%>
<c:if test="${showForm}">
    <div class="fixed inset-0 bg-black/80 backdrop-blur-md z-50 flex items-center justify-center p-4 overflow-y-auto" id="movie-modal" data-modal>
        <div class="bg-surface-container-low border border-white/10 rounded-2xl w-full max-w-3xl p-space-xl shadow-2xl relative my-8">
            <a class="absolute right-4 top-4 text-on-surface-variant hover:text-on-surface"
               href="${ctx}/manager/movies${listQuery}" title="Đóng">
                <span class="material-symbols-outlined">close</span>
            </a>
            <h3 class="text-headline-md text-on-surface mb-space-lg flex items-center gap-space-sm">
                <span class="material-symbols-outlined text-primary">movie</span>
                ${form.movieId > 0 ? 'Chỉnh sửa phim' : 'Thêm phim mới vào hệ thống'}
            </h3>

            <c:if test="${not empty errors.general}">
                <div class="mb-space-md form-error-banner"><c:out value="${errors.general}"/></div>
            </c:if>

            <form method="post" action="${ctx}/manager/movies${listQuery}" enctype="multipart/form-data" novalidate class="space-y-space-md">
                <input type="hidden" name="csrfToken" value="${csrfToken}"/>
                <input type="hidden" name="movieId" value="${form.movieId}"/>

                <div>
                    <label class="block text-label-md text-on-surface-variant mb-1" for="title">Tên phim <span class="req">*</span></label>
                    <input class="${inputClass}" id="title" name="title" type="text" maxlength="200"
                           value="<c:out value='${form.title}'/>" placeholder="VD: Avatar: The Way of Water"/>
                    <c:if test="${not empty errors.title}"><p class="form-error"><c:out value="${errors.title}"/></p></c:if>
                </div>

                <div class="grid grid-cols-1 md:grid-cols-3 gap-space-md">
                    <div>
                        <label class="block text-label-md text-on-surface-variant mb-1" for="duration">Thời lượng (phút) <span class="req">*</span></label>
                        <input class="${inputClass}" id="duration" name="duration" type="number" min="1" step="1"
                               value="<c:out value='${form.duration}'/>" placeholder="120"/>
                        <c:if test="${not empty errors.duration}"><p class="form-error"><c:out value="${errors.duration}"/></p></c:if>
                    </div>
                    <div>
                        <label class="block text-label-md text-on-surface-variant mb-1" for="ageRating">Phân loại tuổi <span class="req">*</span></label>
                        <select class="${inputClass}" id="ageRating" name="ageRating">
                            <option value="">-- Chọn --</option>
                            <option value="P" ${form.ageRating == 'P' ? 'selected' : ''}>P - Mọi lứa tuổi</option>
                            <option value="C13" ${form.ageRating == 'C13' ? 'selected' : ''}>C13 - Từ 13 tuổi</option>
                            <option value="C16" ${form.ageRating == 'C16' ? 'selected' : ''}>C16 - Từ 16 tuổi</option>
                            <option value="C18" ${form.ageRating == 'C18' ? 'selected' : ''}>C18 - Từ 18 tuổi</option>
                        </select>
                        <c:if test="${not empty errors.ageRating}"><p class="form-error"><c:out value="${errors.ageRating}"/></p></c:if>
                    </div>
                    <div>
                        <label class="block text-label-md text-on-surface-variant mb-1" for="releaseDate">Ngày khởi chiếu <span class="req">*</span></label>
                        <input class="${inputClass}" id="releaseDate" name="releaseDate" type="date"
                               value="<c:out value='${form.releaseDate}'/>"/>
                        <c:if test="${not empty errors.releaseDate}"><p class="form-error"><c:out value="${errors.releaseDate}"/></p></c:if>
                    </div>
                </div>

                <div class="grid grid-cols-1 md:grid-cols-2 gap-space-md">
                    <div>
                        <label class="block text-label-md text-on-surface-variant mb-1" for="director">Đạo diễn</label>
                        <input class="${inputClass}" id="director" name="director" type="text" maxlength="100"
                               value="<c:out value='${form.director}'/>"/>
                        <c:if test="${not empty errors.director}"><p class="form-error"><c:out value="${errors.director}"/></p></c:if>
                    </div>
                    <div>
                        <label class="block text-label-md text-on-surface-variant mb-1" for="cast">Diễn viên</label>
                        <input class="${inputClass}" id="cast" name="cast" type="text" maxlength="500"
                               value="<c:out value='${form.cast}'/>" placeholder="Ngăn cách bằng dấu phẩy"/>
                        <c:if test="${not empty errors.cast}"><p class="form-error"><c:out value="${errors.cast}"/></p></c:if>
                    </div>
                </div>

                <div>
                    <label class="block text-label-md text-on-surface-variant mb-1" for="synopsis">Nội dung phim</label>
                    <textarea class="${inputClass}" id="synopsis" name="synopsis" rows="3"><c:out value="${form.synopsis}"/></textarea>
                </div>

                <div>
                    <span class="block text-label-md text-on-surface-variant mb-2">Thể loại</span>
                    <div class="flex flex-wrap gap-2">
                        <c:forEach var="genre" items="${genres}">
                            <label class="cursor-pointer">
                                <input type="checkbox" class="peer sr-only" name="genreIds" value="${genre.genreId}"
                                       ${form.genreIds.contains(genre.genreId) ? 'checked' : ''}/>
                                <span class="inline-block px-3 py-1.5 rounded-lg text-body-sm bg-surface-container-high text-on-surface-variant border border-white/10 peer-checked:bg-primary/20 peer-checked:text-primary peer-checked:border-primary/50 peer-focus-visible:ring-2 peer-focus-visible:ring-primary/50 transition-all">
                                    <c:out value="${genre.genreName}"/>
                                </span>
                            </label>
                        </c:forEach>
                    </div>
                    <c:if test="${not empty errors.genres}"><p class="form-error"><c:out value="${errors.genres}"/></p></c:if>
                </div>

                <div>
                    <span class="block text-label-md text-on-surface-variant mb-2">Định dạng chiếu</span>
                    <div class="flex flex-wrap gap-2">
                        <c:forEach var="format" items="${formats}">
                            <label class="cursor-pointer">
                                <input type="checkbox" class="peer sr-only" name="formats" value="${format}"
                                       ${form.formats.contains(format) ? 'checked' : ''}/>
                                <span class="inline-block px-3 py-1.5 rounded-lg text-body-sm font-bold bg-surface-container-high text-on-surface-variant border border-white/10 peer-checked:bg-primary/20 peer-checked:text-primary peer-checked:border-primary/50 peer-focus-visible:ring-2 peer-focus-visible:ring-primary/50 transition-all">
                                    <c:out value="${format}"/>
                                </span>
                            </label>
                        </c:forEach>
                    </div>
                    <c:if test="${not empty errors.formats}"><p class="form-error"><c:out value="${errors.formats}"/></p></c:if>
                </div>

                <div class="grid grid-cols-1 md:grid-cols-2 gap-space-md">
                    <div>
                        <label class="block text-label-md text-on-surface-variant mb-1" for="posterFile">Poster (tải ảnh lên)</label>
                        <input class="${inputClass}" id="posterFile" name="posterFile" type="file" accept="image/jpeg,image/png,image/webp"/>
                        <c:if test="${not empty errors.poster}"><p class="form-error"><c:out value="${errors.poster}"/></p></c:if>
                    </div>
                    <div>
                        <label class="block text-label-md text-on-surface-variant mb-1" for="trailerUrl">Trailer (YouTube)</label>
                        <input class="${inputClass}" id="trailerUrl" name="trailerUrl" type="text" maxlength="500"
                               value="<c:out value='${form.trailerUrl}'/>" placeholder="https://www.youtube.com/watch?v=..."/>
                        <c:if test="${not empty errors.trailer}"><p class="form-error"><c:out value="${errors.trailer}"/></p></c:if>
                    </div>
                    <div>
                        <label class="block text-label-md text-on-surface-variant mb-1" for="posterUrl">hoặc URL ảnh poster</label>
                        <input class="${inputClass}" id="posterUrl" name="posterUrl" type="text" maxlength="500"
                               value="<c:out value='${form.posterUrl}'/>" placeholder="https://..."/>
                    </div>
                    <div>
                        <label class="block text-label-md text-on-surface-variant mb-1" for="movieStatus">Trạng thái chiếu <span class="req">*</span></label>
                        <select class="${inputClass}" id="movieStatus" name="movieStatus">
                            <c:forEach var="st" items="${statuses}">
                                <option value="${st}" ${form.status == st ? 'selected' : ''}><c:out value="${statusLabels[st]}"/></option>
                            </c:forEach>
                        </select>
                        <c:if test="${not empty errors.status}"><p class="form-error"><c:out value="${errors.status}"/></p></c:if>
                    </div>
                    <p class="md:col-span-2 -mt-2 text-outline text-body-sm">Chọn file sẽ thay thế URL. Tối đa 5MB (JPG, PNG, WEBP).</p>
                </div>

                <div class="flex items-center justify-end gap-space-md pt-space-md">
                    <a class="px-5 py-2.5 rounded-full bg-surface-container-high text-on-surface hover:bg-surface-bright font-label-lg transition-all"
                       href="${ctx}/manager/movies${listQuery}">Hủy bỏ</a>
                    <button type="submit"
                            class="px-6 py-2.5 rounded-full bg-primary text-on-primary hover:bg-primary/90 font-label-lg shadow-[0_0_20px_-2px_rgba(229,9,20,0.45)] transition-all">
                        ${form.movieId > 0 ? 'Lưu thay đổi' : 'Lưu phim'}
                    </button>
                </div>
            </form>
        </div>
    </div>
</c:if>

<script src="${ctx}/assets/js/manager-common.js"></script>
</body>
</html>
