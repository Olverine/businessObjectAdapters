package org.notima.fortnox.command;

import java.util.List;
import java.util.Properties;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.notima.api.fortnox.FortnoxClient3;
import org.notima.api.fortnox.FortnoxException;
import org.notima.api.fortnox.clients.FortnoxClientManager;
import org.notima.api.fortnox.entities3.CompanySetting;
import org.notima.api.fortnox.entities3.Customer;
import org.notima.businessobjects.adapter.fortnox.FortnoxAdapter;
import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;

/**
 * 
 * Adds a fortnox client.
 * 
 * @author Daniel Tamm
 *
 */
@Command(scope = "fortnox", name = "add-client", description = "Add a client to Fortnox integration")
@Service
public class AddClient implements Action {

	@SuppressWarnings("rawtypes")
	@Reference
	private List<BusinessObjectFactory> bofs;
	
	@Reference
	private Session sess;
	
	@Argument(index = 0, name = "orgNo", description ="The orgno of the client to configure", required = false, multiValued = false)
	String orgNo;

    @Argument(index = 1, name = "apiCode", description = "API code provided by client", required = false, multiValued = false)
    String apiCode;
   
    @Option(name = "--orgName", description = "The name of the organisation", required = false, multiValued = false)
    private String orgName;
    
    @Option(name = "--accessToken", description = "If there's an existing access token, omit apiCode and supply the access token", required = false, multiValued = false)
	private String accessToken;

    @Option(name = "--clientSecret", description = "The client secret for our Fortnox integration. If omitted, the default client secret is used (if set).", required = false, multiValued = false)
    private String clientSecret;
    
	@SuppressWarnings("rawtypes")
	@Override
	public Object execute() throws Exception {

		BusinessObjectFactory b = null;
		for (BusinessObjectFactory bf : bofs) {
			if ("Fortnox".equals(bf.getSystemName())) {
				b = bf;
				break;
			}
		}

		if (b==null) {
			sess.getConsole().println("No Fortnox adapter available");
			return null;
		}
		
		FortnoxAdapter fa = (FortnoxAdapter)b;
		FortnoxClientManager mgr = fa.getClientManager();

		if (mgr!=null) {
			if (mgr.getDefaultClientSecret()!=null && clientSecret==null) {
				clientSecret = mgr.getDefaultClientSecret();
			}
		}
		
		CompanySetting cs = null;
		if (orgNo==null) {
			if (accessToken!=null && clientSecret!=null) {
				FortnoxClient3 fc3 = fa.getClient();
				fc3.setAccessToken(accessToken, clientSecret);
				try { 
					cs = fc3.getCompanySetting();
					if (cs!=null) {
						orgNo = cs.getOrganizationNumber();
						orgName = cs.getName();
					}
				} catch (FortnoxException fe) {
					if (FortnoxClient3.ERROR_NOT_AUTH_FOR_SCOPE.equals(fe.getErrorInformation().getCode())) {
						sess.getConsole().println("Can't fetch company information from Fortnox.");
					} else {
						sess.getConsole().println(fe.toString()); 
					}
				}
			}
		}
		
		if (orgNo==null) {
			sess.getConsole().println("OrgNo must be supplied when there are no working credentials");
			return null;
		}
		
		Properties props = new Properties();
		if (apiCode!=null)
			props.setProperty("apiCode", apiCode);
		if (accessToken!=null)
			props.setProperty("accessToken", accessToken);
		if (clientSecret!=null) {
			props.setProperty("clientSecret", clientSecret);
		}
		
		BusinessPartner<Customer> bp = fa.addTenant(orgNo, "SE", orgName, props);

		if (bp!=null) {
			sess.getConsole().println("Customer [" + bp.getTaxId() + "] " + bp.getName() + " added.");
		}
		return null;
	}
	
}
