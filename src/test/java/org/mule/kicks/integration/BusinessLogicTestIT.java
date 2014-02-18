package org.mule.kicks.integration;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.construct.Flow;
import org.mule.kicks.builders.SfdcObjectBuilder;
import org.mule.kicks.util.BatchTestHelper;
import org.mule.processor.chain.SubflowInterceptingChainLifecycleWrapper;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.NullPayload;

import com.sforce.soap.partner.SaveResult;

/**
 * The objective of this class is to validate the correct behavior of the Mule Kick that make calls to external systems.
 * 
 * The test will invoke the batch process and afterwards check that the contacts had been correctly created and that the ones that should be filtered are not in
 * the destination sand box.
 * 
 * @author damiansima
 */
public class BusinessLogicTestIT extends AbstractKickTestCase {
	private static final int TIMEOUT_MILLIS = 60;

	private static SubflowInterceptingChainLifecycleWrapper checkContactflow;
	private static List<Map<String, String>> createdContacts = new ArrayList<Map<String, String>>();

	private BatchTestHelper helper;

	@Rule
	public DynamicPort port = new DynamicPort("http.port");

	@Before
	public void setUp() throws Exception {

		helper = new BatchTestHelper(muleContext);

		checkContactflow = getSubFlow("retrieveContactFlow");
		checkContactflow.initialise();

		createTestDataInSandBox();
	}

	@After
	public void tearDown() throws Exception {
		deleteTestDataFromSandBox();
	}

	@Test
	public void testMainFlow() throws Exception {
		Flow flow = getFlow("mainFlow");
		flow.process(getTestEvent("", MessageExchangePattern.REQUEST_RESPONSE));

		// Wait for the batch job executed by the poll flow to finish
		helper.awaitJobTermination(TIMEOUT_MILLIS * 1000, 500);
		helper.assertJobWasSuccessful();

		Assert.assertEquals("The contact should not have been sync", null, invokeRetrieveContactFlow(checkContactflow, createdContacts.get(0)));

		Assert.assertEquals("The contact should not have been sync", null, invokeRetrieveContactFlow(checkContactflow, createdContacts.get(1)));

		Map<String, String> payload = invokeRetrieveContactFlow(checkContactflow, createdContacts.get(2));
		Assert.assertEquals("The contact should have been sync", createdContacts.get(2)
																				.get("Email"), payload.get("Email"));
	}

	@SuppressWarnings("unchecked")
	private Map<String, String> invokeRetrieveContactFlow(SubflowInterceptingChainLifecycleWrapper flow, Map<String, String> contact) throws Exception {
		Map<String, String> contactMap = SfdcObjectBuilder.aContact()
															.with("Email", contact.get("Email"))
															.with("FirstName", contact.get("FirstName"))
															.with("LastName", contact.get("LastName"))
															.build();

		MuleEvent event = flow.process(getTestEvent(contactMap, MessageExchangePattern.REQUEST_RESPONSE));
		Object payload = event.getMessage()
								.getPayload();
		if (payload instanceof NullPayload) {
			return null;
		} else {
			return (Map<String, String>) payload;
		}
	}

	@SuppressWarnings("unchecked")
	private void createTestDataInSandBox() throws MuleException, Exception {
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
		List<SaveResult> results = (List<SaveResult>) event.getMessage()
															.getPayload();
		for (int i = 0; i < results.size(); i++) {
			createdContacts.get(i)
							.put("Id", results.get(i)
												.getId());
		}
	}

	private void deleteTestDataFromSandBox() throws MuleException, Exception {
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

	private Map<String, String> createContact(String orgId, int sequence) {
		return SfdcObjectBuilder.aContact()
								.with("FirstName", "FirstName_" + sequence)
								.with("LastName", buildUniqueEmail("LastName_" + sequence))
								.with("Email", "some.email." + sequence + "@fakemail.com")
								.with("Description", "Some fake description")
								.with("MailingCity", "Denver")
								.with("MailingCountry", "USA")
								.with("MobilePhone", "123456789")
								.with("Department", "department_" + sequence + "_" + orgId)
								.with("Phone", "123456789")
								.with("Title", "Dr")
								.build();

	}

	private String buildUniqueEmail(String user) {
		String server = "fakemail";
		String kickName = "contactmigration";
		String timeStamp = new Long(new Date().getTime()).toString();

		StringBuilder builder = new StringBuilder();
		builder.append(user);
		builder.append(".");
		builder.append(timeStamp);
		builder.append("@");
		builder.append(server);
		builder.append(kickName);
		builder.append(".com");

		return builder.toString();

	}

}
