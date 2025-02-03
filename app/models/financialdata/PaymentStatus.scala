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

package models.financialdata

import models.{Enumerable, WithName}

sealed trait PaymentStatus

object PaymentStatus extends Enumerable.Implicits {

  case object Unpaid extends WithName("UNPAID") with PaymentStatus

  case object Partial extends WithName("PARTIAL") with PaymentStatus

  case object Paid extends WithName("PAID") with PaymentStatus

  case object Unknown extends WithName("UNKNOWN") with PaymentStatus

  case object NilReturn extends WithName("NIL_RETURN") with PaymentStatus

  case object Excluded extends WithName("EXCLUDED") with PaymentStatus

  val values: Seq[PaymentStatus] = Seq(Unpaid, Partial, Paid, Unknown, NilReturn, Excluded)

  implicit val enumerable: Enumerable[PaymentStatus] =
    Enumerable(values.map(v => v.toString -> v): _*)
}
