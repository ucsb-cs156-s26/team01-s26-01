package edu.ucsb.cs156.example.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * This is a JPA entity that represents MenuItemReview, i.e. an entry that comes from the
 * MenuItemReview table in the database.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity(name = "menu_item_reviews")
public class MenuItemReview {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long id;

  private Long itemId; // the id in the UCSBDiningCommonsMenuItems table of a menu item
  private String reviewerEmail; // the email of the reviewer
  private int stars; // 0 to 5 stars
  private LocalDateTime dateReviewed;
  private String comments;
}
