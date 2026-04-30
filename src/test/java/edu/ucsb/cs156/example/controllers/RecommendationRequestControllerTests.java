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
import edu.ucsb.cs156.example.entities.RecommendationRequest;
import edu.ucsb.cs156.example.repositories.RecommendationRequestRepository;
import edu.ucsb.cs156.example.repositories.UserRepository;
import edu.ucsb.cs156.example.testconfig.TestConfig;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(controllers = RecommendationRequestController.class)
@Import(TestConfig.class)
public class RecommendationRequestControllerTests extends ControllerTestCase {

  @MockitoBean RecommendationRequestRepository recommendationRequestRepository;

  @MockitoBean UserRepository userRepository;

  // Authorization tests for /api/recommendationrequest/admin/all

  @Test
  public void logged_out_users_cannot_get_all() throws Exception {
    mockMvc
        .perform(get("/api/recommendationrequest/all"))
        .andExpect(status().is(403)); // logged out users can't get all
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_users_can_get_all() throws Exception {
    mockMvc.perform(get("/api/recommendationrequest/all")).andExpect(status().is(200)); // logged
  }

  @Test
  public void logged_out_users_cannot_get_by_id() throws Exception {
    mockMvc
        .perform(get("/api/recommendationrequest").param("id", "7"))
        .andExpect(status().is(403)); // logged out users can't get by id
  }

  // Authorization tests for /api/recommendationrequest/post
  // (Perhaps should also have these for put and delete)

  @Test
  public void logged_out_users_cannot_post() throws Exception {
    mockMvc
        .perform(
            post("/api/recommendationrequest/post")
                .param("requesterEmail", "testrequester@123.com")
                .param("professorEmail", "testprofessor@123.com")
                .param("explanation", "test explanation")
                .param("dateRequested", "2022-01-03T00:00:00")
                .param("dateNeeded", "2022-01-17T00:00:00")
                .param("done", "false")
                .with(csrf()))
        .andExpect(status().is(403));
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_regular_users_cannot_post() throws Exception {
    mockMvc
        .perform(
            post("/api/recommendationrequest/post")
                .param("requesterEmail", "testrequester@123.com")
                .param("professorEmail", "testprofessor@123.com")
                .param("explanation", "test explanation")
                .param("dateRequested", "2022-01-03T00:00:00")
                .param("dateNeeded", "2022-01-17T00:00:00")
                .param("done", "false")
                .with(csrf()))
        .andExpect(status().is(403)); // only admins can post
  }

  // // Tests with mocks for database actions

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_user_can_get_all_recommendationrequest() throws Exception {

    // arrange
    LocalDateTime ldt1 = LocalDateTime.parse("2022-01-03T00:00:00");

    RecommendationRequest recommendationRequest1 =
        RecommendationRequest.builder()
            .requesterEmail("testrequester1@234.com")
            .professorEmail("testprofessor1@234.com")
            .explanation("test explanation1")
            .dateRequested(ldt1)
            .dateNeeded(ldt1)
            .done(false)
            .build();

    LocalDateTime ldt2 = LocalDateTime.parse("2022-03-11T00:00:00");

    RecommendationRequest recommendationRequest2 =
        RecommendationRequest.builder()
            .requesterEmail("testrequester2@123.com")
            .professorEmail("testprofessor2@123.com")
            .explanation("test explanatison2")
            .dateRequested(ldt2)
            .dateNeeded(ldt2)
            .done(false)
            .build();

    ArrayList<RecommendationRequest> expectedDates = new ArrayList<>();
    expectedDates.addAll(Arrays.asList(recommendationRequest1, recommendationRequest2));

    when(recommendationRequestRepository.findAll()).thenReturn(expectedDates);

    // act
    MvcResult response =
        mockMvc
            .perform(get("/api/recommendationrequest/all"))
            .andExpect(status().isOk())
            .andReturn();

    // assert

    verify(recommendationRequestRepository, times(1)).findAll();
    String expectedJson = mapper.writeValueAsString(expectedDates);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void an_admin_user_can_post_a_new_recommendationrequest() throws Exception {
    // arrange

    LocalDateTime ldt1 = LocalDateTime.parse("2022-01-03T00:00:00");
    LocalDateTime ldt2 = LocalDateTime.parse("2022-01-17T00:00:00");

    RecommendationRequest recommendationRequest1 =
        RecommendationRequest.builder()
            .requesterEmail("testrequester@123.com")
            .professorEmail("testprofessor@123.com")
            .explanation("test explanation")
            .dateRequested(ldt1)
            .dateNeeded(ldt2)
            .done(false)
            .build();

    when(recommendationRequestRepository.save(eq(recommendationRequest1)))
        .thenReturn(recommendationRequest1);

    // act
    MvcResult response =
        mockMvc
            .perform(
                post("/api/recommendationrequest/post")
                    .param("requesterEmail", "testrequester@123.com")
                    .param("professorEmail", "testprofessor@123.com")
                    .param("explanation", "test explanation")
                    .param("dateRequested", "2022-01-03T00:00:00")
                    .param("dateNeeded", "2022-01-17T00:00:00")
                    .param("done", "false")
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    // assert
    verify(recommendationRequestRepository, times(1)).save(recommendationRequest1);
    String expectedJson = mapper.writeValueAsString(recommendationRequest1);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void test_that_logged_in_user_can_get_by_id_when_the_id_exists() throws Exception {

    // arrange
    LocalDateTime ldt1 = LocalDateTime.parse("2022-01-03T00:00:00");
    LocalDateTime ldt2 = LocalDateTime.parse("2022-01-17T00:00:00");

    RecommendationRequest recommendationRequest =
        RecommendationRequest.builder()
            .requesterEmail("testrequester@123.com")
            .professorEmail("testprofessor@123.com")
            .explanation("test explanation")
            .dateRequested(ldt1)
            .dateNeeded(ldt2)
            .done(false)
            .build();

    when(recommendationRequestRepository.findById(eq(7L)))
        .thenReturn(Optional.of(recommendationRequest));

    // act
    MvcResult response =
        mockMvc
            .perform(get("/api/recommendationrequest").param("id", "7"))
            .andExpect(status().isOk())
            .andReturn();

    // assert

    verify(recommendationRequestRepository, times(1)).findById(eq(7L));
    String expectedJson = mapper.writeValueAsString(recommendationRequest);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void test_that_logged_in_user_can_get_by_id_when_the_id_does_not_exist() throws Exception {

    // arrange

    when(recommendationRequestRepository.findById(eq(7L))).thenReturn(Optional.empty());

    // act
    MvcResult response =
        mockMvc
            .perform(get("/api/recommendationrequest").param("id", "7"))
            .andExpect(status().isNotFound())
            .andReturn();

    // assert

    verify(recommendationRequestRepository, times(1)).findById(eq(7L));
    Map<String, Object> json = responseToJson(response);
    assertEquals("EntityNotFoundException", json.get("type"));
    assertEquals("RecommendationRequest with id 7 not found", json.get("message"));
  }
}
