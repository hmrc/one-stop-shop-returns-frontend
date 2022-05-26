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

package viewmodels.yourAccount

import models.SubmissionStatus.{Due, Next, Overdue}
import play.api.libs.json.{Json, OFormat}


case class OpenReturns(
                           currentReturn: Option[Return],
                           dueReturn: Option[Return],
                           overdueReturns: Seq[Return],
                           nextReturn: Option[Return]
                           )

object OpenReturns {
  implicit val format: OFormat[OpenReturns] = Json.format[OpenReturns]

  def fromReturns(returns: Seq[Return]): OpenReturns = {
    OpenReturns(
      returns.find(_.inProgress),
      returns.find(_.submissionStatus == Due),
      returns.filter(_.submissionStatus == Overdue),
      returns.find(_.submissionStatus == Next)
    )
  }
}
