package ru.kurilov.time.manager.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.kurilov.time.manager.model.EventModel;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Repository
@RequiredArgsConstructor
@Transactional
public class TimeManagerRepository {

    private final JdbcClient jdbcClient;

    public int addEvent(EventModel eventModel) {
        return jdbcClient.sql("""
                        INSERT INTO EVENTS (USER_ID, EVENT_NAME, EVENT_DATE_TIME)
                        VALUES (:userId, :event, :eventTime)""")
                .param("event", eventModel.getEventName())
                .param("userId", eventModel.getUserId())
                .param("eventTime", eventModel.getEventDateTime())
                .update();
    }

    public List<String> getEvents(long userId) {
        AtomicInteger count = new AtomicInteger();
        return jdbcClient.sql("""
                        SELECT * FROM EVENTS
                        WHERE USER_ID = :userId
                        AND EVENT_DATE_TIME > date('now')
                        ORDER BY EVENT_DATE_TIME""")
                .param("userId", userId)
                .query((rs, rowNum) -> new EventModel()
                        .setEventName(rs.getString("EVENT_NAME"))
                        .setEventDateTime(OffsetDateTime.parse(rs.getString("EVENT_DATE_TIME")))
                        .toMessage(count.incrementAndGet()))
                .list();
    }

    public int deleteEventByUserChose(long userId, int userIdChose) {
        Long id = jdbcClient.sql("""
                        SELECT ID FROM EVENTS
                        WHERE USER_ID = :userId
                        AND EVENT_DATE_TIME > date('now')
                        ORDER BY EVENT_DATE_TIME
                        LIMIT 1 OFFSET :userIdChose
                        """)
                .param("userId", userId)
                .param("userIdChose", userIdChose - 1)
                .query(Long.class)
                .single();
        return jdbcClient.sql("""
                        DELETE FROM EVENTS
                        WHERE ID = :id""")
                .param("id", id)
                .update();
    }

    public EventModel getEventByUserChose(long userId, int userIdChose) {
        return jdbcClient.sql("""
                        SELECT * FROM EVENTS
                        WHERE USER_ID = :userId
                        AND EVENT_DATE_TIME > date('now')
                        ORDER BY EVENT_DATE_TIME
                        LIMIT 1 OFFSET :userIdChose
                        """)
                .param("userId", userId)
                .param("userIdChose", userIdChose - 1)
                .query((rs, rowNum) -> new EventModel()
                        .setEventName(rs.getString("EVENT_NAME"))
                        .setEventDateTime(OffsetDateTime.parse(rs.getString("EVENT_DATE_TIME"))))
                .single();
    }

    public List<String> getEventsTomorrow(Long userId) {
        AtomicInteger count = new AtomicInteger();
        return jdbcClient.sql("""
                        SELECT * FROM EVENTS
                        WHERE USER_ID = :userId
                        AND EVENT_DATE_TIME BETWEEN date('now', '+1 day') AND date('now', '+2 day')
                        ORDER BY EVENT_DATE_TIME""")
                .param("userId", userId)
                .query((rs, rowNum) -> new EventModel()
                        .setEventName(rs.getString("EVENT_NAME"))
                        .setEventDateTime(OffsetDateTime.parse(rs.getString("EVENT_DATE_TIME")))
                        .toMessage(count.incrementAndGet()))
                .list();
    }

    public List<String> getEventsToday(Long userId) {
        AtomicInteger count = new AtomicInteger();
        return jdbcClient.sql("""
                        SELECT * FROM EVENTS
                        WHERE USER_ID = :userId
                        AND EVENT_DATE_TIME BETWEEN date('now') AND date('now', '+1 day')
                        ORDER BY EVENT_DATE_TIME""")
                .param("userId", userId)
                .query((rs, rowNum) -> new EventModel()
                        .setEventName(rs.getString("EVENT_NAME"))
                        .setEventDateTime(OffsetDateTime.parse(rs.getString("EVENT_DATE_TIME")))
                        .toMessage(count.incrementAndGet()))
                .list();
    }
}
