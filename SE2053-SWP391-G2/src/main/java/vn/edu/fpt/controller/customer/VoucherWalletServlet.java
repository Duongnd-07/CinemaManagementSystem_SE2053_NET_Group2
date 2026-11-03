package vn.edu.fpt.controller.customer;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import vn.edu.fpt.model.User;
import vn.edu.fpt.model.Voucher;
import vn.edu.fpt.service.VoucherWalletService;
import vn.edu.fpt.util.Messages;

@WebServlet("/customer/vouchers")
public class VoucherWalletServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(VoucherWalletServlet.class.getName());
    private static final String VIEW = "/WEB-INF/views/customer/voucher-wallet.jsp";
    private final VoucherWalletService voucherWalletService = new VoucherWalletService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        try {
            List<Voucher> vouchers = voucherWalletService.findForCustomer(user.getUserId());
            request.setAttribute("vouchers", vouchers);
            request.setAttribute("voucherCategories", List.of("Active", "Used", "Expired", "Disabled"));
            request.setAttribute("walletEmptyMessage", Messages.VOUCHER_WALLET_EMPTY);
            request.setAttribute("activeNav", "vouchers");
            request.getRequestDispatcher(VIEW).forward(request, response);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi tải ví voucher khách hàng", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Messages.SYSTEM_ERROR);
        }
    }
}
