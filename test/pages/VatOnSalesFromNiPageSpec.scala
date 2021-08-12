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

import models.Index
import pages.behaviours.PageBehaviours

class VatOnSalesFromNiPageSpec extends PageBehaviours {

  private val index = Index(0)

  "VatOnSalesFromNiPage" - {

    beRetrievable[Int](VatOnSalesFromNiPage(index, index))

    beSettable[Int](VatOnSalesFromNiPage(index, index))

    beRemovable[Int](VatOnSalesFromNiPage(index, index))
  }
}
