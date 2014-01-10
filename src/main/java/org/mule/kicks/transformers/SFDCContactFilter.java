package org.mule.kicks.transformers;

import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractMessageTransformer;
import org.mule.transport.NullPayload;

/**
 * The purpose of this class is to decide whether or not a contact should be
 * sync. Provided two contacts one from Org A (source) and one from Org B
 * (destination) the class will decided which one to use.
 * 
 * The newest Last modified date will win. Should no contact in Org B is
 * provided then the contact in Org A will be sync.
 * 
 * @author damiansima
 */
public class SFDCContactFilter extends AbstractMessageTransformer {
	private static final String ID_FIELD = "Id";
	private static final String FIELD_TYPE = "type";
	private static final String LAST_MODIFIED_DATE = "LastModifiedDate";

	private static final String CONTACT_IN_COMPANY_B = "contactInB";

	@Override
	@SuppressWarnings({ "unchecked" })
	public Object transformMessage(MuleMessage message, String outputEncoding) throws TransformerException {
		Map<String, String> contactInA = (Map<String, String>) message.getPayload();

		if (!(message.getInvocationProperty(CONTACT_IN_COMPANY_B) instanceof NullPayload)) {
			Map<String, String> contactInB = (Map<String, String>) message.getInvocationProperty(CONTACT_IN_COMPANY_B);
			if (isAfter(contactInA, contactInB)) {
				contactInA.put(ID_FIELD, contactInB.get(ID_FIELD));
			} else {
				contactInA = null;
			}
		}

		if (contactInA != null) {
			removeUnecesaryFields(contactInA);
		}

		message.setPayload(contactInA);
		return message;
	}

	private void removeUnecesaryFields(Map<String, String> contact) {
		contact.remove(FIELD_TYPE);
		contact.remove(LAST_MODIFIED_DATE);
	}

	private boolean isAfter(Map<String, String> contactA, Map<String, String> contactB) {
		DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		DateTime lastModifiedDateOfA = formatter.parseDateTime(contactA.get(LAST_MODIFIED_DATE));
		DateTime lastModifiedDateOfB = formatter.parseDateTime(contactB.get(LAST_MODIFIED_DATE));

		return lastModifiedDateOfA.isAfter(lastModifiedDateOfB);
	}
}