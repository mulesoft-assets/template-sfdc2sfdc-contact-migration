package org.mule.kicks.transformers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractMessageTransformer;

/**
 * The purpose of this class is to decide whether or not a contact should be
 * sync.
 * 
 * @author damiansima
 */
public class SFDCContactFilter extends AbstractMessageTransformer {
	private static final String EMAIL_KEY = "Email";
	private static final String FILTERED_CONTACTS_COUNT = "filteredContactsCount";

	private final static Logger logger = Logger.getLogger(SFDCContactFilter.class);

	@Override
	public Object transformMessage(MuleMessage message, String outputEncoding) throws TransformerException {

		Map<String, String> contact = (Map<String, String>) message.getPayload();

		if (contact.get(EMAIL_KEY) != null) {
			List<Map<String, String>> contactToSync = new ArrayList<Map<String, String>>();
			contactToSync.add(contact);
			message.setPayload(contactToSync);
		} else {
			increaseFilteredContactsCount(message);
			message.setPayload(null);
		}

		return message;
	}

	private void increaseFilteredContactsCount(MuleMessage message) {
		Integer filteredCount = (Integer) message.getInvocationProperty(FILTERED_CONTACTS_COUNT);
		if (filteredCount == null) {
			filteredCount = 0;
		}
		filteredCount++;
		message.setInvocationProperty(FILTERED_CONTACTS_COUNT, filteredCount);
	}

}
