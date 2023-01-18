/*
 * Copyright 2023 HM Revenue & Customs
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
import models.VatOnSalesChoice.Standard
import models.{Country, Index, TotalVatToCountry, VatOnSales, VatRate, VatRateType}
import org.scalatestplus.mockito.MockitoSugar
import pages._
import pages.corrections.{CorrectionCountryPage, CorrectionReturnPeriodPage, CountryVatCorrectionPage}

class SalesAtVatRateServiceSpec extends SpecBase with MockitoSugar {

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
            VatRatesFromNiPage(index0),
            List(
              VatRate(10, VatRateType.Reduced, arbitraryDate),
              VatRate(20, VatRateType.Reduced, arbitraryDate)
            )
          ).success.value
          .set(NetValueOfSalesFromNiPage(index0, index0), BigDecimal(100)).success.value
          .set(VatOnSalesFromNiPage(index0, index0), VatOnSales(Standard, BigDecimal(200))).success.value
          .set(NetValueOfSalesFromNiPage(index0, index1), BigDecimal(300)).success.value
          .set(VatOnSalesFromNiPage(index0, index1), VatOnSales(Standard, BigDecimal(400))).success.value

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
          .set(NetValueOfSalesFromNiPage(index0, index0), BigDecimal(100)).success.value
          .set(VatOnSalesFromNiPage(index0, index0), VatOnSales(Standard, BigDecimal(200))).success.value
          .set(NetValueOfSalesFromNiPage(index0, index1), BigDecimal(300)).success.value
          .set(VatOnSalesFromNiPage(index0, index1), VatOnSales(Standard, BigDecimal(400))).success.value
          .set(CountryOfConsumptionFromNiPage(index1), Country("OTH", "OtherCountry")).success.value
          .set(VatRatesFromNiPage(index1), List(VatRate(10, VatRateType.Reduced, arbitraryDate))).success.value
          .set(NetValueOfSalesFromNiPage(index1, index0), BigDecimal(100)).success.value
          .set(VatOnSalesFromNiPage(index1, index0), VatOnSales(Standard, BigDecimal(1000))).success.value

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
            VatRatesFromNiPage(index0),
            List(
              VatRate(10, VatRateType.Reduced, arbitraryDate),
              VatRate(20, VatRateType.Reduced, arbitraryDate)
            )
          ).success.value
          .set(NetValueOfSalesFromNiPage(index0, index0), BigDecimal(100)).success.value
          .set(VatOnSalesFromNiPage(index0, index0), VatOnSales(Standard, BigDecimal(200))).success.value
          .set(NetValueOfSalesFromNiPage(index0, index1), BigDecimal(300)).success.value
          .set(VatOnSalesFromNiPage(index0, index1), VatOnSales(Standard, BigDecimal(400))).success.value

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
          .set(NetValueOfSalesFromNiPage(index0, index0), BigDecimal(100)).success.value
          .set(VatOnSalesFromNiPage(index0, index0), VatOnSales(Standard, BigDecimal(200))).success.value
          .set(NetValueOfSalesFromNiPage(index0, index1), BigDecimal(300)).success.value
          .set(VatOnSalesFromNiPage(index0, index1), VatOnSales(Standard, BigDecimal(400))).success.value
          .set(CountryOfConsumptionFromNiPage(index1), Country("OTH", "OtherCountry")).success.value
          .set(VatRatesFromNiPage(index1), List(VatRate(10, VatRateType.Reduced, arbitraryDate))).success.value
          .set(NetValueOfSalesFromNiPage(index1, index0), BigDecimal(100)).success.value
          .set(VatOnSalesFromNiPage(index1, index0), VatOnSales(Standard, BigDecimal(1000))).success.value

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
          .set(NetValueOfSalesFromEuPage(index0, index0, index0), BigDecimal(100)).success.value
          .set(VatOnSalesFromEuPage(index0, index0, index0), VatOnSales(Standard, BigDecimal(20))).success.value
          .set(NetValueOfSalesFromEuPage(index0, index0, index1), BigDecimal(200)).success.value
          .set(VatOnSalesFromEuPage(index0, index0, index1), VatOnSales(Standard, BigDecimal(20))).success.value

        service.getEuTotalVatOnSales(answers) mustBe Some(BigDecimal(40))
      }

      "must show correct total vat from one country, to multiple countries with multiple vat rates" in {
        val answers = completeUserAnswers
          .set(VatRatesFromEuPage(index0, index0), List(twentyPercentVatRate, fivePercentVatRate)).success.value
          .set(NetValueOfSalesFromEuPage(index0, index0, index0), BigDecimal(100)).success.value
          .set(VatOnSalesFromEuPage(index0, index0, index0), VatOnSales(Standard, BigDecimal(20))).success.value
          .set(NetValueOfSalesFromEuPage(index0, index0, index1), BigDecimal(200)).success.value
          .set(VatOnSalesFromEuPage(index0, index0, index1), VatOnSales(Standard, BigDecimal(20))).success.value
          .set(CountryOfConsumptionFromEuPage(index0, index1), Country("DK", "Denmark")).success.value
          .set(VatRatesFromEuPage(index0, index1), List(twentyPercentVatRate)).success.value
          .set(NetValueOfSalesFromEuPage(index0, index1, index0), BigDecimal(100)).success.value
          .set(VatOnSalesFromEuPage(index0, index1, index0), VatOnSales(Standard, BigDecimal(20))).success.value

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
          .set(NetValueOfSalesFromEuPage(index0, index0, index0), BigDecimal(100)).success.value
          .set(VatOnSalesFromEuPage(index0, index0, index0), VatOnSales(Standard, BigDecimal(20))).success.value
          .set(NetValueOfSalesFromEuPage(index0, index1, index0), BigDecimal(200)).success.value
          .set(VatOnSalesFromEuPage(index0, index1, index0), VatOnSales(Standard, BigDecimal(20))).success.value
          .set(NetValueOfSalesFromEuPage(index0, index1, index1), BigDecimal(200)).success.value
          .set(VatOnSalesFromEuPage(index0, index1, index1), VatOnSales(Standard, BigDecimal(20))).success.value
          .set(NetValueOfSalesFromEuPage(index1, index0, index0), BigDecimal(300)).success.value
          .set(VatOnSalesFromEuPage(index1, index0, index0), VatOnSales(Standard, BigDecimal(20))).success.value
          .set(NetValueOfSalesFromEuPage(index1, index1, index0), BigDecimal(400)).success.value
          .set(VatOnSalesFromEuPage(index1, index1, index0), VatOnSales(Standard, BigDecimal(20))).success.value
          .set(NetValueOfSalesFromEuPage(index1, index1, index1), BigDecimal(400)).success.value
          .set(VatOnSalesFromEuPage(index1, index1, index1), VatOnSales(Standard, BigDecimal(20))).success.value

        service.getEuTotalVatOnSales(answers) mustBe Some(BigDecimal(120))
      }
    }

    "getEuTotalNetSales" - {

      "must show correct net total sales for one country from, one country to with one vat rate" in {
        service.getEuTotalNetSales(completeUserAnswers) mustBe Some(BigDecimal(100))
      }

      "must show correct net total sales for one country from, one country to with multiple vat rates" in {
        val answers = completeUserAnswers
          .set(VatRatesFromEuPage(index0, index0), List(twentyPercentVatRate, fivePercentVatRate)).success.value
          .set(NetValueOfSalesFromEuPage(index0, index0, index0), BigDecimal(100)).success.value
          .set(VatOnSalesFromEuPage(index0, index0, index0), VatOnSales(Standard, BigDecimal(20))).success.value
          .set(NetValueOfSalesFromEuPage(index0, index0, index1), BigDecimal(200)).success.value
          .set(VatOnSalesFromEuPage(index0, index0, index1), VatOnSales(Standard, BigDecimal(20))).success.value

        service.getEuTotalNetSales(answers) mustBe Some(BigDecimal(300))
      }

      "must show correct net total sales for one country from, multiple countries to with multiple vat rates" in {
        val answers = completeUserAnswers
          .set(VatRatesFromEuPage(index0, index0), List(twentyPercentVatRate, fivePercentVatRate)).success.value
          .set(NetValueOfSalesFromEuPage(index0, index0, index0), BigDecimal(100)).success.value
          .set(VatOnSalesFromEuPage(index0, index0, index0), VatOnSales(Standard, BigDecimal(20))).success.value
          .set(NetValueOfSalesFromEuPage(index0, index0, index1), BigDecimal(200)).success.value
          .set(VatOnSalesFromEuPage(index0, index0, index1), VatOnSales(Standard, BigDecimal(20))).success.value
          .set(CountryOfConsumptionFromEuPage(index0, index1), Country("DK", "Denmark")).success.value
          .set(VatRatesFromEuPage(index0, index1), List(twentyPercentVatRate)).success.value
          .set(NetValueOfSalesFromEuPage(index0, index1, index0), BigDecimal(100)).success.value
          .set(VatOnSalesFromEuPage(index0, index1, index0), VatOnSales(Standard, BigDecimal(20))).success.value

        service.getEuTotalNetSales(answers) mustBe Some(BigDecimal(400))
      }

      "must show correct net total sales for multiple country from, multiple countries to with multiple vat rates" in {
        val answers = emptyUserAnswers
          .set(SoldGoodsFromEuPage,true).success.value
          //countries from
          .set(CountryOfSaleFromEuPage(index0), Country("HR", "Croatia")).success.value
          .set(CountryOfConsumptionFromEuPage(index0, index0), Country("BE", "Belgium")).success.value
          .set(VatRatesFromEuPage(index0, index0), List(twentyPercentVatRate)).success.value
          .set(NetValueOfSalesFromEuPage(index0, index0, index0), BigDecimal(100)).success.value
          .set(VatOnSalesFromEuPage(index0, index0, index0), VatOnSales(Standard, BigDecimal(20))).success.value

          .set(CountryOfConsumptionFromEuPage(index0, index1), Country("DK", "Denmark")).success.value
          .set(VatRatesFromEuPage(index0, index1), List(twentyPercentVatRate, fivePercentVatRate)).success.value
          .set(NetValueOfSalesFromEuPage(index0, index1, index0), BigDecimal(200)).success.value
          .set(VatOnSalesFromEuPage(index0, index1, index0), VatOnSales(Standard, BigDecimal(20))).success.value
          .set(NetValueOfSalesFromEuPage(index0, index1, index1), BigDecimal(200)).success.value
          .set(VatOnSalesFromEuPage(index0, index1, index1), VatOnSales(Standard, BigDecimal(20))).success.value


          .set(CountryOfSaleFromEuPage(index1), Country("EE", "Estonia")).success.value
          .set(CountryOfConsumptionFromEuPage(index1, index0), Country("BE", "Belgium")).success.value
          .set(VatRatesFromEuPage(index1, index0), List(twentyPercentVatRate)).success.value
          .set(NetValueOfSalesFromEuPage(index1, index0, index0), BigDecimal(300)).success.value
          .set(VatOnSalesFromEuPage(index1, index0, index0), VatOnSales(Standard, BigDecimal(20))).success.value

          .set(CountryOfConsumptionFromEuPage(index1, index1), Country("DK", "Denmark")).success.value
          .set(VatRatesFromEuPage(index1, index1), List(twentyPercentVatRate, fivePercentVatRate)).success.value
          .set(NetValueOfSalesFromEuPage(index1, index1, index0), BigDecimal(400)).success.value
          .set(VatOnSalesFromEuPage(index1, index1, index0), VatOnSales(Standard, BigDecimal(20))).success.value
          .set(NetValueOfSalesFromEuPage(index1, index1, index1), BigDecimal(400)).success.value
          .set(VatOnSalesFromEuPage(index1, index1, index1), VatOnSales(Standard, BigDecimal(20))).success.value


        service.getEuTotalNetSales(answers) mustBe Some(BigDecimal(1600))
      }
    }

    "getVatOwedToEuCountries" - {
      val belgium: Country = Country("BE", "Belgium")
      val denmark: Country = Country("DK", "Denmark")
      val spain: Country = Country("ES", "Spain")

      "when the corrections exist" - {

        "must return correct total vat to eu countries for one country from, one country to with one vat rate" in {
          val ua = emptyUserAnswers
            .set(SoldGoodsFromEuPage,true).success.value
            .set(CountryOfSaleFromEuPage(index0), Country("HR", "Croatia")).success.value
            .set(CountryOfConsumptionFromEuPage(index0, index0), Country("BE", "Belgium")).success.value
            .set(VatRatesFromEuPage(index0, index0), List(twentyPercentVatRate)).success.value
            .set(NetValueOfSalesFromEuPage(index0, index0, index0), BigDecimal(100)).success.value
            .set(VatOnSalesFromEuPage(index0, index0, index0), VatOnSales(Standard, BigDecimal(20))).success.value

          val expected = List(TotalVatToCountry(belgium, BigDecimal(20)))

          service.getVatOwedToEuCountries(ua) mustBe expected
        }

        "must return correct total vat to eu countries for one country from, one country to with one vat rate and a correction for the country" in {
          
          val ua = emptyUserAnswers
            .set(SoldGoodsFromEuPage,true).success.value
            .set(CountryOfSaleFromEuPage(index0), Country("HR", "Croatia")).success.value
            .set(CountryOfConsumptionFromEuPage(index0, index0), Country("BE", "Belgium")).success.value
            .set(VatRatesFromEuPage(index0, index0), List(twentyPercentVatRate)).success.value
            .set(NetValueOfSalesFromEuPage(index0, index0, index0), BigDecimal(100)).success.value
            .set(VatOnSalesFromEuPage(index0, index0, index0), VatOnSales(Standard, BigDecimal(20))).success.value
            .set(CorrectionReturnPeriodPage(index0), period).success.value
            .set(CorrectionCountryPage(index0, index0), belgium).success.value
            .set(CountryVatCorrectionPage(index0, index0), BigDecimal(100)).success.value

          val expected = List(TotalVatToCountry(belgium, BigDecimal(120)))

          service.getVatOwedToEuCountries(ua) mustBe expected
        }

        "must return correct total vat to eu countries for one country from, one country to with one vat rate and a correction for another country" in {
          
          val ua = emptyUserAnswers
            .set(SoldGoodsFromEuPage,true).success.value
            .set(CountryOfSaleFromEuPage(index0), Country("HR", "Croatia")).success.value
            .set(CountryOfConsumptionFromEuPage(index0, index0), Country("BE", "Belgium")).success.value
            .set(VatRatesFromEuPage(index0, index0), List(twentyPercentVatRate)).success.value
            .set(NetValueOfSalesFromEuPage(index0, index0, index0), BigDecimal(100)).success.value
            .set(VatOnSalesFromEuPage(index0, index0, index0), VatOnSales(Standard, BigDecimal(20))).success.value
            .set(CorrectionReturnPeriodPage(index0), period).success.value
            .set(CorrectionCountryPage(index0, index0), spain).success.value
            .set(CountryVatCorrectionPage(index0, index0), BigDecimal(-100)).success.value

          val expected = List(TotalVatToCountry(belgium, BigDecimal(20)), TotalVatToCountry(spain, BigDecimal(-100)))

          service.getVatOwedToEuCountries(ua) mustBe expected
        }

        "must return correct total vat to eu countries for one country from, one country to with multiple vat rates" in {
          
          val answers = emptyUserAnswers
            .set(SoldGoodsFromEuPage,true).success.value
            .set(CountryOfSaleFromEuPage(index0), Country("HR", "Croatia")).success.value
            .set(CountryOfConsumptionFromEuPage(index0, index0), Country("BE", "Belgium")).success.value
            .set(VatRatesFromEuPage(index0, index0), List(twentyPercentVatRate, fivePercentVatRate)).success.value
            .set(NetValueOfSalesFromEuPage(index0, index0, index0), BigDecimal(100)).success.value
            .set(VatOnSalesFromEuPage(index0, index0, index0), VatOnSales(Standard, BigDecimal(20))).success.value
            .set(NetValueOfSalesFromEuPage(index0, index0, index1), BigDecimal(200)).success.value
            .set(VatOnSalesFromEuPage(index0, index0, index1), VatOnSales(Standard, BigDecimal(20))).success.value

          val expected = List(TotalVatToCountry(belgium, BigDecimal(40)))

          service.getVatOwedToEuCountries(answers) mustBe expected
        }

        "must return correct total vat to eu countries for one country from, multiple countries to with multiple vat rates" in {
          
          val answers = emptyUserAnswers
            .set(SoldGoodsFromEuPage,true).success.value
            .set(CountryOfSaleFromEuPage(index0), Country("HR", "Croatia")).success.value
            .set(CountryOfConsumptionFromEuPage(index0, index0), Country("BE", "Belgium")).success.value
            .set(VatRatesFromEuPage(index0, index0), List(twentyPercentVatRate, fivePercentVatRate)).success.value
            .set(NetValueOfSalesFromEuPage(index0, index0, index0), BigDecimal(100)).success.value
            .set(VatOnSalesFromEuPage(index0, index0, index0), VatOnSales(Standard, BigDecimal(20))).success.value
            .set(NetValueOfSalesFromEuPage(index0, index0, index1), BigDecimal(100)).success.value
            .set(VatOnSalesFromEuPage(index0, index0, index1), VatOnSales(Standard, BigDecimal(20))).success.value
            .set(CountryOfConsumptionFromEuPage(index0, index1), denmark).success.value
            .set(VatRatesFromEuPage(index0, index1), List(twentyPercentVatRate)).success.value
            .set(NetValueOfSalesFromEuPage(index0, index1, index0), BigDecimal(100)).success.value
            .set(VatOnSalesFromEuPage(index0, index1, index0), VatOnSales(Standard, BigDecimal(20))).success.value

          service.getVatOwedToEuCountries(answers) must contain theSameElementsAs List(
            TotalVatToCountry(belgium, BigDecimal(40)),
            TotalVatToCountry(denmark, BigDecimal(20))
          )
        }

        "must return correct total vat to eu countries for multiple country from, multiple countries to with multiple vat rates" in {
          
          val answers = emptyUserAnswers
            .set(SoldGoodsFromEuPage,true).success.value
            //countries from
            .set(CountryOfSaleFromEuPage(index0), Country("HR", "Croatia")).success.value
            .set(CountryOfSaleFromEuPage(index1), Country("EE", "Estonia")).success.value

            //countries to
            .set(CountryOfConsumptionFromEuPage(index0, index0), belgium).success.value
            .set(CountryOfConsumptionFromEuPage(index0, index1), denmark).success.value
            .set(CountryOfConsumptionFromEuPage(index1, index0), belgium).success.value
            .set(CountryOfConsumptionFromEuPage(index1, index1), denmark).success.value

            //vat rates
            .set(VatRatesFromEuPage(index0, index0), List(twentyPercentVatRate)).success.value
            .set(VatRatesFromEuPage(index0, index1), List(twentyPercentVatRate, fivePercentVatRate)).success.value
            .set(VatRatesFromEuPage(index1, index0), List(twentyPercentVatRate)).success.value
            .set(VatRatesFromEuPage(index1, index1), List(twentyPercentVatRate, fivePercentVatRate)).success.value

            //sales at vat rate
            .set(NetValueOfSalesFromEuPage(index0, index0, index0), BigDecimal(100)).success.value
            .set(VatOnSalesFromEuPage(index0, index0, index0), VatOnSales(Standard, BigDecimal(10))).success.value
            .set(NetValueOfSalesFromEuPage(index0, index1, index0), BigDecimal(200)).success.value
            .set(VatOnSalesFromEuPage(index0, index1, index0), VatOnSales(Standard, BigDecimal(20))).success.value
            .set(NetValueOfSalesFromEuPage(index0, index1, index1), BigDecimal(200)).success.value
            .set(VatOnSalesFromEuPage(index0, index1, index1), VatOnSales(Standard, BigDecimal(30))).success.value
            .set(NetValueOfSalesFromEuPage(index1, index0, index0), BigDecimal(300)).success.value
            .set(VatOnSalesFromEuPage(index1, index0, index0), VatOnSales(Standard, BigDecimal(40))).success.value
            .set(NetValueOfSalesFromEuPage(index1, index1, index0), BigDecimal(400)).success.value
            .set(VatOnSalesFromEuPage(index1, index1, index0), VatOnSales(Standard, BigDecimal(50))).success.value
            .set(NetValueOfSalesFromEuPage(index1, index1, index1), BigDecimal(400)).success.value
            .set(VatOnSalesFromEuPage(index1, index1, index1), VatOnSales(Standard, BigDecimal(60))).success.value

          service.getVatOwedToEuCountries(answers) must contain theSameElementsAs List(
            TotalVatToCountry(belgium, BigDecimal(50)),
            TotalVatToCountry(denmark, BigDecimal(160))
          )
        }

        "must return correct total vat to eu countries for multiple country from, multiple countries to with multiple vat rates with NI sales and Eu sales" in {
          
          val answers = completeSalesFromNIUserAnswers
            .set(SoldGoodsFromEuPage,true).success.value
            //countries from
            .set(CountryOfSaleFromEuPage(index0), Country("HR", "Croatia")).success.value
            .set(CountryOfSaleFromEuPage(index1), Country("EE", "Estonia")).success.value

            //countries to
            .set(CountryOfConsumptionFromEuPage(index0, index0), belgium).success.value
            .set(CountryOfConsumptionFromEuPage(index0, index1), denmark).success.value
            .set(CountryOfConsumptionFromEuPage(index1, index0), belgium).success.value
            .set(CountryOfConsumptionFromEuPage(index1, index1), denmark).success.value

            //vat rates
            .set(VatRatesFromEuPage(index0, index0), List(twentyPercentVatRate)).success.value
            .set(VatRatesFromEuPage(index0, index1), List(twentyPercentVatRate, fivePercentVatRate)).success.value
            .set(VatRatesFromEuPage(index1, index0), List(twentyPercentVatRate)).success.value
            .set(VatRatesFromEuPage(index1, index1), List(twentyPercentVatRate, fivePercentVatRate)).success.value

            //sales at vat rate
            .set(NetValueOfSalesFromEuPage(index0, index0, index0), BigDecimal(100)).success.value
            .set(VatOnSalesFromEuPage(index0, index0, index0), VatOnSales(Standard, BigDecimal(10))).success.value
            .set(NetValueOfSalesFromEuPage(index0, index1, index0), BigDecimal(200)).success.value
            .set(VatOnSalesFromEuPage(index0, index1, index0), VatOnSales(Standard, BigDecimal(20))).success.value
            .set(NetValueOfSalesFromEuPage(index0, index1, index1), BigDecimal(200)).success.value
            .set(VatOnSalesFromEuPage(index0, index1, index1), VatOnSales(Standard, BigDecimal(30))).success.value
            .set(NetValueOfSalesFromEuPage(index1, index0, index0), BigDecimal(300)).success.value
            .set(VatOnSalesFromEuPage(index1, index0, index0), VatOnSales(Standard, BigDecimal(40))).success.value
            .set(NetValueOfSalesFromEuPage(index1, index1, index0), BigDecimal(400)).success.value
            .set(VatOnSalesFromEuPage(index1, index1, index0), VatOnSales(Standard, BigDecimal(50))).success.value
            .set(NetValueOfSalesFromEuPage(index1, index1, index1), BigDecimal(400)).success.value
            .set(VatOnSalesFromEuPage(index1, index1, index1), VatOnSales(Standard, BigDecimal(60))).success.value

          service.getVatOwedToEuCountries(answers) must contain theSameElementsAs List(
            TotalVatToCountry(belgium, BigDecimal(50)),
            TotalVatToCountry(denmark, BigDecimal(160)),
            TotalVatToCountry(spain, BigDecimal(1000))
          )
        }
      }

      "when the corrections is empty" - {

        "must return correct total vat to eu countries for one country from, one country to with one vat rate" in {
          
          val ua = emptyUserAnswers
            .set(SoldGoodsFromEuPage,true).success.value
            .set(CountryOfSaleFromEuPage(index0), Country("HR", "Croatia")).success.value
            .set(CountryOfConsumptionFromEuPage(index0, index0), Country("BE", "Belgium")).success.value
            .set(VatRatesFromEuPage(index0, index0), List(twentyPercentVatRate)).success.value
            .set(NetValueOfSalesFromEuPage(index0, index0, index0), BigDecimal(100)).success.value
            .set(VatOnSalesFromEuPage(index0, index0, index0), VatOnSales(Standard, BigDecimal(20))).success.value

          val expected = List(TotalVatToCountry(belgium, BigDecimal(20)))

          service.getVatOwedToEuCountries(ua) mustBe expected
        }

        "must return correct total vat to eu countries for one country from, one country to with multiple vat rates" in {
          
          val answers = emptyUserAnswers
            .set(SoldGoodsFromEuPage,true).success.value
            .set(CountryOfSaleFromEuPage(index0), Country("HR", "Croatia")).success.value
            .set(CountryOfConsumptionFromEuPage(index0, index0), Country("BE", "Belgium")).success.value
            .set(VatRatesFromEuPage(index0, index0), List(twentyPercentVatRate, fivePercentVatRate)). success.value
            .set(NetValueOfSalesFromEuPage(index0, index0, index0), BigDecimal(100)).success.value
            .set(VatOnSalesFromEuPage(index0, index0, index0), VatOnSales(Standard, BigDecimal(20))).success.value
            .set(NetValueOfSalesFromEuPage(index0, index0, index1), BigDecimal(200)).success.value
            .set(VatOnSalesFromEuPage(index0, index0, index1), VatOnSales(Standard, BigDecimal(20))).success.value

          val expected = List(TotalVatToCountry(belgium, BigDecimal(40)))

          service.getVatOwedToEuCountries(answers) mustBe expected
        }

        "must return correct total vat to eu countries for one country from, multiple countries to with multiple vat rates" in {
          
          val answers = emptyUserAnswers
            .set(SoldGoodsFromEuPage,true).success.value
            .set(CountryOfSaleFromEuPage(index0), Country("HR", "Croatia")).success.value
            .set(CountryOfConsumptionFromEuPage(index0, index0), Country("BE", "Belgium")).success.value
            .set(VatRatesFromEuPage(index0, index0), List(twentyPercentVatRate, fivePercentVatRate)).success.value
            .set(NetValueOfSalesFromEuPage(index0, index0, index0), BigDecimal(100)).success.value
            .set(VatOnSalesFromEuPage(index0, index0, index0), VatOnSales(Standard, BigDecimal(20))).success.value
            .set(NetValueOfSalesFromEuPage(index0, index0, index1), BigDecimal(100)).success.value
            .set(VatOnSalesFromEuPage(index0, index0, index1), VatOnSales(Standard, BigDecimal(20))).success.value
            .set(CountryOfConsumptionFromEuPage(index0, index1), denmark).success.value
            .set(VatRatesFromEuPage(index0, index1), List(twentyPercentVatRate)).success.value
            .set(NetValueOfSalesFromEuPage(index0, index1, index0), BigDecimal(100)).success.value
            .set(VatOnSalesFromEuPage(index0, index1, index0), VatOnSales(Standard, BigDecimal(20))).success.value

          service.getVatOwedToEuCountries(answers) must contain theSameElementsAs List(
            TotalVatToCountry(belgium, BigDecimal(40)),
            TotalVatToCountry(denmark, BigDecimal(20))
          )
        }

        "must return correct total vat to eu countries for multiple country from, multiple countries to with multiple vat rates" in {
          
          val answers = emptyUserAnswers
            .set(SoldGoodsFromEuPage,true).success.value
            //countries from
            .set(CountryOfSaleFromEuPage(index0), Country("HR", "Croatia")).success.value
            .set(CountryOfSaleFromEuPage(index1), Country("EE", "Estonia")).success.value

            //countries to
            .set(CountryOfConsumptionFromEuPage(index0, index0), belgium).success.value
            .set(CountryOfConsumptionFromEuPage(index0, index1), denmark).success.value
            .set(CountryOfConsumptionFromEuPage(index1, index0), belgium).success.value
            .set(CountryOfConsumptionFromEuPage(index1, index1), denmark).success.value

            //vat rates
            .set(VatRatesFromEuPage(index0, index0), List(twentyPercentVatRate)).success.value
            .set(VatRatesFromEuPage(index0, index1), List(twentyPercentVatRate, fivePercentVatRate)).success.value
            .set(VatRatesFromEuPage(index1, index0), List(twentyPercentVatRate)).success.value
            .set(VatRatesFromEuPage(index1, index1), List(twentyPercentVatRate, fivePercentVatRate)).success.value

            //sales at vat rate
            .set(NetValueOfSalesFromEuPage(index0, index0, index0), BigDecimal(100)).success.value
            .set(VatOnSalesFromEuPage(index0, index0, index0), VatOnSales(Standard, BigDecimal(10))).success.value
            .set(NetValueOfSalesFromEuPage(index0, index1, index0), BigDecimal(200)).success.value
            .set(VatOnSalesFromEuPage(index0, index1, index0), VatOnSales(Standard, BigDecimal(20))).success.value
            .set(NetValueOfSalesFromEuPage(index0, index1, index1), BigDecimal(200)).success.value
            .set(VatOnSalesFromEuPage(index0, index1, index1), VatOnSales(Standard, BigDecimal(30))).success.value
            .set(NetValueOfSalesFromEuPage(index1, index0, index0), BigDecimal(300)).success.value
            .set(VatOnSalesFromEuPage(index1, index0, index0), VatOnSales(Standard, BigDecimal(40))).success.value
            .set(NetValueOfSalesFromEuPage(index1, index1, index0), BigDecimal(400)).success.value
            .set(VatOnSalesFromEuPage(index1, index1, index0), VatOnSales(Standard, BigDecimal(50))).success.value
            .set(NetValueOfSalesFromEuPage(index1, index1, index1), BigDecimal(400)).success.value
            .set(VatOnSalesFromEuPage(index1, index1, index1), VatOnSales(Standard, BigDecimal(60))).success.value

          service.getVatOwedToEuCountries(answers) must contain theSameElementsAs List(
            TotalVatToCountry(belgium, BigDecimal(50)),
            TotalVatToCountry(denmark, BigDecimal(160))
          )
        }

        "must return correct total vat to eu countries for multiple country from, multiple countries to with multiple vat rates with NI sales and Eu sales" in {
          
          val answers = completeSalesFromNIUserAnswers
            .set(SoldGoodsFromEuPage,true).success.value
            //countries from
            .set(CountryOfSaleFromEuPage(index0), Country("HR", "Croatia")).success.value
            .set(CountryOfSaleFromEuPage(index1), Country("EE", "Estonia")).success.value

            //countries to
            .set(CountryOfConsumptionFromEuPage(index0, index0), belgium).success.value
            .set(CountryOfConsumptionFromEuPage(index0, index1), denmark).success.value
            .set(CountryOfConsumptionFromEuPage(index1, index0), belgium).success.value
            .set(CountryOfConsumptionFromEuPage(index1, index1), denmark).success.value

            //vat rates
            .set(VatRatesFromEuPage(index0, index0), List(twentyPercentVatRate)).success.value
            .set(VatRatesFromEuPage(index0, index1), List(twentyPercentVatRate, fivePercentVatRate)).success.value
            .set(VatRatesFromEuPage(index1, index0), List(twentyPercentVatRate)).success.value
            .set(VatRatesFromEuPage(index1, index1), List(twentyPercentVatRate, fivePercentVatRate)).success.value

            //sales at vat rate
            .set(NetValueOfSalesFromEuPage(index0, index0, index0), BigDecimal(100)).success.value
            .set(VatOnSalesFromEuPage(index0, index0, index0), VatOnSales(Standard, BigDecimal(10))).success.value
            .set(NetValueOfSalesFromEuPage(index0, index1, index0), BigDecimal(200)).success.value
            .set(VatOnSalesFromEuPage(index0, index1, index0), VatOnSales(Standard, BigDecimal(20))).success.value
            .set(NetValueOfSalesFromEuPage(index0, index1, index1), BigDecimal(200)).success.value
            .set(VatOnSalesFromEuPage(index0, index1, index1), VatOnSales(Standard, BigDecimal(30))).success.value
            .set(NetValueOfSalesFromEuPage(index1, index0, index0), BigDecimal(300)).success.value
            .set(VatOnSalesFromEuPage(index1, index0, index0), VatOnSales(Standard, BigDecimal(40))).success.value
            .set(NetValueOfSalesFromEuPage(index1, index1, index0), BigDecimal(400)).success.value
            .set(VatOnSalesFromEuPage(index1, index1, index0), VatOnSales(Standard, BigDecimal(50))).success.value
            .set(NetValueOfSalesFromEuPage(index1, index1, index1), BigDecimal(400)).success.value
            .set(VatOnSalesFromEuPage(index1, index1, index1), VatOnSales(Standard, BigDecimal(60))).success.value

          service.getVatOwedToEuCountries(answers) must contain theSameElementsAs List(
            TotalVatToCountry(belgium, BigDecimal(50)),
            TotalVatToCountry(denmark, BigDecimal(160)),
            TotalVatToCountry(spain, BigDecimal(1000))
          )
        }
      }

    }

    "getTotalVatOwedAfterCorrections" - {
      "when corrections exist" - {

        "must return correct total when NI and EU sales exist" in {
          
          service.getTotalVatOwedAfterCorrections(completeUserAnswers) mustBe BigDecimal(1020)
        }

        "must return zero when total NI and EU sales don't exist" in {
          
          service.getTotalVatOwedAfterCorrections(emptyUserAnswers) mustBe BigDecimal(0)
        }

        "must return total when NI exists and EU sales don't exist" in {
          
          service.getTotalVatOwedAfterCorrections(completeSalesFromNIUserAnswers) mustBe BigDecimal(1000)
        }

        "must return total when NI doesn't exist and EU does exist" in {
          
          val answers = emptyUserAnswers
            .set(SoldGoodsFromEuPage, true).success.value
            .set(CountryOfSaleFromEuPage(index0), Country("HR", "Croatia")).success.value
            .set(CountryOfConsumptionFromEuPage(index0, index0), Country("BE", "Belgium")).success.value
            .set(VatRatesFromEuPage(index0, index0), List(twentyPercentVatRate)).success.value
            .set(NetValueOfSalesFromEuPage(index0, index0, index0), BigDecimal(100)).success.value
            .set(VatOnSalesFromEuPage(index0, index0, index0), VatOnSales(Standard, BigDecimal(20))).success.value

          service.getTotalVatOwedAfterCorrections(answers) mustBe BigDecimal(20)
        }

        "must return correct total when there is a positive correction " in {
          
          val ua = emptyUserAnswers
            .set(SoldGoodsFromEuPage, true).success.value
            .set(CountryOfSaleFromEuPage(index0), Country("HR", "Croatia")).success.value
            .set(CountryOfConsumptionFromEuPage(index0, index0), Country("BE", "Belgium")).success.value
            .set(VatRatesFromEuPage(index0, index0), List(twentyPercentVatRate)).success.value
            .set(NetValueOfSalesFromEuPage(index0, index0, index0), BigDecimal(100)).success.value
            .set(VatOnSalesFromEuPage(index0, index0, index0), VatOnSales(Standard, BigDecimal(20))).success.value
            .set(CorrectionReturnPeriodPage(index0), period).success.value
            .set(CorrectionCountryPage(index0, index0), Country("BE", "Belgium")).success.value
            .set(CountryVatCorrectionPage(index0, index0), BigDecimal(100)).success.value

          service.getTotalVatOwedAfterCorrections(ua) mustBe BigDecimal(120)

        }

        "must return correct total when there is a negative correction" in {
          
          val ua = emptyUserAnswers
            .set(SoldGoodsFromEuPage, true).success.value
            .set(CountryOfSaleFromEuPage(index0), Country("HR", "Croatia")).success.value
            .set(CountryOfConsumptionFromEuPage(index0, index0), Country("BE", "Belgium")).success.value
            .set(VatRatesFromEuPage(index0, index0), List(twentyPercentVatRate)).success.value
            .set(NetValueOfSalesFromEuPage(index0, index0, index0), BigDecimal(1000)).success.value
            .set(VatOnSalesFromEuPage(index0, index0, index0), VatOnSales(Standard, BigDecimal(200))).success.value
            .set(CorrectionReturnPeriodPage(index0), period).success.value
            .set(CorrectionCountryPage(index0, index0), Country("BE", "Belgium")).success.value
            .set(CountryVatCorrectionPage(index0, index0), BigDecimal(-100)).success.value

          service.getTotalVatOwedAfterCorrections(ua) mustBe BigDecimal(100)
        }

        "must return zero when the correction makes the total amount negative for a country" in {
          
          val ua = emptyUserAnswers
            .set(SoldGoodsFromEuPage, true).success.value
            .set(CountryOfSaleFromEuPage(index0), Country("HR", "Croatia")).success.value
            .set(CountryOfConsumptionFromEuPage(index0, index0), Country("BE", "Belgium")).success.value
            .set(VatRatesFromEuPage(index0, index0), List(twentyPercentVatRate)).success.value
            .set(NetValueOfSalesFromEuPage(index0, index0, index0), BigDecimal(1000)).success.value
            .set(VatOnSalesFromEuPage(index0, index0, index0), VatOnSales(Standard, BigDecimal(100))).success.value
            .set(CorrectionReturnPeriodPage(index0), period).success.value
            .set(CorrectionCountryPage(index0, index0), Country("BE", "Belgium")).success.value
            .set(CountryVatCorrectionPage(index0, index0), BigDecimal(-1000)).success.value

          service.getTotalVatOwedAfterCorrections(ua) mustBe BigDecimal(0)
        }

        "must not subtract the negative amount for one country from the positive total for other countries" in {
          
          val ua = emptyUserAnswers
            .set(SoldGoodsFromEuPage, true).success.value
            .set(CountryOfSaleFromEuPage(index0), Country("HR", "Croatia")).success.value
            .set(CountryOfConsumptionFromEuPage(index0, index0), Country("BE", "Belgium")).success.value
            .set(VatRatesFromEuPage(index0, index0), List(twentyPercentVatRate)).success.value
            .set(NetValueOfSalesFromEuPage(index0, index0, index0), BigDecimal(1000)).success.value
            .set(VatOnSalesFromEuPage(index0, index0, index0), VatOnSales(Standard, BigDecimal(100))).success.value
            .set(CorrectionReturnPeriodPage(index0), period).success.value
            .set(CorrectionCountryPage(index0, index0), Country("EE", "Estonia")).success.value
            .set(CountryVatCorrectionPage(index0, index0), BigDecimal(-1000)).success.value

          service.getTotalVatOwedAfterCorrections(ua) mustBe BigDecimal(100)
        }
      }

      "when corrections is empty" - {

        "must return correct total when NI and EU sales exist" in {
          
          service.getTotalVatOwedAfterCorrections(completeUserAnswers) mustBe BigDecimal(1020)
        }

        "must return zero when total NI and EU sales don't exist" in {
          
          service.getTotalVatOwedAfterCorrections(emptyUserAnswers) mustBe BigDecimal(0)
        }

        "must return total when NI exists and EU sales don't exist" in {
          
          service.getTotalVatOwedAfterCorrections(completeSalesFromNIUserAnswers) mustBe BigDecimal(1000)
        }

        "must return total when NI doesn't exist and EU does exist" in {
          
          val answers = emptyUserAnswers
            .set(SoldGoodsFromEuPage, true).success.value
            .set(CountryOfSaleFromEuPage(index0), Country("HR", "Croatia")).success.value
            .set(CountryOfConsumptionFromEuPage(index0, index0), Country("BE", "Belgium")).success.value
            .set(VatRatesFromEuPage(index0, index0), List(twentyPercentVatRate)).success.value
            .set(NetValueOfSalesFromEuPage(index0, index0, index0), BigDecimal(100)).success.value
            .set(VatOnSalesFromEuPage(index0, index0, index0), VatOnSales(Standard, BigDecimal(20))).success.value

          service.getTotalVatOwedAfterCorrections(answers) mustBe BigDecimal(20)
        }
      }
    }
  }
}
