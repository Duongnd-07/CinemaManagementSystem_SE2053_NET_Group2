<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="editing" value="${form.roomId > 0}"/>
<c:set var="inputClass" value="form-input bg-surface-container-high text-on-surface px-4 rounded-lg border border-white/10 focus:border-primary outline-none"/>
<%-- Ghế bị giới hạn thao tác (vị trí:lý do), để room-editor.js chặn sớm; server vẫn kiểm tra lại khi lưu --%>
<c:set var="lockData" value=""/>
<c:forEach var="lock" items="${locks}">
    <c:set var="lockData" value="${lockData}${empty lockData ? '' : ','}${lock.key}:${lock.value}"/>
</c:forEach>
<!DOCTYPE html>
<html lang="vi" class="dark">
<head>
    <title>${editing ? 'Sửa phòng chiếu' : 'Thêm phòng chiếu'} - CineAdmin</title>
    <jsp:include page="/WEB-INF/views/common/manager-head.jsp"/>
    <link href="${ctx}/assets/css/room-editor.css" rel="stylesheet"/>
</head>
<body class="bg-surface font-body-md text-on-surface">
<jsp:include page="/WEB-INF/views/common/manager-sidebar.jsp"/>

<main class="pl-64 pt-16">
<div class="px-space-xl pt-space-lg pb-16">
    <div class="mb-space-lg">
        <div class="flex items-center gap-space-sm mb-space-xs">
            <span class="text-label-sm px-2 py-0.5 rounded bg-primary/20 text-primary uppercase">Quản lý rạp</span>
            <span class="text-outline text-body-sm">/</span>
            <a class="text-on-surface-variant text-body-sm hover:text-on-surface" href="${ctx}/manager/rooms">Phòng chiếu</a>
            <span class="text-outline text-body-sm">/</span>
            <span class="text-on-surface-variant text-body-sm">${editing ? 'Chỉnh sửa' : 'Thêm mới'}</span>
        </div>
        <h1 class="text-headline-lg text-on-surface">
            <c:choose>
                <c:when test="${editing}">Sơ đồ ghế: <c:out value="${room.roomName}"/></c:when>
                <c:otherwise>Thêm phòng chiếu mới</c:otherwise>
            </c:choose>
        </h1>
    </div>

    <%-- E1 và các lỗi nghiệp vụ: liệt kê tất cả, lỗi theo trường hiện thêm ngay dưới ô nhập --%>
    <c:if test="${not empty errors}">
        <div class="form-error-banner mb-space-lg" role="alert">
            <p class="font-label-lg mb-1">Chưa lưu được phòng chiếu:</p>
            <ul class="list-disc pl-5 space-y-0.5">
                <c:forEach var="error" items="${errors}"><li><c:out value="${error.value}"/></li></c:forEach>
            </ul>
        </div>
    </c:if>

    <form id="roomForm" method="post" action="${ctx}/manager/rooms" novalidate
          class="grid grid-cols-1 xl:grid-cols-[340px_1fr] gap-space-lg items-start">
        <input type="hidden" name="csrfToken" value="${csrfToken}"/>
        <input type="hidden" name="roomId" value="${form.roomId}"/>
        <input type="hidden" id="layout" name="layout" value="<c:out value='${form.layout}'/>"/>

        <!-- Thông tin phòng + công cụ -->
        <div class="space-y-space-lg">
            <section class="bg-surface-container-low border border-white/5 rounded-xl p-space-lg space-y-space-md">
                <h2 class="font-headline-sm text-on-surface flex items-center gap-2">
                    <span class="material-symbols-outlined text-primary">meeting_room</span>Thông tin phòng
                </h2>
                <div>
                    <label class="block text-label-md text-on-surface-variant mb-1" for="name">Tên / mã phòng <span class="req">*</span></label>
                    <input class="${inputClass} w-full" id="name" name="name" type="text" maxlength="${maxName}"
                           value="<c:out value='${form.name}'/>" placeholder="VD: Phòng chiếu 3, IMAX-01"/>
                    <c:if test="${not empty errors.name}"><p class="form-error"><c:out value="${errors.name}"/></p></c:if>
                </div>
                <div>
                    <label class="block text-label-md text-on-surface-variant mb-1" for="format">Định dạng chiếu <span class="req">*</span></label>
                    <select class="${inputClass} w-full" id="format" name="format">
                        <c:forEach var="format" items="${formats}">
                            <option value="${format}" ${form.format == format ? 'selected' : ''}>${format}</option>
                        </c:forEach>
                    </select>
                    <c:if test="${not empty errors.format}"><p class="form-error"><c:out value="${errors.format}"/></p></c:if>
                    <c:if test="${editing and upcomingShowtimes > 0}">
                        <p class="mt-1 text-body-sm text-secondary">Phòng đang có ${upcomingShowtimes} suất chiếu sắp tới nên không thể đổi định dạng.</p>
                    </c:if>
                </div>
                <div class="grid grid-cols-2 gap-space-md">
                    <div>
                        <label class="block text-label-md text-on-surface-variant mb-1" for="rows">Số hàng (A-Z)</label>
                        <input class="${inputClass} w-full" id="rows" name="rows" type="number" min="1" max="${maxRows}"
                               value="<c:out value='${form.rows}'/>"/>
                    </div>
                    <div>
                        <label class="block text-label-md text-on-surface-variant mb-1" for="cols">Số cột</label>
                        <input class="${inputClass} w-full" id="cols" name="cols" type="number" min="1" max="${maxCols}"
                               value="<c:out value='${form.cols}'/>"/>
                    </div>
                </div>
                <c:if test="${not empty errors.size}"><p class="form-error"><c:out value="${errors.size}"/></p></c:if>
                <button type="button" id="applySize"
                        class="w-full flex items-center justify-center gap-2 px-4 py-2 rounded-lg bg-surface-container-high hover:bg-surface-bright text-on-surface font-label-lg transition-all">
                    <span class="material-symbols-outlined text-[18px]">grid_on</span>Áp dụng kích thước lưới
                </button>
                <c:choose>
                    <c:when test="${editing}">
                        <%-- Phòng có sẵn: đổi trạng thái ở nút Sửa của danh sách phòng --%>
                        <p class="text-body-sm text-on-surface-variant">
                            Trạng thái phòng:
                            <strong class="${room.status == 'Maintenance' ? 'text-amber-400' : room.status == 'Inactive' ? 'text-outline' : 'text-emerald-400'}">${room.status == 'Maintenance' ? 'Bảo trì' : room.status == 'Inactive' ? 'Dừng hoạt động' : 'Hoạt động'}</strong>
                            <a class="text-primary underline" href="${ctx}/manager/rooms?info=${form.roomId}">Đổi</a>
                        </p>
                    </c:when>
                    <c:otherwise>
                        <div>
                            <label class="block text-label-md text-on-surface-variant mb-1" for="status">Trạng thái <span class="req">*</span></label>
                            <select class="${inputClass} w-full" id="status" name="status">
                                <option value="Active" ${form.status == 'Active' ? 'selected' : ''}>Hoạt động</option>
                                <option value="Inactive" ${form.status == 'Inactive' ? 'selected' : ''}>Dừng hoạt động</option>
                            </select>
                            <p class="mt-1 text-body-sm text-outline">Bảo trì chỉ được bật từ mục Sự cố thiết bị.</p>
                            <c:if test="${not empty errors.status}"><p class="form-error"><c:out value="${errors.status}"/></p></c:if>
                        </div>
                    </c:otherwise>
                </c:choose>
            </section>

            <section class="bg-surface-container-low border border-white/5 rounded-xl p-space-lg space-y-space-md">
                <h2 class="font-headline-sm text-on-surface flex items-center gap-2">
                    <span class="material-symbols-outlined text-primary">brush</span>Công cụ
                </h2>
                <div class="grid grid-cols-1 gap-2" role="radiogroup" aria-label="Công cụ tô ghế">
                    <label class="tool"><input type="radio" name="tool" value="S" checked/><span class="tool-swatch cell cell-S"></span>Ghế thường</label>
                    <label class="tool"><input type="radio" name="tool" value="V"/><span class="tool-swatch cell cell-V"></span>Ghế VIP</label>
                    <label class="tool"><input type="radio" name="tool" value="C"/><span class="tool-swatch tool-swatch-wide cell cell-C"></span>Ghế đôi (Sweetbox) - chiếm 2 ô, 2 người</label>
                    <label class="tool"><input type="radio" name="tool" value="X"/><span class="tool-swatch cell cell-off"></span>Khóa ghế (hỏng / không bán)</label>
                    <label class="tool"><input type="radio" name="tool" value="."/><span class="tool-swatch cell cell-aisle"></span>Lối đi (không có ghế)</label>
                </div>
                <p class="text-body-sm text-on-surface-variant">
                    Bấm hoặc kéo chuột trên lưới để tô. Bấm vào chữ cái hàng hoặc số cột để áp dụng cho cả hàng/cột.
                    Tô loại ghế lên ghế đang khóa sẽ mở khóa ghế đó. Ghế đôi chiếm ô được bấm và ô bên phải.
                </p>
            </section>

            <section class="bg-surface-container-low border border-white/5 rounded-xl p-space-lg">
                <h2 class="font-headline-sm text-on-surface mb-space-sm">Thống kê</h2>
                <dl class="grid grid-cols-2 gap-y-1 text-body-sm" id="stats">
                    <dt class="text-on-surface-variant">Tổng số ghế</dt><dd class="text-right font-label-lg" data-stat="total">0</dd>
                    <dt class="text-on-surface-variant">Sức chứa (người)</dt><dd class="text-right font-label-lg" data-stat="capacity">0</dd>
                    <dt class="text-on-surface-variant">Ghế thường</dt><dd class="text-right" data-stat="S">0</dd>
                    <dt class="text-on-surface-variant">Ghế VIP</dt><dd class="text-right" data-stat="V">0</dd>
                    <dt class="text-on-surface-variant">Ghế đôi</dt><dd class="text-right" data-stat="C">0</dd>
                    <dt class="text-on-surface-variant">Đang khóa</dt><dd class="text-right" data-stat="off">0</dd>
                    <dt class="text-on-surface-variant">Lối đi</dt><dd class="text-right" data-stat="aisle">0</dd>
                </dl>
            </section>
        </div>

        <!-- Xem trước sơ đồ (UC-09 hiển thị cho khách theo đúng lưới này) -->
        <section class="bg-surface-container-low border border-white/5 rounded-xl p-space-lg min-w-0">
            <div class="flex flex-wrap items-center justify-between gap-space-sm mb-space-md">
                <h2 class="font-headline-sm text-on-surface">Xem trước sơ đồ ghế</h2>
                <p id="editorMessage" class="text-body-sm text-secondary min-h-[20px]" aria-live="polite"></p>
            </div>
            <div class="room-screen">MÀN HÌNH</div>
            <div class="room-grid-wrap">
                <div id="roomGrid" class="room-grid" data-locks="<c:out value='${lockData}'/>"
                     data-max-rows="${maxRows}" data-max-cols="${maxCols}"></div>
            </div>
            <ul class="room-legend">
                <li><span class="cell cell-S"></span>Thường</li>
                <li><span class="cell cell-V"></span>VIP</li>
                <li><span class="cell cell-C cell-wide"></span>Ghế đôi (2 người)</li>
                <li><span class="cell cell-S cell-off"></span>Đang khóa</li>
                <li><span class="cell cell-aisle"></span>Lối đi</li>
                <c:if test="${editing}">
                    <li><span class="cell cell-S lock-sold"></span>Đã bán vé suất sắp tới (không khóa, đổi loại hay xóa được)</li>
                    <li><span class="cell cell-S cell-off lock-report"></span>Có báo cáo sự cố chưa xử lý (phải giữ khóa)</li>
                    <li><span class="cell cell-S lock-history"></span>Đã có lịch sử vé/sự cố (chỉ khóa, không xóa)</li>
                </c:if>
            </ul>

            <div class="flex items-center justify-end gap-space-md pt-space-lg mt-space-lg border-t border-white/5">
                <a class="px-5 py-2.5 rounded-full bg-surface-container-high text-on-surface hover:bg-surface-bright font-label-lg transition-all"
                   href="${ctx}/manager/rooms">Hủy bỏ</a>
                <button type="submit"
                        class="px-6 py-2.5 rounded-full bg-primary text-on-primary hover:bg-primary/90 font-label-lg shadow-[0_0_20px_-2px_rgba(229,9,20,0.45)] transition-all">
                    ${editing ? 'Lưu thay đổi' : 'Lưu phòng chiếu'}
                </button>
            </div>
        </section>
    </form>
</div>
</main>

<script src="${ctx}/assets/js/manager-common.js"></script>
<script src="${ctx}/assets/js/room-editor.js"></script>
</body>
</html>
