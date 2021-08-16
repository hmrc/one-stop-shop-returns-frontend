package models

import play.api.libs.json.{Json, OFormat}

import java.time.LocalDate

case class VatRate(
                    rate: BigDecimal,
                    rateType: VatRateType,
                    validFrom: LocalDate,
                    validUntil: Option[LocalDate] = None
                  )

object VatRate {

  implicit val format: OFormat[VatRate] = Json.format[VatRate]
}

sealed trait VatRateType

object VatRateType extends Enumerable.Implicits {

  case object Standard extends WithName("STANDARD") with VatRateType
  case object Reduced extends WithName("REDUCED") with VatRateType

  val values: Seq[VatRateType] = Seq(Standard, Reduced)

  implicit val enumerable: Enumerable[VatRateType] =
    Enumerable(values.map(v => v.toString -> v): _*)
}
