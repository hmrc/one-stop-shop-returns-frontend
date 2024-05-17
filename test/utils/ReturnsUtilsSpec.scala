package utils

import base.SpecBase
import models.Quarter.Q1
import models.{StandardPeriod, SubmissionStatus}
import viewmodels.yourAccount.Return

import java.time.LocalDate

class ReturnsUtilsSpec extends SpecBase with CurrencyFormatter {

  val year: Int = 2024

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

        ReturnsUtils.hasDueReturnThreeYearsOld(returns) mustBe true
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

        ReturnsUtils.hasDueReturnThreeYearsOld(returns) mustBe true
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

        ReturnsUtils.hasDueReturnThreeYearsOld(returns) mustBe false
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

        ReturnsUtils.hasDueReturnThreeYearsOld(returns) mustBe false
      }
    }
  }

  "hasDueReturnsLessThanThreeYearsOld" - {
    "should return true" - {
      "when there are 2 or more returns Due less than three years old" in {
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

        val returns = Seq(taxReturn, taxReturn)

        ReturnsUtils.hasDueReturnsLessThanThreeYearsOld(returns) mustBe true
      }
    }
    "should return false" - {
      "when there are 2 or more returns Due exactly three years old" in {
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

        val returns = Seq(taxReturn, taxReturn)

        ReturnsUtils.hasDueReturnsLessThanThreeYearsOld(returns) mustBe false
      }

      "when there are 2 or more returns Completed less than three years old" in {
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

        val returns = Seq(taxReturn, taxReturn)

        ReturnsUtils.hasDueReturnsLessThanThreeYearsOld(returns) mustBe false
      }

      "when there is one return Due less than three years old" in {
        val nextPeriod = StandardPeriod(year, Q1)
        val today = LocalDate.now().minusYears(2)

        val returns = Seq(
          Return(
            nextPeriod,
            nextPeriod.firstDay,
            nextPeriod.lastDay,
            dueDate = today,
            SubmissionStatus.Due,
            inProgress = false,
            isOldest = false
          )
        )

        ReturnsUtils.hasDueReturnsLessThanThreeYearsOld(returns) mustBe false
      }
    }
  }
}
