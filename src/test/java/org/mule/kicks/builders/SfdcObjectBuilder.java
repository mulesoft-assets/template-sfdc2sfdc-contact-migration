package org.mule.kicks.builders;

import java.util.HashMap;
import java.util.Map;

public class SfdcObjectBuilder {

	private Map<String, String> fields;

	public SfdcObjectBuilder() {
		this.fields = new HashMap<String, String>();
	}

	public SfdcObjectBuilder with(String field, String value) {
		SfdcObjectBuilder copy = new SfdcObjectBuilder();
		copy.fields.putAll(this.fields);
		copy.fields.put(field, value);
		return copy;
	}

	public Map<String, String> build() {
		return fields;
	}

	/*
	 * Creation methods
	 */

	public static SfdcObjectBuilder aContact() {
		return new SfdcObjectBuilder();
	}

	public static SfdcObjectBuilder aCustomObject() {
		return new SfdcObjectBuilder();
	}

	public static SfdcObjectBuilder aUser() {
		return new SfdcObjectBuilder();
	}

	public static SfdcObjectBuilder anAccount() {
		return new SfdcObjectBuilder();
	}

}
