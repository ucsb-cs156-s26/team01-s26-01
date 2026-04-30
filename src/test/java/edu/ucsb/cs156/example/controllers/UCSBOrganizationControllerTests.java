package edu.ucsb.cs156.example.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import edu.ucsb.cs156.example.ControllerTestCase;
import edu.ucsb.cs156.example.entities.UCSBOrganization;
import edu.ucsb.cs156.example.repositories.UCSBOrganizationRepository;
import edu.ucsb.cs156.example.repositories.UserRepository;
import edu.ucsb.cs156.example.testconfig.TestConfig;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(controllers = UCSBOrganizationController.class)
@Import(TestConfig.class)
public class UCSBOrganizationControllerTests extends ControllerTestCase {

  @MockitoBean UCSBOrganizationRepository ucsbOrganizationRepository;

  @MockitoBean UserRepository userRepository;

  @Test
  public void logged_out_users_cannot_get_all() throws Exception {
    mockMvc
        .perform(get("/api/UCSBOrganization/all"))
        .andExpect(status().is(403)); // logged out users can't get all
  }

  // Authorization tests for /api/ucsborganizationpost
  // (Perhaps should also have these for put and delete)
  @Test
  public void logged_out_users_cannot_post() throws Exception {
    mockMvc
        .perform(
            post("/api/UCSBOrganization/post")
                .param("orgCode", "CSClub")
                .param("orgTranslationShort", "CSC")
                .param("orgTranslation", "Computer Science Club")
                .param("inactive", "false")
                .with(csrf()))
        .andExpect(status().is(403));
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_regular_users_cannot_post() throws Exception {
    mockMvc
        .perform(
            post("/api/UCSBOrganization/post")
                .param("orgCode", "CSClub")
                .param("orgTranslationShort", "CSC")
                .param("orgTranslation", "Computer Science Club")
                .param("inactive", "false")
                .with(csrf()))
        .andExpect(status().is(403)); // only admins can post
  }

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void an_admin_user_can_post_a_new_organization() throws Exception {
    UCSBOrganization cs =
        UCSBOrganization.builder()
            .orgCode("CSClub")
            .orgTranslationShort("CSC")
            .orgTranslation("Computer Science Club")
            .inactive(true)
            .build();

    UCSBOrganization savedCs =
        UCSBOrganization.builder()
            .orgCode("CSClub")
            .orgTranslationShort("CSC Saved")
            .orgTranslation("Computer Science Club Saved")
            .inactive(true)
            .build();

    when(ucsbOrganizationRepository.save(eq(cs))).thenReturn(savedCs);

    MvcResult response =
        mockMvc
            .perform(
                post("/api/UCSBOrganization/post")
                    .param("orgCode", "CSClub")
                    .param("orgTranslationShort", "CSC")
                    .param("orgTranslation", "Computer Science Club")
                    .param("inactive", "true")
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    verify(ucsbOrganizationRepository, times(1)).save(cs);

    String expectedJson = mapper.writeValueAsString(savedCs);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_user_can_get_all_ucsborganizations() throws Exception {
    UCSBOrganization cs =
        UCSBOrganization.builder()
            .orgCode("CSClub")
            .orgTranslationShort("CSC")
            .orgTranslation("Computer Science Club")
            .inactive(false)
            .build();

    ArrayList<UCSBOrganization> expectedOrganizations = new ArrayList<>();
    expectedOrganizations.add(cs);

    when(ucsbOrganizationRepository.findAll()).thenReturn(expectedOrganizations);

    MvcResult response =
        mockMvc.perform(get("/api/UCSBOrganization/all")).andExpect(status().isOk()).andReturn();

    verify(ucsbOrganizationRepository, times(1)).findAll();

    String expectedJson = mapper.writeValueAsString(expectedOrganizations);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }

  @Test
  public void logged_out_users_cannot_get_by_id() throws Exception {
    mockMvc.perform(get("/api/UCSBOrganization").param("code", "cs")).andExpect(status().is(403));
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void test_that_logged_in_user_can_get_by_id_when_the_id_does_not_exist() throws Exception {
    when(ucsbOrganizationRepository.findById(eq("cs"))).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(get("/api/UCSBOrganization").param("code", "cs"))
            .andExpect(status().isNotFound())
            .andReturn();

    verify(ucsbOrganizationRepository, times(1)).findById(eq("cs"));

    Map<String, Object> json = responseToJson(response);
    assertEquals("EntityNotFoundException", json.get("type"));
    assertEquals("UCSBOrganization with id cs not found", json.get("message"));
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void test_that_logged_in_user_can_get_by_id_when_the_id_does_exist() throws Exception {
    UCSBOrganization cs =
        UCSBOrganization.builder()
            .orgCode("CSClub")
            .orgTranslationShort("CSC")
            .orgTranslation("Computer Science Club")
            .inactive(true)
            .build();

    when(ucsbOrganizationRepository.findById(eq("CSClub"))).thenReturn(Optional.of(cs));

    MvcResult response =
        mockMvc
            .perform(get("/api/UCSBOrganization").param("code", "CSClub"))
            .andExpect(status().isOk())
            .andReturn();

    verify(ucsbOrganizationRepository, times(1)).findById(eq("CSClub"));

    String expectedJson = mapper.writeValueAsString(cs);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }

  // PUT

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void admin_can_edit_an_existing_organization() throws Exception {

    UCSBOrganization original =
        UCSBOrganization.builder()
            .orgCode("CSClub")
            .orgTranslationShort("CSC")
            .orgTranslation("Computer Science Club")
            .inactive(false)
            .build();

    UCSBOrganization updated =
        UCSBOrganization.builder()
            .orgCode("CSClub")
            .orgTranslationShort("CS Updated")
            .orgTranslation("Computer Science Club Updated")
            .inactive(true)
            .build();

    String requestBody = mapper.writeValueAsString(updated);

    when(ucsbOrganizationRepository.findById(eq("CSClub"))).thenReturn(Optional.of(original));

    MvcResult response =
        mockMvc
            .perform(
                put("/api/UCSBOrganization?code=CSClub")
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .content(requestBody)
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    verify(ucsbOrganizationRepository, times(1)).findById(eq("CSClub"));
    verify(ucsbOrganizationRepository, times(1)).save(original);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(requestBody, responseString);
  }

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void admin_cannot_edit_organization_that_does_not_exist() throws Exception {

    UCSBOrganization updated =
        UCSBOrganization.builder()
            .orgCode("CSClub")
            .orgTranslationShort("CS Updated")
            .orgTranslation("Computer Science Club Updated")
            .inactive(true)
            .build();

    String requestBody = mapper.writeValueAsString(updated);

    when(ucsbOrganizationRepository.findById(eq("CSClub"))).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(
                put("/api/UCSBOrganization?code=CSClub")
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .content(requestBody)
                    .with(csrf()))
            .andExpect(status().isNotFound())
            .andReturn();

    verify(ucsbOrganizationRepository, times(1)).findById(eq("CSClub"));
    Map<String, Object> json = responseToJson(response);
    assertEquals("EntityNotFoundException", json.get("type"));
    assertEquals("UCSBOrganization with id CSClub not found", json.get("message"));
  }

  // DELETE

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void admin_can_delete_an_organization() throws Exception {

    UCSBOrganization org =
        UCSBOrganization.builder()
            .orgCode("CSClub")
            .orgTranslationShort("CSC")
            .orgTranslation("Computer Science Club")
            .inactive(false)
            .build();

    when(ucsbOrganizationRepository.findById(eq("CSClub"))).thenReturn(Optional.of(org));

    MvcResult response =
        mockMvc
            .perform(delete("/api/UCSBOrganization?code=CSClub").with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    verify(ucsbOrganizationRepository, times(1)).findById(eq("CSClub"));
    verify(ucsbOrganizationRepository, times(1)).delete(org);
    String responseString = response.getResponse().getContentAsString();
    assertEquals("{\"message\":\"UCSBOrganization with id CSClub deleted\"}", responseString);
  }

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void admin_cannot_delete_organization_that_does_not_exist() throws Exception {

    when(ucsbOrganizationRepository.findById(eq("CSClub"))).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(delete("/api/UCSBOrganization?code=CSClub").with(csrf()))
            .andExpect(status().isNotFound())
            .andReturn();

    verify(ucsbOrganizationRepository, times(1)).findById(eq("CSClub"));
    Map<String, Object> json = responseToJson(response);
    assertEquals("EntityNotFoundException", json.get("type"));
    assertEquals("UCSBOrganization with id CSClub not found", json.get("message"));
  }
}
