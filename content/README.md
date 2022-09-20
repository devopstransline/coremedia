# TLC Test Data

This module contains test-data for the TLC extension.

## /Settings/Options/Settings/Transline

This settings document will tell the TLC extension which credentials
and endpoints to use. By default it contains some dummy values and
chooses "mock" as the default implementation for TLC interactions.
As a result any translations will be forwarded to the Mock TLC RestClient
Facade which just some character replacements to simulate a translation.

Note, that in order to activate Transline, it is required, that you link
this settings document to the root documents (also known as _Homepage_) of
those master sites which shall use Transline for translation.

The Settings Struct for Transline contains the following entries:

* **`transline`**
    * **`url`:** for example `http://yourhostname:9095/api/v2`
    * **`key`:** connector key provided by Transline
    * **`fileType`:** _(optional)_ the name of the file type to use when uploading XLIFF. Defaults to first entry of supported file types as returned by TLC connector config. If set, it must be one of the file types from the connector config.
    * **`type`:** _(optional)_ the type of your connection; Available types:
        * `default`: _(default and fallback)_ the standard TLC connection which
            requires a TLC backend; also used for any unknown types
        * `disabled`: will cause all interaction with TLC to end with an
            exception which signals: _TLC Service disabled._ It may be used
            for TLC maintenance slots where you want to prevent connecting
            to TLC.
        * `mock`: Uses some simple mocked translation approach for translation.
            You will see some virtual latency before translations proceed and
            at the end your translated content will be the master content with
            some characters being replaced. This type is especially useful for
            local development when you have no TLC sandbox at hand.   
