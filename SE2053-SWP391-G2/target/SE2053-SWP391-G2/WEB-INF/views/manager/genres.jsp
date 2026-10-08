<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="inputClass" value="form-input bg-surface-container-high text-on-surface px-4 rounded-lg border border-white/10 focus:border-primary outline-none"/>
<!DOCTYPE html>
<html lang="vi" class="dark">
<head>
    <title>Quản lý thể loại phim - CineAdmin</title>
    <jsp:include page="/WEB-INF/views/common/manager-head.jsp"/>
</head>
<body class="bg-surface font-body-md text-on-surface">
<jsp:include page="/WEB-INF/views/common/manager-sidebar.jsp"/>

<main class="pl-64 pt-16">
<div class="px-space-xl pt-space-lg pb-16">

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
                <span class="text-on-surface-variant text-body-sm">Thể loại phim</span>
            </div>
            <h1 class="text-headline-lg text-on-surface">Quản lý Thể loại phim</h1>
        </div>
        <div class="flex items-center gap-space-md">
            <form method="get" action="${ctx}/manager/genres" class="relative w-72">
                <span class="material-symbols-outlined absolute left-3 top-2.5 text-outline text-[20px]">search</span>
                <input class="w-full bg-surface-container-high text-on-surface text-body-md pl-10 pr-4 py-2 rounded-md outline-none focus:ring-2 focus:ring-primary/40 border border-white/10"
                       type="text" name="q" value="<c:out value='${keyword}'/>" maxlength="50"
                       placeholder="Tìm thể loại..."/>
            </form>
            <a href="${ctx}/manager/genres?add=1${fn:escapeXml(fn:replace(listQuery, '?', '&'))}"
               class="flex items-center gap-space-sm bg-primary hover:bg-primary/90 text-on-primary px-space-lg py-2 rounded-full font-label-lg shadow-[0_0_20px_-2px_rgba(229,9,20,0.45)] transition-all whitespace-nowrap">
                <span class="material-symbols-outlined text-[20px]">add</span>
                <span>Thêm thể loại mới</span>
            </a>
        </div>
    </div>

    <div class="bg-surface-container-low rounded-xl overflow-hidden shadow-xl border border-white/5">
        <div class="overflow-x-auto">
            <table class="w-full text-left border-collapse">
                <thead>
                <tr class="bg-surface-container border-b border-white/10 text-on-surface-variant text-label-md uppercase tracking-wider">
                    <th class="py-4 px-6">Mã</th>
                    <th class="py-4 px-6">Tên thể loại</th>
                    <th class="py-4 px-6">Số phim đang gán</th>
                    <th class="py-4 px-6 text-right">Hành động</th>
                </tr>
                </thead>
                <tbody class="divide-y divide-white/5 text-body-md text-on-surface">
                <c:forEach var="genre" items="${genrePage.genres}">
                    <tr class="hover:bg-surface-container transition-colors">
                        <td class="py-4 px-6 text-outline font-label-sm">#GN-${genre.genreId}</td>
                        <td class="py-4 px-6 font-headline-sm"><c:out value="${genre.genreName}"/></td>
                        <td class="py-4 px-6 text-on-surface-variant">${genre.movieCount} phim</td>
                        <td class="py-4 px-6 text-right">
                            <div class="flex items-center justify-end gap-2">
                                <a class="p-2 rounded-lg bg-surface-container-high hover:bg-surface-bright text-on-surface-variant hover:text-on-surface transition-all"
                                   title="Sửa" href="${ctx}/manager/genres?edit=${genre.genreId}${fn:escapeXml(fn:replace(listQuery, '?', '&'))}">
                                    <span class="material-symbols-outlined text-[18px]">edit</span>
                                </a>
                                <form method="post" action="${ctx}/manager/genres${listQuery}" class="inline"
                                      data-confirm="Xóa thể loại &quot;${fn:escapeXml(genre.genreName)}&quot;?">
                                    <input type="hidden" name="csrfToken" value="${csrfToken}"/>
                                    <input type="hidden" name="action" value="delete"/>
                                    <input type="hidden" name="genreId" value="${genre.genreId}"/>
                                    <button type="submit" title="Xóa"
                                            class="p-2 rounded-lg bg-error/10 hover:bg-error/20 text-error transition-all">
                                        <span class="material-symbols-outlined text-[18px]">delete</span>
                                    </button>
                                </form>
                            </div>
                        </td>
                    </tr>
                </c:forEach>
                <c:if test="${empty genrePage.genres}">
                    <tr><td colspan="4" class="py-12 px-6 text-center text-on-surface-variant"><c:out value="${msgNoResult}"/></td></tr>
                </c:if>
                </tbody>
            </table>
        </div>

        <div class="flex items-center justify-between p-4 bg-surface-container border-t border-white/5 text-body-sm text-on-surface-variant">
            <div>Hiển thị ${genrePage.fromItem}-${genrePage.toItem} trong tổng số ${genrePage.totalItems} thể loại</div>
            <div class="flex items-center gap-2">
                <c:url var="pageBase" value="/manager/genres">
                    <c:if test="${not empty keyword}"><c:param name="q" value="${keyword}"/></c:if>
                </c:url>
                <c:set var="sep" value="${fn:contains(pageBase, '?') ? '&' : '?'}"/>
                <c:choose>
                    <c:when test="${genrePage.page > 1}">
                        <a class="px-3 py-1.5 rounded bg-surface-container-high text-on-surface hover:bg-surface-bright" href="${pageBase}${sep}page=${genrePage.page - 1}">Trước</a>
                    </c:when>
                    <c:otherwise><span class="px-3 py-1.5 rounded bg-surface-container-high text-on-surface opacity-50">Trước</span></c:otherwise>
                </c:choose>
                <c:forEach var="p" begin="1" end="${genrePage.totalPages}">
                    <a class="px-3 py-1.5 rounded ${p == genrePage.page ? 'bg-primary text-on-primary font-bold' : 'bg-surface-container-high text-on-surface hover:bg-surface-bright'}"
                       href="${pageBase}${sep}page=${p}">${p}</a>
                </c:forEach>
                <c:choose>
                    <c:when test="${genrePage.page < genrePage.totalPages}">
                        <a class="px-3 py-1.5 rounded bg-surface-container-high text-on-surface hover:bg-surface-bright" href="${pageBase}${sep}page=${genrePage.page + 1}">Sau</a>
                    </c:when>
                    <c:otherwise><span class="px-3 py-1.5 rounded bg-surface-container-high text-on-surface opacity-50">Sau</span></c:otherwise>
                </c:choose>
            </div>
        </div>
    </div>
</div>
</main>

<c:if test="${showForm}">
    <div class="fixed inset-0 bg-black/80 backdrop-blur-md z-50 flex items-center justify-center p-4 overflow-y-auto" data-modal>
        <div class="bg-surface-container-low border border-white/10 rounded-2xl w-full max-w-md p-space-xl shadow-2xl relative my-8">
            <a class="absolute right-4 top-4 text-on-surface-variant hover:text-on-surface"
               href="${ctx}/manager/genres${listQuery}" title="Đóng">
                <span class="material-symbols-outlined">close</span>
            </a>
            <h3 class="text-headline-md text-on-surface mb-space-lg flex items-center gap-space-sm">
                <span class="material-symbols-outlined text-primary">category</span>
                ${formGenreId > 0 ? 'Chỉnh sửa thể loại' : 'Thêm thể loại mới'}
            </h3>

            <c:if test="${not empty errors.general}">
                <div class="mb-space-md form-error-banner"><c:out value="${errors.general}"/></div>
            </c:if>

            <form method="post" action="${ctx}/manager/genres${listQuery}" novalidate class="space-y-space-md">
                <input type="hidden" name="csrfToken" value="${csrfToken}"/>
                <input type="hidden" name="genreId" value="${formGenreId}"/>
                <div>
                    <label class="block text-label-md text-on-surface-variant mb-1" for="name">Tên thể loại <span class="req">*</span></label>
                    <input class="${inputClass}" id="name" name="name" type="text" maxlength="50" autofocus
                           value="<c:out value='${formName}'/>" placeholder="VD: Hành động, Tình cảm"/>
                    <c:if test="${not empty errors.name}"><p class="form-error"><c:out value="${errors.name}"/></p></c:if>
                </div>
                <div class="flex items-center justify-end gap-space-md pt-space-md">
                    <a class="px-5 py-2.5 rounded-full bg-surface-container-high text-on-surface hover:bg-surface-bright font-label-lg transition-all"
                       href="${ctx}/manager/genres${listQuery}">Hủy bỏ</a>
                    <button type="submit"
                            class="px-6 py-2.5 rounded-full bg-primary text-on-primary hover:bg-primary/90 font-label-lg shadow-[0_0_20px_-2px_rgba(229,9,20,0.45)] transition-all">
                        ${formGenreId > 0 ? 'Lưu thay đổi' : 'Lưu thể loại'}
                    </button>
                </div>
            </form>
        </div>
    </div>
</c:if>

<script src="${ctx}/assets/js/manager-common.js"></script>
</body>
</html>
