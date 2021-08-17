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

import generators.Generators
import models.VatRateType.{Reduced, Standard}
import models.{Country, Period, VatRate}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.OptionValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.{Configuration, Environment}

import java.io.ByteArrayInputStream

class VatRateServiceSpec
  extends AnyFreeSpec
    with Matchers
    with MockitoSugar
    with Generators
    with OptionValues {

  ".vatRates" - {

    "must get all VAT rates for a country that were valid on or before the last day of the period" - {

      "and which have no end date" in {

        val country = arbitrary[Country].sample.value
        val period  = arbitrary[Period].sample.value

        val rates: Map[String, Seq[VatRate]] = Map(
          country.code -> Seq(
            VatRate(BigDecimal(0), Standard, period.firstDay.minusDays(1)),
            VatRate(BigDecimal(1), Reduced, period.firstDay),
            VatRate(BigDecimal(2), Reduced, period.lastDay),
            VatRate(BigDecimal(3), Reduced, period.lastDay.plusDays(1))
          )
        )
        val ratesBytes = Json.toJson(rates).toString.getBytes

        val mockEnv = mock[Environment]
        val mockConfig = mock[Configuration]
        when(mockConfig.get[String](any())(any())).thenReturn("foo")
        when(mockEnv.resourceAsStream(any())).thenReturn(Some(new ByteArrayInputStream(ratesBytes)))

        val service = new VatRateService(mockEnv, mockConfig)

        val result = service.vatRates(period, country)

        result must contain theSameElementsAs Seq(
          VatRate(BigDecimal(0), Standard, period.firstDay.minusDays(1)),
          VatRate(BigDecimal(1), Reduced, period.firstDay),
          VatRate(BigDecimal(2), Reduced, period.lastDay)
        )
      }

      "and which have end dates that are on or after the first day of the period" in {

        val country = arbitrary[Country].sample.value
        val period  = arbitrary[Period].sample.value

        val rates: Map[String, Seq[VatRate]] = Map(
          country.code -> Seq(
            VatRate(BigDecimal(0), Standard, period.firstDay.minusMonths(1), Some(period.firstDay)),
            VatRate(BigDecimal(1), Reduced, period.firstDay.minusMonths(1), Some(period.firstDay.plusDays(1))),
            VatRate(BigDecimal(2), Reduced, period.lastDay.minusMonths(1), Some(period.firstDay.minusDays(1)))
          )
        )
        val ratesBytes = Json.toJson(rates).toString.getBytes

        val mockEnv = mock[Environment]
        val mockConfig = mock[Configuration]
        when(mockConfig.get[String](any())(any())).thenReturn("foo")
        when(mockEnv.resourceAsStream(any())).thenReturn(Some(new ByteArrayInputStream(ratesBytes)))

        val service = new VatRateService(mockEnv, mockConfig)

        val result = service.vatRates(period, country)

        result must contain theSameElementsAs Seq(
          VatRate(BigDecimal(0), Standard, period.firstDay.minusMonths(1), Some(period.firstDay)),
          VatRate(BigDecimal(1), Reduced, period.firstDay.minusMonths(1), Some(period.firstDay.plusDays(1)))
        )
      }
    }
  }
}
