<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="vi" class="dark">
<head>
    <title>Lịch sử bán vé online - CineAdmin</title>
    <jsp:include page="/WEB-INF/views/common/manager-head.jsp"/>
    <link href="${ctx}/assets/css/online-reports.css" rel="stylesheet"/>
</head>
<body class="online-report-page bg-surface font-body-md text-on-surface">
<a class="or-skip" href="#report-main">Đến lịch sử bán vé</a>
<jsp:include page="/WEB-INF/views/common/manager-sidebar.jsp"/>
<main class="or-main" id="report-main">
    <div class="or-header">
        <div><h1 class="text-headline-md">Lịch sử bán vé online</h1><p class="or-muted">Các đơn online đã thanh toán hoàn tất</p></div>
        <c:if test="${not empty result}"><a class="or-button" href="<c:out value='${baseUrl}&export=csv'/>">Xuất CSV</a></c:if>
        <c:if test="${not empty detail}"><a class="or-button" href="<c:out value='${baseUrl}&page=${f.page}'/>">Về lịch sử bán vé</a></c:if>
    </div>
    <c:if test="${not empty errors}">
        <div class="or-notice or-error" role="alert"><p>Không thể tải lịch sử bán vé:</p><ul><c:forEach var="error" items="${errors}"><li><c:out value="${error.value}"/></li></c:forEach></ul></div>
    </c:if>
    <c:choose>
        <c:when test="${not empty detail}">
            <c:set var="order" value="${detail.order}"/>
            <section class="or-card">
                <h2 class="text-headline-sm">Đơn online #${order.bookingId}</h2>
                <p><c:out value="${order.title}" default="0"/> · <c:out value="${order.roomName}" default="0"/> · Suất <c:out value="${fn:replace(order.showtimeAt, 'T', ' ')}" default="0"/></p>
                <p>Tạo: <c:out value="${fn:replace(order.createdAt, 'T', ' ')}" default="0"/> · Thanh toán: <c:out value="${fn:replace(order.paidAt, 'T', ' ')}" default="0"/></p>
                <p>Trạng thái: Đã thanh toán · Khách: <c:out value="${empty order.customerName ? '0' : order.customerName}"/></p>
                <p>Vé trước giảm: <fmt:formatNumber value="${empty order.ticketValue ? 0 : order.ticketValue}" maxFractionDigits="0"/>đ · Bắp nước: <fmt:formatNumber value="${empty order.concessionValue ? 0 : order.concessionValue}" maxFractionDigits="0"/>đ · Giảm giá: <fmt:formatNumber value="${empty order.discount ? 0 : order.discount}" maxFractionDigits="0"/>đ</p>
                <p>Số tiền thực thu: <b><fmt:formatNumber value="${empty order.amount ? 0 : order.amount}" maxFractionDigits="0"/>đ</b> · <c:out value="${order.amount == 0 ? 'Voucher/điểm bù toàn bộ' : (empty order.method ? '0' : order.method)}"/></p>
            </section>
            <div class="or-table-wrap">
                <table class="or-table"><caption>Vé trong đơn</caption>
                    <thead><tr><th scope="col">Mã vé</th><th scope="col">Ghế</th><th scope="col">Trạng thái</th><th scope="col" class="or-money">Giá vé</th></tr></thead>
                    <tbody>
                        <c:forEach var="row" items="${detail.tickets}"><tr><td><c:out value="${empty row.reference ? '0' : row.reference}"/></td><td><c:out value="${empty row.description ? '0' : row.description}"/></td><td><c:out value="${empty row.status ? '0' : row.status}"/></td><td class="or-money"><fmt:formatNumber value="${empty row.amount ? 0 : row.amount}" maxFractionDigits="0"/>đ</td></tr></c:forEach>
                        <c:if test="${empty detail.tickets}"><tr><td colspan="4" class="or-empty">Không có vé trong đơn.</td></tr></c:if>
                    </tbody>
                </table>
            </div>
            <c:if test="${not empty detail.concessions}">
                <div class="or-table-wrap"><table class="or-table"><caption>Bắp nước kèm đơn</caption>
                    <thead><tr><th scope="col">Sản phẩm</th><th scope="col" class="or-number">Số lượng</th><th scope="col" class="or-money">Thành tiền</th></tr></thead>
                    <tbody><c:forEach var="row" items="${detail.concessions}"><tr><td><c:out value="${empty row.description ? '0' : row.description}"/></td><td class="or-number">${row.ticketCount}</td><td class="or-money"><fmt:formatNumber value="${empty row.amount ? 0 : row.amount}" maxFractionDigits="0"/>đ</td></tr></c:forEach></tbody>
                </table></div>
            </c:if>
        </c:when>
        <c:otherwise>
            <nav class="or-actions or-presets" aria-label="Chọn nhanh khoảng ngày">
                <c:set var="presetLabels" value="${['Hôm nay','7 ngày gần nhất','Tháng này','Quý này','Năm này']}"/>
                <c:set var="presetStarts" value="${[today,weekFrom,monthFrom,quarterFrom,yearFrom]}"/>
                <c:forEach var="label" items="${presetLabels}" varStatus="preset">
                    <c:set var="start" value="${presetStarts[preset.index]}"/>
                    <c:set var="groupBy" value="${preset.index >= 3 ? 'month' : 'day'}"/>
                    <c:url var="presetUrl" value="/manager/online-reports"><c:param name="from" value="${start}"/><c:param name="to" value="${today}"/><c:param name="groupBy" value="${groupBy}"/></c:url>
                    <a class="or-button" href="<c:out value='${presetUrl}'/>" ${f.from == start and f.to == today and f.groupBy == groupBy ? 'aria-current="true"' : ''}><c:out value="${label}"/></a>
                </c:forEach>
            </nav>
            <form method="get" action="${ctx}/manager/online-reports" class="or-filters bg-surface-container-low">
                <input type="hidden" name="groupBy" value="${empty f.groupBy ? 'day' : f.groupBy}"/>
                <div><label for="from">Từ ngày</label><input id="from" name="from" type="date" value="<c:out value='${not empty raw.from ? raw.from : f.from}'/>" aria-invalid="${not empty errors.from}" aria-describedby="from-error"/><small class="or-error" id="from-error"><c:out value="${errors.from}"/></small></div>
                <div><label for="to">Đến ngày</label><input id="to" name="to" type="date" value="<c:out value='${not empty raw.to ? raw.to : f.to}'/>" aria-invalid="${not empty errors.to}" aria-describedby="to-error"/><small class="or-error" id="to-error"><c:out value="${errors.to}"/></small></div>
                <button class="or-button bg-primary-container text-on-primary-container" type="submit">Xem lịch sử</button>
            </form>
            <c:if test="${not empty result}">
                <c:set var="s" value="${result.summary}"/>
                <p class="or-muted text-body-sm">Kỳ ${f.from} → ${f.to} · Theo ngày thanh toán · Cập nhật <c:out value="${fn:replace(result.generatedAt, 'T', ' ')}"/></p>
                <dl class="or-kpis">
                    <div class="or-card"><dt>Đơn thành công</dt><dd>${s.orderCount}</dd></div>
                    <div class="or-card"><dt>Số vé đã bán</dt><dd>${s.ticketCount}</dd></div>
                    <div class="or-card"><dt>Số tiền trước giảm giá</dt><dd><fmt:formatNumber value="${empty s.grossAmount ? 0 : s.grossAmount}" maxFractionDigits="0"/>đ</dd></div>
                    <div class="or-card"><dt>Số tiền thực thu</dt><dd><fmt:formatNumber value="${empty s.collected ? 0 : s.collected}" maxFractionDigits="0"/>đ</dd></div>
                </dl>
                <c:set var="monthly" value="${f.groupBy == 'month'}"/>
                <c:set var="chartTitle" value="${monthly ? 'Tiền thu theo tháng' : 'Tiền thu theo ngày'}"/>
                <section class="or-card mt-4" aria-label="${chartTitle}">
                    <h2 class="text-headline-sm"><c:out value="${chartTitle}"/></h2>
                    <div class="or-daily" aria-hidden="true"><c:forEach var="period" items="${result.collections}"><c:set var="periodLabel"><c:choose><c:when test="${monthly}"><c:out value="${fn:substring(period.key, 5, 7)}"/>/<c:out value="${fn:substring(period.key, 0, 4)}"/></c:when><c:otherwise><c:out value="${fn:substring(period.key, 5, 10)}"/></c:otherwise></c:choose></c:set><div class="or-bar" style="--bar: ${result.maxCollection.signum() > 0 ? period.value * 100 / result.maxCollection : 0}%" title="${periodLabel}: ${period.value}đ"><span><c:out value="${periodLabel}"/></span></div></c:forEach></div>
                    <details><summary class="cursor-pointer">Xem bảng số liệu đầy đủ</summary>
                        <div class="or-table-wrap"><table class="or-table"><thead><tr><th scope="col">${monthly ? 'Tháng thanh toán' : 'Ngày thanh toán'}</th><th scope="col" class="or-money">Thu từ đơn online</th></tr></thead><tbody><c:forEach var="period" items="${result.collections}"><tr><td><c:choose><c:when test="${monthly}"><c:out value="${fn:substring(period.key, 5, 7)}"/>/<c:out value="${fn:substring(period.key, 0, 4)}"/></c:when><c:otherwise><c:out value="${period.key}"/></c:otherwise></c:choose></td><td class="or-money"><fmt:formatNumber value="${period.value}" maxFractionDigits="0"/>đ</td></tr></c:forEach></tbody></table></div>
                    </details>
                </section>
                <section class="or-history" aria-labelledby="orders-title">
                    <h2 id="orders-title" class="text-headline-sm">Đơn đã thanh toán</h2>
                    <div class="or-table-wrap" tabindex="0" role="region" aria-label="Đơn online, cuộn ngang để xem đầy đủ">
                        <table class="or-table or-table-wide">
                            <caption>Đơn đã thanh toán trong kỳ</caption>
                            <colgroup><col class="or-col-order"/><col class="or-col-show"/><col class="or-col-count"/><col class="or-col-money"/><col class="or-col-money"/><col class="or-col-money"/><col class="or-col-money"/></colgroup>
                            <thead><tr><th scope="col">Đơn / thanh toán</th><th scope="col">Phim / suất</th><th scope="col" class="or-number">Số vé</th><th scope="col" class="or-money">Vé trước giảm</th><th scope="col" class="or-money">Bắp nước</th><th scope="col" class="or-money">Giảm giá</th><th scope="col" class="or-money">Thực thu</th></tr></thead>
                            <tbody>
                                <c:forEach var="row" items="${result.rows}">
                                    <tr>
                                        <td><a href="<c:out value='${baseUrl}&page=${result.page}&id=${row.bookingId}'/>">#${row.bookingId}</a><small><c:out value="${empty row.paidAt ? '0' : fn:replace(row.paidAt, 'T', ' ')}"/></small><small><c:out value="${row.amount == 0 ? 'Voucher/điểm bù toàn bộ' : (empty row.method ? '0' : row.method)}"/></small></td>
                                        <td><c:out value="${empty row.title ? '0' : row.title}"/><small><c:out value="${empty row.roomName ? '0' : row.roomName}"/> · <c:out value="${empty row.showtimeAt ? '0' : fn:replace(row.showtimeAt, 'T', ' ')}"/></small></td>
                                        <td class="or-number">${row.ticketCount}</td>
                                        <td class="or-money"><fmt:formatNumber value="${empty row.ticketValue ? 0 : row.ticketValue}" maxFractionDigits="0"/>đ</td>
                                        <td class="or-money"><fmt:formatNumber value="${empty row.concessionValue ? 0 : row.concessionValue}" maxFractionDigits="0"/>đ</td>
                                        <td class="or-money"><fmt:formatNumber value="${empty row.discount ? 0 : row.discount}" maxFractionDigits="0"/>đ</td>
                                        <td class="or-money"><fmt:formatNumber value="${empty row.amount ? 0 : row.amount}" maxFractionDigits="0"/>đ</td>
                                    </tr>
                                </c:forEach>
                                <c:if test="${empty result.rows}"><tr><td colspan="7" class="or-empty">Không có đơn online đã thanh toán trong kỳ.</td></tr></c:if>
                            </tbody>
                        </table>
                    </div>
                    <nav class="or-page-nav" aria-label="Phân trang lịch sử"><span>${result.totalRows} đơn · Trang ${result.page}/${result.pageCount} · Tối đa 10 đơn/trang</span><div class="or-actions"><c:if test="${result.page > 1}"><a class="or-button" href="<c:out value='${baseUrl}&page=${result.page - 1}'/>">Trang trước</a></c:if><c:if test="${result.page < result.pageCount}"><a class="or-button" href="<c:out value='${baseUrl}&page=${result.page + 1}'/>">Trang sau</a></c:if></div></nav>
                </section>
            </c:if>
        </c:otherwise>
    </c:choose>
</main>
</body>
</html>
