#!/bin/bash

echo ""
echo "Applying migration NoPayments"

echo "Adding routes to conf/app.routes"
echo "" >> ../conf/app.routes
echo "GET        /:period/noPayments                       controllers.NoPaymentsController.onPageLoad(period: Period)" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "noPayments.title = noPayments" >> ../conf/messages.en
echo "noPayments.heading = noPayments" >> ../conf/messages.en

echo "Migration NoPayments completed"
