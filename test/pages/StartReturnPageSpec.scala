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

package pages

import controllers.routes
import models.{NormalMode, Period}
import org.scalacheck.Arbitrary.arbitrary
import pages.behaviours.PageBehaviours

class StartReturnPageSpec extends PageBehaviours {

  "StartReturnPage" - {

    ".navigate" - {

      "must go to Sold Goods From Unregistered Country when the answer is yes" in {
        forAll(arbitrary[Period]) {
          period =>
            StartReturnPage.navigate(period, startReturn = true)
              .mustEqual(routes.SoldGoodsFromNiController.onPageLoad(NormalMode, period))
        }
      }

      "must go to Index when the answer is no" in {
        forAll(arbitrary[Period]) {
          period =>
            StartReturnPage.navigate(period, startReturn = false)
              .mustEqual(routes.NoOtherPeriodsAvailableController.onPageLoad())
        }
      }
    }
  }
}
