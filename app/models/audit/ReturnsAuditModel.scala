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

package models.audit

import models.requests.corrections.CorrectionRequest
import models.requests.{DataRequest, VatReturnRequest}
import models.{PaymentReference, ReturnReference}
import play.api.libs.json.{JsObject, JsValue, Json}

case class ReturnsAuditModel(
                              credId: String,
                              userAgent: String,
                              vatReturnRequest: VatReturnRequest,
                              correctionRequest: Option[CorrectionRequest],
                              reference: Option[ReturnReference],
                              paymentReference: Option[PaymentReference],
                              result: SubmissionResult
                            ) extends JsonAuditModel {

  override val auditType: String = "ReturnSubmitted"
  override val transactionName: String = "return-submitted"

  private val startDate: JsObject =
    if (vatReturnRequest.startDate.isDefined) {
      Json.obj("startDate" -> vatReturnRequest.startDate)
    } else {
      Json.obj()
    }

  private val endDate: JsObject =
    if (vatReturnRequest.endDate.isDefined) {
      Json.obj("endDate" -> vatReturnRequest.endDate)
    } else {
      Json.obj()
    }

  private val referenceObj: JsObject =
    if (reference.isDefined) {
      Json.obj("returnReference" -> reference)
    } else {
      Json.obj()
    }

  private val paymentReferenceObj: JsObject =
    if (paymentReference.isDefined) {
      Json.obj("paymentReference" -> paymentReference)
    } else {
      Json.obj()
    }

  private val returnDetail: JsValue = Json.obj(
    "vatRegistrationNumber" -> vatReturnRequest.vrn,
    "period" -> Json.toJson(vatReturnRequest.period),
    "salesFromNi" -> Json.toJson(vatReturnRequest.salesFromNi),
    "salesFromEu" -> Json.toJson(vatReturnRequest.salesFromEu),
  ) ++ referenceObj ++
    paymentReferenceObj ++
    startDate ++
    endDate

  private val correctionDetail: JsObject = {
    correctionRequest match {
      case Some(corrRequest) =>
        Json.obj(
          "correctionDetails" -> Json.obj(
            "vatRegistrationNumber" -> corrRequest.vrn,
            "period" -> Json.toJson(corrRequest.period),
            "corrections" -> Json.toJson(corrRequest.corrections)
          )
        )
      case _ => Json.obj()
    }
  }

  override val detail: JsValue = Json.obj(
    "credId" -> credId,
    "browserUserAgent" -> userAgent,
    "submissionResult" -> Json.toJson(result),
    "returnDetails" -> returnDetail
  ) ++ correctionDetail
}

object ReturnsAuditModel {

  def build(
             vatReturnRequest: VatReturnRequest,
             correctionRequest: Option[CorrectionRequest],
             result: SubmissionResult,
             reference: Option[ReturnReference],
             paymentReference: Option[PaymentReference],
             request: DataRequest[_]
           ): ReturnsAuditModel = {
    ReturnsAuditModel(
      credId = request.credentials.providerId,
      userAgent = request.headers.get("user-agent").getOrElse(""),
      vatReturnRequest = vatReturnRequest,
      correctionRequest = correctionRequest,
      reference = reference,
      paymentReference = paymentReference,
      result = result
    )
  }
}
