package org.itmo.testing.lab2.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static java.time.LocalDateTime.now;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserStatusServiceTest {

    @Mock
    private UserAnalyticsService userAnalyticsService;
    @InjectMocks
    private UserStatusService userStatusService;

    private static final String USER_ID = "user123";

    @ParameterizedTest(name = "Проверка получения 'Inactive' при activity time = {0}")
    @ValueSource(longs = {Long.MIN_VALUE, -10, 0, 20, 59})
    void getUserStatusInactiveTest(long value) {
        when(userAnalyticsService.getTotalActivityTime(USER_ID)).thenReturn(value);

        String status = userStatusService.getUserStatus(USER_ID);

        verify(userAnalyticsService, times(1)).getTotalActivityTime(USER_ID);
        assertThat(status, is("Inactive"));
    }


    @ParameterizedTest(name = "Проверка получения 'Active' при activity time = {0}")
    @ValueSource(longs = {60, 78, 119})
    void getUserStatusActiveTest(long value) {
        when(userAnalyticsService.getTotalActivityTime(USER_ID)).thenReturn(value);

        String status = userStatusService.getUserStatus(USER_ID);

        verify(userAnalyticsService, times(1)).getTotalActivityTime(USER_ID);
        assertThat(status, is("Active"));
    }

    @ParameterizedTest(name = "Проверка получения 'Highly active' при activity time = {0}")
    @ValueSource(longs = {120, 121, Long.MAX_VALUE})
    void getUserStatusHighlyActiveTest(long value) {
        when(userAnalyticsService.getTotalActivityTime(USER_ID)).thenReturn(value);

        String status = userStatusService.getUserStatus(USER_ID);

        verify(userAnalyticsService, times(1)).getTotalActivityTime(USER_ID);
        assertThat(status, is("Highly active"));
    }

    @Test
    @DisplayName("Проверка получения не нулевого значения Optional")
    void getUserLastSessionDateNotNullSessionTest() {
        LocalDateTime now = now();

        var loginTime1 = now.minusMinutes(120);
        var logoutTime1 = now.minusMinutes(90);
        var loginTime2 = now.minusMinutes(60);
        var logoutTime2 = now.minusMinutes(30);

        when(userAnalyticsService.getUserSessions(USER_ID)).thenReturn(
            List.of(
                new UserAnalyticsService.Session(loginTime1, logoutTime1),
                new UserAnalyticsService.Session(loginTime2, logoutTime2)
            )
        );

        Optional<String> result = userStatusService.getUserLastSessionDate(USER_ID);

        verify(userAnalyticsService, times(1)).getUserSessions(USER_ID);
        assertThat(result, is(Optional.of(logoutTime2.toLocalDate().toString())));
    }

    @Test
    @DisplayName("Проверка получения null значения Optional")
    void getUserLastSessionDateNullSessionTest() {
        when(userAnalyticsService.getUserSessions(USER_ID)).thenReturn(List.of());

        assertThrows(NoSuchElementException.class, () -> userStatusService.getUserLastSessionDate(USER_ID));
        verify(userAnalyticsService, times(1)).getUserSessions(USER_ID);

        //Optional<String> result = userStatusService.getUserLastSessionDate(USER_ID);
        //assertThat(result, is(Optional.empty()));
    }

}
