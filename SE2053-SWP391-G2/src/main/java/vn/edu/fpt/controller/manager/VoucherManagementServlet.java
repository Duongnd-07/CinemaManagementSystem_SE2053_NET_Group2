package vn.edu.fpt.controller.manager;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import vn.edu.fpt.model.User;
import vn.edu.fpt.model.Voucher;
import vn.edu.fpt.model.VoucherType;
import vn.edu.fpt.service.ValidationException;
import vn.edu.fpt.service.VoucherForm;
import vn.edu.fpt.service.VoucherService;
import vn.edu.fpt.util.CsrfToken;
import vn.edu.fpt.util.Messages;
import vn.edu.fpt.util.Page;

@WebServlet("/manager/vouchers")
public class VoucherManagementServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(VoucherManagementServlet.class.getName());
    private static final String VIEW = "/WEB-INF/views/manager/vouchers.jsp";
    private static final String FLASH_SUCCESS = "flashSuccess";
    private static final String FLASH_ERROR = "flashError";

    private final VoucherService voucherService = new VoucherService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            VoucherForm form = null;
            boolean showForm = false;
            String editId = trim(request.getParameter("edit"));
            if (!editId.isEmpty()) {
                Voucher existing = voucherService.findById(parseInt(editId));
                if (existing == null) {
                    request.getSession().setAttribute(FLASH_ERROR, Messages.VOUCHER_NOT_FOUND);
                } else if (!VoucherType.PROMOTION.equals(existing.getVoucherType())) {
                    request.getSession().setAttribute(FLASH_ERROR, Messages.VOUCHER_SYSTEM_MANAGED);
                } else {
                    form = toForm(existing);
                    request.setAttribute("editingVoucherId", existing.getVoucherId());
                    showForm = true;
                }
            } else if ("1".equals(request.getParameter("add"))) {
                form = new VoucherForm();
                request.setAttribute("editingVoucherId", null);
                showForm = true;
            }
            render(request, response, form, showForm, new LinkedHashMap<>());
        } catch (SQLException e) {
            handleSystemError(response, e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!CsrfToken.isValid(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        try {
            String action = trim(request.getParameter("action"));
            if ("delete".equals(action)) {
                handleDelete(request, response);
                return;
            }
            handleSave(request, response);
        } catch (SQLException e) {
            handleSystemError(response, e);
        }
    }

    private void handleSave(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {
        VoucherForm form = readForm(request);
        Integer voucherId = parseNullableInt(request.getParameter("voucherId"));
        try {
            HttpSession session = request.getSession(false);
            User manager = session == null ? null : (User) session.getAttribute("user");
            Integer managerId = manager == null ? null : manager.getUserId();
            voucherService.save(voucherId, form, managerId);
            request.getSession().setAttribute(FLASH_SUCCESS,
                    voucherId == null || voucherId <= 0 ? Messages.VOUCHER_SAVED : Messages.VOUCHER_UPDATED);
            response.sendRedirect(request.getContextPath() + "/manager/vouchers" + listQuery(request));
        } catch (ValidationException e) {
            render(request, response, form, true, e.getErrors());
        }
    }

    private void handleDelete(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, IOException {
        int voucherId = parseInt(request.getParameter("voucherId"));
        try {
            voucherService.delete(voucherId);
            request.getSession().setAttribute(FLASH_SUCCESS, Messages.VOUCHER_DELETED);
        } catch (ValidationException e) {
            request.getSession().setAttribute(FLASH_ERROR, e.getErrors().values().iterator().next());
        }
        response.sendRedirect(request.getContextPath() + "/manager/vouchers" + listQuery(request));
    }

    private void render(HttpServletRequest request, HttpServletResponse response, VoucherForm form,
                        boolean showForm, Map<String, String> errors)
            throws SQLException, ServletException, IOException {
        String keyword = trim(request.getParameter("q"));
        String requestedType = trim(request.getParameter("type"));
        String type = VoucherType.ALL.contains(requestedType) ? requestedType : "";
        Page<Voucher> voucherPage = voucherService.list(keyword, type, parseInt(request.getParameter("page")));
        Map<String, Integer> overview = voucherService.overview();

        HttpSession session = request.getSession();
        for (String key : List.of(FLASH_SUCCESS, FLASH_ERROR)) {
            Object value = session.getAttribute(key);
            if (value != null) {
                request.setAttribute(key, value);
                session.removeAttribute(key);
            }
        }
        request.setAttribute("voucherPage", voucherPage);
        request.setAttribute("keyword", keyword);
        request.setAttribute("typeFilter", type);
        request.setAttribute("voucherTypes", VoucherType.ALL);
        request.setAttribute("promotionActive", overview.getOrDefault("PromotionActive", 0));
        request.setAttribute("promotionExpired", overview.getOrDefault("PromotionExpired", 0));
        request.setAttribute("promotionScheduled", overview.getOrDefault("PromotionScheduled", 0));
        request.setAttribute("refund80Active", overview.getOrDefault("Refund80Active", 0));
        request.setAttribute("refund80Expired", overview.getOrDefault("Refund80Expired", 0));
        request.setAttribute("listQuery", listQuery(request));
        request.setAttribute("csrfToken", CsrfToken.get(session));
        request.setAttribute("activeMenu", "vouchers");
        request.setAttribute("msgNoResult", Messages.MSG01);
        request.setAttribute("showForm", showForm);
        request.setAttribute("form", form == null ? new VoucherForm() : form);
        Object editingVoucherId = request.getAttribute("editingVoucherId");
        if (editingVoucherId == null) {
            editingVoucherId = request.getParameter("voucherId");
        }
        request.setAttribute("editingVoucherId", editingVoucherId);
        Voucher editingVoucher = editingVoucherId == null || trim(editingVoucherId.toString()).isEmpty()
                ? null : voucherService.findById(parseInt(editingVoucherId.toString()));
        String createdAt = editingVoucher == null || editingVoucher.getCreatedAt() == null
                ? "Tự động khi lưu"
                : editingVoucher.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        request.setAttribute("voucherCreatedAt", createdAt);
        request.setAttribute("errors", errors);
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    private VoucherForm toForm(Voucher voucher) {
        VoucherForm form = new VoucherForm();
        form.setVoucherCode(voucher.getVoucherCode());
        form.setDiscountType(voucher.getDiscountType());
        form.setDiscountValue(voucher.getDiscountValue() == null ? "" : voucher.getDiscountValue().toPlainString());
        form.setMinOrderAmount(voucher.getMinOrderAmount() == null ? "" : voucher.getMinOrderAmount().toPlainString());
        form.setStartDate(toDateTimeInput(voucher.getStartDate()));
        form.setExpiryDate(toDateTimeInput(voucher.getExpiryDate()));
        form.setUsageLimit(voucher.getUsageLimit() == null ? "" : voucher.getUsageLimit().toString());
        return form;
    }

    private VoucherForm readForm(HttpServletRequest request) {
        VoucherForm form = new VoucherForm();
        form.setVoucherCode(trim(request.getParameter("voucherCode")));
        form.setDiscountType(trim(request.getParameter("discountType")));
        form.setDiscountValue(trim(request.getParameter("discountValue")));
        form.setMinOrderAmount(trim(request.getParameter("minOrderAmount")));
        form.setStartDate(trim(request.getParameter("startDate")));
        form.setExpiryDate(trim(request.getParameter("expiryDate")));
        form.setUsageLimit(trim(request.getParameter("usageLimit")));
        return form;
    }

    private String toDateTimeInput(java.time.LocalDateTime value) {
        return value == null ? "" : value.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
    }

    private String listQuery(HttpServletRequest request) {
        List<String> parts = new ArrayList<>();
        for (String name : List.of("q", "type", "page")) {
            String value = trim(request.getParameter(name));
            if (!value.isEmpty()) {
                parts.add(name + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8));
            }
        }
        return parts.isEmpty() ? "" : "?" + String.join("&", parts);
    }

    private void handleSystemError(HttpServletResponse response, SQLException e) throws IOException {
        LOGGER.log(Level.SEVERE, "Lỗi truy cập dữ liệu voucher", e);
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Messages.SYSTEM_ERROR);
    }

    private int parseInt(String value) {
        try {
            return Integer.parseInt(value == null ? "" : value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private Integer parseNullableInt(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
