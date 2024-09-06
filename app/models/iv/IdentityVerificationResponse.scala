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

package models.iv

import play.api.libs.json.{Json, OFormat}

sealed trait IdentityVerificationResponse

sealed trait IdentityVerificationResult extends IdentityVerificationResponse

object IdentityVerificationResult {

  case object Success extends IdentityVerificationResult

  case object Incomplete extends IdentityVerificationResult
  case object FailedMatching extends IdentityVerificationResult
  case object FailedIdentityVerification extends IdentityVerificationResult
  case object InsufficientEvidence extends IdentityVerificationResult
  case object LockedOut extends IdentityVerificationResult
  case object UserAborted extends IdentityVerificationResult
  case object Timeout extends IdentityVerificationResult
  case object TechnicalIssue extends IdentityVerificationResult
  case object PrecondFailed extends IdentityVerificationResult

  def fromString(s: String): Option[IdentityVerificationResult] =
    s match {
      case "Success"              => Some(Success)
      case "Incomplete"           => Some(Incomplete)
      case "FailedMatching"       => Some(FailedMatching)
      case "FailedIV"             => Some(FailedIdentityVerification)
      case "InsufficientEvidence" => Some(InsufficientEvidence)
      case "LockedOut"            => Some(LockedOut)
      case "UserAborted"          => Some(UserAborted)
      case "Timeout"              => Some(Timeout)
      case "TechnicalIssue"       => Some(TechnicalIssue)
      case "PreconditionFailed"   => Some(PrecondFailed)
      case _                      => None
    }
}

case class IdentityVerificationUnexpectedResponse(status: Int) extends IdentityVerificationResponse

case class IdentityVerificationErrorResponse(cause: Exception) extends IdentityVerificationResponse

case class IdentityVerificationProgress(result: String)

object IdentityVerificationProgress {
  implicit val format: OFormat[IdentityVerificationProgress] = Json.format[IdentityVerificationProgress]
}
