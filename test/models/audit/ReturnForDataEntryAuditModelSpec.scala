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

package models.audit

import base.SpecBase
import models._
import models.corrections.{CorrectionToCountry, PeriodWithCorrections}
import models.domain.{EuTaxIdentifier, SalesDetails, SalesFromEuCountry, SalesToCountry, VatRate => DomainVatRate, VatRateType => DomainVatRateType}
import models.requests.VatReturnRequest
import models.requests.corrections.CorrectionRequest
import org.scalacheck.Arbitrary.arbitrary
import play.api.libs.json.Json

class ReturnForDataEntryAuditModelSpec extends SpecBase {

  "detail" - {

    "must be a valid structure when there are no sales from NI or the EU" in {

      val vatReturnRequest = VatReturnRequest(vrn, period, None,None, Nil, Nil)
      val returnReference  = ReturnReference(vrn, period)
      val paymentReference = PaymentReference(vrn, period)

      val auditModel = ReturnForDataEntryAuditModel(vatReturnRequest, None, returnReference, paymentReference)

      auditModel.detail mustEqual Json.obj(
        "vatRegistrationNumber" -> vrn.vrn,
        "period"                -> Json.toJson(period),
        "returnReference"       -> Json.toJson(returnReference),
        "paymentReference"      -> Json.toJson(paymentReference),
        "salesFromNi"           -> Json.arr(),
        "salesFromEu"           -> Json.arr()
      )
    }

    "must be a valid structure when there are sales from NI and the EU without corrections" in new Fixture {

      private val salesFromNi = List(
        SalesToCountry(
          countryOfConsumption = country1,
          amounts = List(
            SalesDetails(domainVatRate1, netSales1, vatOnSales1),
            SalesDetails(domainVatRate2, netSales2, vatOnSales2)
          )
        ),
        SalesToCountry(
          countryOfConsumption = country2,
          amounts = List(
            SalesDetails(domainVatRate3, netSales3, vatOnSales3)
          )
        )
      )

      private val salesFromEu = List(
        SalesFromEuCountry(
          countryOfSale = country1,
          taxIdentifier = None,
          sales = List(
            SalesToCountry(
              countryOfConsumption = country2,
              amounts = List(
                SalesDetails(domainVatRate1, netSales1, vatOnSales1),
                SalesDetails(domainVatRate2, netSales2, vatOnSales2)
              )
            ),
            SalesToCountry(
              countryOfConsumption = country3,
              amounts = List(
                SalesDetails(domainVatRate3, netSales3, vatOnSales3)
              )
            )
          )
        ),
        SalesFromEuCountry(
          countryOfSale = country3,
          taxIdentifier = Some(taxIdentifier),
          sales = List(
            SalesToCountry(
              countryOfConsumption = country4,
              amounts = List(
                SalesDetails(domainVatRate1, netSales1, vatOnSales1)
              )
            )
          )
        )
      )

      private val vatReturnRequest = VatReturnRequest(vrn, period, None,None, salesFromNi, salesFromEu)
      private val returnReference  = ReturnReference(vrn, period)
      private val paymentReference = PaymentReference(vrn, period)

      private val auditModel = ReturnForDataEntryAuditModel(vatReturnRequest, None, returnReference, paymentReference)

      auditModel.detail mustEqual Json.obj(
        "vatRegistrationNumber" -> vrn.vrn,
        "period"                -> Json.toJson(period),
        "returnReference"       -> Json.toJson(returnReference),
        "paymentReference"      -> Json.toJson(paymentReference),
        "salesFromNi"           -> Json.arr(
          Json.obj(
            "countryOfConsumption" -> country1.name,
            "vatRate"              -> domainVatRate1.rate,
            "vatRateType"          -> domainVatRate1.rateType,
            "netValueOfSales"      -> netSales1,
            "vatOnSales"           -> vatOnSales1.amount
          ),
          Json.obj(
            "countryOfConsumption" -> country1.name,
            "vatRate"              -> domainVatRate2.rate,
            "vatRateType"          -> domainVatRate2.rateType,
            "netValueOfSales"      -> netSales2,
            "vatOnSales"           -> vatOnSales2.amount
          ),
          Json.obj(
            "countryOfConsumption" -> country2.name,
            "vatRate"              -> domainVatRate3.rate,
            "vatRateType"          -> domainVatRate3.rateType,
            "netValueOfSales"      -> netSales3,
            "vatOnSales"           -> vatOnSales3.amount
          )
        ),
        "salesFromEu" -> Json.arr(
          Json.obj(
            "countryOfSale"        -> country1.name,
            "countryOfConsumption" -> country2.name,
            "vatRate"              -> domainVatRate1.rate,
            "vatRateType"          -> domainVatRate1.rateType,
            "netValueOfSales"      -> netSales1,
            "vatOnSales"           -> vatOnSales1.amount
          ),
          Json.obj(
            "countryOfSale"        -> country1.name,
            "countryOfConsumption" -> country2.name,
            "vatRate"              -> domainVatRate2.rate,
            "vatRateType"          -> domainVatRate2.rateType,
            "netValueOfSales"      -> netSales2,
            "vatOnSales"           -> vatOnSales2.amount
          ),
          Json.obj(
            "countryOfSale"        -> country1.name,
            "countryOfConsumption" -> country3.name,
            "vatRate"              -> domainVatRate3.rate,
            "vatRateType"          -> domainVatRate3.rateType,
            "netValueOfSales"      -> netSales3,
            "vatOnSales"           -> vatOnSales3.amount
          ),
          Json.obj(
            "countryOfSale"        -> country3.name,
            "countryOfConsumption" -> country4.name,
            "vatRate"              -> domainVatRate1.rate,
            "vatRateType"          -> domainVatRate1.rateType,
            "netValueOfSales"      -> netSales1,
            "vatOnSales"           -> vatOnSales1.amount,
            "taxIdentifierType"    -> taxIdentifier.identifierType.toString,
            "taxIdentifier"        -> taxIdentifier.value
          )
        )
      )
    }

    "must be a valid structure when there are sales from NI and the EU with corrections" in new Fixture {

      private val salesFromNi = List(
        SalesToCountry(
          countryOfConsumption = country1,
          amounts = List(
            SalesDetails(domainVatRate1, netSales1, vatOnSales1),
            SalesDetails(domainVatRate2, netSales2, vatOnSales2)
          )
        ),
        SalesToCountry(
          countryOfConsumption = country2,
          amounts = List(
            SalesDetails(domainVatRate3, netSales3, vatOnSales3)
          )
        )
      )

      private val salesFromEu = List(
        SalesFromEuCountry(
          countryOfSale = country1,
          taxIdentifier = None,
          sales = List(
            SalesToCountry(
              countryOfConsumption = country2,
              amounts = List(
                SalesDetails(domainVatRate1, netSales1, vatOnSales1),
                SalesDetails(domainVatRate2, netSales2, vatOnSales2)
              )
            ),
            SalesToCountry(
              countryOfConsumption = country3,
              amounts = List(
                SalesDetails(domainVatRate3, netSales3, vatOnSales3)
              )
            )
          )
        ),
        SalesFromEuCountry(
          countryOfSale = country3,
          taxIdentifier = Some(taxIdentifier),
          sales = List(
            SalesToCountry(
              countryOfConsumption = country4,
              amounts = List(
                SalesDetails(domainVatRate1, netSales1, vatOnSales1)
              )
            )
          )
        )
      )

      private val corrections = List(PeriodWithCorrections(
        correctionPeriod1,
        Some(List(CorrectionToCountry(
          country3,
          Some(correctionAmount)
        )))
      ))

      private val vatReturnRequest = VatReturnRequest(vrn, period, None, None, salesFromNi, salesFromEu)
      private val correctionRequest = CorrectionRequest(vrn, period, corrections)
      private val returnReference  = ReturnReference(vrn, period)
      private val paymentReference = PaymentReference(vrn, period)

      private val auditModel = ReturnForDataEntryAuditModel(vatReturnRequest, Some(correctionRequest), returnReference, paymentReference)

      auditModel.detail mustEqual Json.obj(
        "vatRegistrationNumber" -> vrn.vrn,
        "period"                -> Json.toJson(period),
        "returnReference"       -> Json.toJson(returnReference),
        "paymentReference"      -> Json.toJson(paymentReference),
        "salesFromNi"           -> Json.arr(
          Json.obj(
            "countryOfConsumption" -> country1.name,
            "vatRate"              -> domainVatRate1.rate,
            "vatRateType"          -> domainVatRate1.rateType,
            "netValueOfSales"      -> netSales1,
            "vatOnSales"           -> vatOnSales1.amount
          ),
          Json.obj(
            "countryOfConsumption" -> country1.name,
            "vatRate"              -> domainVatRate2.rate,
            "vatRateType"          -> domainVatRate2.rateType,
            "netValueOfSales"      -> netSales2,
            "vatOnSales"           -> vatOnSales2.amount
          ),
          Json.obj(
            "countryOfConsumption" -> country2.name,
            "vatRate"              -> domainVatRate3.rate,
            "vatRateType"          -> domainVatRate3.rateType,
            "netValueOfSales"      -> netSales3,
            "vatOnSales"           -> vatOnSales3.amount
          )
        ),
        "salesFromEu" -> Json.arr(
          Json.obj(
            "countryOfSale"        -> country1.name,
            "countryOfConsumption" -> country2.name,
            "vatRate"              -> domainVatRate1.rate,
            "vatRateType"          -> domainVatRate1.rateType,
            "netValueOfSales"      -> netSales1,
            "vatOnSales"           -> vatOnSales1.amount
          ),
          Json.obj(
            "countryOfSale"        -> country1.name,
            "countryOfConsumption" -> country2.name,
            "vatRate"              -> domainVatRate2.rate,
            "vatRateType"          -> domainVatRate2.rateType,
            "netValueOfSales"      -> netSales2,
            "vatOnSales"           -> vatOnSales2.amount
          ),
          Json.obj(
            "countryOfSale"        -> country1.name,
            "countryOfConsumption" -> country3.name,
            "vatRate"              -> domainVatRate3.rate,
            "vatRateType"          -> domainVatRate3.rateType,
            "netValueOfSales"      -> netSales3,
            "vatOnSales"           -> vatOnSales3.amount
          ),
          Json.obj(
            "countryOfSale"        -> country3.name,
            "countryOfConsumption" -> country4.name,
            "vatRate"              -> domainVatRate1.rate,
            "vatRateType"          -> domainVatRate1.rateType,
            "netValueOfSales"      -> netSales1,
            "vatOnSales"           -> vatOnSales1.amount,
            "taxIdentifierType"    -> taxIdentifier.identifierType.toString,
            "taxIdentifier"        -> taxIdentifier.value
          )
        ),
        "corrections" -> Json.arr(
          Json.obj(
            "correctionPeriod" -> correctionPeriod1,
            "correctionToCountry" -> Json.arr(
              Json.obj(
                "correctionCountry" -> country3.name,
                "countryVatCorrectionAmount" -> correctionAmount
              )
            )
          )
        )
      )
    }

    "must be a valid structure when there are sales from NI and the EU with multiple corrections" in new Fixture {

      private val salesFromNi = List(
        SalesToCountry(
          countryOfConsumption = country1,
          amounts = List(
            SalesDetails(domainVatRate1, netSales1, vatOnSales1),
            SalesDetails(domainVatRate2, netSales2, vatOnSales2)
          )
        ),
        SalesToCountry(
          countryOfConsumption = country2,
          amounts = List(
            SalesDetails(domainVatRate3, netSales3, vatOnSales3)
          )
        )
      )

      private val salesFromEu = List(
        SalesFromEuCountry(
          countryOfSale = country1,
          taxIdentifier = None,
          sales = List(
            SalesToCountry(
              countryOfConsumption = country2,
              amounts = List(
                SalesDetails(domainVatRate1, netSales1, vatOnSales1),
                SalesDetails(domainVatRate2, netSales2, vatOnSales2)
              )
            ),
            SalesToCountry(
              countryOfConsumption = country3,
              amounts = List(
                SalesDetails(domainVatRate3, netSales3, vatOnSales3)
              )
            )
          )
        ),
        SalesFromEuCountry(
          countryOfSale = country3,
          taxIdentifier = Some(taxIdentifier),
          sales = List(
            SalesToCountry(
              countryOfConsumption = country4,
              amounts = List(
                SalesDetails(domainVatRate1, netSales1, vatOnSales1)
              )
            )
          )
        )
      )

      private val corrections = List(
        PeriodWithCorrections(
          correctionPeriod1,
          Some(List(
            CorrectionToCountry(
            country3,
              Some(correctionAmount)
            )
          ))
        ),
        PeriodWithCorrections(
          correctionPeriod2,
          Some(List(
            CorrectionToCountry(
              country4,
              Some(correctionAmount)
            )
          ))
        )
      )

      private val vatReturnRequest = VatReturnRequest(vrn, period, None, None, salesFromNi, salesFromEu)
      private val correctionRequest = CorrectionRequest(vrn, period, corrections)
      private val returnReference  = ReturnReference(vrn, period)
      private val paymentReference = PaymentReference(vrn, period)

      private val auditModel = ReturnForDataEntryAuditModel(vatReturnRequest, Some(correctionRequest), returnReference, paymentReference)

      auditModel.detail mustEqual Json.obj(
        "vatRegistrationNumber" -> vrn.vrn,
        "period"                -> Json.toJson(period),
        "returnReference"       -> Json.toJson(returnReference),
        "paymentReference"      -> Json.toJson(paymentReference),
        "salesFromNi"           -> Json.arr(
          Json.obj(
            "countryOfConsumption" -> country1.name,
            "vatRate"              -> domainVatRate1.rate,
            "vatRateType"          -> domainVatRate1.rateType,
            "netValueOfSales"      -> netSales1,
            "vatOnSales"           -> vatOnSales1.amount
          ),
          Json.obj(
            "countryOfConsumption" -> country1.name,
            "vatRate"              -> domainVatRate2.rate,
            "vatRateType"          -> domainVatRate2.rateType,
            "netValueOfSales"      -> netSales2,
            "vatOnSales"           -> vatOnSales2.amount
          ),
          Json.obj(
            "countryOfConsumption" -> country2.name,
            "vatRate"              -> domainVatRate3.rate,
            "vatRateType"          -> domainVatRate3.rateType,
            "netValueOfSales"      -> netSales3,
            "vatOnSales"           -> vatOnSales3.amount
          )
        ),
        "salesFromEu" -> Json.arr(
          Json.obj(
            "countryOfSale"        -> country1.name,
            "countryOfConsumption" -> country2.name,
            "vatRate"              -> domainVatRate1.rate,
            "vatRateType"          -> domainVatRate1.rateType,
            "netValueOfSales"      -> netSales1,
            "vatOnSales"           -> vatOnSales1.amount
          ),
          Json.obj(
            "countryOfSale"        -> country1.name,
            "countryOfConsumption" -> country2.name,
            "vatRate"              -> domainVatRate2.rate,
            "vatRateType"          -> domainVatRate2.rateType,
            "netValueOfSales"      -> netSales2,
            "vatOnSales"           -> vatOnSales2.amount
          ),
          Json.obj(
            "countryOfSale"        -> country1.name,
            "countryOfConsumption" -> country3.name,
            "vatRate"              -> domainVatRate3.rate,
            "vatRateType"          -> domainVatRate3.rateType,
            "netValueOfSales"      -> netSales3,
            "vatOnSales"           -> vatOnSales3.amount
          ),
          Json.obj(
            "countryOfSale"        -> country3.name,
            "countryOfConsumption" -> country4.name,
            "vatRate"              -> domainVatRate1.rate,
            "vatRateType"          -> domainVatRate1.rateType,
            "netValueOfSales"      -> netSales1,
            "vatOnSales"           -> vatOnSales1.amount,
            "taxIdentifierType"    -> taxIdentifier.identifierType.toString,
            "taxIdentifier"        -> taxIdentifier.value
          )
        ),
        "corrections" -> Json.arr(
          Json.obj(
            "correctionPeriod" -> correctionPeriod1,
            "correctionToCountry" -> Json.arr(
              Json.obj(
                "correctionCountry" -> country3.name,
                "countryVatCorrectionAmount" -> correctionAmount
              )
            )
          ),
          Json.obj(
            "correctionPeriod" -> correctionPeriod2,
            "correctionToCountry" -> Json.arr(
              Json.obj(
                "correctionCountry" -> country4.name,
                "countryVatCorrectionAmount" -> correctionAmount
              )
            )
          )
        )
      )
    }

  }

  trait Fixture {

    protected val country1: Country              = arbitrary[Country].sample.value
    protected val country2: Country              = arbitrary[Country].sample.value
    protected val country3: Country              = arbitrary[Country].sample.value
    protected val country4: Country              = arbitrary[Country].sample.value
    protected val vatRate1: VatRate              = arbitrary[VatRate].sample.value
    protected val vatRate2: VatRate              = arbitrary[VatRate].sample.value
    protected val vatRate3: VatRate              = arbitrary[VatRate].sample.value
    protected val domainVatRate1: DomainVatRate  = toDomainVatRate(vatRate1)
    protected val domainVatRate2: DomainVatRate  = toDomainVatRate(vatRate2)
    protected val domainVatRate3: DomainVatRate  = toDomainVatRate(vatRate3)
    protected val vatOnSales1: VatOnSales        = arbitrary[VatOnSales].sample.value
    protected val vatOnSales2: VatOnSales        = arbitrary[VatOnSales].sample.value
    protected val vatOnSales3: VatOnSales        = arbitrary[VatOnSales].sample.value
    protected val netSales1: BigDecimal          = arbitrary[BigDecimal].sample.value
    protected val netSales2: BigDecimal          = arbitrary[BigDecimal].sample.value
    protected val netSales3: BigDecimal          = arbitrary[BigDecimal].sample.value
    protected val correctionPeriod1: StandardPeriod      = arbitrary[StandardPeriod].sample.value
    protected val correctionPeriod2: StandardPeriod      = arbitrary[StandardPeriod].sample.value
    protected val correctionAmount: BigDecimal   = arbitrary[BigDecimal].sample.value
    protected val taxIdentifier: EuTaxIdentifier = arbitrary[EuTaxIdentifier].sample.value

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
