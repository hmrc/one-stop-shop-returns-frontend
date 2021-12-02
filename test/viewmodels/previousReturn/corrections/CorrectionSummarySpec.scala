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

package viewmodels.previousReturn.corrections

import base.SpecBase
import models.{Country, Period, VatOnSales}
import models.Quarter._
import models.VatOnSalesChoice.Standard
import models.corrections.{CorrectionPayload, CorrectionToCountry, PeriodWithCorrections}
import models.domain.{SalesDetails, SalesToCountry, VatRate => DomainVatRate, VatRateType => DomainVatRateType}
import play.api.i18n.Messages
import play.api.test.Helpers.stubMessages
import org.scalacheck.Arbitrary.arbitrary
import uk.gov.hmrc.domain.Vrn

import java.time.Instant

class CorrectionSummarySpec extends SpecBase {

  implicit val m: Messages = stubMessages()

  "groupByCountry" - {

    "should add multiple countries with corrections and vat return" in {

      val country1 = arbitrary[Country].sample.value

      val correctionPayload = CorrectionPayload(
        vrn = completeVatReturn.vrn,
        period = completeVatReturn.period,
        corrections = List(
          PeriodWithCorrections(
            correctionReturnPeriod = Period(2021, Q3),
            correctionsToCountry = List(
              CorrectionToCountry(country1, BigDecimal(10))
            )),
          PeriodWithCorrections(
            correctionReturnPeriod = Period(2021, Q4),
            correctionsToCountry = List(
              CorrectionToCountry(country1, BigDecimal(10))
            ))
        ),
        submissionReceived = Instant.now(),
        lastUpdated = Instant.now()
      )

      val vatReturn = completeVatReturn.copy(
        salesFromNi = List(SalesToCountry(country1,
          List(SalesDetails(DomainVatRate(10.00,
            DomainVatRateType.Reduced),
            1000.00,
            VatOnSales(Standard, 100.00))))),
        salesFromEu = List.empty
      )

      CorrectionSummary.groupByCountry(correctionPayload, vatReturn) mustBe Map(country1 -> BigDecimal(120))
    }

    "should add multiple countries with negative corrections" in {

      val country1 = arbitrary[Country].sample.value
      val country2 = arbitrary[Country].suchThat(_ != country1).sample.value

      val correctionPayload = CorrectionPayload(
        vrn = arbitrary[Vrn].sample.value,
        period = arbitrary[Period].sample.value,
        corrections = List(
          PeriodWithCorrections(
            correctionReturnPeriod = Period(2021, Q3),
            correctionsToCountry = List(
              CorrectionToCountry(country1, BigDecimal(10)),
              CorrectionToCountry(country2, BigDecimal(-10))
            )),
          PeriodWithCorrections(
            correctionReturnPeriod = Period(2021, Q4),
            correctionsToCountry = List(
              CorrectionToCountry(country1, BigDecimal(-10)),
              CorrectionToCountry(country2, BigDecimal(10))
            ))
        ),
        submissionReceived = Instant.now(),
        lastUpdated = Instant.now()
      )

      val vatReturn = completeVatReturn.copy(
        salesFromNi = List(SalesToCountry(country1,
          List(SalesDetails(DomainVatRate(10.00,
            DomainVatRateType.Reduced),
            1000.00,
            VatOnSales(Standard, 100.00))))),
        salesFromEu = List.empty
      )

      CorrectionSummary.groupByCountry(correctionPayload, vatReturn) must contain.theSameElementsAs(
        Map(
          country1 -> BigDecimal(100),
          country2 -> BigDecimal(0)
        )
      )
    }

    "should have nil return with with a mix of corrections" in {

      val country1 = arbitrary[Country].sample.value
      val country2 = arbitrary[Country].suchThat(_ != country1).sample.value

      val correctionPayload = CorrectionPayload(
        vrn = arbitrary[Vrn].sample.value,
        period = arbitrary[Period].sample.value,
        corrections = List(
          PeriodWithCorrections(
            correctionReturnPeriod = Period(2021, Q3),
            correctionsToCountry = List(
              CorrectionToCountry(country1, BigDecimal(52.44)),
              CorrectionToCountry(country2, BigDecimal(-589.24))
            )),
          PeriodWithCorrections(
            correctionReturnPeriod = Period(2021, Q4),
            correctionsToCountry = List(
              CorrectionToCountry(country1, BigDecimal(-10)),
            ))
        ),
        submissionReceived = Instant.now(),
        lastUpdated = Instant.now()
      )

      val vatReturn = emptyVatReturn

      CorrectionSummary.groupByCountry(correctionPayload, vatReturn) must contain.theSameElementsAs(
        Map(
          country1 -> BigDecimal(42.44),
          country2 -> BigDecimal(-589.24)
        )
      )
    }

  }

}
