<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- UC-13: các bước của luồng đặt vé; trang gọi truyền param step = 1 (chọn ghế), 2 (bắp nước & ưu đãi), 3 (thanh toán) --%>
<c:set var="step" value="${param.step}"/>
<ol class="booking-steps" aria-label="Các bước đặt vé">
    <li class="booking-step ${step > 1 ? 'is-done' : (step == 1 ? 'is-current' : '')}"><b>1</b>Chọn ghế</li>
    <li class="booking-step ${step > 2 ? 'is-done' : (step == 2 ? 'is-current' : '')}"><b>2</b>Bắp nước &amp; ưu đãi</li>
    <li class="booking-step ${step == 3 ? 'is-current' : ''}"><b>3</b>Thanh toán</li>
</ol>
