#!/bin/bash

echo ""
echo "Applying migration NoLongerAbleToViewReturn"

echo "Adding routes to conf/app.routes"
echo "" >> ../conf/app.routes
echo "GET        /:period/noLongerAbleToViewReturn                       controllers.NoLongerAbleToViewReturnController.onPageLoad(period: Period)" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "noLongerAbleToViewReturn.title = noLongerAbleToViewReturn" >> ../conf/messages.en
echo "noLongerAbleToViewReturn.heading = noLongerAbleToViewReturn" >> ../conf/messages.en

echo "Migration NoLongerAbleToViewReturn completed"
