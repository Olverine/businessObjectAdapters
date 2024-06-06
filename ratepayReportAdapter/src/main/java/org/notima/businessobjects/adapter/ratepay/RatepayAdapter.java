package org.notima.businessobjects.adapter.ratepay;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.notima.generic.businessobjects.PaymentBatch;
import org.notima.generic.ifacebusinessobjects.PaymentFactory;
import org.notima.ratepay.RatepayReport;
import org.notima.ratepay.RatepayReportParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RatepayAdapter implements PaymentFactory {

	public static String SYSTEMNAME = "Ratepay";
	
	public static Logger log = LoggerFactory.getLogger(RatepayAdapter.class);	
	
	@Override
	public List<PaymentBatch> readPaymentBatchesFromSource(String source) throws Exception {

		List<PaymentBatch> batchList = new ArrayList<PaymentBatch>();
		
		// Check to see if source is a file
		File sourceFile = new File(source);
		File sourceDir = null;
		if (sourceFile.exists() && sourceFile.getParentFile().isDirectory()) {
			sourceDir = sourceFile.getParentFile();
			String filenameonly = sourceFile.getName();
			RatepayDirectoryToPaymentBatch directoryReader = new RatepayDirectoryToPaymentBatch(sourceDir.getCanonicalPath());
			if (!sourceFile.isDirectory()) {
				PaymentBatch result = directoryReader.createPaymentBatchFromFile(filenameonly);
				batchList.add(result);
			} else {
				batchList = directoryReader.readFilesInDirectory();
			}
			return batchList;
			
		} else {
			// TODO: Probably obsolete code
	        RatepayReport report = RatepayReportParser.createFromFile(source);
	        RatepayToPaymentBatch converter = RatepayToPaymentBatch.buildFromReport(report);
	        PaymentBatch pb = converter.getPaymentBatch(); 
	        batchList.add(pb);
			return batchList;
		}
	}

	@Override
	public String getSystemName() {
		return SYSTEMNAME;
	}
	
	
}
