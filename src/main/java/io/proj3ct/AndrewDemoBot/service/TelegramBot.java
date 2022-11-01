/*
 *   Developed by Andrei Muryn© 2022
 */

package io.proj3ct.AndrewDemoBot.service;

import com.vdurmont.emoji.EmojiParser;
import io.proj3ct.AndrewDemoBot.model.AdRepository;
import io.proj3ct.AndrewDemoBot.model.Ads;
import io.proj3ct.AndrewDemoBot.model.User;
import io.proj3ct.AndrewDemoBot.model.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class TelegramBot extends TelegramLongPollingBot {

    private static final String YES_BUTTON = "YES_BUTTON";
    private static final String NO_BUTTON = "NO_BUTTON";

    private static final String HELP_TEXT = "This bot is created to demonstraite Spring capabilities.\n" +
            "You can execute commands from the main menu on the left or by typing command:\n" +
            "Type /start to see a welcome message\n" +
            "Type /mydata to see data stored about yourself\n" +
            "Type /help to see this message once again";

    private static final List<BotCommand> COMMAND_LIST = Arrays.asList(
            new BotCommand("/start", "get a welcome message"),
            new BotCommand("/mydata", "get your data store"),
            new BotCommand("/deletedata", "delete my data"),
            new BotCommand("/help", "additional info"),
            new BotCommand("/settings", "set your preferences")
    );

    private final String name;
    private final String token;
    private final UserRepository userRepository;
    private final AdRepository adRepository;

    public TelegramBot(
            @Value("${bot.name}") final String name,
            @Value("${bot.api-token}") final String token,
            final UserRepository userRepository,
            final AdRepository adRepository
    ) {
        this.name = name;
        this.token = token;
        this.userRepository = userRepository;
        this.adRepository = adRepository;
        try {
            this.execute(new SetMyCommands(COMMAND_LIST, new BotCommandScopeDefault(), null));
        } catch (final TelegramApiException ex) {
            System.err.println("Failed to initialize menu");
        }
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

            if (message.contains("/send") && "378035819".equals(String.valueOf(chatId))) { //bot owner id
                final String textToSend = EmojiParser.parseToUnicode(message.substring(
                        message.indexOf(" ")
                ));
                final Iterable<User> users = userRepository.findAll();
                for (final User user : users) {
                    prepareAndSendMessage(user.getChatId(), textToSend);
                }

            } else {
                switch (message) {
                    case "/start":
                        registerUser(update.getMessage());
                        final String firstName = update.getMessage().getChat().getFirstName();
                        sendMessage(
                                chatId,
                                EmojiParser.parseToUnicode(
                                        String.format("Hello %s :blush:", firstName)
                                )
                        );
                        break;
                    case "/help":
                        prepareAndSendMessage(chatId, HELP_TEXT);
                        break;
                    case "/end":
                        prepareAndSendMessage(chatId, "Good bye!");
                        break;
                    case "/register":
                        register(chatId);
                        break;
                    default:
                        prepareAndSendMessage(chatId, "Sorry, the command is not ready yet.");
                }
            }

        } else if (update.hasCallbackQuery()) {
            final String callbackData = update.getCallbackQuery().getData(); //button id(yes/no)
            final long messageId = update.getCallbackQuery().getMessage().getMessageId();
            final long chatId = update.getCallbackQuery().getMessage().getChatId();

            final EditMessageText message = new EditMessageText();
            if (callbackData.equals(YES_BUTTON)) {
                final String text = "You pressed YES button";
                executeEditMessageText((int) messageId, chatId, message, text);
            } else if (callbackData.equals(NO_BUTTON)) {
                final String text = "You pressed NO button";
                executeEditMessageText((int) messageId, chatId, message, text);
            }
        }

    }

    private void executeEditMessageText(
            final int messageId,
            final long chatId,
            final EditMessageText message,
            final String text
    ) {
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setMessageId(messageId);
        try {
            execute(message);
        } catch (final TelegramApiException ex) {
            System.err.println("Failed to use markup");
        }
    }

    private void register(final long chatId) {
        final SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Do you really want to register?");

        final InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        final List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        final List<InlineKeyboardButton> rowInLine = new ArrayList<>();

        final InlineKeyboardButton yesButton = new InlineKeyboardButton();
        yesButton.setText("Yes");
        yesButton.setCallbackData("YES_BUTTON"); //button id

        final InlineKeyboardButton noButton = new InlineKeyboardButton();
        noButton.setText("No");
        noButton.setCallbackData("NO_BUTTON"); //button id

        rowInLine.add(yesButton);
        rowInLine.add(noButton);

        rowsInLine.add(rowInLine);
        markupInLine.setKeyboard(rowsInLine);
        message.setReplyMarkup(markupInLine);

        executeMessage(message);
    }

    private void registerUser(final Message message) {
        if (!userRepository.findById(message.getChatId()).isPresent()) {
            final long chatId = message.getChatId();
            final Chat chat = message.getChat();
            final User user = new User();
            user.setChatId(chatId);
            user.setFirstName(chat.getFirstName());
            user.setLastName(chat.getLastName());
            user.setUserName(chat.getUserName());
            user.setRegisteredAt(new Timestamp(System.currentTimeMillis()));
            userRepository.save(user);
        }
    }

    private void sendMessage(final long chatId, final String message) {
        final SendMessage sMessage = new SendMessage(String.valueOf(chatId), message);
        final ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        final List<KeyboardRow> keyboardRowList = new ArrayList<>();

        final KeyboardRow row = new KeyboardRow();
        row.add("weather");
        row.add("get random joke");
        keyboardRowList.add(row);

        final KeyboardRow row1 = new KeyboardRow();
        row1.add("register");
        row1.add("check my data");
        row1.add("delete my data");
        keyboardRowList.add(row1);

        keyboardMarkup.setKeyboard(keyboardRowList);
        sMessage.setReplyMarkup(keyboardMarkup);
        executeMessage(sMessage);
    }

    private void executeMessage(final SendMessage sMessage) {
        try {
            execute(sMessage);
        } catch (final TelegramApiException ex) {
            System.err.println("Failed to send message");
        }
    }

    private void prepareAndSendMessage(final long chatId, final String textToSend) {
        final SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);
        executeMessage(message);
    }

    @Scheduled(cron = "0 * * * * *")
    private void sendAds() {
        final Iterable<Ads> ads = adRepository.findAll();
        final Iterable<User> users = userRepository.findAll();
        for (final Ads ad : ads) {
            for (final User user : users) {
                prepareAndSendMessage(user.getChatId(), ad.getAd());
            }
        }

    }
}
