<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<%-- UC-19: bán vé & bắp nước tại quầy. Ba trạng thái: chưa mở ca -> mở ca; đã mở ca -> chọn suất chiếu;
     có attribute "booking" -> màn hình bán (ghế, bắp nước, thành viên, voucher, điểm, thu tiền) --%>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="paymentLabels" value="${{'Cash':'Tiền mặt','Transfer':'Chuyển khoản / QR','Card':'Thẻ (máy POS)'}}"/>
<c:set var="paymentIcons" value="${{'Cash':'payments','Transfer':'qr_code_2','Card':'credit_card'}}"/>
<c:set var="inputClass" value="form-input w-full bg-surface-container-high text-on-surface px-4 rounded-lg border border-white/10 focus:border-primary outline-none"/>
<c:set var="cardClass" value="bg-surface-container-low rounded-xl border border-white/10 p-space-lg"/>
<!DOCTYPE html>
<html lang="vi" class="dark">
<head>
    <title>Bán vé tại quầy - CineLuxe Staff</title>
    <jsp:include page="/WEB-INF/views/common/manager-head.jsp"/>
    <link href="${ctx}/assets/css/cineluxe.css" rel="stylesheet"/>
    <link href="${ctx}/assets/css/seat-map.css" rel="stylesheet"/>
</head>
<body class="bg-surface font-body-md text-on-surface min-h-screen">
<jsp:include page="/WEB-INF/views/common/staff-header.jsp"/>

<main class="max-w-6xl mx-auto px-space-lg pt-space-lg pb-16">

    <c:if test="${not empty flashSuccess or not empty flashError}">
        <div id="toast" role="status"
             class="fixed top-20 right-8 z-[60] max-w-md flex items-start gap-space-sm px-space-lg py-space-md rounded-xl shadow-2xl border
                    ${not empty flashSuccess ? 'bg-emerald-950 border-emerald-500/40 text-emerald-300' : 'bg-error-container border-error/40 text-on-error-container'}">
            <span class="material-symbols-outlined">${not empty flashSuccess ? 'check_circle' : 'error'}</span>
            <span class="font-label-lg"><c:out value="${not empty flashSuccess ? flashSuccess : flashError}"/></span>
        </div>
    </c:if>

    <div class="flex flex-col md:flex-row md:items-center justify-between gap-space-md mb-space-lg">
        <div>
            <h1 class="text-headline-md text-on-surface flex items-center gap-space-sm">
                <span class="material-symbols-outlined text-primary-container">point_of_sale</span>Bán vé tại quầy
            </h1>
            <p class="text-body-sm text-on-surface-variant mt-1">Bán vé và bắp nước cho khách mua trực tiếp, thu tiền tại quầy.</p>
        </div>
        <c:if test="${not empty shift}">
            <div class="flex items-center gap-space-md bg-surface-container-low border border-white/10 rounded-xl px-space-md py-2">
                <span class="cl-pill cl-pill--success">Ca đang mở</span>
                <span class="text-body-sm">Quầy ${fn:substring(shift.counterNo, 1, 2)} · từ
                    ${fn:substring(shift.startedAt, 11, 16)} ${fn:substring(shift.startedAt, 8, 10)}/${fn:substring(shift.startedAt, 5, 7)}</span>
                <%-- UC-22: kết ca kèm đếm két ở trang Ca làm việc --%>
                <a class="flex items-center gap-1 text-error hover:underline font-label-lg" href="${ctx}/staff/shifts">
                    <span class="material-symbols-outlined text-[18px]">logout</span>Kết ca
                </a>
            </div>
        </c:if>
    </div>

    <c:if test="${shiftEndingSoon}">
        <div class="mb-space-lg flex items-center gap-space-sm px-space-md py-space-sm rounded-xl bg-amber-950/60 border border-amber-500/40 text-amber-200">
            <span class="material-symbols-outlined">alarm</span>
            Ca của bạn sẽ tự kết thúc lúc <b>${fn:substring(shiftEndsAt, 11, 16)}</b>. Hoàn tất đơn đang bán và chuẩn bị đếm két.
        </div>
    </c:if>

    <c:choose>
        <%-- ===== Chưa mở ca ===== --%>
        <c:when test="${empty shift}">
            <form method="post" action="${ctx}/staff/counter" class="${cardClass} max-w-md mx-auto space-y-space-md">
                <input type="hidden" name="csrfToken" value="${csrfToken}"/>
                <input type="hidden" name="action" value="openShift"/>
                <h2 class="text-title-lg flex items-center gap-space-sm">
                    <span class="material-symbols-outlined text-primary-container">schedule</span>Mở ca làm việc
                </h2>
                <p class="text-body-sm text-on-surface-variant">Mọi đơn bán trong ca được tính vào báo cáo ca của bạn khi kết ca.</p>
                <div>
                    <label class="block text-label-md text-on-surface-variant mb-1" for="counterNo">Quầy làm việc</label>
                    <select class="${inputClass} h-11" id="counterNo" name="counterNo" required>
                        <c:forEach var="counter" items="${counters}">
                            <option value="${counter}">Quầy ${fn:substring(counter, 1, 2)}</option>
                        </c:forEach>
                    </select>
                </div>
                <div>
                    <label class="block text-label-md text-on-surface-variant mb-1" for="openingCash">Tiền lẻ có sẵn trong két (đ)</label>
                    <input class="${inputClass} h-11" type="text" inputmode="numeric" id="openingCash" name="openingCash"
                           required placeholder="VD: 500000 (nhập 0 nếu két trống)"/>
                    <p class="text-body-sm text-on-surface-variant mt-1">Dùng để đối chiếu két tiền khi kết ca (UC-22).</p>
                </div>
                <button type="submit"
                        class="w-full flex items-center justify-center gap-space-sm bg-primary-container text-on-primary-container py-3 rounded-full font-label-lg">
                    <span class="material-symbols-outlined">play_arrow</span>Mở ca
                </button>
            </form>
        </c:when>

        <%-- ===== Chọn suất chiếu ===== --%>
        <c:when test="${empty booking}">
            <form method="get" action="${ctx}/staff/counter" class="flex flex-wrap items-end gap-space-md mb-space-lg">
                <div>
                    <label class="block text-label-md text-on-surface-variant mb-1" for="date">Ngày chiếu</label>
                    <input class="${inputClass} h-11" type="date" id="date" name="date" value="${date}" min="${today}"
                           onchange="this.form.submit()"/>
                </div>
                <c:if test="${date ne today}">
                    <a class="text-primary hover:underline font-label-lg pb-3" href="${ctx}/staff/counter">Về hôm nay</a>
                </c:if>
            </form>
            <c:choose>
                <c:when test="${empty showtimes}">
                    <div class="${cardClass} text-center text-on-surface-variant">
                        <span class="material-symbols-outlined text-[40px] mb-2">event_busy</span>
                        <p>Không còn suất chiếu nào mở bán trong ngày này.</p>
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="grid sm:grid-cols-2 lg:grid-cols-3 gap-space-md">
                        <c:forEach var="st" items="${showtimes}">
                            <a href="${ctx}/staff/counter?showtimeId=${st.showtimeId}"
                               class="${cardClass} hover:border-primary transition-colors block">
                                <div class="flex items-start justify-between gap-space-sm">
                                    <span class="text-headline-sm text-primary">${fn:substring(st.startTime, 11, 16)}</span>
                                    <span class="cl-pill cl-pill--muted"><c:out value="${st.movie.ageRating}"/></span>
                                </div>
                                <div class="font-label-lg text-on-surface mt-1"><c:out value="${st.movie.title}"/></div>
                                <div class="text-body-sm text-on-surface-variant mt-1">
                                    <c:out value="${st.room.roomName}"/> · <c:out value="${st.room.format}"/> ·
                                    <fmt:formatNumber value="${st.basePrice}" maxFractionDigits="0"/>đ
                                </div>
                            </a>
                        </c:forEach>
                    </div>
                </c:otherwise>
            </c:choose>
        </c:when>

        <%-- ===== Màn hình bán ===== --%>
        <c:otherwise>
            <c:set var="seatMap" value="${booking.seatMap}"/>
            <c:set var="st" value="${seatMap.showtime}"/>
            <a class="inline-flex items-center gap-1 text-on-surface-variant hover:text-primary mb-space-md"
               href="${ctx}/staff/counter?date=${fn:substring(st.startTime, 0, 10)}">
                <span class="material-symbols-outlined text-[18px]">arrow_back</span>Chọn suất khác
            </a>

            <form method="post" action="${ctx}/staff/counter" id="saleForm" novalidate
                  class="grid lg:grid-cols-[1fr_380px] gap-space-lg items-start">
                <input type="hidden" name="csrfToken" value="${csrfToken}"/>
                <input type="hidden" name="showtimeId" value="${st.showtimeId}"/>

                <div class="space-y-space-lg min-w-0">
                    <%-- Thông tin suất --%>
                    <div class="${cardClass} flex flex-wrap items-center gap-x-space-lg gap-y-1">
                        <span class="text-title-lg"><c:out value="${seatMap.movie.title}"/></span>
                        <span class="cl-pill cl-pill--muted"><c:out value="${seatMap.movie.ageRating}"/></span>
                        <span class="text-body-sm text-on-surface-variant">
                            ${fn:substring(st.startTime, 11, 16)} ngày ${fn:substring(st.startTime, 8, 10)}/${fn:substring(st.startTime, 5, 7)}/${fn:substring(st.startTime, 0, 4)}
                            · <c:out value="${seatMap.room.roomName}"/>
                        </span>
                    </div>

                    <%-- Bước 2: ghế --%>
                    <section class="${cardClass}">
                        <h2 class="text-title-md mb-space-md flex items-center gap-space-sm">
                            <span class="material-symbols-outlined text-primary-container">event_seat</span>Chọn ghế
                        </h2>
                        <c:if test="${not empty errors.seats}"><p class="form-error mb-space-sm"><c:out value="${errors.seats}"/></p></c:if>
                        <div class="seat-map-page counter-seat-map">
                            <div class="seat-map-scroll">
                                <div class="seat-map-stage" style="--seat-cols: ${seatMap.columnCount}">
                                    <div class="seat-map-screen">MÀN HÌNH</div>
                                    <c:forEach var="row" items="${seatMap.rows}">
                                        <div class="seat-row">
                                            <span class="seat-row-label"><c:out value="${row.rowName}"/></span>
                                            <div class="seat-row-cells">
                                                <c:forEach var="seat" items="${row.cells}">
                                                    <c:choose>
                                                        <c:when test="${empty seat}"><span class="seat-gap"></span></c:when>
                                                        <c:otherwise>
                                                            <c:set var="available" value="${seat.status == 'AVAILABLE'}"/>
                                                            <c:set var="picked" value="${available and fn:contains(','.concat(selectedSeats).concat(','), ','.concat(seat.seatId).concat(','))}"/>
                                                            <label class="seat seat--${fn:toLowerCase(seat.seatType)} is-${fn:toLowerCase(seat.status)} ${picked ? 'is-selected' : ''} ${available ? 'cursor-pointer' : ''}"
                                                                   title="<c:out value='${seat.label}'/>">
                                                                <input type="checkbox" class="sr-only" name="seatIds" value="${seat.seatId}"
                                                                       data-price="${booking.seatPrices[seat.seatType]}"
                                                                       ${picked ? 'checked' : ''} ${available ? '' : 'disabled'}/>${seat.colNumber}
                                                            </label>
                                                        </c:otherwise>
                                                    </c:choose>
                                                </c:forEach>
                                            </div>
                                            <span class="seat-row-label"><c:out value="${row.rowName}"/></span>
                                        </div>
                                    </c:forEach>
                                </div>
                            </div>
                            <ul class="legend-list mt-space-md">
                                <li><span class="seat seat--legend is-available"></span>Trống</li>
                                <li><span class="seat seat--legend is-selected"></span>Đang chọn</li>
                                <li><span class="seat seat--legend is-pending"></span>Khách online đang giữ</li>
                                <li><span class="seat seat--legend is-sold"></span>Đã bán</li>
                                <li><span class="seat seat--legend is-maintenance"></span>Bảo trì</li>
                                <c:forEach var="price" items="${booking.seatPrices}">
                                    <li><span class="seat seat--legend seat--${fn:toLowerCase(price.key)} is-available"></span>
                                        ${price.key == 'VIP' ? 'VIP' : price.key == 'Sweetbox' ? 'Ghế đôi' : 'Thường'}
                                        <b><fmt:formatNumber value="${price.value}" maxFractionDigits="0"/>đ</b></li>
                                </c:forEach>
                            </ul>
                        </div>
                        <c:if test="${booking.ageRestricted}">
                            <label class="flex items-start gap-space-sm mt-space-md cursor-pointer">
                                <input type="checkbox" name="ageConfirmed" value="1" class="mt-1" ${form.ageConfirmed ? 'checked' : ''}/>
                                <span>Đã kiểm tra giấy tờ: khách đủ tuổi xem phim <b><c:out value="${seatMap.movie.ageRating}"/></b></span>
                            </label>
                            <c:if test="${not empty errors.age}"><p class="form-error"><c:out value="${errors.age}"/></p></c:if>
                        </c:if>
                    </section>

                    <%-- Bước 3: bắp nước --%>
                    <section class="${cardClass}">
                        <h2 class="text-title-md mb-space-md flex items-center gap-space-sm">
                            <span class="material-symbols-outlined text-primary-container">fastfood</span>Bắp nước
                            <span class="text-body-sm text-on-surface-variant font-normal">(không bắt buộc)</span>
                        </h2>
                        <c:if test="${not empty errors.concession}"><p class="form-error mb-space-sm"><c:out value="${errors.concession}"/></p></c:if>
                        <c:choose>
                            <c:when test="${empty concessions}">
                                <p class="text-body-sm text-on-surface-variant">Hiện không có món nào còn hàng.</p>
                            </c:when>
                            <c:otherwise>
                                <div class="grid sm:grid-cols-2 gap-space-sm">
                                    <c:forEach var="item" items="${concessions}">
                                        <c:set var="typedQty" value="${form.quantities[item.concessionId]}"/>
                                        <div class="flex items-center justify-between gap-space-sm bg-surface-container-high rounded-lg px-space-md py-2">
                                            <div class="min-w-0">
                                                <%-- Tên món dài xuống dòng để nhân viên đọc đủ, không cắt "..." --%>
                                                <div class="font-label-lg break-words"><c:out value="${item.name}"/></div>
                                                <div class="text-body-sm text-on-surface-variant whitespace-nowrap">
                                                    <fmt:formatNumber value="${item.unitPrice}" maxFractionDigits="0"/>đ · còn ${item.maxQuantity}
                                                </div>
                                            </div>
                                            <%-- inputClass có w-full nên bọc khung rộng cố định, không để ô số lượng chiếm chỗ tên món --%>
                                            <div class="w-20 shrink-0">
                                                <input class="${inputClass} h-10 text-center" type="number" min="0" max="${item.maxQuantity}"
                                                       name="qty_${item.concessionId}" data-price="${item.unitPrice}"
                                                       value="${not empty typedQty ? fn:escapeXml(typedQty) : (item.quantity > 0 ? item.quantity : '')}"
                                                       placeholder="0" aria-label="Số lượng <c:out value='${item.name}'/>"/>
                                            </div>
                                        </div>
                                    </c:forEach>
                                </div>
                            </c:otherwise>
                        </c:choose>
                    </section>

                    <%-- Bước 4: thành viên, voucher, điểm --%>
                    <section class="${cardClass} space-y-space-md">
                        <h2 class="text-title-md flex items-center gap-space-sm">
                            <span class="material-symbols-outlined text-primary-container">loyalty</span>Khách thành viên
                            <span class="text-body-sm text-on-surface-variant font-normal">(bỏ trống nếu là khách vãng lai)</span>
                        </h2>
                        <div class="flex gap-space-sm">
                            <input class="${inputClass} h-11" type="text" name="customer" maxlength="100"
                                   value="<c:out value='${form.customer}'/>" placeholder="Số điện thoại hoặc email của khách"/>
                            <button type="submit" name="action" value="quote"
                                    class="shrink-0 px-space-md rounded-lg bg-surface-container-highest hover:bg-surface-bright font-label-lg">Tra cứu</button>
                        </div>
                        <c:if test="${not empty errors.customer}"><p class="form-error"><c:out value="${errors.customer}"/></p></c:if>
                        <c:if test="${not empty quote.customer}">
                            <div class="flex items-center gap-space-sm text-emerald-300">
                                <span class="material-symbols-outlined">verified_user</span>
                                <span><b><c:out value="${quote.customer.fullName}"/></b> · có <fmt:formatNumber value="${quote.availablePoints}"/> điểm dùng được</span>
                            </div>

                            <div>
                                <label class="block text-label-md text-on-surface-variant mb-1" for="voucherCode">Voucher</label>
                                <select class="${inputClass} h-11" id="voucherCode" name="voucherCode">
                                    <option value="">-- Không dùng voucher trong ví --</option>
                                    <c:forEach var="v" items="${quote.vouchers}">
                                        <option value="<c:out value='${v.voucherCode}'/>" ${v.applied ? 'selected' : ''} ${v.usable or v.applied ? '' : 'disabled'}>
                                            <c:out value="${v.voucherCode}"/> · giảm
                                            <c:choose><c:when test="${v.percent}"><fmt:formatNumber value="${v.discountValue}" maxFractionDigits="0"/>%</c:when>
                                                <c:otherwise><fmt:formatNumber value="${v.discountValue}" maxFractionDigits="0"/>đ</c:otherwise></c:choose>
                                            ${v.belowMinOrder ? ' (chưa đủ đơn tối thiểu)' : v.inOtherOrder ? ' (đang dùng ở đơn khác)' : ''}
                                        </option>
                                    </c:forEach>
                                </select>
                                <%-- Mã không nằm trong ví (voucher khuyến mãi chung, hoặc mã gõ sai) hiện lại ở ô gõ tay --%>
                                <c:set var="inWallet" value="false"/>
                                <c:forEach var="v" items="${quote.vouchers}"><c:if test="${v.applied}"><c:set var="inWallet" value="true"/></c:if></c:forEach>
                                <input class="${inputClass} h-11 mt-space-sm uppercase" type="text" name="voucherCodeTyped" maxlength="50"
                                       value="${inWallet ? '' : fn:escapeXml(form.voucherCode)}"
                                       placeholder="Hoặc nhập mã voucher khuyến mãi"/>
                                <c:if test="${not empty errors.voucher}"><p class="form-error"><c:out value="${errors.voucher}"/></p></c:if>
                            </div>

                            <div>
                                <label class="block text-label-md text-on-surface-variant mb-1" for="points">
                                    Dùng điểm (1 điểm = <fmt:formatNumber value="${quote.pointRate}"/>đ, tối đa <fmt:formatNumber value="${quote.maxPoints}"/> điểm cho đơn này)
                                </label>
                                <input class="${inputClass} h-11" type="number" min="0" max="${quote.maxPoints}" id="points" name="points"
                                       value="${quote.pointsRedeemed > 0 ? quote.pointsRedeemed : fn:escapeXml(form.points)}" placeholder="0"/>
                                <c:if test="${not empty errors.points}"><p class="form-error"><c:out value="${errors.points}"/></p></c:if>
                            </div>
                        </c:if>
                        <c:if test="${empty quote.customer}">
                            <c:if test="${not empty errors.voucher}"><p class="form-error"><c:out value="${errors.voucher}"/></p></c:if>
                            <c:if test="${not empty errors.points}"><p class="form-error"><c:out value="${errors.points}"/></p></c:if>
                        </c:if>
                    </section>
                </div>

                <%-- Bước 5-7: tổng tiền, thu tiền --%>
                <aside class="${cardClass} space-y-space-md lg:sticky lg:top-20">
                    <h2 class="text-title-md flex items-center gap-space-sm">
                        <span class="material-symbols-outlined text-primary-container">receipt_long</span>Thanh toán
                    </h2>
                    <c:choose>
                        <c:when test="${not empty quote}">
                            <dl class="space-y-1 text-body-md">
                                <div class="flex justify-between"><dt>Vé (${fn:length(quote.seats)} ghế<c:forEach var="s" items="${quote.seats}" varStatus="i">${i.first ? ': ' : ', '}<c:out value="${s.label}"/></c:forEach>)</dt>
                                    <dd><fmt:formatNumber value="${quote.ticketTotal}" maxFractionDigits="0"/>đ</dd></div>
                                <div class="flex justify-between"><dt>Bắp nước</dt>
                                    <dd><fmt:formatNumber value="${quote.concessionTotal}" maxFractionDigits="0"/>đ</dd></div>
                                <c:if test="${quote.voucherDiscount > 0}">
                                    <div class="flex justify-between text-emerald-300"><dt>Voucher <c:out value="${quote.voucherCode}"/></dt>
                                        <dd>-<fmt:formatNumber value="${quote.voucherDiscount}" maxFractionDigits="0"/>đ</dd></div>
                                </c:if>
                                <c:if test="${quote.pointsRedeemed > 0}">
                                    <div class="flex justify-between text-emerald-300"><dt>Điểm (${quote.pointsRedeemed})</dt>
                                        <dd>-<fmt:formatNumber value="${quote.pointsDiscount}" maxFractionDigits="0"/>đ</dd></div>
                                </c:if>
                                <div class="flex justify-between text-title-lg pt-2 border-t border-white/10"><dt>Cần thu</dt>
                                    <dd class="text-primary"><fmt:formatNumber value="${quote.finalAmount}" maxFractionDigits="0"/>đ</dd></div>
                            </dl>
                        </c:when>
                        <c:otherwise>
                            <p class="text-body-sm text-on-surface-variant">Tạm tính (chưa gồm voucher, điểm):
                                <b class="text-on-surface" id="liveTotal">0đ</b></p>
                            <p class="text-body-sm text-on-surface-variant">Chọn ghế, bắp nước rồi bấm <b>Tính tiền</b>.</p>
                        </c:otherwise>
                    </c:choose>

                    <fieldset class="space-y-space-sm">
                        <legend class="text-label-md text-on-surface-variant mb-1">Hình thức thu tiền</legend>
                        <c:forEach var="method" items="${paymentMethods}">
                            <label class="flex items-center gap-space-sm px-space-md py-2 rounded-lg bg-surface-container-high cursor-pointer has-[:checked]:ring-2 has-[:checked]:ring-primary">
                                <input type="radio" name="paymentMethod" value="${method}" ${form.paymentMethod == method ? 'checked' : ''}/>
                                <span class="material-symbols-outlined text-[20px]">${paymentIcons[method]}</span>${paymentLabels[method]}
                            </label>
                        </c:forEach>
                    </fieldset>
                    <div id="cashField" ${form.paymentMethod == 'Cash' ? '' : 'hidden'}>
                        <label class="block text-label-md text-on-surface-variant mb-1" for="cashReceived">Tiền khách đưa (đ)</label>
                        <input class="${inputClass} h-11" type="text" inputmode="numeric" id="cashReceived" name="cashReceived"
                               value="<c:out value='${form.cashReceived}'/>" placeholder="VD: 200000"/>
                        <c:if test="${not empty quote.changeDue}">
                            <p class="mt-1 text-emerald-300">Tiền thối: <b><fmt:formatNumber value="${quote.changeDue}" maxFractionDigits="0"/>đ</b></p>
                        </c:if>
                    </div>
                    <%-- Thẻ / chuyển khoản: chỉ xác nhận khi đã thấy tiền về, nhập mã giao dịch làm bằng chứng (đối chiếu ở UC-22) --%>
                    <input type="hidden" name="transferNote" value="${form.transferNote}"/>
                    <div id="referenceField" class="space-y-space-sm" ${form.paymentMethod == 'Card' or form.paymentMethod == 'Transfer' ? '' : 'hidden'}>
                        <div data-method="Transfer" class="rounded-lg border border-white/10 bg-surface-container-high p-space-sm text-body-sm"
                             ${form.paymentMethod == 'Transfer' ? '' : 'hidden'}>
                            Nội dung chuyển khoản khách cần ghi: <b class="text-primary tracking-wider">${form.transferNote}</b>
                            <span class="block text-on-surface-variant">Đợi app ngân hàng báo đã nhận đúng số tiền với nội dung này rồi nhập mã giao dịch.</span>
                        </div>
                        <label class="block text-label-md text-on-surface-variant" for="paymentReference">
                            <span data-method="Card" ${form.paymentMethod == 'Card' ? '' : 'hidden'}>Mã chuẩn chi trên hóa đơn POS (6 số)</span>
                            <span data-method="Transfer" ${form.paymentMethod == 'Transfer' ? '' : 'hidden'}>Mã giao dịch ngân hàng</span>
                        </label>
                        <input class="${inputClass} h-11 uppercase" type="text" id="paymentReference" name="paymentReference" maxlength="30"
                               value="<c:out value='${form.paymentReference}'/>" placeholder="VD: 123456 / FT26297XXXXXX" autocomplete="off"/>
                    </div>
                    <c:if test="${not empty errors.payment}"><p class="form-error"><c:out value="${errors.payment}"/></p></c:if>

                    <button type="submit" name="action" value="quote"
                            class="w-full py-3 rounded-full bg-surface-container-highest hover:bg-surface-bright font-label-lg">Tính tiền</button>
                    <button type="submit" name="action" value="confirm" id="confirmButton"
                            ${not empty quote and quote.valid ? '' : 'disabled'}
                            class="w-full flex items-center justify-center gap-space-sm py-3 rounded-full bg-primary-container text-on-primary-container font-label-lg disabled:opacity-40 disabled:cursor-not-allowed">
                        <span class="material-symbols-outlined">task_alt</span>Xác nhận đã thu tiền
                    </button>
                    <p class="text-body-sm text-on-surface-variant" id="confirmHint">
                        ${not empty quote and quote.valid ? 'Thu đủ tiền của khách rồi mới bấm xác nhận.' : 'Bấm Tính tiền để kiểm tra đơn trước khi thu tiền.'}
                    </p>
                    <a class="block text-center text-on-surface-variant hover:text-error font-label-lg"
                       href="${ctx}/staff/counter?showtimeId=${st.showtimeId}">Hủy giao dịch</a>
                </aside>
            </form>
        </c:otherwise>
    </c:choose>
</main>

<script src="${ctx}/assets/js/counter-sale.js?v=2"></script>
</body>
</html>
