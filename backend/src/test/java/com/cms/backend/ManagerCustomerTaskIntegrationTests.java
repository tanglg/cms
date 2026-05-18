package com.cms.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
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
class ManagerCustomerTaskIntegrationTests {
    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void managerCanCreateCustomersForOwnersAndListWholeTeam() throws Exception {
        String managerToken = login("13800000000", "admin123456");
        Member firstOwner = createSalesMember(managerToken, "16000000000");
        Member secondOwner = createSalesMember(managerToken, "16100000000");

        createManagerCustomer(managerToken, "管理端潜在客户", "PROSPECTIVE_CUSTOMER", firstOwner.id(), null);
        createManagerCustomer(managerToken, "管理端正式客户", "FORMAL_CUSTOMER", secondOwner.id(), "2026-05-18");

        mvc.perform(get("/api/manager/customers")
                        .queryParam("name", "管理端")
                        .header("Authorization", bearer(managerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("管理端潜在客户", "管理端正式客户")))
                .andExpect(jsonPath("$[?(@.name=='管理端潜在客户')].status", hasItem("PROSPECTIVE_CUSTOMER")))
                .andExpect(jsonPath("$[?(@.name=='管理端潜在客户')].owner.id", hasItem(firstOwner.id())))
                .andExpect(jsonPath("$[?(@.name=='管理端正式客户')].status", hasItem("FORMAL_CUSTOMER")))
                .andExpect(jsonPath("$[?(@.name=='管理端正式客户')].agreementSigningDate", hasItem("2026-05-18")))
                .andExpect(jsonPath("$[?(@.name=='管理端正式客户')].owner.id", hasItem(secondOwner.id())));
    }

    @Test
    void managerCanFilterSearchAndSortCustomersByAttentionLevel() throws Exception {
        String managerToken = login("13800000000", "admin123456");
        Member firstOwner = createSalesMember(managerToken, "16200000000");
        Member secondOwner = createSalesMember(managerToken, "16300000000");
        String highAttentionId = createManagerCustomer(managerToken, "筛选高关注客户", "PROSPECTIVE_CUSTOMER", firstOwner.id(), null);
        String lowAttentionId = createManagerCustomer(managerToken, "筛选低关注客户", "FORMAL_CUSTOMER", secondOwner.id(), "2026-05-18");

        updateManagerCustomer(managerToken, highAttentionId, "筛选高关注客户", firstOwner.id(), 5);
        updateManagerCustomer(managerToken, lowAttentionId, "筛选低关注客户", secondOwner.id(), 2);

        mvc.perform(get("/api/manager/customers")
                        .queryParam("ownerId", firstOwner.id())
                        .queryParam("status", "PROSPECTIVE_CUSTOMER")
                        .queryParam("attentionLevel", "5")
                        .queryParam("name", "高关注")
                        .header("Authorization", bearer(managerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(highAttentionId))
                .andExpect(jsonPath("$[1]").doesNotExist());

        mvc.perform(get("/api/manager/customers")
                        .queryParam("name", "筛选")
                        .header("Authorization", bearer(managerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(highAttentionId))
                .andExpect(jsonPath("$[0].attentionLevel").value(5))
                .andExpect(jsonPath("$[1].id").value(lowAttentionId))
                .andExpect(jsonPath("$[1].attentionLevel").value(2));
    }

    @Test
    void managerCanRenameChangeOwnerAndAttentionLevelWithUniqueCustomerNames() throws Exception {
        String managerToken = login("13800000000", "admin123456");
        Member firstOwner = createSalesMember(managerToken, "16400000000");
        Member secondOwner = createSalesMember(managerToken, "16500000000");
        String customerId = createManagerCustomer(managerToken, "可治理客户", "PROSPECTIVE_CUSTOMER", firstOwner.id(), null);
        createManagerCustomer(managerToken, "已占用客户名称", "PROSPECTIVE_CUSTOMER", firstOwner.id(), null);

        mvc.perform(patch("/api/manager/customers/" + customerId)
                        .header("Authorization", bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"已占用客户名称","ownerId":"%s","attentionLevel":4}
                                """.formatted(secondOwner.id())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("客户名称已存在"));

        mvc.perform(patch("/api/manager/customers/" + customerId)
                        .header("Authorization", bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"已治理客户","ownerId":"%s","attentionLevel":4}
                                """.formatted(secondOwner.id())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(customerId))
                .andExpect(jsonPath("$.name").value("已治理客户"))
                .andExpect(jsonPath("$.owner.id").value(secondOwner.id()))
                .andExpect(jsonPath("$.attentionLevel").value(4));
    }

    @Test
    void managerCanDeactivateAndRestoreCustomersToPreviousStatus() throws Exception {
        String managerToken = login("13800000000", "admin123456");
        Member owner = createSalesMember(managerToken, "16600000000");
        String customerId = createManagerCustomer(managerToken, "可停用正式客户", "FORMAL_CUSTOMER", owner.id(), "2026-05-18");

        mvc.perform(post("/api/manager/customers/" + customerId + "/deactivate")
                        .header("Authorization", bearer(managerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(customerId))
                .andExpect(jsonPath("$.status").value("INACTIVE_CUSTOMER"))
                .andExpect(jsonPath("$.owner.id").value(owner.id()));

        mvc.perform(post("/api/manager/customers/" + customerId + "/restore")
                        .header("Authorization", bearer(managerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(customerId))
                .andExpect(jsonPath("$.status").value("FORMAL_CUSTOMER"))
                .andExpect(jsonPath("$.owner.id").value(owner.id()))
                .andExpect(jsonPath("$.agreementSigningDate").value("2026-05-18"));
    }

    @Test
    void managerCustomerDetailShowsGovernanceFieldsAndFormalCustomerRequiresAgreementSigningDate() throws Exception {
        String managerToken = login("13800000000", "admin123456");
        Member owner = createSalesMember(managerToken, "16700000000");
        String customerId = createManagerCustomer(managerToken, "详情正式客户", "FORMAL_CUSTOMER", owner.id(), "2026-05-18");

        mvc.perform(post("/api/manager/customers")
                        .header("Authorization", bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"缺日期正式客户","status":"FORMAL_CUSTOMER","ownerId":"%s"}
                                """.formatted(owner.id())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("正式客户必须填写协议签署日期"));

        mvc.perform(get("/api/manager/customers/" + customerId)
                        .header("Authorization", bearer(managerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(customerId))
                .andExpect(jsonPath("$.name").value("详情正式客户"))
                .andExpect(jsonPath("$.status").value("FORMAL_CUSTOMER"))
                .andExpect(jsonPath("$.owner.id").value(owner.id()))
                .andExpect(jsonPath("$.attentionLevel").value(1))
                .andExpect(jsonPath("$.agreementSigningDate").value("2026-05-18"))
                .andExpect(jsonPath("$.futurePlannedContacts").isArray())
                .andExpect(jsonPath("$.customerContacts").isArray());
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

    private String createManagerCustomer(
            String managerToken,
            String name,
            String status,
            String ownerId,
            String agreementSigningDate) throws Exception {
        String agreementField = agreementSigningDate == null
                ? ""
                : """
                        ,"agreementSigningDate":"%s"
                        """.formatted(agreementSigningDate);
        String response = mvc.perform(post("/api/manager/customers")
                        .header("Authorization", bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","status":"%s","ownerId":"%s"%s}
                                """.formatted(name, status, ownerId, agreementField)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode json = objectMapper.readTree(response);
        assertThat(json.get("id").asText()).isNotBlank();
        return json.get("id").asText();
    }

    private void updateManagerCustomer(
            String managerToken,
            String customerId,
            String name,
            String ownerId,
            int attentionLevel) throws Exception {
        mvc.perform(patch("/api/manager/customers/" + customerId)
                        .header("Authorization", bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","ownerId":"%s","attentionLevel":%d}
                                """.formatted(name, ownerId, attentionLevel)))
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
