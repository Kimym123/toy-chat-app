package org.example.back.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.example.back.dto.message.request.ChatMessageRequest;

public class ChatMessageValidator implements ConstraintValidator<ValidChatMessage, ChatMessageRequest> {
    
    @Override
    public boolean isValid(ChatMessageRequest request, ConstraintValidatorContext context) {
        if (request == null) {
            return false;
        }
        
        if (request.getType() == null) {
            addViolation(context, "메시지 타입은 필수입니다.");
            return false;
        }
        
        return switch (request.getType()) {
            case TEXT -> validateTextMessage(request, context);
            case IMAGE, FILE -> validateFileMessage(request, context);
            case SYSTEM -> {
                addViolation(context, "SYSTEM 타입은 클라이언트에서 전송할 수 없습니다.");
                yield false;
            }
        };
    }
    
    private boolean validateTextMessage(ChatMessageRequest request, ConstraintValidatorContext context) {
        if (request.getContent() == null || request.getContent().isBlank()) {
            addViolation(context, "텍스트 메시지의 내용은 비어 있을 수 없습니다.");
            return false;
        }
        return true;
    }
    
    private boolean validateFileMessage(ChatMessageRequest request, ConstraintValidatorContext context) {
        if (request.getFileUrl() == null || request.getFileUrl().isBlank()) {
            addViolation(context, "파일/이미지 메시지에는 파일 URL이 필수입니다.");
            return false;
        }
        return true;
    }
    
    private void addViolation(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }
}
