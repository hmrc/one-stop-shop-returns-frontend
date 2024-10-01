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

package config

import play.api.{Configuration, Logging}
import play.api.http.HeaderNames.AUTHORIZATION
import play.api.libs.json.Json
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.net.URL
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

abstract class InternalAuthTokenInitialiser {
  val initialised: Future[Unit]
}

object InternalAuthTokenInitialiser {

  val resourceType: String = "eu-vat-rates"
  val resourceLocation: String = "*"
  val actions: Seq[String] = List("READ", "WRITE", "DELETE")
}

@Singleton
class NoOpInternalAuthTokenInitialiser @Inject()() extends InternalAuthTokenInitialiser {
  override val initialised: Future[Unit] = Future.successful(())
}

@Singleton
class InternalAuthTokenInitialiserImpl @Inject()(
                                                  configuration: Configuration,
                                                  httpClient: HttpClientV2,
                                                  servicesConfig: ServicesConfig
                                 )(implicit ec: ExecutionContext) extends InternalAuthTokenInitialiser with Logging {

  import InternalAuthTokenInitialiser._
  private val internalAuthService: String = servicesConfig.baseUrl("internal-auth")
  private val authToken: String = configuration.get[String]("internal-auth.token")
  private val appName: String = configuration.get[String]("appName")
  private val url = new URL(s"$internalAuthService/test-only/token")
  private val requestBody = Json.obj(
    "token" -> authToken,
    "principal" -> appName,
    "permissions" -> Seq(
      Json.obj(
        "resourceType" -> resourceType,
        "resourceLocation" -> resourceLocation,
        "actions" -> actions
      )
    )
  )

  override val initialised: Future[Unit] = ensureAuthToken()

  private def ensureAuthToken(): Future[Unit] = {
    authTokenIsValid().flatMap { isValid =>
      if (isValid) {
        logger.info("Auth Token is already valid")
        Future.successful(())
      } else {
        createClientAuthToken()
      }
    }
  }

  private def createClientAuthToken(): Future[Unit] = {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    httpClient.post(url)
      .withBody(requestBody)
      .execute[HttpResponse]
      .flatMap { response =>
        if (response.status == 201) {
          logger.info("Auth token initialised successfully")
          Future.successful(())
        } else {
          Future.failed(new RuntimeException("Unable to initialise internal-auth token"))
        }
      }
  }

  private def authTokenIsValid(): Future[Boolean] = {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    httpClient.get(url)
      .setHeader((AUTHORIZATION, authToken))
      .execute[HttpResponse]
      .map(_.status == 200)
  }

}
