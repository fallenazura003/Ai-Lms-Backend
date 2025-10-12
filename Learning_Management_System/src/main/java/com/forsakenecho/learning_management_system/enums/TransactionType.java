package com.forsakenecho.learning_management_system.enums;

public enum TransactionType {
    /** Giao dịch nạp tiền vào tài khoản người dùng */
    TOP_UP,
    /** Giao dịch mua khóa học (thường là chi tiêu) */
    PURCHASE,
    /** Giao dịch thu nhập từ việc bán khóa học (sau chiết khấu cho Teacher) */
    EARNINGS,
    /** Giao dịch chiết khấu hệ thống (thu nhập của Admin) */
    COMMISSION
}
