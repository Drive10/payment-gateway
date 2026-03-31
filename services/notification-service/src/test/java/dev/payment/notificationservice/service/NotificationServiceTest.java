package dev.payment.notificationservice.service;

import dev.payment.notificationservice.dto.*;
import dev.payment.notificationservice.entity.Notification;
import dev.payment.notificationservice.entity.NotificationChannel;
import dev.payment.notificationservice.entity.NotificationStatus;
import dev.payment.notificationservice.entity.NotificationType;
import dev.payment.notificationservice.entity.Template;
import dev.payment.notificationservice.repository.NotificationRepository;
import dev.payment.notificationservice.repository.TemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private TemplateRepository templateRepository;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(notificationRepository, templateRepository);
    }

    @Test
    void sendEmail_SavesAndReturnsNotification() {
        EmailRequest request = new EmailRequest();
        request.setTo("test@example.com");
        request.setSubject("Test Subject");
        request.setBody("Test body content");

        Notification savedNotification = new Notification();
        savedNotification.setId(UUID.randomUUID());
        savedNotification.setUserId(UUID.randomUUID());
        savedNotification.setType(NotificationType.PAYMENT_SUCCESS);
        savedNotification.setChannel(NotificationChannel.EMAIL);
        savedNotification.setStatus(NotificationStatus.SENT);
        savedNotification.setRecipient("test@example.com");
        savedNotification.setSubject("Test Subject");
        savedNotification.setContent("Test body content");
        savedNotification.setCreatedAt(LocalDateTime.now());
        savedNotification.setSentAt(LocalDateTime.now());

        when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);

        NotificationResponse response = notificationService.sendEmail(request);

        assertThat(response).isNotNull();
        assertThat(response.getRecipient()).isEqualTo("test@example.com");
        assertThat(response.getChannel()).isEqualTo(NotificationChannel.EMAIL);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(2)).save(captor.capture());
    }

    @Test
    void sendSms_SavesAndReturnsNotification() {
        SmsRequest request = new SmsRequest();
        request.setTo("+1234567890");
        request.setMessage("Test SMS message");

        Notification savedNotification = new Notification();
        savedNotification.setId(UUID.randomUUID());
        savedNotification.setUserId(UUID.randomUUID());
        savedNotification.setType(NotificationType.PAYMENT_SUCCESS);
        savedNotification.setChannel(NotificationChannel.SMS);
        savedNotification.setStatus(NotificationStatus.SENT);
        savedNotification.setRecipient("+1234567890");
        savedNotification.setContent("Test SMS message");
        savedNotification.setCreatedAt(LocalDateTime.now());
        savedNotification.setSentAt(LocalDateTime.now());

        when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);

        NotificationResponse response = notificationService.sendSms(request);

        assertThat(response).isNotNull();
        assertThat(response.getRecipient()).isEqualTo("+1234567890");
        assertThat(response.getChannel()).isEqualTo(NotificationChannel.SMS);
    }

    @Test
    void sendWebhook_SavesAndReturnsNotification() {
        WebhookRequest request = new WebhookRequest();
        request.setUrl("https://example.com/webhook");
        request.setEventType("payment.completed");
        request.setPayload("{\"transactionId\": \"123\"}");

        Notification savedNotification = new Notification();
        savedNotification.setId(UUID.randomUUID());
        savedNotification.setUserId(UUID.randomUUID());
        savedNotification.setType(NotificationType.PAYMENT_SUCCESS);
        savedNotification.setChannel(NotificationChannel.WEBHOOK);
        savedNotification.setStatus(NotificationStatus.SENT);
        savedNotification.setRecipient("https://example.com/webhook");
        savedNotification.setSubject("payment.completed");
        savedNotification.setEventType("payment.completed");
        savedNotification.setCreatedAt(LocalDateTime.now());
        savedNotification.setSentAt(LocalDateTime.now());

        when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);

        NotificationResponse response = notificationService.sendWebhook(request);

        assertThat(response).isNotNull();
        assertThat(response.getRecipient()).isEqualTo("https://example.com/webhook");
        assertThat(response.getChannel()).isEqualTo(NotificationChannel.WEBHOOK);
    }

    @Test
    void findById_WhenExists_ReturnsNotification() {
        UUID id = UUID.randomUUID();
        Notification notification = new Notification();
        notification.setId(id);
        notification.setUserId(UUID.randomUUID());
        notification.setType(NotificationType.PAYMENT_SUCCESS);
        notification.setChannel(NotificationChannel.EMAIL);
        notification.setStatus(NotificationStatus.SENT);
        notification.setRecipient("test@example.com");
        notification.setCreatedAt(LocalDateTime.now());

        when(notificationRepository.findById(id)).thenReturn(Optional.of(notification));

        Optional<NotificationResponse> result = notificationService.findById(id);

        assertThat(result).isPresent();
        assertThat(result.get().getRecipient()).isEqualTo("test@example.com");
    }

    @Test
    void findById_WhenNotExists_ReturnsEmpty() {
        UUID id = UUID.randomUUID();
        when(notificationRepository.findById(id)).thenReturn(Optional.empty());

        Optional<NotificationResponse> result = notificationService.findById(id);

        assertThat(result).isEmpty();
    }

    @Test
    void findAll_ReturnsAllNotifications() {
        List<Notification> notifications = List.of(
                Notification.builder().userId(UUID.randomUUID()).type(NotificationType.PAYMENT_SUCCESS)
                        .channel(NotificationChannel.EMAIL).status(NotificationStatus.SENT)
                        .recipient("test1@example.com").createdAt(LocalDateTime.now()).build(),
                Notification.builder().userId(UUID.randomUUID()).type(NotificationType.PAYMENT_FAILED)
                        .channel(NotificationChannel.SMS).status(NotificationStatus.FAILED)
                        .recipient("+1234567890").createdAt(LocalDateTime.now()).build()
        );

        when(notificationRepository.findAll()).thenReturn(notifications);

        List<NotificationResponse> result = notificationService.findAll(null, null, 0, 20);

        assertThat(result).hasSize(2);
    }

    @Test
    void getAllTemplates_ReturnsAllTemplates() {
        List<Template> templates = List.of(
                createTemplate("welcome-email", "Welcome Email"),
                createTemplate("payment-confirmation", "Payment Confirmation")
        );

        when(templateRepository.findAll()).thenReturn(templates);

        List<TemplateResponse> result = notificationService.getAllTemplates();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTemplateKey()).isEqualTo("welcome-email");
    }

    @Test
    void createTemplate_SavesAndReturnsTemplate() {
        CreateTemplateRequest request = new CreateTemplateRequest();
        request.setTemplateKey("new-template");
        request.setName("New Template");
        request.setChannel(NotificationChannel.EMAIL);
        request.setBodyTemplate("Hello {{name}}");
        request.setSubject("Welcome");

        Template savedTemplate = createTemplate("new-template", "New Template");

        when(templateRepository.save(any(Template.class))).thenReturn(savedTemplate);

        TemplateResponse result = notificationService.createTemplate(request);

        assertThat(result).isNotNull();
        assertThat(result.getTemplateKey()).isEqualTo("new-template");
        verify(templateRepository).save(any(Template.class));
    }

    @Test
    void retry_UpdatesStatusAndRetryCount() {
        UUID id = UUID.randomUUID();
        Notification notification = new Notification();
        notification.setId(id);
        notification.setUserId(UUID.randomUUID());
        notification.setType(NotificationType.PAYMENT_SUCCESS);
        notification.setChannel(NotificationChannel.EMAIL);
        notification.setStatus(NotificationStatus.FAILED);
        notification.setRecipient("test@example.com");
        notification.setRetryCount(0);
        notification.setCreatedAt(LocalDateTime.now());

        when(notificationRepository.findById(id)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification n = invocation.getArgument(0);
            return n;
        });

        Optional<NotificationResponse> result = notificationService.retry(id);

        assertThat(result).isPresent();
        verify(notificationRepository, times(2)).save(any(Notification.class));
    }

    private Template createTemplate(String key, String name) {
        Template template = new Template();
        template.setTemplateKey(key);
        template.setName(name);
        template.setChannel(NotificationChannel.EMAIL);
        template.setActive(true);
        return template;
    }
}
