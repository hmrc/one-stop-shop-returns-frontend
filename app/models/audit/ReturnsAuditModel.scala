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

package models.audit

import models.requests.{DataRequest, VatReturnRequest}
import play.api.libs.json.{JsObject, JsValue, Json}

case class ReturnsAuditModel(
                              credId: String,
                              userAgent: String,
                              vatReturnRequest: VatReturnRequest,
                              result: SubmissionResult
                            ) extends JsonAuditModel {

  override val auditType: String = "ReturnSubmitted"
  override val transactionName: String = "return-submitted"

  private val startDate: JsObject =
    if (vatReturnRequest.startDate.nonEmpty) Json.obj("startDate" -> vatReturnRequest.startDate) else Json.obj()

  private val endDate: JsObject =
    if (vatReturnRequest.endDate.nonEmpty) Json.obj("endDate" -> vatReturnRequest.endDate) else Json.obj()

  private val returnDetail: JsValue = Json.obj(
    "vatRegistrationNumber" -> vatReturnRequest.vrn,
    "period" -> Json.toJson(vatReturnRequest.period),
    "salesFromNi" -> Json.toJson(vatReturnRequest.salesFromNi),
    "salesFromEu" -> Json.toJson(vatReturnRequest.salesFromEu),
  ) ++ startDate ++
    endDate

  override val detail: JsValue = Json.obj(
    "credId" -> credId,
    "browserUserAgent" -> userAgent,
    "submissionResult" -> Json.toJson(result),
    "returnDetails" -> returnDetail
  )
}

object ReturnsAuditModel {

  def build(vatReturnRequest: VatReturnRequest, result: SubmissionResult, request: DataRequest[_]): ReturnsAuditModel =
    ReturnsAuditModel(
      credId           = request.credentials.providerId,
      userAgent        = request.headers.get("user-agent").getOrElse(""),
      vatReturnRequest = vatReturnRequest,
      result           = result
    )
}
