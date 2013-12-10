package org.mule.kicks.components;

import org.apache.commons.lang.Validate;
import org.apache.log4j.Logger;
import org.mule.api.MuleContext;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.construct.FlowConstructAware;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Callable;

public class ConfigurationValidator implements MuleContextAware, FlowConstructAware, Callable {

	private final static Logger logger = Logger.getLogger(ConfigurationValidator.class);

	private static final String SYNC_OBJECT_TYPE = "syncObjectType";
	private static final String SYNC_FIELD_LIST = "syncFieldList";
	private static final String SYNC_FILTER_CONDITIONS = "syncFilterConditions";

	private MuleContext muleContext;
	private FlowConstruct flowConstruct;

	public void initialize() throws MuleException {

	}

	@Override
	public Object onCall(MuleEventContext eventContext) throws Exception {
		MuleMessage message = eventContext.getMessage();

		String objectType = (String) message.getInvocationProperty(SYNC_OBJECT_TYPE);
		String fieldList = (String) message.getInvocationProperty(SYNC_FIELD_LIST);
		String filterConditions = (String) message.getInvocationProperty(SYNC_FILTER_CONDITIONS);

		Validate.notEmpty(objectType, "The object type should not be null nor empty.");

		fieldList = "," + fieldList;
		message.setInvocationProperty(SYNC_FIELD_LIST, fieldList);

		filterConditions = "AND " + filterConditions;
		message.setInvocationProperty(SYNC_FILTER_CONDITIONS, filterConditions);

		return message;
	}

	public void setMuleContext(final MuleContext muleContext) {
		this.muleContext = muleContext;
	}

	public void setFlowConstruct(final FlowConstruct flowConstruct) {
		this.flowConstruct = flowConstruct;
	}

}
