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

package models.audit

import models.requests.IdentifierRequest
import play.api.libs.json.{Json, JsValue}

case class LoginAuditModel(
                              credId: String,
                              userAgent: String,
                              groupId: String,
                              vrn: String
                            ) extends JsonAuditModel {

  override val auditType: String = "OssAuthRequest"
  override val transactionName: String = "oss-auth-request"

  override val detail: JsValue = Json.obj(
    "credId" -> credId,
    "browserUserAgent" -> userAgent,
    "groupId" -> groupId,
    "vrn" -> vrn
  )
}

object LoginAuditModel {

  def build(
             groupId: String,
             request: IdentifierRequest[_]
           ): LoginAuditModel = {
    LoginAuditModel(
      credId = request.credentials.providerId,
      userAgent = request.headers.get("user-agent").getOrElse(""),
      groupId = groupId,
      vrn = request.vrn.vrn
    )
  }
}
