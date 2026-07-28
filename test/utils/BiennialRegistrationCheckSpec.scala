package utils

import base.SpecBase

import java.time.LocalDateTime

class BiennialRegistrationCheckSpec extends SpecBase {

  "BiennialRegistrationCheck" - {

    "must return true when a given change date is more than two years old" in {

      val changeDate: LocalDateTime = LocalDateTime.now(stubClockAtArbitraryDate).minusYears(2).minusDays(1)
      val result = BiennialRegistrationCheck.changeDateMoreThanTwoYears(changeDate, stubClockAtArbitraryDate)
      result mustBe true
    }

    "must return false when a given change date is exactly two years old" in {

      val changeDate: LocalDateTime = LocalDateTime.now(stubClockAtArbitraryDate).minusYears(2)
      val result = BiennialRegistrationCheck.changeDateMoreThanTwoYears(changeDate, stubClockAtArbitraryDate)
      result mustBe false
    }

    "must return false when a given change date is less than two years old" in {

      val changeDate: LocalDateTime = LocalDateTime.now(stubClockAtArbitraryDate).minusYears(2).plusDays(1)
      val result = BiennialRegistrationCheck.changeDateMoreThanTwoYears(changeDate, stubClockAtArbitraryDate)
      result mustBe false
    }
  }
}
