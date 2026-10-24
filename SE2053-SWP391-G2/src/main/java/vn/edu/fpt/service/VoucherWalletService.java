package vn.edu.fpt.service;

import java.sql.SQLException;
import java.util.List;
import vn.edu.fpt.dao.VoucherDAO;
import vn.edu.fpt.model.Voucher;

public class VoucherWalletService {
    private final VoucherDAO voucherDAO = new VoucherDAO();

    public List<Voucher> findForCustomer(int userId) throws SQLException {
        return voucherDAO.findWalletForOwner(userId);
    }
}
