package ru.kurilov.time.manager.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.kurilov.time.manager.bot.TimeManagerTelegramBot;
import ru.kurilov.time.manager.repository.TimeManagerRepository;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@EnableScheduling
public class TimeManagerNotificationService {

    private final TimeManagerRepository timeManagerRepository;
    private final TimeManagerTelegramBot timeManagerTelegramBot;

    @Scheduled(cron = "0 00 6 * * *")
    public void sendNotification() throws TelegramApiException {
        Map<Long, List<String>> eventsForDay = timeManagerRepository.getEventsForDay();
        for (Map.Entry<Long, List<String>> entry : eventsForDay.entrySet()) {
            List<String> events = entry.getValue();
            timeManagerTelegramBot.execute(timeManagerTelegramBot.createMessage(
                    entry.getKey(), events));
        }
    }
}
