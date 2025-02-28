/*
 * Copyright 2025 HM Revenue & Customs
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

package controllers

import base.SpecBase
import controllers.actions.CheckBouncedEmailFilterImpl
import models.registration.Registration
import models.requests.RegistrationRequest
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import play.api.test.FakeRequest
import play.api.test.Helpers.running

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CheckBouncedEmailFilterSpec extends SpecBase with MockitoSugar {

  private val registration: Registration = arbitraryRegistration.arbitrary.sample.value

  class Harness extends CheckBouncedEmailFilterImpl() {
    def callFilter(request: RegistrationRequest[_]): Future[Option[Result]] = filter(request)
  }

  ".filter" - {

    "must redirect to Unusable Email Page when unusable status is true" in {

      val registrationWithTrueUnusableStatus: Registration = registration.copy(unusableStatus = Some(true))

      val application = applicationBuilder().build()

      running(application) {

        val request = RegistrationRequest(FakeRequest(), testCredentials, vrn, registrationWithTrueUnusableStatus)

        val controller = new Harness

        val result = controller.callFilter(request).futureValue

        result.value `mustBe` Redirect(routes.InterceptUnusableEmailController.onPageLoad().url)
      }
    }

    "must return None when unusable status is false" in {

      val registrationWithTrueUnusableStatus: Registration = registration.copy(unusableStatus = Some(false))

      val application = applicationBuilder().build()

      running(application) {

        val request = RegistrationRequest(FakeRequest(), testCredentials, vrn, registrationWithTrueUnusableStatus)

        val controller = new Harness

        val result = controller.callFilter(request).futureValue

        result `mustBe` None
      }
    }
  }
}
