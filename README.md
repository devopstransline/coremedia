<!--
  On Update:
     * Change "message" for CMCC version to recent version.
     * Change "message" for TLC (Used) version to the recently used version.
-->

![CoreMedia Content Cloud Version](https://img.shields.io/static/v1?message=2107&label=CoreMedia%20Content%20Cloud&style=for-the-badge&color=672779)

# Translation via Transline Tblue

This workspace enables CoreMedia CMS to communicate with Transline Tblue
REST API in order to send content to be translated, query
the translation status and to update content with the received translation
result eventually.

## Feature Overview

This extension adds the following functionality to the CoreMedia Studio:
* Send content to Transline for translation into one or multiple languages
    with individual due dates in one or multiple workflows.
* Retrieve content from Transline once the translation is finished.
* Automatically detect cancellations of submissions at Transline and cancel the
    translation workflow in CoreMedia Studio.
* Configure the connection to Transline per site hierarchy.
* Show additional information like the translation status from Transline in
    CoreMedia Studio.
* Download XLIFF files and import log files in CoreMedia Studio if an error
    occurs during import.
* Editors in CoreMedia Studio are notified about completion, cancellation, and
    import and communication errors of a translation workflow with Transline.

**Detailed documentation available at
[docs/ folder](./docs/README.md).**

# ⑃ Branches &amp; Tags

* **master:**

    When development has finished on `develop` branch, changes will be merged to
    `master` branch.

* **develop:**

    Will contain preparations for next supported major.

* **Version Tags:**

    For adaptions to CoreMedia CMS major versions you will find corresponding
    tags named according to the CMS major version. It is recommended to
    take these tags as starting point from within your project,
    choosing the major version matching your project version.

## TLC Java REST Client Facades

* **[README: tlc-restclient-facade](apps/workflow-server/tlc-workflow-server-facade/tlc-restclient-facade/README.md)**

    Facade encapsulating all calls to TLC REST, the Java API as well as the REST
    backend.
    
* **[README: tlc-restclient-facade-default](apps/workflow-server/tlc-workflow-server-facade/tlc-restclient-facade-default/README.md)**

    This is the default and fallback facade used when nothing is defined in
    settings — or if there is no other facade applicable.

* **[README: tlc-restclient-facade-disabled](apps/workflow-server/tlc-workflow-server-facade/tlc-restclient-facade-disabled/README.md)**

    This facade mainly serves as example how to implement custom connection
    types. The implementation just throws exceptions on every interaction with the facade.
    
* **[README: tlc-restclient-facade-mock](apps/workflow-server/tlc-workflow-server-facade/tlc-restclient-facade-mock/README.md)**

    The mock facade will simulate a translation service in that way, that it
    replaces the target nodes (pre-filled with values from source nodes) in
    XLIFF with some other characters. 