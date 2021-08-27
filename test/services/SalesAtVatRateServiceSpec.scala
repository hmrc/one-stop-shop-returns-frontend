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

package services

import base.SpecBase
import models.{Country, Index, SalesAtVatRate, VatRate, VatRateType}
import pages._

class SalesAtVatRateServiceSpec extends SpecBase {

  private val index0 = Index(0)
  private val index1 = Index(1)

  val service = new SalesAtVatRateService()

  "SalesAtVatRateService" - {

    "getNiTotalVatOnSales" - {

      "must show correct vat total for one country with one vat rate" in {
        service.getNiTotalVatOnSales(completeSalesFromNIUserAnswers) mustBe Some(BigDecimal(1000))
      }

      "must show correct vat total sales for one country with multiple vat rates" in {
        val answers = completeSalesFromNIUserAnswers
          .set(
            VatRatesFromNiPage(index),
            List(
              VatRate(10, VatRateType.Reduced, arbitraryDate),
              VatRate(20, VatRateType.Reduced, arbitraryDate)
            )
          ).success.value
          .set(SalesAtVatRateFromNiPage(index, index), SalesAtVatRate(BigDecimal(100), BigDecimal(200))).success.value
          .set(SalesAtVatRateFromNiPage(index, index + 1), SalesAtVatRate(BigDecimal(100), BigDecimal(400))).success.value

        service.getNiTotalVatOnSales(answers) mustBe Some(BigDecimal(600))
      }

      "must show correct vat total sales for multiple countries with vat rates" in {
        val answers = completeSalesFromNIUserAnswers
          .set(
            VatRatesFromNiPage(index),
            List(
              VatRate(10, VatRateType.Reduced, arbitraryDate),
              VatRate(20, VatRateType.Reduced, arbitraryDate)
            )
          ).success.value
          .set(SalesAtVatRateFromNiPage(index, index), SalesAtVatRate(BigDecimal(100), BigDecimal(200))).success.value
          .set(SalesAtVatRateFromNiPage(index, index + 1), SalesAtVatRate(BigDecimal(300), BigDecimal(400))).success.value
          .set(CountryOfConsumptionFromNiPage(index + 1), Country("OTH", "OtherCountry")).success.value
          .set(VatRatesFromNiPage(index + 1), List(VatRate(10, VatRateType.Reduced, arbitraryDate))).success.value
          .set(SalesAtVatRateFromNiPage(index + 1, index), SalesAtVatRate(BigDecimal(100), BigDecimal(1000))).success.value

        service.getNiTotalVatOnSales(answers) mustBe Some(BigDecimal(1600))
      }
    }

    "getNiTotalNetSales" - {

      "must show correct net total sales for one country with one vat rate" in {
        service.getNiTotalNetSales(completeUserAnswers) mustBe Some(BigDecimal(100))
      }

      "must show correct net total sales for one country with multiple vat rates" in {
        val answers = completeSalesFromNIUserAnswers
          .set(
            VatRatesFromNiPage(index),
            List(
              VatRate(10, VatRateType.Reduced, arbitraryDate),
              VatRate(20, VatRateType.Reduced, arbitraryDate)
            )
          ).success.value
          .set(SalesAtVatRateFromNiPage(index, index), SalesAtVatRate(BigDecimal(100), BigDecimal(200))).success.value
          .set(SalesAtVatRateFromNiPage(index, index + 1), SalesAtVatRate(BigDecimal(300), BigDecimal(400))).success.value

        service.getNiTotalNetSales(answers) mustBe Some(BigDecimal(400))
      }

      "must show correct net total sales for multiple countries with vat rates" in {
        val answers = completeSalesFromNIUserAnswers
          .set(
            VatRatesFromNiPage(index),
            List(
              VatRate(10, VatRateType.Reduced, arbitraryDate),
              VatRate(20, VatRateType.Reduced, arbitraryDate)
            )
          ).success.value
          .set(SalesAtVatRateFromNiPage(index, index), SalesAtVatRate(BigDecimal(100), BigDecimal(200))).success.value
          .set(SalesAtVatRateFromNiPage(index, index + 1), SalesAtVatRate(BigDecimal(300), BigDecimal(400))).success.value
          .set(CountryOfConsumptionFromNiPage(index + 1), Country("OTH", "OtherCountry")).success.value
          .set(VatRatesFromNiPage(index + 1), List(VatRate(10, VatRateType.Reduced, arbitraryDate))).success.value
          .set(SalesAtVatRateFromNiPage(index + 1, index), SalesAtVatRate(BigDecimal(100), BigDecimal(1000))).success.value

        service.getNiTotalNetSales(answers) mustBe Some(BigDecimal(500))
      }
    }

    "getEuTotalVatOnSales" - {

      "must show correct total vat from one country, to one country, with one vat rate" in {
        service.getEuTotalVatOnSales(completeUserAnswers) mustBe Some(BigDecimal(20))
      }

      "must show correct total vat from one country, to one country with multiple vat rates" in {
        val answers = completeUserAnswers
          .set(VatRatesFromEuPage(index0, index0), List(twentyPercentVatRate, fivePercentVatRate)).success.value
          .set(SalesAtVatRateFromEuPage(index0, index0, index0), SalesAtVatRate(BigDecimal(100), BigDecimal(20))).success.value
          .set(SalesAtVatRateFromEuPage(index0, index0, index1), SalesAtVatRate(BigDecimal(200), BigDecimal(20))).success.value

        service.getEuTotalVatOnSales(answers) mustBe Some(BigDecimal(40))
      }

      "must show correct total vat from one country, to multiple countries with multiple vat rates" in {
        val answers = completeUserAnswers
          .set(VatRatesFromEuPage(index0, index0), List(twentyPercentVatRate, fivePercentVatRate)).success.value
          .set(SalesAtVatRateFromEuPage(index0, index0, index0), SalesAtVatRate(BigDecimal(100), BigDecimal(20))).success.value
          .set(SalesAtVatRateFromEuPage(index0, index0, index1), SalesAtVatRate(BigDecimal(200), BigDecimal(20))).success.value
          .set(CountryOfConsumptionFromEuPage(index0, index1), Country("DK", "Denmark")).success.value
          .set(VatRatesFromEuPage(index0, index1), List(twentyPercentVatRate)).success.value
          .set(SalesAtVatRateFromEuPage(index0, index1, index0), SalesAtVatRate(BigDecimal(100), BigDecimal(20))).success.value

        service.getEuTotalVatOnSales(answers) mustBe Some(BigDecimal(60))
      }

      "must show correct total vat from multiple countries, to multiple countries with multiple vat rates" in {
        val answers = emptyUserAnswers
          .set(SoldGoodsFromEuPage,true).success.value
          //countries from
          .set(CountryOfSaleFromEuPage(index0), Country("HR", "Croatia")).success.value
          .set(CountryOfSaleFromEuPage(index1), Country("EE", "Estonia")).success.value

          //countries to
          .set(CountryOfConsumptionFromEuPage(index0, index0), Country("BE", "Belgium")).success.value
          .set(CountryOfConsumptionFromEuPage(index0, index1), Country("DK", "Denmark")).success.value
          .set(CountryOfConsumptionFromEuPage(index1, index0), Country("BE", "Belgium")).success.value
          .set(CountryOfConsumptionFromEuPage(index1, index1), Country("DK", "Denmark")).success.value

          //vat rates
          .set(VatRatesFromEuPage(index0, index0), List(twentyPercentVatRate)).success.value
          .set(VatRatesFromEuPage(index0, index1), List(twentyPercentVatRate, fivePercentVatRate)).success.value
          .set(VatRatesFromEuPage(index1, index0), List(twentyPercentVatRate)).success.value
          .set(VatRatesFromEuPage(index1, index1), List(twentyPercentVatRate, fivePercentVatRate)).success.value

          //sales at vat rate
          .set(SalesAtVatRateFromEuPage(index0, index0, index0), SalesAtVatRate(BigDecimal(100), BigDecimal(20))).success.value
          .set(SalesAtVatRateFromEuPage(index0, index1, index0), SalesAtVatRate(BigDecimal(200), BigDecimal(20))).success.value
          .set(SalesAtVatRateFromEuPage(index0, index1, index1), SalesAtVatRate(BigDecimal(200), BigDecimal(20))).success.value
          .set(SalesAtVatRateFromEuPage(index1, index0, index0), SalesAtVatRate(BigDecimal(300), BigDecimal(20))).success.value
          .set(SalesAtVatRateFromEuPage(index1, index1, index0), SalesAtVatRate(BigDecimal(400), BigDecimal(20))).success.value
          .set(SalesAtVatRateFromEuPage(index1, index1, index1), SalesAtVatRate(BigDecimal(400), BigDecimal(20))).success.value

        service.getEuTotalVatOnSales(answers) mustBe Some(BigDecimal(120))
      }
    }

    "getEuTotalNetSales" - {

      "must show correct net total sales for one country from, one country to with one vat rate" in {
        service.getEuTotalNetSales(completeUserAnswers) mustBe Some(BigDecimal(100))
      }

      "must show correct net total sales for one country from, one country to with multiple vat rates" in {
        val answers = completeUserAnswers
          .set(VatRatesFromEuPage(index, index), List(twentyPercentVatRate, fivePercentVatRate)).success.value
          .set(SalesAtVatRateFromEuPage(index, index, index), SalesAtVatRate(BigDecimal(100), BigDecimal(20))).success.value
          .set(SalesAtVatRateFromEuPage(index, index, index + 1), SalesAtVatRate(BigDecimal(200), BigDecimal(20))).success.value

        service.getEuTotalNetSales(answers) mustBe Some(BigDecimal(300))
      }

      "must show correct net total sales for one country from, multiple countries to with multiple vat rates" in {
        val answers = completeUserAnswers
          .set(VatRatesFromEuPage(index, index), List(twentyPercentVatRate, fivePercentVatRate)).success.value
          .set(SalesAtVatRateFromEuPage(index, index, index), SalesAtVatRate(BigDecimal(100), BigDecimal(20))).success.value
          .set(SalesAtVatRateFromEuPage(index, index, index + 1), SalesAtVatRate(BigDecimal(200), BigDecimal(20))).success.value
          .set(CountryOfConsumptionFromEuPage(index, index + 1), Country("DK", "Denmark")).success.value
          .set(VatRatesFromEuPage(index, index + 1), List(twentyPercentVatRate)).success.value
          .set(SalesAtVatRateFromEuPage(index, index + 1, index), SalesAtVatRate(BigDecimal(100), BigDecimal(20))).success.value

        service.getEuTotalNetSales(answers) mustBe Some(BigDecimal(400))
      }

      "must show correct net total sales for multiple country from, multiple countries to with multiple vat rates" in {
        val answers = emptyUserAnswers
          .set(SoldGoodsFromEuPage,true).success.value
          //countries from
          .set(CountryOfSaleFromEuPage(index0), Country("HR", "Croatia")).success.value
          .set(CountryOfSaleFromEuPage(index1), Country("EE", "Estonia")).success.value

          //countries to
          .set(CountryOfConsumptionFromEuPage(index0, index0), Country("BE", "Belgium")).success.value
          .set(CountryOfConsumptionFromEuPage(index0, index1), Country("DK", "Denmark")).success.value
          .set(CountryOfConsumptionFromEuPage(index1, index0), Country("BE", "Belgium")).success.value
          .set(CountryOfConsumptionFromEuPage(index1, index1), Country("DK", "Denmark")).success.value

          //vat rates
          .set(VatRatesFromEuPage(index0, index0), List(twentyPercentVatRate)).success.value
          .set(VatRatesFromEuPage(index0, index1), List(twentyPercentVatRate)).success.value
          .set(VatRatesFromEuPage(index1, index0), List(twentyPercentVatRate)).success.value
          .set(VatRatesFromEuPage(index1, index1), List(twentyPercentVatRate)).success.value

          //sales at vat rate
          .set(SalesAtVatRateFromEuPage(index0, index0, index0), SalesAtVatRate(BigDecimal(100), BigDecimal(20))).success.value
          .set(SalesAtVatRateFromEuPage(index0, index1, index0), SalesAtVatRate(BigDecimal(200), BigDecimal(20))).success.value
          .set(SalesAtVatRateFromEuPage(index1, index0, index0), SalesAtVatRate(BigDecimal(300), BigDecimal(20))).success.value
          .set(SalesAtVatRateFromEuPage(index1, index1, index0), SalesAtVatRate(BigDecimal(400), BigDecimal(20))).success.value

        service.getEuTotalNetSales(answers) mustBe Some(BigDecimal(1000))
      }
    }

    "getTotalNetSales" - {

      "must return correct total when NI and EU sales exist" in {

        service.getTotalNetSales(completeUserAnswers) mustBe BigDecimal(200)
      }

      "must return zero when total NI and EU sales don't exist" in {

        service.getTotalNetSales(emptyUserAnswers) mustBe BigDecimal(0)
      }

      "must return total when NI exists and EU sales don't exist" in {

        service.getTotalNetSales(completeSalesFromNIUserAnswers) mustBe BigDecimal(100)
      }

      "must return total when NI doesn't exist and EU does exist" in {
        val answers = emptyUserAnswers
          .set(SoldGoodsFromEuPage,true).success.value
          .set(CountryOfSaleFromEuPage(index), Country("HR", "Croatia")).success.value
          .set(CountryOfConsumptionFromEuPage(index, index), Country("BE", "Belgium")).success.value
          .set(VatRatesFromEuPage(index, index), List(twentyPercentVatRate)).success.value
          .set(SalesAtVatRateFromEuPage(index, index, index), SalesAtVatRate(BigDecimal(100), BigDecimal(20))).success.value

        service.getTotalNetSales(answers) mustBe BigDecimal(100)
      }
    }

    "getTotalVatOnSales" - {

      "must return correct total when NI and EU sales exist" in {

        service.getTotalVatOnSales(completeUserAnswers) mustBe BigDecimal(1020)
      }

      "must return zero when total NI and EU sales don't exist" in {

        service.getTotalVatOnSales(emptyUserAnswers) mustBe BigDecimal(0)
      }

      "must return total when NI exists and EU sales don't exist" in {

        service.getTotalVatOnSales(completeSalesFromNIUserAnswers) mustBe BigDecimal(1000)
      }

      "must return total when NI doesn't exist and EU does exist" in {
        val answers = emptyUserAnswers
          .set(SoldGoodsFromEuPage,true).success.value
          .set(CountryOfSaleFromEuPage(index), Country("HR", "Croatia")).success.value
          .set(CountryOfConsumptionFromEuPage(index, index), Country("BE", "Belgium")).success.value
          .set(VatRatesFromEuPage(index, index), List(twentyPercentVatRate)).success.value
          .set(SalesAtVatRateFromEuPage(index, index, index), SalesAtVatRate(BigDecimal(100), BigDecimal(20))).success.value

        service.getTotalVatOnSales(answers) mustBe BigDecimal(20)
      }
    }
  }
}
