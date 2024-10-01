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
import connectors.EuVatRateConnector
import models.{Country, EuVatRate, StandardPeriod, VatRate}
import models.VatRateType.{Reduced, Standard}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll
import uk.gov.hmrc.http.HeaderCarrier
import utils.FutureSyntax.FutureOps

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.math.BigDecimal.RoundingMode.HALF_EVEN

class VatRateServiceSpec
  extends SpecBase
    with MockitoSugar
    with BeforeAndAfterEach {

  private implicit lazy val emptyHC: HeaderCarrier = HeaderCarrier()

  private val mockEuVatRateConnector: EuVatRateConnector = mock[EuVatRateConnector]

  override def beforeEach(): Unit = {
    Mockito.reset(mockEuVatRateConnector)
  }

  ".vatRates" - {

    "covert all EuVatRates to VatRates" - {

      "and which have no end date and removes 0% rate" in {

        val country = arbitrary[Country].sample.value
        val period = arbitrary[StandardPeriod].sample.value

        val rates: Seq[EuVatRate] = Seq(
          EuVatRate(country, BigDecimal(0), Standard, period.firstDay.minusDays(1)),
          EuVatRate(country, BigDecimal(1), Reduced, period.firstDay),
          EuVatRate(country, BigDecimal(2), Reduced, period.lastDay),
          EuVatRate(country, BigDecimal(3), Reduced, period.lastDay.plusDays(1))
        )

        when(mockEuVatRateConnector.getEuVatRates(any(), any(), any())(any())) thenReturn rates.toFuture

        val service = new VatRateService(mockEuVatRateConnector)

        val result = service.vatRates(period, country).futureValue

        result must contain theSameElementsAs Seq(
          VatRate(BigDecimal(1), Reduced, period.firstDay),
          VatRate(BigDecimal(2), Reduced, period.lastDay),
          VatRate(BigDecimal(3), Reduced, period.lastDay.plusDays(1))
        )
      }

    }
  }

  ".normalVatOnSales" - {

    val service = new VatRateService(mockEuVatRateConnector)

    "must equal the net sales multiplied by the VAT rate as a percentage, rounded to 2 decimal places" - {

      "generally" in {

        forAll(Gen.choose[BigDecimal](1, 1000000), arbitrary[VatRate]) {
          case (netSales, vatRate) =>

            val result = service.standardVatOnSales(netSales, vatRate)
            result mustEqual ((netSales * vatRate.rate) / 100).setScale(2, HALF_EVEN)
        }
      }

      "specific examples" - {

        "£100 at 20% must equal £20" in {

          val vatRate = VatRate(20, Standard, LocalDate.now)
          service.standardVatOnSales(100, vatRate) mustEqual BigDecimal(20)
        }

        "£100.024 at 20% must equal £20" in {

          val vatRate = VatRate(20, Standard, LocalDate.now)
          service.standardVatOnSales(BigDecimal(100.024), vatRate) mustEqual BigDecimal(20)
        }

        "£100.025 at 20% must equal £20.00" in {

          val vatRate = VatRate(20, Standard, LocalDate.now)
          service.standardVatOnSales(BigDecimal(100.025), vatRate) mustEqual BigDecimal(20.00)
        }

        "£100 at 20.004% must equal £20.00" in {

          val vatRate = VatRate(BigDecimal(20.004), Standard, LocalDate.now)
          service.standardVatOnSales(100, vatRate) mustEqual BigDecimal(20)
        }

        "£100 at 20.005% must equal £20.00" in {

          val vatRate = VatRate(BigDecimal(20.005), Standard, LocalDate.now)
          service.standardVatOnSales(100, vatRate) mustEqual BigDecimal(20.00)
        }
      }
    }
  }
}
