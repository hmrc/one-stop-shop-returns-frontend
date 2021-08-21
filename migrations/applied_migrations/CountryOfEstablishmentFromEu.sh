#!/bin/bash

echo ""
echo "Applying migration CountryOfEstablishmentFromEu"

echo "Adding routes to conf/app.routes"

echo "" >> ../conf/app.routes
echo "GET        /:period/countryOfEstablishmentFromEu                        controllers.CountryOfEstablishmentFromEuController.onPageLoad(mode: Mode = NormalMode, period: Period)" >> ../conf/app.routes
echo "POST       /:period/countryOfEstablishmentFromEu                        controllers.CountryOfEstablishmentFromEuController.onSubmit(mode: Mode = NormalMode, period: Period)" >> ../conf/app.routes

echo "GET        /:period/changeCountryOfEstablishmentFromEu                  controllers.CountryOfEstablishmentFromEuController.onPageLoad(mode: Mode = CheckMode, period: Period)" >> ../conf/app.routes
echo "POST       /:period/changeCountryOfEstablishmentFromEu                  controllers.CountryOfEstablishmentFromEuController.onSubmit(mode: Mode = CheckMode, period: Period)" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "countryOfEstablishmentFromEu.title = countryOfEstablishmentFromEu" >> ../conf/messages.en
echo "countryOfEstablishmentFromEu.heading = countryOfEstablishmentFromEu" >> ../conf/messages.en
echo "countryOfEstablishmentFromEu.option1 = Option 1" >> ../conf/messages.en
echo "countryOfEstablishmentFromEu.option2 = Option 2" >> ../conf/messages.en
echo "countryOfEstablishmentFromEu.checkYourAnswersLabel = countryOfEstablishmentFromEu" >> ../conf/messages.en
echo "countryOfEstablishmentFromEu.error.required = Select countryOfEstablishmentFromEu" >> ../conf/messages.en
echo "countryOfEstablishmentFromEu.change.hidden = CountryOfEstablishmentFromEu" >> ../conf/messages.en

echo "Adding to UserAnswersEntryGenerators"
awk '/trait UserAnswersEntryGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitraryCountryOfEstablishmentFromEuUserAnswersEntry: Arbitrary[(CountryOfEstablishmentFromEuPage.type, JsValue)] =";\
    print "    Arbitrary {";\
    print "      for {";\
    print "        page  <- arbitrary[CountryOfEstablishmentFromEuPage.type]";\
    print "        value <- arbitrary[CountryOfEstablishmentFromEu].map(Json.toJson(_))";\
    print "      } yield (page, value)";\
    print "    }";\
    next }1' ../test/generators/UserAnswersEntryGenerators.scala > tmp && mv tmp ../test/generators/UserAnswersEntryGenerators.scala

echo "Adding to PageGenerators"
awk '/trait PageGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitraryCountryOfEstablishmentFromEuPage: Arbitrary[CountryOfEstablishmentFromEuPage.type] =";\
    print "    Arbitrary(CountryOfEstablishmentFromEuPage)";\
    next }1' ../test/generators/PageGenerators.scala > tmp && mv tmp ../test/generators/PageGenerators.scala

echo "Adding to ModelGenerators"
awk '/trait ModelGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitraryCountryOfEstablishmentFromEu: Arbitrary[CountryOfEstablishmentFromEu] =";\
    print "    Arbitrary {";\
    print "      Gen.oneOf(CountryOfEstablishmentFromEu.values.toSeq)";\
    print "    }";\
    next }1' ../test/generators/ModelGenerators.scala > tmp && mv tmp ../test/generators/ModelGenerators.scala

echo "Adding to UserAnswersGenerator"
awk '/val generators/ {\
    print;\
    print "    arbitrary[(CountryOfEstablishmentFromEuPage.type, JsValue)] ::";\
    next }1' ../test/generators/UserAnswersGenerator.scala > tmp && mv tmp ../test/generators/UserAnswersGenerator.scala

echo "Migration CountryOfEstablishmentFromEu completed"
