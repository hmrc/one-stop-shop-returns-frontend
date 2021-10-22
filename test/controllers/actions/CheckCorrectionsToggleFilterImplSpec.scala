package controllers.actions

import base.SpecBase
import config.FrontendAppConfig
import controllers.routes
import models.requests.OptionalDataRequest
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import play.api.test.FakeRequest
import play.api.test.Helpers.running

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CheckCorrectionsToggleFilterImplSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  val mockAppConfig: FrontendAppConfig = mock[FrontendAppConfig]

  class Harness() extends CheckCorrectionsToggleFilterImpl(mockAppConfig) {
    def callFilter(request: OptionalDataRequest[_]): Future[Option[Result]] = filter(request)
  }

  override def beforeEach(): Unit = {
    Mockito.reset(mockAppConfig)
  }

  ".filter" - {

    "must return None when toggle on" in {

      val app = applicationBuilder(None)
        .build()

      when(mockAppConfig.correctionToggle).thenReturn(true)

      running(app) {
        val request = OptionalDataRequest(FakeRequest(), testCredentials, vrn, registration, Some(emptyUserAnswers))
        val controller = new Harness()
        val result = controller.callFilter(request).futureValue
        result must not be defined
      }
    }

    "must redirect when toggle off" in {

      val app = applicationBuilder(None)
        .build()

      when(mockAppConfig.correctionToggle).thenReturn(false)

      running(app) {
        val request = OptionalDataRequest(FakeRequest(), testCredentials, vrn, registration, Some(emptyUserAnswers))
        val controller = new Harness()
        val result = controller.callFilter(request).futureValue
        result.value mustEqual Redirect(routes.YourAccountController.onPageLoad())
      }
    }
  }
}
