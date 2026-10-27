<%-- UC-17: khung đánh giá & nhận xét phim, dùng chung ở trang chi tiết phim và trang đánh giá phim đã kết thúc --%>
<%@ tag body-content="empty" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ attribute name="movieId" required="true" type="java.lang.Integer" %>
<%@ attribute name="baseUrl" required="true" description="URL trang hiện tại (chưa có tham số page), dùng cho phân trang và gửi form" %>
<%@ attribute name="summary" required="true" type="vn.edu.fpt.model.ReviewSummary" %>
<%@ attribute name="myReview" required="false" type="vn.edu.fpt.model.Review" %>
<%@ attribute name="reviewPage" required="true" type="vn.edu.fpt.util.Page" %>
<%@ attribute name="canReview" required="true" type="java.lang.Boolean" %>
<%@ attribute name="isCustomer" required="true" type="java.lang.Boolean" %>
<%@ attribute name="csrfToken" required="false" %>
<%@ attribute name="star" required="false" type="java.lang.Integer" description="Số sao đang lọc (null = tất cả)" %>
<%@ attribute name="sort" required="true" description="newest | highest | lowest (đã chuẩn hóa ở Service)" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="sep" value="${fn:contains(baseUrl, '?') ? '&' : '?'}"/>
<%-- filterBase giữ cách sắp xếp khi đổi số sao; listUrl giữ cả bộ lọc khi chuyển trang --%>
<c:set var="starParam" value=""/>
<c:if test="${not empty star}"><c:set var="starParam" value="&star=${star}"/></c:if>
<c:set var="filterBase" value="${baseUrl}${sep}sort=${sort}"/>
<c:set var="listUrl" value="${filterBase}${starParam}"/>
<c:set var="sortLabels" value="${{'newest':'Mới nhất','highest':'Điểm cao nhất','lowest':'Điểm thấp nhất'}}"/>

<section id="reviews" class="space-y-5">
    <h2 class="text-xl font-extrabold text-white flex items-center gap-2">
        <span class="w-1.5 h-6 rounded-full bg-[#ffb95f]"></span>Đánh giá &amp; nhận xét
    </h2>

    <%-- Điểm trung bình + phân bố số sao --%>
    <div class="flex flex-col sm:flex-row gap-6 p-5 rounded-2xl bg-[#171b26] border border-white/10">
        <div class="flex flex-col items-center justify-center sm:w-40 shrink-0">
            <span class="text-4xl font-extrabold text-white"><fmt:formatNumber value="${summary.averageRating}" pattern="0.0"/></span>
            <%-- Font icon chỉ nạp FILL 0 nên sao đạt/không đạt được phân biệt bằng màu; điểm làm tròn tới sao gần nhất --%>
            <div class="flex items-center gap-0.5 mt-1">
                <c:forEach begin="1" end="5" var="i">
                    <span class="material-symbols-outlined text-lg ${i <= summary.averageRating + 0.5 ? 'text-[#ffb95f]' : 'text-[#4a4e5c]'}">star</span>
                </c:forEach>
            </div>
            <span class="text-xs text-[#8e919f] mt-1">${summary.totalReviews} lượt đánh giá</span>
        </div>
        <div class="flex-1 space-y-1.5">
            <%-- starCounts đã được xếp theo thứ tự 5 sao -> 1 sao từ ReviewSummary; bấm vào một dòng để lọc theo mức sao đó --%>
            <c:forEach var="entry" items="${summary.starCounts}">
                <a href="${filterBase}&star=${entry.key}#reviews"
                   class="flex items-center gap-2 text-xs rounded-md px-1 transition hover:bg-white/5 ${star == entry.key ? 'text-white' : 'text-[#8e919f]'}">
                    <span class="w-8 text-right">${entry.key} ★</span>
                    <div class="flex-1 h-2 rounded-full bg-white/10 overflow-hidden">
                        <div class="h-full bg-[#ffb95f]" style="width: ${summary.percentOf(entry.key)}%"></div>
                    </div>
                    <span class="w-10">${entry.value}</span>
                </a>
            </c:forEach>
        </div>
    </div>

    <%-- Đánh giá của chính khách hàng --%>
    <c:choose>
        <c:when test="${not empty myReview}">
            <div class="p-5 rounded-2xl bg-[#171b26] border border-white/10 space-y-3">
                <div class="flex items-center justify-between">
                    <span class="text-sm font-bold text-white">Đánh giá của bạn</span>
                    <c:if test="${myReview.status == 'Hidden'}">
                        <span class="px-2 py-0.5 rounded-full text-xs font-bold bg-red-500/15 text-red-300 border border-red-500/30">Đã bị ẩn</span>
                    </c:if>
                </div>
                <c:if test="${myReview.status == 'Hidden'}">
                    <p class="text-sm text-[#ffb4ab]">Đánh giá của bạn đang bị ẩn do vi phạm. Lý do: <c:out value="${myReview.hiddenReason}"/></p>
                </c:if>
                <c:if test="${myReview.status != 'Hidden'}">
                    <form method="post" action="${ctx}/customer/reviews" novalidate class="space-y-3" data-review-form>
                        <input type="hidden" name="csrfToken" value="${csrfToken}"/>
                        <input type="hidden" name="movieId" value="${movieId}"/>
                        <div class="flex items-center gap-1" data-star-picker>
                            <c:forEach begin="1" end="5" var="i">
                                <label class="cursor-pointer">
                                    <input type="radio" name="rating" value="${i}" class="sr-only" ${myReview.rating == i ? 'checked' : ''}/>
                                    <span class="material-symbols-outlined text-2xl ${i <= myReview.rating ? 'text-[#ffb95f]' : 'text-[#4a4e5c]'}" data-star="${i}">star</span>
                                </label>
                            </c:forEach>
                        </div>
                        <textarea name="comment" rows="3" maxlength="1000" placeholder="Chia sẻ cảm nhận của bạn (không bắt buộc)..."
                                  class="w-full rounded-xl bg-[#0f131d] border border-white/10 p-3 text-sm text-[#dfe2f1] focus:border-[#e50914] outline-none"><c:out value="${myReview.comment}"/></textarea>
                        <div class="flex items-center justify-end gap-3">
                            <button type="submit" class="h-10 px-5 rounded-xl bg-[#e50914] hover:bg-[#b80710] text-white text-sm font-bold transition">Cập nhật đánh giá</button>
                        </div>
                    </form>
                    <form method="post" action="${ctx}/customer/reviews" data-confirm="Xóa đánh giá này?">
                        <input type="hidden" name="csrfToken" value="${csrfToken}"/>
                        <input type="hidden" name="movieId" value="${movieId}"/>
                        <input type="hidden" name="reviewId" value="${myReview.reviewId}"/>
                        <input type="hidden" name="action" value="delete"/>
                        <button type="submit" class="text-xs font-semibold text-[#ffb4ab] hover:text-white">Xóa đánh giá</button>
                    </form>
                </c:if>
            </div>
        </c:when>
        <c:when test="${canReview}">
            <form method="post" action="${ctx}/customer/reviews" novalidate class="p-5 rounded-2xl bg-[#171b26] border border-white/10 space-y-3" data-review-form>
                <input type="hidden" name="csrfToken" value="${csrfToken}"/>
                <input type="hidden" name="movieId" value="${movieId}"/>
                <span class="text-sm font-bold text-white">Viết đánh giá của bạn</span>
                <div class="flex items-center gap-1" data-star-picker>
                    <c:forEach begin="1" end="5" var="i">
                        <label class="cursor-pointer">
                            <input type="radio" name="rating" value="${i}" class="sr-only" required/>
                            <span class="material-symbols-outlined text-2xl text-[#4a4e5c]" data-star="${i}">star</span>
                        </label>
                    </c:forEach>
                </div>
                <textarea name="comment" rows="3" maxlength="1000" placeholder="Chia sẻ cảm nhận của bạn (không bắt buộc)..."
                          class="w-full rounded-xl bg-[#0f131d] border border-white/10 p-3 text-sm text-[#dfe2f1] focus:border-[#e50914] outline-none"></textarea>
                <div class="flex items-center justify-end">
                    <button type="submit" class="h-10 px-5 rounded-xl bg-[#e50914] hover:bg-[#b80710] text-white text-sm font-bold transition">Gửi đánh giá</button>
                </div>
            </form>
        </c:when>
        <c:when test="${isCustomer}">
            <p class="text-sm text-[#8e919f] flex items-center gap-1 p-4 rounded-xl bg-[#171b26] border border-white/10">
                <span class="material-symbols-outlined text-base">info</span>Bạn cần có vé đã thanh toán và suất chiếu đã kết thúc mới có thể đánh giá phim này.
            </p>
        </c:when>
        <c:otherwise>
            <c:url var="loginUrl" value="/login">
                <c:param name="returnUrl" value="${baseUrl}"/>
            </c:url>
            <p class="text-sm text-[#8e919f] flex items-center gap-2 p-4 rounded-xl bg-[#171b26] border border-white/10">
                <span class="material-symbols-outlined text-base">info</span>
                <a href="${fn:escapeXml(loginUrl)}" class="text-[#ffb4ab] hover:text-white font-semibold">Đăng nhập</a>
                để viết đánh giá nếu bạn đã xem phim này.
            </p>
        </c:otherwise>
    </c:choose>

    <%-- Bộ lọc số sao + sắp xếp cho danh sách đánh giá của khách khác --%>
    <c:set var="chipOn" value="bg-[#e50914] border-[#e50914] text-white"/>
    <c:set var="chipOff" value="bg-white/5 border-white/10 text-[#dfe2f1] hover:bg-white/10"/>
    <div class="flex flex-wrap items-center justify-between gap-3">
        <div class="flex flex-wrap items-center gap-2">
            <a href="${filterBase}#reviews" class="px-3 py-1 rounded-full text-xs font-semibold border transition ${empty star ? chipOn : chipOff}">Tất cả</a>
            <c:forEach var="entry" items="${summary.starCounts}">
                <a href="${filterBase}&star=${entry.key}#reviews"
                   class="px-3 py-1 rounded-full text-xs font-semibold border transition ${star == entry.key ? chipOn : chipOff}">${entry.key} ★ (${entry.value})</a>
            </c:forEach>
        </div>
        <div class="flex items-center gap-3 text-xs">
            <span class="text-[#8e919f]">Sắp xếp:</span>
            <%-- Lặp theo chuỗi cố định để giữ thứ tự (map literal của EL không giữ thứ tự) --%>
            <c:forEach var="option" items="${fn:split('newest,highest,lowest', ',')}">
                <a href="${baseUrl}${sep}sort=${option}${starParam}#reviews"
                   class="font-semibold ${sort == option ? 'text-[#ffb95f]' : 'text-[#8e919f] hover:text-white'}">${sortLabels[option]}</a>
            </c:forEach>
        </div>
    </div>

    <%-- Danh sách đánh giá của khách khác --%>
    <div class="space-y-4">
        <c:if test="${empty reviewPage.items}">
            <p class="text-sm text-[#8e919f]">
                <c:choose>
                    <c:when test="${empty star}">Chưa có đánh giá nào khác cho phim này.</c:when>
                    <c:otherwise>Chưa có đánh giá ${star} sao nào.</c:otherwise>
                </c:choose>
            </p>
        </c:if>
        <c:forEach var="item" items="${reviewPage.items}">
            <div class="p-4 rounded-xl bg-[#171b26] border border-white/10 space-y-1.5">
                <div class="flex items-center justify-between">
                    <span class="text-sm font-bold text-white"><c:out value="${item.reviewerName}"/></span>
                    <div class="flex items-center">
                        <c:forEach begin="1" end="5" var="i">
                            <span class="material-symbols-outlined text-base ${i <= item.rating ? 'text-[#ffb95f]' : 'text-[#4a4e5c]'}">star</span>
                        </c:forEach>
                    </div>
                </div>
                <c:if test="${not empty item.comment}">
                    <p class="text-sm text-[#dfe2f1] whitespace-pre-line"><c:out value="${item.comment}"/></p>
                </c:if>
                <c:set var="sentAt" value="${item.createdAt}"/>
                <span class="text-xs text-[#8e919f]">${fn:substring(sentAt, 8, 10)}/${fn:substring(sentAt, 5, 7)}/${fn:substring(sentAt, 0, 4)} ${fn:substring(sentAt, 11, 16)}<c:if test="${not empty item.updatedAt}"> · đã chỉnh sửa</c:if></span>
            </div>
        </c:forEach>
    </div>
    <c:if test="${reviewPage.totalPages > 1}">
        <div class="flex justify-end gap-5 text-sm">
            <c:if test="${reviewPage.page > 1}"><a class="text-[#ffb4ab]" href="${listUrl}&page=${reviewPage.page - 1}#reviews">← Trang trước</a></c:if>
            <span class="text-gray-400">Trang ${reviewPage.page} / ${reviewPage.totalPages}</span>
            <c:if test="${reviewPage.page < reviewPage.totalPages}"><a class="text-[#ffb4ab]" href="${listUrl}&page=${reviewPage.page + 1}#reviews">Trang sau →</a></c:if>
        </div>
    </c:if>
</section>
