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

package controllers.actions

import base.SpecBase
import config.FrontendAppConfig
import models.Country
import models.domain.{EuTaxIdentifier, EuTaxIdentifierType}
import models.registration.{InternationalAddress, RegistrationWithFixedEstablishment, TradeDetails}
import models.requests.RegistrationRequest
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import play.api.test.FakeRequest
import play.api.test.Helpers.running

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CheckVatGroupFixedEstablishmentFilterSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  class Harness(frontendAppConfig: FrontendAppConfig) extends CheckVatGroupFixedEstablishmentFilterImpl(frontendAppConfig) {
    def callFilter(request: RegistrationRequest[_]): Future[Option[Result]] = filter(request)
  }

  private val redirectUrl = "http://localhost:10200/pay-vat-on-goods-sold-to-eu/northern-ireland-register/delete-all-fixed-establishment"

  ".filter" - {

    "must return None" - {
      "when vat group false" in {

        val app = applicationBuilder(None)
          .build()

        val frontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

        running(app) {
          val request = RegistrationRequest(FakeRequest(), testCredentials, vrn, registration)
          val controller = new Harness(frontendAppConfig)

          val result = controller.callFilter(request).futureValue

          result must not be defined
        }
      }

      "when vat group is true and there are no fixed establishments" in {

        val vatDetailsWithVatGroup = vatDetails.copy(partOfVatGroup = true)
        val registrationWithVatGroup = registration.copy(vatDetails = vatDetailsWithVatGroup)

        val app = applicationBuilder(None)
          .build()

        val frontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

        running(app) {
          val request = RegistrationRequest(FakeRequest(), testCredentials, vrn, registrationWithVatGroup)
          val controller = new Harness(frontendAppConfig)

          val result = controller.callFilter(request).futureValue

          result must not be defined
        }
      }
    }

    "must redirect to configured URL page" - {

      "when vat group true and there are fixed establishments" in {

        val vatDetailsWithVatGroup = vatDetails.copy(partOfVatGroup = true)
        val fixedEstablishment = RegistrationWithFixedEstablishment(
          Country.euCountries.head,
          EuTaxIdentifier(
            EuTaxIdentifierType.Other, "EU123456789"),
          TradeDetails("test", InternationalAddress("line 1", None, "town", None, None, Country.euCountries.head))
        )
        val registrationWithVatGroup = registration.copy(vatDetails = vatDetailsWithVatGroup, euRegistrations = Seq(fixedEstablishment))

        val app = applicationBuilder(None)
          .build()

        val frontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

        running(app) {
          val request = RegistrationRequest(FakeRequest(), testCredentials, vrn, registrationWithVatGroup)
          val controller = new Harness(frontendAppConfig)

          val result = controller.callFilter(request).futureValue

          result.value mustEqual Redirect(redirectUrl)
        }
      }

    }

  }

}
