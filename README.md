
# one-stop-shop-returns-frontend

This is the repository for One Stop Shop Returns Frontend

Backend: https://github.com/hmrc/one-stop-shop-returns

Stub: https://github.com/hmrc/one-stop-shop-returns-stub

One Stop Shop Returns Service
------------

The main function of this service is to allow traders who are registered with the One Stop Shop Registration
service to submit a One Stop Shop VAT return and pay VAT.

The service provides a main dashboard where traders can see which returns are due, how much they owe and view
previously submitted returns. 

In addition to this, depending on the status of their account and returns, there are links to amend registration 
(redirects to one-stop-shop-registration-frontend), rejoin registration (redirects to 
one-stop-shop-registration-frontend and to leave the service (redirects to one-stop-shop-exclusions-frontend).
If a trader has been excluded from the service, there will also be messaging around this above the tiles on this page.

Summary of APIs
------------

This service utilises various APIs from other platforms in order to obtain and store information required for the
returns process.

ETMP:
- HMRC VAT registration details are retrieved via one-stop-shop-registration
- One Stop Shop Registration details are obtained including any exclusions via one-stop-shop-registration
- Previously submitted returns, outstanding returns (known as obligations) and corrections data is retrieved

Core:
- Submitted returns are sent to Core for any further EU processing

Note: locally (and on staging) these connections are stubbed via one-stop-shop-returns-stub.

Requirements
------------

This service is written in [Scala](http://www.scala-lang.org/) and [Play](http://playframework.com/), so needs at least a [JRE] to run.

## Run the application locally via Service Manager

```
sm2 --start ONE_STOP_SHOP_ALL -r
```

### To run the application locally from the repository, execute the following:
```
sm2 --stop ONE_STOP_SHOP_RETURNS_FRONTEND
```
and 
```
sbt run -Dapplication.router=testOnlyDoNotUseInAppConf.Routes
```

### Running correct version of mongo
Mongo 6 with a replica set is required to run the service. Please refer to the MDTP Handbook for instructions on how to run this


### Using the application

Access the Authority Wizard to log in:
http://localhost:9949/auth-login-stub/gg-sign-in

Enter the following details on this page and submit:
- Redirect URL: http://localhost:10204/pay-vat-on-goods-sold-to-eu/northern-ireland-returns-payments
- Affinity Group: Organisation
- Enrolments (there are two rows this time):
- Enrolment Key: HMRC-MTD-VAT
- Identifier Name: VRN
- Identifier Value: 100000002
- Enrolment Key: HMRC-OSS-ORG
- Identifier Name: VRN
- Identifier Value: 100000002

It is recommended to use VRN 100000002 for a regular returns journey, however alternatives can be found in the
one-stop-shop-registration-stub and one-stop-shop-returns-stub, which hold scenarios for registered traders and
returns/corrections that will be available to submit.

Unit and Integration Tests
------------

To run the unit and integration tests, you will need to open an sbt session on the browser.

### Unit Tests

To run all tests, run the following command in your sbt session:
```
test
```

To run a single test, run the following command in your sbt session:
```
testOnly <package>.<SpecName>
```

An asterisk can be used as a wildcard character without having to enter the package, as per the example below:
```
testOnly *CheckSalesFromNiControllerSpec
```

### Integration Tests

To run all tests, run the following command in your sbt session:
```
it:test
```

To run a single test, run the following command in your sbt session:
```
it:testOnly <package>.<SpecName>
```

An asterisk can be used as a wildcard character without having to enter the package, as per the example below:
```
it:testOnly *RegistrationRepositorySpec
```

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
