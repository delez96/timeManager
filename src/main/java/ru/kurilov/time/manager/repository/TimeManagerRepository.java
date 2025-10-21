package ru.kurilov.time.manager.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.kurilov.time.manager.model.EventModel;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.commons.collections4.ListUtils.emptyIfNull;

@Repository
@RequiredArgsConstructor
@Transactional
public class TimeManagerRepository {

    private final JdbcClient jdbcClient;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public int addEvent(EventModel eventModel) {
        return jdbcClient.sql("""
                        INSERT INTO EVENTS (USER_ID, EVENT_NAME, EVENT_DATE_TIME)
                        VALUES (:userId, :event, :eventTime)""")
                .param("event", eventModel.getEventName())
                .param("userId", eventModel.getUserId())
                .param("eventTime", eventModel.getEventDateTime().minusHours(3)
                        .format(formatter))
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
                        .setEventDateTime(LocalDateTime.parse(rs.getString("EVENT_DATE_TIME"), formatter).plusHours(3))
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
                        .setEventDateTime(LocalDateTime.parse(rs.getString("EVENT_DATE_TIME"), formatter).plusHours(3)))
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
                        .setEventDateTime(LocalDateTime.parse(rs.getString("EVENT_DATE_TIME"), formatter).plusHours(3))
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
                        .setEventDateTime(LocalDateTime.parse(rs.getString("EVENT_DATE_TIME"), formatter).plusHours(3))
                        .toMessage(count.incrementAndGet()))
                .list();
    }

    public Map<Long, List<String>> getEventsForDay() {
        HashMap<Long, List<String>> byUser = new HashMap<>();
        List<EventModel> listResult = jdbcClient.sql("""
                        SELECT * FROM EVENTS
                        WHERE EVENT_DATE_TIME BETWEEN datetime('now') AND datetime('now', '+1 day')
                        ORDER BY EVENT_DATE_TIME""")
                .query((rs, rowNum) -> new EventModel()
                        .setEventName(rs.getString("EVENT_NAME"))
                        .setUserId(rs.getLong("USER_ID"))
                        .setEventDateTime(LocalDateTime.parse(rs.getString("EVENT_DATE_TIME"), formatter).plusHours(3)))
                .list();
        emptyIfNull(listResult).forEach(ev -> {
            if (byUser.get(ev.getUserId()) == null) {
                byUser.put(ev.getUserId(), new ArrayList<>(List.of(ev.toMessage())));
            } else {
                byUser.get(ev.getUserId()).add(ev.toMessage());
            }
        });
        return byUser;
    }

}
