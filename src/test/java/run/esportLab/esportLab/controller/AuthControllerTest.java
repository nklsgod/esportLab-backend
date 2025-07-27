package run.esportLab.esportLab.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import run.esportLab.esportLab.config.SecurityConfig;
import run.esportLab.esportLab.service.CustomOAuth2UserService;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CustomOAuth2UserService customOAuth2UserService;

    @Test
    void discordLogin_ShouldRedirectToOAuth2() throws Exception {
        mockMvc.perform(get("/auth/discord/login"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "/oauth2/authorization/discord"));
    }

    // Logout is tested separately as it's handled by Spring Security

    @Test
    void authStatus_WhenNotAuthenticated_ShouldReturnFalse() throws Exception {
        mockMvc.perform(get("/auth/status"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.authenticated").value(false));
    }

    @Test
    @WithMockUser(username = "testuser")
    void authStatus_WhenAuthenticated_ShouldReturnTrue() throws Exception {
        mockMvc.perform(get("/auth/status"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.principal").value("testuser"));
    }
}