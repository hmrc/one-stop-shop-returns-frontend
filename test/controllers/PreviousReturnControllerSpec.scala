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
import models.Period.getPeriod
import models.Quarter.{Q1, Q3}
import models.corrections.CorrectionPayload
import models.domain.VatReturn
import models.etmp.{EtmpVatReturn, EtmpVatReturnCorrection}
import models.exclusions.{ExcludedTrader, ExclusionReason}
import models.external.ExternalEntryUrl
import models.financialdata.Charge
import models.responses.{UnexpectedResponseStatus, NotFound as NotFoundResponse}
import models.{Country, PartialReturnPeriod, Period, StandardPeriod}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.{CountryOfConsumptionFromEuPage, CountryOfSaleFromEuPage}
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.{PartialReturnPeriodService, VatReturnSalesService}
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{Card, CardTitle}
import utils.FutureSyntax.FutureOps
import viewmodels.govuk.summarylist.*
import viewmodels.previousReturn.*
import viewmodels.previousReturn.corrections.CorrectionSummary
import views.html.{NewPreviousReturnView, PreviousReturnView}

import java.time.LocalDate
import scala.concurrent.Future

class PreviousReturnControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val mockVatReturnConnector = mock[VatReturnConnector]
  private val vatReturnSalesService = mock[VatReturnSalesService]
  private val mockFinancialDataConnector = mock[FinancialDataConnector]
  private val correctionConnector = mock[CorrectionConnector]
  private val mockPartialReturnPeriodService = mock[PartialReturnPeriodService]

  override def beforeEach(): Unit = {
    Mockito.reset(mockVatReturnConnector)
    Mockito.reset(vatReturnSalesService)
    Mockito.reset(mockFinancialDataConnector)
    Mockito.reset(correctionConnector)
    Mockito.reset(mockPartialReturnPeriodService)
    super.beforeEach()
  }

  private lazy val previousReturnRoute = routes.PreviousReturnController.onPageLoad(period).url

  private val countryFrom = arbitrary[Country].sample.value
  private val countryTo = arbitrary[Country].sample.value

  private val vatReturn = arbitrary[VatReturn].sample.value
  private val correctionPayload = arbitrary[CorrectionPayload].sample.value
  private val year = 2015

  private val baseAnswers = {
    emptyUserAnswers
      .set(CountryOfSaleFromEuPage(index), countryFrom).success.value
      .set(CountryOfConsumptionFromEuPage(index, index), countryTo).success.value
  }

  private val etmpVatReturn: EtmpVatReturn = arbitraryEtmpVatReturn.arbitrary.sample.value
  private val charge: Charge = arbitraryCharge.arbitrary.sample.value


  "Previous Return Controller" - {

    "legacy" - {

      "must redirect to NoLongerAbleToViewReturnController when the return period is older than six years" in {

        val period = StandardPeriod(year, Q1)

        val application = applicationBuilder(Some(baseAnswers))
          .configure("features.strategic-returns.enabled" -> false)
          .overrides(
            bind[VatReturnConnector].toInstance(mockVatReturnConnector),
            bind[VatReturnSalesService].toInstance(vatReturnSalesService),
            bind[FinancialDataConnector].toInstance(mockFinancialDataConnector),
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
          .configure("features.strategic-returns.enabled" -> false)
          .overrides(
            bind[VatReturnConnector].toInstance(mockVatReturnConnector),
            bind[VatReturnSalesService].toInstance(vatReturnSalesService),
            bind[FinancialDataConnector].toInstance(mockFinancialDataConnector),
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

        when(mockVatReturnConnector.get(any())(any())) thenReturn Future.successful(Right(vatReturn))
        when(mockFinancialDataConnector.getCharge(any())(any())) thenReturn Future.successful(Right(Some(charge)))
        when(vatReturnSalesService.getTotalNetSalesToCountry(any())) thenReturn netSalesFromNi
        when(vatReturnSalesService.getEuTotalNetSales(any())) thenReturn netSalesFromEu
        when(vatReturnSalesService.getTotalVatOnSalesToCountry(any())) thenReturn vatOnSalesFromNi
        when(vatReturnSalesService.getEuTotalVatOnSales(any())) thenReturn vatOnSalesFromEu
        when(correctionConnector.get(any())(any())) thenReturn Future.successful(Right(correctionPayload))
        when(vatReturnSalesService.getTotalVatOnSalesBeforeCorrection(any())) thenReturn totalVatOnSalesBeforeCorrection
        when(vatReturnSalesService.getTotalVatOnSalesAfterCorrection(any(), eqTo(Some(correctionPayload)))) thenReturn totalVatOnSalesAfterCorrection
        when(mockVatReturnConnector.getSavedExternalEntry()(any())) thenReturn Future.successful(Right(ExternalEntryUrl(None)))

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
          .configure("features.strategic-returns.enabled" -> false)
          .overrides(
            bind[VatReturnConnector].toInstance(mockVatReturnConnector),
            bind[VatReturnSalesService].toInstance(vatReturnSalesService),
            bind[FinancialDataConnector].toInstance(mockFinancialDataConnector),
            bind[CorrectionConnector].toInstance(correctionConnector)
          )
          .build()

        val netSalesFromNi = BigDecimal(4141)
        val netSalesFromEu = BigDecimal(2333)
        val vatOnSalesFromNi = BigDecimal(55)
        val vatOnSalesFromEu = BigDecimal(44)
        val totalVatOnSales = vatOnSalesFromNi + vatOnSalesFromEu

        when(mockVatReturnConnector.get(any())(any())) thenReturn Future.successful(Right(vatReturn))
        when(mockFinancialDataConnector.getCharge(any())(any())) thenReturn Future.successful(Right(None))
        when(vatReturnSalesService.getTotalNetSalesToCountry(any())) thenReturn netSalesFromNi
        when(vatReturnSalesService.getEuTotalNetSales(any())) thenReturn netSalesFromEu
        when(vatReturnSalesService.getTotalVatOnSalesToCountry(any())) thenReturn vatOnSalesFromNi
        when(vatReturnSalesService.getEuTotalVatOnSales(any())) thenReturn vatOnSalesFromEu
        when(vatReturnSalesService.getTotalVatOnSalesBeforeCorrection(any())) thenReturn totalVatOnSales
        when(vatReturnSalesService.getTotalVatOnSalesAfterCorrection(any(), eqTo(None))) thenReturn totalVatOnSales
        when(correctionConnector.get(any())(any())) thenReturn Future.successful(Left(NotFoundResponse))
        when(mockVatReturnConnector.getSavedExternalEntry()(any())) thenReturn Future.successful(Right(ExternalEntryUrl(None)))

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
          .configure("features.strategic-returns.enabled" -> false)
          .overrides(
            bind[VatReturnConnector].toInstance(mockVatReturnConnector),
            bind[VatReturnSalesService].toInstance(vatReturnSalesService),
            bind[FinancialDataConnector].toInstance(mockFinancialDataConnector),
            bind[CorrectionConnector].toInstance(correctionConnector)
          )
          .build()

        val zero = BigDecimal(0)
        when(mockVatReturnConnector.get(any())(any())) thenReturn Future.successful(Right(vatReturn))
        when(mockFinancialDataConnector.getCharge(any())(any())) thenReturn Future.successful(Right(None))
        when(vatReturnSalesService.getTotalNetSalesToCountry(any())) thenReturn zero
        when(vatReturnSalesService.getEuTotalNetSales(any())) thenReturn zero
        when(vatReturnSalesService.getTotalVatOnSalesToCountry(any())) thenReturn zero
        when(vatReturnSalesService.getEuTotalVatOnSales(any())) thenReturn zero
        when(vatReturnSalesService.getTotalVatOnSalesBeforeCorrection(any())) thenReturn zero
        when(vatReturnSalesService.getTotalVatOnSalesAfterCorrection(any(), eqTo(None))) thenReturn zero
        when(correctionConnector.get(any())(any())) thenReturn Future.successful(Left(NotFoundResponse))
        when(mockVatReturnConnector.getSavedExternalEntry()(any())) thenReturn Future.successful(Right(ExternalEntryUrl(None)))

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
          .configure("features.strategic-returns.enabled" -> false)
          .overrides(
            bind[VatReturnConnector].toInstance(mockVatReturnConnector),
            bind[VatReturnSalesService].toInstance(vatReturnSalesService),
            bind[FinancialDataConnector].toInstance(mockFinancialDataConnector),
            bind[CorrectionConnector].toInstance(correctionConnector)
          )
          .build()

        val netSalesFromNi = BigDecimal(4141)
        val netSalesFromEu = BigDecimal(2333)
        val vatOnSalesFromNi = BigDecimal(55)
        val vatOnSalesFromEu = BigDecimal(44)
        val totalVatOnSales = vatOnSalesFromNi + vatOnSalesFromEu

        when(mockVatReturnConnector.get(any())(any())) thenReturn Future.successful(Right(vatReturn))
        when(mockFinancialDataConnector.getCharge(any())(any())) thenReturn Future.successful(Left(NotFoundResponse))
        when(vatReturnSalesService.getTotalNetSalesToCountry(any())) thenReturn netSalesFromNi
        when(vatReturnSalesService.getEuTotalNetSales(any())) thenReturn netSalesFromEu
        when(vatReturnSalesService.getTotalVatOnSalesToCountry(any())) thenReturn vatOnSalesFromNi
        when(vatReturnSalesService.getEuTotalVatOnSales(any())) thenReturn vatOnSalesFromEu
        when(vatReturnSalesService.getTotalVatOnSalesBeforeCorrection(any())) thenReturn totalVatOnSales
        when(vatReturnSalesService.getTotalVatOnSalesAfterCorrection(any(), eqTo(None))) thenReturn totalVatOnSales
        when(correctionConnector.get(any())(any())) thenReturn Future.successful(Left(NotFoundResponse))
        when(mockVatReturnConnector.getSavedExternalEntry()(any())) thenReturn Future.successful(Right(ExternalEntryUrl(None)))

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
          .configure("features.strategic-returns.enabled" -> false)
          .overrides(
            bind[VatReturnConnector].toInstance(mockVatReturnConnector),
            bind[VatReturnSalesService].toInstance(vatReturnSalesService),
            bind[FinancialDataConnector].toInstance(mockFinancialDataConnector),
            bind[CorrectionConnector].toInstance(correctionConnector)
          )
          .build()

        val zero = BigDecimal(0)
        val correctionAmount = BigDecimal(100)

        when(mockVatReturnConnector.get(any())(any())) thenReturn Future.successful(Right(vatReturn))
        when(mockFinancialDataConnector.getCharge(any())(any())) thenReturn Future.successful(Right(None))
        when(vatReturnSalesService.getTotalNetSalesToCountry(any())) thenReturn zero
        when(vatReturnSalesService.getEuTotalNetSales(any())) thenReturn zero
        when(vatReturnSalesService.getTotalVatOnSalesToCountry(any())) thenReturn zero
        when(vatReturnSalesService.getEuTotalVatOnSales(any())) thenReturn zero
        when(vatReturnSalesService.getTotalVatOnSalesBeforeCorrection(any())) thenReturn zero
        when(vatReturnSalesService.getTotalVatOnSalesAfterCorrection(any(), eqTo(Some(correctionPayload)))) thenReturn correctionAmount
        when(correctionConnector.get(any())(any())) thenReturn Future.successful(Right(correctionPayload))
        when(mockVatReturnConnector.getSavedExternalEntry()(any())) thenReturn Future.successful(Right(ExternalEntryUrl(None)))

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
          .configure("features.strategic-returns.enabled" -> false)
          .overrides(
            bind[VatReturnConnector].toInstance(mockVatReturnConnector),
            bind[VatReturnSalesService].toInstance(vatReturnSalesService),
            bind[FinancialDataConnector].toInstance(mockFinancialDataConnector),
            bind[CorrectionConnector].toInstance(correctionConnector)
          )
          .build()

        val correctionAmount = BigDecimal(100)

        when(mockVatReturnConnector.get(any())(any())) thenReturn Future.successful(Left(NotFoundResponse))
        when(mockFinancialDataConnector.getCharge(any())(any())) thenReturn Future.successful(Right(None))
        when(vatReturnSalesService.getTotalVatOnSalesAfterCorrection(any(), eqTo(Some(correctionPayload)))) thenReturn correctionAmount
        when(correctionConnector.get(any())(any())) thenReturn Future.successful(Right(correctionPayload))
        when(mockVatReturnConnector.getSavedExternalEntry()(any())) thenReturn Future.successful(Right(ExternalEntryUrl(None)))

        running(application) {
          val request = FakeRequest(GET, previousReturnRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.YourAccountController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery when an unexpected error received from vat return" in {

        val application = applicationBuilder(Some(emptyUserAnswers))
          .configure("features.strategic-returns.enabled" -> false)
          .overrides(
            bind[VatReturnConnector].toInstance(mockVatReturnConnector),
            bind[VatReturnSalesService].toInstance(vatReturnSalesService),
            bind[FinancialDataConnector].toInstance(mockFinancialDataConnector),
            bind[CorrectionConnector].toInstance(correctionConnector)
          )
          .build()

        val correctionAmount = BigDecimal(100)

        when(mockVatReturnConnector.get(any())(any())) thenReturn Future.successful(Left(UnexpectedResponseStatus(INTERNAL_SERVER_ERROR, "foo")))
        when(mockFinancialDataConnector.getCharge(any())(any())) thenReturn Future.successful(Right(None))
        when(vatReturnSalesService.getTotalVatOnSalesAfterCorrection(any(), eqTo(Some(correctionPayload)))) thenReturn correctionAmount
        when(correctionConnector.get(any())(any())) thenReturn Future.successful(Right(correctionPayload))
        when(mockVatReturnConnector.getSavedExternalEntry()(any())) thenReturn Future.successful(Right(ExternalEntryUrl(None)))

        running(application) {
          val request = FakeRequest(GET, previousReturnRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery and throw an exception when an unexpected result is returned" in {

        val application = applicationBuilder(Some(emptyUserAnswers))
          .configure("features.strategic-returns.enabled" -> false)
          .overrides(
            bind[VatReturnConnector].toInstance(mockVatReturnConnector),
            bind[VatReturnSalesService].toInstance(vatReturnSalesService),
            bind[FinancialDataConnector].toInstance(mockFinancialDataConnector),
            bind[CorrectionConnector].toInstance(correctionConnector)
          )
          .build()

        val correctionAmount = BigDecimal(100)

        when(mockVatReturnConnector.get(any())(any())) thenReturn Future.failed(new Exception("Some exception"))
        when(mockFinancialDataConnector.getCharge(any())(any())) thenReturn Future.successful(Right(None))
        when(vatReturnSalesService.getTotalVatOnSalesAfterCorrection(any(), eqTo(Some(correctionPayload)))) thenReturn correctionAmount
        when(correctionConnector.get(any())(any())) thenReturn Future.successful(Right(correctionPayload))
        when(mockVatReturnConnector.getSavedExternalEntry()(any())) thenReturn Future.successful(Right(ExternalEntryUrl(None)))

        running(application) {
          val request = FakeRequest(GET, previousReturnRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must return OK and the correct view for a GET with no banner and add the external backToYourAccount url that has been saved" in {

        val application = applicationBuilder(Some(baseAnswers))
          .configure("features.strategic-returns.enabled" -> false)
          .overrides(
            bind[VatReturnConnector].toInstance(mockVatReturnConnector),
            bind[VatReturnSalesService].toInstance(vatReturnSalesService),
            bind[FinancialDataConnector].toInstance(mockFinancialDataConnector),
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

        when(mockVatReturnConnector.get(any())(any())) thenReturn Future.successful(Right(vatReturn))
        when(mockFinancialDataConnector.getCharge(any())(any())) thenReturn Future.successful(Right(Some(charge)))
        when(vatReturnSalesService.getTotalNetSalesToCountry(any())) thenReturn netSalesFromNi
        when(vatReturnSalesService.getEuTotalNetSales(any())) thenReturn netSalesFromEu
        when(vatReturnSalesService.getTotalVatOnSalesToCountry(any())) thenReturn vatOnSalesFromNi
        when(vatReturnSalesService.getEuTotalVatOnSales(any())) thenReturn vatOnSalesFromEu
        when(correctionConnector.get(any())(any())) thenReturn Future.successful(Right(correctionPayload))
        when(vatReturnSalesService.getTotalVatOnSalesBeforeCorrection(any())) thenReturn totalVatOnSalesBeforeCorrection
        when(vatReturnSalesService.getTotalVatOnSalesAfterCorrection(any(), eqTo(Some(correctionPayload)))) thenReturn totalVatOnSalesAfterCorrection
        when(mockVatReturnConnector.getSavedExternalEntry()(any())) thenReturn Future.successful(Right(ExternalEntryUrl(Some("example"))))

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

    "new" - {

      "must redirect to NoLongerAbleToViewReturnController when the return period is older than six years" in {

        val period = StandardPeriod(year, Q1)

        val application = applicationBuilder(Some(baseAnswers))
          .configure("features.strategic-returns.enabled" -> true)
          .overrides(
            bind[VatReturnConnector].toInstance(mockVatReturnConnector),
            bind[FinancialDataConnector].toInstance(mockFinancialDataConnector)
          ).build()

        running(application) {
          val request = FakeRequest(GET, routes.PreviousReturnController.onPageLoad(period).url)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.NoLongerAbleToViewReturnController.onPageLoad().url
        }
      }

      "must return OK and the correct view for a GET" - {

        "when there are corrections present" in {

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .configure("features.strategic-returns.enabled" -> true)
            .overrides(
              bind[VatReturnConnector].toInstance(mockVatReturnConnector),
              bind[FinancialDataConnector].toInstance(mockFinancialDataConnector),
              bind[PartialReturnPeriodService].toInstance(mockPartialReturnPeriodService)
            ).build()

          running(application) {
            when(mockVatReturnConnector.getEtmpVatReturn(any())(any())) thenReturn Right(etmpVatReturn).toFuture
            when(mockFinancialDataConnector.getCharge(any())(any())) thenReturn Right(Some(charge)).toFuture
            when(mockVatReturnConnector.getSavedExternalEntry()(any())) thenReturn Right(ExternalEntryUrl(None)).toFuture
            when(mockPartialReturnPeriodService.getPartialReturnPeriod(any(), any())(any())) thenReturn None.toFuture

            implicit val msgs: Messages = messages(application)

            val request = FakeRequest(GET, routes.PreviousReturnController.onPageLoad(period).url)

            val result = route(application, request).value

            val view = application.injector.instanceOf[NewPreviousReturnView]

            val mainSummaryList = SummaryListViewModel(
              rows = Seq(
                NewPreviousReturnSummary.rowPayableVatDeclared(etmpVatReturn.totalVATAmountDueForAllMSGBP),
                NewPreviousReturnSummary.rowAmountLeftToPay(Some(charge.outstandingAmount)),
                NewPreviousReturnSummary.rowReturnSubmittedDate(etmpVatReturn.returnVersion),
                NewPreviousReturnSummary.rowPaymentDueDate(period.paymentDeadline),
                NewPreviousReturnSummary.rowReturnReference(etmpVatReturn.returnReference),
                NewPreviousReturnSummary.rowPaymentReference(etmpVatReturn.paymentReference)
              ).flatten
            )

            val allEuSales = PreviousReturnTotalNetValueOfSalesSummary.rows(etmpVatReturn.goodsSupplied)

            val correctionRowsSummaryList = PreviousReturnCorrectionsSummary.correctionRows(etmpVatReturn.correctionPreviousVATReturn)

            val negativeAndZeroBalanceCorrectionCountriesSummaryList =
              PreviousReturnDeclaredVATNoPaymentDueSummary.summaryRowsOfNegativeAndZeroValues(etmpVatReturn)

            val vatOwedSummaryList = SummaryListViewModel(
              rows = PreviousReturnVatOwedSummary.row(etmpVatReturn)
            ).withCard(
              card = Card(
                title = Some(CardTitle(content = HtmlContent(msgs("newPreviousReturn.vatOwed.titleWithCorrections"))))
              )
            )

            val outstandingAmount: BigDecimal = charge.outstandingAmount
            val vatDeclared = etmpVatReturn.totalVATAmountDueForAllMSGBP

            status(result) mustBe OK
            contentAsString(result) mustBe
              view(
                period,
                mainSummaryList,
                allEuSales,
                correctionRowsSummaryList,
                negativeAndZeroBalanceCorrectionCountriesSummaryList,
                vatOwedSummaryList,
                displayPayNow = vatDeclared > 0 && outstandingAmount > 0,
                outstandingAmount,
                returnIsExcludedAndOutstandingAmount = false,
                vatOwedInPence = (vatDeclared * 100).toLong
              )(request, messages(application)).toString
          }
        }

        "when there are no corrections present" in {

          val etmpVatReturnNoCorrections: EtmpVatReturn = etmpVatReturn.copy(correctionPreviousVATReturn = Seq.empty)

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .configure("features.strategic-returns.enabled" -> true)
            .overrides(
              bind[VatReturnConnector].toInstance(mockVatReturnConnector),
              bind[FinancialDataConnector].toInstance(mockFinancialDataConnector),
              bind[PartialReturnPeriodService].toInstance(mockPartialReturnPeriodService)
            ).build()

          running(application) {
            when(mockVatReturnConnector.getEtmpVatReturn(any())(any())) thenReturn Right(etmpVatReturnNoCorrections).toFuture
            when(mockFinancialDataConnector.getCharge(any())(any())) thenReturn Right(Some(charge)).toFuture
            when(mockVatReturnConnector.getSavedExternalEntry()(any())) thenReturn Right(ExternalEntryUrl(None)).toFuture
            when(mockPartialReturnPeriodService.getPartialReturnPeriod(any(), any())(any())) thenReturn None.toFuture

            implicit val msgs: Messages = messages(application)

            val request = FakeRequest(GET, routes.PreviousReturnController.onPageLoad(period).url)

            val result = route(application, request).value

            val view = application.injector.instanceOf[NewPreviousReturnView]

            val mainSummaryList = SummaryListViewModel(
              rows = Seq(
                NewPreviousReturnSummary.rowPayableVatDeclared(etmpVatReturnNoCorrections.totalVATAmountDueForAllMSGBP),
                NewPreviousReturnSummary.rowAmountLeftToPay(Some(charge.outstandingAmount)),
                NewPreviousReturnSummary.rowReturnSubmittedDate(etmpVatReturnNoCorrections.returnVersion),
                NewPreviousReturnSummary.rowPaymentDueDate(period.paymentDeadline),
                NewPreviousReturnSummary.rowReturnReference(etmpVatReturnNoCorrections.returnReference),
                NewPreviousReturnSummary.rowPaymentReference(etmpVatReturnNoCorrections.paymentReference)
              ).flatten
            )

            val allEuSales = PreviousReturnTotalNetValueOfSalesSummary.rows(etmpVatReturn.goodsSupplied)

            val correctionRowsSummaryList = PreviousReturnCorrectionsSummary.correctionRows(etmpVatReturnNoCorrections.correctionPreviousVATReturn)

            val negativeAndZeroBalanceCorrectionCountriesSummaryList =
              PreviousReturnDeclaredVATNoPaymentDueSummary.summaryRowsOfNegativeAndZeroValues(etmpVatReturnNoCorrections)

            val vatOwedSummaryList = SummaryListViewModel(
              rows = PreviousReturnVatOwedSummary.row(etmpVatReturnNoCorrections)
            ).withCard(
              card = Card(
                title = Some(CardTitle(content = HtmlContent(msgs("newPreviousReturn.vatOwed.title"))))
              )
            )

            val outstandingAmount: BigDecimal = charge.outstandingAmount
            val vatDeclared = etmpVatReturnNoCorrections.totalVATAmountDueForAllMSGBP

            status(result) mustBe OK
            contentAsString(result) mustBe
              view(
                period,
                mainSummaryList,
                allEuSales,
                correctionRowsSummaryList,
                negativeAndZeroBalanceCorrectionCountriesSummaryList,
                vatOwedSummaryList,
                displayPayNow = vatDeclared > 0 && outstandingAmount > 0,
                outstandingAmount,
                returnIsExcludedAndOutstandingAmount = false,
                vatOwedInPence = (vatDeclared * 100).toLong
              )(request, messages(application)).toString
          }
        }

        "when there are no negative corrections present" in {

          val etmpVatReturnPositiveCorrections: EtmpVatReturn =
            etmpVatReturn.copy(correctionPreviousVATReturn =
              Seq(EtmpVatReturnCorrection(
                periodKey = arbitraryPeriodKey.arbitrary.sample.value,
                periodFrom = arbitrary[String].sample.value,
                periodTo = arbitrary[String].sample.value,
                msOfConsumption = arbitraryCountry.arbitrary.sample.value.code,
                totalVATAmountCorrectionGBP = BigDecimal(200.56),
                totalVATAmountCorrectionEUR = BigDecimal(200.56)
              )))

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .configure("features.strategic-returns.enabled" -> true)
            .overrides(
              bind[VatReturnConnector].toInstance(mockVatReturnConnector),
              bind[FinancialDataConnector].toInstance(mockFinancialDataConnector),
              bind[PartialReturnPeriodService].toInstance(mockPartialReturnPeriodService)
            ).build()

          running(application) {
            when(mockVatReturnConnector.getEtmpVatReturn(any())(any())) thenReturn Right(etmpVatReturnPositiveCorrections).toFuture
            when(mockFinancialDataConnector.getCharge(any())(any())) thenReturn Right(Some(charge)).toFuture
            when(mockVatReturnConnector.getSavedExternalEntry()(any())) thenReturn Right(ExternalEntryUrl(None)).toFuture
            when(mockPartialReturnPeriodService.getPartialReturnPeriod(any(), any())(any())) thenReturn None.toFuture

            implicit val msgs: Messages = messages(application)

            val request = FakeRequest(GET, routes.PreviousReturnController.onPageLoad(period).url)

            val result = route(application, request).value

            val view = application.injector.instanceOf[NewPreviousReturnView]

            val mainSummaryList = SummaryListViewModel(
              rows = Seq(
                NewPreviousReturnSummary.rowPayableVatDeclared(etmpVatReturnPositiveCorrections.totalVATAmountDueForAllMSGBP),
                NewPreviousReturnSummary.rowAmountLeftToPay(Some(charge.outstandingAmount)),
                NewPreviousReturnSummary.rowReturnSubmittedDate(etmpVatReturnPositiveCorrections.returnVersion),
                NewPreviousReturnSummary.rowPaymentDueDate(period.paymentDeadline),
                NewPreviousReturnSummary.rowReturnReference(etmpVatReturnPositiveCorrections.returnReference),
                NewPreviousReturnSummary.rowPaymentReference(etmpVatReturnPositiveCorrections.paymentReference)
              ).flatten
            )

            val allEuSales = PreviousReturnTotalNetValueOfSalesSummary.rows(etmpVatReturn.goodsSupplied)

            val correctionRowsSummaryList = PreviousReturnCorrectionsSummary
              .correctionRows(etmpVatReturnPositiveCorrections.correctionPreviousVATReturn)

            val negativeAndZeroBalanceCorrectionCountriesSummaryList =
              PreviousReturnDeclaredVATNoPaymentDueSummary.summaryRowsOfNegativeAndZeroValues(etmpVatReturnPositiveCorrections)

            val vatOwedSummaryList = SummaryListViewModel(
              rows = PreviousReturnVatOwedSummary.row(etmpVatReturnPositiveCorrections)
            ).withCard(
              card = Card(
                title = Some(CardTitle(content = HtmlContent(msgs("newPreviousReturn.vatOwed.titleWithCorrections"))))
              )
            )

            val outstandingAmount: BigDecimal = charge.outstandingAmount
            val vatDeclared = etmpVatReturnPositiveCorrections.totalVATAmountDueForAllMSGBP

            status(result) mustBe OK
            contentAsString(result) mustBe
              view(
                period,
                mainSummaryList,
                allEuSales,
                correctionRowsSummaryList,
                negativeAndZeroBalanceCorrectionCountriesSummaryList,
                vatOwedSummaryList,
                displayPayNow = vatDeclared > 0 && outstandingAmount > 0,
                outstandingAmount,
                returnIsExcludedAndOutstandingAmount = false,
                vatOwedInPence = (vatDeclared * 100).toLong
              )(request, messages(application)).toString
          }
        }

        "when there is a nil return" in {

          val nilEtmpVatReturn: EtmpVatReturn =
            etmpVatReturn.copy(
              goodsSupplied = Seq.empty,
              totalVATGoodsSuppliedGBP = BigDecimal(0),
              totalVATAmountPayable = BigDecimal(0),
              totalVATAmountPayableAllSpplied = BigDecimal(0),
              correctionPreviousVATReturn = Seq.empty,
              totalVATAmountFromCorrectionGBP = BigDecimal(0),
              balanceOfVATDueForMS = Seq.empty,
              totalVATAmountDueForAllMSGBP = BigDecimal(0)
            )

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .configure("features.strategic-returns.enabled" -> true)
            .overrides(
              bind[VatReturnConnector].toInstance(mockVatReturnConnector),
              bind[FinancialDataConnector].toInstance(mockFinancialDataConnector),
              bind[PartialReturnPeriodService].toInstance(mockPartialReturnPeriodService)
            ).build()

          running(application) {
            when(mockVatReturnConnector.getEtmpVatReturn(any())(any())) thenReturn Right(nilEtmpVatReturn).toFuture
            when(mockFinancialDataConnector.getCharge(any())(any())) thenReturn Right(None).toFuture
            when(mockPartialReturnPeriodService.getPartialReturnPeriod(any(), any())(any())) thenReturn None.toFuture

            implicit val msgs: Messages = messages(application)

            val request = FakeRequest(GET, routes.PreviousReturnController.onPageLoad(period).url)

            val result = route(application, request).value

            val view = application.injector.instanceOf[NewPreviousReturnView]

            val mainSummaryList = SummaryListViewModel(
              rows = Seq(
                NewPreviousReturnSummary.rowPayableVatDeclared(nilEtmpVatReturn.totalVATAmountDueForAllMSGBP),
                NewPreviousReturnSummary.rowAmountLeftToPay(None),
                NewPreviousReturnSummary.rowReturnSubmittedDate(nilEtmpVatReturn.returnVersion),
                NewPreviousReturnSummary.rowPaymentDueDate(period.paymentDeadline),
                NewPreviousReturnSummary.rowReturnReference(nilEtmpVatReturn.returnReference),
                NewPreviousReturnSummary.rowPaymentReference(nilEtmpVatReturn.paymentReference)
              ).flatten
            )

            val allEuSales = PreviousReturnTotalNetValueOfSalesSummary.rows(nilEtmpVatReturn.goodsSupplied)

            val correctionRowsSummaryList = PreviousReturnCorrectionsSummary
              .correctionRows(nilEtmpVatReturn.correctionPreviousVATReturn)

            val negativeAndZeroBalanceCorrectionCountriesSummaryList =
              PreviousReturnDeclaredVATNoPaymentDueSummary.summaryRowsOfNegativeAndZeroValues(nilEtmpVatReturn)

            val vatOwedSummaryList = SummaryListViewModel(
              rows = Seq.empty
            )

            val outstandingAmount: BigDecimal = BigDecimal(0)
            val vatDeclared = nilEtmpVatReturn.totalVATAmountDueForAllMSGBP

            status(result) mustBe OK
            contentAsString(result) mustBe
              view(
                period,
                mainSummaryList,
                allEuSales,
                correctionRowsSummaryList,
                negativeAndZeroBalanceCorrectionCountriesSummaryList,
                vatOwedSummaryList,
                displayPayNow = vatDeclared > 0 && outstandingAmount > 0,
                outstandingAmount,
                returnIsExcludedAndOutstandingAmount = false,
                vatOwedInPence = (vatDeclared * 100).toLong
              )(request, messages(application)).toString
          }
        }

        "when FinancialData API is down" in {

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .configure("features.strategic-returns.enabled" -> true)
            .overrides(
              bind[VatReturnConnector].toInstance(mockVatReturnConnector),
              bind[FinancialDataConnector].toInstance(mockFinancialDataConnector),
              bind[PartialReturnPeriodService].toInstance(mockPartialReturnPeriodService)
            ).build()

          running(application) {
            when(mockVatReturnConnector.getEtmpVatReturn(any())(any())) thenReturn Right(etmpVatReturn).toFuture
            when(mockFinancialDataConnector.getCharge(any())(any())) thenReturn Left(UnexpectedResponseStatus(INTERNAL_SERVER_ERROR, "ERROR")).toFuture
            when(mockVatReturnConnector.getSavedExternalEntry()(any())) thenReturn Right(ExternalEntryUrl(None)).toFuture
            when(mockPartialReturnPeriodService.getPartialReturnPeriod(any(), any())(any())) thenReturn None.toFuture

            implicit val msgs: Messages = messages(application)

            val request = FakeRequest(GET, routes.PreviousReturnController.onPageLoad(period).url)

            val result = route(application, request).value

            val view = application.injector.instanceOf[NewPreviousReturnView]

            val mainSummaryList = SummaryListViewModel(
              rows = Seq(
                NewPreviousReturnSummary.rowPayableVatDeclared(etmpVatReturn.totalVATAmountDueForAllMSGBP),
                NewPreviousReturnSummary.rowAmountLeftToPay(None),
                NewPreviousReturnSummary.rowReturnSubmittedDate(etmpVatReturn.returnVersion),
                NewPreviousReturnSummary.rowPaymentDueDate(period.paymentDeadline),
                NewPreviousReturnSummary.rowReturnReference(etmpVatReturn.returnReference),
                NewPreviousReturnSummary.rowPaymentReference(etmpVatReturn.paymentReference)
              ).flatten
            )

            val allEuSales = PreviousReturnTotalNetValueOfSalesSummary.rows(etmpVatReturn.goodsSupplied)

            val correctionRowsSummaryList = PreviousReturnCorrectionsSummary.correctionRows(etmpVatReturn.correctionPreviousVATReturn)

            val negativeAndZeroBalanceCorrectionCountriesSummaryList =
              PreviousReturnDeclaredVATNoPaymentDueSummary.summaryRowsOfNegativeAndZeroValues(etmpVatReturn)

            val vatOwedSummaryList = SummaryListViewModel(
              rows = PreviousReturnVatOwedSummary.row(etmpVatReturn)
            ).withCard(
              card = Card(
                title = Some(CardTitle(content = HtmlContent(msgs("newPreviousReturn.vatOwed.titleWithCorrections"))))
              )
            )

            val outstandingAmount: BigDecimal = etmpVatReturn.totalVATAmountPayable
            val vatDeclared = etmpVatReturn.totalVATAmountDueForAllMSGBP

            status(result) mustBe OK
            contentAsString(result) mustBe
              view(
                period,
                mainSummaryList,
                allEuSales,
                correctionRowsSummaryList,
                negativeAndZeroBalanceCorrectionCountriesSummaryList,
                vatOwedSummaryList,
                displayPayNow = vatDeclared > 0 && outstandingAmount > 0,
                outstandingAmount,
                returnIsExcludedAndOutstandingAmount = false,
                vatOwedInPence = (vatDeclared * 100).toLong
              )(request, messages(application)).toString
          }
        }

        "when there are exclusions present and vat return due date has exceeded 3 years" - {

          val date = LocalDate.ofInstant(stubClockAtArbitraryDate.instant(), stubClockAtArbitraryDate.getZone)
          val exceededDate = date.minusYears(3).minusMonths(4)
          val exceededPeriod = getPeriod(exceededDate)
          val vatReturnNoCorrections: EtmpVatReturn = etmpVatReturn.copy(
            correctionPreviousVATReturn = Seq.empty,
            periodKey = exceededPeriod.toString.replace("-", "").substring(2, 6)
          )

          val excludedTrader: ExcludedTrader = ExcludedTrader(
            exclusionReason = ExclusionReason.NoLongerSupplies,
            effectiveDate = LocalDate.now(stubClockAtArbitraryDate).plusMonths(6),
            vrn = vrn,
            quarantined = false
          )

          val registration = arbitraryRegistration.arbitrary.sample.value.copy(vrn = vrn, excludedTrader = Some(excludedTrader))

          "and nothing owed" in {

            val nilCharge = charge.copy(outstandingAmount = BigDecimal(0))

            val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), registration = registration)
              .configure("features.strategic-returns.enabled" -> true)
              .overrides(
                bind[VatReturnConnector].toInstance(mockVatReturnConnector),
                bind[FinancialDataConnector].toInstance(mockFinancialDataConnector)
              ).build()

            running(application) {
              when(mockVatReturnConnector.getEtmpVatReturn(any())(any())) thenReturn Right(vatReturnNoCorrections).toFuture
              when(mockFinancialDataConnector.getCharge(any())(any())) thenReturn Right(Some(nilCharge)).toFuture

              implicit val msgs: Messages = messages(application)

              val request = FakeRequest(GET, routes.PreviousReturnController.onPageLoad(exceededPeriod).url)

              val result = route(application, request).value

              val view = application.injector.instanceOf[NewPreviousReturnView]

              val mainSummaryList = SummaryListViewModel(
                rows = Seq(
                  NewPreviousReturnSummary.rowPayableVatDeclared(vatReturnNoCorrections.totalVATAmountDueForAllMSGBP),
                  NewPreviousReturnSummary.rowAmountLeftToPay(Some(nilCharge.outstandingAmount)),
                  NewPreviousReturnSummary.rowReturnSubmittedDate(vatReturnNoCorrections.returnVersion),
                  NewPreviousReturnSummary.rowPaymentDueDate(exceededPeriod.paymentDeadline),
                  NewPreviousReturnSummary.rowReturnReference(vatReturnNoCorrections.returnReference),
                  NewPreviousReturnSummary.rowPaymentReference(vatReturnNoCorrections.paymentReference)
                ).flatten
              )

              val allEuSales = PreviousReturnTotalNetValueOfSalesSummary.rows(vatReturnNoCorrections.goodsSupplied)

              val correctionRowsSummaryList = PreviousReturnCorrectionsSummary
                .correctionRows(vatReturnNoCorrections.correctionPreviousVATReturn)

              val negativeAndZeroBalanceCorrectionCountriesSummaryList =
                PreviousReturnDeclaredVATNoPaymentDueSummary.summaryRowsOfNegativeAndZeroValues(vatReturnNoCorrections)

              val vatOwedSummaryList = SummaryListViewModel(
                rows = PreviousReturnVatOwedSummary.row(vatReturnNoCorrections)
              ).withCard(
                card = Card(
                  title = Some(CardTitle(content = HtmlContent(msgs("newPreviousReturn.vatOwed.title"))))
                )
              )

              val outstandingAmount: BigDecimal = nilCharge.outstandingAmount
              val vatDeclared: BigDecimal = vatReturnNoCorrections.totalVATAmountDueForAllMSGBP

              status(result) mustBe OK
              contentAsString(result) mustBe
                view(
                  exceededPeriod,
                  mainSummaryList,
                  allEuSales,
                  correctionRowsSummaryList,
                  negativeAndZeroBalanceCorrectionCountriesSummaryList,
                  vatOwedSummaryList,
                  displayPayNow = false,
                  outstandingAmount,
                  returnIsExcludedAndOutstandingAmount = false,
                  vatOwedInPence = (vatDeclared * 100).toLong
                )(request, messages(application)).toString
            }
          }

          "and something owed" in {

            val outstandingCharge = charge.copy(outstandingAmount = BigDecimal(1000))

            val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), registration = registration)
              .configure("features.strategic-returns.enabled" -> true)
              .overrides(
                bind[VatReturnConnector].toInstance(mockVatReturnConnector),
                bind[FinancialDataConnector].toInstance(mockFinancialDataConnector)
              ).build()

            running(application) {
              when(mockVatReturnConnector.getEtmpVatReturn(any())(any())) thenReturn Right(vatReturnNoCorrections).toFuture
              when(mockFinancialDataConnector.getCharge(any())(any())) thenReturn Right(Some(outstandingCharge)).toFuture

              implicit val msgs: Messages = messages(application)

              val request = FakeRequest(GET, routes.PreviousReturnController.onPageLoad(exceededPeriod).url)

              val result = route(application, request).value

              val view = application.injector.instanceOf[NewPreviousReturnView]

              val mainSummaryList = SummaryListViewModel(
                rows = Seq(
                  NewPreviousReturnSummary.rowPayableVatDeclared(vatReturnNoCorrections.totalVATAmountDueForAllMSGBP),
                  NewPreviousReturnSummary.rowAmountLeftToPay(Some(outstandingCharge.outstandingAmount)),
                  NewPreviousReturnSummary.rowReturnSubmittedDate(vatReturnNoCorrections.returnVersion),
                  NewPreviousReturnSummary.rowPaymentDueDate(exceededPeriod.paymentDeadline),
                  NewPreviousReturnSummary.rowReturnReference(vatReturnNoCorrections.returnReference),
                  NewPreviousReturnSummary.rowPaymentReference(vatReturnNoCorrections.paymentReference)
                ).flatten
              )

              val allEuSales = PreviousReturnTotalNetValueOfSalesSummary.rows(vatReturnNoCorrections.goodsSupplied)

              val correctionRowsSummaryList = PreviousReturnCorrectionsSummary
                .correctionRows(vatReturnNoCorrections.correctionPreviousVATReturn)

              val negativeAndZeroBalanceCorrectionCountriesSummaryList =
                PreviousReturnDeclaredVATNoPaymentDueSummary.summaryRowsOfNegativeAndZeroValues(vatReturnNoCorrections)

              val vatOwedSummaryList = SummaryListViewModel(
                rows = PreviousReturnVatOwedSummary.row(vatReturnNoCorrections)
              ).withCard(
                card = Card(
                  title = Some(CardTitle(content = HtmlContent(msgs("newPreviousReturn.vatOwed.title"))))
                )
              )

              val outstandingAmount: BigDecimal = outstandingCharge.outstandingAmount
              val vatDeclared: BigDecimal = vatReturnNoCorrections.totalVATAmountDueForAllMSGBP

              status(result) mustBe OK
              contentAsString(result) mustBe
                view(
                  exceededPeriod,
                  mainSummaryList,
                  allEuSales,
                  correctionRowsSummaryList,
                  negativeAndZeroBalanceCorrectionCountriesSummaryList,
                  vatOwedSummaryList,
                  displayPayNow = false,
                  outstandingAmount,
                  returnIsExcludedAndOutstandingAmount = true,
                  vatOwedInPence = (vatDeclared * 100).toLong
                )(request, messages(application)).toString
            }
          }
        }

        "when there is a partial return period" in {

          val partialReturnPeriod: PartialReturnPeriod = arbitraryPartialReturnPeriod.arbitrary.sample.value
          val periodKey = s"${partialReturnPeriod.year.toString.substring(2, 4)}${partialReturnPeriod.quarter}"
          val partialReturn: EtmpVatReturn = etmpVatReturn.copy(periodKey = periodKey)

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .configure("features.strategic-returns.enabled" -> true)
            .overrides(
              bind[VatReturnConnector].toInstance(mockVatReturnConnector),
              bind[FinancialDataConnector].toInstance(mockFinancialDataConnector),
              bind[PartialReturnPeriodService].toInstance(mockPartialReturnPeriodService)
            ).build()

          running(application) {
            when(mockVatReturnConnector.getEtmpVatReturn(any())(any())) thenReturn Right(partialReturn).toFuture
            when(mockFinancialDataConnector.getCharge(any())(any())) thenReturn Right(Some(charge)).toFuture
            when(mockVatReturnConnector.getSavedExternalEntry()(any())) thenReturn Right(ExternalEntryUrl(None)).toFuture
            when(mockPartialReturnPeriodService.getPartialReturnPeriod(any(), any())(any())) thenReturn Some(partialReturnPeriod).toFuture

            implicit val msgs: Messages = messages(application)

            val request = FakeRequest(GET, routes.PreviousReturnController.onPageLoad(partialReturnPeriod).url)

            val result = route(application, request).value

            val view = application.injector.instanceOf[NewPreviousReturnView]

            val mainSummaryList = SummaryListViewModel(
              rows = Seq(
                NewPreviousReturnSummary.rowPayableVatDeclared(partialReturn.totalVATAmountDueForAllMSGBP),
                NewPreviousReturnSummary.rowAmountLeftToPay(Some(charge.outstandingAmount)),
                NewPreviousReturnSummary.rowReturnSubmittedDate(partialReturn.returnVersion),
                NewPreviousReturnSummary.rowPaymentDueDate(partialReturnPeriod.paymentDeadline),
                NewPreviousReturnSummary.rowReturnReference(partialReturn.returnReference),
                NewPreviousReturnSummary.rowPaymentReference(partialReturn.paymentReference)
              ).flatten
            )

            val allEuSales = PreviousReturnTotalNetValueOfSalesSummary.rows(partialReturn.goodsSupplied)

            val correctionRowsSummaryList = PreviousReturnCorrectionsSummary.correctionRows(partialReturn.correctionPreviousVATReturn)

            val negativeAndZeroBalanceCorrectionCountriesSummaryList =
              PreviousReturnDeclaredVATNoPaymentDueSummary.summaryRowsOfNegativeAndZeroValues(partialReturn)

            val vatOwedSummaryList = SummaryListViewModel(
              rows = PreviousReturnVatOwedSummary.row(partialReturn)
            ).withCard(
              card = Card(
                title = Some(CardTitle(content = HtmlContent(msgs("newPreviousReturn.vatOwed.titleWithCorrections"))))
              )
            )

            val outstandingAmount: BigDecimal = charge.outstandingAmount
            val vatDeclared = partialReturn.totalVATAmountDueForAllMSGBP

            status(result) mustBe OK
            contentAsString(result) mustBe
              view(
                partialReturnPeriod,
                mainSummaryList,
                allEuSales,
                correctionRowsSummaryList,
                negativeAndZeroBalanceCorrectionCountriesSummaryList,
                vatOwedSummaryList,
                displayPayNow = vatDeclared > 0 && outstandingAmount > 0,
                outstandingAmount,
                returnIsExcludedAndOutstandingAmount = false,
                vatOwedInPence = (vatDeclared * 100).toLong
              )(request, messages(application)).toString
          }
        }
      }

      "must throw Exception when VAT Return retrieval fails" in {

        val message: String = s"There was an error retrieving ETMP VAT return"

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .configure("features.strategic-returns.enabled" -> true)
          .overrides(
            bind[VatReturnConnector].toInstance(mockVatReturnConnector),
            bind[FinancialDataConnector].toInstance(mockFinancialDataConnector)
          ).build()

        running(application) {
          when(mockFinancialDataConnector.getCharge(any())(any())) thenReturn Right(None).toFuture
          when(mockVatReturnConnector.getEtmpVatReturn(any())(any())) thenReturn Left(UnexpectedResponseStatus(INTERNAL_SERVER_ERROR, "ERROR")).toFuture

          val request = FakeRequest(GET, routes.PreviousReturnController.onPageLoad(period).url)

          val result = route(application, request).value

          whenReady(result.failed) { e =>
            e mustBe a[Exception]
            e.getMessage mustBe message
          }
        }
      }
    }
  }
}
