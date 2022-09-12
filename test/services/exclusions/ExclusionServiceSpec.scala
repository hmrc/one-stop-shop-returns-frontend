/*
 * Copyright 2022 HM Revenue & Customs
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

package services.exclusions

import base.SpecBase
import config.FrontendAppConfig
import models.Period
import models.Quarter.Q3
import models.exclusions.ExcludedTrader
import org.mockito.{Mockito, MockitoSugar}
import org.scalacheck.Gen
import org.scalatest.BeforeAndAfterEach
import play.api.inject.bind
import uk.gov.hmrc.domain.Vrn

import scala.util.{Failure, Success, Try}


class ExclusionServiceSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  val mockConfig = mock[FrontendAppConfig]
  val service = new ExclusionService(mockConfig)
  private val exclusionSource = Gen.oneOf("HMRC", "TRADER").sample.value
  private val exclusionReason = Gen.oneOf("01", "02", "03", "04", "05", "06", "-01").sample.value.toInt
  private val exclusionPeriod = Period(2022, Q3)

  override def beforeEach(): Unit = {
    Mockito.reset(mockConfig)
    super.beforeEach()
  }


  ".findExcludedTrader" - {

    "must return ExcludedTrader if vrn is matched" in {

      when(mockConfig.excludedTraders) thenReturn Seq(ExcludedTrader(Vrn("123456789"), exclusionSource, exclusionReason, exclusionPeriod))

      val expected = ExcludedTrader(vrn, exclusionSource, exclusionReason, exclusionPeriod)

      service.findExcludedTrader(vrn).futureValue mustBe Some(expected)

    }

    "must return None if vrn is not matched" in {

      when(mockConfig.excludedTraders) thenReturn Seq.empty

      service.findExcludedTrader(vrn).futureValue mustBe None

    }

    "must return an Exception when excluded trader effective period is not parsed correctly" in {

      val exclusionService: ExclusionService = mock[ExclusionService]

      Try {
        applicationBuilder(None)
          .overrides(bind[ExclusionService].toInstance(exclusionService))
          .configure("features.exclusions.excluded-traders.1.effectivePeriod" -> "fail")
          .build()
      } match {
        case Success(_) => fail("failed")
        case Failure(exception) =>
          exception mustBe a[Exception]
          exception.getCause.getMessage mustBe ("Unable to parse period")
      }
    }
  }

}
