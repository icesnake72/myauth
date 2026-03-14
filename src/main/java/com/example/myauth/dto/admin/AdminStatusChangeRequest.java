package com.example.myauth.dto.admin;

import com.example.myauth.entity.User;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminStatusChangeRequest {

  @NotNull(message = "변경할 상태는 필수입니다")
  private User.Status status;

  private String reason;
}
