package ru.kurilov.time.manager.bot;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ForceReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.kurilov.time.manager.configuration.MainConfigurationProperties;
import ru.kurilov.time.manager.model.EventModel;
import ru.kurilov.time.manager.service.AiTextAnalyzer;
import ru.kurilov.time.manager.repository.TimeManagerRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@Service
public class TimeManagerTelegramBot extends TelegramLongPollingBot {

    private static final String ADD_EVENT = "Введите событие и дату/время";
    private static final String DELETE_EVENT = "Выберите номер записи для удаления";
    private MainConfigurationProperties properties;
    private TimeManagerRepository timeManagerRepository;
    private AiTextAnalyzer aiTextAnalyzer;
    private HashMap<Long, Integer> deleteStorage = new HashMap<>();
    private HashMap<Long, EventModel> addStorage = new HashMap<>();


    public TimeManagerTelegramBot(MainConfigurationProperties properties,
                                  TimeManagerRepository timeManagerRepository,
                                  AiTextAnalyzer aiTextAnalyzer) {
        super(properties.getTgApiToken());
        this.properties = properties;
        this.timeManagerRepository = timeManagerRepository;
        this.aiTextAnalyzer = aiTextAnalyzer;
    }

    @Override
    public String getBotUsername() {
        return properties.getUserName();
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasMessage() && update.getMessage().hasText()) {
                Message message = update.getMessage();
                Long chatId = message.getChatId();

                if ("/add".equals(update.getMessage().getText())) {
                    SendMessage question = new SendMessage();
                    question.setChatId(chatId.toString());
                    question.setText(ADD_EVENT);
                    question.setReplyMarkup(ForceReplyKeyboard.builder().selective(true).build());
                    execute(question);
                } else if ("/events".equals(message.getText())) {
                    List<String> events = timeManagerRepository.getEvents(chatId);
                    eventsResponse(events, chatId);
                } else if ("/today".equals(message.getText())) {
                    List<String> events = timeManagerRepository.getEventsToday(chatId);
                    eventsResponse(events, chatId);
                } else if ("/tomorrow".equals(message.getText())) {
                    List<String> events = timeManagerRepository.getEventsTomorrow(chatId);
                    eventsResponse(events, chatId);
                } else if ("/delete".equals(message.getText())) {
                    execute(createMessage(chatId, timeManagerRepository.getEvents(chatId)));
                    SendMessage question = new SendMessage();
                    question.setChatId(chatId.toString());
                    question.setText(DELETE_EVENT);
                    question.setReplyMarkup(ForceReplyKeyboard.builder().selective(true).build());
                    execute(question);
                } else if (message.getReplyToMessage() != null) {
                    if (DELETE_EVENT.equals(message.getReplyToMessage().getText())) {
                        try {
                            int id = Integer.parseInt(message.getText());
                            EventModel eventByUserChose = timeManagerRepository.getEventByUserChose(chatId, id);
                            execute(createMessage(chatId, Collections.singletonList(eventByUserChose.toMessage())));
                            deleteStorage.put(chatId, id);
                            askForDelete(chatId);
                        } catch (NumberFormatException e) {
                            execute(createMessage(chatId, Collections.singletonList("Введите только номер события")));
                        }
                    } else {
                        if (ADD_EVENT.equals(message.getReplyToMessage().getText())) {
                            EventModel eventModel;
                            try {
                                eventModel = aiTextAnalyzer.parseUserEvent(message.getText(), chatId);
                            } catch (RuntimeException e) {
                                execute(createMessage(chatId, Collections.singletonList("Произошла ошибка. Повторите запрос еще раз.")));
                                return;
                            }
                            if (eventModel == null) {
                                execute(createMessage(chatId, Collections.singletonList("Не удалось распознать ваше сообщение. Укажите название события и дату со временем")));
                            } else {
                                addStorage.put(chatId, eventModel);
                                askForAdd(chatId);
                            }
                        }
                    }
                }
            } else if (update.hasCallbackQuery()) {
                callbackQueryHandle(update);
            }
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    public SendMessage createMessage(long chatId, List<String> messages) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        StringBuilder sb = new StringBuilder();
        if (messages.size() > 1) {
            messages.forEach(msg -> sb.append(msg).append("\n=====================\n"));
            message.setText(sb.toString());
        } else {
            message.setText(messages.get(0));
        }
        return message;
    }

    private void eventsResponse(List<String> events, Long chatId) throws TelegramApiException {
        if (events.isEmpty()) {
            execute(createMessage(chatId, Collections.singletonList("Событий не найдено")));
        } else {
            execute(createMessage(chatId, events));
        }
    }

    private void callbackQueryHandle(Update update) throws TelegramApiException {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        String data = callbackQuery.getData();
        long chatId = callbackQuery.getMessage().getChatId();
        int messageId = callbackQuery.getMessage().getMessageId();
        EditMessageReplyMarkup removeKeyboard = new EditMessageReplyMarkup();
        removeKeyboard.setChatId(chatId);
        removeKeyboard.setMessageId(messageId);
        removeKeyboard.setReplyMarkup(null);
        execute(removeKeyboard);
        EditMessageText newText = new EditMessageText();
        newText.setChatId(chatId);
        newText.setMessageId(messageId);
        if (data.equals("YES_ADD")) {
            newText.setText("Вы ответили: Да ✅");
            EventModel eventModel = addStorage.remove(chatId);
            int result = timeManagerRepository.addEvent(eventModel);
            if (result != 0) {
                execute(createMessage(chatId, List.of(eventModel.toMessage(),
                        "Успешно добавлено")));
            }
        } else if (data.equals("YES_DELETE")) {
            newText.setText("Вы ответили: Да ✅");
            int result = timeManagerRepository.deleteEventByUserChose(chatId, deleteStorage.remove(chatId));
            if (result != 0) {
                execute(createMessage(chatId, Collections.singletonList("Событие успешно удалено")));
            }
        } else if (data.contains("NO")) {
            newText.setText("Вы ответили: Нет ❌");
        }
        execute(newText);

    }

    private void askForDelete(long chatId) throws TelegramApiException {
        InlineKeyboardMarkup markup = addButtons("YES_DELETE", "NOT_DELETE");

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Удаляем выбранное событие?");
        message.setReplyMarkup(markup);

        execute(message);
    }

    private void askForAdd(long chatId) throws TelegramApiException {
        InlineKeyboardMarkup markup = addButtons("YES_ADD", "NOT_ADD");
        execute(createMessage(chatId, Collections.singletonList(addStorage.get(chatId).toMessage())));
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Сохраняем получившееся событие?");
        message.setReplyMarkup(markup);

        execute(message);
    }

    private static InlineKeyboardMarkup addButtons(String yes, String no) {
        InlineKeyboardButton yesButton = new InlineKeyboardButton();
        yesButton.setText("Да");
        yesButton.setCallbackData(yes);

        InlineKeyboardButton noButton = new InlineKeyboardButton();
        noButton.setText("Нет");
        noButton.setCallbackData(no);

        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(yesButton);
        row.add(noButton);

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        rows.add(row);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(rows);
        return markup;
    }

}