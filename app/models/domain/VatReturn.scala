package models.domain

import models.{Period, ReturnReference}
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.domain.Vrn

import java.time.{Instant, LocalDate}

case class VatReturn(
                      vrn: Vrn,
                      period: Period,
                      reference: ReturnReference,
                      startDate: Option[LocalDate],
                      endDate: Option[LocalDate],
                      salesFromNi: List[SalesToCountry],
                      salesFromEu: List[SalesFromEuCountry],
                      submissionReceived: Instant,
                      lastUpdated: Instant
                    )

object VatReturn {

  implicit val format: OFormat[VatReturn] = Json.format[VatReturn]
}
