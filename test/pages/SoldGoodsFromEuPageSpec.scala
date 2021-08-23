/*
 * Copyright 2021 HM Revenue & Customs
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
import models.{Index, NormalMode}
import pages.behaviours.PageBehaviours

class SoldGoodsFromEuPageSpec extends PageBehaviours {

  "SoldGoodsFromEuPage" - {

    beRetrievable[Boolean](SoldGoodsFromEuPage)

    beSettable[Boolean](SoldGoodsFromEuPage)

    beRemovable[Boolean](SoldGoodsFromEuPage)

    "must navigate in Normal mode" - {

      "to Country of Sale from EU when the answer is yes" in {

        val answers = emptyUserAnswers.set(SoldGoodsFromEuPage, true).success.value

        SoldGoodsFromEuPage.navigate(NormalMode, answers)
          .mustEqual(routes.CountryOfSaleFromEuController.onPageLoad(NormalMode, answers.period, Index(0)))
      }

      "to Check your answers when the answer is no" in {

        val answers = emptyUserAnswers.set(SoldGoodsFromEuPage, false).success.value

        SoldGoodsFromEuPage.navigate(NormalMode, answers)
          .mustEqual(routes.CheckYourAnswersController.onPageLoad(answers.period))
      }
    }
  }
}
