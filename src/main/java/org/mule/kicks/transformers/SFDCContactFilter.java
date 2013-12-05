package org.mule.kicks.transformers;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
	private static final String EMAIL_KEY = "Email";
	private static final String MAILING_COUNTRY_KEY = "MailingCountry";

	private static final String VALID_COUNTRY = "USA";

	private static final String FILTERED_CONTACTS_COUNT = "filteredContactsCount";

	private final static Logger logger = Logger.getLogger(SFDCContactFilter.class);

	@Override
	public Object transformMessage(MuleMessage message, String outputEncoding) throws TransformerException {

		Map<String, String> contactInA = (Map<String, String>) message.getPayload();

		System.out.println(contactInA.get("MailingCountry"));
		
		if (StringUtils.isEmpty(contactInA.get(MAILING_COUNTRY_KEY))) {
			message.setPayload(null);
			return message;
		}
		

		if (StringUtils.isNotEmpty(contactInA.get(EMAIL_KEY)) && contactInA.get(MAILING_COUNTRY_KEY).equals(VALID_COUNTRY) ) {
			
			List<Map<String, String>> contactToSync = new ArrayList<Map<String, String>>();
			
			if ( message.getInvocationProperty("contactInB") instanceof NullPayload) {
				
				contactInA.remove("LastModifiedDate");
				contactToSync.add(contactInA);
				message.setPayload(contactToSync);
			
			} else {
			
				Map<String, String> contactInB = message.getInvocationProperty("contactInB");
				DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
				DateTime lastModifiedDateOfA = formatter.parseDateTime(contactInA.get("LastModifiedDate"));
				DateTime lastModifiedDateOfB = formatter.parseDateTime(contactInB.get("LastModifiedDate"));

				if (lastModifiedDateOfA.isAfter(lastModifiedDateOfB)){
					contactInA.put("Id", contactInB.get("Id"));
					contactInA.remove("LastModifiedDate");
					contactToSync.add(contactInA);
					message.setPayload(contactToSync);
				} else {
					message.setPayload(null);
				}
			}
			
			
		} else {
			increaseFilteredContactsCount(message);
			message.setPayload(null);
		}
		
		return message;
	}
	
	private Map<String, String> createContatToImport(Map<String, String> contactInA, Map<String, String> contactInB){
		if (contactInB != null) {
			contactInA.put("Id", contactInA.get("ID"));
		} 
		return contactInA;
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
