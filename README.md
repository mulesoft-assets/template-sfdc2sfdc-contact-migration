
# Anypoint Template: Salesforce to Salesforce Contact Migration

Moves a large set of contacts from one Salesforce org to another. You can trigger this manually or programmatically with an HTTP call. Contacts are upserted so that the migration can be run multiple times without worrying about creating duplicates. 

Parent accounts of the contacts are created if they don’t already exist in the destination system, or can be set to be a specific account for all contacts that are migrated. This template uses batch to efficiently process many records at a time.

![d169f604-cb6e-4298-8de7-04cdf86bb1a9-image.png](https://exchange2-file-upload-service-kprod.s3.us-east-1.amazonaws.com:443/d169f604-cb6e-4298-8de7-04cdf86bb1a9-image.png)

[//]: # (![]\(https://www.youtube.com/embed/DC9WpCc11hQ?wmode=transparent\)

[![YouTube Video](http://img.youtube.com/vi/DC9WpCc11hQ/0.jpg)](https://www.youtube.com/watch?v=DC9WpCc11hQ)

### License Agreement

This template is subject to the conditions of the [MuleSoft License Agreement](https://s3.amazonaws.com/templates-examples/AnypointTemplateLicense.pdf "MuleSoft License Agreement").

Review the terms of the license before downloading and using this template. You can use this template for free with the Mule Enterprise Edition, CloudHub, or as a trial in Anypoint Studio.

## Use Case

As a Salesforce admin I want to synchronize contacts between two Salesforce orgs.

This template serves as a foundation for the process of migrating contacts from one Salesforce instance to another, being able to specify filtering criteria and desired behavior when a contact already exists in the destination org. 

As implemented, this template leverages the Batch Module.

The batch job is divided into Input, Process, and On Complete stages.

- During the Input stage the template goes to the Salesforce Org A and queries all the existing contacts that match the filter criteria.
- During the Process stage, each Salesforce contact is filtered depending on if it has an existing matching contact in the Salesforce Org B and if the last updated date of the contact from Salesforce Org A is greater than the one in Salesforce Org B (in case that the same contact already exists).
- The last step of the Process stage groups the contacts and creates them in Salesforce Org B.
- Finally during the On Complete stage the template outputs statistics data into the console and sends a notification email with the results of the batch execution.

In any event the template can be configure to also move over the account to which the contact is related. The application can either, create the account if it doesn't exists, assign the contact to a pre-existing account in Salesforce instance B, or do nothing in regards to the account.

# Considerations

To make this template run, there are certain preconditions that must be considered. All of them deal with the preparations in both, that must be made for the template to run smoothly. Failing to do so can lead to unexpected behavior of the template.

## Salesforce Considerations

Here's what you need to know about Salesforce to get this template to work:

- Where can I check that the field configuration for my Salesforce instance is the right one? See: [Salesforce: Checking Field Accessibility for a Particular Field](https://help.salesforce.com/HTViewHelpDoc?id=checking_field_accessibility_for_a_particular_field.htm&language=en_US "Salesforce: Checking Field Accessibility for a Particular Field")
- Can I modify the Field Access Settings? How? See: [Salesforce: Modifying Field Access Settings](https://help.salesforce.com/HTViewHelpDoc?id=modifying_field_access_settings.htm&language=en_US "Salesforce: Modifying Field Access Settings")

### As a Data Source

If the user who configured the template for the source system does not have at least _read only_ permissions for the fields that are fetched, then an _InvalidFieldFault_ API fault displays.

```
java.lang.RuntimeException: [InvalidFieldFault 
[ApiQueryFault [ApiFault  exceptionCode='INVALID_FIELD'
exceptionMessage='Account.Phone, Account.Rating, 
Account.RecordTypeId, Account.ShippingCity
^
ERROR at Row:1:Column:486
No such column 'RecordTypeId' on entity 'Account'. If you are 
attempting to use a custom field, be sure to append the '__c' 
after the custom field name. Reference your WSDL or the describe 
call for the appropriate names.'
]
row='1'
column='486'
]
]
```

### As a Data Destination

There are no considerations with using Salesforce as a data destination.

# Run it!

Simple steps to get Salesforce to Salesforce Contact Migration running.

In any of the ways you would like to run this Anypoint Template, here is an example of the output you'll see after hitting the HTTP endpoint:

```
{
  "Message": "Batch Process initiated",
  "ID": "7fc674b0-e4b7-11e7-9627-100ba905a441",
  "RecordCount": 32,
  "StartExecutionOn": "2017-12-19T13:24:03Z"
}
```

## Running On Premises

In this section we help you run your template on your computer.

### Where to Download Anypoint Studio and the Mule Runtime

If you are a newcomer to Mule, here is where to get the tools.

- [Download Anypoint Studio](https://www.mulesoft.com/platform/studio)
- [Download Mule runtime](https://www.mulesoft.com/lp/dl/mule-esb-enterprise)

### Importing a Template into Studio

In Studio, click the Exchange X icon in the upper left of the taskbar, log in with your Anypoint Platform credentials, search for the template, and click **Open**.

### Running on Studio

After you import your template into Anypoint Studio, follow these steps to run it:

- Locate the properties file `mule.dev.properties`, in src/main/resources.
- Complete all the properties required as per the examples in the "Properties to Configure" section.
- Right click the template project folder.
- Hover your mouse over `Run as`.
- Click `Mule Application (configure)`.
- Inside the dialog, select Environment and set the variable `mule.env` to the value `dev`.
- Click `Run`.

### Running on Mule Standalone

Complete all properties in one of the property files, for example in mule.prod.properties and run your app with the corresponding environment variable. To follow the example, this is `mule.env=prod`. 

## Running on CloudHub

While creating your application on CloudHub (or you can do it later as a next step), go to Runtime Manager > Manage Application > Properties to set the environment variables listed in "Properties to Configure" as well as the **mule.env**.

Once your app is all set and started, supposing you choose as domain name `sfdccontactsync` to trigger the use case you just need to browse to `http://sfdccontactsync.cloudhub.io/synccontacts` and report will be sent to the emails configured.

### Properties to Configure

To use this template, configure properties (credentials, configurations, etc.) in the properties file or in CloudHub from Runtime Manager > Manage Application > Properties. The sections that follow list example values.

### Application Configuration

**Application configuration**

- http.port `9090`
- account.sync.policy `syncAccount`

**Note:** the property **account.sync.policy** can take any of the two following values:

- **empty\_value**: If the property has no value assigned to it then application will do nothing in what respect to the account and it'll just move the contact over.
- **syncAccount**: It tries to create a new contact's account if is not present in Salesforce instance B or assigns already any according to the Account's name.

**Salesforce Connector configuration for company A**

- sfdc.a.username `bob.dylan@orga`
- sfdc.a.password `DylanPassword123`
- sfdc.a.securityToken `avsfwCUl7apQs56Xq2AKi3X`

**Salesforce Connector configuration for company B**

- sfdc.b.username `joan.baez@orgb`
- sfdc.b.password `JoanBaez456`
- sfdc.b.securityToken `ces56arl7apQs56XTddf34X`

**SMTP Services configuration**

- smtp.host `smtp.gmail.com`
- smtp.port `587`
- smtp.user `gmailuser`
- smtp.password `gmailpassword`

**Email Details**

- mail.from `your.email@gmail.com`
- mail.to `your.email@gmail.com`
- mail.subject `Mail subject`

# API Calls

Salesforce imposes limits on the number of API calls that can be made. Therefore calculating this amount may be an important factor to consider. The template calls to the API can be calculated using the formula:

_**1 + X + X / 200**_

_**X**_ is the number of contacts to be synchronized on each run. 

Divide by _**200**_ because, by default, contacts are gathered in groups of 200 for each Upsert API Call in the commit step. Also consider that this calls are executed repeatedly every polling cycle.    

For instance if 10 records are fetched from origin instance, then 12 API calls are made (1 + 10 + 1).

# Customize It!

This brief guide intends to give a high level idea of how this template is built and how you can change it according to your needs.

As Mule applications are based on XML files, this page describes the XML files used with this template.

More files are available such as test classes and Mule application files, but to keep it simple, we focus on these XML files:

- config.xml
- businessLogic.xml
- endpoints.xml
- errorHandling.xml

## config.xml

Configuration for connectors and configuration properties are set in this file. Even change the configuration here, all parameters that can be modified are in properties file, which is the recommended place to make your changes. However if you want to do core changes to the logic, you need to modify this file.

In the Studio visual editor, the properties are on the _Global Element_ tab.

## businessLogic.xml

Functional aspects of the template are implemented in this XML, directed by the flow responsible for executing the logic.

For the purpose of this particular Anypoint Template the _mainFlow_ just executes the Batch Job which handles all the logic of it.

This flow has Error Handling that basically consists on invoking the _On Error Propagate Component_ defined in _errorHandling.xml_ file.

## endpoints.xml

This is the file where you find the inbound and outbound sides of your integration app.

This template has only an HTTP Listener as the way to trigger the use case.

**HTTP Inbound Endpoint** - Start Synchronization

+ `${http.port}` is set as a property to be defined either on a property file or in CloudHub environment variables.

+ The path configured by default is `synccontacts` and you are free to change for the one you prefer.

+ The host name for all endpoints in your CloudHub configuration should be defined as `localhost`. CloudHub routes requests from your application domain URL to the endpoint.

+ The endpoint is configured as a _request-response_ since as a result of calling it the response is the total of contacts migrated and filtered by the criteria specified.

## errorHandling.xml

This file handles how your integration reacts depending on the different exceptions.

This file provides error handling that is referenced by the main flow in the business logic.
