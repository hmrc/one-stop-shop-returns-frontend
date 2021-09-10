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
import cats.data.NonEmptyChain
import cats.data.Validated.{Invalid, Valid}
import models.domain.EuTaxIdentifierType.{Other, Vat}
import models.domain.{EuTaxIdentifier, SalesDetails, SalesFromEuCountry, SalesToCountry}
import models.registration._
import models.{Country, DataMissingError, Index, SalesAtVatRate, VatOnSales, VatRate}
import models.requests.VatReturnRequest
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages._
import queries.{AllSalesFromEuQuery, AllSalesFromNiQuery, NiSalesAtVatRateQuery}

class VatReturnServiceSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {
  
  ".fromUserAnswers" - {

    "must return a vat return request" - {

      "when the user has not sold any goods" in new Fixture {

        private val answers =
          emptyUserAnswers
            .set(SoldGoodsFromNiPage, false).success.value
            .set(SoldGoodsFromEuPage, false).success.value

        private val expectedResult = VatReturnRequest(vrn, period, None, None, List.empty, List.empty)

        service.fromUserAnswers(answers, vrn, period, registrationWithoutEuDetails) mustEqual Valid(expectedResult)
      }

      "when the user has sold goods from NI" in new Fixture {

        private val answers =
          emptyUserAnswers
            .set(SoldGoodsFromNiPage, true).success.value
            .set(CountryOfConsumptionFromNiPage(Index(0)), country1).success.value
            .set(VatRatesFromNiPage(Index(0)), List(vatRate1)).success.value
            .set(NetValueOfSalesFromNiPage(Index(0), Index(0)), netValueOfSales1).success.value
            .set(VatOnSalesFromNiPage(Index(0), Index(0)), vatOnSales1).success.value
            .set(CountryOfConsumptionFromNiPage(Index(1)), country2).success.value
            .set(VatRatesFromNiPage(Index(1)), List(vatRate2, vatRate3)).success.value
            .set(NetValueOfSalesFromNiPage(Index(1), Index(0)), netValueOfSales2).success.value
            .set(VatOnSalesFromNiPage(Index(1), Index(0)), vatOnSales2).success.value
            .set(NetValueOfSalesFromNiPage(Index(1), Index(1)), netValueOfSales3).success.value
            .set(VatOnSalesFromNiPage(Index(1), Index(1)), vatOnSales3).success.value
            .set(SoldGoodsFromEuPage, false).success.value

        private val expectedResult =
          VatReturnRequest(
            vrn,
            period,
            None,
            None,
            List(
              SalesToCountry(
                country1,
                List(SalesDetails(vatRate1, netValueOfSales1, vatOnSales1.amount))
              ),
              SalesToCountry(
                country2,
                List(
                  SalesDetails(vatRate2, netValueOfSales2, vatOnSales2.amount),
                  SalesDetails(vatRate3, netValueOfSales3, vatOnSales3.amount)
                )
              )
            ),
            List.empty
          )

        service.fromUserAnswers(answers, vrn, period, registrationWithoutEuDetails) mustEqual Valid(expectedResult)
      }

      "when the user has sold goods from the EU" - {

        "with no EU registrations" in new Fixture {

          private val answers =
            emptyUserAnswers
              .set(SoldGoodsFromNiPage, false).success.value
              .set(SoldGoodsFromEuPage, true).success.value
              .set(CountryOfSaleFromEuPage(Index(0)), country1).success.value
              .set(CountryOfConsumptionFromEuPage(Index(0), Index(0)), country2).success.value
              .set(VatRatesFromEuPage(Index(0), Index(0)), List(vatRate1)).success.value
              .set(SalesAtVatRateFromEuPage(Index(0), Index(0), Index(0)), salesDetails1).success.value
              .set(CountryOfConsumptionFromEuPage(Index(0), Index(1)), country3).success.value
              .set(VatRatesFromEuPage(Index(0), Index(1)), List(vatRate2, vatRate3)).success.value
              .set(SalesAtVatRateFromEuPage(Index(0), Index(1), Index(0)), salesDetails2).success.value
              .set(SalesAtVatRateFromEuPage(Index(0), Index(1), Index(1)), salesDetails3).success.value

          private val expectedResult =
            VatReturnRequest(
              vrn,
              period,
              None,
              None,
              List.empty,
              List(
                SalesFromEuCountry(
                  country1,
                  None,
                  List(
                    SalesToCountry(
                      country2,
                      List(SalesDetails(vatRate1, salesDetails1.netValueOfSales, salesDetails1.vatOnSales))
                    ),
                    SalesToCountry(
                      country3,
                      List(
                        SalesDetails(vatRate2, salesDetails2.netValueOfSales, salesDetails2.vatOnSales),
                        SalesDetails(vatRate3, salesDetails3.netValueOfSales, salesDetails3.vatOnSales)
                      )
                    )
                  )
                )
              )
            )

          service.fromUserAnswers(answers, vrn, period, registrationWithoutEuDetails) mustEqual Valid(expectedResult)
        }

        "with EU registrations" in new Fixture {

          private val registration = registrationWithoutEuDetails.copy (
            euRegistrations = Seq(
              EuVatRegistration(country1, "VAT number"),
              RegistrationWithFixedEstablishment(country3, EuTaxIdentifier(Other, "Tax identifier"), arbitrary[FixedEstablishment].sample.value),
              RegistrationWithoutFixedEstablishment(country4)
            )
          )

          private val answers =
            emptyUserAnswers
              .set(SoldGoodsFromNiPage, false).success.value
              .set(SoldGoodsFromEuPage, true).success.value
              .set(CountryOfSaleFromEuPage(Index(0)), country1).success.value
              .set(CountryOfConsumptionFromEuPage(Index(0), Index(0)), country2).success.value
              .set(VatRatesFromEuPage(Index(0), Index(0)), List(vatRate1)).success.value
              .set(SalesAtVatRateFromEuPage(Index(0), Index(0), Index(0)), salesDetails1).success.value
              .set(CountryOfSaleFromEuPage(Index(1)), country3).success.value
              .set(CountryOfConsumptionFromEuPage(Index(1), Index(0)), country2).success.value
              .set(VatRatesFromEuPage(Index(1), Index(0)), List(vatRate2)).success.value
              .set(SalesAtVatRateFromEuPage(Index(1), Index(0), Index(0)), salesDetails2).success.value
              .set(CountryOfSaleFromEuPage(Index(2)), country4).success.value
              .set(CountryOfConsumptionFromEuPage(Index(2), Index(0)), country2).success.value
              .set(VatRatesFromEuPage(Index(2), Index(0)), List(vatRate3)).success.value
              .set(SalesAtVatRateFromEuPage(Index(2), Index(0), Index(0)), salesDetails3).success.value

          private val expectedResult =
            VatReturnRequest(
              vrn,
              period,
              None,
              None,
              List.empty,
              List(
                SalesFromEuCountry(
                  country1,
                  Some(EuTaxIdentifier(Vat, "VAT number")),
                  List(
                    SalesToCountry(
                      country2,
                      List(SalesDetails(vatRate1, salesDetails1.netValueOfSales, salesDetails1.vatOnSales))
                    )
                  )
                ),
                SalesFromEuCountry(
                  country3,
                  Some(EuTaxIdentifier(Other, "Tax identifier")),
                  List(
                    SalesToCountry(
                      country2,
                      List(SalesDetails(vatRate2, salesDetails2.netValueOfSales, salesDetails2.vatOnSales))
                    )
                  )
                ),
                SalesFromEuCountry(
                  country4,
                  None,
                  List(
                    SalesToCountry(
                      country2,
                      List(SalesDetails(vatRate3, salesDetails3.netValueOfSales, salesDetails3.vatOnSales))
                    )
                  )
                )
              )
            )

          service.fromUserAnswers(answers, vrn, period, registration) mustEqual Valid(expectedResult)
        }
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

      "when there is a country of consumption with no corresponding VAT rates" in new Fixture {

        private val answers =
          emptyUserAnswers
            .set(SoldGoodsFromNiPage, true).success.value
            .set(CountryOfConsumptionFromNiPage(Index(0)), country1).success.value
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
            .set(NetValueOfSalesFromNiPage(Index(0), Index(0)), netValueOfSales1).success.value
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
            .set(NetValueOfSalesFromNiPage(Index(0), Index(0)), netValueOfSales1).success.value
            .set(VatOnSalesFromNiPage(Index(0), Index(0)), vatOnSales1).success.value
            .set(NetValueOfSalesFromNiPage(Index(0), Index(1)), netValueOfSales2).success.value
            .set(SoldGoodsFromEuPage, false).success.value

        private val result = service.fromUserAnswers(answers, vrn, period, registrationWithoutEuDetails)
        result mustEqual Invalid(NonEmptyChain(DataMissingError(AllSalesFromNiQuery)))
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
        result mustEqual Invalid(NonEmptyChain(DataMissingError(AllSalesFromEuQuery)))
      }

      "when there is a country of sale and country of consumption from EU, but no other details" in new Fixture {

        private val answers =
          emptyUserAnswers
            .set(SoldGoodsFromNiPage, false).success.value
            .set(SoldGoodsFromEuPage, true).success.value
            .set(CountryOfSaleFromEuPage(Index(0)), country1).success.value
            .set(CountryOfConsumptionFromEuPage(Index(0), Index(0)), country2).success.value

        private val result = service.fromUserAnswers(answers, vrn, period, registrationWithoutEuDetails)
        result mustEqual Invalid(NonEmptyChain(DataMissingError(AllSalesFromEuQuery)))
      }

      "when there is a VAT rate from the EU with no corresponding sales details" in new Fixture {

        private val answers =
          emptyUserAnswers
            .set(SoldGoodsFromNiPage, false).success.value
            .set(SoldGoodsFromEuPage, true).success.value
            .set(CountryOfSaleFromEuPage(Index(0)), country1).success.value
            .set(CountryOfConsumptionFromEuPage(Index(0), Index(0)), country2).success.value
            .set(VatRatesFromEuPage(Index(0), Index(0)), List(vatRate1)).success.value

        private val result = service.fromUserAnswers(answers, vrn, period, registrationWithoutEuDetails)
        result mustEqual Invalid(NonEmptyChain(DataMissingError(AllSalesFromEuQuery)))
      }
    }
  }

  trait Fixture {

    protected val service = new VatReturnService()

    protected val country1: Country             = arbitrary[Country].sample.value
    protected val country2: Country             = arbitrary[Country].sample.value
    protected val country3: Country             = arbitrary[Country].sample.value
    protected val country4: Country             = arbitrary[Country].sample.value
    protected val vatRate1: VatRate             = arbitrary[VatRate].sample.value
    protected val vatRate2: VatRate             = arbitrary[VatRate].sample.value
    protected val vatRate3: VatRate             = arbitrary[VatRate].sample.value
    protected val salesDetails1: SalesAtVatRate = arbitrary[SalesAtVatRate].sample.value
    protected val salesDetails2: SalesAtVatRate = arbitrary[SalesAtVatRate].sample.value
    protected val salesDetails3: SalesAtVatRate = arbitrary[SalesAtVatRate].sample.value
    protected val vatOnSales1: VatOnSales       = arbitrary[VatOnSales].sample.value
    protected val vatOnSales2: VatOnSales       = arbitrary[VatOnSales].sample.value
    protected val vatOnSales3: VatOnSales       = arbitrary[VatOnSales].sample.value
    protected val netValueOfSales1: BigDecimal  = arbitrary[BigDecimal].sample.value
    protected val netValueOfSales2: BigDecimal  = arbitrary[BigDecimal].sample.value
    protected val netValueOfSales3: BigDecimal  = arbitrary[BigDecimal].sample.value

    protected val registrationWithoutEuDetails: Registration = registration
  }
}
