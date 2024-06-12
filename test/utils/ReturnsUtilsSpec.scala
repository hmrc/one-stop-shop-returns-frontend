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

package utils

import base.SpecBase
import models.Quarter.Q1
import models.{StandardPeriod, SubmissionStatus}
import viewmodels.yourAccount.Return

import java.time.{Clock, Instant, LocalDate, ZoneId}

class ReturnsUtilsSpec extends SpecBase {

  val year: Int = 2024

  val instant: Instant = Instant.now()

  implicit val stubClock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

  "hasDueReturnThreeYearsOld" - {
    "should return true" - {
      "when there is a return Due exactly three years old" in {

        val nextPeriod: StandardPeriod = StandardPeriod(year, Q1)
        val today: LocalDate = LocalDate.now().minusYears(3)

        val returns = Seq(Return(
          nextPeriod,
          nextPeriod.firstDay,
          nextPeriod.lastDay,
          dueDate = today,
          SubmissionStatus.Due,
          inProgress = false,
          isOldest = false
        ))

        ReturnsUtils.hasReturnThreeYearsOld(returns) mustBe true
      }

      "when there a return Due more than three years old" in {
        val nextPeriod = StandardPeriod(year, Q1)
        val today = LocalDate.now().minusYears(3).minusDays(1)

        val returns = Seq(Return(
          nextPeriod,
          nextPeriod.firstDay,
          nextPeriod.lastDay,
          dueDate = today,
          SubmissionStatus.Due,
          inProgress = false,
          isOldest = false
        ))

        ReturnsUtils.hasReturnThreeYearsOld(returns) mustBe true
      }
    }

    "should return false" - {
      "when there is no three year old returns Due" in {
        val nextPeriod = StandardPeriod(year, Q1)
        val today = LocalDate.now()

        val returns = Seq(Return(
          nextPeriod,
          nextPeriod.firstDay,
          nextPeriod.lastDay,
          dueDate = today,
          SubmissionStatus.Due,
          inProgress = false,
          isOldest = false
        ))

        ReturnsUtils.hasReturnThreeYearsOld(returns) mustBe false
      }

      "when there is a Completed return more than three years old" in {
        val nextPeriod = StandardPeriod(year, Q1)
        val today = LocalDate.now().minusYears(3).minusDays(1)

        val returns = Seq(Return(
          nextPeriod,
          nextPeriod.firstDay,
          nextPeriod.lastDay,
          dueDate = today,
          SubmissionStatus.Complete,
          inProgress = false,
          isOldest = false
        ))

        ReturnsUtils.hasReturnThreeYearsOld(returns) mustBe false
      }
    }
  }

  "hasDueReturnsLessThanThreeYearsOld" - {
    "should return true" - {
      "when there are 1 or more returns Due less than three years old" in {
        val nextPeriod = StandardPeriod(year, Q1)
        val today = LocalDate.now().minusYears(2)

        val taxReturn = Return(
          nextPeriod,
          nextPeriod.firstDay,
          nextPeriod.lastDay,
          dueDate = today,
          SubmissionStatus.Due,
          inProgress = false,
          isOldest = false
        )

        val returns = Seq(taxReturn)

        ReturnsUtils.hasDueReturnsLessThanThreeYearsOld(returns) mustBe true
      }
    }
    "should return false" - {
      "when there are 1 or more returns Due exactly three years old" in {
        val nextPeriod = StandardPeriod(year, Q1)
        val today = LocalDate.now().minusYears(3)
        val taxReturn = Return(
          nextPeriod,
          nextPeriod.firstDay,
          nextPeriod.lastDay,
          dueDate = today,
          SubmissionStatus.Due,
          inProgress = false,
          isOldest = false
        )

        val returns = Seq(taxReturn)

        ReturnsUtils.hasDueReturnsLessThanThreeYearsOld(returns) mustBe false
      }

      "when there are 1 or more returns Completed less than three years old" in {
        val nextPeriod = StandardPeriod(year, Q1)
        val today = LocalDate.now().minusYears(2)
        val taxReturn = Return(
          nextPeriod,
          nextPeriod.firstDay,
          nextPeriod.lastDay,
          dueDate = today,
          SubmissionStatus.Complete,
          inProgress = false,
          isOldest = false
        )

        val returns = Seq(taxReturn)

        ReturnsUtils.hasDueReturnsLessThanThreeYearsOld(returns) mustBe false
      }

      "when there are no returns" in {

        val returns = Seq.empty

        ReturnsUtils.hasDueReturnsLessThanThreeYearsOld(returns) mustBe false
      }
    }
  }
}
