#!/bin/bash

echo ""
echo "Applying migration CountryOfSaleFromEu"

echo "Adding routes to conf/app.routes"

echo "" >> ../conf/app.routes
echo "GET        /:period/countryOfSaleFromEu                        controllers.CountryOfSaleFromEuController.onPageLoad(mode: Mode = NormalMode, period: Period)" >> ../conf/app.routes
echo "POST       /:period/countryOfSaleFromEu                        controllers.CountryOfSaleFromEuController.onSubmit(mode: Mode = NormalMode, period: Period)" >> ../conf/app.routes

echo "GET        /:period/changeCountryOfSaleFromEu                  controllers.CountryOfSaleFromEuController.onPageLoad(mode: Mode = CheckMode, period: Period)" >> ../conf/app.routes
echo "POST       /:period/changeCountryOfSaleFromEu                  controllers.CountryOfSaleFromEuController.onSubmit(mode: Mode = CheckMode, period: Period)" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "countryOfSaleFromEu.title = countryOfSaleFromEu" >> ../conf/messages.en
echo "countryOfSaleFromEu.heading = countryOfSaleFromEu" >> ../conf/messages.en
echo "countryOfSaleFromEu.checkYourAnswersLabel = countryOfSaleFromEu" >> ../conf/messages.en
echo "countryOfSaleFromEu.error.required = Enter countryOfSaleFromEu" >> ../conf/messages.en
echo "countryOfSaleFromEu.error.length = CountryOfSaleFromEu must be 100 characters or less" >> ../conf/messages.en
echo "countryOfSaleFromEu.change.hidden = CountryOfSaleFromEu" >> ../conf/messages.en

echo "Adding to UserAnswersEntryGenerators"
awk '/trait UserAnswersEntryGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitraryCountryOfSaleFromEuUserAnswersEntry: Arbitrary[(CountryOfSaleFromEuPage.type, JsValue)] =";\
    print "    Arbitrary {";\
    print "      for {";\
    print "        page  <- arbitrary[CountryOfSaleFromEuPage.type]";\
    print "        value <- arbitrary[String].suchThat(_.nonEmpty).map(Json.toJson(_))";\
    print "      } yield (page, value)";\
    print "    }";\
    next }1' ../test/generators/UserAnswersEntryGenerators.scala > tmp && mv tmp ../test/generators/UserAnswersEntryGenerators.scala

echo "Adding to PageGenerators"
awk '/trait PageGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitraryCountryOfSaleFromEuPage: Arbitrary[CountryOfSaleFromEuPage.type] =";\
    print "    Arbitrary(CountryOfSaleFromEuPage)";\
    next }1' ../test/generators/PageGenerators.scala > tmp && mv tmp ../test/generators/PageGenerators.scala

echo "Adding to UserAnswersGenerator"
awk '/val generators/ {\
    print;\
    print "    arbitrary[(CountryOfSaleFromEuPage.type, JsValue)] ::";\
    next }1' ../test/generators/UserAnswersGenerator.scala > tmp && mv tmp ../test/generators/UserAnswersGenerator.scala

echo "Migration CountryOfSaleFromEu completed"
