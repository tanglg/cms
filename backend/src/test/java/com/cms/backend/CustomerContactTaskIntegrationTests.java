package com.cms.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cms.backend.activity.CustomerContactRepository;
import com.cms.backend.activity.PlannedContactRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class CustomerContactTaskIntegrationTests {
    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    PlannedContactRepository plannedContacts;

    @Autowired
    CustomerContactRepository customerContacts;

    @Test
    void salesMemberCanSubmitImmutableCustomerContactForOwnedActiveCustomer() throws Exception {
        String managerToken = login("13800000000", "admin123456");
        createSalesMember(managerToken, "19000000000");
        String memberToken = login("19000000000", "member123");
        String customerId = createProspectiveCustomer(memberToken, "可登记接触客户");
        createTodayPlannedContact(memberToken, customerId);

        Instant beforeSubmit = Instant.now();
        String response = mvc.perform(post("/api/sales/customer-contacts")
                        .header("Authorization", bearer(memberToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerId":"%s","communicationSummary":"电话联系王经理，确认演示时间。"}
                                """.formatted(customerId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").value(customerId))
                .andExpect(jsonPath("$.customerName").value("可登记接触客户"))
                .andExpect(jsonPath("$.communicationSummary").value("电话联系王经理，确认演示时间。"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Instant afterSubmit = Instant.now();
        JsonNode json = objectMapper.readTree(response);
        Instant contactTime = Instant.parse(json.get("contactTime").asText());
        assertThat(json.get("id").asText()).isNotBlank();
        assertThat(contactTime).isBetween(beforeSubmit, afterSubmit);

        mvc.perform(get("/api/sales/activity-calendar/today")
                        .header("Authorization", bearer(memberToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].customerId").value(customerId))
                .andExpect(jsonPath("$[0].registered").value(true));

        mvc.perform(get("/api/sales/customers/" + customerId)
                        .header("Authorization", bearer(memberToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerContacts[0].id").value(json.get("id").asText()))
                .andExpect(jsonPath("$.customerContacts[0].communicationSummary").value("电话联系王经理，确认演示时间。"));

        mvc.perform(delete("/api/sales/customer-contacts/" + json.get("id").asText())
                        .header("Authorization", bearer(memberToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void salesMemberCannotSubmitContactForUnownedInactiveOrBlankSummary() throws Exception {
        String managerToken = login("13800000000", "admin123456");
        createSalesMember(managerToken, "19100000000");
        createSalesMember(managerToken, "19200000000");
        String firstMemberToken = login("19100000000", "member123");
        String secondMemberToken = login("19200000000", "member123");
        String firstCustomerId = createProspectiveCustomer(firstMemberToken, "接触归属客户");
        String inactiveCustomerId = createProspectiveCustomer(firstMemberToken, "停用接触客户");
        deactivateCustomer(managerToken, inactiveCustomerId);

        mvc.perform(post("/api/sales/customer-contacts")
                        .header("Authorization", bearer(secondMemberToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerId":"%s","communicationSummary":"尝试登记他人客户。"}
                                """.formatted(firstCustomerId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("客户不存在"));

        mvc.perform(post("/api/sales/customer-contacts")
                        .header("Authorization", bearer(firstMemberToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerId":"%s","communicationSummary":"尝试登记停用客户。"}
                                """.formatted(inactiveCustomerId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("停用客户不能提交客户接触记录"));

        mvc.perform(post("/api/sales/customer-contacts")
                        .header("Authorization", bearer(firstMemberToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerId":"%s","communicationSummary":" "}
                                """.formatted(firstCustomerId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void additionalSameDayContactsDoNotCreateAdditionalTodayPlanRows() throws Exception {
        String managerToken = login("13800000000", "admin123456");
        createSalesMember(managerToken, "19300000000");
        String memberToken = login("19300000000", "member123");
        String customerId = createProspectiveCustomer(memberToken, "多次接触计划客户");
        createTodayPlannedContact(memberToken, customerId);

        submitCustomerContact(memberToken, customerId, "上午电话联系。");
        submitCustomerContact(memberToken, customerId, "下午微信补充材料。");

        mvc.perform(get("/api/sales/activity-calendar/today")
                        .header("Authorization", bearer(memberToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].customerId").value(customerId))
                .andExpect(jsonPath("$[0].registered").value(true))
                .andExpect(jsonPath("$[1]").doesNotExist());
        assertThat(customerContacts.findByCustomerIdAndSalesMemberIdOrderByContactTimeDesc(customerId, salesMemberId("19300000000")))
                .hasSize(2);
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

    private void createTodayPlannedContact(String memberToken, String customerId) throws Exception {
        String plannedContactId = createPlannedContact(memberToken, customerId, LocalDate.now().plusDays(1));
        var plannedContact = plannedContacts.findById(plannedContactId).orElseThrow();
        plannedContact.changePlannedDate(LocalDate.now());
        plannedContacts.saveAndFlush(plannedContact);
    }

    private void submitCustomerContact(String memberToken, String customerId, String summary) throws Exception {
        mvc.perform(post("/api/sales/customer-contacts")
                        .header("Authorization", bearer(memberToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerId":"%s","communicationSummary":"%s"}
                                """.formatted(customerId, summary)))
                .andExpect(status().isCreated());
    }

    private void deactivateCustomer(String managerToken, String customerId) throws Exception {
        mvc.perform(post("/api/manager/customers/" + customerId + "/deactivate")
                        .header("Authorization", bearer(managerToken)))
                .andExpect(status().isOk());
    }

    private String salesMemberId(String phoneNumber) throws Exception {
        String managerToken = login("13800000000", "admin123456");
        String response = mvc.perform(get("/api/manager/members")
                        .header("Authorization", bearer(managerToken)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        for (JsonNode member : objectMapper.readTree(response)) {
            if (member.get("phoneNumber").asText().equals(phoneNumber)) {
                return member.get("id").asText();
            }
        }
        throw new IllegalArgumentException("成员不存在");
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
