package com.cms.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cms.backend.activity.PlannedContactRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class PlannedContactTaskIntegrationTests {
    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    PlannedContactRepository plannedContacts;

    @Test
    void salesMemberCanCreateFuturePlannedContactForOwnedCustomer() throws Exception {
        String managerToken = login("13800000000", "admin123456");
        createSalesMember(managerToken, "17000000000");
        String memberToken = login("17000000000", "member123");
        String customerId = createProspectiveCustomer(memberToken, "未来计划客户");
        String plannedDate = LocalDate.now().plusDays(7).toString();

        String response = mvc.perform(post("/api/sales/planned-contacts")
                        .header("Authorization", bearer(memberToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerId":"%s","plannedDate":"%s"}
                                """.formatted(customerId, plannedDate)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").value(customerId))
                .andExpect(jsonPath("$.customerName").value("未来计划客户"))
                .andExpect(jsonPath("$.plannedDate").value(plannedDate))
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(objectMapper.readTree(response).get("id").asText()).isNotBlank();

        mvc.perform(get("/api/sales/customers/" + customerId)
                        .header("Authorization", bearer(memberToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.futurePlannedContacts[0].customerId").value(customerId))
                .andExpect(jsonPath("$.futurePlannedContacts[0].customerName").value("未来计划客户"))
                .andExpect(jsonPath("$.futurePlannedContacts[0].plannedDate").value(plannedDate));
    }

    @Test
    void salesMemberCanChangeFuturePlannedContactDate() throws Exception {
        String managerToken = login("13800000000", "admin123456");
        createSalesMember(managerToken, "17100000000");
        String memberToken = login("17100000000", "member123");
        String customerId = createProspectiveCustomer(memberToken, "修改计划客户");
        String plannedContactId = createPlannedContact(memberToken, customerId, LocalDate.now().plusDays(5));
        String newPlannedDate = LocalDate.now().plusDays(10).toString();

        mvc.perform(patch("/api/sales/planned-contacts/" + plannedContactId)
                        .header("Authorization", bearer(memberToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"plannedDate":"%s"}
                                """.formatted(newPlannedDate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(plannedContactId))
                .andExpect(jsonPath("$.customerId").value(customerId))
                .andExpect(jsonPath("$.plannedDate").value(newPlannedDate));

        mvc.perform(get("/api/sales/customers/" + customerId)
                        .header("Authorization", bearer(memberToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.futurePlannedContacts[0].id").value(plannedContactId))
                .andExpect(jsonPath("$.futurePlannedContacts[0].plannedDate").value(newPlannedDate));
    }

    @Test
    void salesMemberCanDeleteFuturePlannedContact() throws Exception {
        String managerToken = login("13800000000", "admin123456");
        createSalesMember(managerToken, "17200000000");
        String memberToken = login("17200000000", "member123");
        String customerId = createProspectiveCustomer(memberToken, "删除计划客户");
        String plannedContactId = createPlannedContact(memberToken, customerId, LocalDate.now().plusDays(4));

        mvc.perform(delete("/api/sales/planned-contacts/" + plannedContactId)
                        .header("Authorization", bearer(memberToken)))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/sales/customers/" + customerId)
                        .header("Authorization", bearer(memberToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.futurePlannedContacts[0]").doesNotExist());
    }

    @Test
    void managerCustomerDetailShowsFuturePlannedContactsInDateOrder() throws Exception {
        String managerToken = login("13800000000", "admin123456");
        createSalesMember(managerToken, "17300000000");
        String memberToken = login("17300000000", "member123");
        String customerId = createProspectiveCustomer(memberToken, "管理端计划客户");
        String laterId = createPlannedContact(memberToken, customerId, LocalDate.now().plusDays(12));
        String earlierId = createPlannedContact(memberToken, customerId, LocalDate.now().plusDays(3));

        mvc.perform(get("/api/manager/customers/" + customerId)
                        .header("Authorization", bearer(managerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.futurePlannedContacts[0].id").value(earlierId))
                .andExpect(jsonPath("$.futurePlannedContacts[1].id").value(laterId))
                .andExpect(jsonPath("$.futurePlannedContacts[2]").doesNotExist());
    }

    @Test
    void salesMemberCannotCreatePlannedContactForUnownedInactiveNonFutureOrDuplicateCustomerDate() throws Exception {
        String managerToken = login("13800000000", "admin123456");
        createSalesMember(managerToken, "17400000000");
        createSalesMember(managerToken, "17500000000");
        String firstMemberToken = login("17400000000", "member123");
        String secondMemberToken = login("17500000000", "member123");
        String firstCustomerId = createProspectiveCustomer(firstMemberToken, "计划约束客户");
        String inactiveCustomerId = createProspectiveCustomer(firstMemberToken, "停用计划约束客户");
        String plannedDate = LocalDate.now().plusDays(6).toString();
        deactivateCustomer(managerToken, inactiveCustomerId);

        mvc.perform(post("/api/sales/planned-contacts")
                        .header("Authorization", bearer(secondMemberToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerId":"%s","plannedDate":"%s"}
                                """.formatted(firstCustomerId, plannedDate)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("客户不存在"));

        mvc.perform(post("/api/sales/planned-contacts")
                        .header("Authorization", bearer(firstMemberToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerId":"%s","plannedDate":"%s"}
                                """.formatted(inactiveCustomerId, plannedDate)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("停用客户不能创建计划接触"));

        mvc.perform(post("/api/sales/planned-contacts")
                        .header("Authorization", bearer(firstMemberToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerId":"%s","plannedDate":"%s"}
                                """.formatted(firstCustomerId, LocalDate.now())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("计划日期必须是未来日期"));

        createPlannedContact(firstMemberToken, firstCustomerId, LocalDate.parse(plannedDate));

        mvc.perform(post("/api/sales/planned-contacts")
                        .header("Authorization", bearer(firstMemberToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerId":"%s","plannedDate":"%s"}
                                """.formatted(firstCustomerId, plannedDate)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("同一客户同一天已有计划接触"));
    }

    @Test
    void salesMemberCannotMovePlannedContactOntoDuplicateCustomerDate() throws Exception {
        String managerToken = login("13800000000", "admin123456");
        createSalesMember(managerToken, "17600000000");
        String memberToken = login("17600000000", "member123");
        String customerId = createProspectiveCustomer(memberToken, "修改重复日期客户");
        String firstPlannedContactId = createPlannedContact(memberToken, customerId, LocalDate.now().plusDays(2));
        String duplicateDate = LocalDate.now().plusDays(8).toString();
        createPlannedContact(memberToken, customerId, LocalDate.parse(duplicateDate));

        mvc.perform(patch("/api/sales/planned-contacts/" + firstPlannedContactId)
                        .header("Authorization", bearer(memberToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"plannedDate":"%s"}
                                """.formatted(duplicateDate)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("同一客户同一天已有计划接触"));
    }

    @Test
    void salesMemberCannotDeleteTodayPlannedContact() throws Exception {
        String managerToken = login("13800000000", "admin123456");
        createSalesMember(managerToken, "17700000000");
        String memberToken = login("17700000000", "member123");
        String customerId = createProspectiveCustomer(memberToken, "今日计划客户");
        String plannedContactId = createPlannedContact(memberToken, customerId, LocalDate.now().plusDays(1));
        var plannedContact = plannedContacts.findById(plannedContactId).orElseThrow();
        plannedContact.changePlannedDate(LocalDate.now());
        plannedContacts.saveAndFlush(plannedContact);

        mvc.perform(delete("/api/sales/planned-contacts/" + plannedContactId)
                        .header("Authorization", bearer(memberToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("今天和过去的计划不能手动删除"));
    }

    private void createSalesMember(String managerToken, String phoneNumber) throws Exception {
        mvc.perform(post("/api/manager/members")
                        .header("Authorization", bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"销售成员%s","phoneNumber":"%s","initialPassword":"member123","salesMember":true,"manager":false}
                                """.formatted(phoneNumber, phoneNumber)))
                .andExpect(status().isCreated());
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

    private void deactivateCustomer(String managerToken, String customerId) throws Exception {
        mvc.perform(post("/api/manager/customers/" + customerId + "/deactivate")
                        .header("Authorization", bearer(managerToken)))
                .andExpect(status().isOk());
    }

    private String createPlannedContact(String memberToken, String customerId, LocalDate plannedDate) throws Exception {
        String response = mvc.perform(post("/api/sales/planned-contacts")
                        .header("Authorization", bearer(memberToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerId":"%s","plannedDate":"%s"}
                                """.formatted(customerId, plannedDate)))
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
