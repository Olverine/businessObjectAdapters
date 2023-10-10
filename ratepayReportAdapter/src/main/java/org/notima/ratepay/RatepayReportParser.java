package org.notima.ratepay;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.jline.utils.InputStreamReader;

public class RatepayReportParser {

	private String 			filename;
	private RatepayReport	report;
	// There must only be one report row per descriptor. Fees are described by using multiple rows with the same descriptor.
	private Map<String, RatepayReportRow> descriptorMap;
	
    private static final String K_SHOP_ID = "SHOP_ID";
    private static final String K_PAYMENTDATE = "PAYMENTDATE";
    private static final String K_SHOPNAME = "SHOPNAME";
    private static final String K_AMOUNT = "AMOUNT";
    private static final String K_DESCRIPTOR = "DESCRIPTOR";
    private static final String K_SHOPINVOICE_ID = "SHOPINVOICE_ID";
    private static final String K_SHOPSORDER_ID = "SHOPSORDER_ID";
    private static final String K_INVOICENUMBER = "INVOICENUMBER";
    private static final String K_DESCRIPTION = "DESCRIPTION";
    private static final String K_FEETYPE = "FEETYPE";
    private static final String K_ORDERDATE = "ORDERDATE";
    private static final String K_SENTDATE = "SENTDATE";
    private static final String K_TRANSACTION_ID = "TRANSACTION_ID";
    private static final String K_CUSTOMERGROUP = "CUSTOMERGROUP";
    private static final String K_KNOWNCUSTOMER = "KNOWNCUSTOMER";
    private static final String K_PRODUCT = "PRODUCT";
    private static final String K_REFERENCE_ID_ACCOUNTING = "REFERENCE_ID_ACCOUNTING";

    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    private List<RatepayReportRow> reportLines;
    
    public static RatepayReport createFromFile(String filename) throws IOException, Exception {
    	RatepayReportParser parser = new RatepayReportParser(filename);
    	return parser.parseRatepayFile();
    }
    
    /**
     * Private constructor to hide the inner workings of this class.
     * 
     * @param filename
     */
    private RatepayReportParser(String filename) {
    	this.filename = filename;
    	report = new RatepayReport();
    	descriptorMap = new TreeMap<String,RatepayReportRow>();
    }
    
    private RatepayReport parseRatepayFile() throws IOException, Exception {
    	InputStream in = new FileInputStream(new File(filename));
    	report.setReportRows(parseFile(in));
    	return report;
    }
    
    private List<RatepayReportRow> parseFile (InputStream inStream) throws IOException, Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inStream));
        reportLines = new ArrayList<RatepayReportRow>();
        Iterable<CSVRecord> records = CSVFormat.INFORMIX_UNLOAD
            .withFirstRecordAsHeader()
            .withDelimiter(';')
            .withQuote('"')
            .parse(reader);
    
        List<RatepayReportRow> rowsToProcess = new ArrayList<RatepayReportRow>();
        
        for (CSVRecord record : records) {
            RatepayReportRow row = parseRecord(record);
            rowsToProcess.add(row);
        }
        
        reader.close();
        inStream.close();
        
        // First add the actual payment records
        for (RatepayReportRow row : rowsToProcess) {
        	addPaymentRecord(row);
        }
        
        // Add fees to payment records
        for (RatepayReportRow row : rowsToProcess) {
            addFee(row);
        }
        
        return reportLines;
    }

    private void addFee(RatepayReportRow row) throws Exception {
    	RatepayReportRow existingRow = null;
    	if (row.getFeeType()!=RatepayReportRow.FEETYPE_PAYMENT) {
    		existingRow = descriptorMap.get(row.getDescriptor());
    		if (existingRow==null) {
    			reportLines.add(row);
    			descriptorMap.put(row.getDescriptor(), row);
    		} else {
    			existingRow.mergeRow(row);
    		}
    	}
    }
    
    private void addPaymentRecord(RatepayReportRow row) throws Exception {
    	RatepayReportRow existingRow = null;
    	if (row.getFeeType()==RatepayReportRow.FEETYPE_PAYMENT) {
    		existingRow = descriptorMap.get(row.getDescriptor());
    		if (existingRow==null) {
    			reportLines.add(row);
    			descriptorMap.put(row.getDescriptor(), row);
    		} else {
    			existingRow.mergeRow(row);
    		}
    	}
    }
    
    private RatepayReportRow parseRecord(CSVRecord record) throws ParseException {
        RatepayReportRow row = new RatepayReportRow();

        if(record.isMapped(K_SHOP_ID))
            row.setShopId(record.get(K_SHOP_ID));
        if(record.isMapped(K_PAYMENTDATE))
            row.setPaymentDate(dateFormat.parse(record.get(K_PAYMENTDATE)));
        if(record.isMapped(K_SHOPNAME))
            row.setShopName(record.get(K_SHOPNAME));
        if(record.isMapped(K_SHOPNAME))
            row.setAmount(Double.parseDouble(record.get(K_AMOUNT)));
        if(record.isMapped(K_DESCRIPTOR))
            row.setDescriptor(record.get(K_DESCRIPTOR));
        if(record.isMapped(K_SHOPINVOICE_ID))
            row.setShopInvoiceId(record.get(K_SHOPINVOICE_ID));
        if(record.isMapped(K_SHOPSORDER_ID))
            row.setShopsOrderId(record.get(K_SHOPSORDER_ID));
        if(record.isMapped(K_INVOICENUMBER))
            row.setInvoiceNumber(record.get(K_INVOICENUMBER));
        if(record.isMapped(K_DESCRIPTION))
            row.setDescription(record.get(K_DESCRIPTION));
        if(record.isMapped(K_FEETYPE))
            row.setFeeType(Integer.parseInt(record.get(K_FEETYPE)));
        if(record.isMapped(K_ORDERDATE))
            row.setOrderDate(dateFormat.parse(record.get(K_ORDERDATE)));
        if(record.isMapped(K_SENTDATE))
            row.setSentDate(dateFormat.parse(record.get(K_SENTDATE)));
        if(record.isMapped(K_TRANSACTION_ID))
            row.setTransactionId(record.get(K_TRANSACTION_ID));
        if(record.isMapped(K_CUSTOMERGROUP))
            row.setCustomerGroup(Integer.parseInt(record.get(K_CUSTOMERGROUP)));
        if(record.isMapped(K_KNOWNCUSTOMER))
            row.setKnownCustomer(Integer.parseInt(record.get(K_KNOWNCUSTOMER)) == 1);
        if(record.isMapped(K_PRODUCT))
            row.setProduct(Integer.parseInt(record.get(K_PRODUCT)));
        if(record.isMapped(K_REFERENCE_ID_ACCOUNTING) && !record.get(K_REFERENCE_ID_ACCOUNTING).isEmpty()) 
           row.setReferenceIdAccounting(Integer.parseInt(record.get(K_REFERENCE_ID_ACCOUNTING)));

        return row;
    }
}
