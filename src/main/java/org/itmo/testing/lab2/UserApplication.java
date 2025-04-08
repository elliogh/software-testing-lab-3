package org.itmo.testing.lab2;

import org.itmo.testing.lab2.controller.UserAnalyticsController;

public class UserApplication {
    public static void main(String[] args) {
        UserAnalyticsController.createApp().start(7002);
    }
}
