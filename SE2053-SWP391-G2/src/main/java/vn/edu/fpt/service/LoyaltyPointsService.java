package vn.edu.fpt.service;

import java.sql.SQLException;
import vn.edu.fpt.dao.LoyaltyDAO;
import vn.edu.fpt.model.LoyaltyTransaction;
import vn.edu.fpt.util.Page;

public class LoyaltyPointsService {
    private static final int PAGE_SIZE = 10;
    private final LoyaltyDAO loyaltyDAO = new LoyaltyDAO();

    public Integer findBalance(int userId) throws SQLException {
        return loyaltyDAO.findBalance(userId);
    }

    public Page<LoyaltyTransaction> findHistory(int userId, int page) throws SQLException {
        return Page.of(page, PAGE_SIZE, loyaltyDAO.countTransactions(userId),
                (offset, limit) -> loyaltyDAO.findTransactions(userId, offset, limit));
    }
}
