package com.example.myauth.dto.admin;

import com.example.myauth.entity.Visibility;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminVisibilityChangeRequest {

  @NotNull(message = "변경할 공개 범위는 필수입니다")
  private Visibility visibility;
}
