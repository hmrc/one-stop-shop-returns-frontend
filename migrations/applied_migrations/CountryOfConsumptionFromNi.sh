#!/bin/bash

echo ""
echo "Applying migration CountryOfConsumptionFromNi"

echo "Adding routes to conf/app.routes"

echo "" >> ../conf/app.routes
echo "GET        /countryOfConsumptionFromNi                        controllers.CountryOfConsumptionFromNiController.onPageLoad(mode: Mode = NormalMode)" >> ../conf/app.routes
echo "POST       /countryOfConsumptionFromNi                        controllers.CountryOfConsumptionFromNiController.onSubmit(mode: Mode = NormalMode)" >> ../conf/app.routes

echo "GET        /changeCountryOfConsumptionFromNi                  controllers.CountryOfConsumptionFromNiController.onPageLoad(mode: Mode = CheckMode)" >> ../conf/app.routes
echo "POST       /changeCountryOfConsumptionFromNi                  controllers.CountryOfConsumptionFromNiController.onSubmit(mode: Mode = CheckMode)" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "countryOfConsumptionFromNi.title = countryOfConsumptionFromNi" >> ../conf/messages.en
echo "countryOfConsumptionFromNi.heading = countryOfConsumptionFromNi" >> ../conf/messages.en
echo "countryOfConsumptionFromNi.checkYourAnswersLabel = countryOfConsumptionFromNi" >> ../conf/messages.en
echo "countryOfConsumptionFromNi.error.required = Enter countryOfConsumptionFromNi" >> ../conf/messages.en
echo "countryOfConsumptionFromNi.error.length = CountryOfConsumptionFromNi must be 100 characters or less" >> ../conf/messages.en
echo "countryOfConsumptionFromNi.change.hidden = CountryOfConsumptionFromNi" >> ../conf/messages.en

echo "Adding to UserAnswersEntryGenerators"
awk '/trait UserAnswersEntryGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitraryCountryOfConsumptionFromNiUserAnswersEntry: Arbitrary[(CountryOfConsumptionFromNiPage.type, JsValue)] =";\
    print "    Arbitrary {";\
    print "      for {";\
    print "        page  <- arbitrary[CountryOfConsumptionFromNiPage.type]";\
    print "        value <- arbitrary[String].suchThat(_.nonEmpty).map(Json.toJson(_))";\
    print "      } yield (page, value)";\
    print "    }";\
    next }1' ../test/generators/UserAnswersEntryGenerators.scala > tmp && mv tmp ../test/generators/UserAnswersEntryGenerators.scala

echo "Adding to PageGenerators"
awk '/trait PageGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitraryCountryOfConsumptionFromNiPage: Arbitrary[CountryOfConsumptionFromNiPage.type] =";\
    print "    Arbitrary(CountryOfConsumptionFromNiPage)";\
    next }1' ../test/generators/PageGenerators.scala > tmp && mv tmp ../test/generators/PageGenerators.scala

echo "Adding to UserAnswersGenerator"
awk '/val generators/ {\
    print;\
    print "    arbitrary[(CountryOfConsumptionFromNiPage.type, JsValue)] ::";\
    next }1' ../test/generators/UserAnswersGenerator.scala > tmp && mv tmp ../test/generators/UserAnswersGenerator.scala

echo "Migration CountryOfConsumptionFromNi completed"
