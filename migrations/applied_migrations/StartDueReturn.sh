#!/bin/bash

echo ""
echo "Applying migration StartDueReturn"

echo "Adding routes to conf/app.routes"
echo "" >> ../conf/app.routes
echo "GET        /:period/startDueReturn                       controllers.StartDueReturnController.onPageLoad(period: Period)" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "startDueReturn.title = startDueReturn" >> ../conf/messages.en
echo "startDueReturn.heading = startDueReturn" >> ../conf/messages.en

echo "Migration StartDueReturn completed"
