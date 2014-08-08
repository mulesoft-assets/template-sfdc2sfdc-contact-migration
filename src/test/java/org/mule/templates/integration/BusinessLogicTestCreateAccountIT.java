/**
 * Mule Anypoint Template
 * Copyright (c) MuleSoft, Inc.
 * All rights reserved.  http://www.mulesoft.com
 */

package org.mule.templates.integration;

import static org.mule.templates.builders.SfdcObjectBuilder.aContact;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.processor.chain.InterceptingChainLifecycleWrapper;
import org.mule.processor.chain.SubflowInterceptingChainLifecycleWrapper;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.templates.AbstractTemplatesTestCase;
import org.mule.templates.builders.SfdcObjectBuilder;

import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.mulesoft.module.batch.BatchTestHelper;
import com.sforce.soap.partner.SaveResult;

/**
 * The objective of this class is validating the correct behavior of the flows
 * for this Mule Anypoint Template
 * 
 */
@SuppressWarnings("unchecked")
public class BusinessLogicTestCreateAccountIT extends AbstractTemplatesTestCase {

	private static final String ANYPOINT_TEMPLATE_NAME = "sfdc2sfdc-contact-migration";
	private static final String B_INBOUND_FLOW_NAME = "triggerMainFlow";
	private static final String AN_ACCOUNT_ID_IN_B = "0012000001AHHlvAAH";
	private static final int TIMEOUT_MILLIS = 60;

	private static List<String> contactsCreatedInA = new ArrayList<String>();
	private static List<String> contactsCreatedInB = new ArrayList<String>();
	private static SubflowInterceptingChainLifecycleWrapper deleteContactFromAFlow;
	private static SubflowInterceptingChainLifecycleWrapper deleteContactFromBFlow;

	private SubflowInterceptingChainLifecycleWrapper createContactInAFlow;
	private SubflowInterceptingChainLifecycleWrapper createContactInBFlow;
	private InterceptingChainLifecycleWrapper queryContactFromAFlow;
	private InterceptingChainLifecycleWrapper queryContactFromBFlow;
	private BatchTestHelper batchTestHelper;
	private SubflowInterceptingChainLifecycleWrapper createAccountInAFlow;
	private SubflowInterceptingChainLifecycleWrapper createAccountInBFlow;
	private SubflowInterceptingChainLifecycleWrapper queryContactsAccountNameFromAFlow;
	private SubflowInterceptingChainLifecycleWrapper queryContactsAccountNameFromBFlow;
	
	@Rule
	public DynamicPort port = new DynamicPort("http.port");

	@BeforeClass
	public static void beforeTestClass() {
		System.setProperty("page.size", "1000");
		System.setProperty("account.sync.policy", "syncAccount");
	}
	
	@Before
	public void setUp() throws MuleException {
		getAndInitializeFlows();
		
		batchTestHelper = new BatchTestHelper(muleContext);
	}

	@AfterClass
	public static void shutDown() {
		System.clearProperty("account.sync.policy");
	}

	@After
	public void tearDown() throws MuleException, Exception {
		cleanUpSandboxesByRemovingTestContacts();
	}

	private void getAndInitializeFlows() throws InitialisationException {
		// Flow for creating contacts in sfdc A instance
		createContactInAFlow = getSubFlow("createContactInAFlow");
		createContactInAFlow.initialise();

		// Flow for creating contacts in sfdc B instance
		createContactInBFlow = getSubFlow("createContactInBFlow");
		createContactInBFlow.initialise();

		// Flow for deleting contacts in sfdc A instance
		deleteContactFromAFlow = getSubFlow("deleteContactFromAFlow");
		deleteContactFromAFlow.initialise();

		// Flow for deleting contacts in sfdc B instance
		deleteContactFromBFlow = getSubFlow("deleteContactFromBFlow");
		deleteContactFromBFlow.initialise();

		// Flow for querying the contact in sfdc A instance
		queryContactFromAFlow = getSubFlow("queryContactFromAFlow");
		queryContactFromAFlow.initialise();

		// Flow for querying the contact in sfdc B instance
		queryContactFromBFlow = getSubFlow("queryContactFromBFlow");
		queryContactFromBFlow.initialise();
		
		// Flow for creating accounts in sfdc A instance
		createAccountInAFlow = getSubFlow("createAccountInAFlow");
		createAccountInAFlow.initialise();
		
		// Flow for creating accounts in sfdc B instance
		createAccountInBFlow = getSubFlow("createAccountInBFlow");
		createAccountInBFlow.initialise();

		// Flow for querying accounts in sfdc A instance
		queryContactsAccountNameFromAFlow = getSubFlow("queryContactsAccountNameFromAFlow");
		queryContactsAccountNameFromAFlow.initialise();

		// Flow for querying accounts in sfdc B instance
		queryContactsAccountNameFromBFlow = getSubFlow("queryContactsAccountNameFromBFlow");
		queryContactsAccountNameFromBFlow.initialise();
	}

	private static void cleanUpSandboxesByRemovingTestContacts()
			throws MuleException, Exception {
		final List<String> idList = new ArrayList<String>();
		for (String contact : contactsCreatedInA) {
			idList.add(contact);
		}
		deleteContactFromAFlow.process(getTestEvent(idList,
				MessageExchangePattern.REQUEST_RESPONSE));
		idList.clear();
		for (String contact : contactsCreatedInB) {
			idList.add(contact);
		}
		deleteContactFromBFlow.process(getTestEvent(idList,
				MessageExchangePattern.REQUEST_RESPONSE));
	}

	@Test
	public void testMainFlow()
			throws MuleException, Exception {
		// Build test contacts
		SfdcObjectBuilder contact = aContact()
				.with("FirstName", "Manuel")
				.with("LastName", "Valadares")
				.with("MailingCountry", "US")
				.with("Email",
						ANYPOINT_TEMPLATE_NAME + "-"
								+ System.currentTimeMillis()
								+ "portuga@mail.com");

		SfdcObjectBuilder contactB = contact.with("AccountId", AN_ACCOUNT_ID_IN_B);

		// Create contact in sand-box and keep track of it for posterior
		// cleaning up
		contactsCreatedInB.add(createTestContactsInSfdcSandbox(
				contactB.build(), createContactInBFlow));

		// Execution
		executeWaitAndAssertBatchJob(B_INBOUND_FLOW_NAME);

		// Assertions
		Map<String, String> retrievedContactFromA = (Map<String, String>) queryContact(
				contact.build(), queryContactFromAFlow);
		Map<String, String> retrievedContactFromB = (Map<String, String>) queryContact(
				contact.build(), queryContactFromBFlow);

		final MapDifference<String, String> contactMapsDifference = Maps.difference(
				retrievedContactFromA, retrievedContactFromB);
		Assert.assertTrue(
				"Some contacts are not synchronized between systems. "
						+ contactMapsDifference.toString(), contactMapsDifference.areEqual());
		
		Map<String, String> retrievedContactsAccountIdFromA = (Map<String, String>) queryContact(
				contact.build(), queryContactsAccountNameFromAFlow);
		Map<String, String> retrievedContactsAccountIdFromB = (Map<String, String>) queryContact(
				contact.build(), queryContactsAccountNameFromBFlow);
		final MapDifference<String, String> accountMapsDifference = Maps.difference(
				retrievedContactsAccountIdFromA, retrievedContactsAccountIdFromB);
		Assert.assertTrue("The contact should belong to the same account!" 
						+ accountMapsDifference.toString(), accountMapsDifference.areEqual());
	}

	private Object queryContact(Map<String, Object> contact,
			InterceptingChainLifecycleWrapper queryContactFlow)
			throws MuleException, Exception {
		return queryContactFlow
				.process(
						getTestEvent(contact,
								MessageExchangePattern.REQUEST_RESPONSE))
				.getMessage().getPayload();
	}

	private String createTestContactsInSfdcSandbox(Map<String, Object> contact,
			InterceptingChainLifecycleWrapper createContactFlow)
			throws MuleException, Exception {
		List<Map<String, Object>> salesforceContacts = new ArrayList<Map<String, Object>>();
		salesforceContacts.add(contact);

		final List<SaveResult> payloadAfterExecution = (List<SaveResult>) createContactFlow
				.process(
						getTestEvent(salesforceContacts,
								MessageExchangePattern.REQUEST_RESPONSE))
				.getMessage().getPayload();
		return payloadAfterExecution.get(0).getId();
	}

	private void executeWaitAndAssertBatchJob(String flowConstructName)
			throws Exception {

		// Execute synchronization
		runSchedulersOnce(flowConstructName);

		// Wait for the batch job execution to finish
		batchTestHelper.awaitJobTermination(TIMEOUT_MILLIS * 1000, 500);
		batchTestHelper.assertJobWasSuccessful();
	}
	

}
