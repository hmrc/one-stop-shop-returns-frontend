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
import models.{CheckMode, NormalMode}
import pages.behaviours.PageBehaviours

class NetValueOfSalesFromNiPageSpec extends PageBehaviours {

  "NetValueOfSalesFromNiPage" - {

    beRetrievable[BigDecimal](NetValueOfSalesFromNiPage(index, index))

    beSettable[BigDecimal](NetValueOfSalesFromNiPage(index, index))

    beRemovable[BigDecimal](NetValueOfSalesFromNiPage(index, index))

    "must navigate in Normal mode" - {

      "to Vat on Sales" in {

        NetValueOfSalesFromNiPage(index, index).navigate(NormalMode, emptyUserAnswers)
          .mustEqual(routes.VatOnSalesFromNiController.onPageLoad(NormalMode, emptyUserAnswers.period, index, index))
      }
    }

    "must navigate in Check mode" - {

      "to Vat on Sales" in {

        NetValueOfSalesFromNiPage(index, index).navigate(CheckMode, emptyUserAnswers)
          .mustEqual(routes.VatOnSalesFromNiController.onPageLoad(CheckMode, emptyUserAnswers.period, index, index))
      }
    }
  }
}
