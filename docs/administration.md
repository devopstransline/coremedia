# Administration

## Table of Content

1. [Prerequisites](#prerequisites)
2. [Retrieving Client Secret Key](#retrieve-client-secret-key)
3. [Setting up Transline Connector](#setup-of-transline-connector)
4. [Configuring Transline Connection Settings](#configuring-transline-connection-settings)
5. [Questions &amp; Answers](#questions-amp-answers)
6. [See Also](#see-also)

## Prerequisites

Ensure you have your TBlue parameters at hand:

* TBlue REST Base URL
* API key
* File type

For more details on available options and how to configure them in CoreMedia Studio read [this](#configuring-transline-connection-settings).

## Configuring Transline Connection Settings

After you have created your Transline settings at `/Settings/Options/Settings/Transline` and linked them to your site, you need to configure your personal Transline parameters. There can be more similar setting content items with different names and values to enable site-specific connections. However, global settings like `dayOffsetForDueDate` have to be defined in `/Settings/Options/Settings/Transline`.

Therefore, you need to add a struct to the Transline settings, named `transline`. ** To prevent sensitive information from leaking, make sure to restrict the read and write rights for this content to those user groups that actually need access.**

Within that struct the following parameters must/can be specified:

* `url` for TBlue REST Base URL  (type:`String`)
* `key` The TBlue API key (type:`String`)
* `fileType` If there is more than one file format in your Transline setup, then this has to be set to the XLIFF file type identifier to be used by your connector. (_optional_, default: `xliff`, type:`String`)
* `type` Determines which facade implementation will be used (see [Facade Documentation](../apps/workflow-server/tlc-workflow-server-facade/tlc-restclient-facade/README.md)). (_optional_, type:`String`)
* `dayOffsetForDueDate` Defines the offset for the `Due Date` of the workflow "Translation with Transline" in the Start Workflow Window to lie within the future in days.
  (_optional_, default: `0`, type:`Integer`, scope:**global**)
* `retryCommunicationErrors` Number of retries in case of a communication error with Transline. (_optional_, default: `5`, type:`Integer`)
* `isSendSubmitter` Defines whether the name of the editor that started the workflow is send to Transline as part of the submission.
  (_optional_, default: `false`, type:`Boolean`)

The following parameters of the struct should be handled carefully and only after consulting Translations.com. They affect all new and running workflows, and as such they can instantly cause high loads on Transline's servers or lead to unexpectedly long update intervals. Intervals shorter than 60 seconds or longer than a day are not allowed and will fall back to the corresponding max or min values. The interval is also limited to be not longer than one day. In case the interval is set to a very large, there is no possibility for correction, and you would have to wait until it is expired.

* `sendTranslationRequestRetryDelay` Overrides the default interval (secs) between retries if the XLIFF could not be sent on first attempt.
  (_optional_, default: `180`, type:`Integer`)
* `downloadTranslationRetryDelay` Overrides the update interval (secs) of the submission's state and the translated XLIFF(s)is/ are not immediately ready.
  (_optional_, default: `1800`, type:`Integer`)
* `cancelTranslationRetryDelay` Overrides the interval (secs) for retrying the cancellation of a submission. (_optional_, default: `180`, type:`Integer`)

The defaults for these values are defined globally in the properties file of the `tlc-workflow-server` module. Other parameters can be defined here too.
You can also define parameters for testing with the mock facade.


## Questions &amp; Answers

### What to do when deriving a new site?

**Short:** _Enable Target Language at TLC, Configure Language Mapping_

When you derive a new site and want to propagate translations to this site via the Transline Translation Workflow, you need to ensure that your target locale is supported by Transline.

You will find the language tags in `/Settings/Options/Settings/LocaleSettings` in your CMS.

