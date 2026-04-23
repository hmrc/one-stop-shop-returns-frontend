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
import connectors.financialdata.FinancialDataConnector
import models.Period.{fromEtmpPeriodKey, getPeriod}
import models.Quarter.Q1
import models.etmp.{EtmpVatReturn, EtmpVatReturnCorrection}
import models.exclusions.{ExcludedTrader, ExclusionReason}
import models.external.ExternalEntryUrl
import models.financialdata.Charge
import models.responses.UnexpectedResponseStatus
import models.{Country, PartialReturnPeriod, Period, StandardPeriod}
import org.mockito.ArgumentMatchers.any
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
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{Card, CardTitle}
import utils.FutureSyntax.FutureOps
import viewmodels.govuk.summarylist.*
import viewmodels.previousReturn.*
import views.html.NewPreviousReturnView

import java.time.LocalDate

class PreviousReturnControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val mockVatReturnConnector = mock[VatReturnConnector]
  private val mockFinancialDataConnector = mock[FinancialDataConnector]

  override def beforeEach(): Unit = {
    Mockito.reset(mockVatReturnConnector)
    Mockito.reset(mockFinancialDataConnector)
    super.beforeEach()
  }

  private val countryFrom = arbitrary[Country].sample.value
  private val countryTo = arbitrary[Country].sample.value

  private val year = 2015

  private val baseAnswers = {
    emptyUserAnswers
      .set(CountryOfSaleFromEuPage(index), countryFrom).success.value
      .set(CountryOfConsumptionFromEuPage(index, index), countryTo).success.value
  }

  private val etmpVatReturn: EtmpVatReturn = arbitraryEtmpVatReturn.arbitrary.sample.value
  private val charge: Charge = arbitraryCharge.arbitrary.sample.value


  "Previous Return Controller" - {

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

          status(result) `mustBe` SEE_OTHER
          redirectLocation(result).value `mustBe` routes.NoLongerAbleToViewReturnController.onPageLoad().url
        }
      }

      "must return OK and the correct view for a GET" - {

        "when there are corrections present" in {

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .configure("features.strategic-returns.enabled" -> true)
            .overrides(
              bind[VatReturnConnector].toInstance(mockVatReturnConnector),
              bind[FinancialDataConnector].toInstance(mockFinancialDataConnector)
            ).build()

          running(application) {
            when(mockVatReturnConnector.getEtmpVatReturn(any())(any())) thenReturn Right(etmpVatReturn).toFuture
            when(mockFinancialDataConnector.getCharge(any())(any())) thenReturn Right(Some(charge)).toFuture
            when(mockVatReturnConnector.getSavedExternalEntry()(any())) thenReturn Right(ExternalEntryUrl(None)).toFuture

            implicit val msgs: Messages = messages(application)

            val determinedPeriod: Period = fromEtmpPeriodKey(etmpVatReturn.periodKey)

            val request = FakeRequest(GET, routes.PreviousReturnController.onPageLoad(determinedPeriod).url)

            val result = route(application, request).value

            val view = application.injector.instanceOf[NewPreviousReturnView]

            val mainSummaryList = SummaryListViewModel(
              rows = Seq(
                NewPreviousReturnSummary.rowPayableVatDeclared(etmpVatReturn.totalVATAmountDueForAllMSGBP),
                NewPreviousReturnSummary.rowAmountLeftToPay(Some(charge.outstandingAmount)),
                NewPreviousReturnSummary.rowReturnSubmittedDate(etmpVatReturn.returnVersion),
                NewPreviousReturnSummary.rowPaymentDueDate(determinedPeriod.paymentDeadline),
                NewPreviousReturnSummary.rowReturnReference(etmpVatReturn.returnReference),
                NewPreviousReturnSummary.rowPaymentReference(etmpVatReturn.paymentReference)
              ).flatten
            )

            val allNiSales = PreviousReturnTotalNetValueOfSalesFromNiSummary.rows(etmpVatReturn.goodsSupplied)

            val allEuSales = PreviousReturnTotalNetValueOfSalesToEuSummary.rows(etmpVatReturn.goodsDispatched)

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

            status(result) `mustBe` OK
            contentAsString(result) `mustBe`
              view(
                determinedPeriod,
                mainSummaryList,
                allNiSales,
                allEuSales,
                correctionRowsSummaryList,
                negativeAndZeroBalanceCorrectionCountriesSummaryList,
                vatOwedSummaryList,
                displayPayNow = vatDeclared > 0 && outstandingAmount > 0,
                vatDeclared,
                returnIsExcludedAndOutstandingAmount = false,
                vatOwedInPence = (outstandingAmount * 100).toLong
              )(request, messages(application)).toString
          }
        }

        "when there are no corrections present" in {

          val etmpVatReturnNoCorrections: EtmpVatReturn = etmpVatReturn.copy(correctionPreviousVATReturn = Seq.empty)

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .configure("features.strategic-returns.enabled" -> true)
            .overrides(
              bind[VatReturnConnector].toInstance(mockVatReturnConnector),
              bind[FinancialDataConnector].toInstance(mockFinancialDataConnector)
            ).build()

          running(application) {
            when(mockVatReturnConnector.getEtmpVatReturn(any())(any())) thenReturn Right(etmpVatReturnNoCorrections).toFuture
            when(mockFinancialDataConnector.getCharge(any())(any())) thenReturn Right(Some(charge)).toFuture
            when(mockVatReturnConnector.getSavedExternalEntry()(any())) thenReturn Right(ExternalEntryUrl(None)).toFuture

            implicit val msgs: Messages = messages(application)

            val determinedPeriod: Period = fromEtmpPeriodKey(etmpVatReturnNoCorrections.periodKey)

            val request = FakeRequest(GET, routes.PreviousReturnController.onPageLoad(determinedPeriod).url)

            val result = route(application, request).value

            val view = application.injector.instanceOf[NewPreviousReturnView]

            val mainSummaryList = SummaryListViewModel(
              rows = Seq(
                NewPreviousReturnSummary.rowPayableVatDeclared(etmpVatReturnNoCorrections.totalVATAmountDueForAllMSGBP),
                NewPreviousReturnSummary.rowAmountLeftToPay(Some(charge.outstandingAmount)),
                NewPreviousReturnSummary.rowReturnSubmittedDate(etmpVatReturnNoCorrections.returnVersion),
                NewPreviousReturnSummary.rowPaymentDueDate(determinedPeriod.paymentDeadline),
                NewPreviousReturnSummary.rowReturnReference(etmpVatReturnNoCorrections.returnReference),
                NewPreviousReturnSummary.rowPaymentReference(etmpVatReturnNoCorrections.paymentReference)
              ).flatten
            )

            val allNiSales = PreviousReturnTotalNetValueOfSalesFromNiSummary.rows(etmpVatReturnNoCorrections.goodsSupplied)

            val allEuSales = PreviousReturnTotalNetValueOfSalesToEuSummary.rows(etmpVatReturnNoCorrections.goodsDispatched)

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

            status(result) `mustBe` OK
            contentAsString(result) `mustBe`
              view(
                determinedPeriod,
                mainSummaryList,
                allNiSales,
                allEuSales,
                correctionRowsSummaryList,
                negativeAndZeroBalanceCorrectionCountriesSummaryList,
                vatOwedSummaryList,
                displayPayNow = vatDeclared > 0 && outstandingAmount > 0,
                vatDeclared,
                returnIsExcludedAndOutstandingAmount = false,
                vatOwedInPence = (outstandingAmount * 100).toLong
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
                totalVATAmountCorrectionGBP = BigDecimal(200.56)
              )))

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .configure("features.strategic-returns.enabled" -> true)
            .overrides(
              bind[VatReturnConnector].toInstance(mockVatReturnConnector),
              bind[FinancialDataConnector].toInstance(mockFinancialDataConnector)
            ).build()

          running(application) {
            when(mockVatReturnConnector.getEtmpVatReturn(any())(any())) thenReturn Right(etmpVatReturnPositiveCorrections).toFuture
            when(mockFinancialDataConnector.getCharge(any())(any())) thenReturn Right(Some(charge)).toFuture
            when(mockVatReturnConnector.getSavedExternalEntry()(any())) thenReturn Right(ExternalEntryUrl(None)).toFuture

            implicit val msgs: Messages = messages(application)

            val determinedPeriod: Period = fromEtmpPeriodKey(etmpVatReturnPositiveCorrections.periodKey)

            val request = FakeRequest(GET, routes.PreviousReturnController.onPageLoad(determinedPeriod).url)

            val result = route(application, request).value

            val view = application.injector.instanceOf[NewPreviousReturnView]

            val mainSummaryList = SummaryListViewModel(
              rows = Seq(
                NewPreviousReturnSummary.rowPayableVatDeclared(etmpVatReturnPositiveCorrections.totalVATAmountDueForAllMSGBP),
                NewPreviousReturnSummary.rowAmountLeftToPay(Some(charge.outstandingAmount)),
                NewPreviousReturnSummary.rowReturnSubmittedDate(etmpVatReturnPositiveCorrections.returnVersion),
                NewPreviousReturnSummary.rowPaymentDueDate(determinedPeriod.paymentDeadline),
                NewPreviousReturnSummary.rowReturnReference(etmpVatReturnPositiveCorrections.returnReference),
                NewPreviousReturnSummary.rowPaymentReference(etmpVatReturnPositiveCorrections.paymentReference)
              ).flatten
            )

            val allNiSales = PreviousReturnTotalNetValueOfSalesFromNiSummary.rows(etmpVatReturnPositiveCorrections.goodsSupplied)

            val allEuSales = PreviousReturnTotalNetValueOfSalesToEuSummary.rows(etmpVatReturnPositiveCorrections.goodsDispatched)

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

            status(result) `mustBe` OK
            contentAsString(result) `mustBe`
              view(
                determinedPeriod,
                mainSummaryList,
                allNiSales,
                allEuSales,
                correctionRowsSummaryList,
                negativeAndZeroBalanceCorrectionCountriesSummaryList,
                vatOwedSummaryList,
                displayPayNow = vatDeclared > 0 && outstandingAmount > 0,
                vatDeclared,
                returnIsExcludedAndOutstandingAmount = false,
                vatOwedInPence = (outstandingAmount * 100).toLong
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
              bind[FinancialDataConnector].toInstance(mockFinancialDataConnector)
            ).build()

          running(application) {
            when(mockVatReturnConnector.getEtmpVatReturn(any())(any())) thenReturn Right(nilEtmpVatReturn).toFuture
            when(mockFinancialDataConnector.getCharge(any())(any())) thenReturn Right(None).toFuture

            implicit val msgs: Messages = messages(application)

            val determinedPeriod: Period = fromEtmpPeriodKey(nilEtmpVatReturn.periodKey)

            val request = FakeRequest(GET, routes.PreviousReturnController.onPageLoad(determinedPeriod).url)

            val result = route(application, request).value

            val view = application.injector.instanceOf[NewPreviousReturnView]

            val mainSummaryList = SummaryListViewModel(
              rows = Seq(
                NewPreviousReturnSummary.rowPayableVatDeclared(nilEtmpVatReturn.totalVATAmountDueForAllMSGBP),
                NewPreviousReturnSummary.rowAmountLeftToPay(None),
                NewPreviousReturnSummary.rowReturnSubmittedDate(nilEtmpVatReturn.returnVersion),
                NewPreviousReturnSummary.rowPaymentDueDate(determinedPeriod.paymentDeadline),
                NewPreviousReturnSummary.rowReturnReference(nilEtmpVatReturn.returnReference),
                NewPreviousReturnSummary.rowPaymentReference(nilEtmpVatReturn.paymentReference)
              ).flatten
            )

            val allNiSales = PreviousReturnTotalNetValueOfSalesFromNiSummary.rows(nilEtmpVatReturn.goodsSupplied)

            val allEuSales = PreviousReturnTotalNetValueOfSalesToEuSummary.rows(nilEtmpVatReturn.goodsDispatched)

            val correctionRowsSummaryList = PreviousReturnCorrectionsSummary
              .correctionRows(nilEtmpVatReturn.correctionPreviousVATReturn)

            val negativeAndZeroBalanceCorrectionCountriesSummaryList =
              PreviousReturnDeclaredVATNoPaymentDueSummary.summaryRowsOfNegativeAndZeroValues(nilEtmpVatReturn)

            val vatOwedSummaryList = SummaryListViewModel(
              rows = Seq.empty
            )

            val outstandingAmount: BigDecimal = BigDecimal(0)
            val vatDeclared = nilEtmpVatReturn.totalVATAmountDueForAllMSGBP

            status(result) `mustBe` OK
            contentAsString(result) `mustBe`
              view(
                determinedPeriod,
                mainSummaryList,
                allNiSales,
                allEuSales,
                correctionRowsSummaryList,
                negativeAndZeroBalanceCorrectionCountriesSummaryList,
                vatOwedSummaryList,
                displayPayNow = vatDeclared > 0 && outstandingAmount > 0,
                vatDeclared,
                returnIsExcludedAndOutstandingAmount = false,
                vatOwedInPence = (outstandingAmount * 100).toLong
              )(request, messages(application)).toString
          }
        }

        "when FinancialData API is down" in {

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .configure("features.strategic-returns.enabled" -> true)
            .overrides(
              bind[VatReturnConnector].toInstance(mockVatReturnConnector),
              bind[FinancialDataConnector].toInstance(mockFinancialDataConnector)
            ).build()

          running(application) {
            when(mockVatReturnConnector.getEtmpVatReturn(any())(any())) thenReturn Right(etmpVatReturn).toFuture
            when(mockFinancialDataConnector.getCharge(any())(any())) thenReturn Left(UnexpectedResponseStatus(INTERNAL_SERVER_ERROR, "ERROR")).toFuture
            when(mockVatReturnConnector.getSavedExternalEntry()(any())) thenReturn Right(ExternalEntryUrl(None)).toFuture

            implicit val msgs: Messages = messages(application)

            val determinedPeriod: Period = fromEtmpPeriodKey(etmpVatReturn.periodKey)

            val request = FakeRequest(GET, routes.PreviousReturnController.onPageLoad(determinedPeriod).url)

            val result = route(application, request).value

            val view = application.injector.instanceOf[NewPreviousReturnView]

            val mainSummaryList = SummaryListViewModel(
              rows = Seq(
                NewPreviousReturnSummary.rowPayableVatDeclared(etmpVatReturn.totalVATAmountDueForAllMSGBP),
                NewPreviousReturnSummary.rowAmountLeftToPay(None),
                NewPreviousReturnSummary.rowReturnSubmittedDate(etmpVatReturn.returnVersion),
                NewPreviousReturnSummary.rowPaymentDueDate(determinedPeriod.paymentDeadline),
                NewPreviousReturnSummary.rowReturnReference(etmpVatReturn.returnReference),
                NewPreviousReturnSummary.rowPaymentReference(etmpVatReturn.paymentReference)
              ).flatten
            )

            val allNiSales = PreviousReturnTotalNetValueOfSalesFromNiSummary.rows(etmpVatReturn.goodsSupplied)

            val allEuSales = PreviousReturnTotalNetValueOfSalesToEuSummary.rows(etmpVatReturn.goodsDispatched)

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

            status(result) `mustBe` OK
            contentAsString(result) `mustBe`
              view(
                determinedPeriod,
                mainSummaryList,
                allNiSales,
                allEuSales,
                correctionRowsSummaryList,
                negativeAndZeroBalanceCorrectionCountriesSummaryList,
                vatOwedSummaryList,
                displayPayNow = vatDeclared > 0 && outstandingAmount > 0,
                vatDeclared,
                returnIsExcludedAndOutstandingAmount = false,
                vatOwedInPence = (outstandingAmount * 100).toLong
              )(request, messages(application)).toString
          }
        }

        "when there are exclusions present and vat return due date has exceeded 3 years" - {

          val date = LocalDate.ofInstant(stubClockAtArbitraryDate.instant(), stubClockAtArbitraryDate.getZone)
          val exceededDate = date.minusYears(3).minusMonths(4)
          val exceededPeriod = getPeriod(exceededDate)
          val vatReturnNoCorrections: EtmpVatReturn = etmpVatReturn.copy(
            correctionPreviousVATReturn = Seq.empty,
            periodKey = exceededPeriod.toEtmpPeriodString,
            returnPeriodFrom = exceededPeriod.firstDay,
            returnPeriodTo = exceededPeriod.lastDay
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

              val determinedPeriod: Period = fromEtmpPeriodKey(vatReturnNoCorrections.periodKey)

              val request = FakeRequest(GET, routes.PreviousReturnController.onPageLoad(determinedPeriod).url)

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

              val allNiSales = PreviousReturnTotalNetValueOfSalesFromNiSummary.rows(vatReturnNoCorrections.goodsSupplied)

              val allEuSales = PreviousReturnTotalNetValueOfSalesToEuSummary.rows(vatReturnNoCorrections.goodsDispatched)

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

              status(result) `mustBe` OK
              contentAsString(result) `mustBe`
                view(
                  exceededPeriod,
                  mainSummaryList,
                  allNiSales,
                  allEuSales,
                  correctionRowsSummaryList,
                  negativeAndZeroBalanceCorrectionCountriesSummaryList,
                  vatOwedSummaryList,
                  displayPayNow = false,
                  vatDeclared,
                  returnIsExcludedAndOutstandingAmount = false,
                  vatOwedInPence = (outstandingAmount * 100).toLong
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

              val determinedPeriod: Period = fromEtmpPeriodKey(vatReturnNoCorrections.periodKey)

              val request = FakeRequest(GET, routes.PreviousReturnController.onPageLoad(determinedPeriod).url)

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

              val allNiSales = PreviousReturnTotalNetValueOfSalesFromNiSummary.rows(vatReturnNoCorrections.goodsSupplied)

              val allEuSales = PreviousReturnTotalNetValueOfSalesToEuSummary.rows(vatReturnNoCorrections.goodsDispatched)

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

              status(result) `mustBe` OK
              contentAsString(result) `mustBe`
                view(
                  exceededPeriod,
                  mainSummaryList,
                  allNiSales,
                  allEuSales,
                  correctionRowsSummaryList,
                  negativeAndZeroBalanceCorrectionCountriesSummaryList,
                  vatOwedSummaryList,
                  displayPayNow = false,
                  vatDeclared,
                  returnIsExcludedAndOutstandingAmount = true,
                  vatOwedInPence = (outstandingAmount * 100).toLong
                )(request, messages(application)).toString
            }
          }
        }

        "when there is a partial return period" in {

          val partialReturnPeriod: PartialReturnPeriod = arbitraryPartialReturnPeriod.arbitrary.sample.value
          val periodKey = partialReturnPeriod.toEtmpPeriodString
          val partialReturn: EtmpVatReturn = etmpVatReturn.copy(
            periodKey = periodKey,
            returnPeriodFrom = partialReturnPeriod.firstDay,
            returnPeriodTo = partialReturnPeriod.lastDay
          )

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .configure("features.strategic-returns.enabled" -> true)
            .overrides(
              bind[VatReturnConnector].toInstance(mockVatReturnConnector),
              bind[FinancialDataConnector].toInstance(mockFinancialDataConnector)
            ).build()

          running(application) {
            when(mockVatReturnConnector.getEtmpVatReturn(any())(any())) thenReturn Right(partialReturn).toFuture
            when(mockFinancialDataConnector.getCharge(any())(any())) thenReturn Right(Some(charge)).toFuture
            when(mockVatReturnConnector.getSavedExternalEntry()(any())) thenReturn Right(ExternalEntryUrl(None)).toFuture

            implicit val msgs: Messages = messages(application)

            val determinedPeriod: Period = fromEtmpPeriodKey(partialReturn.periodKey)

            val request = FakeRequest(GET, routes.PreviousReturnController.onPageLoad(determinedPeriod).url)

            val result = route(application, request).value

            val view = application.injector.instanceOf[NewPreviousReturnView]

            val mainSummaryList = SummaryListViewModel(
              rows = Seq(
                NewPreviousReturnSummary.rowPayableVatDeclared(partialReturn.totalVATAmountDueForAllMSGBP),
                NewPreviousReturnSummary.rowAmountLeftToPay(Some(charge.outstandingAmount)),
                NewPreviousReturnSummary.rowReturnSubmittedDate(partialReturn.returnVersion),
                NewPreviousReturnSummary.rowPaymentDueDate(determinedPeriod.paymentDeadline),
                NewPreviousReturnSummary.rowReturnReference(partialReturn.returnReference),
                NewPreviousReturnSummary.rowPaymentReference(partialReturn.paymentReference)
              ).flatten
            )

            val allNiSales = PreviousReturnTotalNetValueOfSalesFromNiSummary.rows(partialReturn.goodsSupplied)

            val allEuSales = PreviousReturnTotalNetValueOfSalesToEuSummary.rows(partialReturn.goodsDispatched)

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

            status(result) `mustBe` OK
            contentAsString(result) `mustBe`
              view(
                partialReturnPeriod,
                mainSummaryList,
                allNiSales,
                allEuSales,
                correctionRowsSummaryList,
                negativeAndZeroBalanceCorrectionCountriesSummaryList,
                vatOwedSummaryList,
                displayPayNow = vatDeclared > 0 && outstandingAmount > 0,
                vatDeclared,
                returnIsExcludedAndOutstandingAmount = false,
                vatOwedInPence = (outstandingAmount * 100).toLong
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
            e `mustBe` a[Exception]
            e.getMessage `mustBe` message
          }
        }
      }
    }
  }
}
