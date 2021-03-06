package com.societegenerale.cidroid.tasks.consumer.infrastructure.notifiers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.societegenerale.cidroid.tasks.consumer.services.model.Message;
import com.societegenerale.cidroid.tasks.consumer.services.model.github.PullRequest;
import com.societegenerale.cidroid.tasks.consumer.services.model.github.User;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.util.*;

import static com.societegenerale.cidroid.tasks.consumer.services.notifiers.Notifier.PULL_REQUEST;
import static org.assertj.core.api.Assertions.assertThat;

public class EMailNotifierTest {

    private static final String MAIL_SENT_FROM = "ci-droid@socgen.com";

    private EMailNotifier emailNotifier;

    private GreenMail mailServer = new GreenMail(ServerSetupTest.SMTP);

    @BeforeEach
    public void setUp() {

        emailNotifier = buildAndConfigure();

        mailServer.start();
    }

    private EMailNotifier buildAndConfigure() {

        JavaMailSenderImpl mailSenderImpl = new JavaMailSenderImpl();
        Properties properties = new Properties();
        properties.put("mail.smtp.auth", "false");
        properties.put("mail.smtp.starttls.enable", "false");

        mailSenderImpl.setJavaMailProperties(properties);
        mailSenderImpl.setHost("localhost");
        mailSenderImpl.setPort(3025);
        mailSenderImpl.setProtocol("smtp");

        return new EMailNotifier(mailSenderImpl, MAIL_SENT_FROM);
    }

    @AfterEach
    public void stopGreenMailServer() {
        mailServer.stop();
    }

    @Test
    public void shouldSendExpectedEmail() throws MessagingException, IOException {

        String recipientEmail = "test_login@myDomain.com";
        User user = User.builder().login("test_login")
                .email(recipientEmail)
                .build();

        String messageContent = "PR is not mergeable anymore";

        Message msg = new Message(messageContent);

        PullRequest pr = new ObjectMapper().readValue(
                getClass().getClassLoader().getResourceAsStream("singlePullRequest.json"),
                PullRequest.class);

        Map<String, Object> additionalInfos = new HashMap<>();
        additionalInfos.put(PULL_REQUEST, pr);

        emailNotifier.notify(user, msg, additionalInfos);

        List<MimeMessage> actualSentEmailsFromServer = Arrays.asList(mailServer.getReceivedMessages());

        assertThat(actualSentEmailsFromServer).hasSize(1);

        MimeMessage actualEmail = actualSentEmailsFromServer.get(0);

        assertThat(actualEmail.getFrom()[0].toString()).isEqualTo(MAIL_SENT_FROM);

        assertThat(actualEmail.getSubject()).isEqualTo("Pull Request Not Mergeable");

        assertThat(actualEmail.getAllRecipients()).hasSize(1);
        assertThat(actualEmail.getAllRecipients()[0].toString()).isEqualTo(recipientEmail);

        assertThat(actualEmail.getContent().toString()).containsOnlyOnce(messageContent);
    }

}