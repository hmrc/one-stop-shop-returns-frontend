package models.external

import base.SpecBase
import models.Quarter.Q1
import models.StandardPeriod

import java.time.Month

class ExternalTargetPageSpec extends SpecBase {

  "ExternalTargetPage" - {

    "return the correct URL for YourAccount" in {
      val url = YourAccount.url

      url mustBe controllers.routes.YourAccountController.onPageLoad().url
    }

    "return the correct URL for ReturnsHistory" in {

      val url = ReturnsHistory.url

      url mustBe controllers.routes.SubmittedReturnsHistoryController.onPageLoad().url
    }

    "generate the correct URL for StartReturn with a given period" in {

      val period = StandardPeriod(2023, Q1)
      val url = StartReturn.url(period)

      url mustBe controllers.routes.StartReturnController.onPageLoad(period).url
    }

    "generate the correct URL for ContinueReturn with a given period" in {

      val period = StandardPeriod(2023, Q1)
      val url = ContinueReturn.url(period)

      url mustBe controllers.routes.ContinueReturnController.onPageLoad(period).url
    }

    "return the correct URL for Payment" in {

      val url = Payment.url

      url mustBe controllers.routes.WhichVatPeriodToPayController.onPageLoad().url
    }
  }
}
