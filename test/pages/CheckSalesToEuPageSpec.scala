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
import models.{CheckMode, CheckSecondLoopMode, CheckThirdLoopMode, NormalMode}
import pages.behaviours.PageBehaviours

class CheckSalesToEuPageSpec extends PageBehaviours {

  "CheckSalesToEu page" - {

    "must navigate in Normal mode" - {

      "to sales to EU List" in {

        CheckSalesToEuPage(index).navigate(NormalMode, emptyUserAnswers)
          .mustEqual(routes.SalesToEuListController.onPageLoad(NormalMode, emptyUserAnswers.period, index))
      }
    }

    "must navigate in Check mode" - {

      "to sales to EU List" in {

        CheckSalesToEuPage(index).navigate(CheckMode, emptyUserAnswers)
          .mustEqual(routes.SalesToEuListController.onPageLoad(CheckMode, emptyUserAnswers.period, index))
      }
    }

    "must navigate in CheckSecondLoop mode" - {

      "to sales to EU List" in {

        CheckSalesToEuPage(index).navigate(CheckSecondLoopMode, emptyUserAnswers)
          .mustEqual(routes.SalesToEuListController.onPageLoad(NormalMode, emptyUserAnswers.period, index))
      }
    }

    "must navigate in CheckThirdLoop mode" - {

      "to sales to EU List" in {

        CheckSalesToEuPage(index).navigate(CheckThirdLoopMode, emptyUserAnswers)
          .mustEqual(routes.SalesToEuListController.onPageLoad(CheckThirdLoopMode, emptyUserAnswers.period, index))
      }
    }
  }
}
