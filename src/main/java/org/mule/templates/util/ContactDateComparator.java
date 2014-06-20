/**
 * Mule Anypoint Template
 * Copyright (c) MuleSoft, Inc.
 * All rights reserved.  http://www.mulesoft.com
 */

package org.mule.templates.util;

import java.util.Map;

import org.apache.commons.lang.Validate;

/**
 * The function of this class is to establish a relation happens before between
 * two maps representing SFDC contacts.
 * 
 * It's assumed that these maps are well formed maps from SFDC thus they both
 * contain an entry with the expected key. Never the less validations are being
 * done.
 * 
 * @author damiansima
 */
public class ContactDateComparator {
	private static final String LAST_MODIFIED_DATE = "LastModifiedDate";

	/**
	 * Validate which contact has the latest last modification date.
	 * 
	 * @param contactA
	 *            SFDC contact map
	 * @param contactB
	 *            SFDC contact map
	 * @return true if the last modified date from contactA is after the one
	 *         from contact B
	 */
	public static boolean isAfter(Map<String, String> contactA, Map<String, String> contactB) {
		Validate.notNull(contactA, "The contact A should not be null");
		Validate.notNull(contactB, "The contact B should not be null");

		Validate.isTrue(contactA.containsKey(LAST_MODIFIED_DATE), "The contact A map should containt the key " + LAST_MODIFIED_DATE);
		Validate.isTrue(contactB.containsKey(LAST_MODIFIED_DATE), "The contact B map should containt the key " + LAST_MODIFIED_DATE);

		return DateUtils.isAfter(contactA.get(LAST_MODIFIED_DATE), contactB.get(LAST_MODIFIED_DATE));
	}
}
