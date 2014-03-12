package org.mule.templates.integration;

import static org.mule.templates.builders.SfdcObjectBuilder.anAccount;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.processor.chain.SubflowInterceptingChainLifecycleWrapper;

import com.mulesoft.module.batch.BatchTestHelper;
import com.sforce.soap.partner.SaveResult;

/**
 * The objective of this class is to validate the correct behavior of the Mule Kick that make calls to external systems.
 * 
 * The test will invoke the batch process and afterwards check that the contacts had been correctly created and that the ones that should be filtered are not in
 * the destination sand box.
 * 
 * The test validates that an account will get sync as result of the integration.
 * 
 * @author damiansima
 */
public class BusinessLogicTestCreateAccountIT extends AbstractTemplateTestCase {

	private BatchTestHelper helper;
	private List<Map<String, Object>> createdContacts = new ArrayList<Map<String, Object>>();
	private List<Map<String, Object>> createdAccounts = new ArrayList<Map<String, Object>>();

	@BeforeClass
	public static void init() {
		System.setProperty("account.sync.policy", "syncAccount");
		System.setProperty("account.id.in.b", "");

	}

	@AfterClass
	public static void shutDown() {
		System.clearProperty("account.sync.policy");
	}

	@Before
	public void setUp() throws Exception {
		helper = new BatchTestHelper(muleContext);

		checkContactflow = getSubFlow("retrieveContactFlow");
		checkContactflow.initialise();

		checkAccountflow = getSubFlow("retrieveAccountFlow");
		checkAccountflow.initialise();

		createTestDataInSandBox();
	}

	@After
	public void tearDown() throws Exception {
		deleteTestDataFromSandBox();
	}

	@Test
	public void testMainFlow() throws Exception {

		runFlow("mainFlow");

		// Wait for the batch job executed by the poll flow to finish
		helper.awaitJobTermination(TIMEOUT_SEC * 1000, 500);
		helper.assertJobWasSuccessful();

		Assert.assertEquals("The contact should not have been sync", null, invokeRetrieveFlow(checkContactflow, createdContacts.get(0)));

		Assert.assertEquals("The contact should not have been sync", null, invokeRetrieveFlow(checkContactflow, createdContacts.get(1)));

		Map<String, Object> accountPayload = invokeRetrieveFlow(checkAccountflow, createdAccounts.get(0));
		Map<String, Object> contacPayload = invokeRetrieveFlow(checkContactflow, createdContacts.get(2));
		Assert.assertEquals("The contact should have been sync", createdContacts.get(2)
																				.get("Email"), contacPayload.get("Email"));

		Assert.assertEquals("The contact should belong to a different account ", accountPayload.get("Id"), contacPayload.get("AccountId"));
	}

	private void createTestDataInSandBox() throws MuleException, Exception {
		createAccounts();
		createContacts();
	}

	@SuppressWarnings("unchecked")
	private void createAccounts() throws Exception {
		SubflowInterceptingChainLifecycleWrapper flow = getSubFlow("createAccountFlow");
		flow.initialise();
		createdAccounts.add(anAccount().with("Name", buildUniqueName(TEMPLATE_NAME, "ReferencedAccountTest"))
										.with("BillingCity", "San Francisco")
										.with("BillingCountry", "USA")
										.with("Phone", "123456789")
										.with("Industry", "Education")
										.with("NumberOfEmployees", 9000)
										.build());

		MuleEvent event = flow.process(getTestEvent(createdAccounts, MessageExchangePattern.REQUEST_RESPONSE));
		List<SaveResult> results = (List<SaveResult>) event.getMessage()
															.getPayload();
		for (int i = 0; i < results.size(); i++) {
			createdAccounts.get(i)
							.put("Id", results.get(i)
												.getId());
		}

		System.out.println("Results of data creation in sandbox" + createdAccounts.toString());
	}

	@SuppressWarnings("unchecked")
	private void createContacts() throws Exception {
		SubflowInterceptingChainLifecycleWrapper flow = getSubFlow("createContactFlow");
		flow.initialise();

		// This contact should not be sync
		Map<String, Object> contact = createContact("A", 0);
		contact.put("Email", "");
		createdContacts.add(contact);

		// This contact should not be sync
		contact = createContact("A", 1);
		contact.put("MailingCountry", "ARG");
		createdContacts.add(contact);

		// This contact should BE sync with it's account
		contact = createContact("A", 2);
		contact.put("AccountId", createdAccounts.get(0)
												.get("Id"));
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
		deleteTestContactFromSandBox(createdContacts);
		deleteTestAccountFromSandBox(createdAccounts);
	}

}
