package com.perkmanager.perk_manager.services;

import com.perkmanager.perk_manager.controller.AuthController;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Exercises the web, service, repository, and database layers together. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Test
    void registrationLoginAndMembershipChangesPersistThroughTheWebFlow() throws Exception {
        String username = "it-" + UUID.randomUUID().toString().substring(0, 12);
        String password = "integration-pass";

        var registration = mockMvc.perform(post("/register")
                        .param("username", username)
                        .param("password", password)
                        .param("confirmPassword", password))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) registration.getRequest().getSession(false);
        Long userId = (Long) session.getAttribute(AuthController.SESSION_USER_ID);
        org.junit.jupiter.api.Assertions.assertNotNull(userId);
        org.junit.jupiter.api.Assertions.assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE id = ? AND username = ?", Integer.class, userId, username));

        // Logout and authenticate against the stored password hash.
        mockMvc.perform(post("/logout").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
        var login = mockMvc.perform(post("/login")
                        .param("username", username)
                        .param("password", password))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"))
                .andReturn();
        session = (MockHttpSession) login.getRequest().getSession(false);
        org.junit.jupiter.api.Assertions.assertNotNull(session);
        org.junit.jupiter.api.Assertions.assertEquals(userId,
                session.getAttribute(AuthController.SESSION_USER_ID),
                "The login request should leave the authenticated user in its session.");

        Long membershipId = jdbcTemplate.queryForObject(
                "SELECT id FROM memberships WHERE name = 'Air Miles'", Long.class);
        mockMvc.perform(post("/profile/memberships")
                        .session(session)
                        .param("membershipId", membershipId.toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"));

        // The test transaction is rolled back at the end, so flush JPA writes before
        // checking the join table directly with JdbcTemplate.
        entityManager.flush();
        org.junit.jupiter.api.Assertions.assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_memberships WHERE user_id = ? AND membership_id = ?",
                Integer.class, userId, membershipId));

        mockMvc.perform(get("/profile").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"))
                .andExpect(model().attribute("profileName", username));

        mockMvc.perform(post("/profile/memberships/{membershipId}/remove", membershipId).session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"));
        entityManager.flush();
        org.junit.jupiter.api.Assertions.assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_memberships WHERE user_id = ? AND membership_id = ?",
                Integer.class, userId, membershipId));
    }

    @Test
    void perkApiAndSearchPageReadMigratedPerksFromTheDatabase() throws Exception {
        Long airMilesId = jdbcTemplate.queryForObject(
                "SELECT id FROM memberships WHERE name = 'Air Miles'", Long.class);

        mockMvc.perform(get("/api/perks").param("membershipId", airMilesId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Free Domestic Flight"))
                .andExpect(jsonPath("$[0].membership.name").value("Air Miles"));

        mockMvc.perform(get("/perks").param("q", "Domestic Flights"))
                .andExpect(status().isOk())
                .andExpect(view().name("perks"))
                .andExpect(model().attribute("loggedIn", false))
                .andExpect(model().attribute("rows", hasSize(1)));
    }

    @Test
    void signedInUserCanCreateAndVoteForPerkAndSeeItOnTheirProfile() throws Exception {
        String username = "perk-it-" + UUID.randomUUID().toString().substring(0, 10);
        String password = "integration-pass";
        Long membershipId = jdbcTemplate.queryForObject(
                "SELECT id FROM memberships WHERE name = 'Air Miles'", Long.class);

        var registration = mockMvc.perform(post("/register")
                        .param("username", username)
                        .param("password", password)
                        .param("confirmPassword", password))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"))
                .andReturn();
        MockHttpSession session = (MockHttpSession) registration.getRequest().getSession(false);
        Long userId = (Long) session.getAttribute(AuthController.SESSION_USER_ID);

        mockMvc.perform(post("/profile/memberships")
                        .session(session)
                        .param("membershipId", membershipId.toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"));

        String title = "Integration perk " + UUID.randomUUID().toString().substring(0, 8);
        mockMvc.perform(post("/perks")
                        .session(session)
                        .param("title", title)
                        .param("productName", "Integration product")
                        .param("description", "Created through the integration test")
                        .param("discountType", "PERCENTAGE")
                        .param("discountValue", "15")
                        .param("membershipId", membershipId.toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/perks"));

        entityManager.flush();
        Long perkId = jdbcTemplate.queryForObject(
                "SELECT id FROM perks WHERE title = ?", Long.class, title);
        org.junit.jupiter.api.Assertions.assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM perks WHERE id = ? AND membership_id = ? AND discount_value = 15",
                Integer.class, perkId, membershipId));

        mockMvc.perform(post("/perks/{perkId}/vote", perkId)
                        .session(session)
                        .param("value", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/perks"));
        entityManager.flush();
        org.junit.jupiter.api.Assertions.assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM perk_votes WHERE user_id = ? AND perk_id = ? AND vote_value = 1",
                Integer.class, userId, perkId));

        mockMvc.perform(get("/profile").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"))
                .andExpect(model().attribute("profileName", username))
                .andExpect(model().attribute("memberships", hasSize(1)))
                .andExpect(model().attribute("perkRows", hasItem(
                        allOf(
                                hasProperty("perk", hasProperty("title", is(title))),
                                hasProperty("voteScore", is(1)),
                                hasProperty("myVote", is((short) 1))))));
    }
}
