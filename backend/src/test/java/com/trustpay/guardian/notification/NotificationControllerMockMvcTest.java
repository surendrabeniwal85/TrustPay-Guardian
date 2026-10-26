package com.trustpay.guardian.notification;

import com.trustpay.guardian.security.JwtService;
import com.trustpay.guardian.security.UserPrincipal;
import com.trustpay.guardian.user.Role;
import com.trustpay.guardian.user.User;
import com.trustpay.guardian.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NotificationControllerMockMvcTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private JwtService jwtService;

    private MockMvc mockMvc;
    private User user;
    private String token;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        user = userRepository.save(new User("Notify User", "notify@test.com", "9199999999", "hash", Role.CUSTOMER));
        token = "Bearer " + jwtService.generateAccessToken(UserPrincipal.create(user));
    }

    @Test
    @DisplayName("Notification list, unread count, read single, and read all endpoints")
    void notificationEndpoints() throws Exception {
        Notification n = notificationRepository.save(new Notification(
                user, NotificationType.TRANSFER_RECEIVED, "Credit", "₹500 received"));

        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(1));

        mockMvc.perform(patch("/api/v1/notifications/" + n.getId() + "/read")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Notification marked as read"));

        mockMvc.perform(patch("/api/v1/notifications/read-all")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("All notifications marked as read"));
    }
}
