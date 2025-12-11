package org.example.service.invoice;

import org.example.entity.Invoice;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

public class DbInvoiceService implements InvoiceService {
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Override
    public int save(Invoice invoice) {
        String sql = "MERGE OP_InvoiceData AS target " +
                "USING (SELECT ? AS FInvoiceNo, ? AS FInvoiceDate, ? AS FInvoiceName, ? AS FEmpName, " +
                "? AS FServiceType, ? AS FSellerName, ? AS FPurchaserName, ? AS FAmount, ? AS FTax) AS source " +
                "ON target.FInvoiceNo = source.FInvoiceNo " +
                "WHEN MATCHED THEN " +
                "UPDATE SET FInvoiceDate = source.FInvoiceDate, " +
                "FInvoiceName = source.FInvoiceName, " +
                "FEmpName = source.FEmpName, " +
                "FServiceType = source.FServiceType, " +
                "FSellerName = source.FSellerName, " +
                "FPurchaserName = source.FPurchaserName, " +
                "FAmount = source.FAmount, " +
                "FTax = source.FTax " +
                "WHEN NOT MATCHED THEN " +
                "INSERT (FInvoiceNo, FInvoiceDate, FInvoiceName, FEmpName, FServiceType, FSellerName, FPurchaserName, FAmount, FTax) " +
                "VALUES (source.FInvoiceNo, source.FInvoiceDate, source.FInvoiceName, source.FEmpName, " +
                "source.FServiceType, source.FSellerName, source.FPurchaserName, source.FAmount, source.FTax);";

        int count = jdbcTemplate.update(sql,
                invoice.getInvoiceNumConfirm(),
                invoice.getInvoiceDate(),
                invoice.getFileName(),
                invoice.getReportName(),
                invoice.getServiceType(),
                invoice.getSellerName(),
                invoice.getPurchaserName(),
                emptyAsZero(invoice.getTotalAmount()),
                emptyAsZero(invoice.getTotalTax())
        );
        return count;
    }
    private Object emptyAsZero(String totalTax) {
        if(totalTax==null ||totalTax.trim().equals("")){
            return 0;
        }
        return totalTax;
    }
}
