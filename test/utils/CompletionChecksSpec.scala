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

package utils

import base.SpecBase
import config.Constants.{maxCurrencyAmount, minCurrencyAmount}
import models.VatRateType.Standard
import models._
import models.corrections.{CorrectionToCountry, PeriodWithCorrections}
import models.requests.DataRequest
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.AnyContent
import play.api.test.FakeRequest
import play.api.test.Helpers.running
import queries._
import queries.corrections._

import java.time.LocalDate

class CompletionChecksSpec extends SpecBase with MockitoSugar {

  object TestCompletionChecks extends CompletionChecks

  implicit val request: DataRequest[AnyContent] = mock[DataRequest[AnyContent]]
  private val country = arbitrary[Country].sample.value
  private val vatRate = 20
  private val vatOnSales = 100
  private val completeCorrection = CorrectionToCountry(country, Some(BigDecimal(vatOnSales)))
  private val incompleteCorrection = CorrectionToCountry(country, None)
  private val salesValue: BigDecimal = Gen.chooseNum(minCurrencyAmount, maxCurrencyAmount).sample.value
  private val vatOnSalesValue: VatOnSales = arbitraryVatOnSales.arbitrary.sample.value
  private val periodYear = 2023
  private val period1 = PeriodWithCorrections(StandardPeriod(periodYear, Quarter.Q1), Some(List(completeCorrection)))
  private val period2 = PeriodWithCorrections(StandardPeriod(periodYear, Quarter.Q2), Some(List(incompleteCorrection)))

  private val completedVatRate = VatRateAndSalesWithOptionalVat(
    rate = BigDecimal(vatRate),
    rateType = Standard,
    validFrom = LocalDate.now,
    validUntil = Some(LocalDate.now.plusDays(1)),
    sales = Some(SalesAtVatRateWithOptionalVat(
      netValueOfSales = salesValue,
      vatOnSales = Some(vatOnSalesValue)
    ))
  )

  private val salesFromCountry = SalesFromCountryWithOptionalVat(
    countryOfConsumption = country,
    vatRates = Some(List(completedVatRate))
  )
  private val incompleteSalesFromCountry = SalesFromCountryWithOptionalVat(
    countryOfConsumption = country,
    vatRates = None
  )
  private val salesFromEuCountry = SalesFromEuWithOptionalVat(
    countryOfSale = country,
    salesFromCountry = Some(List(salesFromCountry))
  )
  private val incompleteSalesFromEuCountry = SalesFromEuWithOptionalVat(
    countryOfSale = country,
    salesFromCountry = None
  )


  "CompletionChecks" - {

    "getIncompleteCorrectionsToCountry" - {

      "return None if all corrections are complete" in {

        val userAnswers = emptyUserAnswers
          .set(CorrectionToCountryQuery(index, index), completeCorrection).success.value

        val application = applicationBuilder(Some(userAnswers)).build()

        running(application) {
          implicit val request: DataRequest[AnyContent] = DataRequest(FakeRequest(), testCredentials, vrn, registration, userAnswers)

          val result = TestCompletionChecks.getIncompleteCorrectionToCountry(index, index)

          result mustBe None
        }
      }

      "return Some if there are incomplete corrections" in {

        val userAnswers = emptyUserAnswers
          .set(CorrectionToCountryQuery(index, index), incompleteCorrection).success.value

        val application = applicationBuilder(Some(userAnswers)).build()

        running(application) {
          implicit val request: DataRequest[AnyContent] = DataRequest(FakeRequest(), testCredentials, vrn, registration, userAnswers)

          val result = TestCompletionChecks.getIncompleteCorrectionToCountry(index, index)

          result mustBe Some(incompleteCorrection)
        }
      }
    }

    "getIncompleteCorrections" - {

      "return an empty list if all corrections are complete" in {

        val userAnswers = emptyUserAnswers
          .set(AllCorrectionCountriesQuery(index), List(completeCorrection)).success.value

        val application = applicationBuilder(Some(userAnswers)).build()

        running(application) {
          implicit val request: DataRequest[AnyContent] = DataRequest(FakeRequest(), testCredentials, vrn, registration, userAnswers)

          val result = TestCompletionChecks.getIncompleteCorrections(index)

          result mustBe empty
        }
      }

      "return a list of incomplete corrections" in {

        val userAnswers = emptyUserAnswers
          .set(AllCorrectionCountriesQuery(index), List(incompleteCorrection)).success.value

        val application = applicationBuilder(Some(userAnswers)).build()

        running(application) {
          implicit val request: DataRequest[AnyContent] = DataRequest(FakeRequest(), testCredentials, vrn, registration, userAnswers)

          val result = TestCompletionChecks.getIncompleteCorrections(index)

          result mustBe List(incompleteCorrection)
        }
      }
    }

    "getPeriodsWithIncompleteCorrections" - {

      "return an empty list if all periods have complete corrections" in {

        val userAnswers = emptyUserAnswers
          .set(AllCorrectionPeriodsQuery, List(period1)).success.value

        val application = applicationBuilder(Some(userAnswers)).build()

        running(application) {
          implicit val request: DataRequest[AnyContent] = DataRequest(FakeRequest(), testCredentials, vrn, registration, userAnswers)

          val result = TestCompletionChecks.getPeriodsWithIncompleteCorrections()

          result mustBe empty
        }
      }

      "return a list of periods with incomplete corrections" in {

        val userAnswers = emptyUserAnswers
          .set(AllCorrectionPeriodsQuery, List(period1, period2)).success.value

        val application = applicationBuilder(Some(userAnswers)).build()

        running(application) {
          implicit val request: DataRequest[AnyContent] = DataRequest(FakeRequest(), testCredentials, vrn, registration, userAnswers)

          val result = TestCompletionChecks.getPeriodsWithIncompleteCorrections()

          result must contain theSameElementsAs List(period2.correctionReturnPeriod)
        }
      }

      "return an empty list if there are no periods at all" in {

        val application = applicationBuilder(Some(emptyUserAnswers)).build()

        running(application) {
          implicit val request: DataRequest[AnyContent] = DataRequest(FakeRequest(), testCredentials, vrn, registration, emptyUserAnswers)

          val result = TestCompletionChecks.getPeriodsWithIncompleteCorrections()

          result mustBe empty
        }
      }
    }

    "firstIndexedIncompleteCorrection" - {

      "return None if there are no incomplete corrections" in {

        val userAnswers = emptyUserAnswers
          .set(AllCorrectionCountriesQuery(index), List(completeCorrection)).success.value

        val application = applicationBuilder(Some(userAnswers)).build()

        running(application) {
          implicit val request: DataRequest[AnyContent] = DataRequest(FakeRequest(), testCredentials, vrn, registration, userAnswers)

          val result = TestCompletionChecks.firstIndexedIncompleteCorrection(index, List(CorrectionToCountry(country, None)))

          result mustBe None
        }
      }

      "return the first incomplete correction with its index" in {

        val userAnswers = emptyUserAnswers
          .set(AllCorrectionCountriesQuery(index), List(incompleteCorrection)).success.value

        val application = applicationBuilder(Some(userAnswers)).build()

        running(application) {
          implicit val request: DataRequest[AnyContent] = DataRequest(FakeRequest(), testCredentials, vrn, registration, userAnswers)

          val result = TestCompletionChecks.firstIndexedIncompleteCorrection(index, List(incompleteCorrection))

          result mustBe Some((incompleteCorrection, 0))
        }
      }
    }

    "getNiCountriesWithIncompleteSales" - {

      "return an empty sequence if all countries have complete sales data" in {

        val userAnswers = emptyUserAnswers
          .set(AllSalesFromNiWithOptionalVatQuery, List(salesFromCountry)).success.value

        val application = applicationBuilder(Some(userAnswers)).build()

        running(application) {
          implicit val request: DataRequest[AnyContent] = DataRequest(FakeRequest(), testCredentials, vrn, registration, userAnswers)

          val result = TestCompletionChecks.getNiCountriesWithIncompleteSales()

          result mustBe empty
        }
      }

      "return a sequence of countries with incomplete sales data" in {

        val userAnswers = emptyUserAnswers
          .set(AllSalesFromNiWithOptionalVatQuery, List(incompleteSalesFromCountry)).success.value

        val application = applicationBuilder(Some(userAnswers)).build()

        running(application) {
          implicit val request: DataRequest[AnyContent] = DataRequest(FakeRequest(), testCredentials, vrn, registration, userAnswers)

          val result = TestCompletionChecks.getNiCountriesWithIncompleteSales()

          result must contain theSameElementsAs List(country)
        }
      }
    }

    "getIncompleteNiVatRateAndSales" - {

      "return an empty sequence if all sales and VAT are complete" in {

        val userAnswers = emptyUserAnswers
          .set(AllNiVatRateAndSalesWithOptionalVatQuery(index), List(completedVatRate)).success.value

        val application = applicationBuilder(Some(userAnswers)).build()

        running(application) {
          implicit val request: DataRequest[AnyContent] = DataRequest(FakeRequest(), testCredentials, vrn, registration, userAnswers)

          val result = TestCompletionChecks.getIncompleteNiVatRateAndSales(index)

          result mustBe empty
        }
      }

      "return a sequence of sales with no VAT or no sales" in {

        val noSales = VatRateAndSalesWithOptionalVat(
          rate = BigDecimal(vatRate),
          rateType = Standard,
          validFrom = LocalDate.now,
          validUntil = Some(LocalDate.now.plusDays(1)),
          sales = None
        )

        val noVat = VatRateAndSalesWithOptionalVat(
          rate = BigDecimal(vatRate),
          rateType = Standard,
          validFrom = LocalDate.now,
          validUntil = Some(LocalDate.now.plusDays(1)),
          sales = Some(SalesAtVatRateWithOptionalVat(BigDecimal(vatOnSales), None))
        )

        val userAnswers = emptyUserAnswers
          .set(AllNiVatRateAndSalesWithOptionalVatQuery(index), List(noSales, noVat)).success.value

        val application = applicationBuilder(Some(userAnswers)).build()

        running(application) {
          implicit val request: DataRequest[AnyContent] = DataRequest(FakeRequest(), testCredentials, vrn, registration, userAnswers)

          val result = TestCompletionChecks.getIncompleteNiVatRateAndSales(index)

          result must contain theSameElementsAs Seq(noSales, noVat)
        }
      }
    }

    "firstIndexedIncompleteNiCountrySales" - {

      "return None if there are no incomplete country sales" in {

        val userAnswers = emptyUserAnswers
          .set(AllSalesFromNiWithOptionalVatQuery, List(salesFromCountry)).success.value

        val application = applicationBuilder(Some(userAnswers)).build()

        running(application) {
          implicit val request: DataRequest[AnyContent] = DataRequest(FakeRequest(), testCredentials, vrn, registration, userAnswers)

          val result = TestCompletionChecks.firstIndexedIncompleteNiCountrySales(List(Country("FR", "France")))

          result mustBe None
        }
      }

      "return the first incomplete country sales with its index" in {

        val userAnswers = emptyUserAnswers
          .set(AllSalesFromNiWithOptionalVatQuery, List(incompleteSalesFromCountry)).success.value

        val application = applicationBuilder(Some(userAnswers)).build()

        running(application) {
          implicit val request: DataRequest[AnyContent] = DataRequest(FakeRequest(), testCredentials, vrn, registration, userAnswers)

          val result = TestCompletionChecks.firstIndexedIncompleteNiCountrySales(List(country))

          result mustBe Some((incompleteSalesFromCountry, 0))
        }
      }
    }

    "getIncompleteToEuSales" - {

      "return an empty sequence if all countries have complete sales data" in {

        val userAnswers = emptyUserAnswers
          .set(AllSalesToEuWithOptionalVatQuery(index), List(salesFromCountry)).success.value

        val application = applicationBuilder(Some(userAnswers)).build()

        running(application) {
          implicit val request: DataRequest[AnyContent] = DataRequest(FakeRequest(), testCredentials, vrn, registration, userAnswers)

          val result = TestCompletionChecks.getIncompleteToEuSales(index)

          result mustBe empty
        }
      }

      "return a sequence of countries with incomplete sales data" in {

        val userAnswers = emptyUserAnswers
          .set(AllSalesToEuWithOptionalVatQuery(index), List(incompleteSalesFromCountry)).success.value

        val application = applicationBuilder(Some(userAnswers)).build()

        running(application) {
          implicit val request: DataRequest[AnyContent] = DataRequest(FakeRequest(), testCredentials, vrn, registration, userAnswers)

          val result = TestCompletionChecks.getIncompleteToEuSales(index)

          result must contain theSameElementsAs List(incompleteSalesFromCountry)
        }
      }
    }

    "firstIndexedIncompleteSaleToEu" - {

      "return None if there are no incomplete country sales" in {

        val userAnswers = emptyUserAnswers
          .set(AllSalesToEuWithOptionalVatQuery(index), List(salesFromCountry)).success.value

        val application = applicationBuilder(Some(userAnswers)).build()

        running(application) {
          implicit val request: DataRequest[AnyContent] = DataRequest(FakeRequest(), testCredentials, vrn, registration, userAnswers)

          val result = TestCompletionChecks.firstIndexedIncompleteSaleToEu(index)

          result mustBe None
        }
      }

      "return the first incomplete country sales with its index" in {

        val userAnswers = emptyUserAnswers
          .set(AllSalesToEuWithOptionalVatQuery(index), List(incompleteSalesFromCountry)).success.value

        val application = applicationBuilder(Some(userAnswers)).build()

        running(application) {
          implicit val request: DataRequest[AnyContent] = DataRequest(FakeRequest(), testCredentials, vrn, registration, userAnswers)

          val result = TestCompletionChecks.firstIndexedIncompleteSaleToEu(index)

          result mustBe Some((incompleteSalesFromCountry, 0))
        }
      }
    }

    "getIncompleteFromEuSales" - {

      "return an empty sequence if all countries have complete sales data" in {

        val userAnswers = emptyUserAnswers
          .set(AllSalesFromEuQueryWithOptionalVatQuery, List(salesFromEuCountry)).success.value

        val application = applicationBuilder(Some(userAnswers)).build()

        running(application) {
          implicit val request: DataRequest[AnyContent] = DataRequest(FakeRequest(), testCredentials, vrn, registration, userAnswers)

          val result = TestCompletionChecks.getIncompleteFromEuSales()

          result mustBe empty
        }
      }

      "return a sequence of countries with incomplete sales data" in {

        val userAnswers = emptyUserAnswers
          .set(AllSalesFromEuQueryWithOptionalVatQuery, List(incompleteSalesFromEuCountry)).success.value

        val application = applicationBuilder(Some(userAnswers)).build()

        running(application) {
          implicit val request: DataRequest[AnyContent] = DataRequest(FakeRequest(), testCredentials, vrn, registration, userAnswers)

          val result = TestCompletionChecks.getIncompleteFromEuSales()

          result must contain theSameElementsAs List(incompleteSalesFromEuCountry)
        }
      }
    }

    "firstIndexedIncompleteSaleFromEu" - {

      "return None if there are no incomplete country sales" in {

        val userAnswers = emptyUserAnswers
          .set(AllSalesFromEuQueryWithOptionalVatQuery, List(salesFromEuCountry)).success.value

        val application = applicationBuilder(Some(userAnswers)).build()

        running(application) {
          implicit val request: DataRequest[AnyContent] = DataRequest(FakeRequest(), testCredentials, vrn, registration, userAnswers)

          val result = TestCompletionChecks.firstIndexedIncompleteSaleFromEu()

          result mustBe None
        }
      }

      "return the first incomplete country sales with its index" in {

        val userAnswers = emptyUserAnswers
          .set(AllSalesFromEuQueryWithOptionalVatQuery, List(incompleteSalesFromEuCountry)).success.value

        val application = applicationBuilder(Some(userAnswers)).build()

        running(application) {
          implicit val request: DataRequest[AnyContent] = DataRequest(FakeRequest(), testCredentials, vrn, registration, userAnswers)

          val result = TestCompletionChecks.firstIndexedIncompleteSaleFromEu()

          result mustBe Some((incompleteSalesFromEuCountry, 0))
        }
      }
    }
  }

}
