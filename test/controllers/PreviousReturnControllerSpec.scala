/*
 * Copyright 2021 HM Revenue & Customs
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
import models.Quarter.Q3
import models.{Country, Period}
import models.domain.VatReturn
import models.financialdata.Charge
import models.responses.{NotFound => NotFoundResponse}
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
import play.api.test.Helpers._
import services.VatReturnSalesService
import viewmodels.previousReturn.{PreviousReturnSummary, SaleAtVatRateSummary, TotalSalesSummary}
import viewmodels.govuk.summarylist._
import viewmodels.TitledSummaryList
import views.html.PreviousReturnView

import scala.concurrent.Future

class PreviousReturnControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val vatReturnConnector = mock[VatReturnConnector]
  private val vatReturnSalesService = mock[VatReturnSalesService]
  private val vatReturnsPaymentConnector = mock[FinancialDataConnector]

  override def beforeEach(): Unit = {
    Mockito.reset(vatReturnConnector)
    Mockito.reset(vatReturnSalesService)
    Mockito.reset(vatReturnsPaymentConnector)
    super.beforeEach()
  }

  private lazy val previousReturnRoute = routes.PreviousReturnController.onPageLoad(period).url

  private val countryFrom = arbitrary[Country].sample.value
  private val countryTo   = arbitrary[Country].sample.value

  private val vatReturn = arbitrary[VatReturn].sample.value

  private val baseAnswers =
    emptyUserAnswers
      .set(CountryOfSaleFromEuPage(index), countryFrom).success.value
      .set(CountryOfConsumptionFromEuPage(index, index), countryTo).success.value

  "Previous Return Controller" - {

    "must return OK and the correct view for a GET with no banner" in {

      val application = applicationBuilder(Some(baseAnswers))
        .overrides(
          bind[VatReturnConnector].toInstance(vatReturnConnector),
          bind[VatReturnSalesService].toInstance(vatReturnSalesService),
          bind[FinancialDataConnector].toInstance(vatReturnsPaymentConnector)
        ).build()

      val netSalesFromNi = BigDecimal(4141)
      val netSalesFromEu = BigDecimal(2333)
      val vatOnSalesFromNi = BigDecimal(55)
      val vatOnSalesFromEu = BigDecimal(44)
      val totalVatOnSales = vatOnSalesFromNi + vatOnSalesFromEu

      val clearedAmount = BigDecimal(3333.33)
      val outstandingAmount = BigDecimal(2222.22)

      val charge = Charge(Period(2021, Q3), BigDecimal(7777.77), outstandingAmount, clearedAmount)

      when(vatReturnConnector.get(any())(any())) thenReturn Future.successful(Right(vatReturn))
      when(vatReturnsPaymentConnector.getCharge(any())(any())) thenReturn Future.successful(Right(Some(charge)))
      when(vatReturnSalesService.getTotalNetSalesToCountry(any())) thenReturn netSalesFromNi
      when(vatReturnSalesService.getEuTotalNetSales(any())) thenReturn netSalesFromEu
      when(vatReturnSalesService.getTotalVatOnSalesToCountry(any())) thenReturn vatOnSalesFromNi
      when(vatReturnSalesService.getEuTotalVatOnSales(any())) thenReturn vatOnSalesFromEu
      when(vatReturnSalesService.getTotalVatOnSales(any())) thenReturn totalVatOnSales

      running(application) {
        val request = FakeRequest(GET, previousReturnRoute)

        val result = route(application, request).value

        val view                    = application.injector.instanceOf[PreviousReturnView]
        implicit val msgs: Messages = messages(application)
        val summaryList             = SummaryListViewModel(
          rows = PreviousReturnSummary.rows(vatReturn, totalVatOnSales, Some(clearedAmount), Some(outstandingAmount)))
        val niSalesList             = SaleAtVatRateSummary.getAllNiSales(vatReturn)
        val euSalesList             = SaleAtVatRateSummary.getAllEuSales(vatReturn)
        val totalSalesList          = TitledSummaryList(
          title = "All sales",
          list = SummaryListViewModel(
            TotalSalesSummary.rows(netSalesFromNi, netSalesFromEu, vatOnSalesFromNi, vatOnSalesFromEu, totalVatOnSales)
          ))
        val displayPayNow = true

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          vatReturn,
          summaryList,
          niSalesList,
          euSalesList,
          totalSalesList,
          displayPayNow,
          (charge.outstandingAmount * 100).toLong,
          false
        )(request, implicitly).toString
      }
    }

    "must return OK and the correct view for a GET with banner when charge is empty but expected" in {

      val application = applicationBuilder(Some(baseAnswers))
        .overrides(
          bind[VatReturnConnector].toInstance(vatReturnConnector),
          bind[VatReturnSalesService].toInstance(vatReturnSalesService),
          bind[FinancialDataConnector].toInstance(vatReturnsPaymentConnector)
        ).build()

      val netSalesFromNi = BigDecimal(4141)
      val netSalesFromEu = BigDecimal(2333)
      val vatOnSalesFromNi = BigDecimal(55)
      val vatOnSalesFromEu = BigDecimal(44)
      val totalVatOnSales = vatOnSalesFromNi + vatOnSalesFromEu

      val clearedAmount = BigDecimal(3333.33)
      val outstandingAmount = BigDecimal(2222.22)

      val charge = Charge(Period(2021, Q3), BigDecimal(7777.77), outstandingAmount, clearedAmount)

      when(vatReturnConnector.get(any())(any())) thenReturn Future.successful(Right(vatReturn))
      when(vatReturnsPaymentConnector.getCharge(any())(any())) thenReturn Future.successful(Right(None))
      when(vatReturnSalesService.getTotalNetSalesToCountry(any())) thenReturn netSalesFromNi
      when(vatReturnSalesService.getEuTotalNetSales(any())) thenReturn netSalesFromEu
      when(vatReturnSalesService.getTotalVatOnSalesToCountry(any())) thenReturn vatOnSalesFromNi
      when(vatReturnSalesService.getEuTotalVatOnSales(any())) thenReturn vatOnSalesFromEu
      when(vatReturnSalesService.getTotalVatOnSales(any())) thenReturn totalVatOnSales

      running(application) {
        val request = FakeRequest(GET, previousReturnRoute)

        val result = route(application, request).value

        val view                    = application.injector.instanceOf[PreviousReturnView]
        implicit val msgs: Messages = messages(application)
        val summaryList             = SummaryListViewModel(
          rows = PreviousReturnSummary.rows(vatReturn, totalVatOnSales, None, None))
        val niSalesList             = SaleAtVatRateSummary.getAllNiSales(vatReturn)
        val euSalesList             = SaleAtVatRateSummary.getAllEuSales(vatReturn)
        val totalSalesList          = TitledSummaryList(
          title = "All sales",
          list = SummaryListViewModel(
            TotalSalesSummary.rows(netSalesFromNi, netSalesFromEu, vatOnSalesFromNi, vatOnSalesFromEu, totalVatOnSales)
          ))
        val displayPayNow = true

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          vatReturn,
          summaryList,
          niSalesList,
          euSalesList,
          totalSalesList,
          displayPayNow,
          (totalVatOnSales * 100).toLong,
          true
        )(request, implicitly).toString
      }
    }

    "must return OK and the correct view for a GET without banner for nil return" in {

      val application = applicationBuilder(Some(baseAnswers))
        .overrides(
          bind[VatReturnConnector].toInstance(vatReturnConnector),
          bind[VatReturnSalesService].toInstance(vatReturnSalesService),
          bind[FinancialDataConnector].toInstance(vatReturnsPaymentConnector)
        ).build()

      val zero = BigDecimal(0)
      when(vatReturnConnector.get(any())(any())) thenReturn Future.successful(Right(vatReturn))
      when(vatReturnsPaymentConnector.getCharge(any())(any())) thenReturn Future.successful(Right(None))
      when(vatReturnSalesService.getTotalNetSalesToCountry(any())) thenReturn zero
      when(vatReturnSalesService.getEuTotalNetSales(any())) thenReturn zero
      when(vatReturnSalesService.getTotalVatOnSalesToCountry(any())) thenReturn zero
      when(vatReturnSalesService.getEuTotalVatOnSales(any())) thenReturn zero
      when(vatReturnSalesService.getTotalVatOnSales(any())) thenReturn zero

      running(application) {
        val request = FakeRequest(GET, previousReturnRoute)

        val result = route(application, request).value

        val view                    = application.injector.instanceOf[PreviousReturnView]
        implicit val msgs: Messages = messages(application)
        val summaryList             = SummaryListViewModel(
          rows = PreviousReturnSummary.rows(vatReturn, zero, None, None))
        val niSalesList             = SaleAtVatRateSummary.getAllNiSales(vatReturn)
        val euSalesList             = SaleAtVatRateSummary.getAllEuSales(vatReturn)
        val totalSalesList          = TitledSummaryList(
          title = "All sales",
          list = SummaryListViewModel(
            TotalSalesSummary.rows(zero, zero, zero, zero, zero)
          ))
        val displayPayNow = false

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          vatReturn,
          summaryList,
          niSalesList,
          euSalesList,
          totalSalesList,
          displayPayNow,
          zero.toLong,
          false
        )(request, implicitly).toString
      }
    }

    "must return OK and view without charge elements when unsuccessful ChargeResponse" in {

      val application = applicationBuilder(Some(baseAnswers))
        .overrides(
          bind[VatReturnConnector].toInstance(vatReturnConnector),
          bind[VatReturnSalesService].toInstance(vatReturnSalesService),
          bind[FinancialDataConnector].toInstance(vatReturnsPaymentConnector)
        ).build()

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
      when(vatReturnSalesService.getTotalVatOnSales(any())) thenReturn totalVatOnSales

      running(application) {
        val request = FakeRequest(GET, previousReturnRoute)

        val result = route(application, request).value

        val view                    = application.injector.instanceOf[PreviousReturnView]
        implicit val msgs: Messages = messages(application)
        val summaryList             = SummaryListViewModel(
          rows = PreviousReturnSummary.rows(vatReturn, totalVatOnSales, None, None))
        val niSalesList             = SaleAtVatRateSummary.getAllNiSales(vatReturn)
        val euSalesList             = SaleAtVatRateSummary.getAllEuSales(vatReturn)
        val totalSalesList          = TitledSummaryList(
          title = "All sales",
          list = SummaryListViewModel(
            TotalSalesSummary.rows(netSalesFromNi, netSalesFromEu, vatOnSalesFromNi, vatOnSalesFromEu, totalVatOnSales)
          ))
        val displayPayNow = true

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          vatReturn,
          summaryList,
          niSalesList,
          euSalesList,
          totalSalesList,
          displayPayNow,
          (totalVatOnSales * 100).toLong,
          true
        )(request, implicitly).toString
      }
    }
  }
}
