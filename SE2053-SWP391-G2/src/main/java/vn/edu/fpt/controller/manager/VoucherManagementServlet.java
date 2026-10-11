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
            render(request, response, null, new LinkedHashMap<>());
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
        VoucherForm form = readForm(request);
        try {
            HttpSession session = request.getSession(false);
            User manager = session == null ? null : (User) session.getAttribute("user");
            Integer managerId = manager == null ? null : manager.getUserId();
            voucherService.createPromotion(form, managerId);
            request.getSession().setAttribute(FLASH_SUCCESS, Messages.VOUCHER_SAVED);
            response.sendRedirect(request.getContextPath() + "/manager/vouchers" + listQuery(request));
        } catch (ValidationException e) {
            try {
                render(request, response, form, e.getErrors());
            } catch (SQLException sqlException) {
                handleSystemError(response, sqlException);
            }
        } catch (SQLException e) {
            handleSystemError(response, e);
        }
    }

    private void render(HttpServletRequest request, HttpServletResponse response, VoucherForm form,
                        Map<String, String> errors) throws SQLException, ServletException, IOException {
        String keyword = trim(request.getParameter("q"));
        String requestedType = trim(request.getParameter("type"));
        String type = VoucherType.ALL.contains(requestedType) ? requestedType : "";
        Page<Voucher> voucherPage = voucherService.list(keyword, type, parseInt(request.getParameter("page")));
        Map<String, Integer> refundInventory = voucherService.refundInventory();

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
        request.setAttribute("statuses", List.of(VoucherService.ACTIVE, VoucherService.USED,
                VoucherService.EXPIRED, VoucherService.DISABLED));
        request.setAttribute("refundActive", refundInventory.getOrDefault(VoucherService.ACTIVE, 0));
        request.setAttribute("refundUsed", refundInventory.getOrDefault(VoucherService.USED, 0));
        request.setAttribute("refundExpired", refundInventory.getOrDefault(VoucherService.EXPIRED, 0));
        request.setAttribute("listQuery", listQuery(request));
        request.setAttribute("csrfToken", CsrfToken.get(session));
        request.setAttribute("activeMenu", "vouchers");
        request.setAttribute("msgNoResult", Messages.MSG01);
        request.setAttribute("showForm", form != null || "1".equals(request.getParameter("add")));
        request.setAttribute("form", form == null ? new VoucherForm() : form);
        request.setAttribute("errors", errors);
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    private VoucherForm readForm(HttpServletRequest request) {
        VoucherForm form = new VoucherForm();
        form.setVoucherCode(trim(request.getParameter("voucherCode")));
        form.setDiscountType(trim(request.getParameter("discountType")));
        form.setDiscountValue(trim(request.getParameter("discountValue")));
        form.setMinOrderAmount(trim(request.getParameter("minOrderAmount")));
        form.setExpiryDate(trim(request.getParameter("expiryDate")));
        return form;
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
            return 1;
        }
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
