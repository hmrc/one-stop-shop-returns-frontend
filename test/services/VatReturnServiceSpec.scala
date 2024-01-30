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

package services

import base.SpecBase
import cats.data.NonEmptyChain
import cats.data.Validated.{Invalid, Valid}
import connectors.VatReturnConnector
import models.corrections.CorrectionToCountry
import models.domain.{SalesDetails, SalesFromEuCountry, SalesToCountry, VatRate => DomainVatRate, VatRateType => DomainVatRateType}
import models.registration._
import models.requests.VatReturnRequest
import models.responses.UnexpectedResponseStatus
import models.{Country, DataMissingError, Index, PartialReturnPeriod, StandardPeriod, VatOnSales, VatRate, VatRateType}
import models.Quarter.Q4
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages._
import queries._
import services.corrections.CorrectionService
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

class VatReturnServiceSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()
  implicit private lazy val ec: ExecutionContext = ExecutionContext.global

  ".fromUserAnswers" - {

    "must return a vat return request" - {

      "when the user has not sold any goods" in new Fixture {

        private val answers =
          emptyUserAnswers
            .set(SoldGoodsFromNiPage, false).success.value
            .set(SoldGoodsFromEuPage, false).success.value

        private val expectedResult = VatReturnRequest(vrn, period, Some(period.firstDay), Some(period.lastDay), List.empty, List.empty)

        service.fromUserAnswers(answers, vrn, period, registrationWithoutEuDetails) mustEqual Valid(expectedResult)
      }

      "when the user has not sold any goods with partial return period" in new Fixture {

        private val answers =
          emptyUserAnswers
            .set(SoldGoodsFromNiPage, false).success.value
            .set(SoldGoodsFromEuPage, false).success.value

        val startDate = LocalDate.of(2023, 11, 16)

        val standardPeriod = StandardPeriod(2023, Q4)

        val partialReturnPeriod = PartialReturnPeriod(startDate, standardPeriod.lastDay, standardPeriod.year, standardPeriod.quarter)

        private val expectedResult = VatReturnRequest(vrn, standardPeriod, Some(startDate), Some(standardPeriod.lastDay), List.empty, List.empty)

        service.fromUserAnswers(answers, vrn, partialReturnPeriod, registrationWithoutEuDetails) mustEqual Valid(expectedResult)
      }

      "when the user has sold goods from NI" in new Fixture {

        private val answers =
          emptyUserAnswers
            .set(SoldGoodsFromNiPage, true).success.value
            .set(CountryOfConsumptionFromNiPage(Index(0)), country1).success.value
            .set(VatRatesFromNiPage(Index(0)), List(vatRate1)).success.value
            .set(NetValueOfSalesFromNiPage(Index(0), Index(0)), netSales1).success.value
            .set(VatOnSalesFromNiPage(Index(0), Index(0)), vatOnSales1).success.value
            .set(CountryOfConsumptionFromNiPage(Index(1)), country2).success.value
            .set(VatRatesFromNiPage(Index(1)), List(vatRate2, vatRate3)).success.value
            .set(NetValueOfSalesFromNiPage(Index(1), Index(0)), netSales2).success.value
            .set(VatOnSalesFromNiPage(Index(1), Index(0)), vatOnSales2).success.value
            .set(NetValueOfSalesFromNiPage(Index(1), Index(1)), netSales3).success.value
            .set(VatOnSalesFromNiPage(Index(1), Index(1)), vatOnSales3).success.value
            .set(SoldGoodsFromEuPage, false).success.value

        private val expectedResult =
          VatReturnRequest(
            vrn,
            period,
            Some(period.firstDay),
            Some(period.lastDay),
            List(
              SalesToCountry(
                country1,
                List(SalesDetails(domainVatRate1, netSales1, vatOnSales1))
              ),
              SalesToCountry(
                country2,
                List(
                  SalesDetails(domainVatRate2, netSales2, vatOnSales2),
                  SalesDetails(domainVatRate3, netSales3, vatOnSales3)
                )
              )
            ),
            List.empty
          )

        service.fromUserAnswers(answers, vrn, period, registrationWithoutEuDetails) mustEqual Valid(expectedResult)
      }

      "when the user has sold goods from the EU" in new Fixture {

        private val answers =
          emptyUserAnswers
            .set(SoldGoodsFromNiPage, false).success.value
            .set(SoldGoodsFromEuPage, true).success.value
            .set(CountryOfSaleFromEuPage(Index(0)), country1).success.value
            .set(CountryOfConsumptionFromEuPage(Index(0), Index(0)), country2).success.value
            .set(VatRatesFromEuPage(Index(0), Index(0)), List(vatRate1)).success.value
            .set(NetValueOfSalesFromEuPage(Index(0), Index(0), Index(0)), netSales1).success.value
            .set(VatOnSalesFromEuPage(Index(0), Index(0), Index(0)), vatOnSales1).success.value
            .set(CountryOfConsumptionFromEuPage(Index(0), Index(1)), country3).success.value
            .set(VatRatesFromEuPage(Index(0), Index(1)), List(vatRate2, vatRate3)).success.value
            .set(NetValueOfSalesFromEuPage(Index(0), Index(1), Index(0)), netSales2).success.value
            .set(VatOnSalesFromEuPage(Index(0), Index(1), Index(0)), vatOnSales2).success.value
            .set(NetValueOfSalesFromEuPage(Index(0), Index(1), Index(1)), netSales3).success.value
            .set(VatOnSalesFromEuPage(Index(0), Index(1), Index(1)), vatOnSales3).success.value
            .set(CountryOfSaleFromEuPage(Index(1)), country3).success.value
            .set(CountryOfConsumptionFromEuPage(Index(1), Index(0)), country2).success.value
            .set(VatRatesFromEuPage(Index(1), Index(0)), List(vatRate2)).success.value
            .set(NetValueOfSalesFromEuPage(Index(1), Index(0), Index(0)), netSales2).success.value
            .set(VatOnSalesFromEuPage(Index(1), Index(0), Index(0)), vatOnSales2).success.value

        private val expectedResult =
          VatReturnRequest(
            vrn,
            period,
            Some(period.firstDay),
            Some(period.lastDay),
            List.empty,
            List(
              SalesFromEuCountry(
                country1,
                None,
                List(
                  SalesToCountry(
                    country2,
                    List(SalesDetails(domainVatRate1, netSales1, vatOnSales1))
                  ),
                  SalesToCountry(
                    country3,
                    List(
                      SalesDetails(domainVatRate2, netSales2, vatOnSales2),
                      SalesDetails(domainVatRate3, netSales3, vatOnSales3)
                    )
                  )
                )
              ),
              SalesFromEuCountry(
                country3,
                None,
                List(
                  SalesToCountry(
                    country2,
                    List(SalesDetails(domainVatRate2, netSales2, vatOnSales2))
                  )
                )
              )
            )
          )

        service.fromUserAnswers(answers, vrn, period, registrationWithoutEuDetails) mustEqual Valid(expectedResult)
      }
    }


    "must return Invalid" - {

      "when Sold Goods from NI is missing" in new Fixture {

        private val answers =
          emptyUserAnswers
            .set(SoldGoodsFromEuPage, false).success.value

        private val result = service.fromUserAnswers(answers, vrn, period, registrationWithoutEuDetails)
        result mustEqual Invalid(NonEmptyChain(DataMissingError(SoldGoodsFromNiPage)))
      }

      "when Sold Goods from NI is true but there are no sales details" in new Fixture {

        private val answers =
          emptyUserAnswers
            .set(SoldGoodsFromNiPage, true).success.value
            .set(SoldGoodsFromEuPage, false).success.value

        private val result = service.fromUserAnswers(answers, vrn, period, registrationWithoutEuDetails)
        result mustEqual Invalid(NonEmptyChain(DataMissingError(AllSalesFromNiQuery)))
      }


      "when there is a NI VAT rate with no corresponding net value of sales at that VAT rate" in new Fixture {

        private val answers =
          emptyUserAnswers
            .set(SoldGoodsFromNiPage, true).success.value
            .set(CountryOfConsumptionFromNiPage(Index(0)), country1).success.value
            .set(VatRatesFromNiPage(Index(0)), List(vatRate1, vatRate2)).success.value
            .set(NetValueOfSalesFromNiPage(Index(0), Index(0)), netSales1).success.value
            .set(VatOnSalesFromNiPage(Index(0), Index(0)), vatOnSales1).success.value
            .set(VatOnSalesFromNiPage(Index(0), Index(1)), vatOnSales2).success.value
            .set(SoldGoodsFromEuPage, false).success.value

        private val result = service.fromUserAnswers(answers, vrn, period, registrationWithoutEuDetails)
        result mustEqual Invalid(NonEmptyChain(DataMissingError(AllSalesFromNiQuery)))
      }

      "when there is a NI VAT rate with no corresponding VAT on sales at that VAT rate" in new Fixture {

        private val answers =
          emptyUserAnswers
            .set(SoldGoodsFromNiPage, true).success.value
            .set(CountryOfConsumptionFromNiPage(Index(0)), country1).success.value
            .set(VatRatesFromNiPage(Index(0)), List(vatRate1, vatRate2)).success.value
            .set(NetValueOfSalesFromNiPage(Index(0), Index(0)), netSales1).success.value
            .set(VatOnSalesFromNiPage(Index(0), Index(0)), vatOnSales1).success.value
            .set(NetValueOfSalesFromNiPage(Index(0), Index(1)), netSales2).success.value
            .set(SoldGoodsFromEuPage, false).success.value

        private val result = service.fromUserAnswers(answers, vrn, period, registrationWithoutEuDetails)
        result mustEqual Invalid(NonEmptyChain(DataMissingError(VatOnSalesFromNiQuery(Index(0),Index(1)))))
      }

      "when sold goods from EU is true but there are no sales details" in new Fixture {

        private val answers =
          emptyUserAnswers
            .set(SoldGoodsFromNiPage, false).success.value
            .set(SoldGoodsFromEuPage, true).success.value

        private val result = service.fromUserAnswers(answers, vrn, period, registrationWithoutEuDetails)
        result mustEqual Invalid(NonEmptyChain(DataMissingError(AllSalesFromEuQuery)))
      }

      "when there is a country of sale from the EU but no other details" in new Fixture {

        private val answers =
          emptyUserAnswers
            .set(SoldGoodsFromNiPage, false).success.value
            .set(SoldGoodsFromEuPage, true).success.value
            .set(CountryOfSaleFromEuPage(Index(0)), country1).success.value

        private val result = service.fromUserAnswers(answers, vrn, period, registrationWithoutEuDetails)
        result mustEqual Invalid(NonEmptyChain(DataMissingError(AllSalesToEuQuery(Index(0)))))
      }

      "when there is a country of sale and country of consumption from EU, but no other details" in new Fixture {

        private val answers =
          emptyUserAnswers
            .set(SoldGoodsFromNiPage, false).success.value
            .set(SoldGoodsFromEuPage, true).success.value
            .set(CountryOfSaleFromEuPage(Index(0)), country1).success.value
            .set(CountryOfConsumptionFromEuPage(Index(0), Index(0)), country2).success.value

        private val result = service.fromUserAnswers(answers, vrn, period, registrationWithoutEuDetails)
        result mustEqual Invalid(NonEmptyChain(DataMissingError(VatRatesFromEuPage(Index(0), Index(0)))))
      }

      "when there is a VAT rate from the EU with no corresponding net value of sales" in new Fixture {

        private val answers =
          emptyUserAnswers
            .set(SoldGoodsFromNiPage, false).success.value
            .set(SoldGoodsFromEuPage, true).success.value
            .set(CountryOfSaleFromEuPage(Index(0)), country1).success.value
            .set(CountryOfConsumptionFromEuPage(Index(0), Index(0)), country2).success.value
            .set(VatRatesFromEuPage(Index(0), Index(0)), List(vatRate1)).success.value
            .set(VatOnSalesFromEuPage(Index(0), Index(0), Index(0)), vatOnSales1).success.value

        private val result = service.fromUserAnswers(answers, vrn, period, registrationWithoutEuDetails)
        result mustEqual Invalid(NonEmptyChain(DataMissingError(AllSalesFromEuQuery)))
      }

      "when there is a VAT rate from the EU with no corresponding VAT on sales" in new Fixture {

        private val answers =
          emptyUserAnswers
            .set(SoldGoodsFromNiPage, false).success.value
            .set(SoldGoodsFromEuPage, true).success.value
            .set(CountryOfSaleFromEuPage(Index(0)), country1).success.value
            .set(CountryOfConsumptionFromEuPage(Index(0), Index(0)), country2).success.value
            .set(VatRatesFromEuPage(Index(0), Index(0)), List(vatRate1)).success.value
            .set(NetValueOfSalesFromEuPage(Index(0), Index(0), Index(0)), netSales1).success.value

        private val result = service.fromUserAnswers(answers, vrn, period, registrationWithoutEuDetails)
        result mustEqual Invalid(NonEmptyChain(DataMissingError(VatOnSalesFromEuQuery(Index(0), Index(0), Index(0)))))
      }
    }
  }

  "getVatOwedToCountryOnReturn" - {

    "must return a valid amount when connector returns a VatReturnResponse" in {

      val vatReturnConnector = mock[VatReturnConnector]
      val correctionService = mock[CorrectionService]
      val vatReturnService = new VatReturnService(vatReturnConnector, correctionService)

      when(vatReturnConnector.get(any())(any())) thenReturn Future.successful(Right(simpleCompleteVatReturn))

      val result = vatReturnService.getVatOwedToCountryOnReturn(Country("LT", "Lithuania"), period)

      val expected = BigDecimal(1800.00)

      assert(result.futureValue == expected)
    }

    "must return an exception when connector returns a ErrorResponse" in {

      val vatReturnConnector = mock[VatReturnConnector]
      val correctionService = mock[CorrectionService]
      val vatReturnService = new VatReturnService(vatReturnConnector, correctionService)

      when(vatReturnConnector.get(any())(any())) thenReturn Future.successful(Left(UnexpectedResponseStatus(1, "error")))

      val result = vatReturnService.getVatOwedToCountryOnReturn(Country("LT", "Lithuania"), period)

      whenReady(result.failed) { exp => exp mustBe a[Exception] }
    }
  }

  "getLatestVatAmountForPeriodAndCountry" - {

    "must return a valid amount when connector returns a VatReturnResponse and the service returns a list of corrections" in {

      val vatReturnConnector = mock[VatReturnConnector]
      val correctionService = mock[CorrectionService]
      val vatReturnService = new VatReturnService(vatReturnConnector, correctionService)

      when(vatReturnConnector.get(any())(any())) thenReturn Future.successful(Right(simpleCompleteVatReturn))
      when(correctionService.getCorrectionsForPeriod(any())(any(), any())) thenReturn Future.successful(Seq(CorrectionToCountry(Country("LT", "Lithuania"), Some(BigDecimal(10)))))

      val result = vatReturnService.getLatestVatAmountForPeriodAndCountry(Country("LT", "Lithuania"), period)

      val expected = BigDecimal(1810.00)

      assert(result.futureValue == expected)
    }

    "must return an exception when connector returns a ErrorResponse" in {

      val vatReturnConnector = mock[VatReturnConnector]
      val correctionService = mock[CorrectionService]
      val vatReturnService = new VatReturnService(vatReturnConnector, correctionService)
      when(correctionService.getCorrectionsForPeriod(any())(any(), any())) thenReturn Future.successful(Seq(CorrectionToCountry(Country("LT", "Lithuania"), Some(BigDecimal(10)))))
      when(vatReturnConnector.get(any())(any())) thenReturn Future.successful(Left(UnexpectedResponseStatus(1, "error")))

      val result = vatReturnService.getLatestVatAmountForPeriodAndCountry(Country("LT", "Lithuania"), period)

      whenReady(result.failed) { exp => exp mustBe a[Exception] }
    }
  }

  trait Fixture {
    protected val connector = mock[VatReturnConnector]
    protected val correctionService = mock[CorrectionService]

    protected val service = new VatReturnService(connector, correctionService)

    protected val country1: Country             = arbitrary[Country].sample.value
    protected val country2: Country             = arbitrary[Country].sample.value
    protected val country3: Country             = arbitrary[Country].sample.value
    protected val country4: Country             = arbitrary[Country].sample.value
    protected val vatRate1: VatRate             = arbitrary[VatRate].sample.value
    protected val vatRate2: VatRate             = arbitrary[VatRate].sample.value
    protected val vatRate3: VatRate             = arbitrary[VatRate].sample.value
    protected val domainVatRate1: DomainVatRate = toDomainVatRate(vatRate1)
    protected val domainVatRate2: DomainVatRate = toDomainVatRate(vatRate2)
    protected val domainVatRate3: DomainVatRate = toDomainVatRate(vatRate3)
    protected val vatOnSales1: VatOnSales       = arbitrary[VatOnSales].sample.value
    protected val vatOnSales2: VatOnSales       = arbitrary[VatOnSales].sample.value
    protected val vatOnSales3: VatOnSales       = arbitrary[VatOnSales].sample.value
    protected val netSales1: BigDecimal         = arbitrary[BigDecimal].sample.value
    protected val netSales2: BigDecimal         = arbitrary[BigDecimal].sample.value
    protected val netSales3: BigDecimal         = arbitrary[BigDecimal].sample.value

    protected val registrationWithoutEuDetails: Registration = registration

    private def toDomainVatRate(vatRate: VatRate): DomainVatRate = {
      DomainVatRate(
        vatRate.rate,
        if(vatRate.rateType == VatRateType.Reduced) {
          DomainVatRateType.Reduced
        } else {
          DomainVatRateType.Standard
        }
      )
    }
  }


}
