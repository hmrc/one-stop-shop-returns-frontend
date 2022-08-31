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
import models.exclusions.ExcludedTrader
import org.mockito.{Mockito, MockitoSugar}
import org.scalacheck.Gen
import org.scalatest.BeforeAndAfterEach
import uk.gov.hmrc.domain.Vrn

import java.time.LocalDate

class ExclusionServiceSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  val mockConfig = mock[FrontendAppConfig]
  val service = new ExclusionService(mockConfig)
  private val exclusionSource = Gen.oneOf("HMRC", "TRADER").sample.value
  private val exclusionReason = Gen.oneOf("01", "02", "03", "04", "05", "06", "-01").sample.value.toInt
  private val date = LocalDate.now().format(ExcludedTrader.dateFormatter)

  override def beforeEach(): Unit = {
    Mockito.reset(mockConfig)
    super.beforeEach()
  }


  ".findExcludedTrader" - {

    "must return ExcludedTrader if vrn is matched" in {

      when(mockConfig.exclusions) thenReturn Seq(ExcludedTrader(Vrn("123456789"), exclusionSource, exclusionReason, date))

      val expected = ExcludedTrader(vrn, exclusionSource, exclusionReason, date)

      service.findExcludedTrader(vrn) mustBe Some(expected)

    }

    "must return None if vrn is not matched" in {

      when(mockConfig.exclusions) thenReturn Seq.empty

      val expected = None

      service.findExcludedTrader(vrn) mustBe expected

    }
  }

}
