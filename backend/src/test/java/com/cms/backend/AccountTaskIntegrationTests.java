package com.cms.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AccountTaskIntegrationTests {
    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void userCanLoginWithPhoneNumberAndPassword() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phoneNumber":"13800000000","password":"admin123456"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.account.phoneNumber").value("13800000000"))
                .andExpect(jsonPath("$.account.roles[0]").value("SALES_MEMBER"))
                .andExpect(jsonPath("$.account.roles[1]").value("MANAGER"));
    }

    @Test
    void managerCanCreateMemberAndDuplicatePhoneNumbersAreRejected() throws Exception {
        String token = login("13800000000", "admin123456");

        mvc.perform(post("/api/manager/members")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"张三","phoneNumber":"13900000000","initialPassword":"member123","salesMember":true,"manager":false}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("张三"))
                .andExpect(jsonPath("$.phoneNumber").value("13900000000"))
                .andExpect(jsonPath("$.roles[0]").value("SALES_MEMBER"));

        mvc.perform(post("/api/manager/members")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"李四","phoneNumber":"13900000000","initialPassword":"member123","salesMember":true,"manager":false}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("手机号已存在"));
    }

    @Test
    void managerCanResetPasswordAndUserCanChangeOwnPassword() throws Exception {
        String managerToken = login("13800000000", "admin123456");
        String memberId = createMember(managerToken, "14000000000");

        mvc.perform(post("/api/manager/members/" + memberId + "/reset-password")
                        .header("Authorization", bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"newPassword":"reset123"}
                                """))
                .andExpect(status().isNoContent());

        String memberToken = login("14000000000", "reset123");
        mvc.perform(post("/api/auth/password")
                        .header("Authorization", bearer(memberToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"reset123","newPassword":"changed123"}
                                """))
                .andExpect(status().isNoContent());

        login("14000000000", "changed123");
    }

    @Test
    void inactiveAccountCannotLoginAndSessionCanLogout() throws Exception {
        String managerToken = login("13800000000", "admin123456");
        String memberId = createMember(managerToken, "14100000000");
        String memberToken = login("14100000000", "member123");

        mvc.perform(delete("/api/auth/session")
                        .header("Authorization", bearer(memberToken)))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/auth/me")
                        .header("Authorization", bearer(memberToken)))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/manager/members/" + memberId + "/deactivate")
                        .header("Authorization", bearer(managerToken)))
                .andExpect(status().isNoContent());

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phoneNumber":"14100000000","password":"member123"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("手机号或密码错误"));
    }

    @Test
    void managerCanModifyPhoneAndRolesButMustKeepPhoneUnique() throws Exception {
        String managerToken = login("13800000000", "admin123456");
        String firstId = createMember(managerToken, "14200000000");
        createMember(managerToken, "14300000000");

        mvc.perform(patch("/api/manager/members/" + firstId)
                        .header("Authorization", bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"张三新","phoneNumber":"14300000000","salesMember":true,"manager":true}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("手机号已存在"));

        mvc.perform(patch("/api/manager/members/" + firstId)
                        .header("Authorization", bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"张三新","phoneNumber":"14200000001","salesMember":true,"manager":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("张三新"))
                .andExpect(jsonPath("$.phoneNumber").value("14200000001"))
                .andExpect(jsonPath("$.roles[0]").value("SALES_MEMBER"))
                .andExpect(jsonPath("$.roles[1]").value("MANAGER"));
    }

    @Test
    void salesMemberCannotManageMembers() throws Exception {
        String managerToken = login("13800000000", "admin123456");
        createMember(managerToken, "14400000000");
        String memberToken = login("14400000000", "member123");

        mvc.perform(get("/api/manager/members")
                        .header("Authorization", bearer(memberToken)))
                .andExpect(status().isForbidden());
    }

    private String createMember(String managerToken, String phoneNumber) throws Exception {
        String response = mvc.perform(post("/api/manager/members")
                        .header("Authorization", bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"成员","phoneNumber":"%s","initialPassword":"member123","salesMember":true,"manager":false}
                                """.formatted(phoneNumber)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode json = objectMapper.readTree(response);
        assertThat(json.get("id").asText()).isNotBlank();
        return json.get("id").asText();
    }

    private String login(String phoneNumber, String password) throws Exception {
        String response = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phoneNumber":"%s","password":"%s"}
                                """.formatted(phoneNumber, password)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("token").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
