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

package connectors

import config.FrontendAppConfig
import models.iv.{IdentityVerificationEvidenceSource, IdentityVerificationResponse}
import connectors.IdentityVerificationHttpParser.IdentityVerificationResponseReads
import connectors.IdentityVerificationEvidenceSourceHttpParser.IdentityVerificationEvidenceSourcesReads
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IdentityVerificationConnector @Inject()(
                                               httpClient: HttpClientV2,
                                               frontendAppConfig: FrontendAppConfig
                                             )(implicit executionContext: ExecutionContext) {

  def getJourneyStatus(journeyId: String)(implicit hc: HeaderCarrier): Future[Option[IdentityVerificationResponse]] =
    httpClient.get(url"${frontendAppConfig.ivJourneyResultUrl(journeyId)}").execute[Option[IdentityVerificationResponse]]

  def getDisabledEvidenceSources()(implicit hc: HeaderCarrier): Future[List[IdentityVerificationEvidenceSource]] =
    httpClient.get(url"${frontendAppConfig.ivEvidenceStatusUrl}").execute[List[IdentityVerificationEvidenceSource]]
}