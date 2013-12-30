package org.mule.kicks.integration;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import junit.framework.Assert;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.config.MuleProperties;
import org.mule.construct.Flow;
import org.mule.processor.chain.SubflowInterceptingChainLifecycleWrapper;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transport.NullPayload;

import com.sforce.soap.partner.SaveResult;

/**
 * The objective of this class is to validate the correct behavior of the Mule
 * Kick that make calls to external systems.
 * 
 * @author damiansima
 */
public class BusinessLogicTestIT extends FunctionalTestCase {

	private static SubflowInterceptingChainLifecycleWrapper checkContactflow;
	private static List<Map<String, String>> createdContacts = new ArrayList<Map<String, String>>();

	@BeforeClass
	public static void beforeClass() {
		System.setProperty("mule.env", "test");
	}

	@AfterClass
	public static void afterClass() {
		System.getProperties().remove("mule.env");
	}

	@Before
	@SuppressWarnings("unchecked")
	public void setUp() throws Exception {
		checkContactflow = getSubFlow("retrieveContactFlow");
		checkContactflow.initialise();

		SubflowInterceptingChainLifecycleWrapper flow = getSubFlow("createContactFlow");
		flow.initialise();

		// This contact should not be sync
		Map<String, String> contact = createContact("A", 0);
		contact.put("Email", "");
		createdContacts.add(contact);

		// This contact should not be sync
		contact = createContact("A", 1);
		contact.put("MailingCountry", "ARG");
		createdContacts.add(contact);

		// This contact should BE sync
		contact = createContact("A", 2);
		createdContacts.add(contact);

		MuleEvent event = flow.process(getTestEvent(createdContacts, MessageExchangePattern.REQUEST_RESPONSE));
		List<SaveResult> results = (List<SaveResult>) event.getMessage().getPayload();
		for (int i = 0; i < results.size(); i++) {
			createdContacts.get(i).put("Id", results.get(i).getId());
		}
	}

	@After
	public void tearDown() throws Exception {
		// Delete the created contacts in A
		SubflowInterceptingChainLifecycleWrapper flow = getSubFlow("deleteContactFromAFlow");
		flow.initialise();

		List<String> idList = new ArrayList<String>();
		for (Map<String, String> c : createdContacts) {
			idList.add(c.get("Id"));
		}
		flow.process(getTestEvent(idList, MessageExchangePattern.REQUEST_RESPONSE));

		// Delete the created contacts in B
		flow = getSubFlow("deleteContactFromBFlow");
		flow.initialise();
		idList.clear();
		for (Map<String, String> c : createdContacts) {
			Map<String, String> contact = invokeRetrieveContactFlow(checkContactflow, c);
			if (contact != null) {
				idList.add(contact.get("Id"));
			}
		}
		flow.process(getTestEvent(idList, MessageExchangePattern.REQUEST_RESPONSE));
	}

	@Override
	protected String getConfigResources() {
		String resources = "";
		try {
			Properties props = new Properties();
			props.load(new FileInputStream("./src/main/app/mule-deploy.properties"));
			resources = props.getProperty("config.resources");
		} catch (Exception e) {
			throw new IllegalStateException(
					"Could not find mule-deploy.properties file on classpath. Please add any of those files or override the getConfigResources() method to provide the resources by your own.");
		}

		return resources + ",./configurations/test-flows.xml";
	}

	@Override
	protected Properties getStartUpProperties() {
		Properties properties = new Properties(super.getStartUpProperties());

		String pathToResource = "./mappings";
		File graphFile = new File(pathToResource);

		properties.put(MuleProperties.APP_HOME_DIRECTORY_PROPERTY, graphFile.getAbsolutePath());

		return properties;
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testMainFlow() throws Exception {
		Flow flow = getFlow("mainFlow");
		flow.process(getTestEvent("", MessageExchangePattern.REQUEST_RESPONSE));

		Assert.assertEquals("The contact should not have been sync", null,
				invokeRetrieveContactFlow(checkContactflow, createdContacts.get(0)));

		Assert.assertEquals("The contact should not have been sync", null,
				invokeRetrieveContactFlow(checkContactflow, createdContacts.get(1)));

		Map<String, String> payload = invokeRetrieveContactFlow(checkContactflow, createdContacts.get(2));
		Assert.assertEquals("The contact should have been sync", createdContacts.get(2).get("Email"),
				payload.get("Email"));
	}

	@SuppressWarnings("unchecked")
	private Map<String, String> invokeRetrieveContactFlow(SubflowInterceptingChainLifecycleWrapper flow,
			Map<String, String> contact) throws Exception {
		Map<String, String> contactMap = new HashMap<String, String>();

		contactMap.put("Email", contact.get("Email"));
		contactMap.put("FirstName", contact.get("FirstName"));
		contactMap.put("LastName", contact.get("LastName"));

		MuleEvent event = flow.process(getTestEvent(contactMap, MessageExchangePattern.REQUEST_RESPONSE));
		Object payload = event.getMessage().getPayload();
		if (payload instanceof NullPayload) {
			return null;
		} else {
			return (Map<String, String>) payload;
		}
	}

	private Flow getFlow(String flowName) {
		return (Flow) muleContext.getRegistry().lookupObject(flowName);
	}

	private SubflowInterceptingChainLifecycleWrapper getSubFlow(String flowName) {
		return (SubflowInterceptingChainLifecycleWrapper) muleContext.getRegistry().lookupObject(flowName);
	}

	private Map<String, String> createContact(String orgId, int sequence) {
		Map<String, String> contact = new HashMap<String, String>();

		contact.put("FirstName", "FirstName_" + sequence);
		contact.put("LastName", "LastName_" + sequence);
		contact.put("Email", "some.email." + sequence + "@fakemail.com");
		contact.put("Description", "Some fake description");
		contact.put("MailingCity", "Denver");
		contact.put("MailingCountry", "USA");
		contact.put("MobilePhone", "123456789");
		contact.put("Department", "department_" + sequence + "_" + orgId);
		contact.put("Phone", "123456789");
		contact.put("Title", "Dr");

		return contact;
	}

}
