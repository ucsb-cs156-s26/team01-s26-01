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
import edu.ucsb.cs156.example.entities.MenuItemReview;
import edu.ucsb.cs156.example.repositories.MenuItemReviewRepository;
import edu.ucsb.cs156.example.repositories.UserRepository;
import edu.ucsb.cs156.example.testconfig.TestConfig;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(controllers = MenuItemReviewController.class)
@Import(TestConfig.class)
public class MenuItemReviewControllerTests extends ControllerTestCase {

  @MockBean MenuItemReviewRepository menuItemReviewRepository;

  @MockBean UserRepository userRepository;

  // GET ALL

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_users_can_get_all() throws Exception {
    mockMvc.perform(get("/api/menuitemreview/all")).andExpect(status().isOk());
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_user_can_get_all_menuitemreviews() throws Exception {

    LocalDateTime ldt1 = LocalDateTime.parse("2022-01-03T00:00:00");

    MenuItemReview review1 =
        MenuItemReview.builder()
            .itemId(1L)
            .reviewerEmail("user1@test.com")
            .stars(5)
            .dateReviewed(ldt1)
            .comments("Great")
            .build();

    ArrayList<MenuItemReview> expectedReviews = new ArrayList<>();
    expectedReviews.addAll(Arrays.asList(review1));

    when(menuItemReviewRepository.findAll()).thenReturn(expectedReviews);

    // act
    MvcResult response =
        mockMvc.perform(get("/api/menuitemreview/all")).andExpect(status().isOk()).andReturn();

    // assert
    verify(menuItemReviewRepository, times(1)).findAll();
    String expectedJson = mapper.writeValueAsString(expectedReviews);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }

  // POST

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void an_admin_user_can_post_a_new_review() throws Exception {
    // arrange
    LocalDateTime ldt1 = LocalDateTime.parse("2022-01-03T00:00:00");

    MenuItemReview review1 =
        MenuItemReview.builder()
            .itemId(1L)
            .reviewerEmail("admin@test.com")
            .stars(5)
            .dateReviewed(ldt1)
            .comments("Awesome")
            .build();

    when(menuItemReviewRepository.save(eq(review1))).thenReturn(review1);

    // act
    MvcResult response =
        mockMvc
            .perform(
                post("/api/menuitemreview/post?itemId=1&reviewerEmail=admin@test.com&stars=5&dateReviewed=2022-01-03T00:00:00&comments=Awesome")
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    // assert
    verify(menuItemReviewRepository, times(1)).save(review1);
    String expectedJson = mapper.writeValueAsString(review1);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }

  // GET BY ID

  @WithMockUser(roles = {"USER"})
  @Test
  public void test_that_logged_in_user_can_get_by_id_when_the_id_exists() throws Exception {

    // arrange
    LocalDateTime ldt = LocalDateTime.parse("2022-01-03T00:00:00");

    MenuItemReview review =
        MenuItemReview.builder()
            .itemId(1L)
            .reviewerEmail("user@test.com")
            .stars(4)
            .dateReviewed(ldt)
            .comments("Nice")
            .build();

    when(menuItemReviewRepository.findById(eq(7L))).thenReturn(java.util.Optional.of(review));

    // act
    MvcResult response =
        mockMvc.perform(get("/api/menuitemreview?id=7")).andExpect(status().isOk()).andReturn();

    // assert
    verify(menuItemReviewRepository, times(1)).findById(eq(7L));
    String expectedJson = mapper.writeValueAsString(review);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void test_that_logged_in_user_can_get_by_id_when_the_id_does_not_exist() throws Exception {

    // arrange
    when(menuItemReviewRepository.findById(eq(7L))).thenReturn(java.util.Optional.empty());

    // act
    MvcResult response =
        mockMvc
            .perform(get("/api/menuitemreview?id=7"))
            .andExpect(status().isNotFound())
            .andReturn();

    // assert
    verify(menuItemReviewRepository, times(1)).findById(eq(7L));
    java.util.Map<String, Object> json = responseToJson(response);
    assertEquals("EntityNotFoundException", json.get("type"));
    assertEquals("MenuItemReview with id 7 not found", json.get("message"));
  }

  // PUT (UPDATE)

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void admin_can_edit_an_existing_review() throws Exception {

    // arrange
    LocalDateTime ldt1 = LocalDateTime.parse("2022-01-03T00:00:00");
    LocalDateTime ldt2 = LocalDateTime.parse("2023-06-15T12:00:00");

    MenuItemReview originalReview =
        MenuItemReview.builder()
            .itemId(1L)
            .reviewerEmail("user@test.com")
            .stars(3)
            .dateReviewed(ldt1)
            .comments("Okay")
            .build();

    MenuItemReview updatedReview =
        MenuItemReview.builder()
            .itemId(2L)
            .reviewerEmail("updated@test.com")
            .stars(5)
            .dateReviewed(ldt2)
            .comments("Actually great")
            .build();

    String requestBody = mapper.writeValueAsString(updatedReview);

    when(menuItemReviewRepository.findById(eq(7L)))
        .thenReturn(java.util.Optional.of(originalReview));

    // act
    MvcResult response =
        mockMvc
            .perform(
                put("/api/menuitemreview?id=7")
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .content(requestBody)
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    // assert
    verify(menuItemReviewRepository, times(1)).findById(eq(7L));
    verify(menuItemReviewRepository, times(1)).save(originalReview);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(requestBody, responseString);
  }

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void admin_cannot_edit_review_that_does_not_exist() throws Exception {

    // arrange
    LocalDateTime ldt = LocalDateTime.parse("2022-01-03T00:00:00");

    MenuItemReview updatedReview =
        MenuItemReview.builder()
            .itemId(1L)
            .reviewerEmail("user@test.com")
            .stars(5)
            .dateReviewed(ldt)
            .comments("Great")
            .build();

    String requestBody = mapper.writeValueAsString(updatedReview);

    when(menuItemReviewRepository.findById(eq(7L))).thenReturn(java.util.Optional.empty());

    // act
    MvcResult response =
        mockMvc
            .perform(
                put("/api/menuitemreview?id=7")
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .content(requestBody)
                    .with(csrf()))
            .andExpect(status().isNotFound())
            .andReturn();

    // assert
    verify(menuItemReviewRepository, times(1)).findById(eq(7L));
    java.util.Map<String, Object> json = responseToJson(response);
    assertEquals("EntityNotFoundException", json.get("type"));
    assertEquals("MenuItemReview with id 7 not found", json.get("message"));
  }

  // DELETE

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void admin_can_delete_a_review() throws Exception {

    // arrange
    LocalDateTime ldt = LocalDateTime.parse("2022-01-03T00:00:00");

    MenuItemReview review =
        MenuItemReview.builder()
            .itemId(1L)
            .reviewerEmail("user@test.com")
            .stars(5)
            .dateReviewed(ldt)
            .comments("Great")
            .build();

    when(menuItemReviewRepository.findById(eq(7L))).thenReturn(java.util.Optional.of(review));

    // act
    MvcResult response =
        mockMvc
            .perform(delete("/api/menuitemreview?id=7").with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    // assert
    verify(menuItemReviewRepository, times(1)).findById(eq(7L));
    verify(menuItemReviewRepository, times(1)).delete(review);
    String responseString = response.getResponse().getContentAsString();
    assertEquals("{\"message\":\"MenuItemReview with id 7 deleted\"}", responseString);
  }

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void admin_cannot_delete_review_that_does_not_exist() throws Exception {

    // arrange
    when(menuItemReviewRepository.findById(eq(7L))).thenReturn(java.util.Optional.empty());

    // act
    MvcResult response =
        mockMvc
            .perform(delete("/api/menuitemreview?id=7").with(csrf()))
            .andExpect(status().isNotFound())
            .andReturn();

    // assert
    verify(menuItemReviewRepository, times(1)).findById(eq(7L));
    java.util.Map<String, Object> json = responseToJson(response);
    assertEquals("EntityNotFoundException", json.get("type"));
    assertEquals("MenuItemReview with id 7 not found", json.get("message"));
  }
}
