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

import play.api.libs.json._

sealed trait IdentityVerificationEvidenceSource {
  def asString: String
}

case object PayslipService extends IdentityVerificationEvidenceSource {
  override def asString: String = "PayslipService"
}

case object P60Service extends IdentityVerificationEvidenceSource {
  override def asString: String = "P60Service"
}

case object NtcService extends IdentityVerificationEvidenceSource {
  override def asString: String = "NtcService"
}

case object Passport extends IdentityVerificationEvidenceSource {
  override def asString: String = "passport"
}

case object CallValidate extends IdentityVerificationEvidenceSource {
  override def asString: String = "call-validate"
}

case class UnrecognisedSource(source: String) extends IdentityVerificationEvidenceSource {
  override def asString: String = source
}


object IdentityVerificationEvidenceSource {
  implicit val reads: Reads[IdentityVerificationEvidenceSource] = Reads {
    case JsString("PayslipService") => JsSuccess(PayslipService)
    case JsString("P60Service")     => JsSuccess(P60Service)
    case JsString("NtcService")     => JsSuccess(NtcService)
    case JsString("passport")       => JsSuccess(Passport)
    case JsString("call-validate")  => JsSuccess(CallValidate)
    case value: JsValue             => JsSuccess(UnrecognisedSource(value.toString))
  }

  implicit val writes: Writes[IdentityVerificationEvidenceSource] = Writes { evidence =>
    JsString(evidence.asString)
  }
}
