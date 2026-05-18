package com.cms.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cms.backend.account.UserAccountRepository;
import com.cms.backend.activity.CustomerContact;
import com.cms.backend.activity.CustomerContactRepository;
import com.cms.backend.activity.PlannedContactRepository;
import com.cms.backend.customer.CustomerRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class TodayListTaskIntegrationTests {
    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    PlannedContactRepository plannedContacts;

    @Autowired
    CustomerContactRepository customerContacts;

    @Autowired
    CustomerRepository customers;

    @Autowired
    UserAccountRepository accounts;

    @Test
    void todayListShowsOnlyCurrentSalesMembersTodayPlansWithUnregisteredHighAttentionFirst() throws Exception {
        String managerToken = login("13800000000", "admin123456");
        createSalesMember(managerToken, "18000000000");
        createSalesMember(managerToken, "18100000000");
        String memberToken = login("18000000000", "member123");
        String otherMemberToken = login("18100000000", "member123");

        String lowTodayCustomerId = createProspectiveCustomer(memberToken, "今日低关注客户");
        String highTodayCustomerId = createProspectiveCustomer(memberToken, "今日高关注客户");
        String tomorrowCustomerId = createProspectiveCustomer(memberToken, "明日计划客户");
        String otherMemberCustomerId = createProspectiveCustomer(otherMemberToken, "其他成员今日客户");
        updateAttentionLevel(memberToken, lowTodayCustomerId, 2);
        updateAttentionLevel(memberToken, highTodayCustomerId, 5);

        createTodayPlannedContact(memberToken, lowTodayCustomerId);
        createTodayPlannedContact(memberToken, highTodayCustomerId);
        createPlannedContact(memberToken, tomorrowCustomerId, LocalDate.now().plusDays(2));
        createTodayPlannedContact(otherMemberToken, otherMemberCustomerId);

        mvc.perform(get("/api/sales/activity-calendar/today")
                        .header("Authorization", bearer(memberToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].customerId").value(highTodayCustomerId))
                .andExpect(jsonPath("$[0].customerName").value("今日高关注客户"))
                .andExpect(jsonPath("$[0].attentionLevel").value(5))
                .andExpect(jsonPath("$[0].registered").value(false))
                .andExpect(jsonPath("$[1].customerId").value(lowTodayCustomerId))
                .andExpect(jsonPath("$[1].customerName").value("今日低关注客户"))
                .andExpect(jsonPath("$[1].attentionLevel").value(2))
                .andExpect(jsonPath("$[1].registered").value(false))
                .andExpect(jsonPath("$[2]").doesNotExist());
    }

    @Test
    void todayListKeepsRegisteredPlansAfterUnregisteredPlansAndSortsByLastContactWithinAttentionLevel() throws Exception {
        String managerToken = login("13800000000", "admin123456");
        createSalesMember(managerToken, "18200000000");
        String memberToken = login("18200000000", "member123");

        String neverContactedId = createProspectiveCustomer(memberToken, "从未接触客户");
        String olderContactId = createProspectiveCustomer(memberToken, "较早接触客户");
        String recentContactId = createProspectiveCustomer(memberToken, "最近接触客户");
        String registeredId = createProspectiveCustomer(memberToken, "已登记高关注客户");
        updateAttentionLevel(memberToken, neverContactedId, 4);
        updateAttentionLevel(memberToken, olderContactId, 4);
        updateAttentionLevel(memberToken, recentContactId, 4);
        updateAttentionLevel(memberToken, registeredId, 5);

        createTodayPlannedContact(memberToken, neverContactedId);
        createTodayPlannedContact(memberToken, olderContactId);
        createTodayPlannedContact(memberToken, recentContactId);
        createTodayPlannedContact(memberToken, registeredId);

        var member = accounts.findByPhoneNumber("18200000000").orElseThrow();
        saveCustomerContact(member.getId(), olderContactId, Instant.now().minus(7, ChronoUnit.DAYS));
        saveCustomerContact(member.getId(), recentContactId, Instant.now().minus(1, ChronoUnit.DAYS));
        saveCustomerContact(member.getId(), registeredId, Instant.now());

        mvc.perform(get("/api/sales/activity-calendar/today")
                        .header("Authorization", bearer(memberToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].customerId").value(neverContactedId))
                .andExpect(jsonPath("$[0].registered").value(false))
                .andExpect(jsonPath("$[1].customerId").value(olderContactId))
                .andExpect(jsonPath("$[1].registered").value(false))
                .andExpect(jsonPath("$[2].customerId").value(recentContactId))
                .andExpect(jsonPath("$[2].registered").value(false))
                .andExpect(jsonPath("$[3].customerId").value(registeredId))
                .andExpect(jsonPath("$[3].registered").value(true))
                .andExpect(jsonPath("$[4]").doesNotExist());
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

    private void updateAttentionLevel(String memberToken, String customerId, int attentionLevel) throws Exception {
        mvc.perform(patch("/api/sales/customers/" + customerId + "/attention-level")
                        .header("Authorization", bearer(memberToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"attentionLevel":%d}
                                """.formatted(attentionLevel)))
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

    private void createTodayPlannedContact(String memberToken, String customerId) throws Exception {
        String plannedContactId = createPlannedContact(memberToken, customerId, LocalDate.now().plusDays(1));
        var plannedContact = plannedContacts.findById(plannedContactId).orElseThrow();
        plannedContact.changePlannedDate(LocalDate.now());
        plannedContacts.saveAndFlush(plannedContact);
    }

    private void saveCustomerContact(String salesMemberId, String customerId, Instant contactTime) {
        var salesMember = accounts.findById(salesMemberId).orElseThrow();
        var customer = customers.findById(customerId).orElseThrow();
        customerContacts.saveAndFlush(new CustomerContact(salesMember, customer, contactTime, "一次客户接触记录"));
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
