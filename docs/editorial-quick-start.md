## Editorial Quick Start

Assuming that you are familiar with the CoreMedia Studio and that you have created a new campaign in the English master site that has now to be translated into French, German, and Spanish. This guide shows how this task can be accomplished using the Transline TBlue connector.

### Configure Connection to Transline TBlue

If the connection is not set up yet, go to `/Settings/Options/Settings/` create a _Settings_ content called _Transline_ and add it to the _Linked Settings_ property of the master site's homepage.

![TLC Settings](img/tlc-settings.png)

### Send content to Transline

Once finished working on the campaign content, open the Control Room and click the _Start a localization workflow_ button in the toolbar of _Localization workflows_.

![TLC Start Workflow](img/tlc-start-wf.png)

In the _Start Localization Workflow_ window, select the _Translation with Transline_ workflow type, set a self-describing name, a due date, drop the to-be-translated content, and set the target locales. The notes are included as instructions for the translators.

![TLC Select](img/tlc-select-type.png)

After having started the workflow, it is shown in the pending workflow section, and the details contain the submission id and the current state.

![TLC Running](img/tlc-running.png)

In case an error occurs, it is shown in your inbox. There you can select it you can select it to cancel the workflow, or you can try to fix the problem and retry.

![TLC Error Handling](img/tlc-connect-error.png)

After the translation is finished, you will receive a notification. The workflow is shown in the inbox and once accepting the task, you can review the content and finish it.
