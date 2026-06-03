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

package connectors

import config.FrontendAppConfig
import models.upscan.{PreparedUpload, UpscanFileReference, UpscanInitiateRequest, UpscanInitiateResponse}
import play.api.libs.json.*
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import uk.gov.hmrc.http.client.HttpClientV2

import java.net.URL
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.HttpReads.Implicits.*

class UpscanInitiateConnector @Inject()(
                                         appConfig: FrontendAppConfig,
                                         httpClientV2: HttpClientV2
                                       )(implicit ec: ExecutionContext) {

  private val headers: Seq[(String, String)] = Seq("Content-Type" -> "application/json")
  
  def initiateV2(redirectOnSuccess: Option[String], redirectOnError: Option[String])
                (implicit hc: HeaderCarrier): Future[UpscanInitiateResponse] = {
    
    val request = UpscanInitiateRequest(
      callbackUrl = appConfig.upscanCallbackUrl,
      successRedirect = redirectOnSuccess,
      errorRedirect = redirectOnError,
      minimumFileSize = Some(1),
      maximumFileSize = Some(appConfig.maxFileSize),
      expectedContentType = Some("text/csv")
    )
    
    initiate(url"${appConfig.initiateV2Url}", request)
  }
  
  private def initiate(url: URL, request: UpscanInitiateRequest)
                      (implicit hc: HeaderCarrier, format: Format[UpscanInitiateRequest]): Future[UpscanInitiateResponse] = {
    
    for {
      response <- httpClientV2
        .post(url)(hc)
        .withBody(Json.toJson(request))
        .setHeader(headers*)
        .execute[PreparedUpload]
      
      fileReference = UpscanFileReference(response.reference.reference)
    } yield UpscanInitiateResponse(
      fileReference = fileReference,
      postTarget = response.uploadRequest.href,
      formFields = response.uploadRequest.fields
    )
  }

}
