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

package controllers.fileUpload

import base.SpecBase
import connectors.UpscanInitiateConnector
import controllers.routes
import forms.FileUploadFormProvider
import models.NormalMode
import models.upscan.{UpscanFileReference, UpscanInitiateResponse}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.fileUpload.FileUploadPage
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.fileUpload.FileUploadView
import utils.FutureSyntax.FutureOps

class FileUploadControllerSpec extends SpecBase with MockitoSugar {

  private val formProvider = new FileUploadFormProvider()
  private val form = formProvider()
  private val csvFile = "test.csv"

  private val fakeInitiateResponse = UpscanInitiateResponse(
    fileReference = UpscanFileReference("fake-ref"),
    postTarget = "/fake-post",
    formFields = Map("someKey" -> "someValue")
  )

  private lazy val fileUploadRoute = controllers.fileUpload.routes.FileUploadController.onPageLoad(NormalMode, period).url

  "FileUpload Controller" - {

    "must return OK and the correct view for a GET" in {

      val mockConnector = mock[UpscanInitiateConnector]

      when(mockConnector.initiateV2(any(), any())(any())) thenReturn fakeInitiateResponse.toFuture

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[UpscanInitiateConnector].toInstance(mockConnector))
        .build()


      running(application) {
        val request = FakeRequest(GET, fileUploadRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[FileUploadView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          form,
          NormalMode,
          period,
          postTarget = fakeInitiateResponse.postTarget,
          formFields = fakeInitiateResponse.formFields,
        )(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = emptyUserAnswers.set(FileUploadPage, csvFile).success.value

      val mockConnector = mock[UpscanInitiateConnector]

      when(mockConnector.initiateV2(any(), any())(any())) thenReturn fakeInitiateResponse.toFuture

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(bind[UpscanInitiateConnector].toInstance(mockConnector))
        .build()

      running(application) {
        val request = FakeRequest(GET, fileUploadRoute)

        val view = application.injector.instanceOf[FileUploadView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          form,
          NormalMode,
          period,
          postTarget = fakeInitiateResponse.postTarget,
          formFields = fakeInitiateResponse.formFields,
          None
        )(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, fileUploadRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }

  "downloadTemplate" - {

    "return OK" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, controllers.fileUpload.routes.FileUploadController.downloadTemplate(period).url)

        val result = route(application, request).value

        status(result) mustEqual OK

        whenReady(result) { response =>
          val headers: Option[String] = response.header.headers.get("Content-Disposition")

          headers mustBe Some("""attachment; filename="OSS return template.ods"""")
        }
      }
    }

    "Redirect to journey recovery with no answers" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, controllers.fileUpload.routes.FileUploadController.downloadTemplate(period).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value `mustBe` controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
