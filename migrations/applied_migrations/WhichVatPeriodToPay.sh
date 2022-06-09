#!/bin/bash

echo ""
echo "Applying migration WhichVatPeriodToPay"

echo "Adding routes to conf/app.routes"

echo "" >> ../conf/app.routes
echo "GET        /:period/whichVatPeriodToPay                        controllers.WhichVatPeriodToPayController.onPageLoad(mode: Mode = NormalMode, period: Period)" >> ../conf/app.routes
echo "POST       /:period/whichVatPeriodToPay                        controllers.WhichVatPeriodToPayController.onSubmit(mode: Mode = NormalMode, period: Period)" >> ../conf/app.routes

echo "GET        /:period/changeWhichVatPeriodToPay                  controllers.WhichVatPeriodToPayController.onPageLoad(mode: Mode = CheckMode, period: Period)" >> ../conf/app.routes
echo "POST       /:period/changeWhichVatPeriodToPay                  controllers.WhichVatPeriodToPayController.onSubmit(mode: Mode = CheckMode, period: Period)" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "whichVatPeriodToPay.title = WhichVatPeriodToPay" >> ../conf/messages.en
echo "whichVatPeriodToPay.heading = WhichVatPeriodToPay" >> ../conf/messages.en
echo "whichVatPeriodToPay.option1 = Option 1" >> ../conf/messages.en
echo "whichVatPeriodToPay.option2 = Option 2" >> ../conf/messages.en
echo "whichVatPeriodToPay.checkYourAnswersLabel = WhichVatPeriodToPay" >> ../conf/messages.en
echo "whichVatPeriodToPay.error.required = Select whichVatPeriodToPay" >> ../conf/messages.en
echo "whichVatPeriodToPay.change.hidden = WhichVatPeriodToPay" >> ../conf/messages.en

echo "Adding to UserAnswersEntryGenerators"
awk '/trait UserAnswersEntryGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitraryWhichVatPeriodToPayUserAnswersEntry: Arbitrary[(WhichVatPeriodToPayPage.type, JsValue)] =";\
    print "    Arbitrary {";\
    print "      for {";\
    print "        page  <- arbitrary[WhichVatPeriodToPayPage.type]";\
    print "        value <- arbitrary[WhichVatPeriodToPay].map(Json.toJson(_))";\
    print "      } yield (page, value)";\
    print "    }";\
    next }1' ../test/generators/UserAnswersEntryGenerators.scala > tmp && mv tmp ../test/generators/UserAnswersEntryGenerators.scala

echo "Adding to PageGenerators"
awk '/trait PageGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitraryWhichVatPeriodToPayPage: Arbitrary[WhichVatPeriodToPayPage.type] =";\
    print "    Arbitrary(WhichVatPeriodToPayPage)";\
    next }1' ../test/generators/PageGenerators.scala > tmp && mv tmp ../test/generators/PageGenerators.scala

echo "Adding to ModelGenerators"
awk '/trait ModelGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitraryWhichVatPeriodToPay: Arbitrary[WhichVatPeriodToPay] =";\
    print "    Arbitrary {";\
    print "      Gen.oneOf(WhichVatPeriodToPay.values.toSeq)";\
    print "    }";\
    next }1' ../test/generators/ModelGenerators.scala > tmp && mv tmp ../test/generators/ModelGenerators.scala

echo "Adding to UserAnswersGenerator"
awk '/val generators/ {\
    print;\
    print "    arbitrary[(WhichVatPeriodToPayPage.type, JsValue)] ::";\
    next }1' ../test/generators/UserAnswersGenerator.scala > tmp && mv tmp ../test/generators/UserAnswersGenerator.scala

echo "Migration WhichVatPeriodToPay completed"
