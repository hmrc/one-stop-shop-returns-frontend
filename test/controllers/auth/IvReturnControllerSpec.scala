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

package controllers.auth

import base.SpecBase
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import views.html.iv._

class IvReturnControllerSpec extends SpecBase {

  private val continueUrl = "http://localhost/foo"

  "Iv Return Controller" - {

    ".error" - {

      "must return Ok and the correct view" in {

        val app = applicationBuilder(None).build()

        running(app) {
          val view = app.injector.instanceOf[IvErrorView]
          val request = FakeRequest(GET, routes.IvReturnController.error(RedirectUrl("http://localhost/foo")).url)
          val result = route(app, request).value

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(continueUrl)(request, messages(app)).toString
        }
      }
    }

    ".failedMatching" - {

      "must return Ok and the correct view" in {

        val app = applicationBuilder(None).build()

        running(app) {
          val view = app.injector.instanceOf[IvFailedMatchingView]
          val request = FakeRequest(GET, routes.IvReturnController.failedMatching(RedirectUrl("http://localhost/foo")).url)
          val result = route(app, request).value

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(continueUrl)(request, messages(app)).toString
        }
      }
    }

    ".failed" - {

      "must return Ok and the correct view" in {

        val app = applicationBuilder(None).build()

        running(app) {
          val view = app.injector.instanceOf[IvFailedView]
          val request = FakeRequest(GET, routes.IvReturnController.failed(RedirectUrl("http://localhost/foo")).url)
          val result = route(app, request).value

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(continueUrl)(request, messages(app)).toString
        }
      }
    }

    ".incomplete" - {

      "must return Ok and the correct view" in {

        val app = applicationBuilder(None).build()

        running(app) {
          val view = app.injector.instanceOf[IvIncompleteView]
          val request = FakeRequest(GET, routes.IvReturnController.incomplete(RedirectUrl("http://localhost/foo")).url)
          val result = route(app, request).value

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(continueUrl)(request, messages(app)).toString
        }
      }
    }

    ".insufficientEvidence" - {

      "must return Ok and the correct view" in {

        val app = applicationBuilder(None).build()

        running(app) {
          val view = app.injector.instanceOf[InsufficientEvidenceView]
          val request = FakeRequest(GET, routes.IvReturnController.insufficientEvidence(RedirectUrl("http://localhost/foo")).url)
          val result = route(app, request).value

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(continueUrl)(request, messages(app)).toString
        }
      }
    }

    ".lockedOut" - {

      "must return Ok and the correct view" in {

        val app = applicationBuilder(None).build()

        running(app) {
          val view = app.injector.instanceOf[IvLockedOutView]
          val request = FakeRequest(GET, routes.IvReturnController.lockedOut(RedirectUrl("http://localhost/foo")).url)
          val result = route(app, request).value

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(continueUrl)(request, messages(app)).toString
        }
      }
    }

    ".notEnoughEvidenceSources" - {

      "must return Ok and the correct view" in {

        val app = applicationBuilder(None).build()

        running(app) {
          val view = app.injector.instanceOf[IvNotEnoughEvidenceView]
          val request = FakeRequest(GET, routes.IvReturnController.notEnoughEvidenceSources(RedirectUrl("http://localhost/foo")).url)
          val result = route(app, request).value

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(continueUrl)(request, messages(app)).toString
        }
      }
    }

    ".preconditionFailed" - {

      "must return Ok and the correct view" in {

        val app = applicationBuilder(None).build()

        running(app) {
          val view = app.injector.instanceOf[IvPreconditionFailedView]
          val request = FakeRequest(GET, routes.IvReturnController.preconditionFailed(RedirectUrl("http://localhost/foo")).url)
          val result = route(app, request).value

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(continueUrl)(request, messages(app)).toString
        }
      }
    }

    ".technicalIssue" - {

      "must return Ok and the correct view" in {

        val app = applicationBuilder(None).build()

        running(app) {
          val view = app.injector.instanceOf[IvTechnicalIssueView]
          val request = FakeRequest(GET, routes.IvReturnController.technicalIssue(RedirectUrl("http://localhost/foo")).url)
          val result = route(app, request).value

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(continueUrl)(request, messages(app)).toString
        }
      }
    }

    ".timeout" - {

      "must return Ok and the correct view" in {

        val app = applicationBuilder(None).build()

        running(app) {
          val view = app.injector.instanceOf[IvTimeoutView]
          val request = FakeRequest(GET, routes.IvReturnController.timeout(RedirectUrl("http://localhost/foo")).url)
          val result = route(app, request).value

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(continueUrl)(request, messages(app)).toString
        }
      }
    }

    ".userAborted" - {

      "must return Ok and the correct view" in {

        val app = applicationBuilder(None).build()

        running(app) {
          val view = app.injector.instanceOf[IvUserAbortedView]
          val request = FakeRequest(GET, routes.IvReturnController.userAborted(RedirectUrl("http://localhost/foo")).url)
          val result = route(app, request).value

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(continueUrl)(request, messages(app)).toString
        }
      }
    }
  }
}