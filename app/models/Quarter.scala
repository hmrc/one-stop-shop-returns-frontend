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

package models

import java.time.Month
import java.time.Month.{APRIL, JANUARY, JULY, OCTOBER}
import scala.util.{Failure, Success, Try}

sealed trait Quarter {
  def startMonth: Month
}

object Quarter extends Enumerable.Implicits {

  case object Q1 extends WithName("Q1") with Quarter { val startMonth = JANUARY }
  case object Q2 extends WithName("Q2") with Quarter { val startMonth = APRIL }
  case object Q3 extends WithName("Q3") with Quarter { val startMonth = JULY }
  case object Q4 extends WithName("Q4") with Quarter { val startMonth = OCTOBER }

  val values: Seq[Quarter] = Seq(Q1, Q2, Q3, Q4)

  implicit val enumerable: Enumerable[Quarter] =
    Enumerable(values.map(v => v.toString -> v): _*)

  def fromString(string: String): Try[Quarter] = string match {
    case Q1.toString => Success(Q1)
    case Q2.toString => Success(Q2)
    case Q3.toString => Success(Q3)
    case Q4.toString => Success(Q4)
    case _           => Failure(new IllegalArgumentException(s"$string is not a valid quarter"))
  }

}