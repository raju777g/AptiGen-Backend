package com.aptigen.support;

import com.aptigen.storage.FileStorageService;
import com.aptigen.storage.StoredFile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SupportChatService {

    private final SupportChatRepository chatRepository;
    private final SupportMessageRepository messageRepository;
    private final FileStorageService fileStorageService;

    private static final long MAX_ATTACHMENT_BYTES = 20 * 1024 * 1024; // 20MB — generous for screenshots/short clips

    @Transactional
    public SupportChat startChat(Long userId, String subject, String firstMessage) {
        SupportChat chat = chatRepository.save(SupportChat.builder()
                .userId(userId)
                .subject(subject)
                .build());

        messageRepository.save(SupportMessage.builder()
                .chatId(chat.getId())
                .senderType(SenderType.USER)
                .message(firstMessage)
                .build());

        return chat;
    }

    public List<SupportChat> getUserChats(Long userId) {
        return chatRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<SupportMessage> getMessages(Long chatId, Long userId) {
        chatRepository.findByIdAndUserId(chatId, userId)
                .orElseThrow(() -> new IllegalStateException("Chat not found"));
        return messageRepository.findByChatIdOrderByCreatedAtAsc(chatId);
    }

    @Transactional
    public SupportMessage sendMessage(Long chatId, Long userId, String text, MultipartFile attachment) throws IOException {
        SupportChat chat = chatRepository.findByIdAndUserId(chatId, userId)
                .orElseThrow(() -> new IllegalStateException("Chat not found"));

        String attachmentUrl = null;
        String attachmentType = null;

        if (attachment != null && !attachment.isEmpty()) {
            if (attachment.getSize() > MAX_ATTACHMENT_BYTES) {
                throw new IllegalArgumentException("Attachment must be under 20MB");
            }
            String contentType = attachment.getContentType();
            if (contentType == null) throw new IllegalArgumentException("Unknown file type");

            if (contentType.startsWith("image/")) attachmentType = "image";
            else if (contentType.startsWith("video/")) attachmentType = "video";
            else if (contentType.startsWith("audio/")) attachmentType = "audio";
            else attachmentType = "file";

            StoredFile storedFile = fileStorageService.store(attachment);
            attachmentUrl = "/api/files/" + storedFile.getId();
        }

        return messageRepository.save(SupportMessage.builder()
                .chatId(chatId)
                .senderType(SenderType.USER)
                .message(text)
                .attachmentUrl(attachmentUrl)
                .attachmentType(attachmentType)
                .build());
    }
}
