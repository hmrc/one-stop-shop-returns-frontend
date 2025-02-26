package models.emails

import base.SpecBase
import play.api.libs.json.{JsError, JsSuccess, Json}

class EmailParametersSpec extends SpecBase{

  "RegistrationConfirmation" - {

    "serialize and deserialize correctly" in {
      val registration = ReturnsConfirmationEmailParameters(
        recipientName_line1 = "John Doe",
        businessName = "ABC Ltd",
        period = "Period",
        paymentDeadline = "Payment DeadLine"
      )

      val json = Json.toJson(registration)
      val expectedJson = Json.parse(
        """
          {
            "recipientName_line1": "John Doe",
            "businessName": "ABC Ltd",
            "period": "Period",
            "paymentDeadline": "Payment DeadLine"
          }
          """
      )

      json mustBe expectedJson
      json.as[ReturnsConfirmationEmailParameters] mustBe registration
    }

    "fail to deserialize when a required field is missing" in {
      val invalidJson = Json.parse(
        """
          {
            "businessName": "ABC Ltd",
            "periodOfFirstReturn": "2024-04"
          }
          """
      )

      invalidJson.validate[ReturnsConfirmationEmailParameters] mustBe a[JsError]
    }
  }

  "AmendRegistrationConfirmation" - {

    "serialize and deserialize correctly" in {
      val amendment = ReturnsConfirmationEmailNoVatOwedParameters(
        recipientName_line1 = "Jane Doe",
        period = "period"
      )

      val json = Json.toJson(amendment)
      val expectedJson = Json.parse(
        """
          {
            "recipientName_line1": "Jane Doe",
            "period": "period"
          }
          """
      )

      json mustBe expectedJson
      json.as[ReturnsConfirmationEmailNoVatOwedParameters] mustBe amendment
    }

    "fail to deserialize when a required field is missing" in {
      val invalidJson = Json.parse(
        """
          {
            "recipientName_line1": "Jane Doe"
          }
          """
      )

      invalidJson.validate[ReturnsConfirmationEmailNoVatOwedParameters] mustBe a[JsError]
    }
  }

  "EmailParameters" - {

    "serialize and deserialize RegistrationConfirmation correctly as EmailParameters" in {
      val registration = ReturnsConfirmationEmailParameters(
        recipientName_line1 = "John Doe",
        businessName = "ABC Ltd",
        period = "Period",
        paymentDeadline = "Payment DeadLine"
      )

      val json = Json.toJson(registration)(EmailParameters.writes)
      val deserialized = json.as[ReturnsConfirmationEmailParameters]

      deserialized mustBe registration
    }

    "serialize and deserialize AmendRegistrationConfirmation correctly as EmailParameters" in {
      val amendment = ReturnsConfirmationEmailNoVatOwedParameters(
        recipientName_line1 = "Jane Doe",
        period = "period"
      )

      val json = Json.toJson(amendment)(EmailParameters.writes)
      val deserialized = json.as[ReturnsConfirmationEmailNoVatOwedParameters]

      deserialized mustBe amendment
    }

    "fail to deserialize unknown EmailParameters type" in {
      val invalidJson = Json.parse(
        """
            {
              "unknownField": "value"
            }
          """
      )

      val deserialized = invalidJson.validate[EmailParameters]

      deserialized mustBe a[JsError]
    }

    "fail to deserialize an unknown EmailParameters subtype" in {
      val json = Json.parse(
        """
          {
            "someUnknownField": "unknownValue"
          }
        """
      )

      json.validate[EmailParameters] mustBe a[JsError]
    }

    "fail to deserialize missing EmailParameters type" in {
      val invalidJson = Json.obj()

      val deserialized = invalidJson.validate[EmailParameters]

      deserialized mustBe a[JsError]
    }

    "fail to deserialize null EmailParameters type" in {
      val invalidJson = Json.parse(
        """
            {
              "businessName": null,
              "periodOfFirstReturn": "2024-04",
              "firstDayOfNextPeriod": "2024-05-01",
              "commencementDate": "2024-03-15",
              "redirectLink": "http://example.com"
            }
              """
      )

      val deserialized = invalidJson.validate[EmailParameters]

      deserialized mustBe a[JsError]
    }

    "serialize RegistrationConfirmation correctly via EmailParameters writes" in {
      val registration = ReturnsConfirmationEmailParameters(
        recipientName_line1 = "John Doe",
        businessName = "XYZ Corp",
        period = "Period",
        paymentDeadline = "Payment DeadLine"
      )

      val json = Json.toJson(registration)(EmailParameters.writes)
      val expectedJson = Json.parse(
        """
          {
            "recipientName_line1": "John Doe",
            "businessName": "XYZ Corp",
            "period": "Period",
            "paymentDeadline": "Payment DeadLine"
          }
        """
      )

      json mustBe expectedJson
    }

    "deserialize RegistrationConfirmation correctly via RegistrationConfirmation reads" in {
      val json = Json.parse(
        """
      {
        "recipientName_line1": "John Doe",
        "businessName": "ABC Ltd",
        "period": "Period",
        "paymentDeadline": "Payment DeadLine"
      }
    """
      )

      json.validate[ReturnsConfirmationEmailParameters] mustBe JsSuccess(ReturnsConfirmationEmailParameters(
        recipientName_line1 = "John Doe",
        businessName = "ABC Ltd",
        period = "Period",
        paymentDeadline = "Payment DeadLine"
      ))
    }

    "fail to deserialize EmailParameters from a null value" in {
      val nullJson = Json.parse("null")

      nullJson.validate[EmailParameters] mustBe a[JsError]
    }
  }
}