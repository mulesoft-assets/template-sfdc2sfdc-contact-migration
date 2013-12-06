## Use Case
As a Salesforce admin I want to syncronize contacts between two Salesfoce orgs.

This Kick (template) should serve as a foundation for the process of migrating contacts from one Salesfoce instance to another, being able to specify filtering criterias and desired behaviour when a contact already exists in the destination org. 

As implemented, for each one of the contacts from one instance of Salesforce, determines if it meets the requirements to be synced and if so, checks if the contact already exists syncing only if the record from the target instance is older. 

## Run it!

Simple steps to get SFDC to SFDC Contacts Sync running [here] (https://github.com/mulesoft/sfdc2sfdc-contactsync/wiki/Run-SFDC2SFDC-ContactSync-Mule-Kick!).

## Details

Having an idea on how this Kick is built and how can you customise it can be found [here] (https://github.com/mulesoft/sfdc2sfdc-contactsync/wiki/How-this-kick-was-built.-Customise-it!).
