package com.cms.backend;

import static org.assertj.core.api.Assertions.assertThat;
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
class CustomerTaskIntegrationTests {
    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void salesMemberCanCreateProspectiveCustomerWithNameOnly() throws Exception {
        String managerToken = login("13800000000", "admin123456");
        Member member = createSalesMember(managerToken, "15000000000");
        String memberToken = login("15000000000", "member123");

        mvc.perform(post("/api/sales/customers")
                        .header("Authorization", bearer(memberToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"华东医械供应链"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("华东医械供应链"))
                .andExpect(jsonPath("$.status").value("PROSPECTIVE_CUSTOMER"))
                .andExpect(jsonPath("$.attentionLevel").value(1))
                .andExpect(jsonPath("$.owner.id").value(member.id()))
                .andExpect(jsonPath("$.owner.name").value("销售成员15000000000"));
    }

    @Test
    void salesMemberCanListOnlyTheirOwnCustomers() throws Exception {
        String managerToken = login("13800000000", "admin123456");
        createSalesMember(managerToken, "15100000000");
        createSalesMember(managerToken, "15200000000");
        String firstMemberToken = login("15100000000", "member123");
        String secondMemberToken = login("15200000000", "member123");

        createProspectiveCustomer(firstMemberToken, "第一成员客户");
        createProspectiveCustomer(secondMemberToken, "第二成员客户");

        mvc.perform(get("/api/sales/customers")
                        .header("Authorization", bearer(firstMemberToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("第一成员客户"))
                .andExpect(jsonPath("$[1]").doesNotExist());
    }

    @Test
    void salesMemberCanUpdateAttentionLevelForOwnCustomer() throws Exception {
        String managerToken = login("13800000000", "admin123456");
        createSalesMember(managerToken, "15300000000");
        String memberToken = login("15300000000", "member123");
        String customerId = createProspectiveCustomer(memberToken, "可调整关注客户");

        mvc.perform(patch("/api/sales/customers/" + customerId + "/attention-level")
                        .header("Authorization", bearer(memberToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"attentionLevel":4}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(customerId))
                .andExpect(jsonPath("$.attentionLevel").value(4));
    }

    @Test
    void myCustomersAreSortedByAttentionLevelDescending() throws Exception {
        String managerToken = login("13800000000", "admin123456");
        createSalesMember(managerToken, "15400000000");
        String memberToken = login("15400000000", "member123");
        String lowId = createProspectiveCustomer(memberToken, "低关注客户");
        String highId = createProspectiveCustomer(memberToken, "高关注客户");
        String middleId = createProspectiveCustomer(memberToken, "中关注客户");

        updateAttentionLevel(memberToken, lowId, 2);
        updateAttentionLevel(memberToken, highId, 5);
        updateAttentionLevel(memberToken, middleId, 4);

        mvc.perform(get("/api/sales/customers")
                        .header("Authorization", bearer(memberToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("高关注客户"))
                .andExpect(jsonPath("$[1].name").value("中关注客户"))
                .andExpect(jsonPath("$[2].name").value("低关注客户"));
    }

    @Test
    void salesMemberCanSearchMyCustomersByName() throws Exception {
        String managerToken = login("13800000000", "admin123456");
        createSalesMember(managerToken, "15500000000");
        String memberToken = login("15500000000", "member123");
        createProspectiveCustomer(memberToken, "华东医械供应链-搜索");
        createProspectiveCustomer(memberToken, "青竹康养中心");

        mvc.perform(get("/api/sales/customers")
                        .queryParam("name", "华东")
                        .header("Authorization", bearer(memberToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("华东医械供应链-搜索"))
                .andExpect(jsonPath("$[1]").doesNotExist());
    }

    @Test
    void salesMemberCanViewOwnCustomerDetailWithoutOwnerField() throws Exception {
        String managerToken = login("13800000000", "admin123456");
        createSalesMember(managerToken, "15600000000");
        String memberToken = login("15600000000", "member123");
        String customerId = createProspectiveCustomer(memberToken, "详情页客户");

        mvc.perform(get("/api/sales/customers/" + customerId)
                        .header("Authorization", bearer(memberToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(customerId))
                .andExpect(jsonPath("$.name").value("详情页客户"))
                .andExpect(jsonPath("$.status").value("PROSPECTIVE_CUSTOMER"))
                .andExpect(jsonPath("$.attentionLevel").value(1))
                .andExpect(jsonPath("$.owner").doesNotExist())
                .andExpect(jsonPath("$.futurePlannedContacts").isArray())
                .andExpect(jsonPath("$.futurePlannedContacts[0]").doesNotExist())
                .andExpect(jsonPath("$.customerContacts").isArray())
                .andExpect(jsonPath("$.customerContacts[0]").doesNotExist());
    }

    @Test
    void duplicateCustomerNamesAreRejectedGlobally() throws Exception {
        String managerToken = login("13800000000", "admin123456");
        createSalesMember(managerToken, "15700000000");
        createSalesMember(managerToken, "15800000000");
        String firstMemberToken = login("15700000000", "member123");
        String secondMemberToken = login("15800000000", "member123");
        createProspectiveCustomer(firstMemberToken, "全局唯一客户名称");

        mvc.perform(post("/api/sales/customers")
                        .header("Authorization", bearer(secondMemberToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"全局唯一客户名称"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("客户名称已存在"));
    }

    private Member createSalesMember(String managerToken, String phoneNumber) throws Exception {
        String response = mvc.perform(post("/api/manager/members")
                        .header("Authorization", bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"销售成员%s","phoneNumber":"%s","initialPassword":"member123","salesMember":true,"manager":false}
                                """.formatted(phoneNumber, phoneNumber)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode json = objectMapper.readTree(response);
        assertThat(json.get("id").asText()).isNotBlank();
        return new Member(json.get("id").asText());
    }

    private String createProspectiveCustomer(String memberToken, String name) throws Exception {
        String response = mvc.perform(post("/api/sales/customers")
                        .header("Authorization", bearer(memberToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s"}
                                """.formatted(name)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode json = objectMapper.readTree(response);
        assertThat(json.get("id").asText()).isNotBlank();
        return json.get("id").asText();
    }

    private void updateAttentionLevel(String memberToken, String customerId, int attentionLevel) throws Exception {
        mvc.perform(patch("/api/sales/customers/" + customerId + "/attention-level")
                        .header("Authorization", bearer(memberToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"attentionLevel":%d}
                                """.formatted(attentionLevel)))
                .andExpect(status().isOk());
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

    private record Member(String id) {
    }
}
