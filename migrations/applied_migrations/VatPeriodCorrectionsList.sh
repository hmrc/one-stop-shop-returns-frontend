#!/bin/bash

echo ""
echo "Applying migration VatPeriodCorrectionsList"

echo "Adding routes to conf/app.routes"

echo "" >> ../conf/app.routes
echo "GET        /:period/vatPeriodCorrectionsList                        controllers.VatPeriodCorrectionsListController.onPageLoad(mode: Mode = NormalMode, period: Period)" >> ../conf/app.routes
echo "POST       /:period/vatPeriodCorrectionsList                        controllers.VatPeriodCorrectionsListController.onSubmit(mode: Mode = NormalMode, period: Period)" >> ../conf/app.routes

echo "GET        /:period/changeVatPeriodCorrectionsList                  controllers.VatPeriodCorrectionsListController.onPageLoad(mode: Mode = CheckMode, period: Period)" >> ../conf/app.routes
echo "POST       /:period/changeVatPeriodCorrectionsList                  controllers.VatPeriodCorrectionsListController.onSubmit(mode: Mode = CheckMode, period: Period)" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "vatPeriodCorrectionsList.title = vatPeriodCorrectionsList" >> ../conf/messages.en
echo "vatPeriodCorrectionsList.heading = vatPeriodCorrectionsList" >> ../conf/messages.en
echo "vatPeriodCorrectionsList.checkYourAnswersLabel = vatPeriodCorrectionsList" >> ../conf/messages.en
echo "vatPeriodCorrectionsList.error.required = Select yes if vatPeriodCorrectionsList" >> ../conf/messages.en
echo "vatPeriodCorrectionsList.change.hidden = VatPeriodCorrectionsList" >> ../conf/messages.en

echo "Adding to UserAnswersEntryGenerators"
awk '/trait UserAnswersEntryGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitraryVatPeriodCorrectionsListUserAnswersEntry: Arbitrary[(VatPeriodCorrectionsListPage.type, JsValue)] =";\
    print "    Arbitrary {";\
    print "      for {";\
    print "        page  <- arbitrary[VatPeriodCorrectionsListPage.type]";\
    print "        value <- arbitrary[Boolean].map(Json.toJson(_))";\
    print "      } yield (page, value)";\
    print "    }";\
    next }1' ../test/generators/UserAnswersEntryGenerators.scala > tmp && mv tmp ../test/generators/UserAnswersEntryGenerators.scala

echo "Adding to PageGenerators"
awk '/trait PageGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitraryVatPeriodCorrectionsListPage: Arbitrary[VatPeriodCorrectionsListPage.type] =";\
    print "    Arbitrary(VatPeriodCorrectionsListPage)";\
    next }1' ../test/generators/PageGenerators.scala > tmp && mv tmp ../test/generators/PageGenerators.scala

echo "Adding to UserAnswersGenerator"
awk '/val generators/ {\
    print;\
    print "    arbitrary[(VatPeriodCorrectionsListPage.type, JsValue)] ::";\
    next }1' ../test/generators/UserAnswersGenerator.scala > tmp && mv tmp ../test/generators/UserAnswersGenerator.scala

echo "Migration VatPeriodCorrectionsList completed"
