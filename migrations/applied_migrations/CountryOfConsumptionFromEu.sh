#!/bin/bash

echo ""
echo "Applying migration CountryOfConsumptionFromEu"

echo "Adding routes to conf/app.routes"

echo "" >> ../conf/app.routes
echo "GET        /:period/countryOfConsumptionFromEu                        controllers.CountryOfConsumptionFromEuController.onPageLoad(mode: Mode = NormalMode, period: Period)" >> ../conf/app.routes
echo "POST       /:period/countryOfConsumptionFromEu                        controllers.CountryOfConsumptionFromEuController.onSubmit(mode: Mode = NormalMode, period: Period)" >> ../conf/app.routes

echo "GET        /:period/changeCountryOfConsumptionFromEu                  controllers.CountryOfConsumptionFromEuController.onPageLoad(mode: Mode = CheckMode, period: Period)" >> ../conf/app.routes
echo "POST       /:period/changeCountryOfConsumptionFromEu                  controllers.CountryOfConsumptionFromEuController.onSubmit(mode: Mode = CheckMode, period: Period)" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "countryOfConsumptionFromEu.title = countryOfConsumptionFromEu" >> ../conf/messages.en
echo "countryOfConsumptionFromEu.heading = countryOfConsumptionFromEu" >> ../conf/messages.en
echo "countryOfConsumptionFromEu.checkYourAnswersLabel = countryOfConsumptionFromEu" >> ../conf/messages.en
echo "countryOfConsumptionFromEu.error.required = Enter countryOfConsumptionFromEu" >> ../conf/messages.en
echo "countryOfConsumptionFromEu.error.length = CountryOfConsumptionFromEu must be 100 characters or less" >> ../conf/messages.en
echo "countryOfConsumptionFromEu.change.hidden = CountryOfConsumptionFromEu" >> ../conf/messages.en

echo "Adding to UserAnswersEntryGenerators"
awk '/trait UserAnswersEntryGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitraryCountryOfConsumptionFromEuUserAnswersEntry: Arbitrary[(CountryOfConsumptionFromEuPage.type, JsValue)] =";\
    print "    Arbitrary {";\
    print "      for {";\
    print "        page  <- arbitrary[CountryOfConsumptionFromEuPage.type]";\
    print "        value <- arbitrary[String].suchThat(_.nonEmpty).map(Json.toJson(_))";\
    print "      } yield (page, value)";\
    print "    }";\
    next }1' ../test/generators/UserAnswersEntryGenerators.scala > tmp && mv tmp ../test/generators/UserAnswersEntryGenerators.scala

echo "Adding to PageGenerators"
awk '/trait PageGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitraryCountryOfConsumptionFromEuPage: Arbitrary[CountryOfConsumptionFromEuPage.type] =";\
    print "    Arbitrary(CountryOfConsumptionFromEuPage)";\
    next }1' ../test/generators/PageGenerators.scala > tmp && mv tmp ../test/generators/PageGenerators.scala

echo "Adding to UserAnswersGenerator"
awk '/val generators/ {\
    print;\
    print "    arbitrary[(CountryOfConsumptionFromEuPage.type, JsValue)] ::";\
    next }1' ../test/generators/UserAnswersGenerator.scala > tmp && mv tmp ../test/generators/UserAnswersGenerator.scala

echo "Migration CountryOfConsumptionFromEu completed"
