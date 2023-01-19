/*
 * Copyright 2023 HM Revenue & Customs
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

import models.{CheckFinalInnerLoopMode, CheckInnerLoopMode, CheckLoopMode, CheckMode, CheckSecondInnerLoopMode, CheckSecondLoopMode, CheckThirdInnerLoopMode, CheckThirdLoopMode, Mode, NormalMode, UserAnswers}
import play.api.mvc.Call

import scala.language.implicitConversions

trait Page {

  def navigate(mode: Mode, answers: UserAnswers): Call = mode match {
    case NormalMode        => navigateInNormalMode(answers)
    case CheckMode         => navigateInCheckMode(answers)
    case CheckLoopMode     => navigateInCheckLoopMode(answers)
    case CheckSecondLoopMode    => navigateInCheckSecondLoopMode(answers)
    case CheckThirdLoopMode    => navigateInCheckThirdLoopMode(answers)
    case CheckInnerLoopMode    => navigateInCheckInnerLoopMode(answers)
    case CheckSecondInnerLoopMode    => navigateInCheckSecondInnerLoopMode(answers)
    case CheckThirdInnerLoopMode    => navigateInCheckThirdInnerLoopMode(answers)
    case CheckFinalInnerLoopMode    => navigateInCheckFinalInnerLoopMode(answers)
  }

  protected def navigateInNormalMode(answers: UserAnswers): Call =
    throw new NotImplementedError("navigateInNormalMode is not implemented on this page")

  protected def navigateInCheckMode(answers: UserAnswers): Call =
    throw new NotImplementedError("navigateInCheckMode is not implemented on this page")

  protected def navigateInCheckLoopMode(answers: UserAnswers): Call =
    throw new NotImplementedError("navigateInCheckLoopMode is not implemented on this page")

  protected def navigateInCheckSecondLoopMode(answers: UserAnswers): Call =
    throw new NotImplementedError("navigateInCheckSecondLoopMode is not implemented on this page")

  protected def navigateInCheckThirdLoopMode(answers: UserAnswers): Call =
    throw new NotImplementedError("navigateInCheckThirdLoopMode is not implemented on this page")

  protected def navigateInCheckInnerLoopMode(answers: UserAnswers): Call =
    throw new NotImplementedError("navigateInCheckInnerLoopMode is not implemented on this page")

  protected def navigateInCheckSecondInnerLoopMode(answers: UserAnswers): Call =
    throw new NotImplementedError("navigateInCheckSecondInnerLoopMode is not implemented on this page")

  protected def navigateInCheckThirdInnerLoopMode(answers: UserAnswers): Call =
    throw new NotImplementedError("navigateInCheckThirdInnerLoopMode is not implemented on this page")

  protected def navigateInCheckFinalInnerLoopMode(answers: UserAnswers): Call =
    throw new NotImplementedError("navigateInCheckFinalInnerLoopMode is not implemented on this page")
}

object Page {

  implicit def toString(page: Page): String =
    page.toString
}
