package com.searchstax.aem.connector.core.services;

import com.searchstax.aem.connector.core.config.model.EmailConfig;
import com.searchstax.aem.connector.core.dto.request.EmailRequest;

public interface EmailService {

    boolean sendEmail(EmailRequest request);

    String sendEmailOrError(EmailRequest request);

    String sendEmailOrError(EmailRequest request, EmailConfig config);
}
