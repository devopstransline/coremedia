# Translation via Transline TBlue

This open-source workspace enables CoreMedia CMS to communicate with Transline TBlue REST API in order to send contents to be translated, query the translation status and to update contents with the received translation result eventually.

## Feature Overview

This extension adds the following functionality to the CoreMedia Studio:
* Send content to Transline for translation into one or multiple languages with individual due dates in one or multiple workflows.
* Retrieve content from Transline once the translation is finished.
* Automatically detect cancellations of submissions to Transline and cancel the translation workflow in CoreMedia Studio.
* Configure the connection to Transline per site hierarchy.
* Show additional information like the translation status from Transline in CoreMedia Studio.
* Download XLIFF files and import log files in CoreMedia Studio if an error occurs during import.
* Editors in CoreMedia Studio are notified about completion, cancellation, as well as import and communication errors of a translation workflow with Transline.

## Table of Contents

1. [Editorial Quick Start](editorial-quick-start.md)

Use Transline Translation Workflow in CoreMedia Studio.

2. [Administration](administration.md)

How to administrate Transline extension (especially in CoreMedia Studio).

2. [Development](development.md)

How to integrate the extension to your Blueprint workspace.

3. [Release Steps](release/README.md)

How to release this workspace.
