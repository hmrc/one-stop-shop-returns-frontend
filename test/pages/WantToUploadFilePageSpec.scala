/*
 * Copyright 2026 HM Revenue & Customs
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
import models.{CheckMode, NormalMode}
import pages.behaviours.PageBehaviours
import pages.fileUpload.WantToUploadFilePage

class WantToUploadFilePageSpec extends PageBehaviours {

  "WantToUploadFilePage" - {

    beRetrievable[Boolean](WantToUploadFilePage)

    beSettable[Boolean](WantToUploadFilePage)

    beRemovable[Boolean](WantToUploadFilePage)

    "must navigate in Normal mode" - {

      "when the answer is yes" - {

        "to file upload page" in {

          val answers = emptyUserAnswers.set(WantToUploadFilePage, true).success.value

          WantToUploadFilePage.navigate(NormalMode, answers)
            .mustEqual(controllers.fileUpload.routes.FileUploadController.onPageLoad(NormalMode, answers.period))

        }
      }

      "when the answer is no" - {

        "to Sold Goods from Northern Ireland page" in {

          val answers = emptyUserAnswers.set(WantToUploadFilePage, false).success.value

          WantToUploadFilePage.navigate(NormalMode, answers)
            .mustEqual(routes.SoldGoodsFromNiController.onPageLoad(NormalMode, answers.period))
        }
      }
    }

    "must navigate in Check mode" - {

      "when the answer is yes" - {

        "to file upload page" in {

          val answers = emptyUserAnswers.set(WantToUploadFilePage, true).success.value

          WantToUploadFilePage.navigate(CheckMode, answers)
            .mustEqual(controllers.fileUpload.routes.FileUploadController.onPageLoad(NormalMode, answers.period))
        }
      }

      "when the answer is no" - {

        "to Sold Goods from Northern Ireland page" in {

          val answers = emptyUserAnswers.set(WantToUploadFilePage, false).success.value

          WantToUploadFilePage.navigate(CheckMode, answers)
            .mustEqual(routes.SoldGoodsFromNiController.onPageLoad(CheckMode, answers.period))
        }
      }
    }
  }
}
