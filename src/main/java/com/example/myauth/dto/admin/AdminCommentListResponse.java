package com.example.myauth.dto.admin;

import com.example.myauth.entity.Comment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminCommentListResponse {
  private Long id;
  private String content;
  private Long postId;
  private String postContentPreview;
  private AdminPostListResponse.AdminAuthorInfo author;
  private Long parentId;
  private Integer likeCount;
  private Boolean isDeleted;
  private LocalDateTime createdAt;

  public static AdminCommentListResponse from(Comment comment) {
    String postPreview = comment.getPost().getContent();
    if (postPreview != null && postPreview.length() > 50) {
      postPreview = postPreview.substring(0, 50) + "...";
    }

    return AdminCommentListResponse.builder()
        .id(comment.getId())
        .content(comment.getContent())
        .postId(comment.getPost().getId())
        .postContentPreview(postPreview)
        .author(AdminPostListResponse.AdminAuthorInfo.builder()
            .id(comment.getUser().getId())
            .name(comment.getUser().getName())
            .email(comment.getUser().getEmail())
            .build())
        .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
        .likeCount(comment.getLikeCount())
        .isDeleted(comment.getIsDeleted())
        .createdAt(comment.getCreatedAt())
        .build();
  }
}
