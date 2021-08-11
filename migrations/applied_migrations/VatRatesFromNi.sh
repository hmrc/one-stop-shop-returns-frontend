#!/bin/bash

echo ""
echo "Applying migration VatRatesFromNi"

echo "Adding routes to conf/app.routes"

echo "" >> ../conf/app.routes
echo "GET        /vatRatesFromNi                        controllers.VatRatesFromNiController.onPageLoad(mode: Mode = NormalMode)" >> ../conf/app.routes
echo "POST       /vatRatesFromNi                        controllers.VatRatesFromNiController.onSubmit(mode: Mode = NormalMode)" >> ../conf/app.routes

echo "GET        /changeVatRatesFromNi                  controllers.VatRatesFromNiController.onPageLoad(mode: Mode = CheckMode)" >> ../conf/app.routes
echo "POST       /changeVatRatesFromNi                  controllers.VatRatesFromNiController.onSubmit(mode: Mode = CheckMode)" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "vatRatesFromNi.title = vatRatesFromNi" >> ../conf/messages.en
echo "vatRatesFromNi.heading = vatRatesFromNi" >> ../conf/messages.en
echo "vatRatesFromNi.option1 = Option 1" >> ../conf/messages.en
echo "vatRatesFromNi.option2 = Option 2" >> ../conf/messages.en
echo "vatRatesFromNi.checkYourAnswersLabel = vatRatesFromNi" >> ../conf/messages.en
echo "vatRatesFromNi.error.required = Select vatRatesFromNi" >> ../conf/messages.en
echo "vatRatesFromNi.change.hidden = VatRatesFromNi" >> ../conf/messages.en

echo "Adding to UserAnswersEntryGenerators"
awk '/trait UserAnswersEntryGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitraryVatRatesFromNiUserAnswersEntry: Arbitrary[(VatRatesFromNiPage.type, JsValue)] =";\
    print "    Arbitrary {";\
    print "      for {";\
    print "        page  <- arbitrary[VatRatesFromNiPage.type]";\
    print "        value <- arbitrary[VatRatesFromNi].map(Json.toJson(_))";\
    print "      } yield (page, value)";\
    print "    }";\
    next }1' ../test/generators/UserAnswersEntryGenerators.scala > tmp && mv tmp ../test/generators/UserAnswersEntryGenerators.scala

echo "Adding to PageGenerators"
awk '/trait PageGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitraryVatRatesFromNiPage: Arbitrary[VatRatesFromNiPage.type] =";\
    print "    Arbitrary(VatRatesFromNiPage)";\
    next }1' ../test/generators/PageGenerators.scala > tmp && mv tmp ../test/generators/PageGenerators.scala

echo "Adding to ModelGenerators"
awk '/trait ModelGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitraryVatRatesFromNi: Arbitrary[VatRatesFromNi] =";\
    print "    Arbitrary {";\
    print "      Gen.oneOf(VatRatesFromNi.values)";\
    print "    }";\
    next }1' ../test/generators/ModelGenerators.scala > tmp && mv tmp ../test/generators/ModelGenerators.scala

echo "Adding to UserAnswersGenerator"
awk '/val generators/ {\
    print;\
    print "    arbitrary[(VatRatesFromNiPage.type, JsValue)] ::";\
    next }1' ../test/generators/UserAnswersGenerator.scala > tmp && mv tmp ../test/generators/UserAnswersGenerator.scala

echo "Migration VatRatesFromNi completed"
