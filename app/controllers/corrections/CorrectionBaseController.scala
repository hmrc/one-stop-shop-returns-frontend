package controllers.corrections

import models.requests.DataRequest
import models.{Index, Period}
import play.api.mvc.Results.Redirect
import play.api.mvc.{AnyContent, Result}
import queries.corrections.CorrectionPeriodQuery

import scala.concurrent.Future

trait CorrectionBaseController {

  protected def getCorrectionReturnPeriod(periodIndex: Index)
                                         (block: Period => Future[Result])
                                         (implicit request: DataRequest[AnyContent]): Future[Result] =
    request.userAnswers
      .get(CorrectionPeriodQuery(periodIndex))
      .map(_.correctionReturnPeriod)
      .map(block(_))
      .getOrElse(Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())))
}
