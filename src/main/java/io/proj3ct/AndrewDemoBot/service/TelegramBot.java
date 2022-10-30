/*
 *   Developed by Andrei Muryn© 2022
 */

package io.proj3ct.AndrewDemoBot.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
public class TelegramBot extends TelegramLongPollingBot {

    private final String name;
    private final String token;

    public TelegramBot(
            @Value("${bot.name}") final String name,
            @Value("${bot.api-token}") final String token
    ) {
        this.name = name;
        this.token = token;
    }


    @Override
    public String getBotUsername() {
        return name;
    }

    @Override
    public String getBotToken() {
        return token;
    }

    @Override
    public void onUpdateReceived(final Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            final long chatId = update.getMessage().getChatId();
            final String message = update.getMessage().getText();

            switch (message) {
                case "/start":
                    sendMessage(chatId, update.getMessage().getChat().getFirstName());
                    break;
                case "/end":
                    sendMessage(chatId, "Good bye!");
                    break;
                default:
                    sendMessage(chatId, "Sorry, the command is not ready yet.");
            }
        }

    }

    private void sendMessage(final long chatId, final String message) {
        final SendMessage sMessage = new SendMessage(String.valueOf(chatId), message);
        try {
            execute(sMessage);
        } catch (final TelegramApiException ex) {
            System.err.println("Failed to send message");
        }
    }

}
