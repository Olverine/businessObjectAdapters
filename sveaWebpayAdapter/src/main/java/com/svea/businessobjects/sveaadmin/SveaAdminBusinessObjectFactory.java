package com.svea.businessobjects.sveaadmin;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.notima.factoring.AbstractAddress;
import org.notima.factoring.FactoringEngine;
import org.notima.factoring.FactoringReply;
import org.notima.generic.businessobjects.BasicBusinessObjectConverter;
import org.notima.generic.businessobjects.BasicBusinessObjectFactory;
import org.notima.generic.businessobjects.BasicFactoringReservation;
import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.businessobjects.BusinessPartnerList;
import org.notima.generic.businessobjects.Currency;
import org.notima.generic.businessobjects.DunningRun;
import org.notima.generic.businessobjects.Invoice;
import org.notima.generic.businessobjects.Location;
import org.notima.generic.businessobjects.Order;
import org.notima.generic.businessobjects.PaymentTerm;
import org.notima.generic.businessobjects.Person;
import org.notima.generic.businessobjects.PriceList;
import org.notima.generic.businessobjects.Product;
import org.notima.generic.businessobjects.ProductCategory;
import org.notima.generic.businessobjects.Tax;
import org.notima.generic.ifacebusinessobjects.FactoringReservation;
import org.notima.generic.ifacebusinessobjects.OrderInvoice;

import com.svea.webpay.common.auth.SveaCredential;
import com.svea.webpayadmin.WebpayAdminBase;
import com.svea.webpayadminservice.client.ArrayOfDeliverOrderInformation;
import com.svea.webpayadminservice.client.ArrayOfGetInvoiceInformation;
import com.svea.webpayadminservice.client.ArrayOfGetOrderInformation;
import com.svea.webpayadminservice.client.ArrayOfInvoice;
import com.svea.webpayadminservice.client.ArrayOfOrder;
import com.svea.webpayadminservice.client.ArrayOfOrderDeliveryStatus;
import com.svea.webpayadminservice.client.ArrayOfOrderListItem;
import com.svea.webpayadminservice.client.ArrayOfOrderStatus;
import com.svea.webpayadminservice.client.ArrayOflong;
import com.svea.webpayadminservice.client.CreateOrderRequest;
import com.svea.webpayadminservice.client.CreateOrderResponse;
import com.svea.webpayadminservice.client.CreatePaymentPlanDetails;
import com.svea.webpayadminservice.client.DeliverOrderInformation;
import com.svea.webpayadminservice.client.DeliveryRequest;
import com.svea.webpayadminservice.client.DeliveryResponse;
import com.svea.webpayadminservice.client.GetInvoiceInformation;
import com.svea.webpayadminservice.client.GetInvoicesRequest;
import com.svea.webpayadminservice.client.GetInvoicesResponse;
import com.svea.webpayadminservice.client.GetOrderInformation;
import com.svea.webpayadminservice.client.GetOrdersRequest;
import com.svea.webpayadminservice.client.GetOrdersResponse;
import com.svea.webpayadminservice.client.IAdminService;
import com.svea.webpayadminservice.client.OrderDeliveryStatus;
import com.svea.webpayadminservice.client.OrderListItem;
import com.svea.webpayadminservice.client.OrderStatus;
import com.svea.webpayadminservice.client.OrderType;
import com.svea.webpayadminservice.client.SearchOrderFilter;
import com.svea.webpayadminservice.client.SearchOrdersRequest;
import com.svea.webpayadminservice.client.SearchOrdersResponse;
import com.svea.webpayadminservice.client.TextMatchType;


public class SveaAdminBusinessObjectFactory extends BasicBusinessObjectFactory<IAdminService,
							  com.svea.webpayadminservice.client.Invoice,
							  com.svea.webpayadminservice.client.Order,
							  Object,
							  Object,
							  Object> implements FactoringEngine {

	public static String SETTING_INCLUDE_CANCELLED_ROWS = "includeCancelledRows";
	
	private boolean includeCancelledRows = false;
	
	protected IAdminService		adminServicePort;
	
	private WebpayAdminBase webpayAdminBase;
	
	public SveaAdminBusinessObjectFactory() {
		webpayAdminBase = new WebpayAdminBase();
		adminServicePort = webpayAdminBase.getAdminServicePort();
	}

	public IAdminService getAdminClient() {
		return adminServicePort;
	}
	
	/**
	 * Initialize the business object factory with credentials
	 * This must be called before any operations can be performed.
	 * 
	 * @param aCre		The credentials.
	 */
	public void initCredentials(SveaCredential aCre) {
		webpayAdminBase.initCredentials(aCre);
		refreshSettings();
	}
	
	private void refreshSettings() {
		String ic = this.getSetting(SETTING_INCLUDE_CANCELLED_ROWS);
		includeCancelledRows = ic!=null ? Boolean.parseBoolean(ic) : false; 
	}
	
	public CreateOrderResponse createOrder(CreateOrderRequest request) {
		
		CreateOrderResponse response = adminServicePort.createOrder(request);
		return response;
		
	}

	/**
	 * Returns order type based on given credentials.
	 * Order type for credit card doesn't exist, in that case null is returned.
	 * 
	 * 
	 * @return
	 */
	public OrderType getOrderType() {
		
		switch (webpayAdminBase.getCredentials().getAccountType()) {
		
		case SveaCredential.ACCOUNTTYPE_INVOICE:
			return OrderType.INVOICE;
			
		case SveaCredential.ACCOUNTTYPE_PAYMENTPLAN:
			return OrderType.PAYMENT_PLAN;
			
		case SveaCredential.ACCOUNTTYPE_LOAN:
			return OrderType.LOAN;
			
		case SveaCredential.ACCOUNTTYPE_ACCOUNT_CREDIT:
			return OrderType.ACCOUNT_CREDIT;
			
		default: return null;
		}
		
	}
	
	public CreateOrderRequest toSveaCreateOrderRequest(OrderInvoice orderInvoice, CreatePaymentPlanDetails cpp) throws Exception {
		
		CreateOrderRequest dst;
		
		Order src = null;
		
		if (orderInvoice instanceof Order) {
			src = (Order)orderInvoice;
		} else {
			// TODO: Convert invoice to order
			throw new Exception("Can't yet convert invoice to order");
		}
		
		dst = SveaAdminConverter.convert(src, getOrderType(), cpp);
		
		dst.setAuthentication(webpayAdminBase.getAuth());
		dst.setClientId(Long.parseLong(webpayAdminBase.getCredentials().getAccountNo()));

		
		return dst;
	}
	
	/**
	 * Deliver one single order
	 * 
	 * @param sveaOrderId
	 * @return
	 */
	public DeliveryResponse deliverOrder(Long sveaOrderId, OrderType ot) {
		
		DeliveryRequest req = new DeliveryRequest();
		req.setAuthentication(webpayAdminBase.getAuth());
		
		ArrayOfDeliverOrderInformation orders = new ArrayOfDeliverOrderInformation();
		List<DeliverOrderInformation> olist = orders.getDeliverOrderInformation();
		
		DeliverOrderInformation order;
		order = new DeliverOrderInformation();
		order.setClientId(Long.parseLong(webpayAdminBase.getCredentials().getAccountNo()));
		order.setSveaOrderId(sveaOrderId);
		order.setOrderType(ot);
		olist.add(order);
		
		req.setOrdersToDeliver(orders);
		
		DeliveryResponse response = this.adminServicePort.deliverOrders(req);
		
		return response;
	}

	@Override
	public BusinessPartner<?> lookupBusinessPartner(String key)
			throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<BusinessPartner<?>> lookupAllBusinessPartners()
			throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DunningRun lookupDunningRun(String key, Date dueDateUntil) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IAdminService getClient() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public com.svea.webpayadminservice.client.Invoice lookupNativeInvoice(String key) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public com.svea.webpayadminservice.client.Invoice persistNativeInvoice(com.svea.webpayadminservice.client.Invoice invoice) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public com.svea.webpayadminservice.client.Order lookupNativeOrder(String key) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public com.svea.webpayadminservice.client.Order persistNativeOrder(com.svea.webpayadminservice.client.Order order) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Invoice<com.svea.webpayadminservice.client.Invoice> lookupInvoice(String key) throws Exception {
		
		IAdminService client = getAdminClient();
		
		GetInvoicesRequest req = new GetInvoicesRequest();
		ArrayOfGetInvoiceInformation aog = new ArrayOfGetInvoiceInformation();
		req.setInvoicesToRetrieve(aog);
		List<GetInvoiceInformation> ogl = aog.getGetInvoiceInformation();
		GetInvoiceInformation oi = new GetInvoiceInformation();
		oi.setInvoiceId(Integer.parseInt(key));
		ogl.add(oi);
		
		req.setAuthentication(webpayAdminBase.getAuth());
		
		oi.setClientId(Long.parseLong(webpayAdminBase.getCredentials().getAccountNo()));
		
		GetInvoicesResponse response = client.getInvoices(req);
		if (response.getErrorMessage()!=null) {
			throw new Exception(response.toString());
		}
		
		ArrayOfInvoice result = response.getInvoices();
		com.svea.webpayadminservice.client.Invoice resultInvoice = null;
		if (result.getInvoice()!=null && !result.getInvoice().isEmpty()) {
			resultInvoice = result.getInvoice().get(0);
		}
		
		return SveaAdminConverter.convert(resultInvoice);
		
		
	}

	/**
	 * Looks up order using sveas order ID.
	 * 
	 */
	@Override
	public Order<com.svea.webpayadminservice.client.Order> lookupOrder(String key) throws Exception {
		
		IAdminService client = getAdminClient();
		
		GetOrdersRequest req = new GetOrdersRequest();
		ArrayOfGetOrderInformation aog = new ArrayOfGetOrderInformation();
		req.setOrdersToRetrieve(aog);
		List<GetOrderInformation> ogl = aog.getGetOrderInformation();
		GetOrderInformation oi = new GetOrderInformation();
		oi.setSveaOrderId(Integer.parseInt(key));
		ogl.add(oi);
		
		req.setAuthentication(webpayAdminBase.getAuth());
		OrderType ot = null;
		if (SveaCredential.ACCOUNTTYPE_INVOICE.equals(webpayAdminBase.getCredentials().getAccountType())) {
			ot = OrderType.INVOICE;
		} else if (SveaCredential.ACCOUNTTYPE_PAYMENTPLAN.equals(webpayAdminBase.getCredentials().getAccountType())) {
			ot = OrderType.PAYMENT_PLAN;
		} else if (SveaCredential.ACCOUNTTYPE_ACCOUNT_CREDIT.equals(webpayAdminBase.getCredentials().getAccountType())) {
			ot = OrderType.ACCOUNT_CREDIT;
		} else if (SveaCredential.ACCOUNTTYPE_LOAN.equals(webpayAdminBase.getCredentials().getAccountType())) {
			ot = OrderType.LOAN;
		}
		
		oi.setClientId(Long.parseLong(webpayAdminBase.getCredentials().getAccountNo()));
		oi.setOrderType(ot);
		
		GetOrdersResponse response = client.getOrders(req);
		if (response.getErrorMessage()!=null) {
			throw new Exception("Order " + key + " : " + response.getErrorMessage());
		}
		
		ArrayOfOrder result = response.getOrders();
		com.svea.webpayadminservice.client.Order resultOrder = null;
		if (result.getOrder()!=null && !result.getOrder().isEmpty()) {
			resultOrder = result.getOrder().get(0);
		}
		
		return SveaAdminConverter.convert(resultOrder, includeCancelledRows);
	}

	
	/**
	 * Finds an order using client order id
	 * 
	 * @param clientOrderId
	 * @return
	 * @throws Exception
	 */
	public Order<com.svea.webpayadminservice.client.Order> findByClientOrderId(String clientOrderId) throws Exception {
		
		IAdminService client = getAdminClient();
		
		SearchOrdersRequest req = new SearchOrdersRequest();
		req.setAuthentication(webpayAdminBase.getAuth());
		
		SearchOrderFilter sof = new SearchOrderFilter();
		req.setSearchOrderFilter(sof);
		ArrayOfOrderDeliveryStatus aods = new ArrayOfOrderDeliveryStatus();
		sof.setAcceptedDeliveryStatus(aods);
		aods.getOrderDeliveryStatus().add(OrderDeliveryStatus.CREATED);
		aods.getOrderDeliveryStatus().add(OrderDeliveryStatus.DELIVERED);
		aods.getOrderDeliveryStatus().add(OrderDeliveryStatus.PARTIALLY_DELIVERED);
		//TODO If Cancelled is added below, the cancelled order with number clientOrderId comes up but not
		//     the next order that's not cancelled with the same clientOrderId. Wierd.
		// aods.getOrderDeliveryStatus().add(OrderDeliveryStatus.CANCELLED);
		
		ArrayOfOrderStatus aoos = new ArrayOfOrderStatus();
		sof.setAcceptedOrderStatus(aoos);
		aoos.getOrderStatus().add(OrderStatus.ACTIVE);
		aoos.getOrderStatus().add(OrderStatus.CREATED);
		aoos.getOrderStatus().add(OrderStatus.PENDING);
		aoos.getOrderStatus().add(OrderStatus.CLOSED);
		aoos.getOrderStatus().add(OrderStatus.DENIED);
		aoos.getOrderStatus().add(OrderStatus.ERROR);
		
		ArrayOflong clientIds = new ArrayOflong();
		sof.setClientIds(clientIds);
		clientIds.getLong().add(Long.parseLong(webpayAdminBase.getCredentials().getAccountNo()));
		
		sof.setTextMatchType(TextMatchType.CLIENT_ORDER_NUMBER);
		sof.setTextMatch(clientOrderId);
		
		SearchOrdersResponse response = client.searchOrders(req);
		
		Order<com.svea.webpayadminservice.client.Order> order = null;
		
		if (response!=null) {
			if (response.getErrorMessage()!=null && response.getErrorMessage().trim().length()>0) {
				throw new Exception(response.getErrorMessage());
			}
			ArrayOfOrderListItem result = response.getOrderListItems();
			
			for (OrderListItem oli : result.getOrderListItem()) {
				order = lookupOrder(Long.toString(oli.getSveaOrderId()));
				break;
			}
		}

		return order;
		
	}
	
	
	@Override
	public Product<Object> lookupProduct(String key) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Product<Object> lookupProductByEan(String ean) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Product<Object>> lookupProductByName(String name)
			throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<Object, Object> lookupList(String listName) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Product<Object> lookupRoundingProduct() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Tax lookupTax(String key) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PaymentTerm lookupPaymentTerm(String key) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public FactoringReservation lookupFactoringReservation(String key)
			throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<FactoringReservation> lookupFactoringReservationForOrder(
			String orderKey) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<FactoringReservation> lookupFactoringReservationForInvoice(
			String invoiceKey) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object persist(Object o) throws Exception {
		return null;
	}

	@Override
	public boolean isConnected() throws Exception {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void destroy() throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public FactoringReservation reserveForFactoring(Order order,
			BusinessPartner factoringCompany) throws Exception {

		
		CreateOrderRequest req = toSveaCreateOrderRequest(order, null);
		CreateOrderResponse result = createOrder(req);
		
		FactoringReservation fr = new BasicFactoringReservation();
		
		if (result.getErrorMessage()!=null) {
			fr.setInfoText(result.getErrorMessage());
			fr.setReservationResponseCode(FactoringReservation.FACTORING_ERROR_IN_COMMUNICATION);
		} else {
			fr.setOrderNo(Long.toString(result.getOrderResult().getSveaOrderId()));
			if (result.getOrderResult().isSveaWillBuyOrder()) {
				fr.setReservationResponseCode(FactoringReservation.FACTORING_OK);
			} else {
				fr.setReservationResponseCode(FactoringReservation.FACTORING_DENIED);
			}
				
		}
		
		return fr;
	}

	@Override
	public FactoringReservation reserveForFactoring(Invoice invoice,
			BusinessPartner factoringCompany) throws Exception {

		BasicBusinessObjectConverter converter = new BasicBusinessObjectConverter();
		Order order = converter.toOrder(invoice);
		
		return reserveForFactoring(order, factoringCompany);
		
	}

	@Override
	public FactoringReservation cancelReservation(Order order,
			FactoringReservation reservation) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double getPossibleFactoringAmount(BusinessPartner customer,
			Currency currency, BusinessPartner factoringCompany)
			throws Exception {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public FactoringReply sendToFactoring(BusinessPartner factoringCompany,
			Invoice invoice) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void afterSendToFactoring(BusinessPartner factoringCompany,
			Invoice invoice, FactoringReply reply) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public AbstractAddress makeAddressFromLocation(BusinessPartner customer,
			Person user, Location location) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getErrorMessage() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public FactoringReply returnToFactoring(BusinessPartner factoringCompany,
			Invoice invoice) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setFactoringProperties(Properties props) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public PriceList lookupPriceForProduct(String productKey, String currency,
			Boolean salesPriceList) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ProductCategory> lookupProductCategory(String key)
			throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BusinessPartner<Object> lookupThisCompanyInformation() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<BusinessPartner<?>> lookupBusinessPartners(int maxCount,
			boolean customers, boolean suppliers) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BusinessPartnerList<Object> listTenants() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getSystemName() {
		return "Webpay-AdminService";
	}


	
}
