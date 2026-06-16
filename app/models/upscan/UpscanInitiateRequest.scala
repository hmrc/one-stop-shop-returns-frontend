/*
 * Copyright 2026 HM Revenue & Customs
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

package models.upscan

import play.api.libs.json.{Format, Json}

final case class UpscanInitiateRequest(
                                        callbackUrl: String,
                                        successRedirect: Option[String] = None,
                                        errorRedirect: Option[String] = None,
                                        minimumFileSize: Option[Int] = None,
                                        maximumFileSize: Option[Int] = None,
                                        expectedContentType: Option[String] = None,
                                      )

object UpscanInitiateRequest {
  implicit val format: Format[UpscanInitiateRequest] = Json.format[UpscanInitiateRequest]
}

final case class UpscanUploadRequest(href: String, fields: Map[String, String])

object UpscanUploadRequest {
  implicit val format: Format[UpscanUploadRequest] = Json.format[UpscanUploadRequest]
}

final case class UpscanFileReference(reference: String)

object UpscanFileReference {
  implicit val format: Format[UpscanFileReference] = Json.format[UpscanFileReference]
}

final case class UpscanInitiateResponse(fileReference: UpscanFileReference, postTarget: String, formFields: Map[String, String])

object UpscanInitiateResponse {
  implicit val format: Format[UpscanInitiateResponse] = Json.format[UpscanInitiateResponse]
}

final case class FileUploadOutcome(fileName: Option[String], status: String, failureReason: Option[String] = None)

object FileUploadOutcome {
  implicit val format: Format[FileUploadOutcome] = Json.format[FileUploadOutcome]
}
