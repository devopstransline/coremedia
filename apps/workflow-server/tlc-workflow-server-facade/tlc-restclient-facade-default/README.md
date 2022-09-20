# Default Facade for TLC REST Java library

This is the default and fallback facade used when nothing is defined in
settings &mdash; or if there is no other facade applicable.

The default facade connects to a real TLC REST Backend via TLC REST Java API.

This module comes with a so called _contract-test_ which is recommended to run
at least with every TLC REST Java API Update. It requires an available TLC
REST Backend to connect to. It will test, if the TLC REST Java API as well as the
TLC REST Backend still fulfill the requirements of this facade.

If the contract is not fulfilled, you typically adapt the facade's implementation.

## Running Contract Tests
 
 In order to run the contract tests you need:
 
 * An Transline API key
 * And a file `.tlc.properties` in your user home which contains these
     credentials and will look like this:
     
     ```properties
     url=https://tblue.de
     key=0e...abc
     fileType=xliff
     ```
     
 The contract tests then run as normal unit tests. These tests are ignored
 when the properties are not available or not readable.
 
