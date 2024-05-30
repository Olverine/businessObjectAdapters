package org.notima.businessobjects.adapter.adyen;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.jline.utils.Log;
import org.notima.generic.businessobjects.BankAccountDetail;
import org.notima.generic.businessobjects.Payment.PaymentType;
import org.notima.generic.businessobjects.PaymentBatch;
import org.notima.generic.businessobjects.TaxSubjectIdentifier;
import org.notima.generic.ifacebusinessobjects.PaymentBatchFactory;
import org.notima.adyen.AdyenReport;
import org.notima.adyen.AdyenReportParser;

/**
 * Class that takes a directory as argument, scans it for files and converts to payment batches.
 * The class looks for a property file to determine who the files belong to.
 * 
 * @author Daniel Tamm
 *
 */
public class AdyenDirectoryToPaymentBatch implements PaymentBatchFactory {

	public static String			ADYEN_PROPERTY_FILE = "adyen.properties";
	
	private TaxSubjectIdentifier 	taxIdentifier;
	private String					directory;
	private File					directoryFile;
	private String					defaultCurrency;
	private String					generalLedgerBankAccount;
	private String					generalLedgerInTransitAccount;
	private String					generalLedgerReconciliationAccount;
	private String					generalLedgerFeeAccount;
	private String					generalLedgerUnknownTrxAccount;
	private String					voucherSeries;
	
	public AdyenDirectoryToPaymentBatch(String directoryToRead) throws Exception {
		setSource(directoryToRead);
	}

	public AdyenDirectoryToPaymentBatch() {
		
	}
	
	public void setSource(String directoryToRead) throws Exception {

		directory = directoryToRead;
		taxIdentifier = TaxSubjectIdentifier.getUndefinedIdentifier();
		checkDirectoryValid();
		checkForTaxIdentifierAndCurrency();
		
	}
	
	
	
	/**
	 * Finds all applicable files in the directory and returns them as payment batches.
	 * 
	 * @return
	 */
	public List<PaymentBatch> readFilesInDirectory() {
		List<PaymentBatch> result = new ArrayList<PaymentBatch>();

		String[] filesToRead = getXlsxFiles();
		for (String file : filesToRead) {
			try {
				result.add(createPaymentBatchFromFile(file));
			} catch (Exception ee) {
				ee.printStackTrace();
			}
		}
		
		return result;
	}
	
	public PaymentBatch createPaymentBatchFromFile(String file) throws IOException, Exception {
		
		AdyenReport ratepayReport = AdyenReportParser.createFromFile(directory + File.separator + file);
		ratepayReport.setCurrency(defaultCurrency);
		AdyenReportToPaymentBatch converter = AdyenReportToPaymentBatch.buildFromReport(ratepayReport);
		PaymentBatch result = converter.getPaymentBatch();
		result.setBatchOwner(taxIdentifier);
		result.setPaymentType(PaymentType.RECEIVABLE);
		BankAccountDetail bad = new BankAccountDetail();
		bad.setCurrency(defaultCurrency);
		bad.setGeneralLedgerBankAccount(generalLedgerBankAccount);
		bad.setGeneralLedgerInTransitAccount(generalLedgerInTransitAccount);
		bad.setGeneralLedgerReconciliationAccount(generalLedgerReconciliationAccount);
		bad.setGeneralLedgerFeeAccount(generalLedgerFeeAccount);
		result.setVoucherSeries(voucherSeries);
		result.setBankAccount(bad);
		result.setSource(file);
		result.setGeneralLedgerUnknownTrxAccount(generalLedgerUnknownTrxAccount);
		return result;
		
	}
	
	private String[] getXlsxFiles() {
		String[] files = directoryFile.list(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				if (name.toLowerCase().endsWith("xlsx"))
					return true;
				return false;
			}});
		return files;
	}
	
	public TaxSubjectIdentifier getTaxIdentifier() {
		return taxIdentifier;
	}

	public void setTaxIdentifier(TaxSubjectIdentifier taxIdentifier) {
		this.taxIdentifier = taxIdentifier;
	}

	public String getDefaultCurrency() {
		return defaultCurrency;
	}

	public void setDefaultCurrency(String defaultCurrency) {
		this.defaultCurrency = defaultCurrency;
	}

	private void checkDirectoryValid() throws FileNotFoundException {
		File f = new File(directory);
		if (!f.isDirectory()) {
			throw new FileNotFoundException(directory);
		}
		directoryFile = f;
	}
	
	/**
	 * Checks if the directory is readable and retreives directory information.
	 */
	private void checkForTaxIdentifierAndCurrency() {

		File f = new File(directory + File.separator + ADYEN_PROPERTY_FILE);
		if (f.exists() && f.canRead()) {
			readAdyenPropertyFile(f);
		}
		
	}
	
	private void readAdyenPropertyFile(File f) {
		
		Properties props = new Properties();
		try {
			props.load(new FileReader(f));
			
			String taxId = props.getProperty("taxId");
			String countryCode = props.getProperty("countryCode");
			defaultCurrency = props.getProperty("defaultCurrency");
			taxIdentifier = new TaxSubjectIdentifier(taxId, countryCode);
			generalLedgerBankAccount = props.getProperty("generalLedgerBankAccount");
			generalLedgerInTransitAccount = props.getProperty("generalLedgerInTransitAccount");
			generalLedgerReconciliationAccount = props.getProperty("generalLedgerReconciliationAccount");
			generalLedgerFeeAccount = props.getProperty("generalLedgerFeeAccount");
			generalLedgerUnknownTrxAccount = props.getProperty("generalLedgerUnknownTrxAccount");
			voucherSeries = props.getProperty("voucherSeries");
			logRetrievedProperties();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	private void logRetrievedProperties() {
		// TODO: Improve logging
		if (AdyenAdapter.log.isDebugEnabled()) {
			if (defaultCurrency!=null) {
				Log.debug("Currency defined in ratepay.properties: %s",  defaultCurrency);
			}
		}
	}
	
	@Override
	public String getSystemName() {
		return AdyenAdapter.SYSTEMNAME;
	}
	
	@Override
	public List<PaymentBatch> readPaymentBatches() {
		return readFilesInDirectory();
	}

	@Override
	public void setDestination(String dest) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public PaymentBatch writePaymentBatch(PaymentBatch batch) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}
	
	
}
