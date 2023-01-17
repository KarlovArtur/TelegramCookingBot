package com.karlov.controller;

import com.karlov.service.UpdateProducer;
import com.karlov.utils.MessageUtils;
import lombok.extern.log4j.Log4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import static com.karlov.model.RabbitQueue.DOCUMENT_UPDATE;
import static com.karlov.model.RabbitQueue.TEXT_UPDATE;

@Component
@Log4j
public class UpdateController {

    private TelegramBot telegramBot;
    private final MessageUtils messageUtils;
    @Autowired
    private UpdateProducer updateProducer;

    public UpdateController(MessageUtils messageUtils) {
        this.messageUtils = messageUtils;
    }

    public void registerBot(TelegramBot telegramBot) {
        this.telegramBot = telegramBot;
    }

    public void processUpdate(Update update) {
        if (update == null) {
            log.error("Update is null");
            return;
        }
        if (update.getMessage() != null) {
            distributeMessagesByType(update);
        } else {
            log.error("Unsupported message type " + update);
        }
    }

    private void distributeMessagesByType(Update update) {
        var message = update.getMessage();
        if (message.getText() != null) {
            processTextMessage(update);
        } else if (message.getDocument() != null) {
            processDocumentFromMessage(update);
        } else {
            setUnsupportedMessageTypeView(update);
        }
    }

    private void setUnsupportedMessageTypeView(Update update) {
        var sendMessage = messageUtils.generateMessageWithText(update,
                "Unsupported type message");
        setView(sendMessage);
    }

    private void setView(SendMessage sendMessage) {
        telegramBot.sendAnswerMessage(sendMessage);
    }

    private void processDocumentFromMessage(Update update) {
        updateProducer.produce(DOCUMENT_UPDATE, update);
    }

    private void processTextMessage(Update update) {
        updateProducer.produce(TEXT_UPDATE, update);
    }
}
