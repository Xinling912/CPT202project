package com.cpt202.app.controller;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import java.util.Collections;
import com.cpt202.app.model.*;
import com.cpt202.app.repository.*;
import com.cpt202.app.security.JwtUtils;
import com.cpt202.app.service.SpecialistService;
import com.cpt202.app.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SpecialistController.class)
@AutoConfigureMockMvc(addFilters = false) // Bypass Security filters to focus on Controller logic
class SpecialistControllerTest {

    @Autowired
    private MockMvc mockMvc; // Core object to simulate HTTP requests
    @MockBean
    private JwtUtils jwtUtils;
    @MockBean
    private SpecialistService specialistService;
    @MockBean
    private UserService userService;
    @MockBean
    private SpecialistProfileRepository specialistRepository;
    @MockBean
    private TimeSlotRepository timeSlotRepository;
    @MockBean
    private ExpertiseCategoryRepository expertiseCategoryRepository;
    @MockBean
    private UserRepository userRepository;

    private User testUser;
    private SpecialistProfile testProfile;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(100L);
        testUser.setUsername("testSpecialist");
        testUser.setRole(UserRole.SPECIALIST);

        testProfile = new SpecialistProfile();
        testProfile.setId(10L);
        testProfile.setUser(testUser);
        testProfile.setStatus(SpecialistStatus.ACTIVE);
    }

    // ==========================================
    // 1. getSpecialists() - Specialist Hall Search
    // ==========================================
    @Test
    void getSpecialists_HappyPath_ShouldReturnPageData() throws Exception {
        // Arrange: Mock a paged result from the database
        Page<SpecialistProfile> mockPage = new PageImpl<>(List.of(testProfile));
        when(specialistRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        // Act & Assert: Simulate GET request with search parameters
        mockMvc.perform(get("/api/specialists")
                        .param("page", "0")
                        .param("size", "10")
                        .param("keyword", "test")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.page").value(0));
    }

    @Test
    void getSpecialists_InvalidDate_ShouldReturnBadRequest() throws Exception {
        // Act & Assert: Simulate an invalid date format
        mockMvc.perform(get("/api/specialists")
                        .param("date", "invalid-date"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("日期格式不正确，请输入 yyyy-MM-dd"));
    }

    // ==========================================
    // 2. getSpecialistDetail() - Specialist Detail
    // ==========================================
    @Test
    void getSpecialistDetail_Found_ShouldReturnProfile() throws Exception {
        when(specialistRepository.findById(10L)).thenReturn(Optional.of(testProfile));

        mockMvc.perform(get("/api/specialists/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void getSpecialistDetail_NotFound_ShouldReturn404() throws Exception {
        when(specialistRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/specialists/99"))
                .andExpect(status().isNotFound());
    }

    // ==========================================
    // 3. getFilterOptions() - Filter Dictionary
    // ==========================================
    @Test
    void getFilterOptions_ShouldReturnExpertisesAndLevels() throws Exception {
        ExpertiseCategory exp = new ExpertiseCategory();
        exp.setId(1L);
        exp.setName("Psychology");
        when(expertiseCategoryRepository.findAll()).thenReturn(List.of(exp));

        mockMvc.perform(get("/api/specialists/filters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expertises[0].name").value("Psychology"))
                .andExpect(jsonPath("$.levels").isArray());
    }

    // ==========================================
    // 4. submitProfile() - Submit/Edit Application (POST)
    // ==========================================
    @Test
    void submitProfile_HappyPath_ShouldReturnSuccessMessage() throws Exception {
        String requestJson = "{\"realName\":\"John Doe\",\"level\":\"EXPERT\",\"hourlyFee\":100.0,\"resume\":\"Test\",\"expertiseId\":1}";

        doNothing().when(specialistService).submitProfileApplication(anyString(), any());

        mockMvc.perform(post("/api/specialists/apply")
                        // 修正点：使用真正的 Authentication Token
                        .principal(new UsernamePasswordAuthenticationToken("testSpecialist", null, Collections.emptyList()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("个人信息已成功提交，请等待管理员审核。"));
    }

    @Test
    void submitProfile_BlankName_ShouldReturnBadRequest() throws Exception {
        String requestJson = "{\"realName\":\" \",\"level\":\"EXPERT\"}";

        mockMvc.perform(post("/api/specialists/apply")
                        // 修正点：同步修改
                        .principal(new UsernamePasswordAuthenticationToken("testSpecialist", null, Collections.emptyList()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("真实姓名不能为空"));
    }

    // ==========================================
    // 5. getTotalEarnings() - Specialist Earnings
    // ==========================================
    @Test
    void getTotalEarnings_AsSpecialist_ShouldReturnAmount() throws Exception {
        when(userRepository.findByUsername("testSpecialist")).thenReturn(Optional.of(testUser));
        when(specialistService.getTotalEarnings("testSpecialist")).thenReturn(new BigDecimal("998.50"));

        mockMvc.perform(get("/api/specialists/earnings")
                        // 使用真实的 Token 对象替换之前的 Lambda
                        .principal(new UsernamePasswordAuthenticationToken("testSpecialist", null, Collections.emptyList())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEarnings").value(998.50));
    }

    // ==========================================
    // 6. getMyProfile() - Get Logged-in Specialist Profile
    // ==========================================
    @Test
    void getMyProfile_AsCustomer_ShouldReturnForbidden() throws Exception {
        User customer = new User();
        customer.setUsername("normalUser");
        customer.setRole(UserRole.CUSTOMER);

        when(userRepository.findByUsername("normalUser")).thenReturn(Optional.of(customer));

        mockMvc.perform(get("/api/specialists/profile")
                        // 同样替换这里的 Token
                        .principal(new UsernamePasswordAuthenticationToken("normalUser", null, Collections.emptyList())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("只有专家可以查看自己的资料"));
    }

    // ==========================================
    // 7. getMyApplicationStatus() - Check Application Status
    // ==========================================
    @Test
    void getMyApplicationStatus_ShouldReturnStatusAndMessage() throws Exception {
        when(userService.getByUsername("testSpecialist")).thenReturn(testUser);
        when(specialistService.getCurrentApplicationStatus(testUser)).thenReturn(ApplicationStatus.APPLY_PENDING);

        mockMvc.perform(get("/api/specialists/apply-status")
                        // 修正点：同步修改
                        .principal(new UsernamePasswordAuthenticationToken("testSpecialist", null, Collections.emptyList())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPLY_PENDING"));
    }
}