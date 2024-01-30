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

import generators.Generators
import models.Quarter._
import models.StandardPeriod
import org.scalatest.OptionValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import java.time.{Clock, Instant, LocalDate, ZoneId}

class PeriodServiceSpec
  extends AnyFreeSpec
    with Matchers
    with MockitoSugar
    with ScalaCheckPropertyChecks
    with Generators
    with OptionValues {

  ".getAvailablePeriods" - {

    "when today is 11th October" - {

      val instant = Instant.ofEpochSecond(1633959834)
      val stubClock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

      "should return Q3 for commencement date of 30th September" in {
        val commencementDate = LocalDate.of(2021, 9, 30)

        val service = new PeriodService(stubClock)

        val expectedPeriods = Seq(StandardPeriod(2021, Q3))

        service.getReturnPeriods(commencementDate) must contain theSameElementsAs expectedPeriods
      }

      "should return nothing for commencement date of 10th October" in {
        val commencementDate = LocalDate.of(2021, 10, 10)

        val service = new PeriodService(stubClock)

        val expectedPeriods = Seq.empty

        service.getReturnPeriods(commencementDate) must contain theSameElementsAs expectedPeriods
      }
    }

  }
  ".getAllPeriods" - {
    "when today is 11th October" in {
      val instant = Instant.ofEpochSecond(1633959834)
      val stubClock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

      val service = new PeriodService(stubClock)

      val expectedPeriods = Seq(StandardPeriod(2021, Q3))

      service.getAllPeriods must contain theSameElementsAs expectedPeriods
    }

    "when today is 11th January" in {
      val instant = Instant.parse("2022-01-11T12:00:00Z")

      val stubClock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

      val service = new PeriodService(stubClock)

      val expectedPeriods = Seq(StandardPeriod(2021, Q3), StandardPeriod(2021, Q4))

      service.getAllPeriods must contain theSameElementsAs expectedPeriods
    }
  }
  ".getNextPeriod" - {
    "when current period is Q1" in {
      val year = 2021
      val current = StandardPeriod(year, Q1)
      val expected = StandardPeriod(year, Q2)
      val instant = Instant.parse("2022-01-11T12:00:00Z")
      val stubClock: Clock = Clock.fixed(instant, ZoneId.systemDefault)
      val service = new PeriodService(stubClock)
      service.getNextPeriod(current) mustBe expected
    }

    "when current period is Q2" in {
      val year = 2021
      val current = StandardPeriod(year, Q2)
      val expected = StandardPeriod(year, Q3)
      val instant = Instant.parse("2022-01-11T12:00:00Z")
      val stubClock: Clock = Clock.fixed(instant, ZoneId.systemDefault)
      val service = new PeriodService(stubClock)
      service.getNextPeriod(current) mustBe expected
    }

    "when current period is Q3" in {
      val year = 2021
      val current = StandardPeriod(year, Q3)
      val expected = StandardPeriod(year, Q4)
      val instant = Instant.parse("2022-01-11T12:00:00Z")
      val stubClock: Clock = Clock.fixed(instant, ZoneId.systemDefault)
      val service = new PeriodService(stubClock)
      service.getNextPeriod(current) mustBe expected
    }

    "when current period is Q4" in {
      val year = 2021
      val current = StandardPeriod(year, Q4)
      val expected = StandardPeriod(year + 1, Q1)
      val instant = Instant.parse("2022-01-11T12:00:00Z")
      val stubClock: Clock = Clock.fixed(instant, ZoneId.systemDefault)
      val service = new PeriodService(stubClock)
      service.getNextPeriod(current) mustBe expected
    }
  }

  ".getPreviousPeriod" - {

    "when current period is Q1" in {
      val year = 2021
      val current = StandardPeriod(year, Q1)
      val expected = StandardPeriod(year - 1, Q4)
      val instant = Instant.parse("2022-01-11T12:00:00Z")
      val stubClock: Clock = Clock.fixed(instant, ZoneId.systemDefault)
      val service = new PeriodService(stubClock)
      service.getPreviousPeriod(current) mustBe expected
    }

    "when current period is Q2" in {
      val year = 2021
      val current = StandardPeriod(year, Q2)
      val expected = StandardPeriod(year, Q1)
      val instant = Instant.parse("2022-01-11T12:00:00Z")
      val stubClock: Clock = Clock.fixed(instant, ZoneId.systemDefault)
      val service = new PeriodService(stubClock)
      service.getPreviousPeriod(current) mustBe expected
    }

    "when current period is Q3" in {
      val year = 2021
      val current = StandardPeriod(year, Q3)
      val expected = StandardPeriod(year, Q2)
      val instant = Instant.parse("2022-01-11T12:00:00Z")
      val stubClock: Clock = Clock.fixed(instant, ZoneId.systemDefault)
      val service = new PeriodService(stubClock)
      service.getPreviousPeriod(current) mustBe expected
    }

    "when current period is Q4" in {
      val year = 2021
      val current = StandardPeriod(year, Q4)
      val expected = StandardPeriod(year, Q3)
      val instant = Instant.parse("2022-01-11T12:00:00Z")
      val stubClock: Clock = Clock.fixed(instant, ZoneId.systemDefault)
      val service = new PeriodService(stubClock)
      service.getPreviousPeriod(current) mustBe expected
    }
  }
}
