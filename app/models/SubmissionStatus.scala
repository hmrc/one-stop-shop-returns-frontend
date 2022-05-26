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

package models

sealed trait SubmissionStatus

object SubmissionStatus extends Enumerable.Implicits {

  case object Due extends WithName("DUE") with SubmissionStatus
  case object Overdue extends WithName("OVERDUE") with SubmissionStatus
  case object Complete extends WithName("COMPLETE") with SubmissionStatus
  case object Next extends WithName("NEXT") with SubmissionStatus

  val values: Seq[SubmissionStatus] = Seq(Due, Overdue, Complete, Next)

  implicit val enumerable: Enumerable[SubmissionStatus] =
    Enumerable(values.map(v => v.toString -> v): _*)

}
