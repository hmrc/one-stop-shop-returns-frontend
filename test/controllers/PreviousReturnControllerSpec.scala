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
import forms.SalesToEuListFormProvider
import models.{Country, NormalMode}
import models.domain.VatReturn
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

  override def beforeEach(): Unit = {
    Mockito.reset(vatReturnConnector)
    Mockito.reset(vatReturnSalesService)
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

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(Some(baseAnswers))
        .overrides(
          bind[VatReturnConnector].toInstance(vatReturnConnector),
          bind[VatReturnSalesService].toInstance(vatReturnSalesService)
        ).build()

      val netSalesFromNi = BigDecimal(4141)
      val netSalesFromEu = BigDecimal(2333)
      val vatOnSalesFromNi = BigDecimal(55)
      val vatOnSalesFromEu = BigDecimal(44)
      val totalVatOnSales = vatOnSalesFromNi + vatOnSalesFromEu

      when(vatReturnConnector.get(any())(any())) thenReturn Future.successful(Right(vatReturn))
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
        val summaryList             = SummaryListViewModel(rows = PreviousReturnSummary.rows(vatReturn, totalVatOnSales))
        val niSalesList             = SaleAtVatRateSummary.getAllNiSales(vatReturn)
        val euSalesList             = SaleAtVatRateSummary.getAllEuSales(vatReturn)
        val totalSalesList          = TitledSummaryList(
          title = "All sales",
          list = SummaryListViewModel(
            TotalSalesSummary.rows(netSalesFromNi, netSalesFromEu, vatOnSalesFromNi, vatOnSalesFromEu, totalVatOnSales)
          ))

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(vatReturn, summaryList, niSalesList, euSalesList, totalSalesList)(request, implicitly).toString
      }
    }

  }
}
