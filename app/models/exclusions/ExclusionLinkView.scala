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

package models.exclusions

import models.{Enumerable, WithName}

case class ExclusionLinkView(displayText: String, id: String, href: String)

sealed trait ExclusionViewType

object ExclusionViewType extends Enumerable.Implicits {
  case object Quarantined extends WithName("Quarantined") with ExclusionViewType
  case object RejoinEligible extends WithName("Rejoin") with ExclusionViewType
  case object ReversalEligible extends WithName("Reversal") with ExclusionViewType
  case object ExcludedFinalReturnPending extends WithName("ExcludedFinalReturnPending") with ExclusionViewType
  case object DeregisteredTrader extends WithName("DeregisteredTrader") with ExclusionViewType
  case object Default extends WithName("Default") with ExclusionViewType
}
