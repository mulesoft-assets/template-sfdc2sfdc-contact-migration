package org.mule.kicks.transformers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractMessageTransformer;
import org.mule.transport.NullPayload;

/**
 * The purpose of this class is to decide whether or not a contact should be
 * sync.
 * 
 * @author damiansima
 */
public class SFDCContactFilter extends AbstractMessageTransformer {
	private static final String ID_FIELD = "Id";
	private static final String CONTACT_IN_COMPANY_B = "contactInB";
	private static final String LAST_MODIFIED_DATE = "LastModifiedDate";
	private static final String FILTERED_CONTACTS_COUNT = "filteredContactsCount";

	private final static Logger logger = Logger.getLogger(SFDCContactFilter.class);

	@Override
	public Object transformMessage(MuleMessage message, String outputEncoding) throws TransformerException {

		List<Map<String, String>> contactToSync = new ArrayList<Map<String, String>>();

		Map<String, String> contactInA = (Map<String, String>) message.getPayload();
		if (message.getInvocationProperty(CONTACT_IN_COMPANY_B) instanceof NullPayload) {

			contactInA.remove(LAST_MODIFIED_DATE);
			contactToSync.add(contactInA);

		} else {
			Map<String, String> contactInB = message.getInvocationProperty(CONTACT_IN_COMPANY_B);
			
			DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
			DateTime lastModifiedDateOfA = formatter.parseDateTime(contactInA.get(LAST_MODIFIED_DATE));
			DateTime lastModifiedDateOfB = formatter.parseDateTime(contactInB.get(LAST_MODIFIED_DATE));

			if (lastModifiedDateOfA.isAfter(lastModifiedDateOfB)) {
				contactInA.put(ID_FIELD, contactInB.get(ID_FIELD));
				contactInA.remove(LAST_MODIFIED_DATE);
				contactToSync.add(contactInA);
			} else {
				increaseFilteredContactsCount(message);
			}
		}

		message.setPayload(contactToSync);
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
