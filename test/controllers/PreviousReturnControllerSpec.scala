/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers

import base.SpecBase
import connectors.VatReturnConnector
import connectors.corrections.CorrectionConnector
import connectors.financialdata.FinancialDataConnector
import models.{Country, StandardPeriod}
import models.Quarter.{Q1, Q3}
import models.corrections.CorrectionPayload
import models.domain.VatReturn
import models.external.ExternalEntryUrl
import models.financialdata.Charge
import models.responses.{UnexpectedResponseStatus, NotFound => NotFoundResponse}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.{CountryOfConsumptionFromEuPage, CountryOfSaleFromEuPage}
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.VatReturnSalesService
import viewmodels.govuk.summarylist._
import viewmodels.previousReturn.{PreviousReturnSummary, SaleAtVatRateSummary}
import viewmodels.previousReturn.corrections.CorrectionSummary
import views.html.PreviousReturnView

import scala.concurrent.Future

class PreviousReturnControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val vatReturnConnector = mock[VatReturnConnector]
  private val vatReturnSalesService = mock[VatReturnSalesService]
  private val vatReturnsPaymentConnector = mock[FinancialDataConnector]
  private val correctionConnector = mock[CorrectionConnector]

  override def beforeEach(): Unit = {
    Mockito.reset(vatReturnConnector)
    Mockito.reset(vatReturnSalesService)
    Mockito.reset(vatReturnsPaymentConnector)
    Mockito.reset(correctionConnector)
    super.beforeEach()
  }

  private lazy val previousReturnRoute = routes.PreviousReturnController.onPageLoad(period).url

  private val countryFrom = arbitrary[Country].sample.value
  private val countryTo = arbitrary[Country].sample.value

  private val vatReturn = arbitrary[VatReturn].sample.value
  private val correctionPayload = arbitrary[CorrectionPayload].sample.value
  private val year = 2015

  private val baseAnswers =
    emptyUserAnswers
      .set(CountryOfSaleFromEuPage(index), countryFrom).success.value
      .set(CountryOfConsumptionFromEuPage(index, index), countryTo).success.value

  "Previous Return Controller" - {

    "must redirect to NoLongerAbleToViewReturnController when the return period is older than six years" in {

      val period = StandardPeriod(year, Q1)

      val application = applicationBuilder(Some(baseAnswers))
        .overrides(
          bind[VatReturnConnector].toInstance(vatReturnConnector),
          bind[VatReturnSalesService].toInstance(vatReturnSalesService),
          bind[FinancialDataConnector].toInstance(vatReturnsPaymentConnector),
          bind[CorrectionConnector].toInstance(correctionConnector)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.PreviousReturnController.onPageLoad(period).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.NoLongerAbleToViewReturnController.onPageLoad().url
      }
    }

    "must return OK and the correct view for a GET with no banner" in {

      val application = applicationBuilder(Some(baseAnswers))
        .overrides(
          bind[VatReturnConnector].toInstance(vatReturnConnector),
          bind[VatReturnSalesService].toInstance(vatReturnSalesService),
          bind[FinancialDataConnector].toInstance(vatReturnsPaymentConnector),
          bind[CorrectionConnector].toInstance(correctionConnector),
        )
        .build()

      val netSalesFromNi = BigDecimal(4141)
      val netSalesFromEu = BigDecimal(2333)
      val vatOnSalesFromNi = BigDecimal(55)
      val vatOnSalesFromEu = BigDecimal(44)
      val correctionAmount = BigDecimal(25)
      val totalVatOnSalesBeforeCorrection = vatOnSalesFromNi + vatOnSalesFromEu
      val totalVatOnSalesAfterCorrection = totalVatOnSalesBeforeCorrection + correctionAmount

      val clearedAmount = BigDecimal(3333.33)
      val outstandingAmount = BigDecimal(2247.22)

      val charge = Charge(StandardPeriod(2021, Q3), BigDecimal(7777.77), outstandingAmount, clearedAmount)

      when(vatReturnConnector.get(any())(any())) thenReturn Future.successful(Right(vatReturn))
      when(vatReturnsPaymentConnector.getCharge(any())(any())) thenReturn Future.successful(Right(Some(charge)))
      when(vatReturnSalesService.getTotalNetSalesToCountry(any())) thenReturn netSalesFromNi
      when(vatReturnSalesService.getEuTotalNetSales(any())) thenReturn netSalesFromEu
      when(vatReturnSalesService.getTotalVatOnSalesToCountry(any())) thenReturn vatOnSalesFromNi
      when(vatReturnSalesService.getEuTotalVatOnSales(any())) thenReturn vatOnSalesFromEu
      when(correctionConnector.get(any())(any())) thenReturn Future.successful(Right(correctionPayload))
      when(vatReturnSalesService.getTotalVatOnSalesBeforeCorrection(any())) thenReturn totalVatOnSalesBeforeCorrection
      when(vatReturnSalesService.getTotalVatOnSalesAfterCorrection(any(), eqTo(Some(correctionPayload)))) thenReturn totalVatOnSalesAfterCorrection
      when(vatReturnConnector.getSavedExternalEntry()(any())) thenReturn Future.successful(Right(ExternalEntryUrl(None)))

      running(application) {
        val request = FakeRequest(GET, previousReturnRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[PreviousReturnView]
        implicit val msgs: Messages = messages(application)
        val summaryList = SummaryListViewModel(
          rows = PreviousReturnSummary.mainListRows(vatReturn, totalVatOnSalesAfterCorrection, Some(outstandingAmount)))
        val niSalesList = SaleAtVatRateSummary.getAllNiSales(vatReturn)
        val euSalesList = SaleAtVatRateSummary.getAllEuSales(vatReturn)
        val totalVatSummaryList = SummaryListViewModel(
          rows = PreviousReturnSummary.totalVatSummaryRows(totalVatOnSalesAfterCorrection, hasCorrections = true))
        val displayPayNow = true

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          vatReturn = vatReturn,
          mainList = summaryList,
          niSalesList = niSalesList,
          euSalesList = euSalesList,
          correctionsForPeriodList = CorrectionSummary.getCorrectionPeriods(Some(correctionPayload)),
          declaredVatAfterCorrections = CorrectionSummary.getDeclaredVat(Some(correctionPayload), vatReturn),
          totalVatList = Some(totalVatSummaryList),
          displayPayNow = displayPayNow,
          vatOwedInPence = (charge.outstandingAmount * 100).toLong,
          displayBanner = false
        )(request, implicitly).toString
      }
    }

    "must return OK and the correct view for a GET with banner when charge is empty but expected" in {

      val application = applicationBuilder(Some(baseAnswers))
        .overrides(
          bind[VatReturnConnector].toInstance(vatReturnConnector),
          bind[VatReturnSalesService].toInstance(vatReturnSalesService),
          bind[FinancialDataConnector].toInstance(vatReturnsPaymentConnector),
          bind[CorrectionConnector].toInstance(correctionConnector)
        )
        .build()

      val netSalesFromNi = BigDecimal(4141)
      val netSalesFromEu = BigDecimal(2333)
      val vatOnSalesFromNi = BigDecimal(55)
      val vatOnSalesFromEu = BigDecimal(44)
      val totalVatOnSales = vatOnSalesFromNi + vatOnSalesFromEu

      when(vatReturnConnector.get(any())(any())) thenReturn Future.successful(Right(vatReturn))
      when(vatReturnsPaymentConnector.getCharge(any())(any())) thenReturn Future.successful(Right(None))
      when(vatReturnSalesService.getTotalNetSalesToCountry(any())) thenReturn netSalesFromNi
      when(vatReturnSalesService.getEuTotalNetSales(any())) thenReturn netSalesFromEu
      when(vatReturnSalesService.getTotalVatOnSalesToCountry(any())) thenReturn vatOnSalesFromNi
      when(vatReturnSalesService.getEuTotalVatOnSales(any())) thenReturn vatOnSalesFromEu
      when(vatReturnSalesService.getTotalVatOnSalesBeforeCorrection(any())) thenReturn totalVatOnSales
      when(vatReturnSalesService.getTotalVatOnSalesAfterCorrection(any(), eqTo(None))) thenReturn totalVatOnSales
      when(correctionConnector.get(any())(any())) thenReturn Future.successful(Left(NotFoundResponse))
      when(vatReturnConnector.getSavedExternalEntry()(any())) thenReturn Future.successful(Right(ExternalEntryUrl(None)))

      running(application) {
        val request = FakeRequest(GET, previousReturnRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[PreviousReturnView]
        implicit val msgs: Messages = messages(application)
        val summaryList = SummaryListViewModel(
          rows = PreviousReturnSummary.mainListRows(vatReturn, totalVatOnSales, None))
        val niSalesList = SaleAtVatRateSummary.getAllNiSales(vatReturn)
        val euSalesList = SaleAtVatRateSummary.getAllEuSales(vatReturn)
        val declaredVatAfterCorrections = CorrectionSummary.getDeclaredVat(None, vatReturn)
        val totalVatList = SummaryListViewModel(rows = PreviousReturnSummary.totalVatSummaryRows(totalVatOnSales, hasCorrections = false))
        val displayPayNow = true

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          vatReturn = vatReturn,
          mainList = summaryList,
          niSalesList = niSalesList,
          euSalesList = euSalesList,
          correctionsForPeriodList = None,
          declaredVatAfterCorrections = declaredVatAfterCorrections,
          totalVatList = Some(totalVatList),
          displayPayNow = displayPayNow,
          vatOwedInPence = (totalVatOnSales * 100).toLong,
          displayBanner = true
        )(request, implicitly).toString
      }
    }

    "must return OK and the correct view for a GET without banner for nil return" in {

      val application = applicationBuilder(Some(baseAnswers))
        .overrides(
          bind[VatReturnConnector].toInstance(vatReturnConnector),
          bind[VatReturnSalesService].toInstance(vatReturnSalesService),
          bind[FinancialDataConnector].toInstance(vatReturnsPaymentConnector),
          bind[CorrectionConnector].toInstance(correctionConnector)
        )
        .build()

      val zero = BigDecimal(0)
      when(vatReturnConnector.get(any())(any())) thenReturn Future.successful(Right(vatReturn))
      when(vatReturnsPaymentConnector.getCharge(any())(any())) thenReturn Future.successful(Right(None))
      when(vatReturnSalesService.getTotalNetSalesToCountry(any())) thenReturn zero
      when(vatReturnSalesService.getEuTotalNetSales(any())) thenReturn zero
      when(vatReturnSalesService.getTotalVatOnSalesToCountry(any())) thenReturn zero
      when(vatReturnSalesService.getEuTotalVatOnSales(any())) thenReturn zero
      when(vatReturnSalesService.getTotalVatOnSalesBeforeCorrection(any())) thenReturn zero
      when(vatReturnSalesService.getTotalVatOnSalesAfterCorrection(any(), eqTo(None))) thenReturn zero
      when(correctionConnector.get(any())(any())) thenReturn Future.successful(Left(NotFoundResponse))
      when(vatReturnConnector.getSavedExternalEntry()(any())) thenReturn Future.successful(Right(ExternalEntryUrl(None)))

      running(application) {
        val request = FakeRequest(GET, previousReturnRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[PreviousReturnView]
        implicit val msgs: Messages = messages(application)
        val summaryList = SummaryListViewModel(
          rows = PreviousReturnSummary.mainListRows(vatReturn, zero, None))
        val niSalesList = SaleAtVatRateSummary.getAllNiSales(vatReturn)
        val euSalesList = SaleAtVatRateSummary.getAllEuSales(vatReturn)
        val declaredVatAfterCorrections = CorrectionSummary.getDeclaredVat(None, vatReturn)
        val displayPayNow = false
        val totalVatList = SummaryListViewModel(rows = PreviousReturnSummary.totalVatSummaryRows(zero, hasCorrections = false))

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          vatReturn = vatReturn,
          mainList = summaryList,
          niSalesList = niSalesList,
          euSalesList = euSalesList,
          correctionsForPeriodList = None,
          declaredVatAfterCorrections = declaredVatAfterCorrections,
          totalVatList = Some(totalVatList),
          displayPayNow = displayPayNow,
          vatOwedInPence = zero.toLong,
          displayBanner = false
        )(request, implicitly).toString
      }
    }

    "must return OK and view without charge elements when unsuccessful ChargeResponse" in {

      val application = applicationBuilder(Some(baseAnswers))
        .overrides(
          bind[VatReturnConnector].toInstance(vatReturnConnector),
          bind[VatReturnSalesService].toInstance(vatReturnSalesService),
          bind[FinancialDataConnector].toInstance(vatReturnsPaymentConnector),
          bind[CorrectionConnector].toInstance(correctionConnector)
        )
        .build()

      val netSalesFromNi = BigDecimal(4141)
      val netSalesFromEu = BigDecimal(2333)
      val vatOnSalesFromNi = BigDecimal(55)
      val vatOnSalesFromEu = BigDecimal(44)
      val totalVatOnSales = vatOnSalesFromNi + vatOnSalesFromEu

      when(vatReturnConnector.get(any())(any())) thenReturn Future.successful(Right(vatReturn))
      when(vatReturnsPaymentConnector.getCharge(any())(any())) thenReturn Future.successful(Left(NotFoundResponse))
      when(vatReturnSalesService.getTotalNetSalesToCountry(any())) thenReturn netSalesFromNi
      when(vatReturnSalesService.getEuTotalNetSales(any())) thenReturn netSalesFromEu
      when(vatReturnSalesService.getTotalVatOnSalesToCountry(any())) thenReturn vatOnSalesFromNi
      when(vatReturnSalesService.getEuTotalVatOnSales(any())) thenReturn vatOnSalesFromEu
      when(vatReturnSalesService.getTotalVatOnSalesBeforeCorrection(any())) thenReturn totalVatOnSales
      when(vatReturnSalesService.getTotalVatOnSalesAfterCorrection(any(), eqTo(None))) thenReturn totalVatOnSales
      when(correctionConnector.get(any())(any())) thenReturn Future.successful(Left(NotFoundResponse))
      when(vatReturnConnector.getSavedExternalEntry()(any())) thenReturn Future.successful(Right(ExternalEntryUrl(None)))

      running(application) {
        val request = FakeRequest(GET, previousReturnRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[PreviousReturnView]
        implicit val msgs: Messages = messages(application)
        val summaryList = SummaryListViewModel(
          rows = PreviousReturnSummary.mainListRows(vatReturn, totalVatOnSales, None))
        val niSalesList = SaleAtVatRateSummary.getAllNiSales(vatReturn)
        val euSalesList = SaleAtVatRateSummary.getAllEuSales(vatReturn)
        val declaredVatAfterCorrections = CorrectionSummary.getDeclaredVat(None, vatReturn)
        val displayPayNow = true
        val totalVatList = SummaryListViewModel(rows = PreviousReturnSummary.totalVatSummaryRows(totalVatOnSales, hasCorrections = false))

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          vatReturn = vatReturn,
          mainList = summaryList,
          niSalesList = niSalesList,
          euSalesList = euSalesList,
          correctionsForPeriodList = None,
          declaredVatAfterCorrections = declaredVatAfterCorrections,
          totalVatList = Some(totalVatList),
          displayPayNow = displayPayNow,
          vatOwedInPence = (totalVatOnSales * 100).toLong,
          displayBanner = true
        )(request, implicitly).toString
      }
    }

    "must return OK and correct view with nil return and a correction" in {

      val application = applicationBuilder(Some(baseAnswers))
        .overrides(
          bind[VatReturnConnector].toInstance(vatReturnConnector),
          bind[VatReturnSalesService].toInstance(vatReturnSalesService),
          bind[FinancialDataConnector].toInstance(vatReturnsPaymentConnector),
          bind[CorrectionConnector].toInstance(correctionConnector)
        )
        .build()

      val zero = BigDecimal(0)
      val correctionAmount = BigDecimal(100)

      when(vatReturnConnector.get(any())(any())) thenReturn Future.successful(Right(vatReturn))
      when(vatReturnsPaymentConnector.getCharge(any())(any())) thenReturn Future.successful(Right(None))
      when(vatReturnSalesService.getTotalNetSalesToCountry(any())) thenReturn zero
      when(vatReturnSalesService.getEuTotalNetSales(any())) thenReturn zero
      when(vatReturnSalesService.getTotalVatOnSalesToCountry(any())) thenReturn zero
      when(vatReturnSalesService.getEuTotalVatOnSales(any())) thenReturn zero
      when(vatReturnSalesService.getTotalVatOnSalesBeforeCorrection(any())) thenReturn zero
      when(vatReturnSalesService.getTotalVatOnSalesAfterCorrection(any(), eqTo(Some(correctionPayload)))) thenReturn correctionAmount
      when(correctionConnector.get(any())(any())) thenReturn Future.successful(Right(correctionPayload))
      when(vatReturnConnector.getSavedExternalEntry()(any())) thenReturn Future.successful(Right(ExternalEntryUrl(None)))

      running(application) {
        val request = FakeRequest(GET, previousReturnRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[PreviousReturnView]
        implicit val msgs: Messages = messages(application)
        val summaryList = SummaryListViewModel(
          rows = PreviousReturnSummary.mainListRows(vatReturn, correctionAmount, None))
        val niSalesList = SaleAtVatRateSummary.getAllNiSales(vatReturn)
        val euSalesList = SaleAtVatRateSummary.getAllEuSales(vatReturn)
        val correctionsForPeriodList = CorrectionSummary.getCorrectionPeriods(Some(correctionPayload))
        val declaredVatAfterCorrections = CorrectionSummary.getDeclaredVat(Some(correctionPayload), vatReturn)
        val totalVatSummaryList = SummaryListViewModel(
          rows = PreviousReturnSummary.totalVatSummaryRows(correctionAmount, hasCorrections = true))
        val displayPayNow = true

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          vatReturn = vatReturn,
          mainList = summaryList,
          niSalesList = niSalesList,
          euSalesList = euSalesList,
          correctionsForPeriodList = correctionsForPeriodList,
          declaredVatAfterCorrections = declaredVatAfterCorrections,
          totalVatList = Some(totalVatSummaryList),
          displayPayNow = displayPayNow,
          vatOwedInPence = (correctionAmount * 100).toLong,
          displayBanner = true
        )(request, implicitly).toString
      }
    }

    "must redirect to Your Account for a GET if vatReturnResult NotFound" in {

      val application = applicationBuilder(Some(emptyUserAnswers))
        .overrides(
          bind[VatReturnConnector].toInstance(vatReturnConnector),
          bind[VatReturnSalesService].toInstance(vatReturnSalesService),
          bind[FinancialDataConnector].toInstance(vatReturnsPaymentConnector),
          bind[CorrectionConnector].toInstance(correctionConnector)
        )
        .build()

      val correctionAmount = BigDecimal(100)

      when(vatReturnConnector.get(any())(any())) thenReturn Future.successful(Left(NotFoundResponse))
      when(vatReturnsPaymentConnector.getCharge(any())(any())) thenReturn Future.successful(Right(None))
      when(vatReturnSalesService.getTotalVatOnSalesAfterCorrection(any(), eqTo(Some(correctionPayload)))) thenReturn correctionAmount
      when(correctionConnector.get(any())(any())) thenReturn Future.successful(Right(correctionPayload))
      when(vatReturnConnector.getSavedExternalEntry()(any())) thenReturn Future.successful(Right(ExternalEntryUrl(None)))

      running(application) {
        val request = FakeRequest(GET, previousReturnRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.YourAccountController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery when an unexpected error received from vat return" in {

      val application = applicationBuilder(Some(emptyUserAnswers))
        .overrides(
          bind[VatReturnConnector].toInstance(vatReturnConnector),
          bind[VatReturnSalesService].toInstance(vatReturnSalesService),
          bind[FinancialDataConnector].toInstance(vatReturnsPaymentConnector),
          bind[CorrectionConnector].toInstance(correctionConnector)
        )
        .build()

      val correctionAmount = BigDecimal(100)

      when(vatReturnConnector.get(any())(any())) thenReturn Future.successful(Left(UnexpectedResponseStatus(INTERNAL_SERVER_ERROR, "foo")))
      when(vatReturnsPaymentConnector.getCharge(any())(any())) thenReturn Future.successful(Right(None))
      when(vatReturnSalesService.getTotalVatOnSalesAfterCorrection(any(), eqTo(Some(correctionPayload)))) thenReturn correctionAmount
      when(correctionConnector.get(any())(any())) thenReturn Future.successful(Right(correctionPayload))
      when(vatReturnConnector.getSavedExternalEntry()(any())) thenReturn Future.successful(Right(ExternalEntryUrl(None)))

      running(application) {
        val request = FakeRequest(GET, previousReturnRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery and throw an exception when an unexpected result is returned" in {

      val application = applicationBuilder(Some(emptyUserAnswers))
        .overrides(
          bind[VatReturnConnector].toInstance(vatReturnConnector),
          bind[VatReturnSalesService].toInstance(vatReturnSalesService),
          bind[FinancialDataConnector].toInstance(vatReturnsPaymentConnector),
          bind[CorrectionConnector].toInstance(correctionConnector)
        )
        .build()

      val correctionAmount = BigDecimal(100)

      when(vatReturnConnector.get(any())(any())) thenReturn Future.failed(new Exception("Some exception"))
      when(vatReturnsPaymentConnector.getCharge(any())(any())) thenReturn Future.successful(Right(None))
      when(vatReturnSalesService.getTotalVatOnSalesAfterCorrection(any(), eqTo(Some(correctionPayload)))) thenReturn correctionAmount
      when(correctionConnector.get(any())(any())) thenReturn Future.successful(Right(correctionPayload))
      when(vatReturnConnector.getSavedExternalEntry()(any())) thenReturn Future.successful(Right(ExternalEntryUrl(None)))

      running(application) {
        val request = FakeRequest(GET, previousReturnRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must return OK and the correct view for a GET with no banner and add the external backToYourAccount url that has been saved" in {

      val application = applicationBuilder(Some(baseAnswers))
        .overrides(
          bind[VatReturnConnector].toInstance(vatReturnConnector),
          bind[VatReturnSalesService].toInstance(vatReturnSalesService),
          bind[FinancialDataConnector].toInstance(vatReturnsPaymentConnector),
          bind[CorrectionConnector].toInstance(correctionConnector)
        )
        .build()

      val netSalesFromNi = BigDecimal(4141)
      val netSalesFromEu = BigDecimal(2333)
      val vatOnSalesFromNi = BigDecimal(55)
      val vatOnSalesFromEu = BigDecimal(44)
      val correctionAmount = BigDecimal(25)
      val totalVatOnSalesBeforeCorrection = vatOnSalesFromNi + vatOnSalesFromEu
      val totalVatOnSalesAfterCorrection = totalVatOnSalesBeforeCorrection + correctionAmount

      val clearedAmount = BigDecimal(3333.33)
      val outstandingAmount = BigDecimal(2247.22)

      val charge = Charge(StandardPeriod(2021, Q3), BigDecimal(7777.77), outstandingAmount, clearedAmount)

      when(vatReturnConnector.get(any())(any())) thenReturn Future.successful(Right(vatReturn))
      when(vatReturnsPaymentConnector.getCharge(any())(any())) thenReturn Future.successful(Right(Some(charge)))
      when(vatReturnSalesService.getTotalNetSalesToCountry(any())) thenReturn netSalesFromNi
      when(vatReturnSalesService.getEuTotalNetSales(any())) thenReturn netSalesFromEu
      when(vatReturnSalesService.getTotalVatOnSalesToCountry(any())) thenReturn vatOnSalesFromNi
      when(vatReturnSalesService.getEuTotalVatOnSales(any())) thenReturn vatOnSalesFromEu
      when(correctionConnector.get(any())(any())) thenReturn Future.successful(Right(correctionPayload))
      when(vatReturnSalesService.getTotalVatOnSalesBeforeCorrection(any())) thenReturn totalVatOnSalesBeforeCorrection
      when(vatReturnSalesService.getTotalVatOnSalesAfterCorrection(any(), eqTo(Some(correctionPayload)))) thenReturn totalVatOnSalesAfterCorrection
      when(vatReturnConnector.getSavedExternalEntry()(any())) thenReturn Future.successful(Right(ExternalEntryUrl(Some("example"))))

      running(application) {
        val request = FakeRequest(GET, previousReturnRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[PreviousReturnView]
        implicit val msgs: Messages = messages(application)
        val summaryList = SummaryListViewModel(
          rows = PreviousReturnSummary.mainListRows(vatReturn, totalVatOnSalesAfterCorrection, Some(outstandingAmount)))
        val niSalesList = SaleAtVatRateSummary.getAllNiSales(vatReturn)
        val euSalesList = SaleAtVatRateSummary.getAllEuSales(vatReturn)
        val totalVatSummaryList = SummaryListViewModel(
          rows = PreviousReturnSummary.totalVatSummaryRows(totalVatOnSalesAfterCorrection, hasCorrections = true))
        val displayPayNow = true

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          vatReturn = vatReturn,
          mainList = summaryList,
          niSalesList = niSalesList,
          euSalesList = euSalesList,
          correctionsForPeriodList = CorrectionSummary.getCorrectionPeriods(Some(correctionPayload)),
          declaredVatAfterCorrections = CorrectionSummary.getDeclaredVat(Some(correctionPayload), vatReturn),
          totalVatList = Some(totalVatSummaryList),
          displayPayNow = displayPayNow,
          vatOwedInPence = (charge.outstandingAmount * 100).toLong,
          displayBanner = false,
          Some("example")
        )(request, implicitly).toString
      }
    }
  }
}
