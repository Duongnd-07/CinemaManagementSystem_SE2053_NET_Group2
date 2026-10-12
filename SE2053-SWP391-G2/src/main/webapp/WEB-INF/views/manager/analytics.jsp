<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="periodLabels" value="${{'day':'Ngày','week':'Tuần','month':'Tháng','year':'Năm'}}"/>
<c:set var="tierLabels" value="${{'Top':'Bán chạy','Average':'Trung bình','Low':'Bán chậm'}}"/>
<c:set var="tierPills" value="${{'Top':'cl-pill--success','Average':'cl-pill--muted','Low':'cl-pill--warning'}}"/>
<c:set var="hasData" value="${summary.orderCount > 0 or not empty rooms}"/>
<c:set var="manyBars" value="${fn:length(revenueSeries) > 12}"/>
<!DOCTYPE html>
<html lang="vi" class="dark">
<head>
    <title>Thống kê &amp; Báo cáo - CineAdmin</title>
    <jsp:include page="/WEB-INF/views/common/manager-head.jsp"/>
    <link href="${ctx}/assets/css/cineluxe.css" rel="stylesheet"/>
</head>
<body class="bg-surface font-body-md text-on-surface">
<jsp:include page="/WEB-INF/views/common/manager-sidebar.jsp"/>

<main class="pl-64 pt-16">
<div class="px-space-xl pt-space-lg pb-16">

    <div class="flex flex-col md:flex-row md:items-center justify-between gap-space-md mb-space-lg">
        <div>
            <div class="flex items-center gap-space-sm mb-space-xs">
                <span class="text-label-sm px-2 py-0.5 rounded bg-primary/20 text-primary uppercase">Quản lý rạp</span>
                <span class="text-outline text-body-sm">/</span>
                <span class="text-on-surface-variant text-body-sm">Báo cáo</span>
            </div>
            <h1 class="text-headline-lg text-on-surface">Thống kê &amp; Báo cáo</h1>
            <p class="text-body-sm text-on-surface-variant mt-space-xs">Kỳ báo cáo: <c:out value="${periodLabel}"/></p>
        </div>
    </div>

    <%-- Bộ lọc kỳ báo cáo: một hàng phía trên mọi biểu đồ --%>
    <div class="flex flex-wrap items-center justify-between gap-space-md bg-surface-container-low p-space-md rounded-xl mb-space-lg">
        <div class="flex flex-wrap items-center gap-space-sm">
            <c:set var="tabOn" value="bg-primary-container text-on-primary-container shadow-[0_0_15px_-2px_rgba(229,9,20,0.4)]"/>
            <c:set var="tabOff" value="bg-surface-container-high text-on-surface-variant hover:text-on-surface"/>
            <c:forEach var="p" items="${periods}">
                <c:url var="tabUrl" value="/manager/reports">
                    <c:param name="period" value="${p}"/>
                    <c:param name="date" value="${anchor}"/>
                </c:url>
                <a class="px-4 py-2 rounded-lg font-label-lg text-sm transition-all ${period == p ? tabOn : tabOff}"
                   href="${fn:escapeXml(tabUrl)}">${periodLabels[p]}</a>
            </c:forEach>
        </div>
        <form method="get" action="${ctx}/manager/reports" class="flex items-center gap-space-sm">
            <input type="hidden" name="period" value="${period}"/>
            <label class="text-body-sm text-on-surface-variant" for="date">Ngày thuộc kỳ</label>
            <input class="bg-surface-container-high text-on-surface px-3 py-2 rounded-lg border border-white/10 focus:border-primary outline-none"
                   type="date" id="date" name="date" value="${anchor}" min="2000-01-01" max="2100-12-31"/>
            <button type="submit" class="px-4 py-2 rounded-lg bg-surface-container-high hover:bg-surface-bright text-on-surface font-label-lg transition-all">Xem</button>
        </form>
    </div>

    <%-- E1: kỳ không có giao dịch vẫn hiện biểu đồ trống kèm thông báo --%>
    <c:if test="${not hasData}">
        <div class="flex items-center gap-space-sm rounded-xl border border-amber-500/40 bg-amber-500/10 text-amber-300 px-space-lg py-space-md mb-space-lg" role="status">
            <span class="material-symbols-outlined">info</span>
            <span class="font-label-lg">Không có giao dịch hoặc suất chiếu nào trong kỳ đã chọn. Mọi chỉ số đang bằng 0.</span>
        </div>
    </c:if>

    <%-- Chỉ số chính --%>
    <div class="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-space-md mb-space-lg">
        <div class="cl-card p-space-lg">
            <div class="text-body-sm text-on-surface-variant mb-space-xs">Doanh thu</div>
            <div class="text-headline-md text-on-surface"><fmt:formatNumber value="${summary.revenue}" maxFractionDigits="0"/>đ</div>
            <div class="text-body-sm text-on-surface-variant mt-space-xs">Trong đó bắp nước: <fmt:formatNumber value="${summary.concessionRevenue}" maxFractionDigits="0"/>đ</div>
        </div>
        <div class="cl-card p-space-lg">
            <div class="text-body-sm text-on-surface-variant mb-space-xs">Đơn đã thanh toán</div>
            <div class="text-headline-md text-on-surface"><fmt:formatNumber value="${summary.orderCount}"/></div>
            <div class="text-body-sm text-on-surface-variant mt-space-xs">Không tính đơn hủy, đơn chờ thanh toán</div>
        </div>
        <div class="cl-card p-space-lg">
            <div class="text-body-sm text-on-surface-variant mb-space-xs">Vé đã bán</div>
            <div class="text-headline-md text-on-surface"><fmt:formatNumber value="${summary.ticketCount}"/></div>
            <div class="text-body-sm text-on-surface-variant mt-space-xs">Vé còn hiệu lực của các đơn trong kỳ</div>
        </div>
        <div class="cl-card p-space-lg">
            <div class="text-body-sm text-on-surface-variant mb-space-xs">Tỷ lệ lấp đầy</div>
            <div class="text-headline-md text-on-surface"><fmt:formatNumber value="${overallOccupancy}" maxFractionDigits="1"/>%</div>
            <div class="text-body-sm text-on-surface-variant mt-space-xs">Các suất chiếu diễn ra trong kỳ</div>
        </div>
    </div>

    <div class="grid grid-cols-1 xl:grid-cols-3 gap-space-md mb-space-lg">
        <%-- Doanh thu theo thời gian --%>
        <section class="cl-card p-space-lg xl:col-span-2">
            <h2 class="text-headline-sm text-on-surface">Doanh thu theo ${period == 'year' ? 'tháng' : 'ngày'}</h2>
            <p class="text-body-sm text-on-surface-variant mb-space-md">Tính theo thời điểm thanh toán, đã trừ tiền hoàn. Cao nhất:
                <fmt:formatNumber value="${revenueMax}" maxFractionDigits="0"/>đ</p>
            <div class="flex items-end gap-[2px] h-48 border-b border-white/10" role="img"
                 aria-label="Biểu đồ cột doanh thu theo thời gian, số liệu chi tiết ở bảng bên dưới">
                <c:forEach var="point" items="${revenueSeries}">
                    <div class="flex-1 h-full flex items-end justify-center"
                         title="${point.key}: <fmt:formatNumber value='${point.value}' maxFractionDigits='0'/>đ">
                        <div class="w-full max-w-[40px] bg-primary hover:bg-secondary rounded-t transition-colors"
                             style="height: ${revenueMax > 0 ? point.value * 100 / revenueMax : 0}%; min-height: ${point.value > 0 ? 2 : 0}px"></div>
                    </div>
                </c:forEach>
            </div>
            <div class="flex gap-[2px] mt-space-xs">
                <c:forEach var="point" items="${revenueSeries}" varStatus="st">
                    <div class="flex-1 text-center text-label-sm text-on-surface-variant whitespace-nowrap overflow-visible">
                        <c:if test="${not manyBars or st.index % 5 == 0}">${manyBars ? fn:substring(point.key, 0, 2) : point.key}</c:if>
                    </div>
                </c:forEach>
            </div>
            <details class="mt-space-md">
                <summary class="cursor-pointer text-body-sm text-on-surface-variant hover:text-on-surface">Xem bảng số liệu</summary>
                <table class="w-full mt-space-sm text-body-sm">
                    <thead>
                    <tr class="text-on-surface-variant text-left border-b border-white/10">
                        <th class="py-2 font-normal">${period == 'year' ? 'Tháng' : 'Ngày'}</th>
                        <th class="py-2 font-normal text-right">Doanh thu</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="point" items="${revenueSeries}">
                        <tr class="border-b border-white/5">
                            <td class="py-1.5">${point.key}</td>
                            <td class="py-1.5 text-right"><fmt:formatNumber value="${point.value}" maxFractionDigits="0"/>đ</td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </details>
        </section>

        <%-- Tỷ lệ lấp đầy theo phòng --%>
        <section class="cl-card p-space-lg">
            <h2 class="text-headline-sm text-on-surface">Lấp đầy theo phòng</h2>
            <p class="text-body-sm text-on-surface-variant mb-space-md">Vé bán / tổng số ghế của các suất trong kỳ.</p>
            <c:forEach var="room" items="${rooms}">
                <div class="mb-space-md" title="${fn:escapeXml(room.name)}: ${room.ticketCount}/${room.capacity} ghế, ${room.showtimeCount} suất">
                    <div class="flex items-baseline justify-between gap-space-sm mb-1">
                        <span class="text-on-surface font-label-lg truncate"><c:out value="${room.name}"/></span>
                        <span class="text-on-surface whitespace-nowrap"><fmt:formatNumber value="${room.occupancyPercent}" maxFractionDigits="1"/>%</span>
                    </div>
                    <div class="h-2 rounded-full bg-surface-container-high overflow-hidden">
                        <div class="h-full rounded-full bg-primary" style="width: ${room.occupancyPercent > 100 ? 100 : room.occupancyPercent}%"></div>
                    </div>
                    <div class="text-body-sm text-on-surface-variant mt-1">${room.ticketCount}/${room.capacity} ghế • ${room.showtimeCount} suất</div>
                </div>
            </c:forEach>
            <c:if test="${empty rooms}">
                <div class="py-8 text-center text-on-surface-variant">Chưa có suất chiếu nào trong kỳ.</div>
            </c:if>
        </section>
    </div>

    <%-- Hiệu quả theo phim --%>
    <section class="cl-card p-space-lg">
        <h2 class="text-headline-sm text-on-surface">Hiệu quả theo phim</h2>
        <p class="text-body-sm text-on-surface-variant mb-space-md">
            Bán chạy: lấp đầy từ ${topThreshold}% trở lên. Bán chậm: lấp đầy dưới ${lowThreshold}%. Sắp theo số vé bán.</p>
        <table class="w-full text-body-md">
            <thead>
            <tr class="text-on-surface-variant text-body-sm text-left border-b border-white/10">
                <th class="py-2 font-normal">Phim</th>
                <th class="py-2 font-normal text-right">Suất chiếu</th>
                <th class="py-2 font-normal text-right">Vé bán</th>
                <th class="py-2 font-normal text-right">Doanh thu vé</th>
                <th class="py-2 pl-space-lg font-normal w-64">Tỷ lệ lấp đầy</th>
                <th class="py-2 pl-space-md font-normal">Xếp loại</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach var="movie" items="${movies}">
                <tr class="border-b border-white/5">
                    <td class="py-3 text-on-surface font-label-lg"><c:out value="${movie.name}"/></td>
                    <td class="py-3 text-right">${movie.showtimeCount}</td>
                    <td class="py-3 text-right"><fmt:formatNumber value="${movie.ticketCount}"/></td>
                    <td class="py-3 text-right"><fmt:formatNumber value="${movie.ticketRevenue}" maxFractionDigits="0"/>đ</td>
                    <td class="py-3 pl-space-lg">
                        <div class="flex items-center gap-space-sm" title="${movie.ticketCount}/${movie.capacity} ghế">
                            <div class="flex-1 h-2 rounded-full bg-surface-container-high overflow-hidden">
                                <div class="h-full rounded-full bg-primary" style="width: ${movie.occupancyPercent > 100 ? 100 : movie.occupancyPercent}%"></div>
                            </div>
                            <span class="w-14 text-right whitespace-nowrap"><fmt:formatNumber value="${movie.occupancyPercent}" maxFractionDigits="1"/>%</span>
                        </div>
                    </td>
                    <td class="py-3 pl-space-md"><span class="cl-pill ${tierPills[movie.tier]}">${tierLabels[movie.tier]}</span></td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
        <c:if test="${empty movies}">
            <div class="py-8 text-center text-on-surface-variant">Chưa có phim nào được chiếu trong kỳ.</div>
        </c:if>
    </section>

</div>
</main>
</body>
</html>
