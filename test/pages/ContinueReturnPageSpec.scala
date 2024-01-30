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

package pages

import models.ContinueReturn
import pages.behaviours.PageBehaviours
import play.api.mvc.Call
import uk.gov.hmrc.http.HttpVerbs.GET

class ContinueReturnSpec extends PageBehaviours {

  "ContinueReturnPage" - {

    beRetrievable[ContinueReturn](ContinueReturnPage)

    beSettable[ContinueReturn](ContinueReturnPage)

    beRemovable[ContinueReturn](ContinueReturnPage)

    "must navigate" - {

      "to the saved url when the answer is Continue" in {

        val answers = emptyUserAnswers.set(SavedProgressPage, "test").success.value

        ContinueReturnPage.navigate(answers, ContinueReturn.Continue)
          .mustEqual(Call(GET, "test"))
      }

      "to Delete Return when the answer is Delete" in {
        ContinueReturnPage.navigate(emptyUserAnswers, ContinueReturn.Delete)
          .mustEqual(controllers.routes.DeleteReturnController.onPageLoad(emptyUserAnswers.period))
      }
    }

  }
}
